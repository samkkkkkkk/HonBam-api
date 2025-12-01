## 특징 (로그인/토큰 구조)

- JWT 기반의 Access Token으로 사용자를 인증하는 로그인 방식을 사용한다.
- Access Token은 짧은 만료 시간(예: 15분)을 가지며, 만료되면 더 이상 사용할 수 없다.
- Access Token만으로는 만료·탈취 상황에서 사용자를 제어하기 어렵기 때문에, 별도의 Refresh Token을 추가로 발급한다.
- Access Token은 JWT로 생성하고, Refresh Token은 의미 없는 랜덤 UUID 문자열로 생성한다.
- Refresh Token은 HttpOnly 쿠키에 원본(UUID) 값으로 담아서 내려보내고, 서버(DB·Redis)에는 해시값만 저장한다.

## Access Token만 사용할 때의 한계

- Access Token만 사용하는 경우, 토큰이 만료되면 사용자는 매번 다시 로그인해야 하므로 UX가 나빠진다.
- 이 문제를 피하기 위해 Access Token의 만료 시간을 길게 가져가면, 탈취되었을 때 공격자가 오랜 시간 사용자로 위장할 수 있어 위험이 커진다.
- JWT는 기본적으로 stateless이기 때문에, 발급된 Access Token을 서버에서 개별적으로 “폐기”하거나 “강제 로그아웃”하기 어렵다.
- 따라서 “짧은 수명의 Access Token”과 “토큰 재발급을 위한 별도의 자격 증명”이 필요해진다.

## Refresh Token + RTR + Redis 도입 이유

### Refresh Token 도입 필요

- Access Token의 만료 시간을 짧게 유지하면서도 사용자가 계속 서비스를 이용할 수 있게 하려면, 유효한 Refresh Token을 가지고 있을 때만 Access Token을 재발급하는 구조가 필요하다.
- 이를 통해 Access Token 탈취의 피해 시간을 최소화하면서도, 사용자는 자주 로그인하지 않고 서비스를 사용할 수 있다.

### Refresh Token을 UUID(opaque)로 만드는 이유

- Refresh Token을 JWT로 만들면 자체 정보가 들어가고, 탈취 시 정보 노출 및 관리 복잡도가 커진다.
- UUID처럼 의미 없는 랜덤 문자열(opaque 토큰)로 만들고, 서버에서만 유효성을 판단하면 탈취됐을 때 정보 노출을 줄이고, 서버에서 언제든지 폐기·회전할 수 있다.

### RTR(Refresh Token Rotation)의 필요성

- Refresh Token을 오래, 여러 번 재사용 가능하게 두면 그 토큰이 한 번이라도 탈취되었을 때 공격자가 계속해서 새 Access Token을 발급받을 수 있다.
- RTR 방식(한 번 사용한 Refresh Token은 바로 폐기하고 새 Refresh Token으로 교체)을 사용하면, 탈취된 토큰이 재사용되는 시점에 이상 징후를 감지하고 해당 계정의 세션을 모두 끊는 등 대응을 할 수 있다.

### Redis 도입 필요성

- Access Token 만료 → Refresh Token 재발급 요청은 트래픽이 많은 서비스에서 빈번하게 발생한다.
- 매번 DB에서 **`findByTokenHash`**를 수행하면 부하가 커지고 응답 지연이 발생할 수 있다.
- Redis를 1차 저장소로 사용하면 인메모리에서 빠르게 존재 여부와 사용자 정보를 조회할 수 있어, 존재하지 않는/이미 폐기된 토큰은 DB까지 내려가지 않고 바로 걸러낼 수 있다.
- TTL을 이용해 만료된 Refresh Token 키를 자동으로 삭제할 수 있어 만료 관리가 단순해진다.

## 해결방안 (현재 구현한 구조와 코드)

### 전체 전략 요약

- Access Token:
    - JWT로 생성, 짧은 만료 시간(예: 15분).
    - 응답 시 HttpOnly 쿠키로 내려보내고, 클라이언트는 이 토큰으로 API 요청을 수행.
- Refresh Token:
    - UUID로 생성되는 opaque 토큰.
    - HttpOnly 쿠키에 원본 UUID 값을 담아서 응답.
    - 서버에서는 해시값만 관리:
        - DB(RefreshToken 테이블)에 **`tokenHash`**, **`expiredAt`**, **`revoked`**, **`userId`**, **`deviceInfo`** 등을 저장.
        - Redis에는 **`refresh:<hash>`** → **`userId`** 형태로 저장하고, TTL을 refresh 만료 시간으로 설정.

### RTR 방식 (한 번 사용한 Refresh Token은 폐기 + 새 토큰 발급)

- 클라이언트가 **`/refresh`** 엔드포인트로 요청을 보내면 다음 순서로 처리한다.
1. 쿠키에서 Refresh Token 추출
    - **`extractCookie(req, "refresh_token")`**으로 HttpOnly 쿠키에서 **`refresh_token`** 값을 읽어온다.
    - 값이 없으면 401(NOT_REFRESH_TOKEN)을 반환.
2. Redis 1차 검증
    - **`refreshHash = hashRefreshToken(refresh)`**로 해시를 계산하고, **`redisKey = "refresh:" + refreshHash`**를 생성한다.
    - Redis에서 **`redisTemplate.opsForValue().get(redisKey)`**로 userId를 조회한다.
        - Redis가 정상이고, userId가 없다면: 해당 Refresh Token은 이미 삭제·만료된 것으로 보고 **`401 INVALID_REFRESH_TOKEN`**을 반환.
        - Redis 장애 시에는 **`redisFail = true`**로 표시하고, DB만으로 검증을 계속 진행한다.
3. DB 검증
    - **`refreshTokenRepository.findByTokenHash(refreshHash)`**로 DB에서 RefreshToken 엔티티를 조회한다.
    - 다음 조건을 검사한다.
        - 엔티티가 없거나 **`revoked == true`**이면 → **`401 REFRESH_TOKEN_REVOKED`**.
        - **`expiredAt < now()`**이면 → **`401 REFRESH_TOKEN_EXPIRED`**.
    - Redis에서 userId를 못 가져온 경우(**`userId == null`**)에는 DB의 **`rt.getUserId()`**를 사용한다.
    - userId로 실제 User 엔티티를 조회해서 존재하지 않으면 예외를 던진다.
4. 기존 Refresh Token 사용 완료 처리 (RTR 핵심)
    - DB의 기존 RefreshToken 엔티티에 대해 **`rt.revoke()`**를 호출해서 **`revoked = true`**로 수정한다.
    - 이로써 같은 Refresh Token이 다시 들어왔을 때 DB 검증 단계에서 거부된다.
5. 새 Access Token 발급
    - **`newAccess = tokenProvider.createAccessToken(user)`**로 새로운 Access Token(JWT)을 생성한다.

   (6) Redis 복구(장애 대응용)

    - 앞에서 Redis가 장애였던 경우(**`redisFail == true`**):
        - DB 검증은 통과했으므로, Redis가 복구되었을 때를 대비해 기존 키(**`redisKey`**)에 userId와 TTL을 다시 세팅한다(로그 메시지로 복구 완료를 남김).

7. 새 Refresh Token 발급 + 저장

- **`refreshToken = tokenProvider.createRefreshToken(user)`**로 새로운 UUID Refresh Token을 생성한다.
- **`newRefreshHash = hashRefreshToken(refreshToken)`**로 해시를 만든다.
- **`newRedisKey = "refresh:" + newRefreshHash`**로 새 Redis 키를 만든다.
- Redis 처리:
    - 장애가 아니라면(**`!redisFail`**) 기존 키(**`redisKey`**)를 삭제한다.
    - 새 키(**`newRedisKey`**)에 userId와 TTL을 설정한다.
- DB 처리:
    - 새 RefreshToken 엔티티를 생성해서 저장한다.
    - 예:
        - **`id = user.getId()`** (혹은 별도 PK)
        - **`tokenHash = newRefreshHash`**
        - **`revoked = false`**
        - **`expiredAt = now + refreshExpireDuration`**
        - **`deviceInfo = "local-login"`** 등

8.  응답 (쿠키 세팅)

- **`cookieUtil.createAccessCookie(newAccess)`**로 새 Access Token 쿠키 생성.
- **`cookieUtil.createRefreshCookie(refreshToken)`**로 새 Refresh Token 쿠키 생성.
- 둘 다 HttpOnly, Secure, Path, SameSite 등을 적절히 설정한 뒤 **`SET_COOKIE`** 헤더로 내려보낸다.

### 이 구조가 가지는 특징 요약

- Access Token은 stateless JWT로 짧게 가져가되, 문제 발생 시에도 Refresh Token으로 부드럽게 재인증할 수 있다.
- Refresh Token은 UUID + 서버 저장(해시) + RTR 방식으로 관리하여, 탈취·재사용 위험을 줄인다.
- Redis + DB 조합을 사용해:
    - Redis: 1차 필터링 및 TTL 기반 만료 처리, 성능 최적화.
    - DB: 영속적인 토큰 상태 관리(revoked, 만료 시각, 디바이스 정보 등)와 보안/추적 요구를 만족.
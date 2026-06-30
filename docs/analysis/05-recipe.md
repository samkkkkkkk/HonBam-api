# 05. 레시피(Recipe) 도메인 분석

## 1. 개요

레시피 도메인은 칵테일 레시피 데이터를 **읽기 전용으로 조회/검색**하는 기능을 제공한다.

- 사용자가 직접 레시피를 생성/수정/삭제하는 기능은 존재하지 않으며, 데이터는 시드 CSV(`seed/cocktail_recipe.csv`)를 MySQL 컨테이너 초기화 시점에 일괄 적재(LOAD DATA)하여 채워진다.
- API는 **전체 목록 조회**와 **칵테일 이름 부분 검색** 두 가지뿐이며, 둘 다 **인증이 필요 없다**(`WebSecurityConfig`에서 `GET /api/recipe/**`를 `permitAll`로 허용).
- 도메인 구조가 단순하여 엔티티 1개, DTO 1개, 리포지토리/서비스/컨트롤러 각 1개로 구성된다.

---

## 2. 구성 요소

| 구성 요소 | 파일 경로 | 역할 |
|-----------|-----------|------|
| `Recipe` (엔티티) | `src/main/java/com/example/HonBam/recipeapi/entity/Recipe.java` | `tbl_recipe` 테이블에 매핑되는 JPA 엔티티. 칵테일 ID/이름/이미지/레시피/상세 레시피 컬럼 보유 |
| `RecipeDetailResponseDTO` | `src/main/java/com/example/HonBam/recipeapi/dto/response/RecipeDetailResponseDTO.java` | 조회/검색 응답 DTO. `Recipe` 엔티티를 그대로 평면 매핑(엔티티의 모든 필드를 동일하게 노출) |
| `RecipeRepository` | `src/main/java/com/example/HonBam/recipeapi/repository/RecipeRepository.java` | Spring Data JPA 리포지토리. `findAll()` 및 이름 부분 검색용 파생 쿼리 메서드 제공 |
| `RecipeService` | `src/main/java/com/example/HonBam/recipeapi/service/RecipeService.java` | 전체 조회/검색 비즈니스 로직. 엔티티 → DTO 변환(`stream().map`) 수행. `@Transactional` 부여 |
| `RecipeController` | `src/main/java/com/example/HonBam/recipeapi/api/RecipeController.java` | REST 진입점. `/api/recipe` 목록 및 `/api/recipe/search` 검색 엔드포인트 제공 |

---

## 3. API 엔드포인트

베이스 경로: `api/recipe` (`@RequestMapping("api/recipe")`, `@CrossOrigin`)

| 메서드 | 경로 | 인증 | 요청 파라미터 | 요약 |
|--------|------|------|----------------|------|
| `GET` | `/api/recipe` | 불필요 (`permitAll`) | 없음 | 전체 레시피 목록을 `List<RecipeDetailResponseDTO>`로 반환 (`RecipeController.java:22-32`) |
| `GET` | `/api/recipe/search` | 불필요 (`permitAll`) | `@RequestParam String name` | 칵테일 이름에 `name`이 포함된 레시피를 대소문자 무시로 검색 (`RecipeController.java:34-45`) |

- 두 엔드포인트 모두 `try/catch`로 감싸 성공 시 `200 OK` + 결과 리스트, 예외 시 `400 Bad Request` + `e.getMessage()`를 반환한다.
- 인증 허용 근거: `src/main/java/com/example/HonBam/config/WebSecurityConfig.java:87` → `.antMatchers(HttpMethod.GET,"/api/recipe/**").permitAll()`.

---

## 4. 데이터 흐름

### 데이터 적재 (초기 1회)

```
seed/cocktail_recipe.csv
   └─(MySQL 컨테이너 init)→ init/02_import.sh : LOAD DATA LOCAL INFILE ... INTO TABLE tbl_recipe
        └─ tbl_recipe (data_id, cocktail_img, cocktail_name, recipe, recipe_detail)
```

- `init/02_import.sh:16-25`에서 CSV(`/var/lib/mysql-files/cocktail_recipe.csv`)를 `tbl_recipe`로 적재하며, `recipe`/`recipe_detail`은 빈 문자열을 `NULLIF`로 NULL 처리한다.
- CSV 헤더: `data_id, cocktail_img, cocktail_name, recipe, recipe_detail` (seed 파일 1행).

### 전체 조회 흐름

```
GET /api/recipe
  → RecipeController.list()
    → RecipeService.getAllRecipes()
      → RecipeRepository.findAll()           // tbl_recipe 전체 SELECT
      → Recipe → RecipeDetailResponseDTO 변환 // new RecipeDetailResponseDTO(recipe)
  → 200 OK [ {dataId, cocktailName, cocktailImg, recipe, recipeDetail}, ... ]
```

### 검색 흐름

```
GET /api/recipe/search?name=하이볼
  → RecipeController.search(name)
    → RecipeService.searchRecipes(name)
      → RecipeRepository.findByCocktailNameContainingIgnoreCase(name)
         // SELECT ... WHERE LOWER(cocktail_name) LIKE LOWER('%name%')
      → Recipe → RecipeDetailResponseDTO 변환
  → 200 OK [ ... ]
```

---

## 5. 영속성

### 엔티티 ↔ 테이블 매핑

`Recipe` 엔티티(`@Table(name = "tbl_recipe")`)는 `init/01_schema.sql`로 생성되는 `tbl_recipe` 테이블에 매핑된다.

| 엔티티 필드 | 컬럼명 | 엔티티 정의 | 스키마(`init/01_schema.sql`) |
|-------------|--------|-------------|------------------------------|
| `dataId` (`Long`) | `data_id` | `@Id @GeneratedValue(IDENTITY)` (`Recipe.java:15-18`) | `INT NOT NULL AUTO_INCREMENT`, `PRIMARY KEY` (`01_schema.sql:5,10`) |
| `cocktailName` (`String`) | `cocktail_name` | `Recipe.java:20-21` | `VARCHAR(255) NOT NULL` (`01_schema.sql:7`) |
| `cocktailImg` (`String`) | `cocktail_img` | `Recipe.java:23-24` | `VARCHAR(1024) NULL` (`01_schema.sql:6`) |
| `recipe` (`String`) | `recipe` | `@Column(columnDefinition = "TEXT")` (`Recipe.java:27-28`) | `TEXT NULL` (`01_schema.sql:8`) |
| `recipeDetail` (`String`) | `recipe_detail` | `@Column(columnDefinition = "LONGTEXT")` (`Recipe.java:30-31`) | `LONGTEXT NULL` (`01_schema.sql:9`) |

- 테이블은 애플리케이션이 아닌 **DB 초기화 SQL(`init/01_schema.sql:4-11`)로 사전 생성**되며, `ENGINE=InnoDB`, `CHARSET=utf8mb4`를 사용한다.
- `RecipeDetailResponseDTO`는 위 5개 필드를 그대로 1:1 노출한다(별도 가공/은닉 없음).

---

## 6. 발견사항

### ① [CONFIRMED] 리포지토리 ID 타입 불일치 — PK는 `Long`인데 `JpaRepository<Recipe, Integer>`

- 근거:
  - `RecipeRepository.java:8` → `public interface RecipeRepository extends JpaRepository<Recipe, Integer>`
  - `Recipe.java:18` → `private Long dataId;` (`@Id`)
- 엔티티 PK 타입은 `Long`이지만 리포지토리의 ID 제네릭 파라미터는 `Integer`로 선언되어 있다. 현재 사용 중인 `findAll()`과 파생 검색 메서드(`findByCocktailNameContainingIgnoreCase`)는 ID 타입에 의존하지 않으므로 **실제 호출 경로에서는 문제가 발생하지 않는다**.
- 다만 `findById(...)`, `getById(...)`, `existsById(...)` 등 **ID 기반 메서드를 사용하게 되면 `Integer`를 인자로 받게 되어** 엔티티 실제 키 타입(`Long`)과 어긋난다. 타입 안전성/일관성 측면에서 `JpaRepository<Recipe, Long>`로 정정하는 것이 옳다. (잠재 결함 — 현 시점 미실행 경로)

### ② [CONFIRMED] 컨트롤러의 `e.printStackTrace()` + 무차별 `badRequest` 예외 처리

- 근거:
  - 목록: `RecipeController.java:27-30` → `catch (Exception e) { e.printStackTrace(); return ResponseEntity.badRequest().body(e.getMessage()); }`
  - 검색: `RecipeController.java:39-42` → 동일 패턴
- 모든 예외를 `Exception` 단일 블록으로 잡아 **무조건 `400 Bad Request`**로 응답한다. DB 장애 등 서버측 오류(본래 5xx에 해당)도 클라이언트 오류처럼 보고되어 의미가 왜곡된다.
- 또한 `log.error(...)`는 주석 처리(`:29`, `:41`)된 채 `e.printStackTrace()`로 표준 에러에 스택트레이스를 직접 출력한다. 운영 환경에서는 SLF4J 로거 사용이 바람직하며, `e.getMessage()`를 응답 본문에 그대로 내려보내는 것은 내부 정보 노출 소지가 있다.

### ③ [CONFIRMED] 인증 없이 전체 데이터 노출

- 근거: `WebSecurityConfig.java:87` → `.antMatchers(HttpMethod.GET,"/api/recipe/**").permitAll()`
- `GET /api/recipe`, `GET /api/recipe/search`는 인증/인가 없이 누구나 호출 가능하다. 레시피 데이터가 공개 콘텐츠라면 의도된 설계이나, 별도 레이트리밋이나 페이징이 없어 `findAll()`이 **전체 레코드를 한 번에 반환**(`RecipeService.java:22-26`)하므로 데이터 규모가 커질 경우 응답 크기/부하 측면의 부담이 있다. (현재는 시드 CSV 기반 소량 데이터로 보임)

### 보조 관찰 [PLAUSIBLE]

- 페이징 부재: `getAllRecipes()`가 `findAll()`로 전량 조회 후 메모리에서 DTO 변환(`RecipeService.java:23-25`). 데이터 증가 시 `Pageable` 도입 검토 여지.
- 읽기 전용 도메인임에도 `RecipeService`에 기본 `@Transactional`(`RecipeService.java:17`)이 클래스 단위로 부여되어 있다. 읽기 작업이므로 `@Transactional(readOnly = true)`가 더 적합하다.

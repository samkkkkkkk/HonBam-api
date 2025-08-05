# --- 1단계: 빌드 ---
FROM eclipse-temurin:17-jdk AS builder
WORKDIR /app

# Gradle 래퍼와 설정 먼저 복사
COPY gradlew .
COPY gradle gradle/
COPY settings.gradle build.gradle ./
# 소스 마지막에 복사
COPY src src/

# 실행 권한 부여 & CRLF 방지
RUN chmod +x gradlew && sed -i 's/\r$//' gradlew

# 의존성 캐시 재사용
RUN ./gradlew clean build -x test --no-daemon

# --- 2단계: 런타임 ---
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
ENTRYPOINT ["java","-jar","/app/app.jar"]

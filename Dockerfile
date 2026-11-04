FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app

COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon > /dev/null 2>&1 || true

COPY src ./src
RUN ./gradlew bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

# 서버(컨테이너) 기본 시간대는 UTC다. @CreationTimestamp·LocalDateTime.now()가 한국 시간으로 찍히도록
# JVM과 컨테이너 시간대를 Asia/Seoul로 맞춘다. DB_URL의 serverTimezone=Asia/Seoul과 같아야
# JDBC 드라이버가 저장·조회 때 시각을 9시간씩 옮기지 않는다.
ENV TZ=Asia/Seoul

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8088
ENTRYPOINT ["java", "-Duser.timezone=Asia/Seoul", "-jar", "app.jar"]

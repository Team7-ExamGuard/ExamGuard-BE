# syntax=docker/dockerfile:1

# ---------- 1단계: 빌드 ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
COPY src src

# Gradle 캐시를 BuildKit 캐시로 재사용 → 의존성을 매번 다시 받지 않음
# bootJar만 실행하므로 build/libs에는 실행 가능한 jar 하나만 생성됨
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew bootJar --no-daemon \
 && cp build/libs/*.jar application.jar \
 && java -Djarmode=tools -jar application.jar extract --layers --destination extracted

# ---------- 2단계: 실행 ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

RUN groupadd --system spring && useradd --system --gid spring --no-create-home spring

# 자주 안 바뀌는 레이어부터 복사 → 코드만 바뀌면 마지막 레이어만 새로 받음
COPY --from=build /workspace/extracted/dependencies/ ./
COPY --from=build /workspace/extracted/spring-boot-loader/ ./
COPY --from=build /workspace/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/extracted/application/ ./

USER spring
EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "application.jar"]

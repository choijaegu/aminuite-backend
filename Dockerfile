# 1. 베이스 이미지 선택 (Java 21을 포함하는 이미지)
# Amazon Corretto 또는 Eclipse Temurin 같은 OpenJDK 이미지를 사용하는 것이 좋습니다.
FROM amazoncorretto:21-alpine-jdk
# 또는 FROM eclipse-temurin:21-jdk-alpine

# 2. 작업 디렉토리 설정
WORKDIR /app

# 3. Gradle Wrapper 파일들 복사 (빌드에 필요)
COPY gradlew .
COPY gradle gradle

# 4. build.gradle 및 settings.gradle (또는 필요한 다른 설정 파일) 복사
# 프로젝트 전체를 복사하기 전에 이 파일들을 먼저 복사하면,
# 의존성만 변경되었을 때 Docker 빌드 캐시를 더 잘 활용할 수 있습니다. (선택적 최적화)
COPY build.gradle settings.gradle ./

# 5. 애플리케이션 소스 코드 전체 복사
COPY src src

# 6. Gradle 빌드 실행하여 JAR 파일 생성
# gradlew 스크립트에 실행 권한 부여 (만약 Git에서 권한이 제대로 안 넘어왔을 경우 대비)
RUN chmod +x ./gradlew
RUN ./gradlew build -x test --no-daemon
# --no-daemon 옵션은 Docker 빌드 환경에서 Gradle 데몬을 사용하지 않도록 하여 리소스 문제를 줄일 수 있습니다.

# 7. 실행 단계 - 빌드된 JAR 파일 실행
# EXPOSE 8080 # Render는 보통 PORT 환경 변수를 주입하므로 Dockerfile에서 EXPOSE는 필수는 아님
# JAR 파일 경로와 이름은 실제 생성되는 것으로 정확히 맞춰야 합니다!
ENTRYPOINT ["java", "-jar", "build/libs/political-chat-backend-0.0.1-SNAPSHOT.jar"]
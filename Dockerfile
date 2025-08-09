# ---------- Builder ----------
FROM gradle:8.7-jdk17-alpine AS builder
WORKDIR /app
COPY . .
# 테스트는 CI에서 따로 돌릴 수 있으니 이미지에서는 스킵
RUN gradle clean build -x test \
 && echo "✅ Build complete. Listing WAR file:" \
 && ls -alh build/libs \
 && echo "✅ WAR sha256 checksum:" \
 && sha256sum build/libs/*.war

# ---------- Runtime ----------
FROM tomcat:9.0-jdk17-temurin

# 타임존(서울) 설정 - 선택
RUN apt-get update && \
    apt-get install -y tzdata && \
    ln -sf /usr/share/zoneinfo/Asia/Seoul /etc/localtime && \
    echo "Asia/Seoul" > /etc/timezone && \
    rm -rf /var/lib/apt/lists/*

# 기존 webapps 정리
RUN rm -rf /usr/local/tomcat/webapps/*

# 가장 최근 빌드된 WAR를 ROOT로
COPY --from=builder /app/build/libs/*.war /usr/local/tomcat/webapps/ROOT.war

# 빌드 정보 라벨
ARG GIT_SHA
ARG BUILD_TIME
LABEL git_sha=$GIT_SHA build_time=$BUILD_TIME

EXPOSE 8080
CMD ["catalina.sh", "run"]

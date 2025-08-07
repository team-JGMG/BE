FROM tomcat:9.0-jdk17-temurin

# ✅ tzdata 설치 및 타임존 설정 추가
RUN apt-get update && \
    apt-get install -y vim && \
    apt-get install -y tzdata && \
    ln -sf /usr/share/zoneinfo/Asia/Seoul /etc/localtime && \
    echo "Asia/Seoul" > /etc/timezone && \
    dpkg-reconfigure -f noninteractive tzdata

# 기존 webapps 폴더 비우기
RUN rm -rf /usr/local/tomcat/webapps/*

# WAR 파일 복사
COPY build/libs/BOBJ-1.0-SNAPSHOT.war /usr/local/tomcat/webapps/ROOT.war

# 포트 오픈
EXPOSE 8080


FROM tomcat:9.0-jdk8

# 기존 webapps 폴더 비우기
RUN rm -rf /usr/local/tomcat/webapps/*

# WAR 파일 복사
COPY build/libs/BOBJ-1.0-SNAPSHOT.war /usr/local/tomcat/webapps/ROOT.war

# 포트 오픈
EXPOSE 8080

CMD ["catalina.sh", "run"]

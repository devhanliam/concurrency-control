#arm계열 아니라면 주석해제
FROM azul/zulu-openjdk:11.0.18-11.62.17-arm64
#arm계열이 아니라면 주석해제
#FROM azul/zulu-openjdk:11.0.18-11.62.17
ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java","-jar","app.jar"]

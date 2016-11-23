FROM java:8-jre-alpine
ADD target/ignite-1.0.jar /app.jar

# allow passing -D arguments as CMD
ENTRYPOINT java $0 $@ -jar /app.jar

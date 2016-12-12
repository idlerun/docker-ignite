FROM java:8-jre-alpine
ADD target/ignite-1.0.jar /app.jar

# allow passing -D arguments as CMD
ENTRYPOINT if [ $0 = "/bin/sh" ]; then exec java -jar /app.jar; else exec java $0 $@ -jar /app.jar; fi
FROM eclipse-temurin:21-jre-alpine
LABEL maintainer="felixzoe"

RUN addgroup -g 1000 appgroup && adduser -u 1000 -G appgroup -s /bin/sh -D appuser

WORKDIR /app
COPY Backend/FelixZoe-server/target/FelixZoe-server-1.0-SNAPSHOT.jar ./felixzoe.jar
COPY docker/app/logback-spring.xml ./logback-spring.xml

RUN mkdir -p /app/logs /app/uploads && chown -R appuser:appgroup /app

USER appuser

# Optimized JVM settings for 2-4GB server with virtual threads
ENV JAVA_OPTS="-XX:+UseZGC \
  -Xmx512m -Xms256m \
  -XX:+HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath=/app/logs/heapdump.hprof \
  -XX:+ExitOnOutOfMemoryError \
  -XX:MaxMetaspaceSize=256m \
  -XX:+UseStringDeduplication \
  -Djdk.virtualThreadScheduler.parallelism=4 \
  -Djdk.virtualThreadScheduler.maxPoolSize=64 \
  -Dfile.encoding=UTF-8 \
  -Duser.timezone=Asia/Shanghai \
  -Djava.security.egd=file:/dev/./urandom"

EXPOSE 5922

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:5922/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dlogging.config=/app/logback-spring.xml -jar felixzoe.jar"]

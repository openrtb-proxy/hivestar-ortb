FROM maven:3-jdk-11 as build
WORKDIR /build-dir
COPY pom.xml /build-dir

# this allows all mvn dependencies to be cached in a layer, future builds
# without pom modification can be quick!  works well with CI tools.
# for local use, you might want to map a volume for your ~/.m2
RUN  mvn -B de.qaware.maven:go-offline-maven-plugin:resolve-dependencies

COPY src /build-dir/src
RUN mvn -B -o package

FROM adoptopenjdk:11-jre-hotspot as package
VOLUME /tmp
ARG DEPENDENCY=/build-dir/target/dependency
COPY --from=0 ${DEPENDENCY}/BOOT-INF/lib /app/lib
COPY --from=0 ${DEPENDENCY}/META-INF /app/META-INF
COPY --from=0 ${DEPENDENCY}/BOOT-INF/classes /app
EXPOSE 8080
HEALTHCHECK CMD curl -s --fail http://localhost:8080/hiveproxy/actuator/health || exit 1
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-cp","app:app/lib/*","starproxy.application.StarproxyApplication"]
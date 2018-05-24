FROM openjdk:8-jdk-alpine
LABEL PROJECT=${project.artifactId}

ENV PROFILE="docker"

EXPOSE 8080

# In maven/ the files as specified in the <assembly> section is stored
# and need to be added manually
COPY maven/ /webapps/

CMD exec java -Dspring.profiles.active=$PROFILE -Djava.security.egd=file:/dev/./urandom -jar /webapps/${project.build.finalName}.jar

FROM ubuntu:${ubuntu.tag}

# Store some project metadata in the Docker image
LABEL ContainerName=${project.name} ContainerSourceCode=${project.url}

# Update packages and install tools
#  Removing /var/lib/apt/lists/* prevents using `apt` unless you do `apt update` first
RUN apt-get update -qq && DEBIAN_FRONTEND=noninteractive apt-get install -qq --no-install-recommends \
    openjdk-17-jre-headless="${jdk.version}" \
    python3="${python3.version}" \
    curl="${curl.version}" \
    < /dev/null > /dev/null && \
    rm -rf /var/lib/apt/lists/*

# Run non privileged
RUN adduser --system ${project.artifactId}

WORKDIR /usr/local/${project.artifactId}
COPY maven/${project.artifactId}-${project.version}.jar /usr/local/${project.artifactId}/

COPY docker-entrypoint.sh /usr/local/bin/
COPY configs/${project.artifactId}.properties.tmpl /etc/${project.artifactId}/${project.artifactId}.properties.tmpl
COPY configs/${project.artifactId}.properties.default \
    /etc/${project.artifactId}/${project.artifactId}.properties.default

RUN mkdir -p /var/log/${project.artifactId} /var/cache/${project.artifactId} \
    /usr/local/${project.artifactId}/file-uploads /etc/${project.artifactId} \
 && touch /etc/${project.artifactId}/${project.artifactId}.properties \
 && chown -R ${project.artifactId} /var/log/${project.artifactId} /var/cache/${project.artifactId} \
    /usr/local/${project.artifactId}/file-uploads /etc/${project.artifactId} \
    /etc/${project.artifactId}/${project.artifactId}.properties /usr/local/bin/docker-entrypoint.sh

USER ${project.artifactId}
ENTRYPOINT ["docker-entrypoint.sh"]
ENV CACHE_DIR="-Dvertx.cacheDirBase=/tmp"
ENV CONFIG_FILE="-Dvertx-config-path=/etc/${project.artifactId}/${project.artifactId}.properties"
ENV JAVA_TOOL_OPTIONS="${jdwp.client.config}"
ENV JAR_PATH="/usr/local/${project.artifactId}/${project.artifactId}-${project.version}.jar"
CMD ["sh", "-c", "java $CACHE_DIR $CONFIG_FILE -Xmx2g -jar $JAR_PATH $LOGBACK_PATH"]

EXPOSE ${fester.http.port}

#!/bin/bash

# Go to the savant directory:
cd /usr/local/savant-${project.version}/

# Prefer to use IPv4:
#   -Djava.net.preferIPv4Stack=true
# Set up the proxy:
#   -Dhttp.proxyHost=<proxy.host.name> -Dhttp.proxyPort=<###> "-Dhttp.nonProxyHosts=127.0.0.1|<local host ip>|<server ip>|<server hostname>
java -Xmx4g -jar savant-core-*.jar

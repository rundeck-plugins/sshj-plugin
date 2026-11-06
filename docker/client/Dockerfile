FROM node:24.22.0@sha256:925de64a85319052d6971ae81ef2417da956e8a3a2c4a0fac732327970f017f5

RUN  apt-get update && apt-get upgrade -y -qq && \
     apt-get install -y -qq --no-install-recommends wget git curl jq

RUN mkdir /rundeck-cli
COPY rundeck-cli /rundeck-cli
COPY project /rundeck-cli/projects
COPY import.yml /rundeck-cli
COPY acls /rundeck-cli/acls

WORKDIR /rundeck-cli

COPY scripts ./scripts
RUN chmod +x ./scripts/init.sh

RUN npm install

CMD ./scripts/init.sh

FROM node:12.22.12

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

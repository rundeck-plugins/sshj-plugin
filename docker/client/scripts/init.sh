echo "Init Script Starting"
## Place Init Script Code here as needed.  This is run as part of DockerFile steps for Client node
ssh-keygen -q -t rsa -N '' -f /rundeck-cli/data/keys/id_rsa

chmod +x bin/cli

echo "Data Dir"
ls -last /rundeck-cli/data
echo "-----"

./bin/cli load --rundeck_url $RUNDECK_URL --username $RUNDECK_USER  --password $RUNDECK_PASSWORD --config_file "$CONFIG_FILE" --path /rundeck-cli
./bin/cli updateProject --rundeck_url $RUNDECK_URL --username $RUNDECK_USER  --password $RUNDECK_PASSWORD --config_file "$CONFIG_FILE" --path /rundeck-cli
./bin/cli addUsers --rundeck_url $RUNDECK_URL --username $RUNDECK_USER  --password $RUNDECK_PASSWORD --config_file "$CONFIG_FILE" --path /rundeck-cli

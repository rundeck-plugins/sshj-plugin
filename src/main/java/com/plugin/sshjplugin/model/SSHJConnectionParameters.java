package com.plugin.sshjplugin.model;

import com.dtolabs.rundeck.core.Constants;
import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.dispatcher.DataContextUtils;
import com.dtolabs.rundeck.core.execution.ExecutionContext;
import com.plugin.sshjplugin.SSHJNodeExecutorPlugin;
import com.plugin.sshjplugin.util.PropertyResolver;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.regex.Pattern;

public class SSHJConnectionParameters implements SSHJConnection{

    private INodeEntry node;
    private Framework framework;
    private ExecutionContext context;
    private PropertyResolver propertyResolver;

    public SSHJConnectionParameters(INodeEntry node, ExecutionContext context) {
        this.node = node;
        this.context = context;
        this.framework = context.getFramework();

        propertyResolver = new PropertyResolver(node, framework, context);
    }

    @Override
    public AuthenticationType getAuthenticationType() {
        String authType = propertyResolver.resolve(SSHJNodeExecutorPlugin.NODE_ATTR_SSH_AUTHENTICATION);
        if (null != authType) {
            return AuthenticationType.valueOf(authType);
        }
        return AuthenticationType.privateKey;
    }

    @Override
    public String getPrivateKeyPath() throws IOException {
        String privateKeyFile;
        InputStream sshKey = propertyResolver.getPrivateKeyStorageFromProperty(SSHJNodeExecutorPlugin.NODE_ATTR_SSH_KEY_RESOURCE);

        if(sshKey != null){

            File tempFile = File.createTempFile("tmp", "key");
            tempFile.deleteOnExit();
            FileOutputStream tmpKey = new FileOutputStream(tempFile);
            org.apache.commons.io.IOUtils.copy(sshKey, tmpKey);

            String sshKeyResource = "";

            context.getExecutionListener().log(3, "[sshj-debug] Using ssh key storage path: " + sshKeyResource);
            context.getExecutionListener().log(3, "[sshj-debug] Loading key from storage path: " + tempFile.getAbsolutePath());
            privateKeyFile = tempFile.getAbsolutePath();

        }else{

            privateKeyFile = getPrivateKeyFilePath();
            context.getExecutionListener().log(3, "[sshj-debug] Using ssh keyfile: " + privateKeyFile);
        }


        return privateKeyFile;
    }

    @Override
    public String getPrivateKeyStoragePath(){
        String path = propertyResolver.resolve(SSHJNodeExecutorPlugin.NODE_ATTR_SSH_KEY_RESOURCE);
        if (path == null && framework.hasProperty(Constants.SSH_KEYRESOURCE_PROP)) {
            //return default framework level
            path = framework.getProperty(Constants.SSH_KEYRESOURCE_PROP);
        }
        //expand properties in path
        if (path != null && path.contains("${")) {
            path = DataContextUtils.replaceDataReferencesInString(path, context.getDataContext());
        }
        return path;
    }

    @Override
    public InputStream getPrivateKeyStorageData(String path){
        try {
            return propertyResolver.getPrivateKeyStorageData(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public String getPrivateKeyStorage(String path) throws IOException {
        return propertyResolver.getPrivateKeyStorage(path);
    }

    @Override
    public String getPrivateKeyFilePath() {
        String path = propertyResolver.resolve(SSHJNodeExecutorPlugin.NODE_ATTR_SSH_KEYPATH);
        if (path == null && framework.hasProperty(Constants.SSH_KEYPATH_PROP)) {
            //return default framework level
            path = framework.getProperty(Constants.SSH_KEYPATH_PROP);
        }
        //expand properties in path
        if (path != null && path.contains("${")) {
            path = DataContextUtils.replaceDataReferencesInString(path, context.getDataContext());
        }
        return path;
    }

    @Override
    public String getPasswordStoragePath() {
        return propertyResolver.getStoragePath(SSHJNodeExecutorPlugin.NODE_ATTR_SSH_PASSWORD_STORAGE_PATH);
    }

    @Override
    public String getPassword(String path) throws IOException {
        String secureOption = getSecureOption(SSHJNodeExecutorPlugin.NODE_ATTR_SSH_PASSWORD_OPTION, SSHJNodeExecutorPlugin.DEFAULT_SSH_PASSWORD_OPTION);

        if(secureOption!=null){
            return secureOption;
        }

        if(path != null){
            return propertyResolver.getPasswordFromPath(path);
        }

        return null;
    }

    public String getSecureOption(String property, String defaultpProperty) {
        String opt = propertyResolver.resolve(property);
        if (null != opt) {
            return propertyResolver.evaluateSecureOption(opt, context);
        } else {
            return propertyResolver.evaluateSecureOption(defaultpProperty, context);
        }
    }

    @Override
    public String getSudoPasswordStoragePath() {
        return propertyResolver.getStoragePath(SSHJNodeExecutorPlugin.NODE_ATTR_SUDO_PASSWORD_STORAGE_PATH);
    }

    @Override
    public String getSudoPassword(String path) throws IOException {
        String secureOption = getSecureOption(SSHJNodeExecutorPlugin.NODE_ATTR_SUDO_PASSWORD_OPTION, SSHJNodeExecutorPlugin.DEFAULT_SUDO_PASSWORD_OPTION);

        if(secureOption!=null){
            return secureOption;
        }

        if(path != null){
            return propertyResolver.getPasswordFromPath(path);
        }

        return null;
    }

    @Override
    public String getPrivateKeyPassphraseStoragePath() {
        return propertyResolver.getStoragePath(SSHJNodeExecutorPlugin.NODE_ATTR_SSH_KEY_PASSPHRASE_STORAGE_PATH);
    }

    @Override
    public String getPrivateKeyPassphrase(String path) throws IOException {
        String secureOption = getSecureOption(SSHJNodeExecutorPlugin.NODE_ATTR_SSH_KEY_PASSPHRASE_OPTION, SSHJNodeExecutorPlugin.DEFAULT_SSH_KEY_PASSPHRASE_OPTION);

        if(secureOption!=null){
            return secureOption;
        }

        if(path != null){
            return propertyResolver.getPasswordFromPath(path);
        }

        return null;
    }

    @Override
    public Boolean isSudoEnabled() {
        boolean sudoEnabled = propertyResolver.resolveBoolean(SSHJNodeExecutorPlugin.NODE_ATTR_SUDO_COMMAND_ENABLED);
        return sudoEnabled;
    }

    @Override
    public String getSudoCommandPattern() {
        return propertyResolver.resolve(SSHJNodeExecutorPlugin.NODE_ATTR_SUDO_COMMAND_PATTERN, SSHJNodeExecutorPlugin.DEFAULT_SUDO_COMMAND_PATTERN);
    }

    @Override
    public String getSudoPromptPattern() {
        return propertyResolver.resolve(SSHJNodeExecutorPlugin.NODE_ATTR_SUDO_PROMPT_PATTERN, SSHJNodeExecutorPlugin.DEFAULT_SUDO_PROMPT_PATTERN);
    }


    public boolean matchesCommandPattern(final String command) {
        final String sudoCommandPattern1 = getSudoCommandPattern();
        if (null != sudoCommandPattern1) {
            boolean match = Pattern.compile(sudoCommandPattern1).matcher(command).matches();
            return match;
        } else {
            return false;
        }
    }

    @Override
    public int getKeepAliveInterval() {

        return propertyResolver.resolveLongFwk(
                SSHJNodeExecutorPlugin.NODE_ATTR_SSH_KEEP_ALIVE,
                SSHJNodeExecutorPlugin.FWK_PROP_SSH_KEEP_ALIVE,
                0).intValue();

    }

    @Override
    public int getRetryCounter() {
        return propertyResolver.resolveLongFwk(
                SSHJNodeExecutorPlugin.NODE_ATTR_RETRY_COUNTER,
                SSHJNodeExecutorPlugin.FWK_PROP_RETRY_COUNTER,
                0).intValue();

    }

    @Override
    public boolean isRetryEnabled() {
        return propertyResolver.resolveBoolean(SSHJNodeExecutorPlugin.NODE_ATTR_RETRY_ENABLE);
    }

    @Override
    public int getCommandTimeout() {
        return propertyResolver.resolveLongFwk(
                SSHJNodeExecutorPlugin.NODE_ATTR_SSH_COMMAND_TIMEOUT_PROP,
                SSHJNodeExecutorPlugin.FRAMEWORK_SSH_COMMAND_TIMEOUT_PROP,
                0
        ).intValue();
    }

    @Override
    public int getConnectTimeout() {
        return propertyResolver.resolveLongFwk(
                SSHJNodeExecutorPlugin.NODE_ATTR_SSH_CONNECT_TIMEOUT_PROP,
                SSHJNodeExecutorPlugin.FRAMEWORK_SSH_CONNECT_TIMEOUT_PROP,
                0
        ).intValue();
    }

    @Override
    public String getUsername() {
        String user;
        if (null != propertyResolver.nonBlank(node.getUsername()) || node.containsUserName()) {
            user = propertyResolver.nonBlank(node.extractUserName());
        } else if (propertyResolver.hasProperty(SSHJNodeExecutorPlugin.PROJECT_SSH_USER)
                && null != propertyResolver.nonBlank(propertyResolver.getProperty(SSHJNodeExecutorPlugin.PROJECT_SSH_USER))) {
            user = propertyResolver.nonBlank(propertyResolver.getProperty(SSHJNodeExecutorPlugin.PROJECT_SSH_USER));
        } else {
            user = propertyResolver.nonBlank(propertyResolver.getProperty(Constants.SSH_USER_PROP));
        }
        if (null != user && user.contains("${")) {
            return DataContextUtils.replaceDataReferencesInString(user, context.getDataContext());
        }
        return user;
    }

    @Override
    public Boolean getLocalSSHAgent() {
        return null;
    }

    @Override
    public Integer getTtlSSHAgent() {
        return null;
    }

    @Override
    public Map<String, String> getSshConfig() {
        return null;
    }

    @Override
    public String getBindAddress() {
        return null;
    }






}

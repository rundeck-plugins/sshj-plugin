package com.plugin.sshjplugin;

import com.dtolabs.rundeck.core.Constants;
import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.execution.ExecutionContext;
import com.dtolabs.rundeck.core.execution.impl.common.BaseFileCopier;
import com.dtolabs.rundeck.core.execution.proxy.ProxySecretBundleCreator;
import com.dtolabs.rundeck.core.execution.proxy.SecretBundle;
import com.dtolabs.rundeck.core.execution.script.ScriptfileUtils;
import com.dtolabs.rundeck.core.execution.service.FileCopierException;
import com.dtolabs.rundeck.core.execution.service.MultiFileCopier;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepFailureReason;
import com.dtolabs.rundeck.core.plugins.Plugin;
import com.dtolabs.rundeck.core.plugins.configuration.Describable;
import com.dtolabs.rundeck.core.plugins.configuration.Description;
import com.dtolabs.rundeck.plugins.PluginLogger;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription;
import com.dtolabs.rundeck.plugins.util.DescriptionBuilder;
import com.plugin.sshjplugin.model.SSHJConnectionParameters;
import com.plugin.sshjplugin.model.SSHJScp;
import com.plugin.sshjplugin.util.SSHJSecretBundleUtil;
import net.schmizz.keepalive.KeepAliveProvider;
import net.schmizz.sshj.DefaultConfig;
import net.schmizz.sshj.SSHClient;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Plugin(service = ServiceNameConstants.FileCopier, name = SSHJFileCopierPlugin.SERVICE_PROVIDER_NAME)
@PluginDescription(title = SSHJFileCopierPlugin.SERVICE_TITLE, description = SSHJFileCopierPlugin.SERVICE_DESCRIPCION)
public class SSHJFileCopierPlugin extends BaseFileCopier implements MultiFileCopier, ProxySecretBundleCreator, Describable {

    public static final String SERVICE_TITLE = "SSHJ-SCP";
    public static final String SERVICE_DESCRIPCION = "File Copier using SSHJ library";

    public static final String SERVICE_PROVIDER_NAME = "sshj-scp";

    static final Description DESC = DescriptionBuilder.builder()
            .name(SERVICE_PROVIDER_NAME)
            .title(SERVICE_TITLE)
            .description("Copies a script or file to a remote node via either SCP or SFTP. Supports multiple forms of authentication.")
            .property(SSHJNodeExecutorPlugin.SSH_KEY_FILE_PROP)
            .property(SSHJNodeExecutorPlugin.SSH_KEY_STORAGE_PROP)
            .property(SSHJNodeExecutorPlugin.SSH_PASSWORD_STORAGE_PROP)
            .property(SSHJNodeExecutorPlugin.SSH_AUTH_TYPE_PROP)
            .property(SSHJNodeExecutorPlugin.SSH_PASSPHRASE_STORAGE_PROP)
            .property(SSHJNodeExecutorPlugin.SSH_KEEP_ALIVE_INTERVAL)
            .property(SSHJNodeExecutorPlugin.SSH_KEEP_ALIVE_MAX_ALIVE_COUNT)
            .property(SSHJNodeExecutorPlugin.SSH_RETRY_COUNTER)
            .property(SSHJNodeExecutorPlugin.USE_SFTP)


            .mapping(SSHJNodeExecutorPlugin.CONFIG_KEYPATH, SSHJNodeExecutorPlugin.PROJ_PROP_SSH_KEYPATH)
            .mapping(SSHJNodeExecutorPlugin.CONFIG_AUTHENTICATION, SSHJNodeExecutorPlugin.PROJ_PROP_SSH_AUTHENTICATION)
            .mapping(SSHJNodeExecutorPlugin.CONFIG_KEYSTORE_PATH, SSHJNodeExecutorPlugin.PROJ_PROP_SSH_KEY_RESOURCE)
            .mapping(SSHJNodeExecutorPlugin.CONFIG_PASSSTORE_PATH, SSHJNodeExecutorPlugin.PROJ_PROP_SSH_PASSWORD_STORAGE_PATH)
            .frameworkMapping(SSHJNodeExecutorPlugin.CONFIG_KEYSTORE_PATH, SSHJNodeExecutorPlugin.FWK_PROP_SSH_KEY_RESOURCE)
            .frameworkMapping(SSHJNodeExecutorPlugin.CONFIG_PASSSTORE_PATH, SSHJNodeExecutorPlugin.FWK_PROP_SSH_PASSWORD_STORAGE_PATH)
            .frameworkMapping(SSHJNodeExecutorPlugin.CONFIG_KEYPATH, SSHJNodeExecutorPlugin.FWK_PROP_SSH_KEYPATH)
            .frameworkMapping(SSHJNodeExecutorPlugin.CONFIG_AUTHENTICATION, SSHJNodeExecutorPlugin.FWK_PROP_SSH_AUTHENTICATION)
            .mapping(SSHJNodeExecutorPlugin.CONFIG_PASSPHRASE_STORE_PATH, SSHJNodeExecutorPlugin.PROJ_PROP_SSH_KEY_PASSPHRASE_STORAGE_PATH)
            .frameworkMapping(SSHJNodeExecutorPlugin.CONFIG_PASSPHRASE_STORE_PATH, SSHJNodeExecutorPlugin.FWK_PROP_SSH_KEY_PASSPHRASE_STORAGE_PATH)
            .mapping(SSHJNodeExecutorPlugin.CONFIG_KEEP_ALIVE_INTERVAL, SSHJNodeExecutorPlugin.PROJ_PROP_SSH_KEEP_ALIVE)
            .frameworkMapping(SSHJNodeExecutorPlugin.CONFIG_KEEP_ALIVE_INTERVAL, SSHJNodeExecutorPlugin.FWK_PROP_SSH_KEEP_ALIVE)
            .mapping(SSHJNodeExecutorPlugin.CONFIG_KEEP_ALIVE_MAX_ALIVE_COUNT, SSHJNodeExecutorPlugin.PROJ_PROP_SSH_KEEP_ALIVE_MAX_COUNT)
            .frameworkMapping(SSHJNodeExecutorPlugin.CONFIG_KEEP_ALIVE_MAX_ALIVE_COUNT, SSHJNodeExecutorPlugin.FWK_PROP_SSH_KEEP_ALIVE_MAX_ALIVE_COUNT)
            .mapping(SSHJNodeExecutorPlugin.CONFIG_RETRY_ENABLE, SSHJNodeExecutorPlugin.PROJ_PROP_RETRY_ENABLE)
            .frameworkMapping(SSHJNodeExecutorPlugin.CONFIG_RETRY_ENABLE, SSHJNodeExecutorPlugin.FWK_PROP_RETRY_ENABLE)
            .mapping(SSHJNodeExecutorPlugin.CONFIG_USE_SFTP, SSHJNodeExecutorPlugin.PROJ_PROP_USE_SFTP)
            .frameworkMapping(SSHJNodeExecutorPlugin.CONFIG_USE_SFTP, SSHJNodeExecutorPlugin.FWK_PROP_USE_SFTP)
            .mapping(SSHJNodeExecutorPlugin.CONFIG_RETRY_COUNTER, SSHJNodeExecutorPlugin.PROJ_PROP_RETRY_COUNTER)
            .frameworkMapping(SSHJNodeExecutorPlugin.CONFIG_RETRY_COUNTER, SSHJNodeExecutorPlugin.FWK_PROP_RETRY_COUNTER)
            .build();

    private SSHClient sshClient;

    public void setSshClient(SSHClient sshClient) {
        this.sshClient = sshClient;
    }

    @Override
    public Description getDescription() {
        return DESC;
    }

    @Override
    public String[] copyFiles(ExecutionContext context, File basedir, List<File> files, String remotePath, INodeEntry node) throws FileCopierException {
        return copyMultipleFiles(context, basedir, files, remotePath, node);
    }

    @Override
    public String copyFileStream(ExecutionContext context, InputStream input, INodeEntry node, String destination) throws FileCopierException {
        return copyFile(context, null, input, null, node, destination);
    }

    @Override
    public String copyFile(ExecutionContext context, File file, INodeEntry node, String destination) throws FileCopierException {
        return copyFile(context, file, null, null, node, destination);
    }

    @Override
    public String copyScriptContent(ExecutionContext context, String script, INodeEntry node, String destination) throws FileCopierException {
        return copyFile(context, null, null, script, node, destination);
    }

    private String copyFile(
            final ExecutionContext context,
            final File scriptfile,
            final InputStream input,
            final String script,
            final INodeEntry node,
            final String destinationPath
    ) throws FileCopierException {

        final String remotefile;
        final PluginLogger logger = context.getExecutionListener();

        if (null == destinationPath) {
            String identity = null != context.getDataContext() && null != context.getDataContext().get("job") ?
                    context.getDataContext().get("job").get("execid") : null;
            remotefile = generateRemoteFilepathForNode(
                    node,
                    context.getFramework().getFrameworkProjectMgr().getFrameworkProject(context.getFrameworkProject()),
                    context.getFramework(),
                    (null != scriptfile ? scriptfile.getName() : "dispatch-script"),
                    null,
                    identity
            );
        } else {
            remotefile = destinationPath;
        }
        //write to a local temp file or use the input file
        final File localTempfile =
                null != scriptfile ?
                        scriptfile :
                        writeTempFile(
                                context,
                                scriptfile,
                                input,
                                script
                        );

        SSHJScp scp;
        SSHJConnectionParameters connectionInfo = new SSHJConnectionParameters(node, context);

        try {
            if (null != scriptfile && scriptfile.isDirectory()) {
                logger.log(3, "[sshj-scp] copying folder");
                scp = SSHJBuilder.buildRecursiveScp(node, remotefile, localTempfile, connectionInfo, context.getExecutionListener());
            } else {
                logger.log(3, "[sshj-scp] copying file");
                scp = SSHJBuilder.buildScp(node, remotefile, localTempfile, connectionInfo, context.getExecutionListener());
            }

        } catch (Exception e) {
            throw new FileCopierException("Configuration error: " + e.getMessage(),
                    StepFailureReason.ConfigurationFailure, e);
        }

        if(sshClient ==null){
            final DefaultConfig config = SSHJDefaultConfig.init().getConfig();
            config.setKeepAliveProvider(KeepAliveProvider.KEEP_ALIVE);
            sshClient = new SSHClient(config);
        }
        scp.connect(sshClient);

        try {
            scp.execute(sshClient);
        } catch (Exception e) {
            throw new FileCopierException("Configuration error: " + e.getMessage(),
                    StepFailureReason.ConfigurationFailure, e);
        } finally {
            if (null == scriptfile) {
                if (!ScriptfileUtils.releaseTempFile(localTempfile)) {
                    context.getExecutionListener().log(
                            Constants.WARN_LEVEL,
                            "Unable to remove local temp file: " + localTempfile.getAbsolutePath()
                    );
                }
            }
        }

        try {
            sshClient.disconnect();
            sshClient.close();
        } catch (IOException iex) {
            throw new SSHJBuilder.BuilderException(iex);
        }

        return remotefile;
    }

    private String[] copyMultipleFiles(
            final ExecutionContext context,
            File basedir,
            List<File> files,
            String remotePath,
            final INodeEntry node
    ) throws FileCopierException {

        final PluginLogger logger = context.getExecutionListener();

        if(null==remotePath) {
            throw new FileCopierException("[sshj-scp] remotePath cant be null on multiple files",StepFailureReason.ConfigurationFailure);
        }

        final SSHJScp scp;
        final SSHJConnectionParameters connectionInfo = new SSHJConnectionParameters(node, context);


        try {
            scp = SSHJBuilder.buildMultiScp(node, basedir, files, remotePath, connectionInfo, logger);
        } catch (Exception e) {
            throw new FileCopierException("Configuration error: " + e.getMessage(),
                    StepFailureReason.ConfigurationFailure, e);
        }

        if(sshClient ==null){
            final DefaultConfig config = SSHJDefaultConfig.init().getConfig();
            config.setKeepAliveProvider(KeepAliveProvider.KEEP_ALIVE);
            sshClient = new SSHClient(config);
        }
        scp.connect(sshClient);

        try {
            scp.execute(sshClient);
        } catch (Exception e) {
            throw new FileCopierException("Configuration error: " + e.getMessage(),
                    StepFailureReason.ConfigurationFailure, e);
        }

        try {
            sshClient.disconnect();
            sshClient.close();
        } catch (IOException iex) {
            throw new SSHJBuilder.BuilderException(iex);
        }

        ArrayList<String> ret = new ArrayList<>();
        return ret.toArray(new String[0]);

    }

    @Override
    public SecretBundle prepareSecretBundle(ExecutionContext context, INodeEntry node) {
        return SSHJSecretBundleUtil.createBundle(context, node);
    }

    @Override
    public List<String> listSecretsPath(ExecutionContext context, INodeEntry node) {
        return SSHJSecretBundleUtil.getSecretsPath(context, node);
    }
}

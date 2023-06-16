package com.plugin.sshjplugin;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.execution.ExecutionContext;
import com.dtolabs.rundeck.core.execution.ExecutionListener;
import com.dtolabs.rundeck.core.execution.proxy.ProxySecretBundleCreator;
import com.dtolabs.rundeck.core.execution.proxy.SecretBundle;
import com.dtolabs.rundeck.core.execution.service.NodeExecutor;
import com.dtolabs.rundeck.core.execution.service.NodeExecutorResult;
import com.dtolabs.rundeck.core.execution.service.NodeExecutorResultImpl;
import com.dtolabs.rundeck.core.execution.workflow.steps.FailureReason;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepFailureReason;
import com.dtolabs.rundeck.core.plugins.Plugin;
import com.dtolabs.rundeck.core.plugins.configuration.*;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription;
import com.dtolabs.rundeck.plugins.util.DescriptionBuilder;
import com.dtolabs.rundeck.plugins.util.PropertyBuilder;
import com.plugin.sshjplugin.model.SSHJConnection;
import com.plugin.sshjplugin.model.SSHJConnectionParameters;
import com.plugin.sshjplugin.model.SSHJExec;
import com.plugin.sshjplugin.util.SSHJSecretBundleUtil;
import net.schmizz.keepalive.KeepAliveProvider;
import net.schmizz.sshj.DefaultConfig;
import net.schmizz.sshj.SSHClient;
import org.apache.commons.lang.StringUtils;

import java.util.Arrays;
import java.util.List;


@Plugin(service = ServiceNameConstants.NodeExecutor, name = SSHJNodeExecutorPlugin.SERVICE_PROVIDER_NAME)
@PluginDescription(title = SSHJNodeExecutorPlugin.SERVICE_TITLE, description = SSHJNodeExecutorPlugin.SERVICE_DESCRIPCION)
public class SSHJNodeExecutorPlugin implements NodeExecutor, ProxySecretBundleCreator, Describable {

    public static final String SERVICE_TITLE = "SSHJ-SSH";
    public static final String SERVICE_DESCRIPCION = "Node Executor using SSHJ library";

    public static final String SERVICE_PROVIDER_NAME = "sshj-ssh";

    public static final String CONFIG_PASSSTORE_PATH = "passwordstoragepath";
    public static final String CONFIG_KEYPATH = "keypath";
    public static final String CONFIG_KEYSTORE_PATH = "keystoragepath";
    public static final String CONFIG_AUTHENTICATION = "authentication";
    public static final String CONFIG_PASSPHRASE_STORE_PATH = "passphrasestoragepath";
    public static final String CONFIG_KEEP_ALIVE_INTERVAL = "keepAliveInterval";
    public static final String CONFIG_RETRY_ENABLE = "retryEnable";
    public static final String CONFIG_RETRY_COUNTER = "retryCounter";

    public static final String PROJ_PROP_PREFIX = "project.";
    public static final String FWK_PROP_PREFIX = "framework.";

    public static final String NODE_ATTR_SSH_KEYPATH = "ssh-keypath";
    public static final String NODE_ATTR_SSH_KEY_RESOURCE = "ssh-key-storage-path";
    public static final String NODE_ATTR_SSH_PASSWORD_STORAGE_PATH = "ssh-password-storage-path";
    public static final String NODE_ATTR_SSH_PASSWORD_OPTION = "ssh-password-option";
    public static final String NODE_ATTR_SSH_COMMAND_TIMEOUT_PROP = "ssh-command-timeout";
    public static final String NODE_ATTR_SSH_CONNECT_TIMEOUT_PROP = "ssh-connect-timeout";
    public static final String NODE_ATTR_SSH_AUTHENTICATION = "ssh-authentication";
    public static final String NODE_ATTR_SSH_KEY_PASSPHRASE_STORAGE_PATH = "ssh-key-passphrase-storage-path";
    public static final String NODE_ATTR_SUDO_COMMAND_ENABLED = "sudo-command-enabled";
    public static final String NODE_ATTR_SUDO_PASSWORD_STORAGE_PATH = "sudo-password-storage-path";
    public static final String NODE_ATTR_SUDO_COMMAND_PATTERN = "sudo-command-pattern";
    public static final String NODE_ATTR_SUDO_PROMPT_PATTERN = "sudo-prompt-pattern";
    public static final String NODE_ATTR_SSH_KEY_PASSPHRASE_OPTION = "ssh-key-passphrase-option";
    public static final String NODE_ATTR_SUDO_PASSWORD_OPTION = "password-option";
    public static final String NODE_ATTR_SSH_KEEP_ALIVE = "keep-alive-interval";
    public static final String NODE_ATTR_RETRY_COUNTER = "retry-counter";
    public static final String NODE_ATTR_RETRY_ENABLE = "retry-enable";

    public static final String PROJECT_SSH_USER = PROJ_PROP_PREFIX + "ssh.user";

    public static final String FWK_PROP_SSH_AUTHENTICATION = FWK_PROP_PREFIX + NODE_ATTR_SSH_AUTHENTICATION;
    public static final String PROJ_PROP_SSH_AUTHENTICATION = PROJ_PROP_PREFIX + NODE_ATTR_SSH_AUTHENTICATION;
    public static final String FWK_PROP_SSH_KEYPATH = FWK_PROP_PREFIX + NODE_ATTR_SSH_KEYPATH;
    public static final String PROJ_PROP_SSH_KEYPATH = PROJ_PROP_PREFIX + NODE_ATTR_SSH_KEYPATH;
    public static final String FWK_PROP_SSH_KEY_RESOURCE = FWK_PROP_PREFIX + NODE_ATTR_SSH_KEY_RESOURCE;
    public static final String PROJ_PROP_SSH_KEY_RESOURCE = PROJ_PROP_PREFIX + NODE_ATTR_SSH_KEY_RESOURCE;
    public static final String FWK_PROP_SSH_PASSWORD_STORAGE_PATH = FWK_PROP_PREFIX + NODE_ATTR_SSH_PASSWORD_STORAGE_PATH;
    public static final String PROJ_PROP_SSH_PASSWORD_STORAGE_PATH = PROJ_PROP_PREFIX + NODE_ATTR_SSH_PASSWORD_STORAGE_PATH;
    public static final String FWK_PROP_SSH_KEY_PASSPHRASE_STORAGE_PATH = FWK_PROP_PREFIX + NODE_ATTR_SSH_KEY_PASSPHRASE_STORAGE_PATH;
    public static final String PROJ_PROP_SSH_KEY_PASSPHRASE_STORAGE_PATH = PROJ_PROP_PREFIX + NODE_ATTR_SSH_KEY_PASSPHRASE_STORAGE_PATH;
    public static final String FWK_PROP_SSH_KEEP_ALIVE = FWK_PROP_PREFIX + NODE_ATTR_SSH_KEEP_ALIVE;
    public static final String PROJ_PROP_SSH_KEEP_ALIVE = PROJ_PROP_PREFIX + NODE_ATTR_SSH_KEEP_ALIVE;
    public static final String FWK_PROP_RETRY_COUNTER = FWK_PROP_PREFIX + NODE_ATTR_RETRY_COUNTER;
    public static final String PROJ_PROP_RETRY_COUNTER = PROJ_PROP_PREFIX + NODE_ATTR_RETRY_COUNTER;
    public static final String FWK_PROP_RETRY_ENABLE = FWK_PROP_PREFIX + NODE_ATTR_RETRY_ENABLE;
    public static final String PROJ_PROP_RETRY_ENABLE = PROJ_PROP_PREFIX + NODE_ATTR_RETRY_ENABLE;

    public static final String SUDO_OPT_PREFIX = "sudo-";

    public static final String DEFAULT_SUDO_PROMPT_PATTERN = "[sudo] password for";
    public static final String DEFAULT_SSH_PASSWORD_OPTION = "option.sshPassword";
    public static final String DEFAULT_SUDO_COMMAND_PATTERN = "^sudo\\s.*";
    public static final String DEFAULT_SSH_KEY_PASSPHRASE_OPTION = "option.sshKeyPassphrase";
    public static final String DEFAULT_SUDO_PASSWORD_OPTION = "option.sudoPassword";

    public static final String FRAMEWORK_SSH_COMMAND_TIMEOUT_PROP = "framework.ssh.command.timeout";
    public static final String FRAMEWORK_SSH_CONNECT_TIMEOUT_PROP = "framework.ssh.connect.timeout";

    public static final String COMMAND_TIMEOUT_MESSAGE =
            "Timeout period exceeded, connection dropped.";
    public static final String CON_TIMEOUT_MESSAGE =
            "Connection timeout.";

    public static final Property SSH_AUTH_TYPE_PROP = PropertyUtil.select(CONFIG_AUTHENTICATION, "SSH Authentication",
            "Type of SSH Authentication to use",
            true, SSHJConnection.AuthenticationType.privateKey.toString(), Arrays.asList(SSHJConnection.AuthenticationType.values()), null, null);

    static final Property SSH_KEY_FILE_PROP = PropertyUtil.string(CONFIG_KEYPATH, "SSH Key File path",
            "File Path to the SSH Key to use",
            false, null);

    static final Property SSH_KEY_STORAGE_PROP = PropertyBuilder.builder()
            .string(CONFIG_KEYSTORE_PATH)
            .required(false)
            .title("SSH Key Storage Path")
            .description("Path to the SSH Key to use within Rundeck Storage. E.g. \"keys/path/key1.pem\"")
            .renderingOption(StringRenderingConstants.SELECTION_ACCESSOR_KEY,
                    StringRenderingConstants.SelectionAccessor.STORAGE_PATH)
            .renderingOption(StringRenderingConstants.STORAGE_PATH_ROOT_KEY, "keys")
            .renderingOption(StringRenderingConstants.STORAGE_FILE_META_FILTER_KEY, "Rundeck-key-type=private")
            .build();

    static final Property SSH_PASSPHRASE_STORAGE_PROP = PropertyBuilder.builder()
            .string(CONFIG_PASSPHRASE_STORE_PATH)
            .required(false)
            .title("SSH Key Passphrase Storage Path")
            .description("Path to the key's Passphrase to use within Rundeck Storage. E.g. \"keys/path/my.password\". Can be overridden by a Node attribute named 'ssh-key-passphrase-storage-path'.")
            .renderingOption(StringRenderingConstants.SELECTION_ACCESSOR_KEY,
                    StringRenderingConstants.SelectionAccessor.STORAGE_PATH)
            .renderingOption(StringRenderingConstants.STORAGE_PATH_ROOT_KEY, "keys")
            .renderingOption(StringRenderingConstants.STORAGE_FILE_META_FILTER_KEY, "Rundeck-data-type=password")
            .build();

    static final Property SSH_PASSWORD_STORAGE_PROP = PropertyBuilder.builder()
            .string(CONFIG_PASSSTORE_PATH)
            .required(false)
            .title("SSH Password Storage Path")
            .description("Path to the Password to use within Rundeck Storage. E.g. \"keys/path/my.password\". Can be overridden by a Node attribute named 'ssh-password-storage-path'.")
            .renderingOption(StringRenderingConstants.SELECTION_ACCESSOR_KEY,
                    StringRenderingConstants.SelectionAccessor.STORAGE_PATH)
            .renderingOption(StringRenderingConstants.STORAGE_PATH_ROOT_KEY, "keys")
            .renderingOption(StringRenderingConstants.STORAGE_FILE_META_FILTER_KEY, "Rundeck-data-type=password")
            .build();

    static final Property SSH_KEEP_ALIVE_INTERVAL = PropertyUtil.string(CONFIG_KEEP_ALIVE_INTERVAL, "Keep Alive Interval",
            "Keep Alive Interval",
            false, null);

    static final Property SSH_RETRY_COUNTER = PropertyUtil.string(CONFIG_RETRY_COUNTER, "Number of retries",
            "Set retries limit in case the connection fail (just for Transport Exceptions)",
            false, "3");

    static final Property SSH_RETRY_ENABLE = PropertyUtil.bool(CONFIG_RETRY_ENABLE, "Enable retry on fail?",
            "Enable a connection retry when the connection fails",
            false, "false");

    private SSHClient sshClient;

    public void setSshClient(SSHClient sshClient) {
        this.sshClient = sshClient;
    }

    /**
     * Overriding this method gives the plugin a chance to take part in building the {@link
     * com.dtolabs.rundeck.core.plugins.configuration.Description} presented by this plugin.  This subclass can use the
     * {@link DescriptionBuilder} to modify all aspects of the description, add or remove properties, etc.
     */
    @Override
    public Description getDescription() {

        DescriptionBuilder builder = DescriptionBuilder.builder()
                .name(SERVICE_PROVIDER_NAME)
                .title(SERVICE_TITLE)
                .description(SSHJNodeExecutorPlugin.SERVICE_DESCRIPCION);

        builder.property(SSH_PASSWORD_STORAGE_PROP);
        builder.property(SSH_AUTH_TYPE_PROP);
        builder.property(SSH_KEY_FILE_PROP);
        builder.property(SSH_KEY_STORAGE_PROP);
        builder.property(SSH_PASSPHRASE_STORAGE_PROP);
        builder.property(SSH_KEEP_ALIVE_INTERVAL);
        builder.property(SSH_RETRY_ENABLE);
        builder.property(SSH_RETRY_COUNTER);

        //mapping config input on project and framework level
        builder.mapping(CONFIG_KEYPATH, PROJ_PROP_SSH_KEYPATH);
        builder.frameworkMapping(CONFIG_KEYPATH, FWK_PROP_SSH_KEYPATH);
        builder.mapping(CONFIG_KEYSTORE_PATH, PROJ_PROP_SSH_KEY_RESOURCE);
        builder.frameworkMapping(CONFIG_KEYSTORE_PATH, FWK_PROP_SSH_KEY_RESOURCE);
        builder.mapping(CONFIG_AUTHENTICATION, PROJ_PROP_SSH_AUTHENTICATION);
        builder.frameworkMapping(CONFIG_AUTHENTICATION, FWK_PROP_SSH_AUTHENTICATION);
        builder.mapping(CONFIG_PASSSTORE_PATH, PROJ_PROP_SSH_PASSWORD_STORAGE_PATH);
        builder.frameworkMapping(CONFIG_PASSSTORE_PATH, FWK_PROP_SSH_PASSWORD_STORAGE_PATH);
        builder.mapping(CONFIG_PASSPHRASE_STORE_PATH, PROJ_PROP_SSH_KEY_PASSPHRASE_STORAGE_PATH);
        builder.frameworkMapping(CONFIG_PASSPHRASE_STORE_PATH, FWK_PROP_SSH_KEY_PASSPHRASE_STORAGE_PATH);
        builder.mapping(CONFIG_KEEP_ALIVE_INTERVAL, PROJ_PROP_SSH_KEEP_ALIVE);
        builder.frameworkMapping(CONFIG_KEEP_ALIVE_INTERVAL, FWK_PROP_SSH_KEEP_ALIVE);

        builder.mapping(CONFIG_KEEP_ALIVE_INTERVAL, PROJ_PROP_SSH_KEEP_ALIVE);
        builder.frameworkMapping(CONFIG_KEEP_ALIVE_INTERVAL, FWK_PROP_SSH_KEEP_ALIVE);

        builder.mapping(CONFIG_RETRY_COUNTER, PROJ_PROP_RETRY_COUNTER);
        builder.frameworkMapping(CONFIG_RETRY_COUNTER, FWK_PROP_RETRY_COUNTER);
        builder.mapping(CONFIG_RETRY_ENABLE, PROJ_PROP_RETRY_ENABLE);
        builder.frameworkMapping(CONFIG_RETRY_ENABLE, FWK_PROP_RETRY_ENABLE);

        return builder.build();
    }

    @Override
    public NodeExecutorResult executeCommand(ExecutionContext context, String[] command, INodeEntry node) {

        if (null == node.getHostname() || null == node.extractHostname() || StringUtils.isBlank(node.extractHostname())) {
            return NodeExecutorResultImpl.createFailure(
                    StepFailureReason.ConfigurationFailure,
                    "Hostname must be set to connect to remote node '" + node.getNodename() + "'",
                    node
            );
        }
        boolean success = false;

        final ExecutionListener listener = context.getExecutionListener();

        SSHJConnectionParameters connectionInfo = new SSHJConnectionParameters(node, context);

        long contimeout = connectionInfo.getConnectTimeout();
        long commandtimeout = connectionInfo.getCommandTimeout();

        SSHJExec sshexec = null;
        try {
            sshexec = SSHJBuilder.build(node,
                    command,
                    context.getDataContext(),
                    connectionInfo,
                    listener);
        } catch (Exception e) {
            e.printStackTrace();
        }


        FailureReason failureReason = null;
        String errormsg = null;


        try {
            if(sshClient ==null){
                final DefaultConfig config = SSHJDefaultConfig.init().getConfig();
                config.setLoggerFactory(new SSHJPluginLoggerFactory(listener));
                config.setKeepAliveProvider(KeepAliveProvider.KEEP_ALIVE);
                sshClient = new SSHClient(config);
            }
            sshexec.connect(sshClient);
            sshexec.execute(sshClient);
            success = true;
        } catch (Exception e) {
            final ExtractFailure extractFailure = extractFailure(e, node, commandtimeout, contimeout, context.getFramework());
            errormsg = extractFailure.getErrormsg();
            failureReason = extractFailure.getReason();
            context.getExecutionListener().log(
                    3,
                    String.format(
                            "SSH command execution error: %s: %s",
                            failureReason,
                            errormsg
                    )
            );
        }finally {
            if(sshClient != null){
                try {
                    sshClient.disconnect();
                    sshClient.close();
                } catch (Exception iex) {
                    throw new SSHJBuilder.BuilderException(iex);
                }
            }

        }

        final int resultCode = sshexec.getExitStatus();
        if (null != context.getOutputContext()) {
            context.getOutputContext().addOutput("exec", "exitCode", String.valueOf(resultCode));
        }
        if (success) {
            return NodeExecutorResultImpl.createSuccess(node);
        } else {
            return NodeExecutorResultImpl.createFailure(failureReason, errormsg, node, resultCode);
        }

    }

    static ExtractFailure extractFailure(
            Exception e,
            INodeEntry node,
            long commandTimeout,
            long connectTimeout,
            Framework framework
    ) {
        String errormsg;
        FailureReason failureReason;

        failureReason = StepFailureReason.Unknown;
        errormsg = e.getMessage();
        /*
        if (e.getMessage().contains(COMMAND_TIMEOUT_MESSAGE)) {
            errormsg =
                    "Failed execution for node: " + node.getNodename() + ": Execution Timeout period exceeded (after "
                            + commandTimeout + "ms), connection dropped";
            failureReason = NodeStepFailureReason.ConnectionTimeout;
        } else if (e.getMessage().contains(CON_TIMEOUT_MESSAGE)) {
            errormsg =
                    "Failed execution for node: " + node.getNodename() + ": Connection Timeout period exceeded (after "
                            + connectTimeout + "ms).";
            failureReason = NodeStepFailureReason.ConnectionTimeout;
        } else if (e.getMessage().contains("Remote command failed with exit status")) {
            errormsg = e.getMessage();
            failureReason = NodeStepFailureReason.NonZeroResultCode;
        } else if (null != e.getCause() && e.getCause() instanceof InterruptedException) {
            failureReason = StepFailureReason.Interrupted;
            errormsg = "Connection was interrupted";
        } else {
            failureReason = StepFailureReason.Unknown;
            errormsg = e.getMessage();
        }

         */
        return new ExtractFailure(errormsg, failureReason);
    }

    @Override
    public SecretBundle prepareSecretBundle(ExecutionContext context, INodeEntry node) {
        return SSHJSecretBundleUtil.createBundle(context, node);
    }

    @Override
    public List<String> listSecretsPath(ExecutionContext context, INodeEntry node) {
        return SSHJSecretBundleUtil.getSecretsPath(context, node);
    }

    static class ExtractFailure {

        private String errormsg;
        private FailureReason reason;

        private ExtractFailure(String errormsg, FailureReason reason) {
            this.errormsg = errormsg;
            this.reason = reason;
        }

        public String getErrormsg() {
            return errormsg;
        }

        public FailureReason getReason() {
            return reason;
        }
    }



}

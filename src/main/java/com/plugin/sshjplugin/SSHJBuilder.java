package com.plugin.sshjplugin;

import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.dispatcher.DataContextUtils;
import com.dtolabs.rundeck.plugins.PluginLogger;
import com.plugin.sshjplugin.model.*;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SSHJBuilder {

    static SSHJExec build(final INodeEntry nodeentry,
                          final String[] args,
                          final Map<String, Map<String, String>> dataContext,
                          final SSHJConnection sshjConnectionParameters,
                          final PluginLogger logger) throws BuilderException {

        SSHJExec sshbase = new SSHJExec();
        final String commandString = StringUtils.join(args, " ");
        sshbase.setCommand(commandString);
        sshbase.setAllowPTY(sshjConnectionParameters.isAllocatePTY());
        configureSSHBase(nodeentry, sshjConnectionParameters, sshbase, logger);
        addEnvVars(sshbase, dataContext);

        return sshbase;
    }

    public static SSHJScp buildScp(final INodeEntry nodeentry,
                                   final String remotepath,
                                   final File sourceFile,
                                   final SSHJConnection sshjConnection,
                                   final PluginLogger logger) throws
            BuilderException {

        final String username = sshjConnection.getUsername();

        if (username == null) {
            throw new BuilderException("Username is required.");
        }


        final SSHJScp scp = new SSHJScp();
        logger.log(3, "["+scp.getPluginName()+"] username: " + username);

        buildScp(scp, nodeentry, remotepath, sourceFile, sshjConnection, logger);
        return scp;
    }

    public static SSHJScp buildRecursiveScp(final INodeEntry nodeentry,
                                            final String remotepath,
                                            final File sourceFile,
                                            final SSHJConnection sshConnection,
                                            final PluginLogger logger) throws
            BuilderException {


        final SSHJScp scp = new SSHJScp();
        buildRecursiveScp(scp, nodeentry, remotepath, sourceFile, sshConnection, logger);
        return scp;
    }

    static void buildRecursiveScp(final SSHJScp scp,
                                  final INodeEntry nodeentry,
                                  final String remotePath,
                                  final File sourceFolder,
                                  final SSHJConnection sshConnection,
                                  final PluginLogger logger) throws
            BuilderException {

        if (null == sourceFolder) {
            throw new BuilderException("sourceFolder was not set");
        }
        configureSSHBase(nodeentry, sshConnection, scp, logger);

        //Set the local and remote file paths
        //scp.setLocalFile(sourceFolder.getAbsolutePath());
        scp.addFile(sourceFolder);
        scp.setUseSftp(sshConnection.useSftp());
        scp.setTodir(remotePath);

    }

    static void buildScp(final SSHJScp scp,
                         final INodeEntry nodeentry,
                         final String remotepath,
                         final File sourceFile,
                         final SSHJConnection sshjConnection,
                         final PluginLogger logger) throws
            BuilderException {

        if (null == sourceFile) {
            throw new BuilderException("sourceFile was not set");
        }
        if (null == remotepath) {
            throw new BuilderException("remotePath was not set");
        }

        configureSSHBase(nodeentry, sshjConnection, scp, logger);

        scp.setUseSftp(sshjConnection.useSftp());
        //Set the local and remote file paths
        scp.setLocalFile(sourceFile.getAbsolutePath());
        scp.setRemoteTofile(remotepath);
    }

    public static SSHJScp buildMultiScp(
            final INodeEntry nodeentry,
            final File basedir,
            final List<File> files,
            final String remotePath,
            final SSHJConnection sshConnection, final PluginLogger logger
    ) throws
            Exception {


        final SSHJScp scp = new SSHJScp();
        buildMultiScp(scp, nodeentry, basedir, files, remotePath, sshConnection, logger);
        return scp;
    }

    static void buildMultiScp(
            final SSHJScp scp,
            final INodeEntry nodeentry,
            final File basedir,
            final List<File> files,
            final String remotePath,
            final SSHJConnection sshConnection,
            final PluginLogger logger
    ) throws
            Exception {

        if (null == files || files.size() == 0) {
            throw new Exception("files was not set");
        }

        if (null == remotePath) {
            throw new BuilderException("remotePath was not set");
        }

        scp.setRemoteTofile(remotePath);

        files.stream().forEach(file->{
            scp.addFile(file);
        });

        buildScp(scp, nodeentry, remotePath, basedir, sshConnection,logger);



    }

    private static void configureSSHBase(final INodeEntry nodeentry,
                                         final SSHJConnection sshConnection,
                                         final SSHJBase sshbase,
                                         final PluginLogger logger) throws
            BuilderException {

        String hostname = nodeentry.extractHostname();

        if (hostname == null) {
            throw new BuilderException("Host is required.");
        }

        Integer portNum = null;
        if (nodeentry.containsPort()) {
            try {
                portNum = Integer.parseInt(nodeentry.extractPort());
            } catch (NumberFormatException e) {
                throw new BuilderException("Port number is not valid: " + nodeentry.extractPort(), e);
            }
        }

        logger.log(3, "["+sshbase.getPluginName()+"] hostname: " + hostname);
        logger.log(3, "["+sshbase.getPluginName()+"] port: " + portNum);
        final String username = sshConnection.getUsername();

        if (username == null) {
            throw new BuilderException("Username is required.");
        }

        logger.log(3, "["+sshbase.getPluginName()+"] username: " + username);

        final SSHJConnection.AuthenticationType authenticationType = sshConnection.getAuthenticationType();
        if (null == authenticationType) {
            throw new BuilderException("SSH authentication type undetermined");
        }

        sshbase.setHostname(hostname);
        sshbase.setPort(portNum);
        sshbase.setSshjConnection(sshConnection);
        sshbase.setPluginLogger(logger);

    }

    public static class BuilderException extends RuntimeException {
        public BuilderException() {
        }

        public BuilderException(String s) {
            super(s);
        }

        public BuilderException(String s, Throwable throwable) {
            super(s, throwable);
        }

        public BuilderException(Throwable throwable) {
            super(throwable);
        }
    }


    public static void addEnvVars(final SSHJEnvironments sshexecTask, final Map<String, Map<String, String>> dataContext) {
        final Map<String, String> environment = DataContextUtils.generateEnvVarsFromContext(dataContext);
        if (null != environment) {
            for (final Map.Entry<String, String> entry : environment.entrySet()) {
                final String key = entry.getKey();
                if (null != key && null != entry.getValue()) {
                    final Map<String, String> env = new HashMap<>();
                    env.put(key, entry.getValue());
                    sshexecTask.addEnv(env);
                }
            }
        }
    }
}

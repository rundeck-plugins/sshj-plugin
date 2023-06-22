package com.plugin.sshjplugin.model;

import com.dtolabs.rundeck.plugins.PluginLogger;
import com.plugin.sshjplugin.SSHJAppendable;
import com.plugin.sshjplugin.SSHJBuilder;
import com.plugin.sshjplugin.SSHJPluginLoggerFactory;
import com.plugin.sshjplugin.sudo.SudoCommand;
import com.plugin.sshjplugin.sudo.SudoCommandBuilder;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.StreamCopier;
import net.schmizz.sshj.connection.ConnectionException;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.transport.TransportException;
import com.plugin.sshjplugin.util.DelegateOutputStream;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import net.schmizz.concurrent.Event;

public class SSHJExec extends SSHJBase implements SSHJEnvironments {

    private String command = null;
    private int exitStatus = -1;
    private Map<String, String> envVars = null;

    public void setCommand(String command) {
        this.command = command;
    }

    public void setPluginLogger(PluginLogger pluginLogger) {
        this.pluginLogger = pluginLogger;
    }

    @Override
    public void addEnv(Map env) {
        if (null == envVars) {
            envVars = new HashMap<>();
        }
        envVars.putAll(env);
    }

    public SSHJExec() {
        this.pluginName = "sshj-ssh";
    }

    public void execute(SSHClient ssh) {
        Session session = null;
        Session.Command cmd = null;
        try (DelegateOutputStream outputBuf = new DelegateOutputStream(System.out);
                DelegateOutputStream errBuf = new DelegateOutputStream(System.err)){
            pluginLogger.log(3, "["+getPluginName()+"]  starting session" );

            session = ssh.startSession();
            session.allocateDefaultPTY();

            pluginLogger.log(3, "["+getPluginName()+"] setting environments" );

            /* set env vars if any are embedded */
            if (null != envVars && envVars.size() > 0) {

                for (Map.Entry<String, String> entry : envVars.entrySet()) {
                    try {
                        pluginLogger.log(3, "["+getPluginName()+"] " + entry.getKey() + " => " + entry.getValue());
                        session.setEnvVar(entry.getKey(), entry.getValue());
                    } catch (ConnectionException e) {
                        pluginLogger.log(3, "["+getPluginName()+"] Env variable " + entry.getKey() + " cannot by set: " + e.getMessage());
                    } catch (TransportException e) {
                        pluginLogger.log(3, "["+getPluginName()+"] Env variable " + entry.getKey() + " cannot by set: " + e.getMessage());
                    }
                }
            }

            String scmd = command;
            pluginLogger.log(3, "["+getPluginName()+"]  executing command " + scmd );

            if (this.getSshjConnection().isSudoEnabled() && this.getSshjConnection().matchesCommandPattern(command)) {
                final Session.Shell shell = session.startShell();

                String sudoPasswordPath = this.getSshjConnection().getSudoPasswordStoragePath();

                if(sudoPasswordPath!=null){
                    pluginLogger.log(3, "["+getPluginName()+"]  running sudo with password path  " + sudoPasswordPath );
                }
                String sudoPassword = this.getSshjConnection().getSudoPassword(sudoPasswordPath);

                pluginLogger.log(3, "["+getPluginName()+"]  using sudoPassword value of: " + sudoPassword );

                SudoCommand sudoCommandRunner = new SudoCommandBuilder()
                                                    .sudoPromptPattern(this.getSshjConnection().getSudoPromptPattern())
                                                    .sudoPassword(sudoPassword)
                                                    .echoInput(System.out)
                                                    .echoOutput(new SSHJAppendable(pluginLogger, 3))
                                                    .errorStream(shell.getErrorStream())
                                                    .inputStream(shell.getInputStream())
                                                    .outputStream(shell.getOutputStream())
                                                    .logger(pluginLogger).build();

                String exitCodeStr = sudoCommandRunner.runSudoCommand(command);
                exitStatus = Integer.parseInt(exitCodeStr);

            } else {
                cmd = session.exec(scmd);
                pluginLogger.log(3, "["+getPluginName()+"]  capturing output" );

                SSHJPluginLoggerFactory sshjLogger = new SSHJPluginLoggerFactory(pluginLogger);

                Event<IOException> stdoutEvent = new StreamCopier(cmd.getInputStream(), outputBuf, sshjLogger)
                        .bufSize(cmd.getLocalMaxPacketSize())
                        .keepFlushing(true)
                        .spawn("stdout");
                Event<IOException> stderrEvent = new StreamCopier(cmd.getErrorStream(), errBuf, sshjLogger)
                        .bufSize(cmd.getLocalMaxPacketSize())
                        .keepFlushing(true)
                        .spawn("stderr");

                stderrEvent.await();
                stdoutEvent.await();
                cmd.join();
                exitStatus = cmd.getExitStatus();

            }

            pluginLogger.log(3, "["+getPluginName()+"] exit status: " + exitStatus);

            if (exitStatus != 0) {
                String msg = "Remote command failed with exit status " + exitStatus;
                throw new SSHJBuilder.BuilderException(msg);
            }
            pluginLogger.log(3, "["+getPluginName()+"] done" );

        } catch (IOException iex) {
            pluginLogger.log(0, iex.getMessage());
            throw new SSHJBuilder.BuilderException(iex);
        } finally {
            pluginLogger.log(3, "["+getPluginName()+"] closing session");

            if(cmd!=null){
                try {
                    cmd.getErrorStream().close();
                    cmd.getOutputStream().close();
                    cmd.close();

                } catch (Exception e) {
                    pluginLogger.log(3, "["+getPluginName()+"] error closing " + e.getMessage());
                }
            }

            if (session != null) {
                try {
                    session.close();
                } catch (Exception e) {
                    pluginLogger.log(3, "["+getPluginName()+"] error closing " + e.getMessage());
                }
            }

            pluginLogger.log(3, "["+getPluginName()+"] disconnected");

        }

    }

    public int getExitStatus() {
        return exitStatus;
    }


}

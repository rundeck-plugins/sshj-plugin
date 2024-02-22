package com.plugin.sshjplugin.model;

import com.dtolabs.rundeck.plugins.PluginLogger;
import com.plugin.sshjplugin.SSHJBuilder;
import com.plugin.sshjplugin.SSHJDefaultConfig;
import com.plugin.sshjplugin.SSHJPluginLoggerFactory;
import net.schmizz.keepalive.KeepAlive;
import net.schmizz.keepalive.KeepAliveProvider;
import net.schmizz.keepalive.KeepAliveRunner;
import net.schmizz.sshj.DefaultConfig;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.transport.TransportException;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;

import java.io.IOException;

public class SSHJBase {

    String pluginName;
    PluginLogger pluginLogger;
    int connectionNumber=0;
    int retryConnections=0;
    SSHJConnection sshjConnection;
    String hostname;
    Integer port;

    public String getPluginName() {
        return pluginName;
    }

    public SSHJConnection getSshjConnection() {
        return sshjConnection;
    }

    public void setSshjConnection(SSHJConnection sshjConnection) {
        this.sshjConnection = sshjConnection;
    }

    public PluginLogger getPluginLogger() {
        return pluginLogger;
    }

    public void setPluginLogger(PluginLogger pluginLogger) {
        this.pluginLogger = pluginLogger;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public void connect(SSHClient ssh){
        pluginLogger.log(3, "["+ getPluginName()+"] init SSHJDefaultConfig" );
        int connectTimeout = sshjConnection.getConnectTimeout();
        int commandTimeout = sshjConnection.getCommandTimeout();
        int keepAliveInterval = sshjConnection.getKeepAliveInterval();
        int keepAliveCount = sshjConnection.getKeepAliveMaxAlive();
        boolean retry = sshjConnection.isRetryEnabled();
        int retryCount = sshjConnection.getRetryCounter();
        boolean useSftp = sshjConnection.useSftp();

        SSHJAuthentication authentication = new SSHJAuthentication(sshjConnection, pluginLogger);

        pluginLogger.log(3, "["+getPluginName()+"] setting timeouts" );
        pluginLogger.log(3, "["+getPluginName()+"] getConnectTimeout timeout: " + connectTimeout);
        pluginLogger.log(3, "["+getPluginName()+"] getTimeout timeout: " + commandTimeout);
        pluginLogger.log(3, "["+getPluginName()+"] keepAliveInterval: " + keepAliveInterval);
        pluginLogger.log(3, "["+getPluginName()+"] keepAliveMaxCount: " + keepAliveCount);
        pluginLogger.log(3, "["+getPluginName()+"] retry: " + retry);
        pluginLogger.log(3, "["+getPluginName()+"] retryCount: " + retryCount);
        pluginLogger.log(3, "["+getPluginName()+"] useSftp: " + useSftp);

        ssh.getTransport().getConfig().setLoggerFactory(new SSHJPluginLoggerFactory(pluginLogger));
        ssh.addHostKeyVerifier(new PromiscuousVerifier());
        ssh.setConnectTimeout(connectTimeout);
        ssh.setTimeout(commandTimeout);
        ssh.getTransport().setTimeoutMs(connectTimeout);

        pluginLogger.log(3, "["+getPluginName()+"] adding loadKnownHosts" );

        try {
            ssh.loadKnownHosts();
        } catch (IOException e) {
            pluginLogger.log(3, "["+getPluginName()+"] " + e.getMessage());
        }

        int count=0;
        if(!retry){
            retryCount=0;
        }

        if (keepAliveInterval != 0) {
            KeepAliveRunner keepAlive = (KeepAliveRunner)ssh.getConnection().getKeepAlive();
            keepAlive.setKeepAliveInterval(keepAliveInterval);

            if(keepAliveCount != 0){
                keepAlive.setMaxAliveCount(keepAliveCount);
            }
        }



        while(count <= retryCount) {
            try {
                pluginLogger.log(3, "["+getPluginName()+"] open connection");

                if (port != null) {
                    ssh.connect(hostname, port.intValue());
                } else {
                    ssh.connect(hostname);
                }
                pluginLogger.log(3, "["+getPluginName()+"] connection done");

                authentication.authenticate(ssh);

                pluginLogger.log(3, "["+getPluginName()+"]  authentication set");


                if (ssh.isConnected()) {
                    pluginLogger.log(3, "["+getPluginName()+"] connection done");
                    connectionNumber++;
                }

            } catch (TransportException e) {
                pluginLogger.log(2, "["+getPluginName()+"] TransportException: " + e.getMessage());
                if(retry && count<=retryCount){
                    retryConnections++;
                    pluginLogger.log(2, "["+getPluginName()+"]  trying again");
                    pluginLogger.log(2, "["+getPluginName()+"]  total connections " + connectionNumber);
                    pluginLogger.log(2, "["+getPluginName()+"]  total retries " + retryConnections);
                }else{
                    throw new SSHJBuilder.BuilderException(e.getMessage());
                }
            } catch (IOException e) {
                pluginLogger.log(3, "["+getPluginName()+"] Connection fail: " + e.getMessage());
                throw new SSHJBuilder.BuilderException(e.getMessage());
            }

            count++;

        }
    }


}

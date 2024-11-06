package com.plugin.sshjplugin.model;

import com.dtolabs.rundeck.plugins.PluginLogger;
import com.plugin.sshjplugin.SSHJBuilder;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.xfer.FileSystemFile;
import net.schmizz.sshj.sftp.SFTPClient;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SSHJScp extends SSHJBase {

    private String toDir;
    private String remoteTofile;
    private List<File> fileSets;
    private String localFile;

    private SFTPClient sftp;

    private boolean useSftp;

    public void setTodir(final String aToUri) {
        this.toDir = aToUri;
    }

    public void setLocalFile(String absolutePath) {
        this.localFile = absolutePath;
    }

    public void setRemoteTofile(String s) {
        this.remoteTofile = s;
    }

    public void setUseSftp(boolean useSftp) {
        this.useSftp = useSftp;
    }

    public void addFile(final File set) {
        if (fileSets == null) {
            fileSets = new ArrayList<>();
        }
        fileSets.add(set);
    }

    public SSHJScp() {
        this.pluginName = "sshj-scp";
    }

    public void execute(SSHClient ssh) throws Exception {

        if(useSftp) {
            sftp = ssh.newSFTPClient();
        }

        pluginLogger.log(3, "["+this.getPluginName()+"] SSHJ File Copier");
        try {

            if (this.localFile != null && this.fileSets == null) {
                if (toDir != null && remoteTofile == null) {
                    pluginLogger.log(3, "["+getPluginName()+"] Copying file " + this.localFile + " to " + toDir);

                    if(useSftp) {
                        sftp.put(new FileSystemFile(this.localFile), toDir);
                    } else {
                        ssh.newSCPFileTransfer().upload(new FileSystemFile(this.localFile), toDir);
                    }
                } else {
                    pluginLogger.log(3, "["+getPluginName()+"] Copying file " + this.localFile + " to " + remoteTofile);

                    if(useSftp) {
                        sftp.put(new FileSystemFile(this.localFile), remoteTofile);
                    } else {
                        ssh.newSCPFileTransfer().upload(new FileSystemFile(this.localFile), remoteTofile);
                    }
                }

            } else if (this.fileSets != null && this.fileSets.size() > 0) {
                for(File file:this.fileSets) {
                    pluginLogger.log(3, "["+getPluginName()+"] Copying file " + file.getAbsolutePath() + " to " + toDir);
                    try {

                        if(useSftp) {
                            sftp.put(new FileSystemFile(file), toDir);
                        } else {
                            ssh.newSCPFileTransfer().upload(new FileSystemFile(file), toDir);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException iex) {
            iex.printStackTrace();
            pluginLogger.log(0, iex.getMessage());
            throw new Exception(iex);
        } finally {
            try {
                ssh.disconnect();
                ssh.close();
            } catch (IOException iex) {
                throw new Exception(iex);
            }
        }
    }


}

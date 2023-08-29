package com.plugin.sshjplugin.model;
import com.dtolabs.rundeck.plugins.PluginLogger;
import com.dtolabs.utils.Streams;
import com.plugin.sshjplugin.SSHJBuilder;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.Factory;
import net.schmizz.sshj.userauth.keyprovider.FileKeyProvider;
import net.schmizz.sshj.userauth.keyprovider.KeyFormat;
import net.schmizz.sshj.userauth.keyprovider.KeyProviderUtil;
import net.schmizz.sshj.userauth.password.PasswordUtils;

import java.io.*;

public class SSHJAuthentication {

    SSHJConnection.AuthenticationType authenticationType;
    String username;
    String password;
    String privateKeyContent;
    String passphrase;
    PluginLogger logger;
    SSHJConnection connectionParameters;

    public SSHJAuthentication(SSHJConnection connectionParameters, PluginLogger logger) {
        this.logger = logger;
        this.connectionParameters = connectionParameters;

        this.username = connectionParameters.getUsername();
        this.authenticationType = connectionParameters.getAuthenticationType();
    }

    void authenticate(final SSHClient ssh) throws IOException {

        switch (authenticationType) {
            case privateKey:
                logger.log(3, "Authenticating using private key");
                String privateKeyStoragePath = connectionParameters.getPrivateKeyStoragePath();
                String privateKeyFileSystemPath = connectionParameters.getPrivateKeyFilePath();
                String passphrasePath = connectionParameters.getPrivateKeyPassphraseStoragePath();
                FileKeyProvider keys = null;

                if(passphrasePath!=null){
                    try{
                        passphrase = connectionParameters.getPrivateKeyPassphrase(passphrasePath);
                    } catch (Exception e) {
                        throw new SSHJBuilder.BuilderException("Failed to read SSH Passphrase stored at path: " + passphrasePath);
                    }
                }
                if(privateKeyStoragePath!=null){
                    logger.log(3, "[sshj-debug] Using SSH Storage key: " + privateKeyStoragePath);
                    try{
                        privateKeyContent = connectionParameters.getPrivateKeyStorage(privateKeyStoragePath);
                    } catch (Exception e) {
                        throw new SSHJBuilder.BuilderException("Failed to read SSH Key Storage stored at path: " + privateKeyStoragePath);
                    }
                    KeyFormat format = KeyProviderUtil.detectKeyFileFormat(privateKeyContent,true);
                    keys = Factory.Named.Util.create(ssh.getTransport().getConfig().getFileKeyProviderFactories(), format.toString());
                    if (passphrase == null) {
                        keys.init(new StringReader(privateKeyContent), null);
                    } else {
                        logger.log(3, "[sshj-debug] Using Passphrase: " + passphrasePath);
                        keys.init(new StringReader(privateKeyContent), PasswordUtils.createOneOff(passphrase.toCharArray()));
                    }
                }
                if(privateKeyFileSystemPath!=null){
                    logger.log(3, "[sshj-debug] Using SSH Keyfile: " + privateKeyFileSystemPath);
                    if (passphrase == null) {
                        keys = (FileKeyProvider) ssh.loadKeys(privateKeyFileSystemPath);
                    } else {
                        keys = (FileKeyProvider) ssh.loadKeys(privateKeyFileSystemPath, passphrase);
                        logger.log(3, "[sshj-debug] Using Passphrase: " + passphrasePath);
                    }
                }
                ssh.authPublickey(username, keys);
                break;
            case password:
                String passwordPath = connectionParameters.getPasswordStoragePath();
                if(passwordPath!=null){
                    logger.log(3, "Authenticating using password: " + passwordPath);
                }
                try{
                    password = connectionParameters.getPassword(passwordPath);
                } catch (Exception e) {
                    throw new SSHJBuilder.BuilderException("Failed to read SSH Password stored at path: " + passwordPath);
                }

                ssh.authPassword(username, password);
                break;
        }
    }
}

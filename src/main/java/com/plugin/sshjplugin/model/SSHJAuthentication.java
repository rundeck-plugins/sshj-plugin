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

                String privateKeyPath = connectionParameters.getPrivateKeyStoragePath();
                try{
                    privateKeyContent = connectionParameters.getPrivateKeyStorage(privateKeyPath);
                } catch (Exception e) {
                    throw new SSHJBuilder.BuilderException("Failed to read SSH Key Storage stored at path: " + privateKeyPath);
                }

                String passphrasePath = connectionParameters.getPrivateKeyPassphraseStoragePath();
                try{
                    passphrase = connectionParameters.getPrivateKeyPassphrase(passphrasePath);
                } catch (Exception e) {
                    throw new SSHJBuilder.BuilderException("Failed to read SSH Passphrase stored at path: " + passphrasePath);
                }

                KeyFormat format = KeyProviderUtil.detectKeyFileFormat(privateKeyContent,true);
                FileKeyProvider keys = Factory.Named.Util.create(ssh.getTransport().getConfig().getFileKeyProviderFactories(), format.toString());

                logger.log(3, "[sshj-debug] Using ssh keyfile: " + privateKeyPath);

                if (passphrase == null) {
                    keys.init(new StringReader(privateKeyContent), null);
                } else {
                    logger.log(3, "[sshj-debug] Using Passphrase: " + passphrasePath);
                    keys.init(new StringReader(privateKeyContent), PasswordUtils.createOneOff(passphrase.toCharArray()));
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

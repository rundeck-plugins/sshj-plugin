package com.plugin.sshjplugin.model

import com.dtolabs.rundeck.plugins.PluginLogger
import com.hierynomus.sshj.userauth.keyprovider.OpenSSHKeyV1KeyFile
import net.schmizz.sshj.Config
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.common.Factory
import net.schmizz.sshj.transport.Transport
import net.schmizz.sshj.userauth.keyprovider.FileKeyProvider
import spock.lang.Specification
import net.schmizz.sshj.userauth.keyprovider.PKCS8KeyFile;
import net.schmizz.sshj.userauth.keyprovider.OpenSSHKeyFile;
import net.schmizz.sshj.userauth.keyprovider.PuTTYKeyFile


class SSHJAuthenticationTest extends Specification {

    def "authenticate with private key on filesystem no passphrase"() {
        given:
        SSHClient sshClient = Mock(SSHClient)
        PluginLogger pluginLogger = Mock(PluginLogger)
        SSHJConnection connectionParameters = Mock(SSHJConnection){
            1 * getAuthenticationType() >> SSHJConnection.AuthenticationType.privateKey
            1 * getPrivateKeyFilePath() >> "keys/rundeck/storage"
            0 * getPrivateKeyPassphrase(_)
            0 * getPrivateKeyStorage(_)
        }

        SSHJAuthentication auth = new SSHJAuthentication(connectionParameters,pluginLogger)

        when:
        auth.authenticate(sshClient)

        then:
        1 * sshClient.authPublickey(_, _)
        1 * sshClient.loadKeys(_)
        0 * sshClient.loadKeys(_, _)
    }

    def "authenticate with private key on filesystem with passphrase"() {
        given:
        String passphraseStoragePath = "keys/rundeck/storage/passphrase"

        SSHClient sshClient = Mock(SSHClient)
        PluginLogger pluginLogger = Mock(PluginLogger)
        SSHJConnection connectionParameters = Mock(SSHJConnection){
            1 * getAuthenticationType() >> SSHJConnection.AuthenticationType.privateKey
            1 * getPrivateKeyFilePath() >> "keys/rundeck/storage"
            1 * getPrivateKeyPassphraseStoragePath() >> passphraseStoragePath
            1 * getPrivateKeyPassphrase(passphraseStoragePath) >> "pass"
            0 * getPrivateKeyStorage(_)
        }
        SSHJAuthentication auth = new SSHJAuthentication(connectionParameters,pluginLogger)

        when:
        auth.authenticate(sshClient)

        then:
        0 * sshClient.loadKeys(_)
        1 * sshClient.authPublickey(_, _)
    }



   /* def "authenticate with private key Rundeck storage no passphrase"() {
        given:
        String keyStoragePath = "keys/rundeck/storage"
        SSHClient sshClient = Mock(SSHClient){
            getTransport() >> Mock(Transport){
                getConfig() >> Mock(Config){
                    getFileKeyProviderFactories() >> providerFactoriesList()
                }
            }
        }
        PluginLogger pluginLogger = Mock(PluginLogger)
        SSHJConnection connectionParameters = Mock(SSHJConnection){
            1 * getAuthenticationType() >> SSHJConnection.AuthenticationType.privateKey
            1 * getPrivateKeyStoragePath() >> keyStoragePath
            1 * getPrivateKeyStorage(keyStoragePath) >> "-----BEGIN OPENSSH PRIVATE KEY-----\n" +
                    "b3BlbnNzaC1rZXktdjEAAAAACmFlczI1Ni1jdHIAAAAGYmNyeXB0AAAAGAAAABCBHpyIOm\n" +
                    "Y45NDeuHxAzGCUAAAAEAAAAAEAAAAzAAAAC3NzaC1lZDI1NTE5AAAAIKUYWSH2YrYUH3IA\n" +
                    "t40IcM0ykM03oFRI7m+5jEK+fE4LAAAAoBIhOVxejwLYvIKIBgNQOe0j8h2nnz/+sEYUDc\n" +
                    "ug6KrlPxQ7kuL67It/Tb7IxAGzVWT3g3fkQMGNU/8uxRHAf5fQC9aYValFPr21g7I39OqR\n" +
                    "MbPXHnD8a+DwAw3ArakcZigzWqncuX5cuBgpr5+x/iXWAz0lAHJH1d5HaIsoy1K6VmMR+b\n" +
                    "GN7ixrjWwMVBM+Lv8DdRN5UnniX5grj6M8P0A=\n" +
                    "-----END OPENSSH PRIVATE KEY-----"
            0 * getPrivateKeyPassphrase(_)
            0 * getPrivateKeyStorage(_)
        }

        SSHJAuthentication auth = new SSHJAuthentication(connectionParameters,pluginLogger)
        OpenSSHKeyV1KeyFile

        when:
        auth.authenticate(sshClient)

        then:
        true
    }*/


    public static List <Factory.Named<FileKeyProvider>> providerFactoriesList() {
        List<Factory.Named<FileKeyProvider>> factories = new ArrayList<>();
        factories.add(OpenSSHKeyV1KeyFile);
        factories.add(PKCS8KeyFile);
        factories.add(OpenSSHKeyFile);
        factories.add(PuTTYKeyFile);

        return factories;
    }


    }


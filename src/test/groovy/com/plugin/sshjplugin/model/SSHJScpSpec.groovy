package com.plugin.sshjplugin.model

import com.dtolabs.rundeck.plugins.PluginLogger
import com.plugin.sshjplugin.SSHJBuilder
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.sftp.SFTPClient
import spock.lang.Specification

import java.io.File
import java.io.IOException

class SSHJScpSpec extends Specification {

    def "should close SFTP client in finally block"() {
        given:
        PluginLogger logger = Mock(PluginLogger)
        SSHJConnection connection = Mock(SSHJConnection) {
            getConnectTimeout() >> 5000
            getCommandTimeout() >> 30000
            getKeepAliveInterval() >> 0
            getKeepAliveMaxAlive() >> 0
            isRetryEnabled() >> false
            getRetryCounter() >> 0
            useSftp() >> true
        }
        
        SSHJScp scp = new SSHJScp()
        scp.pluginLogger = logger
        scp.pluginName = "sshj-scp"
        scp.setUseSftp(true)
        scp.sshjConnection = connection
        
        File testFile = File.createTempFile("test", ".txt")
        testFile.write("test content")
        scp.setLocalFile(testFile.getAbsolutePath())
        scp.setRemoteTofile("/tmp/remote.txt")
        
        SFTPClient sftp = Mock(SFTPClient)
        SSHClient ssh = Mock(SSHClient) {
            newSFTPClient() >> sftp
            disconnect() >> {}
            close() >> {}
        }
        _ * logger.log(_, _)  // Allow any log calls

        when:
        scp.execute(ssh)

        then:
        1 * sftp.put(_, "/tmp/remote.txt")
        1 * sftp.close()  // Verify SFTP client is closed
        1 * ssh.disconnect()
        1 * ssh.close()
        
        cleanup:
        testFile?.delete()
    }

    def "should preserve exception cause when file copy fails"() {
        given:
        PluginLogger logger = Mock(PluginLogger)
        SSHJConnection connection = Mock(SSHJConnection) {
            getConnectTimeout() >> 5000
            getCommandTimeout() >> 30000
            getKeepAliveInterval() >> 0
            getKeepAliveMaxAlive() >> 0
            isRetryEnabled() >> false
            getRetryCounter() >> 0
            useSftp() >> true
        }
        
        SSHJScp scp = new SSHJScp()
        scp.pluginLogger = logger
        scp.pluginName = "sshj-scp"
        scp.setUseSftp(true)
        scp.sshjConnection = connection
        
        File testFile = File.createTempFile("test", ".txt")
        testFile.write("test content")
        scp.setLocalFile(testFile.getAbsolutePath())
        scp.setRemoteTofile("/tmp/remote.txt")
        
        IOException copyException = new IOException("Permission denied")
        SFTPClient sftp = Mock(SFTPClient) {
            put(_, "/tmp/remote.txt") >> { throw copyException }
        }
        SSHClient ssh = Mock(SSHClient) {
            newSFTPClient() >> sftp
            disconnect() >> {}
            close() >> {}
        }
        _ * logger.log(_, _)  // Allow any log calls

        when:
        scp.execute(ssh)

        then:
        def exception = thrown(SSHJBuilder.BuilderException)
        exception.message.contains("File copy operation failed")
        exception.cause == copyException
        1 * sftp.close()  // Should still close SFTP client even on error
        1 * ssh.disconnect()
        1 * ssh.close()
        
        cleanup:
        testFile?.delete()
    }

    def "should log cleanup errors instead of throwing"() {
        given:
        PluginLogger logger = Mock(PluginLogger)
        SSHJConnection connection = Mock(SSHJConnection) {
            getConnectTimeout() >> 5000
            getCommandTimeout() >> 30000
            getKeepAliveInterval() >> 0
            getKeepAliveMaxAlive() >> 0
            isRetryEnabled() >> false
            getRetryCounter() >> 0
            useSftp() >> true
        }
        
        SSHJScp scp = new SSHJScp()
        scp.pluginLogger = logger
        scp.pluginName = "sshj-scp"
        scp.setUseSftp(true)
        scp.sshjConnection = connection
        
        File testFile = File.createTempFile("test", ".txt")
        testFile.write("test content")
        scp.setLocalFile(testFile.getAbsolutePath())
        scp.setRemoteTofile("/tmp/remote.txt")
        
        IOException cleanupException = new IOException("Cleanup failed")
        SFTPClient sftp = Mock(SFTPClient) {
            put(_, "/tmp/remote.txt") >> {}
            close() >> { throw cleanupException }
        }
        SSHClient ssh = Mock(SSHClient) {
            newSFTPClient() >> sftp
            disconnect() >> {}
            close() >> {}
        }
        _ * logger.log(_, _)  // Allow any log calls

        when:
        scp.execute(ssh)

        then:
        // Should not throw exception from cleanup - should log instead
        noExceptionThrown()
        1 * logger.log(2, { it.contains("Error closing SFTP client") && it.contains("Cleanup failed") })
        1 * ssh.disconnect()
        1 * ssh.close()
        
        cleanup:
        testFile?.delete()
    }
}

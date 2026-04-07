package com.plugin.sshjplugin.model

import com.dtolabs.rundeck.plugins.PluginLogger
import com.plugin.sshjplugin.SSHJBuilder
import net.schmizz.sshj.SSHClient
import spock.lang.Specification

import java.io.IOException

class SSHJExecSpec extends Specification {

    def "should preserve IOException cause in BuilderException"() {
        given:
        PluginLogger logger = Mock(PluginLogger)
        SSHJConnection connection = Mock(SSHJConnection) {
            getConnectTimeout() >> 5000
            getCommandTimeout() >> 30000
            getKeepAliveInterval() >> 0
            getKeepAliveMaxAlive() >> 0
            isRetryEnabled() >> false
            getRetryCounter() >> 0
            useSftp() >> false
            isSudoEnabled() >> false
        }
        
        SSHJExec exec = new SSHJExec()
        exec.pluginLogger = logger
        exec.pluginName = "sshj-ssh"
        exec.command = "test command"
        exec.sshjConnection = connection
        
        IOException ioException = new IOException("Connection lost")
        SSHClient ssh = Mock(SSHClient) {
            startSession() >> { throw ioException }
        }
        _ * logger.log(_, _)  // Allow any log calls

        when:
        exec.execute(ssh)

        then:
        def exception = thrown(SSHJBuilder.BuilderException)
        exception.message.contains("Command execution failed")
        exception.cause == ioException
        1 * logger.log(2, { it.contains("Command execution failed") && it.contains("Connection lost") && it.contains("IOException") })
    }
}

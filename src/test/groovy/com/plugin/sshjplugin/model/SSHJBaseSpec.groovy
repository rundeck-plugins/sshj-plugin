package com.plugin.sshjplugin.model

import com.dtolabs.rundeck.plugins.PluginLogger
import com.plugin.sshjplugin.SSHJBuilder
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.transport.Transport
import net.schmizz.sshj.transport.TransportException
import spock.lang.Specification

import java.net.UnknownHostException

class SSHJBaseSpec extends Specification {

    def "should handle UnknownHostException with proper error message and preserve cause"() {
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
            getUsername() >> "testuser"
            getAuthenticationType() >> SSHJConnection.AuthenticationType.password
            getPasswordStoragePath() >> null
            getPassword(_) >> "testpass"
        }
        
        SSHJBase base = new SSHJBase()
        base.pluginLogger = logger
        base.pluginName = "test-plugin"
        base.hostname = "nonexistent-host-12345"
        base.sshjConnection = connection
        
        UnknownHostException dnsException = new UnknownHostException("nonexistent-host-12345")
        SSHClient ssh = Mock(SSHClient) {
            getTransport() >> Mock(Transport) {
                getConfig() >> Mock(net.schmizz.sshj.Config) {
                    setLoggerFactory(_) >> {}
                }
                setTimeoutMs(_) >> {}
            }
            addHostKeyVerifier(_) >> {}
            setConnectTimeout(_) >> {}
            setTimeout(_) >> {}
            loadKnownHosts() >> {}
            connect("nonexistent-host-12345") >> { throw dnsException }
        }
        _ * logger.log(_, _)  // Allow any log calls

        when:
        base.connect(ssh)

        then:
        def exception = thrown(SSHJBuilder.BuilderException)
        exception.message.contains("DNS resolution failed for hostname: nonexistent-host-12345")
        exception.cause == dnsException
        1 * logger.log(2, { it.contains("DNS resolution FAILED") && it.contains("nonexistent-host-12345") })
    }

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
            getUsername() >> "testuser"
            getAuthenticationType() >> SSHJConnection.AuthenticationType.password
            getPasswordStoragePath() >> null
            getPassword(_) >> "testpass"
        }
        
        SSHJBase base = new SSHJBase()
        base.pluginLogger = logger
        base.pluginName = "test-plugin"
        base.hostname = "test-host"
        base.sshjConnection = connection
        
        IOException ioEx = new IOException("Network unreachable")
        SSHClient ssh = Mock(SSHClient) {
            getTransport() >> Mock(Transport) {
                getConfig() >> Mock(net.schmizz.sshj.Config) {
                    setLoggerFactory(_) >> {}
                }
                setTimeoutMs(_) >> {}
            }
            addHostKeyVerifier(_) >> {}
            setConnectTimeout(_) >> {}
            setTimeout(_) >> {}
            loadKnownHosts() >> {}
            connect("test-host") >> { throw ioEx }
        }
        _ * logger.log(_, _)  // Allow any log calls

        when:
        base.connect(ssh)

        then:
        def exception = thrown(SSHJBuilder.BuilderException)
        exception.message.contains("Connection failed: Network unreachable")
        exception.cause == ioEx
        1 * logger.log(2, { it.contains("Connection failed") && it.contains("Network unreachable") && it.contains("IOException") })
    }
}

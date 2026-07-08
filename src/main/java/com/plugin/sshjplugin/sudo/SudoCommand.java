package com.plugin.sshjplugin.sudo;

import com.dtolabs.rundeck.plugins.PluginLogger;
import net.sf.expectit.Expect;
import net.sf.expectit.ExpectBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static net.sf.expectit.filter.Filters.removeColors;
import static net.sf.expectit.filter.Filters.removeNonPrintable;
import static net.sf.expectit.matcher.Matchers.*;
import static net.sf.expectit.matcher.Matchers.contains;

public class SudoCommand {
    private OutputStream outputStream;
    private InputStream inputStream;
    private InputStream errorStream;
    private Appendable echoInput;
    private Appendable echoOutput;
    private String sudoPromptPattern;
    private String sudoPassword;
    private PluginLogger logger;

    // Matches a shell prompt ending in dollar sign (regular user) or hash (root),
    // optionally followed by trailing whitespace. The previous pattern "~.*\\$"
    // assumed every prompt contains a literal '~' (e.g. "user@host:~$"), which is
    // not true for custom PS1 values or root shells, causing the initial expect()
    // to never match and block until the (misconfigured) timeout elapsed.
    static final String PROMPT_PATTERN = ".*[#$]\\s*";

    // Matches a leaked ANSI/VT100 control sequence remnant (e.g. a bracketed-paste
    // mode toggle). expectit's removeNonPrintable() filter only strips the leading
    // ESC control byte, leaving the remaining printable characters glued to real
    // command output and corrupting the captured exit code.
    static final Pattern ANSI_ESCAPE_REMNANT = Pattern.compile("\\x1B?\\[\\??\\d+(;\\d+)*[a-zA-Z]");

    /**
     * Strips leaked ANSI control-sequence remnants from a captured exit-code response.
     */
    static String sanitizeExitCode(String raw) {
        return ANSI_ESCAPE_REMNANT.matcher(raw).replaceAll("").trim();
    }

    /**
     * True if the sanitized response is a plain integer, as a shell exit code must be.
     */
    static boolean isValidExitCode(String sanitized) {
        return sanitized.matches("-?\\d+");
    }

    public void setOutputStream(OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    public void setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    public void setErrorStream(InputStream errorStream) {
        this.errorStream = errorStream;
    }

    public void setEchoInput(Appendable echoInput) {
        this.echoInput = echoInput;
    }

    public void setEchoOutput(Appendable echoOutput) {
        this.echoOutput = echoOutput;
    }

    public String getSudoPromptPattern() {
        return sudoPromptPattern;
    }

    public void setSudoPromptPattern(String sudoPromptPattern) {
        this.sudoPromptPattern = sudoPromptPattern;
    }

    public void setSudoPassword(String password) {
        this.sudoPassword = password;
    }

    public void setLogger(PluginLogger logger) {
        this.logger = logger;
    }

    public String runSudoCommand(String command) throws IOException {

        Expect expect = new ExpectBuilder()
                .withOutput(outputStream)
                .withInputs(inputStream, errorStream)
                .withEchoInput(echoInput)
                .withEchoOutput(echoOutput)
                .withExceptionOnFailure()
                //.withEchoOutput(new SSHJAppendable(pluginLogger,3))
                //.withEchoInput(new SSHJAppendable(pluginLogger,2))
                .withInputFilters(removeColors(), removeNonPrintable())
                .withExceptionOnFailure()
                // Was "30000, TimeUnit.SECONDS" (~8.3 hours) - a unit typo that made
                // the sudo flow appear to hang indefinitely instead of failing fast
                // when a prompt/pattern never matched.
                .withTimeout(30000, TimeUnit.MILLISECONDS)
                .build();

        expect.expect(regexp(PROMPT_PATTERN));
        //expect.sendLine("stty -echo");
        //expect.interact();

        expect.sendLine(command);

        logger.log(3, "SUDO command enabled");
        logger.log(3, "sudo pattern :" + sudoPromptPattern);

        //expect.expect(matches(sudoCommand.getInputSuccessPattern()));
        expect.expect(contains(sudoPromptPattern));
        expect.sendLine(sudoPassword);

        expect.expect(regexp(PROMPT_PATTERN));
        expect.sendLine("echo $?");

        String rawExitCode = expect.expect(times(2, contains("\n")))
                .getResults()
                .get(1)
                .getBefore().trim();

        String exitCodeStr = sanitizeExitCode(rawExitCode);

        logger.log(3, "exit code: " + exitCodeStr);

        expect.close();

        if (!isValidExitCode(exitCodeStr)) {
            throw new IOException(
                    "Unable to determine sudo command exit code, unexpected response: '" + rawExitCode + "'"
            );
        }

        return exitCodeStr;

    }
}

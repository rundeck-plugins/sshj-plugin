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
    private long commandTimeoutMs;

    // Fail-fast timeout for the interactive prompt/password exchanges (initial
    // shell prompt, "[sudo] password" prompt, exit-code echo). These are all
    // near-instantaneous shell interactions, so 30s is ample and lets a broken
    // prompt-pattern match fail quickly instead of appearing to hang.
    private static final long PROMPT_TIMEOUT_MS = 30_000;

    // Matches a shell prompt ending in dollar sign (regular user) or hash (root),
    // optionally followed by trailing whitespace, anchored to the end of the
    // buffered output. The previous pattern "~.*\\$" assumed every prompt contains
    // a literal '~' (e.g. "user@host:~$"), which is not true for custom PS1 values
    // or root shells, causing the initial expect() to never match and block until
    // the (misconfigured) timeout elapsed. The end-of-input anchor is required so
    // this doesn't match a '$'/'#' appearing mid-output (e.g. "PATH=$PATH:...")
    // before the real prompt has been read.
    static final String PROMPT_PATTERN = ".*[#$]\\s*$";

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
     * True if the sanitized response actually parses as an int, as a shell exit
     * code must, so callers can safely call Integer.parseInt() afterwards. A plain
     * digit-regex check is not enough: an overly long digit string (e.g. from
     * further unstripped noise) matches "-?\d+" but still overflows int and throws
     * NumberFormatException downstream.
     */
    static boolean isValidExitCode(String sanitized) {
        try {
            Integer.parseInt(sanitized);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
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

    /**
     * @param commandTimeoutMs max time to wait for the sudo'd command itself to
     *                         finish and return to a shell prompt, in milliseconds.
     *                         0 (or less) means wait indefinitely, matching the
     *                         same semantics as the non-sudo ssh-command-timeout
     *                         node/project attribute.
     */
    public void setCommandTimeoutMs(long commandTimeoutMs) {
        this.commandTimeoutMs = commandTimeoutMs;
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
                // Was "30000, TimeUnit.SECONDS" (~8.3 hours) applied to every step
                // below, including the prompt/password exchanges - a unit typo that
                // made a broken prompt-pattern match hang for hours instead of
                // failing fast. This default now only governs those quick prompt
                // exchanges; the actual command-completion wait below gets its own,
                // configurable timeout so long-running sudo commands aren't cut off.
                .withTimeout(PROMPT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
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

        // Waiting for the shell prompt to return here means waiting for the sudo'd
        // command itself to finish, which can take far longer than a prompt
        // exchange - so this uses the user-configurable command timeout (same
        // ssh-command-timeout knob the non-sudo path honors) instead of the short
        // prompt-detection default above.
        Expect commandWait = commandTimeoutMs > 0
                ? expect.withTimeout(commandTimeoutMs, TimeUnit.MILLISECONDS)
                : expect.withInfiniteTimeout();
        commandWait.expect(regexp(PROMPT_PATTERN));
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

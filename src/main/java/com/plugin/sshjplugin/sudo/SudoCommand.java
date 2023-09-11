package com.plugin.sshjplugin.sudo;

import com.dtolabs.rundeck.plugins.PluginLogger;
import net.sf.expectit.Expect;
import net.sf.expectit.ExpectBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

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

    private final String PROMPT_PATTERN = "~.*\\$";

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

        logger.log(3, "Entered runSudoCommand");

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
                .withTimeout(30000, TimeUnit.SECONDS)
                .build();

        logger.log(3, "Looking for pattern :" + PROMPT_PATTERN);
        expect.expect(regexp(PROMPT_PATTERN));
        
        //expect.sendLine("stty -echo");
        //expect.interact();

        logger.log(3, "Issuing command :" + command);
        expect.sendLine(command);

        logger.log(3, "SUDO command enabled");
        logger.log(3, "sudo pattern :" + sudoPromptPattern);

        //expect.expect(matches(sudoCommand.getInputSuccessPattern()));
        expect.expect(contains(sudoPromptPattern));
        expect.sendLine(sudoPassword);

        logger.log(3, "Looking for pattern :" + PROMPT_PATTERN);
        expect.expect(regexp(PROMPT_PATTERN));
        expect.sendLine("echo $?");

        String exitCodeStr = expect.expect(times(2, contains("\n")))
                .getResults()
                .get(1)
                .getBefore().trim();

        logger.log(3, "exit code: " + exitCodeStr);

        expect.close();
        return exitCodeStr;

    }
}

package com.plugin.sshjplugin.sudo;

import com.dtolabs.rundeck.plugins.PluginLogger;

import java.io.InputStream;
import java.io.OutputStream;

public class SudoCommandBuilder {

    private OutputStream outputStream;
    private InputStream inputStream;
    private InputStream errorStream;
    private Appendable echoInput;
    private Appendable echoOutput;
    private String sudoPromptPattern;
    private String sudoPassword;
    private PluginLogger logger;
    private long commandTimeoutMs;

    public SudoCommandBuilder() {
    }

    public SudoCommandBuilder outputStream(OutputStream outputStream){
        this.outputStream = outputStream;
        return this;
    }

    public SudoCommandBuilder inputStream(InputStream inputStream){
        this.inputStream = inputStream;
        return this;
    }

    public SudoCommandBuilder errorStream(InputStream errorStream){
        this.errorStream = errorStream;
        return this;
    }

    public SudoCommandBuilder echoInput(Appendable echoInput){
        this.echoInput = echoInput;
        return this;
    }

    public SudoCommandBuilder echoOutput(Appendable echoOutput){
        this.echoOutput = echoOutput;
        return this;
    }

    public SudoCommandBuilder sudoPromptPattern(String sudoPromptPattern){
        this.sudoPromptPattern = sudoPromptPattern;
        return this;
    }

    public SudoCommandBuilder sudoPassword(String sudoPassword){
        this.sudoPassword = sudoPassword;
        return this;
    }

    public SudoCommandBuilder logger(PluginLogger logger){
        this.logger = logger;
        return this;
    }

    public SudoCommandBuilder commandTimeoutMs(long commandTimeoutMs){
        this.commandTimeoutMs = commandTimeoutMs;
        return this;
    }

    public SudoCommand build(){
        SudoCommand sudoCommand = new SudoCommand();
        sudoCommand.setEchoInput(echoInput);
        sudoCommand.setEchoOutput(echoOutput);
        sudoCommand.setErrorStream(errorStream);
        sudoCommand.setInputStream(inputStream);
        sudoCommand.setOutputStream(outputStream);
        sudoCommand.setLogger(logger);
        sudoCommand.setSudoPassword(sudoPassword);
        sudoCommand.setSudoPromptPattern(sudoPromptPattern);
        sudoCommand.setCommandTimeoutMs(commandTimeoutMs);
        return sudoCommand;
    }


}

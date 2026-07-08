package com.plugin.sshjplugin.sudo

import spock.lang.Specification
import spock.lang.Unroll

class SudoCommandSpec extends Specification {

    @Unroll
    def "sanitizeExitCode strips leaked ANSI remnants from raw response #index"() {
        expect:
        SudoCommand.sanitizeExitCode(raw) == expected

        where:
        index | raw                     | expected
        1     | "0"                     | "0"
        2     | ESC_REMNANT + "0"       | "0"
        3     | "127"                   | "127"
        4     | ESC_REMNANT + "127"     | "127"
        5     | "  0  "                 | "0"
    }

    @Unroll
    def "isValidExitCode returns #valid for '#value'"() {
        expect:
        SudoCommand.isValidExitCode(value) == valid

        where:
        value        | valid
        "0"          | true
        "127"        | true
        "-1"         | true
        ""           | false
        "abc"        | false
    }

    def "prompt pattern matches root shell prompt without a tilde"() {
        expect:
        "root@host:/etc# ".matches(SudoCommand.PROMPT_PATTERN)
    }

    def "prompt pattern matches standard user prompt with a tilde"() {
        expect:
        "user@host:~\$ ".matches(SudoCommand.PROMPT_PATTERN)
    }

    def "prompt pattern matches a custom PS1 without a tilde"() {
        expect:
        "[user@host project]\$ ".matches(SudoCommand.PROMPT_PATTERN)
    }

    // Reconstructed from its unicode code point so the source file never contains
    // a literal raw ESC control byte: the bracketed-paste-mode "disable" toggle
    // that leaks into sudo exit-code output, e.g. "<ESC>[?2004l0".
    private static final String ESC_REMNANT = new String(Character.toChars(0x1B)) + "[?2004l"
}

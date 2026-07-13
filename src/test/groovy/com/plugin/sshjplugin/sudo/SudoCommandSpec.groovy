package com.plugin.sshjplugin.sudo

import spock.lang.Specification
import spock.lang.Unroll

import java.util.regex.Pattern

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
        value                | valid
        "0"                  | true
        "127"                | true
        "-1"                 | true
        ""                   | false
        "abc"                | false
        // 19 digits overflows a signed int; a plain digit-regex would wrongly
        // accept it, but Integer.parseInt() (and thus this check) must not.
        "99999999999999999"  | false
    }

    // expectit's regexp() matcher uses Matcher.find() against the growing output
    // buffer, not a whole-string match, so the pattern itself must anchor to the
    // end of input or it will fire on any '$'/'#' that merely appears mid-output.
    @Unroll
    def "prompt pattern find() behavior for '#text'"() {
        expect:
        Pattern.compile(SudoCommand.PROMPT_PATTERN).matcher(text).find() == shouldMatch

        where:
        text                                    | shouldMatch
        "root@host:/etc# "                      | true
        "user@host:~\$ "                        | true
        "[user@host project]\$ "                | true
        "export PATH=\$PATH:/usr/bin\n"         | false
        "echo \$HOME is set\n"                  | false
    }

    // Reconstructed from its unicode code point so the source file never contains
    // a literal raw ESC control byte: the bracketed-paste-mode "disable" toggle
    // that leaks into sudo exit-code output, e.g. "<ESC>[?2004l0".
    private static final String ESC_REMNANT = new String(Character.toChars(0x1B)) + "[?2004l"
}

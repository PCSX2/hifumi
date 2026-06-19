// SPDX-FileCopyrightText: 2026 PCSX2 Dev Team
// SPDX-License-Identifier: MIT
package net.pcsx2.hifumi.parse;

public enum PnachParserError {

        FILE_NAME("(X) File name is not a valid serial and CRC"),
        START_LOWERCASE("(X) Starting keyword is not lower case."),
        BAD_BRACKET("(X) Missing closing bracket or content after bracket."),
        PARAM_COUNT("(X) Incorrect number of parameters; there should be 5 parameters, separated by commas."),
        SECOND_EQUALS("(X) Illegal second occurrence of `=` character."),
        START_KEYWORD("(X) Line did not start with a valid keyword."),
        NO_EQUALS("(X) No `=` operator found."),
        MISSING_RIGHT("(X) No content found on right side of `=` operator."),
        FIRST_RANGE("(X) First parameter is out of range; use `0` for single execution before boot, `1`, for continuous after boot, `2` for both `0` and `1` combined."),
        FIRST_NAN("(X) First parameter is not a number."),
        SECOND_CAPS("(X) Second parameter must be all capitals."),
        SECOND_CPU("(X) Second parameter was not a valid CPU type. Options are `EE` or `IOP`."),
        THIRD_LEAD_UNCHECKED(
                        "(!) Third parameter's leading digit is a complex type (larger than 2) and will not be checked for correctness."),
        THIRD_LEAD_NOT_ALLOWED(
                        "(X) Third parameter's leading digit is set to a non-zero value. Fourth parameter must be `extended` to use this digit as a mode selector."),
        THIRD_RANGE("(X) Third parameter memory address is out of range, cannot exceed 0x01ffffff."),
        THIRD_ADDRESS("(X) Third parameter is not a memory address."),
        FOURTH_LOWERCASE("(X) Fourth parameter must be all lower case."),
        FOURTH_TYPE("(X) Fourth parameter was not a valid data type. Options are `byte`, `short`, `word`, `double` or `extended`."),
        FIFTH_SCOPE("(!) Fifth parameter value exceeds scope of fourth parameter data type."),
        FIFTH_VALUE("(X) Fifth parameter is not a valid hexadecimal value."),
        BRACKET_WHITESPACE("(X) Leading or trailing whitespace in square brackets will prevent cheats from loading properly.");

        private String displayString;

        private PnachParserError(String displayString) {
                this.displayString = displayString;
        }

        public String getDisplayString() {
                return displayString;
        }
}

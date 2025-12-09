/**
 * This file is part of HifumiBot, licensed under the MIT License (MIT)
 * 
 * Copyright (c) 2020 RedPanda4552 (https://github.com/RedPanda4552)
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
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

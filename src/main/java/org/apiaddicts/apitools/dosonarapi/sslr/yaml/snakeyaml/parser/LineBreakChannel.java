/*
 * doSonarAPI SSLR :: YAML Parser
 * Copyright (C) 2021-2022 Apiaddicts
 * contacta AT apiaddicts DOT org
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.apiaddicts.apitools.dosonarapi.sslr.yaml.snakeyaml.parser;

import com.sonar.sslr.impl.Lexer;
import org.sonar.sslr.channel.Channel;
import org.sonar.sslr.channel.CodeReader;

class LineBreakChannel extends Channel<Lexer> {
    private final LexerState state;

    LineBreakChannel(LexerState state) {
        this.state = state;
    }

    @Override
    public boolean consume(CodeReader code, Lexer output) {
        // If we scanned a line break, then (depending on flow level),
        // simple keys may be allowed.
        if (scanLineBreak(code).length() != 0) {// found a line-break
            if (state.flowLevel() == 0) {
                // Simple keys are allowed at flow-level 0 after a line break
                state.allowSimpleKey(true);
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * Scan a line break, transforming:
     *
     * <pre>
     * '\r\n' : '\n'
     * '\r' : '\n'
     * '\n' : '\n'
     * '\x85' : '\n'
     * default : ''
     * </pre>
     */
    static String scanLineBreak(CodeReader reader) {
        // Transforms:
        // '\r\n' : '\n'
        // '\r' : '\n'
        // '\n' : '\n'
        // '\x85' : '\n'
        // default : ''
        char ch = reader.charAt(0);
        if (ch == '\r' || ch == '\n' || ch == '\u0085') {
            if (ch == '\r' && '\n' == reader.charAt(1)) {
                reader.pop();
                reader.pop();
            } else {
                reader.pop();
            }
            return "\n";
        } else if (ch == '\u2028' || ch == '\u2029') {
            reader.pop();
            return String.valueOf(ch);
        }
        return "";
    }

}

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

import com.sonar.sslr.api.Token;
import org.sonar.sslr.channel.Channel;
import org.sonar.sslr.channel.CodeBuffer;
import org.sonar.sslr.channel.CodeReader;

public class KeyChannel extends Channel<com.sonar.sslr.impl.Lexer> {
    private final LexerState state;
    private final Token.Builder tokenBuilder = Token.builder();

    public KeyChannel(LexerState state) {
        this.state = state;
    }

    @Override
    public boolean consume(CodeReader code, com.sonar.sslr.impl.Lexer output) {
        if (code.charAt(0) == '?' && (state.flowLevel() != 0 || Lexer.NULL_BL_T_LINEBR_S.indexOf(code.charAt(1)) != -1)) {
            fetchKey(code, output);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Fetch a key in a block-style mapping.
     *
     * @see http://www.yaml.org/spec/1.1/#id863975
     */
    private void fetchKey(CodeReader reader, com.sonar.sslr.impl.Lexer output) {
        // Block context needs additional checks.
        if (state.flowLevel() == 0) {
            // Are we allowed to start a key (not necessary a simple)?
            if (!state.allowSimpleKey()) {
                throw new YamlLexerException(null, null, "mapping keys are not allowed here",
                        reader.getCursor());
            }
            // We may need to add BLOCK-MAPPING-START.
            if (state.addIndent(reader.getColumnPosition())) {
                CodeBuffer.Cursor startMark = reader.getCursor();
                Token token = tokenBuilder
                        .setType(Tokens.BLOCK_MAPPING_START)
                        .setValueAndOriginalValue("{", "")
                        .setURI(output.getURI())
                        .setLine(startMark.getLine())
                        .setColumn(startMark.getColumn())
                        .build();
                output.addToken(token);
            }
        }
        // Simple keys are allowed after '?' in the block context.
        state.allowSimpleKey(state.flowLevel() == 0);

        // Reset possible simple key on the current level.
        state.removePossibleSimpleKey(reader);

        // Add KEY.
        CodeBuffer.Cursor startMark = reader.getCursor().clone();
        reader.pop();
        Token token = tokenBuilder
                .setType(Tokens.KEY)
                .setValueAndOriginalValue("?")
                .setURI(output.getURI())
                .setLine(startMark.getLine())
                .setColumn(startMark.getColumn())
                .build();
        output.addToken(token);
    }
}

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

public class ValueChannel extends Channel<com.sonar.sslr.impl.Lexer> {
    private final LexerState state;
    private final Token.Builder tokenBuilder = Token.builder();

    public ValueChannel(LexerState state) {
        this.state = state;
    }

    @Override
    public boolean consume(CodeReader code, com.sonar.sslr.impl.Lexer output) {
        int c = code.peek();
        if (c == ':' && ((state.flowLevel() != 0) || Lexer.NULL_BL_T_LINEBR_S.indexOf(code.charAt(1)) != -1)) {
            fetchValue(code, output);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Fetch a value in a block-style mapping.
     *
     * @see http://www.yaml.org/spec/1.1/#id863975
     */
    private void fetchValue(CodeReader reader, com.sonar.sslr.impl.Lexer output) {
        // Do we determine a simple key?
        SimpleKey key = state.possibleSimpleKeys().remove(state.flowLevel());
        if (key != null) {
            // Add KEY.
            Token token = tokenBuilder
                    .setType(Tokens.KEY)
                    .setValueAndOriginalValue("?", "")
                    .setURI(output.getURI())
                    .setLine(key.getLine())
                    .setColumn(key.getColumn())
                    .build();
            output.addToken(token);

            // If this key starts a new block mapping, we need to add
            // BLOCK-MAPPING-START.
            if (state.flowLevel() == 0 && state.addIndent(key.getColumn())) {
                token = tokenBuilder
                        .setType(Tokens.BLOCK_MAPPING_START)
                        .setValueAndOriginalValue("{", "")
                        .setURI(output.getURI())
                        .setLine(key.getLine())
                        .setColumn(key.getColumn())
                        .build();
                output.addToken(token);
            }
            // There cannot be two simple keys one after another.
            state.allowSimpleKey(false);

        } else {
            // It must be a part of a complex key.
            // Block context needs additional checks. Do we really need them?
            // They will be caught by the parser anyway.
            // We are allowed to start a complex value if and only if we can
            // start a simple key.
            if (state.flowLevel() == 0 && !state.allowSimpleKey()) {
                throw new YamlLexerException(null, null, "mapping values are not allowed here",
                        reader.getCursor());
            }

            // If this value starts a new block mapping, we need to add
            // BLOCK-MAPPING-START. It will be detected as an error later by
            // the parser.
            if (state.flowLevel() == 0 && state.addIndent(reader.getColumnPosition())) {
                CodeBuffer.Cursor mark = reader.getCursor().clone();
                Token token = tokenBuilder
                        .setType(Tokens.BLOCK_MAPPING_START)
                        .setValueAndOriginalValue("{", "")
                        .setURI(output.getURI())
                        .setLine(mark.getLine())
                        .setColumn(mark.getColumn())
                        .build();
                output.addToken(token);
            }

            // Simple keys are allowed after ':' in the block context.
            state.allowSimpleKey(state.flowLevel() == 0);

            // Reset possible simple key on the current level.
            state.removePossibleSimpleKey(reader);
        }
        // Add VALUE.
        CodeBuffer.Cursor startMark = reader.getCursor().clone();
        reader.pop();
        Token token = tokenBuilder
                .setType(Tokens.VALUE)
                .setValueAndOriginalValue(":")
                .setURI(output.getURI())
                .setLine(startMark.getLine())
                .setColumn(startMark.getColumn())
                .build();
        output.addToken(token);
    }
}

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
import com.sonar.sslr.impl.Lexer;
import org.sonar.sslr.channel.Channel;
import org.sonar.sslr.channel.CodeReader;

class BlockEntryChannel extends Channel<Lexer> {
    private static final String LINEBR_S = "\n\u0085\u2028\u2029";
    private static final String FULL_LINEBR_S = "\r" + LINEBR_S;
    private static final String NULL_OR_LINEBR_S = "\0" + FULL_LINEBR_S;
    private static final String NULL_BL_LINEBR_S = " " + NULL_OR_LINEBR_S;
    private static final String NULL_BL_T_LINEBR_S = "\t" + NULL_BL_LINEBR_S;

    private final LexerState state;
    private Token.Builder tokenBuilder = Token.builder();

    BlockEntryChannel(LexerState state) {
        this.state = state;
    }

    @Override
    public boolean consume(CodeReader code, Lexer output) {
        if (code.peek() == '-' && NULL_BL_T_LINEBR_S.indexOf(code.charAt(1)) != -1) {
            fetchBlockEntry(code, output);
            return true;
        }
        return false;
    }

    /**
     * Fetch an entry in the block style.
     *
     * @see http://www.yaml.org/spec/1.1/#id863975
     */
    private void fetchBlockEntry(CodeReader code, Lexer output) {
        // Block context needs additional checks.
        if (state.flowLevel() == 0) {
            // Are we allowed to start a new entry?
            if (!state.allowSimpleKey()) {
                throw new YamlLexerException(null, null, "sequence entries are not allowed here", code.getCursor());
            }

            // We may need to add BLOCK-SEQUENCE-START.
            if (state.addIndent(code.getColumnPosition())) {
                Token token = tokenBuilder
                        .setType(Tokens.BLOCK_SEQUENCE_START)
                        .setValueAndOriginalValue("[", "")
                        .setURI(output.getURI())
                        .setLine(code.getLinePosition())
                        .setColumn(code.getColumnPosition())
                        .build();
                output.addToken(token);
            }
        } else {
            // It's an error for the block entry to occur in the flow
            // but we let the parser detect it
        }
        // Simple keys are allowed after '-'.
        state.allowSimpleKey(true);

        // Reset possible simple key on the current level.
        state.removePossibleSimpleKey(code);

        // Add BLOCK-ENTRY.
        CodeReader.Cursor startMark = code.getCursor().clone();
        code.pop();
        Token token = tokenBuilder
                .setType(Tokens.BLOCK_ENTRY) // set block=true, indentless=false
                .setValueAndOriginalValue("-")
                .setURI(output.getURI())
                .setLine(startMark.getLine())
                .setColumn(startMark.getColumn())
                .build();
        output.addToken(token);
    }

}

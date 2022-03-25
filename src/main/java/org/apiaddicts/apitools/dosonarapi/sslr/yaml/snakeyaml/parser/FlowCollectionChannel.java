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
import com.sonar.sslr.api.TokenType;
import com.sonar.sslr.impl.Lexer;
import org.sonar.sslr.channel.Channel;
import org.sonar.sslr.channel.CodeBuffer;
import org.sonar.sslr.channel.CodeReader;

public class FlowCollectionChannel extends Channel<Lexer> {
    private final LexerState state;
    private final Token.Builder tokenBuilder = Token.builder();

    public FlowCollectionChannel(LexerState state) {
        this.state = state;
    }

    @Override
    public boolean consume(CodeReader code, Lexer output) {
        int c = code.peek();
        switch(c) {
        case '[':
            // Is it the flow sequence start indicator?
            fetchFlowCollectionStart(code, output, false);
            return true;
        case '{':
            // Is it the flow mapping start indicator?
            fetchFlowCollectionStart(code, output, true);
            return true;
        case ']':
            // Is it the flow sequence end indicator?
            fetchFlowCollectionEnd(code, output, false);
            return true;
        case '}':
            // Is it the flow mapping end indicator?
            fetchFlowCollectionEnd(code, output, true);
            return true;
        case ',':
            // Is it the flow entry indicator?
            fetchFlowEntry(code, output);
            return true;
            // see block entry indicator above
        default:
            return false;
        }
    }

    /**
     * Fetch a flow-style collection start, which is either a sequence or a
     * mapping. The type is determined by the given boolean.
     *
     * A flow-style collection is in a format similar to JSON. Sequences are
     * started by '[' and ended by ']'; mappings are started by '{' and ended by
     * '}'.
     *
     * @see http://www.yaml.org/spec/1.1/#id863975
     */
    private void fetchFlowCollectionStart(CodeReader reader, Lexer output, boolean isMappingStart) {
        // '[' and '{' may start a simple key.
        state.savePossibleSimpleKey(reader, output);

        // Increase the flow level.
        state.increaseFlowLevel();

        // Simple keys are allowed after '[' and '{'.
        state.allowSimpleKey(true);

        // Add FLOW-SEQUENCE-START or FLOW-MAPPING-START.
        CodeBuffer.Cursor startMark = reader.getCursor().clone();
        reader.pop();
        TokenType type;
        String value;
        if (isMappingStart) {
            type = Tokens.FLOW_MAPPING_START;
            value = "{";
        } else {
            type = Tokens.FLOW_SEQUENCE_START;
            value = "[";
        }
        Token token = tokenBuilder
                .setType(type)
                .setValueAndOriginalValue(value)
                .setURI(output.getURI())
                .setLine(startMark.getLine())
                .setColumn(startMark.getColumn())
                .build();
        output.addToken(token);
    }

    /**
     * Fetch a flow-style collection end, which is either a sequence or a
     * mapping. The type is determined by the given boolean.
     *
     * A flow-style collection is in a format similar to JSON. Sequences are
     * started by '[' and ended by ']'; mappings are started by '{' and ended by
     * '}'.
     *
     * @see http://www.yaml.org/spec/1.1/#id863975
     */
    private void fetchFlowCollectionEnd(CodeReader reader, Lexer output, boolean isMappingEnd) {
        // Reset possible simple key on the current level.
        state.removePossibleSimpleKey(reader);

        // Decrease the flow level.
        state.decreaseFlowLevel();

        // No simple keys after ']' or '}'.
        state.allowSimpleKey(false);

        // Add FLOW-SEQUENCE-END or FLOW-MAPPING-END.
        CodeBuffer.Cursor startMark = reader.getCursor().clone();
        reader.pop();
        TokenType type;
        String value;
        if (isMappingEnd) {
            type = Tokens.FLOW_MAPPING_END;
            value = "}";
        } else {
            type = Tokens.FLOW_SEQUENCE_END;
            value = "]";
        }
        Token token = tokenBuilder
                .setType(type)
                .setValueAndOriginalValue(value)
                .setURI(output.getURI())
                .setLine(startMark.getLine())
                .setColumn(startMark.getColumn())
                .build();
        output.addToken(token);
    }

    /**
     * Fetch an entry in the flow style. Flow-style entries occur either
     * immediately after the start of a collection, or else after a comma.
     *
     * @see http://www.yaml.org/spec/1.1/#id863975
     */
    private void fetchFlowEntry(CodeReader reader, Lexer output) {
        // Simple keys are allowed after ','.
        state.allowSimpleKey(true);

        // Reset possible simple key on the current level.
        state.removePossibleSimpleKey(reader);

        // Add FLOW-ENTRY.
        CodeBuffer.Cursor startMark = reader.getCursor().clone();
        reader.pop();
        Token token = tokenBuilder
                .setType(Tokens.FLOW_ENTRY)
                .setValueAndOriginalValue(",")
                .setURI(output.getURI())
                .setLine(startMark.getLine())
                .setColumn(startMark.getColumn())
                .build();
        output.addToken(token);
    }
}

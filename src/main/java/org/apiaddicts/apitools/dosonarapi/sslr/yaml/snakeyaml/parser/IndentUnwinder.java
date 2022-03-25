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

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class IndentUnwinder {
    private final LexerState state;
    private final Token.Builder tokenBuilder = Token.builder();

    IndentUnwinder(LexerState state) {
        this.state = state;
    }

    /**
     * * Handle implicitly ending multiple levels of block nodes by decreased
     * indentation. This function becomes important on lines 4 and 7 of this
     * example:
     *
     * <pre>
     * 1) book one:
     * 2)   part one:
     * 3)     chapter one
     * 4)   part two:
     * 5)     chapter one
     * 6)     chapter two
     * 7) book two:
     * </pre>
     *
     * In flow context, tokens should respect indentation. Actually the
     * condition should be `self.indent &gt;= column` according to the spec. But
     * this condition will prohibit intuitively correct constructions such as
     * key : { } </pre>
     */
    public List<Token> unwindIndent(int col, int tokenLine, int tokenCol, URI uri) {
        // In the flow context, indentation is ignored. We make the scanner less
        // restrictive then specification requires.
        if (state.flowLevel() != 0) {
            return Collections.emptyList();
        }

        // In block context, we may need to issue the BLOCK-END tokens.
        List<Token> result = new ArrayList<>();
        while (state.indent() > col) {
            state.popIndent();
            Token token = tokenBuilder
                    .setType(Tokens.BLOCK_END)
                    .setValueAndOriginalValue("}", "")
                    .setURI(uri)
                    .setLine(tokenLine)
                    .setColumn(tokenCol)
                    .build();
            result.add(token);
        }
        return result;
    }

}

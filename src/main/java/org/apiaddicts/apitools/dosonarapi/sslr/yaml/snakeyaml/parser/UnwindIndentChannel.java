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

import java.util.List;

public class UnwindIndentChannel extends Channel<Lexer> {
    private final LexerState state;
    private final IndentUnwinder unwinder;

    UnwindIndentChannel(LexerState state) {
        this.state = state;
        this.unwinder = new IndentUnwinder(state);
    }

    @Override
    public boolean consume(CodeReader code, Lexer output) {
        state.stalePossibleSimpleKeys(code);
        List<Token> tokens = unwinder.unwindIndent(code.getColumnPosition(), code.getLinePosition(), code.getColumnPosition(), output.getURI());
        if (!tokens.isEmpty()) {
            output.addToken(tokens.toArray(new Token[0]));
        }
        return false;
    }

}

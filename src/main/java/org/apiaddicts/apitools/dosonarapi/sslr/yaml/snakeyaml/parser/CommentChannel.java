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
import com.sonar.sslr.api.Trivia;
import java.util.List;
import org.sonar.sslr.channel.Channel;
import org.sonar.sslr.channel.CodeReader;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.sonar.sslr.api.GenericTokenType.COMMENT;
import static org.apiaddicts.apitools.dosonarapi.sslr.yaml.snakeyaml.parser.LineBreakChannel.scanLineBreak;

public class CommentChannel extends Channel<com.sonar.sslr.impl.Lexer> {
    private static final Matcher MATCHER = Pattern.compile("[^" + Lexer.NULL_OR_LINEBR_S + "]*+").matcher("");

    private final StringBuilder tmpBuilder = new StringBuilder();
    private final Token.Builder tokenBuilder = Token.builder();
    private final boolean ignoreLineEndings;

    public CommentChannel(boolean ignoreLineEndings) {
        this.ignoreLineEndings = ignoreLineEndings;
    }

    @Override
    public boolean consume(CodeReader code, com.sonar.sslr.impl.Lexer lexer) {
        // If the character we have skipped forward to is a comment (#),
        // then peek ahead until we find the next end of line. YAML
        // comments are from a # to the next new-line. We then forward
        // past the comment.
        if (code.peek() != '#') {
            return false;
        }
        code.pop();
        String value = "";
        int line = code.getLinePosition();
        int column = code.getColumnPosition() - 1;
        if (Lexer.NULL_OR_LINEBR_S.indexOf(code.charAt(0)) == -1 && code.popTo(MATCHER, tmpBuilder) > 0) {
            value = tmpBuilder.toString();
            tmpBuilder.delete(0, tmpBuilder.length());
            line = code.getPreviousCursor().getLine();
            column = code.getPreviousCursor().getColumn() - 1;
        }

        Token token = tokenBuilder
                .setType(COMMENT)
                .setValueAndOriginalValue(value)
                .setURI(lexer.getURI())
                .setLine(line)
                .setColumn(column)
                .build();

        lexer.addTrivia(Trivia.createComment(token));

        List<Token> tokens = lexer.getTokens();
        if (!ignoreLineEndings && hasNoPrecedingTokenOnLine(tokens, line)) {
            // If we are the first token of the line (so this is a full comment line),
            // need to consume the line breaks to not confuse the IndentUnwinder
            scanLineBreak(code);
        }

        return true;
    }

    private static boolean hasNoPrecedingTokenOnLine(List<Token> tokens, int line) {
        return tokens.isEmpty() || tokens.get(tokens.size() - 1).getLine() < line;
    }
}

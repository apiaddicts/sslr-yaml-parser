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

import com.sonar.sslr.impl.LexerException;
import javax.annotation.Nullable;
import org.sonar.sslr.channel.CodeBuffer;

public class YamlLexerException extends LexerException {
    private final String context;
    private final CodeBuffer.Cursor contextMark;
    private final String problem;
    private final CodeBuffer.Cursor problemMark;

    public YamlLexerException(@Nullable String context, @Nullable CodeBuffer.Cursor contextMark, String problem, CodeBuffer.Cursor problemMark) {
        super(formatMessage(context, problem, problemMark));
        this.context = context;
        this.contextMark = clone(contextMark);
        this.problem = problem;
        this.problemMark = clone(problemMark);
    }

    public YamlLexerException(@Nullable String context, @Nullable CodeBuffer.Cursor contextMark, String problem, @Nullable CodeBuffer.Cursor problemMark, Throwable cause) {
        super(formatMessage(context, problem, problemMark), cause);
        this.context = context;
        this.contextMark = clone(contextMark);
        this.problem = problem;
        this.problemMark = clone(problemMark);
    }

    private static CodeBuffer.Cursor clone(@Nullable CodeBuffer.Cursor contextMark) {
        return contextMark == null ? null : contextMark.clone();
    }

    private static String formatMessage(@Nullable String context, String problem, @Nullable CodeBuffer.Cursor problemMark) {
        return context + "; " + problem + "; " + toString(problemMark);
    }

    @Override
    public String toString() {
        StringBuilder lines = new StringBuilder();
        if (this.context != null) {
            lines.append(this.context);
            lines.append("\n");
        }

        if (this.contextMark != null && (this.problem == null || this.problemMark == null || this.contextMark.getLine() != this.problemMark.getLine() || this.contextMark.getColumn() != this.problemMark.getColumn())) {
            lines.append(toString(contextMark));
            lines.append("\n");
        }

        if (this.problem != null) {
            lines.append(this.problem);
            lines.append("\n");
        }

        if (this.problemMark != null) {
            lines.append(toString(problemMark));
            lines.append("\n");
        }

        return lines.toString();
    }

    public String getContext() {
        return this.context;
    }

    public CodeBuffer.Cursor getContextMark() {
        return this.contextMark;
    }

    public String getProblem() {
        return this.problem;
    }

    public CodeBuffer.Cursor getProblemMark() {
        return this.problemMark;
    }

    private static String toString(@Nullable CodeBuffer.Cursor cursor) {
        if (cursor == null) {
            return "";
        }
        return "line " + cursor.getLine() + ", column " + cursor.getColumn();
    }
}

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

/**
 * Simple keys treatment.
 * <p>
 * Helper class for {@link LexerState}.
 * </p>
 * 
 * @see LexerState
 */
final class SimpleKey {
    private int tokenNumber;
    private boolean required;
    private int line;
    private int column;

    public SimpleKey(int tokenNumber, boolean required, int line, int column) {
        this.tokenNumber = tokenNumber;
        this.required = required;
        this.line = line;
        this.column = column;
    }

    public int getColumn() {
        return this.column;
    }

    public int getLine() {
        return line;
    }

    public boolean isRequired() {
        return required;
    }

    @Override
    public String toString() {
        return "SimpleKey - tokenNumber=" + tokenNumber + " required=" + required + " index="
                + line + " column=" + column;
    }
}

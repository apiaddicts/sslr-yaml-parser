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

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.TokenType;

import javax.annotation.Nonnull;

public enum Tokens implements TokenType {
    BLOCK_SEQUENCE_START,
    BLOCK_MAPPING_START,
    BLOCK_END,
    BLOCK_ENTRY,
    STRING,
    INTEGER,
    FLOAT,
    TRUE,
    FALSE,
    NULL,
    FLOW_MAPPING_START,
    FLOW_SEQUENCE_START,
    FLOW_MAPPING_END,
    FLOW_SEQUENCE_END,
    FLOW_ENTRY,
    KEY,
    VALUE;

    @Override
    public String getName() {
        return name();
    }

    @Override
    public String getValue() {
        return name();
    }

    @Override
    public boolean hasToBeSkippedFromAst(@Nonnull AstNode node) {
        return false;
    }
}

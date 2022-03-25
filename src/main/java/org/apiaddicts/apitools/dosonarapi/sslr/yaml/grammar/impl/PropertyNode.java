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
package org.apiaddicts.apitools.dosonarapi.sslr.yaml.grammar.impl;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.AstNodeType;
import com.sonar.sslr.api.Token;
import javax.annotation.Nullable;
import org.apiaddicts.apitools.dosonarapi.sslr.yaml.grammar.JsonNode;

import static org.apiaddicts.apitools.dosonarapi.sslr.yaml.snakeyaml.parser.Tokens.KEY;
import static org.apiaddicts.apitools.dosonarapi.sslr.yaml.snakeyaml.parser.Tokens.VALUE;

public class PropertyNode extends JsonNode {
  public PropertyNode(AstNodeType type, String name, @Nullable Token token) {
    super(type, name, token);
  }

  @Override
  public JsonNode value() {
    return extractSiblingOf(VALUE);
  }

  @Override
  public JsonNode key() {
    return extractSiblingOf(KEY);
  }

  @Override
  public boolean isSyntax() {
    return true;
  }

  private JsonNode extractSiblingOf(AstNodeType nodeType) {
    AstNode firstChild = getFirstChild(nodeType);
    if (firstChild == null) {
      return MissingNode.MISSING;
    }
    JsonNode nextSibling = (JsonNode) firstChild.getNextSibling();
    if (nextSibling == null) {
      nextSibling = MissingNode.MISSING;
    }
    return nextSibling;
  }

}

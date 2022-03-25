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

import com.fasterxml.jackson.core.JsonPointer;
import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.AstNodeType;
import com.sonar.sslr.api.Token;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.apiaddicts.apitools.dosonarapi.sslr.yaml.grammar.JsonNode;

import static org.apiaddicts.apitools.dosonarapi.sslr.yaml.grammar.YamlGrammar.BLOCK_ARRAY_ELEMENT;
import static org.apiaddicts.apitools.dosonarapi.sslr.yaml.grammar.YamlGrammar.FLOW_ARRAY_ELEMENT;

public class ArrayNode extends JsonNode {
  public ArrayNode(AstNodeType type, String name, @Nullable Token token) {
    super(type, name, token);
  }

  @Override
  public boolean isArray() {
    return true;
  }

  @Override
  public List<JsonNode> elements() {
    return getChildren(BLOCK_ARRAY_ELEMENT, FLOW_ARRAY_ELEMENT).stream().map(n -> (JsonNode)n.getFirstChild()).collect(Collectors.toList());
  }

  @Override
  protected JsonNode internalAt(JsonPointer ptr) {
    int index = ptr.getMatchingIndex();
    int i=0;
    for (AstNode child: getChildren(FLOW_ARRAY_ELEMENT, BLOCK_ARRAY_ELEMENT)) {
      if (i++ == index) {
        return (JsonNode)child.getFirstChild();
      }
    }
    return MissingNode.MISSING;
  }
}

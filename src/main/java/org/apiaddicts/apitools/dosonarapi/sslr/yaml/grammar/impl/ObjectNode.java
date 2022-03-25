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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.apiaddicts.apitools.dosonarapi.sslr.yaml.grammar.JsonNode;
import org.apiaddicts.apitools.dosonarapi.sslr.yaml.grammar.Utils;

import static org.apiaddicts.apitools.dosonarapi.sslr.yaml.grammar.YamlGrammar.BLOCK_PROPERTY;
import static org.apiaddicts.apitools.dosonarapi.sslr.yaml.grammar.YamlGrammar.FLOW_PROPERTY;
import static org.apiaddicts.apitools.dosonarapi.sslr.yaml.grammar.YamlGrammar.ROOT;
import static org.apiaddicts.apitools.dosonarapi.sslr.yaml.grammar.YamlGrammar.SCALAR;
import static org.apiaddicts.apitools.dosonarapi.sslr.yaml.snakeyaml.parser.Tokens.STRING;

public class ObjectNode extends JsonNode {
  public ObjectNode(AstNodeType type, String name, @Nullable Token token) {
    super(type, name, token);
  }

  @Override
  protected JsonNode internalAt(JsonPointer ptr) {
    String propertyName = ptr.getMatchingProperty().replace("~1", "/").replace("~0", "~");
    JsonNode property = getJsonChildren(FLOW_PROPERTY, BLOCK_PROPERTY).stream().filter(n -> n.getFirstChild(SCALAR).getTokenValue().equals(propertyName)).findFirst().orElse(MissingNode.MISSING);
    return property.value();
  }

  @Override
  public JsonNode get(String fieldName) {
    String pointer = Utils.escapeJsonPointer(fieldName);
    return this.at("/" + pointer);
  }

  @Override
  public boolean isObject() {
    return true;
  }

  @Override
  public List<String> propertyNames() {
    return getJsonChildren(BLOCK_PROPERTY, FLOW_PROPERTY).stream()
        .map(n -> n.key().getTokenValue())
        .collect(Collectors.toList());
  }

  @Override
  public Collection<JsonNode> properties() {
    Collection<JsonNode> result = new ArrayList<>();
    for (JsonNode child : getJsonChildren(BLOCK_PROPERTY, FLOW_PROPERTY)) {
      result.add(child.value());
    }
    return result;
  }

  @Override
  public Map<String, JsonNode> propertyMap() {
    Map<String, JsonNode> result = new HashMap<>();
    for (JsonNode child : getJsonChildren(BLOCK_PROPERTY, FLOW_PROPERTY)) {
      JsonNode keyNode = child.key(); // TODO - if the key is not a scalar, this will give inconsistent results
      JsonNode valueNode = child.value();
      result.put(keyNode.getTokenValue(), valueNode);
    }
    return result;
  }

  @Override
  public <T> Map<String, T> propertyMap(Function<JsonNode, T> mapper) {
    Map<String, T> result = new HashMap<>();
    for (JsonNode child : getJsonChildren(BLOCK_PROPERTY, FLOW_PROPERTY)) {
      JsonNode keyNode = child.key(); // TODO - if the key is not a scalar, this will give inconsistent results
      JsonNode valueNode = child.value();
      result.put(keyNode.getTokenValue(), mapper.apply(valueNode));
    }
    return result;
  }

  @Override
  public boolean isRef() {
    JsonNode at = at("/$ref");
    return at.getToken() != null && at.getToken().getType() == STRING;
  }

  /**
   * Resolve this reference to the actual node. This only supports references to the current document.
   * If this node is not a reference, returns {@code this}.
   *
   * @return the resolved node
   */
  @Override
  public JsonNode resolve() {
    if (!isRef()) {
      return this;
    }
    String p = at("/$ref").getTokenValue();
    if (!p.startsWith("#")) {
      throw new IllegalArgumentException("Cannot resolve references to other documents: \"" + p + "\"");
    }
    AstNode root = this;
    while (root.getParent() != null && root.getParent().getType() != ROOT) {
      root = root.getParent();
    }
    return ((JsonNode)root).at(p.substring(1));
  }

}

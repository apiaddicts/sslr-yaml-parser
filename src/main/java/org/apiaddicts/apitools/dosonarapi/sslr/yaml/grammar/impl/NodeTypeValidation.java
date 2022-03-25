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

import com.google.common.collect.Sets;
import com.sonar.sslr.api.AstNodeType;
import java.util.Collections;
import java.util.Set;
import org.apiaddicts.apitools.dosonarapi.sslr.yaml.grammar.JsonNode;

public class NodeTypeValidation extends ValidationBase {
  private final Set<AstNodeType> types;

  public NodeTypeValidation(AstNodeType... types) {
    this.types = Sets.newLinkedHashSet();
    Collections.addAll(this.types, types);
  }

  @Override
  public boolean validate(JsonNode node, Context context) {
    if(!types.contains(node.getType())) {
      context.recordFailure(node, "Expected: " + toString() + ", got: " + node.getType());
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    if (types.size() == 1) {
      return types.stream().map(Object::toString).findFirst().orElse("");
    } else {
      return "One of " + types;
    }
  }
}

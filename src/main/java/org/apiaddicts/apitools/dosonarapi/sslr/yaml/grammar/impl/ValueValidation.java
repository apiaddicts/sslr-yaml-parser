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
import org.apiaddicts.apitools.dosonarapi.sslr.yaml.grammar.JsonNode;

public abstract class ValueValidation extends ValidationBase {

  protected final boolean validate(JsonNode node, Context context) {
    boolean valid = isValueValid(node, context);
    if (!valid) {
      String pointer = node.key().stringValue();
      if (!pointer.isEmpty()) {
        pointer = pointer + ": ";
      }
      context.recordFailure(node, pointer + "Expected: " + toString() + ", got: \"" + node.getTokenValue() + "\"");
    }
    return valid;
  }

  protected abstract boolean isValueValid(AstNode node, Context context);

}

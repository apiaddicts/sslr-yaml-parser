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

import org.apiaddicts.apitools.dosonarapi.sslr.yaml.grammar.JsonNode;
import org.apiaddicts.apitools.dosonarapi.sslr.yaml.grammar.ValidationRule;
import org.junit.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.mock;

public class PropertyDescriptionImplTest {
  @Test
  public void validates_through_delegate() {
    ValidationRule delegate = mock(ValidationRule.class);
    PropertyDescriptionImpl validation = new PropertyDescriptionImpl("keyName", false, false, false, delegate);

    JsonNode node = mock(JsonNode.class);
    ValidationRule.Context context = mock(ValidationRule.Context.class);
    validation.visit(node, context);

    Mockito.verify(delegate).visit(node, context);
  }
}

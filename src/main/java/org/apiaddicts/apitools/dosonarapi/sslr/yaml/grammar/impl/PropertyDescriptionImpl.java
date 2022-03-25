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
import org.apiaddicts.apitools.dosonarapi.sslr.yaml.grammar.PropertyDescription;
import org.apiaddicts.apitools.dosonarapi.sslr.yaml.grammar.ValidationRule;

public class PropertyDescriptionImpl implements PropertyDescription {
  private final String key;
  private final boolean pattern;
  private final boolean mandatory;
  private final ValidationRule delegate;
  private final boolean discriminant;

  public PropertyDescriptionImpl(String key, boolean pattern, boolean mandatory, boolean discriminant, ValidationRule delegate) {
    this.key = key;
    this.pattern = pattern;
    this.mandatory = mandatory;
    this.discriminant = discriminant;
    this.delegate = delegate;
  }

  @Override
  public String getKey() {
    return key;
  }

  @Override
  public boolean isPattern() {
    return pattern;
  }

  @Override
  public boolean isMandatory() {
    return mandatory;
  }

  @Override
  public boolean isDiscriminant() {
    return discriminant;
  }

  @Override
  public boolean visit(JsonNode node, Context context) {
    return delegate.visit(node, context);
  }

  @Override
  public String toString() {
    return delegate.toString();
  }
}

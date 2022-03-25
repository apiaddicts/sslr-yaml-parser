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

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AlwaysTrueValidationTest extends ValidationTestBase {

  private AlwaysTrueValidation validation = new AlwaysTrueValidation();

  @Test
  public void matches_booleans() {
    validation.visit(parseText("y"), context);
    assertThat(context.captured()).isEmpty();
  }

  @Test
  public void matches_floats() {
    validation.visit(parseText("12.0"), context);
    assertThat(context.captured()).isEmpty();
  }

  @Test
  public void matches_arrays() {
    validation.visit(parseText("[ 1, 2, 3 ]"), context);
    assertThat(context.captured()).isEmpty();
  }

  @Test
  public void matches_objects() {
    validation.visit(parseText("p1: v1"), context);
    assertThat(context.captured()).isEmpty();
  }
}

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
package org.apiaddicts.apitools.dosonarapi.sslr.yaml.grammar;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class UtilsTest {
  @Test
  public void can_escape_json_pointers() {
    assertEquals("toto~1titi", Utils.escapeJsonPointer("toto/titi"));
    assertEquals("toto~0titi", Utils.escapeJsonPointer("toto~titi"));
    assertEquals("toto~01titi", Utils.escapeJsonPointer("toto~1titi"));
    assertEquals("toto~00titi", Utils.escapeJsonPointer("toto~0titi"));
    assertEquals("tototiti", Utils.escapeJsonPointer("tototiti"));
    assertEquals("", Utils.escapeJsonPointer(""));
  }

}

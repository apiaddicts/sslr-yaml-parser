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

import com.fasterxml.jackson.core.JsonPointer;

public class Utils {
  private Utils() {
    // Hidden utilty class constructor
  }

  /**
   * Escapes path segment values to an unambiguous form.
   * The escape char to be inserted is '~'. The chars to be escaped
   * are ~, which maps to ~0, and /, which maps to ~1. 
   * @param token the JSONPointer segment value to be escaped
   * @return the escaped value for the token
   */
  public static String escapeJsonPointer(String token) {
    return token.replace("~", "~0")
                .replace("/", "~1");
  }

  public static JsonPointer escape(String token) {
    return JsonPointer.compile("/" + token.replace("~", "~0")
        .replace("/", "~1"));
  }
}

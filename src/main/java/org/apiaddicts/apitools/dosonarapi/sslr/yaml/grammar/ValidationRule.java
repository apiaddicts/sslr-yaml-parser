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

import java.util.List;

public interface ValidationRule {
  /**
   * Validate the supplied node.
   * @param node the node to validate
   * @param context validation context
   * @return {@code true} if the node respects the rule
   */
  boolean visit(JsonNode node, Context context);

  interface Context {
    /**
     * Records a violation of a rule with an ERROR level. Captured errors fail the validation in strict mode.
     * @param node the location of the violation
     * @param message the violation description
     * @param causes any other violations that may have caused or explain this violation
     */
    void recordFailure(JsonNode node, String message, ValidationIssue... causes);

    /**
     * Records a violation of a rule with a WARNING level. Captured warnings won't fail the validation but will be
     * reported as violations.
     * @param node the location of the violation
     * @param message the violation description
     * @param causes any other violations that may have caused or explain this violation
     */
    void recordWarning(JsonNode node, String message, ValidationIssue... causes);

    /**
     * Start capturing a new violation frame.
     *
     * @see #captured()
     */
    void capture();

    /**
     * Get the warnings and errors captured in the current violation frame. Throws NoSuchElementException if called
     * without a former call to {@link #capture()}.
     * @return the list of captured warnings and errors
     */
    List<ValidationIssue> captured();
  }
}

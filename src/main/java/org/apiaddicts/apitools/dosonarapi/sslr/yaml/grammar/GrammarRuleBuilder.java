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

import org.sonar.sslr.grammar.GrammarException;

public interface GrammarRuleBuilder {

  /**
   * Allows to provide definition of a grammar rule.
   * <p>
   * <b>Note:</b> this method can be called only once for a rule. If it is called more than once, an GrammarException will be thrown.
   *
   * @param e  expression of grammar
   * @return this (for method chaining)
   * @throws GrammarException if definition has been already done
   * @throws IllegalArgumentException if given argument is not a parsing expression
   */
  GrammarRuleBuilder is(Object e);

  /**
   * Indicates that grammar rule should not lead to creation of AST node - its children should be attached directly to its parent.
   * This is useful for rules that define a choice between other rules, but should not create new levels in the AST.
   */
  void skip();

}

/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.cleanup.storage.config;

import org.apache.lucene.util.automaton.RegExp;

import static java.lang.String.format;

/**
 * @since 3.19
 */
public class RegexCriteriaValidator
{
  private RegexCriteriaValidator() {
  }

  /**
   * Ensures that a regular expression entered is a valid pattern.
   *
   * @param expression
   * @throws InvalidExpressionException when the expression is deemed invalid
   */
  public static String validate(final String expression) {
    try {
      new RegExp(expression);
    }
    catch (IllegalArgumentException e) {  // NOSONAR
      throw new InvalidExpressionException(
          format("Invalid regular expression pattern: %s", e.getMessage()));
    }
    return expression;
  }

  public static class InvalidExpressionException
      extends RuntimeException
  {
    InvalidExpressionException(final String message) {
      super(message);
    }
  }
}

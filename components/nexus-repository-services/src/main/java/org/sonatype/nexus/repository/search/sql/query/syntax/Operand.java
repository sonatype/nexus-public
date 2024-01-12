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
package org.sonatype.nexus.repository.search.sql.query.syntax;

/**
 * An operand for use in queries.
 */
public enum Operand
{
  /**
   * An operand indicating that the term should be or'd together
   */
  OR(true),

  /**
   * An operand indicating that the term should be and'd together
   */
  AND(true),

  /**
   * An operand indicating that the term should be equal
   */
  EQ(true),

  /**
   * An operand indicating that the term should not be equal
   */
  NOT_EQ(false),

  /**
   * An operand indicating a regular expression match
   */
  REGEX(false),

  /**
   * An operand indicating that one of the terms should match.
   */
  IN(true),

  ANY(true);

  private boolean multiple;

  Operand(final boolean multiple) {
    this.multiple = multiple;
  }

  /**
   * Indicates whether the operand supports multiple terms
   */
  public boolean supportsMultiple() {
    return multiple;
  }
}

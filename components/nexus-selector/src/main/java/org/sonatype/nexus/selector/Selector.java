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
package org.sonatype.nexus.selector;

/**
 * An interface for evaluating selection based on variable input.
 *
 * @since 3.0
 */
public interface Selector
{
  /**
   * Returns {@code true} if this selector matches against the given variables, otherwise {@code false}.
   *
   * @param variableSource the source of variable values
   */
  boolean evaluate(VariableSource variableSource);

  /**
   * Returns SQL representing this selector for use as a 'where' clause against some queryable values.
   *
   * @param sqlBuilder the builder of 'where' clauses for content selectors
   *
   * @throws UnsupportedOperationException if this selector cannot be represented as SQL
   *
   * @since 3.next
   */
  void toSql(SelectorSqlBuilder sqlBuilder);
}

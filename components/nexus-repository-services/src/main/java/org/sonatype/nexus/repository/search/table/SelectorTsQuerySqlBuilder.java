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
package org.sonatype.nexus.repository.search.table;

import org.sonatype.nexus.selector.SelectorSqlBuilder;

/**
 * Builds TSQUERY conditions for content selectors so that they can be used for querying the component_search.paths
 * TSVECTOR column
 */
public class SelectorTsQuerySqlBuilder
    extends SelectorSqlBuilder
{
  private static final String TS_QUERY_CONFIG = "'simple'";

  private static final String TO_TSQUERY_FUNCTION = "TO_TSQUERY";

  private static final char LEFT_PARENTHESIS = '(';

  private static final char RIGHT_PARENTHESIS = ')';

  public void appendTsQueryFunction(final String argument) {
    queryBuilder.append(TO_TSQUERY_FUNCTION).append(LEFT_PARENTHESIS).append(TS_QUERY_CONFIG).append(", ");
    appendLiteral(argument);
    queryBuilder.append(RIGHT_PARENTHESIS);
  }
}

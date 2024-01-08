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
package org.sonatype.nexus.repository.search.sql;

import java.util.Optional;

import org.sonatype.nexus.repository.search.query.SearchFilter;

/**
 * Accumulates conditions created by the {@link SqlSearchQueryContribution} implementations for each {@link
 * SearchFilter} passed into them. It holds the conditions built so far.
 *
 * @since 3.38
 */
public interface SqlSearchQueryBuilder
{
  SqlSearchQueryBuilder add(SqlSearchQueryCondition condition);

  /**
   * Concatenates all the SqlSearchQueryCondition conditions into a single one.
   * All the {@link SqlSearchQueryCondition#getSqlConditionFormat()} are 'ANDed' into one and all the  {@link
   * SqlSearchQueryCondition#getValues()} are combined into one.
   *
   * The intention is for this to be used in building a SQL query through a {@link java.sql.PreparedStatement}.
   * The {@link SqlSearchQueryCondition#getValues()} contains named values which corresponds to the {@link
   * SqlSearchQueryCondition#getSqlConditionFormat()}.
   *
   * @return A {@link SqlSearchQueryCondition} which is a consolidation of all the conditionFormats and named values in
   * this object.
   */
  Optional<SqlSearchQueryCondition> buildQuery();
}

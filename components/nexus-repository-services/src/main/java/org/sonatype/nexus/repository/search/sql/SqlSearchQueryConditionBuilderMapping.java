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

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.rest.SearchFieldSupport;
import org.sonatype.nexus.repository.search.sql.textsearch.PostgresFullTextSearchQueryBuilder;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.rest.sql.TextualQueryType.FULL_TEXT_SEARCH_QUERY;

/**
 * Utility for fetching the {@link SqlSearchQueryConditionBuilder} for a given column.
 *
 */
@Named
@Singleton
public class SqlSearchQueryConditionBuilderMapping
    extends ComponentSupport
{
  private final Map<String, SqlSearchQueryConditionBuilder> conditionBuilders;

  @Inject
  public SqlSearchQueryConditionBuilderMapping(final Map<String, SqlSearchQueryConditionBuilder> conditionBuilders) {
    this.conditionBuilders = checkNotNull(conditionBuilders);
  }

  public SqlSearchQueryConditionBuilder getConditionBuilder(final SearchFieldSupport fieldMapping) {
    if (fieldMapping != null && FULL_TEXT_SEARCH_QUERY == fieldMapping.getTextualQueryType()) {
      return conditionBuilders.get(PostgresFullTextSearchQueryBuilder.NAME);
    }
    return conditionBuilders.get(DefaultSqlSearchQueryConditionBuilder.NAME);
  }
}

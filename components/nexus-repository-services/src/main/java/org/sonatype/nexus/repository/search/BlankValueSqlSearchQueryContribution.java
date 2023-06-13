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
package org.sonatype.nexus.repository.search;

import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.rest.SearchFieldSupport;
import org.sonatype.nexus.repository.rest.SearchMappings;
import org.sonatype.nexus.repository.search.query.SearchFilter;
import org.sonatype.nexus.repository.search.sql.SqlSearchQueryBuilder;
import org.sonatype.nexus.repository.search.sql.SqlSearchQueryCondition;
import org.sonatype.nexus.repository.search.sql.SqlSearchQueryConditionBuilder;
import org.sonatype.nexus.repository.search.sql.SqlSearchQueryConditionBuilderMapping;
import org.sonatype.nexus.repository.search.sql.SqlSearchQueryContributionSupport;

import static java.util.Optional.ofNullable;

/**
 * Creates sql query to check specific field is empty.
 */
@Named(BlankValueSqlSearchQueryContribution.NAME)
@Singleton
public class BlankValueSqlSearchQueryContribution
    extends SqlSearchQueryContributionSupport
{
  public static final String NAME = SqlSearchQueryContributionSupport.NAME_PREFIX + "blankValueSqlSearchContribution";

  @Inject
  public BlankValueSqlSearchQueryContribution(
      final SqlSearchQueryConditionBuilderMapping conditionBuilders,
      final Map<String, SearchMappings> searchMappings)
  {
    super(conditionBuilders, searchMappings);
  }

  @Override
  public void contribute(final SqlSearchQueryBuilder queryBuilder, final SearchFilter searchFilter) {
    ofNullable(searchFilter)
        .flatMap(this::buildQueryCondition)
        .ifPresent(queryBuilder::add);
  }

  @Override
  protected Optional<SqlSearchQueryCondition> buildQueryCondition(final SearchFilter searchFilter) {
    final SearchFieldSupport fieldMappingDef = fieldMappings.get(searchFilter.getProperty());
    log.debug("Mapping for {} is {}", searchFilter, fieldMappingDef);
    final SqlSearchQueryConditionBuilder builder = conditionBuilders.getConditionBuilder(fieldMappingDef);
    return ofNullable(builder.conditionWithEmptyValue(fieldMappingDef.getColumnName()));
  }
}

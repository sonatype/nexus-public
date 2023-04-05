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
package org.sonatype.nexus.content.maven.internal.search.table;

import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.rest.SearchMappings;
import org.sonatype.nexus.repository.search.query.SearchFilter;
import org.sonatype.nexus.repository.search.sql.SqlSearchQueryBuilder;
import org.sonatype.nexus.repository.search.sql.SqlSearchQueryConditionBuilderMapping;
import org.sonatype.nexus.repository.search.sql.SqlSearchQueryContributionSupport;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.sonatype.nexus.content.maven.internal.search.table.MavenSearchCustomFieldContributor.toStoredBaseVersionFormat;

/**
 * Creates sql query conditions for the assets.attributes.baseVersion search term.
 */
@Named(MavenBaseVersionSqlSearchQueryContribution.NAME)
@Singleton
public class MavenBaseVersionSqlSearchQueryContribution
    extends SqlSearchQueryContributionSupport
{
  protected static final String BASE_VERSION = "attributes.maven2.baseVersion";

  public static final String NAME = SqlSearchQueryContributionSupport.NAME_PREFIX + BASE_VERSION;

  @Inject
  public MavenBaseVersionSqlSearchQueryContribution(
      final SqlSearchQueryConditionBuilderMapping conditionBuilders,
      final Map<String, SearchMappings> searchMappings)
  {
    super(conditionBuilders, searchMappings);
  }

  @Override
  public void contribute(final SqlSearchQueryBuilder queryBuilder, final SearchFilter searchFilter) {
    String value = searchFilter.getValue();
    if (isNotBlank(value)) {
      super.contribute(queryBuilder, new SearchFilter(searchFilter.getProperty(), value));
    }
    else {
      super.contribute(queryBuilder, searchFilter);
    }
  }

  @Override
  protected Set<String> split(final String baseVersion) {
    return toStoredBaseVersionFormat(super.split(baseVersion));
  }
}

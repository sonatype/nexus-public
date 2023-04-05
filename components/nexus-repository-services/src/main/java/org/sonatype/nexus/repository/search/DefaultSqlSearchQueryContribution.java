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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.rest.SearchMappings;
import org.sonatype.nexus.repository.search.sql.SqlSearchQueryBuilder;
import org.sonatype.nexus.repository.search.sql.SqlSearchQueryConditionBuilderMapping;
import org.sonatype.nexus.repository.search.sql.SqlSearchQueryContributionSupport;

/**
 * Default {@link SqlSearchQueryContribution} which splits the search term/value by whitespace, builds exact or
 * wildcard sql query conditions and stores them in the supplied {@link SqlSearchQueryBuilder}.
 *
 * @see SqlSearchQueryBuilder
 * @since 3.38
 */
@Named(DefaultSqlSearchQueryContribution.NAME)
@Singleton
public class DefaultSqlSearchQueryContribution
    extends SqlSearchQueryContributionSupport
{
  public static final String NAME = SqlSearchQueryContributionSupport.NAME_PREFIX + "defaultSqlSearchContribution";

  @Inject
  public DefaultSqlSearchQueryContribution(
      SqlSearchQueryConditionBuilderMapping conditionBuilders,
      final Map<String, SearchMappings> searchMappings)
  {
    super(conditionBuilders, searchMappings);
  }
}

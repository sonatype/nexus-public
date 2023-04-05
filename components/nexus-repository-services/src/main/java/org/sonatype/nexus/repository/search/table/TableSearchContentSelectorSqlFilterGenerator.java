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

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.content.search.table.internal.CselToTsQuerySql;
import org.sonatype.nexus.repository.rest.SearchFieldSupport;
import org.sonatype.nexus.repository.rest.SearchMappings;
import org.sonatype.nexus.repository.search.sql.SqlSearchContentSelectorFilter;
import org.sonatype.nexus.repository.search.sql.SqlSearchQueryCondition;
import org.sonatype.nexus.repository.search.sql.SqlSearchQueryConditionBuilderMapping;
import org.sonatype.nexus.selector.CselSelector;
import org.sonatype.nexus.selector.CselToSql;
import org.sonatype.nexus.selector.SelectorConfiguration;
import org.sonatype.nexus.selector.SelectorEvaluationException;
import org.sonatype.nexus.selector.SelectorManager;
import org.sonatype.nexus.selector.SelectorSqlBuilder;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.unmodifiableMap;
import static org.sonatype.nexus.repository.search.SqlSearchQueryContribution.preventTokenization;
import static org.sonatype.nexus.repository.search.index.SearchConstants.REPOSITORY_NAME;
import static org.sonatype.nexus.repository.search.sql.SqlSearchQueryContributionSupport.fieldMappingsByAttribute;

/**
 * Creates SQL filters from content selectors. The expectation is that the generated SQL filters will be used in a SQL
 * WHERE clause.
 */
@Singleton
public class TableSearchContentSelectorSqlFilterGenerator
    extends ComponentSupport
{
  public static final String PATHS = "paths";

  private static final String FILTER_PARAMS = "filterParams";

  private static final String PATH = "path";

  private static final String PATH_ALIAS = "tsvector_paths";

  private static final String FORMAT = "format";

  private static final String FORMAT_ALIAS = "tsvector_format";

  private static final String PATHS_ALIAS = "paths";

  private static final String LEFT_PARENTHESIS = "(";

  private static final String RIGHT_PARENTHESIS = ")";

  private final SelectorManager selectorManager;

  private final SqlSearchQueryConditionBuilderMapping conditionBuilders;

  protected final Map<String, SearchFieldSupport> fieldMappings;

  @Inject
  public TableSearchContentSelectorSqlFilterGenerator(
      final SelectorManager selectorManager,
      final SqlSearchQueryConditionBuilderMapping conditionBuilders,
      final Map<String, SearchMappings> searchMappings)
  {
    this.selectorManager = checkNotNull(selectorManager);
    this.conditionBuilders = checkNotNull(conditionBuilders);
    this.fieldMappings = unmodifiableMap(fieldMappingsByAttribute(checkNotNull(searchMappings)));
  }

  /**
   * Maybe create a {@link SqlSearchQueryCondition} for a Content Selector and the repositories to which it applies.
   *
   * @param selector the content selector
   * @param repositories the repository names to which this content selector applies
   * @param index used as part of parameter names to ensure unique names
   * @return
   */
  public Optional<SqlSearchQueryCondition> createFilter(
      final SelectorConfiguration selector,
      final Set<String> repositories,
      final int index)
  {
    if (!CselSelector.TYPE.equals(selector.getType())) {
      log.debug("Content selector is not CSEL: {}", selector.getName());
      return Optional.empty();
    }

    SqlSearchContentSelectorFilter filters = new SqlSearchContentSelectorFilter();
    SelectorSqlBuilder selectorSqlBuilder = createSelectorSqlBuilder();
    CselToSql cselToTsQuerySql =  new CselToTsQuerySql();

    try {
      String namePrefix = "s" + index + "p";
      transformSelectorToSql(cselToTsQuerySql, selectorSqlBuilder, selector, namePrefix);
      collectGeneratedSql(filters, selectorSqlBuilder, repositories, namePrefix);
    }
    catch (SelectorEvaluationException e) {
      log.warn("Problem evaluating selector {} as SQL", selector.getName(), e);
      return Optional.empty();
    }

    return Optional.of(new SqlSearchQueryCondition(filters.queryFormat(), filters.queryParameters()));
  }

  private static SelectorSqlBuilder createSelectorSqlBuilder() {
    return new SelectorTsQuerySqlBuilder()
        .propertyAlias(PATH, PATH_ALIAS)
        .propertyAlias(PATHS, PATHS_ALIAS)
        .propertyAlias(FORMAT, FORMAT_ALIAS)
        .parameterPrefix("#{" + FILTER_PARAMS + ".")
        .parameterSuffix("}");
  }

  private void transformSelectorToSql(
      final CselToSql cselToTsQuerySql, final SelectorSqlBuilder sqlBuilder,
      final SelectorConfiguration selector,
      final String namePrefix) throws SelectorEvaluationException
  {
    sqlBuilder.parameterNamePrefix(namePrefix);
    selectorManager.toSql(selector, sqlBuilder, cselToTsQuerySql);
  }

  private void collectGeneratedSql(
      final SqlSearchContentSelectorFilter filter,
      final SelectorSqlBuilder sqlBuilder,
      final Set<String> repositories,
      final String namePrefix)
  {
    filter.putQueryParameters(sqlBuilder.getQueryParameters());
    filter.appendQueryFormatPart(LEFT_PARENTHESIS);
    filter.appendQueryFormatPart(sqlBuilder.getQueryString());

    if (!repositories.isEmpty()) {
      filter.appendQueryFormatPart(" AND ");
      SqlSearchQueryCondition condition = repositoryNameCondition(repositories, namePrefix + "_");
      filter.appendQueryFormatPart(condition.getSqlConditionFormat());
      filter.putQueryParameters(condition.getValues());
    }
    filter.appendQueryFormatPart(RIGHT_PARENTHESIS);
  }

  private SqlSearchQueryCondition repositoryNameCondition(
      final Set<String> repositories,
      final String namePrefix)
  {
    SearchFieldSupport fieldMapping = fieldMappings.get(REPOSITORY_NAME);
    return conditionBuilders.getConditionBuilder(fieldMapping)
        .condition(fieldMapping.getColumnName(), preventTokenization(repositories),
            namePrefix);//see repository name storage in SearchTableDAO.xml
  }
}

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

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.rest.SearchFieldSupport;
import org.sonatype.nexus.repository.rest.SearchMappings;
import org.sonatype.nexus.repository.search.sql.SqlSearchContentSelectorFilter;
import org.sonatype.nexus.repository.search.sql.SqlSearchQueryCondition;
import org.sonatype.nexus.repository.search.sql.SqlSearchQueryConditionBuilder;
import org.sonatype.nexus.selector.CselSelector;
import org.sonatype.nexus.selector.SelectorConfiguration;
import org.sonatype.nexus.selector.SelectorEvaluationException;
import org.sonatype.nexus.selector.SelectorManager;
import org.sonatype.nexus.selector.SelectorSqlBuilder;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.unmodifiableMap;
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
  private static final String FILTER_PARAMS = "filterParams";

  private static final String PATH = "path";

  private static final String LEFT_PARENTHESIS = "(";

  private static final String RIGHT_PARENTHESIS = ")";

  private static final String OR = " OR ";

  private final SelectorManager selectorManager;

  private final SqlSearchQueryConditionBuilder conditionBuilder;

  protected final Map<String, SearchFieldSupport> fieldMappings;

  @Inject
  public TableSearchContentSelectorSqlFilterGenerator(
      final SelectorManager selectorManager,
      final SqlSearchQueryConditionBuilder conditionBuilder,
      final Map<String, SearchMappings> searchMappings)
  {
    this.selectorManager = checkNotNull(selectorManager);
    this.conditionBuilder = checkNotNull(conditionBuilder);
    this.fieldMappings = unmodifiableMap(fieldMappingsByAttribute(checkNotNull(searchMappings)));
  }

  public SqlSearchContentSelectorFilter createFilter(
      final List<SelectorConfiguration> selectors,
      final Set<String> selectorRepositories)
  {

    SqlSearchContentSelectorFilter filters = new SqlSearchContentSelectorFilter();
    SelectorSqlBuilder selectorSqlBuilder = createSelectorSqlBuilder();
    for (int selectorCount = 0; selectorCount < selectors.size(); ++selectorCount) {
      final SelectorConfiguration selector = selectors.get(selectorCount);

      if (CselSelector.TYPE.equals(selector.getType())) {
        if (selectorCount > 0) {
          filters.appendQueryFormatPart(OR);
        }
        try {
          String namePrefix = "s" + selectorCount + "p";
          transformSelectorToSql(selectorSqlBuilder, selector, namePrefix);
          collectGeneratedSql(filters, selectorSqlBuilder, selectorRepositories, namePrefix);
        }
        catch (SelectorEvaluationException e) {
          log.warn("Problem evaluating selector {} as SQL", selector.getName(), e);
        }
        finally {
          selectorSqlBuilder.clearQueryString();
        }
      }
    }
    maybeAddOuterParentheses(selectors.size(), filters);
    return filters;
  }

  private static SelectorSqlBuilder createSelectorSqlBuilder() {
    return new SelectorSqlBuilder()
        .propertyAlias(PATH, PATH)

        .parameterPrefix("#{" + FILTER_PARAMS + ".")
        .parameterSuffix("}");
  }

  private static void maybeAddOuterParentheses(
      final int numberOfSelectors,
      final SqlSearchContentSelectorFilter filter)
  {
    if (numberOfSelectors > 1) {
      filter.insert(0, LEFT_PARENTHESIS);
      filter.appendQueryFormatPart(RIGHT_PARENTHESIS);
    }
  }

  private void transformSelectorToSql(
      final SelectorSqlBuilder sqlBuilder,
      final SelectorConfiguration selector,
      final String namePrefix) throws SelectorEvaluationException
  {
    sqlBuilder.parameterNamePrefix(namePrefix);
    selectorManager.toSql(selector, sqlBuilder);
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
    return conditionBuilder.condition(fieldMappings.get(REPOSITORY_NAME).getColumnName(), repositories, namePrefix);
  }
}

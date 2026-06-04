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
package org.sonatype.nexus.repository.search.sql.query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.sonatype.nexus.repository.search.sql.query.security.SqlSearchPermissionBuilder;

import org.sonatype.nexus.common.QualifierUtil;
import org.sonatype.nexus.repository.rest.SearchMapping.FilterType;
import org.sonatype.nexus.repository.rest.SearchMappings;
import org.sonatype.nexus.repository.search.SearchRequest;
import org.sonatype.nexus.repository.search.query.SearchFilter;
import org.sonatype.nexus.repository.search.sql.ExpressionGroup;
import org.sonatype.nexus.repository.search.sql.SqlSearchQueryContribution;
import org.sonatype.nexus.repository.search.sql.query.syntax.Expression;
import org.sonatype.nexus.repository.search.sql.query.syntax.Operand;
import org.sonatype.nexus.repository.search.sql.query.syntax.SqlClause;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.emptyList;
import static java.util.stream.StreamSupport.stream;
import static org.sonatype.nexus.repository.search.index.SearchConstants.REPOSITORY_NAME;
import org.springframework.stereotype.Component;

/**
 * A utility which converts a {@link SearchRequest} into an {@link Expression} with the current user's security.
 */
@Component
public class ExpressionBuilder
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  private final SqlSearchPermissionBuilder permissionBuilder;

  private final Map<String, SqlSearchQueryContribution> handlers;

  private final Map<String, FilterType> filterTypeByAttribute;

  @Autowired
  public ExpressionBuilder(
      final SqlSearchPermissionBuilder permissionBuilder,
      final List<SqlSearchQueryContribution> handlersList,
      final List<SearchMappings> searchMappingsList)
  {
    this.permissionBuilder = checkNotNull(permissionBuilder);
    this.handlers = QualifierUtil.buildQualifierBeanMap(checkNotNull(handlersList));
    this.filterTypeByAttribute = new HashMap<>();
    QualifierUtil.buildQualifierBeanMap(checkNotNull(searchMappingsList))
        .entrySet()
        .stream()
        .flatMap(e -> stream(e.getValue().get().spliterator(), true))
        .forEach(mapping -> {
          filterTypeByAttribute.put(mapping.getAttribute(), mapping.getFilterType());
          filterTypeByAttribute.put(mapping.getAlias(), mapping.getFilterType());
        });
  }

  /**
   * Create an {@link Expression} from the search request with security applied if necessary.
   */
  public Optional<ExpressionGroup> from(final SearchRequest request) {

    log.debug("Search request {}", request);

    Map<FilterType, List<SearchFilter>> filterTypes = groupBySearchMappingFilterType(request);
    final Optional<Expression> permissionExpressions = permissionBuilder.build(request);
    final Optional<Expression> assetFilterExpressions = getAssetFilterExpressions(request, filterTypes);
    final Optional<Expression> componentFiltersExpressions = getComponentFiltersExpressions(request, filterTypes);

    if (!componentFiltersExpressions.isPresent() &&
        !assetFilterExpressions.isPresent() &&
        !permissionExpressions.isPresent()) {
      return Optional.empty();
    }

    final Expression componentsFilterClause =
        getFilterClause(componentFiltersExpressions, permissionExpressions);

    final Expression assetsFilterClause =
        getFilterClause(assetFilterExpressions, Optional.empty());

    final ExpressionGroup expressionGroup =
        new ExpressionGroup(componentsFilterClause, assetsFilterClause);

    log.debug("Created expressions {}", expressionGroup);

    return Optional.of(expressionGroup);
  }

  private static Expression getFilterClause(
      final Optional<Expression> filterExpression,
      final Optional<Expression> permissionExpressions)
  {
    return filterExpression
        .map(expr -> permissionExpressions
            .map(permExpr -> SqlClause.create(Operand.AND, expr, permExpr))
            .orElseGet(() -> SqlClause.create(Operand.AND, expr)))
        .orElse(permissionExpressions.orElse(null));
  }

  private Optional<Expression> getComponentFiltersExpressions(
      final SearchRequest request,
      final Map<FilterType, List<SearchFilter>> filterTypes)
  {
    List<SearchFilter> componentFilters = filterTypes.getOrDefault(FilterType.COMPONENT, emptyList());
    return createExpressionFromFilters(componentFilters, request.isConjunction());
  }

  private Optional<Expression> getAssetFilterExpressions(
      final SearchRequest request,
      final Map<FilterType, List<SearchFilter>> filterTypes)
  {
    List<SearchFilter> assetFilters = filterTypes.getOrDefault(FilterType.ASSET, emptyList());
    return createExpressionFromFilters(assetFilters, request.isConjunction());
  }

  private Map<FilterType, List<SearchFilter>> groupBySearchMappingFilterType(final SearchRequest request) {
    return request.getSearchFilters()
        .stream()
        .collect(Collectors.groupingBy(searchFilter -> {
          FilterType key = filterTypeByAttribute.get(searchFilter.getProperty());
          return key != null ? key : FilterType.COMPONENT;
        }));
  }

  private Optional<Expression> createExpressionFromFilters(
      final List<SearchFilter> searchFilters,
      final boolean isConjunction)
  {
    // Partition filters by operator presence
    PartitionedFilters partitioned = partitionFilters(searchFilters);

    // Convert filters to expressions
    List<Expression> globalFlagExpressions = convertFiltersToExpressions(partitioned.withGlobalFlag);
    Map<SearchFilter.FilterOperator, List<Expression>> explicitOperatorExpressions =
        groupByOperator(partitioned.withExplicitOperator);

    // Combine all expressions with appropriate operators
    return combineAllExpressions(globalFlagExpressions, explicitOperatorExpressions, isConjunction);
  }

  /**
   * Partitions filters into those with explicit operators and those using the global conjunction flag.
   * Package-private for testing.
   */
  PartitionedFilters partitionFilters(final List<SearchFilter> searchFilters) {
    Map<Boolean, List<SearchFilter>> partitioned = searchFilters.stream()
        .filter(Objects::nonNull)
        .filter(this::isNotRepositoryNameFilter)
        .collect(Collectors.partitioningBy(filter -> filter.getOperator().isPresent()));

    return new PartitionedFilters(partitioned.get(true), partitioned.get(false));
  }

  /**
   * Checks if a filter is not a repository name filter.
   */
  boolean isNotRepositoryNameFilter(final SearchFilter filter) {
    return !REPOSITORY_NAME.equalsIgnoreCase(filter.getProperty());
  }

  /**
   * Converts a list of search filters to expressions.
   */
  List<Expression> convertFiltersToExpressions(final List<SearchFilter> filters) {
    return filters.stream()
        .map(this::toExpression)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(Collectors.toList());
  }

  /**
   * Groups filters by their explicit operator type and converts them to expressions.
   */
  Map<SearchFilter.FilterOperator, List<Expression>> groupByOperator(final List<SearchFilter> filters) {
    return filters.stream()
        .collect(Collectors.groupingBy(
            filter -> filter.getOperator().get(),
            Collectors.mapping(
                filter -> toExpression(filter).orElse(null),
                Collectors.filtering(Objects::nonNull, Collectors.toList()))));
  }

  /**
   * Wraps a list of expressions with the specified operand.
   * Returns empty if the list is empty, a single expression if the list has one element,
   * or a combined clause for multiple expressions.
   */
  Optional<Expression> wrapExpressionsWithOperand(final List<Expression> expressions, final Operand operand) {
    if (expressions.isEmpty()) {
      return Optional.empty();
    }
    if (expressions.size() == 1) {
      return Optional.of(expressions.get(0));
    }
    return Optional.of(SqlClause.create(operand, expressions));
  }

  /**
   * Combines all expressions (global flag and explicit operator) into a single expression.
   * Global flag expressions are combined with AND or OR based on isConjunction.
   * Explicit AND expressions are added individually.
   * Explicit OR expressions are wrapped in an OR clause.
   * All top-level expressions are AND'ed together.
   */
  private Optional<Expression> combineAllExpressions(
      final List<Expression> globalFlagExpressions,
      final Map<SearchFilter.FilterOperator, List<Expression>> explicitOperatorExpressions,
      final boolean isConjunction)
  {
    List<Expression> allExpressions = new ArrayList<>();

    // Add global flag expressions as a single clause
    Operand globalOperand = isConjunction ? Operand.AND : Operand.OR;
    wrapExpressionsWithOperand(globalFlagExpressions, globalOperand)
        .ifPresent(allExpressions::add);

    // Add explicit AND expressions (already individual, don't wrap)
    List<Expression> andExpressions =
        explicitOperatorExpressions.getOrDefault(SearchFilter.FilterOperator.AND, emptyList());
    allExpressions.addAll(andExpressions);

    // Add explicit OR expressions as a single clause
    List<Expression> orExpressions =
        explicitOperatorExpressions.getOrDefault(SearchFilter.FilterOperator.OR, emptyList());
    wrapExpressionsWithOperand(orExpressions, Operand.OR)
        .ifPresent(allExpressions::add);

    // All top-level expressions are AND'ed together
    Optional<Expression> result = wrapExpressionsWithOperand(allExpressions, Operand.AND);

    if (result.isPresent()) {
      if (allExpressions.size() == 1) {
        log.debug("Single expression: {}", result.get());
      }
      else {
        log.debug("Creating AND clause for expressions: {}", allExpressions);
      }
    }

    return result;
  }

  /**
   * Container for partitioned search filters.
   */
  static class PartitionedFilters
  {
    final List<SearchFilter> withExplicitOperator;

    final List<SearchFilter> withGlobalFlag;

    PartitionedFilters(final List<SearchFilter> withExplicitOperator, final List<SearchFilter> withGlobalFlag) {
      this.withExplicitOperator = withExplicitOperator;
      this.withGlobalFlag = withGlobalFlag;
    }
  }

  /*
   * Convert a SearchFilter into an Expression
   */
  private Optional<Expression> toExpression(@Nullable final SearchFilter filter) {
    log.debug("Applying handler for {}", filter);

    return Optional.ofNullable(filter)
        .map(SearchFilter::getProperty)
        .map(handlers::get)
        // No specific handler for the property, use the default one
        .orElseGet(() -> handlers.get("default"))
        .createPredicate(filter);
  }
}

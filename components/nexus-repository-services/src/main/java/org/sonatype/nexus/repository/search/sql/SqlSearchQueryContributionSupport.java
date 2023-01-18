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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import org.sonatype.nexus.repository.rest.SearchFieldSupport;
import org.sonatype.nexus.repository.rest.SearchMapping;
import org.sonatype.nexus.repository.rest.SearchMappings;
import org.sonatype.nexus.repository.rest.sql.ComponentSearchField;
import org.sonatype.nexus.repository.search.SqlSearchQueryContribution;
import org.sonatype.nexus.repository.search.SqlSearchValidationSupport;
import org.sonatype.nexus.repository.search.query.SearchFilter;

import org.apache.commons.collections.CollectionUtils;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableMap;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.StreamSupport.stream;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.removeEnd;
import static org.apache.commons.lang3.StringUtils.removeStart;

/**
 * Base implementation for {@link SqlSearchQueryContribution}
 *
 * @since 3.38
 */
public abstract class SqlSearchQueryContributionSupport
    extends SqlSearchValidationSupport
    implements SqlSearchQueryContribution
{
  private static final String QUOTE = "\"";

  private static final String DEFAULT_REGEX = "[^\\s\"]+|\"[^\"]+\"";

  private static final Pattern PATTERN = Pattern.compile(DEFAULT_REGEX);

  public static final String NAME_PREFIX = "datastore_search_";

  public static final String GAVEC = "gavec";

  protected final SqlSearchQueryConditionBuilderMapping conditionBuilders;

  protected final Map<String, SearchFieldSupport> fieldMappings;

  protected SqlSearchQueryContributionSupport(
      final SqlSearchQueryConditionBuilderMapping conditionBuilders,
      final Map<String, SearchMappings> searchMappings)
  {
    this.conditionBuilders = checkNotNull(conditionBuilders);
    this.fieldMappings = unmodifiableMap(fieldMappingsByAttribute(checkNotNull(searchMappings)));
  }

  @Override
  public void contribute(final SqlSearchQueryBuilder queryBuilder, final SearchFilter searchFilter) {
    ofNullable(searchFilter)
        .flatMap(this::buildQueryCondition)
        .ifPresent(queryBuilder::add);
  }

  protected Optional<SqlSearchQueryCondition> buildQueryCondition(final SearchFilter searchFilter) {
    final SearchFieldSupport fieldMappingDef = fieldMappings.get(searchFilter.getProperty());
    log.debug("Mapping for {} is {}", searchFilter, fieldMappingDef);
    final SqlSearchQueryConditionBuilder builder = conditionBuilders.getConditionBuilder(fieldMappingDef);

    final SearchFilter mappedField = getFieldMapping(searchFilter);
    return ofNullable(mappedField)
        .map(SearchFilter::getValue)
        .map(this::split)
        .filter(CollectionUtils::isNotEmpty)
        .map(this::getValidTokens)
        .map(values -> builder.condition(mappedField.getProperty(), values));
  }

  protected Set<String> split(final String value) {
    if (isBlank(value)) {
      return emptySet();
    }

    Matcher matcher = getMatcher(value);
    Set<String> matches = new HashSet<>();
    while (matcher.find()) {
      matches.add(maybeTrimQuotes(matcher.group()));
    }
    return matches;
  }

  protected Matcher getMatcher(final String value) {
    return PATTERN.matcher(value);
  }

  public static String maybeTrimQuotes(String value) {
    value = removeEnd(value, QUOTE);
    value = removeStart(value, QUOTE);
    return value;
  }

  public static Map<String, SearchFieldSupport> fieldMappingsByAttribute(final Map<String, SearchMappings> searchMappings) {
    final Map<String, SearchFieldSupport> byAttribute = searchMappings.entrySet().stream()
        .flatMap(e -> stream(e.getValue().get().spliterator(), false))
        .collect(toMap(SearchMapping::getAttribute, SearchMapping::getField));
    return addCustomMappings(byAttribute);
  }

  private static Map<String, SearchFieldSupport> addCustomMappings(final Map<String, SearchFieldSupport> byAttribute) {
    Map<String, SearchFieldSupport> mappings = new HashMap<>(byAttribute);
    mappings.put(GAVEC, ComponentSearchField.FORMAT_FIELD_4);
    return mappings;
  }

  @Nullable
  private SearchFilter getFieldMapping(final SearchFilter searchFilter) {
    final String property = searchFilter.getProperty();
    final SearchFieldSupport searchFieldSupport = fieldMappings.get(property);

    if (searchFieldSupport != null) {
      String fieldMapping = searchFieldSupport.getColumnName();
      if (isNotBlank(fieldMapping)) {
        return new SearchFilter(fieldMapping, searchFilter.getValue());
      }
      log.debug("Ignoring unsupported search field {}", property);
    }
    return null;
  }
}

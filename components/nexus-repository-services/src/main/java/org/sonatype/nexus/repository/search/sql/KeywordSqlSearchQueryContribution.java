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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.rest.SearchMappings;
import org.sonatype.nexus.repository.search.query.SearchFilter;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.appendIfMissing;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.prependIfMissing;
import static org.sonatype.nexus.repository.rest.internal.DefaultSearchMappings.GROUP_RAW;
import static org.sonatype.nexus.repository.rest.internal.DefaultSearchMappings.NAME_RAW;
import static org.sonatype.nexus.repository.rest.internal.DefaultSearchMappings.VERSION;
import static org.sonatype.nexus.repository.search.sql.SqlSearchQueryConditionBuilder.ZERO_OR_MORE_CHARACTERS;

/**
 * A keyword search is one where the user does not specify a specific
 * search field, rather expects the term to be matched across a number of core fields.
 *
 * A wildcard condition is created for each search term.
 *
 * A search term of the form: "group:name[:version][:extension][:classifier]" results in an exact search condition for
 * maven components with the specified group, name, version and optional extension and classifier.
 *
 * Otherwise wildcard conditions are created for checking each search term in the component's namespace, name and
 * version.
 *
 * @since 3.next
 */
@Named(KeywordSqlSearchQueryContribution.NAME)
@Singleton
public class KeywordSqlSearchQueryContribution
    extends SqlSearchQueryContributionSupport
{
  protected static final String NAME = NAME_PREFIX + "keyword";

  private static final List<String> KEYWORD_FIELDS = asList(GROUP_RAW, NAME_RAW, VERSION);

  private static final String SPLIT_REGEX = "[^-,\\s\"]+|\"[^\"]+\"";

  private static final String GAVEC_REGEX =
      "^(?<group>[^\\s:]+):(?<name>[^\\s:]+)(:(?<version>[^\\s:]+))?(:(?<extension>[^\\s:]+))?(:(?<classifier>[^\\s:]+))?$";
  // Allow dependency searches of the form "group:name[:version][:extension][:classifier]"

  private static final Pattern GAVEC_SPLITTER = Pattern.compile(GAVEC_REGEX);

  private static final Pattern SPLITTER = Pattern.compile(SPLIT_REGEX);

  @Inject
  public KeywordSqlSearchQueryContribution(
      final SqlSearchQueryConditionBuilder sqlSearchQueryConditionBuilder,
      final Map<String, SearchMappings> searchMappings)
  {
    super(sqlSearchQueryConditionBuilder, searchMappings);
  }

  @Override
  public void contribute(final SqlSearchQueryBuilder queryBuilder, final SearchFilter searchFilter) {

    if (searchFilter == null) {
      return;
    }

    final String value = searchFilter.getValue();
    Matcher gavSearchMatcher = GAVEC_SPLITTER.matcher(value.trim());

    if (gavSearchMatcher.matches()) {
      buildGavQuery(queryBuilder, gavSearchMatcher);
    }
    else {
      queryBuilder.add(sqlSearchQueryConditionBuilder.combine(buildKeywordQuery(value)));
    }
  }

  private List<SqlSearchQueryCondition> buildKeywordQuery(final String value) {
    return KEYWORD_FIELDS.stream()
        .map(field -> new SearchFilter(field, value))
        .map(this::buildQueryCondition)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(toList());
  }

  private String toContains(final String value) {
    String zeroOrMore = ZERO_OR_MORE_CHARACTERS + EMPTY;
    return appendIfMissing(prependIfMissing(value, zeroOrMore), zeroOrMore);
  }

  private void buildGavQuery(final SqlSearchQueryBuilder queryBuilder, final Matcher gavSearchMatcher) {
    addMavenAttribute(queryBuilder, "attributes.maven2.groupId", gavSearchMatcher.group("group"));

    addMavenAttribute(queryBuilder, "attributes.maven2.artifactId", gavSearchMatcher.group("name"));

    addMavenAttribute(queryBuilder, "attributes.maven2.baseVersion", gavSearchMatcher.group("version"));

    addMavenAttribute(queryBuilder, "assets.attributes.maven2.extension", gavSearchMatcher.group("extension"));

    addMavenAttribute(queryBuilder, "assets.attributes.maven2.classifier", gavSearchMatcher.group("classifier"));
  }

  private void addMavenAttribute(final SqlSearchQueryBuilder queryBuilder, final String attribute, final String value) {
    if (isNotBlank(value)) {
      queryBuilder.add(sqlSearchQueryConditionBuilder.condition(getColumnName(attribute), value));
    }
  }

  private String getColumnName(final String attribute) {
    return fieldMappings.get(attribute).getColumnName();
  }

  @Override
  protected Set<String> split(String value) {
    if (isBlank(value)) {
      return new HashSet<>();
    }

    Matcher matcher = SPLITTER.matcher(value);
    List<String> matches = new ArrayList<>();
    while (matcher.find()) {
      matches.add(toContains(maybeTrimQuotes(matcher.group())));
    }
    return new HashSet<>(matches);
  }
}

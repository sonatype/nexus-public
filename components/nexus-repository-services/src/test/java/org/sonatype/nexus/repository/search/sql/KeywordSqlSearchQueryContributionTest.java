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
import java.util.stream.Stream;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.rest.SearchFieldSupport;
import org.sonatype.nexus.repository.rest.SearchMapping;
import org.sonatype.nexus.repository.rest.SearchMappings;
import org.sonatype.nexus.repository.rest.internal.DefaultSearchMappings;
import org.sonatype.nexus.repository.rest.sql.ComponentSearchField;
import org.sonatype.nexus.repository.search.KeywordSqlSearchQueryContribution;
import org.sonatype.nexus.repository.search.query.SearchFilter;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static java.util.Arrays.asList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.rest.sql.ComponentSearchField.NAME;
import static org.sonatype.nexus.repository.rest.sql.ComponentSearchField.NAMESPACE;
import static org.sonatype.nexus.repository.rest.sql.ComponentSearchField.VERSION;

public class KeywordSqlSearchQueryContributionTest
    extends TestSupport
{
  public static final String NAMESPACE_CONDITION_FORMAT = "NAMESPACE_CONDITION";

  public static final String NAME_CONDITION_FORMAT = "NAME_CONDITION";

  public static final String VERSION_CONDITION_FORMAT = "VERSION_CONDITION";

  public static final String EXT_CONDITION_FORMAT = "EXT_CONDITION";

  public static final String CLASSIFIER_CONDITION_FORMAT = "CLASSIFIER_CONDITION";

  @Mock
  private SqlSearchQueryConditionBuilder sqlSearchQueryConditionBuilder;

  @Mock
  private SearchMappings searchMappings;

  @Mock
  private SqlSearchQueryBuilder queryBuilder;

  private KeywordSqlSearchQueryContribution underTest;

  @Before
  public void setup() {
    Map<String, SearchMappings> fieldMappings = new HashMap<>();
    fieldMappings.put("maven", searchMappings);
    fieldMappings.put("default", new DefaultSearchMappings());
    when(searchMappings.get()).thenReturn(searchMappings());
    underTest = new KeywordSqlSearchQueryContribution(sqlSearchQueryConditionBuilder, fieldMappings);
  }

  @Test
  public void shouldIgnoreNull() {
    underTest.contribute(queryBuilder, null);

    verifyNoMoreInteractions(sqlSearchQueryConditionBuilder);
    verify(queryBuilder, never()).add(any());
  }

  @Test
  public void shouldBeMavenGavecSearchCondition() {
    mockExactGavecSearch();

    underTest.contribute(queryBuilder, new SearchFilter("keyword", "org.mockito:mockito-core:3.24:jar:tests"));

    verify(queryBuilder).add(aSqlSearchCondition(NAMESPACE_CONDITION_FORMAT, "org.mockito"));
    verify(queryBuilder).add(aSqlSearchCondition(NAME_CONDITION_FORMAT, "mockito-core"));
    verify(queryBuilder).add(aSqlSearchCondition(VERSION_CONDITION_FORMAT, "3.24"));
    verify(queryBuilder).add(aSqlSearchCondition(EXT_CONDITION_FORMAT, "jar"));
    verify(queryBuilder).add(aSqlSearchCondition(CLASSIFIER_CONDITION_FORMAT, "tests"));
  }

  @Test
  public void splitByAndSearch() {
    Map<String, String> values = ImmutableMap.of("samplefield0", "mockito*", "samplefield1", "junit*");

    mockCondition(values);

    Stream.of("mockito junit", "mockito-junit", "mockito,junit")
        .forEach(value -> underTest.contribute(queryBuilder, new SearchFilter("keyword", value)));

    verify(queryBuilder, times(3)).add(aSqlSearchCondition("conditionFormat", values));
  }

  private void mockCondition(final Map<String, String> values) {
    Stream.of(NAMESPACE.getColumnName(), NAME.getColumnName(), VERSION.getColumnName()).forEach(column ->
        when(sqlSearchQueryConditionBuilder.condition(column, new HashSet<>(values.values())))
            .thenReturn(aSqlSearchCondition("conditionFormat", values))
    );

    when(sqlSearchQueryConditionBuilder.combine(
        asList(aSqlSearchCondition("conditionFormat", values),
            aSqlSearchCondition("conditionFormat", values),
            aSqlSearchCondition("conditionFormat", values))))
        .thenReturn(aSqlSearchCondition("conditionFormat", values));
  }

  private void mockExactGavecSearch() {
    when(sqlSearchQueryConditionBuilder.condition(NAMESPACE.getColumnName(), "org.mockito"))
        .thenReturn(aSqlSearchCondition(NAMESPACE_CONDITION_FORMAT, "org.mockito"));
    when(sqlSearchQueryConditionBuilder.condition(NAME.getColumnName(), "mockito-core"))
        .thenReturn(aSqlSearchCondition(NAME_CONDITION_FORMAT, "mockito-core"));
    when(sqlSearchQueryConditionBuilder.condition(VERSION.getColumnName(), "3.24"))
        .thenReturn(aSqlSearchCondition(VERSION_CONDITION_FORMAT, "3.24"));
    when(sqlSearchQueryConditionBuilder.condition("extension", "jar"))
        .thenReturn(aSqlSearchCondition(EXT_CONDITION_FORMAT, "jar"));
    when(sqlSearchQueryConditionBuilder.condition("classifier", "tests"))
        .thenReturn(aSqlSearchCondition(CLASSIFIER_CONDITION_FORMAT, "tests"));
  }

  private SqlSearchQueryCondition aSqlSearchCondition(final String nameConditionFormat, final String value) {
    return aSqlSearchCondition(nameConditionFormat, ImmutableMap.of("name", value));
  }

  private SqlSearchQueryCondition aSqlSearchCondition(
      final String nameConditionFormat,
      final Map<String, String> values)
  {
    return new SqlSearchQueryCondition(nameConditionFormat, values);
  }

  private Iterable<SearchMapping> searchMappings() {
    return ImmutableList.of(
        searchMapping("maven.groupId", "attributes.maven2.groupId", "Maven groupId", NAMESPACE),
        searchMapping("maven.artifactId", "attributes.maven2.artifactId", "Maven artifactId", NAME),
        searchMapping("maven.baseVersion", "attributes.maven2.baseVersion", "Maven base version", VERSION),
        searchMapping("maven.extension", "assets.attributes.maven2.extension", "Maven extension of component's asset",
            componentSearchField("extension")),
        searchMapping("maven.classifier", "assets.attributes.maven2.classifier",
            "Maven classifier of component's asset", componentSearchField("classifier"))
    );
  }

  private ComponentSearchField componentSearchField(final String classifier) {
    return new ComponentSearchField(classifier);
  }

  private SearchMapping searchMapping(
      final String alias,
      final String attribute,
      final String description,
      final SearchFieldSupport searchFieldSupport)
  {
    return new SearchMapping(alias, attribute, description, searchFieldSupport);
  }
}

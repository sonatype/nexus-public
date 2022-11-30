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

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.rest.sql.ComponentSearchField.FORMAT_FIELD_4;
import static org.sonatype.nexus.repository.rest.sql.ComponentSearchField.KEYWORD;

public class KeywordSqlSearchQueryContributionTest
    extends TestSupport
{
  public static final String GAVEC_CONDITION_FORMAT = "GAVEC_CONDITION";

  @Mock
  private SqlSearchQueryConditionBuilderMapping conditionBuilders;

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
    when(conditionBuilders.getConditionBuilder(any(SearchFieldSupport.class)))
        .thenReturn(sqlSearchQueryConditionBuilder);

    underTest = new KeywordSqlSearchQueryContribution(conditionBuilders, fieldMappings);
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

    verify(queryBuilder).add(aSqlSearchCondition());
  }

  @Test
  public void splitByAndSearch() {
    Map<String, String> values = ImmutableMap.of("samplefield0", "mockito*", "samplefield1", "junit*");

    mockCondition(values);

    Stream.of("mockito junit", "mockito-junit", "mockito,junit", "mockito/junit")
        .forEach(value -> underTest.contribute(queryBuilder, new SearchFilter("keyword", value)));

    verify(queryBuilder, times(4)).add(aSqlSearchCondition("conditionFormat", values));
  }

  private void mockCondition(final Map<String, String> values) {
    when(sqlSearchQueryConditionBuilder.condition(KEYWORD.getColumnName(), new HashSet<>(values.values())))
        .thenReturn(aSqlSearchCondition("conditionFormat", values));

    when(sqlSearchQueryConditionBuilder.combine(
        asList(aSqlSearchCondition("conditionFormat", values),
            aSqlSearchCondition("conditionFormat", values),
            aSqlSearchCondition("conditionFormat", values))))
        .thenReturn(aSqlSearchCondition("conditionFormat", values));
  }

  private void mockExactGavecSearch() {
    when(sqlSearchQueryConditionBuilder.condition(FORMAT_FIELD_4.getColumnName(),
        "/org.mockito<->/mockito-core<->/3.24<->/jar<->/tests"))
        .thenReturn(aSqlSearchCondition());
  }

  private SqlSearchQueryCondition aSqlSearchCondition() {
    return aSqlSearchCondition(KeywordSqlSearchQueryContributionTest.GAVEC_CONDITION_FORMAT,
        ImmutableMap.of("name", "g:a:v:e:c"));
  }

  private SqlSearchQueryCondition aSqlSearchCondition(
      final String nameConditionFormat,
      final Map<String, String> values)
  {
    return new SqlSearchQueryCondition(nameConditionFormat, values);
  }

  private Iterable<SearchMapping> searchMappings() {
    return singletonList(new SearchMapping("gavec", "gavec", "Group asset version extension classifier",
        ComponentSearchField.FORMAT_FIELD_4));
  }
}

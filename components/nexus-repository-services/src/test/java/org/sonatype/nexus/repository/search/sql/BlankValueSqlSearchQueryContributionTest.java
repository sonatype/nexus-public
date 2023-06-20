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
import java.util.Map;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.rest.SearchFieldSupport;
import org.sonatype.nexus.repository.rest.SearchMapping;
import org.sonatype.nexus.repository.rest.SearchMappings;
import org.sonatype.nexus.repository.rest.sql.ComponentSearchField;
import org.sonatype.nexus.repository.search.BlankValueSqlSearchQueryContribution;
import org.sonatype.nexus.repository.search.query.SearchFilter;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class BlankValueSqlSearchQueryContributionTest
    extends TestSupport
{
  public static final String GROUP = "group";

  @Mock
  private SqlSearchQueryConditionBuilderMapping conditionBuilders;

  @Mock
  private SqlSearchQueryConditionBuilder sqlSearchQueryConditionBuilder;

  @Mock
  private SqlSearchQueryBuilder queryBuilder;

  @Mock
  private SearchMappings searchMappings;

  private BlankValueSqlSearchQueryContribution underTest;

  @Before
  public void setup() {
    Map<String, SearchMappings> fieldMappings = new HashMap<>();
    fieldMappings.put("default", searchMappings);
    when(searchMappings.get()).thenReturn(searchMappings());
    when(conditionBuilders.getConditionBuilder(any(SearchFieldSupport.class)))
        .thenReturn(sqlSearchQueryConditionBuilder);
    underTest = new BlankValueSqlSearchQueryContribution(conditionBuilders, fieldMappings);
  }

  @Test
  public void shouldIgnoreNull() {
    underTest.contribute(queryBuilder, null);

    verifyNoMoreInteractions(sqlSearchQueryConditionBuilder);

    verify(queryBuilder, never()).add(any());
  }

  @Test
  public void testContribute() {
    Map<String, String> values = ImmutableMap.of("field0", "");

    when(sqlSearchQueryConditionBuilder
        .conditionWithEmptyValue(GROUP))
        .thenReturn(new SqlSearchQueryCondition(GROUP, values));

    underTest.contribute(queryBuilder, new SearchFilter(GROUP, ""));

    verify(queryBuilder).add(new SqlSearchQueryCondition(GROUP, values));
  }

  private Iterable<SearchMapping> searchMappings() {
    return singletonList(new SearchMapping("group", GROUP, "group", new ComponentSearchField(GROUP)));
  }
}

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

import java.util.HashMap;
import java.util.Map;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.rest.SearchFieldSupport;
import org.sonatype.nexus.repository.rest.SearchMapping;
import org.sonatype.nexus.repository.rest.SearchMappings;
import org.sonatype.nexus.repository.search.query.SearchFilter;
import org.sonatype.nexus.repository.search.sql.SqlSearchQueryBuilder;
import org.sonatype.nexus.repository.search.sql.SqlSearchQueryCondition;
import org.sonatype.nexus.repository.search.sql.SqlSearchQueryConditionBuilder;
import org.sonatype.nexus.repository.search.sql.SqlSearchQueryConditionBuilderMapping;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.content.maven.internal.search.table.MavenBaseVersionSqlSearchQueryContribution.BASE_VERSION;
import static org.sonatype.nexus.repository.rest.sql.ComponentSearchField.FORMAT_FIELD_1;

public class MavenBaseVersionSqlSearchQueryContributionTest
    extends TestSupport
{
  @Mock
  private SearchMappings searchMappings;

  @Mock
  private SqlSearchQueryConditionBuilder sqlSearchQueryConditionBuilder;

  @Mock
  private SqlSearchQueryConditionBuilderMapping builders;

  @Mock
  private SqlSearchQueryBuilder queryBuilder;

  private MavenBaseVersionSqlSearchQueryContribution underTest;

  @Before
  public void setup() {
    Map<String, SearchMappings> fieldMappings = new HashMap<>();
    fieldMappings.put("default", searchMappings);
    when(searchMappings.get()).thenReturn(searchMappings());
    when(builders.getConditionBuilder(any(SearchFieldSupport.class))).thenReturn(sqlSearchQueryConditionBuilder);
    underTest = new MavenBaseVersionSqlSearchQueryContribution(builders, fieldMappings);
  }

  @Test
  public void shouldContributeCustomisedBaseVersion() {
    String baseVersion = "1.0-snapshot";
    String storedBaseVersion = "/1.0-snapshot";
    Map<String, String> values = ImmutableMap.of("field0", storedBaseVersion);

    when(sqlSearchQueryConditionBuilder
        .condition(FORMAT_FIELD_1.getColumnName(), ImmutableSet.of(storedBaseVersion)))
        .thenReturn(new SqlSearchQueryCondition(FORMAT_FIELD_1.getColumnName(), values));

    underTest.contribute(queryBuilder, new SearchFilter(BASE_VERSION, baseVersion));

    verify(queryBuilder).add(new SqlSearchQueryCondition(FORMAT_FIELD_1.getColumnName(), values));
  }

  private Iterable<SearchMapping> searchMappings() {
    return singletonList(new SearchMapping("maven.baseVersion",
        BASE_VERSION, "Maven base version", FORMAT_FIELD_1));
  }
}

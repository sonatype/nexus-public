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
package org.sonatype.nexus.repository.content.search.table;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.content.store.FormatStoreManager;
import org.sonatype.nexus.repository.search.BlankValueSqlSearchQueryContribution;
import org.sonatype.nexus.repository.search.DefaultSqlSearchQueryContribution;
import org.sonatype.nexus.repository.search.SearchRequest;
import org.sonatype.nexus.repository.search.SqlSearchQueryContribution;
import org.sonatype.nexus.repository.search.sql.SqlSearchQueryBuilder;
import org.sonatype.nexus.repository.search.table.SqlSearchPermissionBuilder;
import org.sonatype.nexus.repository.search.table.TableSearchUtils;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.search.index.SearchConstants.REPOSITORY_NAME;

public class SqlSearchServiceTest
    extends TestSupport
{
  private static final String REPO_1 = "repo1";

  private static final String REPO_2 = "repo2";

  private static final String REPO_3 = "repo3";

  @Mock
  private SearchTableStore searchStore;

  @Mock
  private SqlSearchSortUtil sqlSearchSortUtil;

  @Mock
  private Set<TableSearchResultDecorator> decorators;

  @Mock
  private Map<String, FormatStoreManager> formatStoreManagersByFormat;

  @Mock
  private SqlSearchPermissionBuilder sqlSearchPermissionManager;

  private TableSearchUtils searchUtils = new TableSearchUtils(
      ImmutableMap.of(DefaultSqlSearchQueryContribution.NAME, mock(SqlSearchQueryContribution.class),
          BlankValueSqlSearchQueryContribution.NAME, mock(SqlSearchQueryContribution.class)));

  private SqlTableSearchService underTest;

  @Before
  public void setup() {
    underTest = new SqlTableSearchService(searchUtils, searchStore, sqlSearchSortUtil, formatStoreManagersByFormat,
        sqlSearchPermissionManager, decorators);
  }

  @Test
  public void shouldSplitRepositoryNameFilter() {
    String repositoryFilter = String.format("%s %s   %s", REPO_1, REPO_2, REPO_3);
    SearchRequest searchRequest = SearchRequest.builder().searchFilter(REPOSITORY_NAME, repositoryFilter).build();

    SqlSearchQueryBuilder queryBuilderWithPermission = mock(SqlSearchQueryBuilder.class);
    when(sqlSearchPermissionManager.build(any(), eq(searchRequest))).thenReturn(queryBuilderWithPermission);
    when(queryBuilderWithPermission.buildQuery()).thenReturn(Optional.empty());

    long count = 99L;
    when(searchStore.count(any())).thenReturn(count);

    assertEquals(underTest.count(searchRequest), count);

    verify(sqlSearchPermissionManager).build(any(), eq(searchRequest));
  }
}

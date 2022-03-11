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
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.group.GroupFacet;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.rest.SearchMappings;
import org.sonatype.nexus.repository.rest.internal.DefaultSearchMappings;
import org.sonatype.nexus.repository.search.query.SearchFilter;
import org.sonatype.nexus.repository.types.GroupType;
import org.sonatype.nexus.repository.types.HostedType;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static java.util.Optional.empty;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class RepositorySqlSearchQueryContributionTest
    extends TestSupport
{
  public static final String HOSTED_REPO_3 = "hosted-repo3";

  public static final String HOSTED_REPO_2 = "hosted-repo2";

  public static final String HOSTED_REPO_1 = "hosted-repo-1";

  public static final String REPOSITORY_NAME = "repository_name";

  public static final String GROUP_REPO = "group-repo";

  public static final String REPOSITORY = "repository";

  @Mock
  private SqlSearchQueryConditionBuilder sqlSearchQueryConditionBuilder;

  @Mock
  private SqlSearchQueryBuilder queryBuilder;

  @Mock
  private RepositoryManager repositoryManager;

  @Mock
  private Repository hostedRepo1;

  @Mock
  private Repository hostedRepo2;

  @Mock
  private Repository hostedRepo3;

  @Mock
  private Repository groupRepo;

  @Mock
  private GroupFacet groupFacet;

  @Mock
  private Type hostedType;

  @Mock
  private Type groupType;

  private RepositorySqlSearchQueryContribution underTest;

  @Before
  public void setup() {
    Map<String, SearchMappings> fieldMappings = new HashMap<>();
    fieldMappings.put("default", new DefaultSearchMappings());
    underTest =
        new RepositorySqlSearchQueryContribution(sqlSearchQueryConditionBuilder, fieldMappings, repositoryManager);
  }

  @Test
  public void shouldIgnoreNull() {
    underTest.contribute(queryBuilder, null);

    verifyZeroInteractions(sqlSearchQueryConditionBuilder);
    verify(queryBuilder, never()).add(any());
  }

  @Test
  public void shouldSearchLeafRepositoryWhenLeafRepositorySpecified() {
    final SearchFilter searchFilter = new SearchFilter(REPOSITORY_NAME, HOSTED_REPO_2 + " " + HOSTED_REPO_1);
    mockHostedRepo();
    final SqlSearchQueryCondition condition = mockCondition(ImmutableSet.of(HOSTED_REPO_1, HOSTED_REPO_2));

    underTest.contribute(queryBuilder, searchFilter);

    verify(queryBuilder).add(condition);
  }

  @Test
  public void shouldSearchLeafRepositoriesWhenGroupRepositorySpecified() {
    final SearchFilter searchFilter = new SearchFilter(REPOSITORY_NAME, GROUP_REPO);
    mockGroupRepo();
    final SqlSearchQueryCondition condition =
        mockCondition(ImmutableSet.of(HOSTED_REPO_1, HOSTED_REPO_2, HOSTED_REPO_3));

    underTest.contribute(queryBuilder, searchFilter);

    verify(queryBuilder).add(condition);
  }

  @Test
  public void shouldCreateConditionWhenUnknownRepositoryNameSpecified() {
    SearchFilter searchFilter = new SearchFilter(REPOSITORY_NAME, HOSTED_REPO_3);
    mockHostedRepo();
    SqlSearchQueryCondition condition = mockCondition(ImmutableSet.of(HOSTED_REPO_3));

    underTest.contribute(queryBuilder, searchFilter);

    verify(queryBuilder).add(condition);
  }

  private SqlSearchQueryCondition mockCondition(final Set<String> repositories) {
    final SqlSearchQueryCondition condition = new SqlSearchQueryCondition("condition", new HashMap<>());
    when(sqlSearchQueryConditionBuilder.condition(any(), eq(repositories))).thenReturn(condition);
    return condition;
  }

  private void mockGroupRepo() {
    when(repositoryManager.get(GROUP_REPO)).thenReturn(groupRepo);
    when(groupFacet.leafMembers()).thenReturn(ImmutableList.of(hostedRepo1, hostedRepo2, hostedRepo3));
    when(groupRepo.optionalFacet(GroupFacet.class)).thenReturn(Optional.of(groupFacet));
    when(hostedRepo1.getName()).thenReturn(HOSTED_REPO_1);
    when(hostedRepo2.getName()).thenReturn(HOSTED_REPO_2);
    when(hostedRepo3.getName()).thenReturn(HOSTED_REPO_3);
    mockHostedRepositoryType(hostedRepo1, hostedRepo2, hostedRepo3);
    when(groupRepo.getType()).thenReturn(groupType);
    when(groupType.getValue()).thenReturn(GroupType.NAME);
  }

  private void mockHostedRepo() {
    mockRepositoryManager();
    when(hostedRepo1.optionalFacet(GroupFacet.class)).thenReturn(empty());
    when(hostedRepo2.optionalFacet(GroupFacet.class)).thenReturn(empty());
    when(hostedRepo1.getName()).thenReturn(HOSTED_REPO_1);
    when(hostedRepo2.getName()).thenReturn(HOSTED_REPO_2);
    mockHostedRepositoryType(hostedRepo1, hostedRepo2);
  }

  private void mockHostedRepositoryType(Repository... repositories) {
    Stream.of(repositories)
        .forEach(repository -> when(repository.getType()).thenReturn(hostedType));
    when(hostedType.getValue()).thenReturn(HostedType.NAME);
  }

  private void mockRepositoryManager() {
    when(repositoryManager.get(HOSTED_REPO_1)).thenReturn(hostedRepo1);
    when(repositoryManager.get(HOSTED_REPO_2)).thenReturn(hostedRepo2);
  }
}

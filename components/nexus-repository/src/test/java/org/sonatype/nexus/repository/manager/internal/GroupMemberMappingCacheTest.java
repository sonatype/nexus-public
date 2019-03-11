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
package org.sonatype.nexus.repository.manager.internal;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.group.GroupFacet;
import org.sonatype.nexus.repository.manager.RepositoryCreatedEvent;
import org.sonatype.nexus.repository.manager.RepositoryDeletedEvent;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.manager.RepositoryUpdatedEvent;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GroupMemberMappingCacheTest
  extends TestSupport
{
  @Mock
  public RepositoryManager repositoryManager;

  private GroupMemberMappingCache underTest;

  @Before
  public void setup() {
    underTest = new GroupMemberMappingCache();
    underTest.init(repositoryManager);
  }

  @Test
  public void testGetGroups() {
    Repository repo1 = mockRepository("repo1");
    Repository repo2 = mockRepository("repo2");
    Repository group1 = mockGroupRepository("group1", repo1);

    when(repositoryManager.browse()).thenReturn(Arrays.asList(repo1, repo2, group1));

    List<String> groups = underTest.getGroups("repo1");
    assertThat(groups, contains("group1"));
    groups = underTest.getGroups("repo2");
    assertThat(groups, empty());
    groups = underTest.getGroups("repo3");
    assertThat(groups, empty());
  }

  @Test
  public void testGetGroups_mutableResponse() {
    Repository repo = mockRepository("repo");
    Repository group = mockGroupRepository("group", repo);

    when(repositoryManager.browse()).thenReturn(Arrays.asList(repo, group));

    List<String> groups = underTest.getGroups("repo");
    assertThat(groups, contains("group"));
    //add some fake item to the list
    //first it validates we can mutate the response if desired
    //second the next assert validates that we didn't touch the actual list in cache
    groups.add("fakevalue");
    groups = underTest.getGroups("repo");
    assertThat(groups, contains("group"));
  }

  @Test
  public void testGetGroups_repositoryContainedInMultipleGroupsWhichAlsoContainEachOther() {
    Repository repo = mockRepository("repo");
    Repository group1 = mockGroupRepository("group1", repo);
    Repository group2 = mockGroupRepository("group2", repo);
    Repository group3 = mockGroupRepository("group3", group1);
    Repository group4 = mockGroupRepository("group4", group3);
    Repository group5 = mockGroupRepository("group5", group4);

    //putting in reverse order just to prove out the expected sorting
    when(repositoryManager.browse()).thenReturn(Arrays.asList(group5, group4, group3, group2, group1, repo));

    List<String> groups = underTest.getGroups("repo");
    assertThat(groups, contains("group1", "group2", "group3", "group4", "group5"));
  }

  @Test
  public void testOnRepositoryCreatedEvent() {
    Repository repo = mockRepository("repo");
    Repository group = mockGroupRepository("group", repo);

    when(repositoryManager.browse()).thenReturn(Arrays.asList(repo, group));

    underTest.on(new RepositoryCreatedEvent(group));

    List<String> groups = underTest.getGroups("repo");

    assertThat(groups, contains("group"));
  }

  @Test
  public void testOnRepositoryCreatedEvent_nonGroupRepo() {
    Repository repo = mockRepository("repo");

    when(repositoryManager.browse()).thenReturn(Collections.emptyList());

    underTest.on(new RepositoryCreatedEvent(repo));

    verify(repositoryManager, never()).browse();
  }

  @Test
  public void testOnRepositoryUpdatedEvent() {
    Repository repo = mockRepository("repo");
    Repository repo2 = mockRepository("repo2");
    Repository group = mockGroupRepository("group", repo);

    when(repositoryManager.browse()).thenReturn(Arrays.asList(repo, repo2, group));

    List<String> groups = underTest.getGroups("repo2");

    assertThat(groups, empty());

    group = mockGroupRepository("group", repo2);

    when(repositoryManager.browse()).thenReturn(Arrays.asList(repo, repo2, group));

    underTest.on(new RepositoryUpdatedEvent(group));

    groups = underTest.getGroups("repo2");

    assertThat(groups, contains("group"));
  }

  @Test
  public void testOnRepositoryUpdatedEvent_nonGroupRepo() {
    Repository repo = mockRepository("repo");

    when(repositoryManager.browse()).thenReturn(Collections.emptyList());

    underTest.on(new RepositoryUpdatedEvent(repo));

    verify(repositoryManager, never()).browse();
  }

  @Test
  public void testOnRepositoryDeletedEvent() {
    Repository repo = mockRepository("repo");
    Repository repo2 = mockRepository("repo2");
    Repository group = mockGroupRepository("group", repo, repo2);

    when(repositoryManager.browse()).thenReturn(Arrays.asList(repo, repo2, group));

    List<String> groups = underTest.getGroups("repo2");

    assertThat(groups, contains("group"));

    when(repositoryManager.browse()).thenReturn(Arrays.asList(repo, repo2));

    underTest.on(new RepositoryDeletedEvent(group));

    groups = underTest.getGroups("repo2");

    assertThat(groups, empty());
  }

  @Test
  public void testOnRepositoryDeletedEvent_nonGroupRepo() {
    Repository repo = mockRepository("repo");
    Repository group = mockGroupRepository("group", repo);

    when(repositoryManager.browse()).thenReturn(Arrays.asList(repo, group));

    List<String> groups = underTest.getGroups("repo");

    assertThat(groups, contains("group"));

    when(repositoryManager.browse()).thenReturn(Collections.singletonList(group));

    underTest.on(new RepositoryDeletedEvent(repo));

    groups = underTest.getGroups("repo");

    assertThat(groups, empty());
  }

  private Repository mockRepository(String name) {
    Repository repository = mock(Repository.class);

    when(repository.getName()).thenReturn(name);
    when(repository.optionalFacet(GroupFacet.class)).thenReturn(Optional.empty());

    return repository;
  }

  private Repository mockGroupRepository(String name, Repository... members) {
    Repository repository = mock(Repository.class);

    when(repository.getName()).thenReturn(name);

    GroupFacet groupFacet = mock(GroupFacet.class);
    for (Repository member : members) {
      when(groupFacet.member(member.getName())).thenReturn(true);
    }

    when(repository.optionalFacet(GroupFacet.class)).thenReturn(Optional.of(groupFacet));

    return repository;
  }
}

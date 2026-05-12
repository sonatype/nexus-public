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

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.sonatype.nexus.common.stateguard.InvalidStateException;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.group.GroupFacet;
import org.sonatype.nexus.repository.manager.RepositoryCreatedEvent;
import org.sonatype.nexus.repository.manager.RepositoryDeletedEvent;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.manager.RepositoryUpdatedEvent;
import org.sonatype.nexus.repository.manager.internal.RepositoryImpl.State;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroupMemberMappingCacheTest
{
  @Mock
  RepositoryManager repositoryManager;

  private GroupMemberMappingCache underTest;

  @BeforeEach
  void setup() {
    underTest = new GroupMemberMappingCache();
  }

  @Test
  void testGetGroups() {
    Repository repo1 = mockRepository("repo1");
    Repository repo2 = mockRepository("repo2");
    Repository group1 = mockGroupRepository("group1", repo1);

    when(repositoryManager.browse()).thenReturn(List.of(repo1, repo2, group1));

    underTest.init(repositoryManager);
    Set<String> groups = underTest.getGroups("repo1");
    assertThat(groups, contains("group1"));
    groups = underTest.getGroups("repo2");
    assertThat(groups, empty());
    groups = underTest.getGroups("repo3");
    assertThat(groups, empty());
  }

  @Test
  void testGetGroups_mutableResponse() {
    Repository repo = mockRepository("repo");
    Repository group = mockGroupRepository("group", repo);

    when(repositoryManager.browse()).thenReturn(List.of(repo, group));

    underTest.init(repositoryManager);
    Set<String> groups = underTest.getGroups("repo");
    assertThat(groups, contains("group"));
    // add some fake item to the set
    // first it validates we can mutate the response if desired
    // second the next assert validates that we didn't touch the actual set in cache
    groups.add("fakevalue");
    groups = underTest.getGroups("repo");
    assertThat(groups, contains("group"));
  }

  @Test
  void testGetGroups_repositoryContainedInMultipleGroupsWhichAlsoContainEachOther() {
    Repository repo = mockRepository("repo");
    Repository group1 = mockGroupRepository("group1", repo);
    Repository group2 = mockGroupRepository("group2", repo);
    Repository group3 = mockGroupRepository("group3", group1, repo);
    Repository group4 = mockGroupRepository("group4", group3, group1, repo);
    Repository group5 = mockGroupRepository("group5", group4, group3, group1, repo);

    // putting in reverse order just to prove out the expected sorting
    when(repositoryManager.browse()).thenReturn(List.of(group5, group4, group3, group2, group1, repo));

    underTest.init(repositoryManager);
    Set<String> groups = underTest.getGroups("repo");
    assertThat(groups, containsInAnyOrder("group1", "group2", "group3", "group4", "group5"));
  }

  @Test
  void testOnRepositoryCreatedEvent() throws InterruptedException {
    Repository repo = mockRepository("repo");
    Repository group = mockGroupRepository("group", repo);

    when(repositoryManager.browse()).thenReturn(List.of(repo, group));

    underTest.init(repositoryManager);
    underTest.on(new RepositoryCreatedEvent(group));

    Set<String> groups = underTest.getGroups("repo");

    assertThat(groups, containsInAnyOrder("group"));
  }

  @Test
  void testOnRepositoryCreatedEvent_nonGroupRepo() throws InterruptedException {
    Repository repo = mockRepository("repo");

    underTest.init(repositoryManager);
    underTest.on(new RepositoryCreatedEvent(repo));

    verify(repo).optionalFacet(GroupFacet.class);
    verify(repo).getName();
    verifyNoMoreInteractions(repo);
  }

  @Test
  void testOnRepositoryUpdatedEvent() throws InterruptedException {
    Repository repo = mockRepository("repo");
    Repository repo2 = mockRepository("repo2");
    Repository repo3 = mockRepository("repo3");
    Repository group = mockGroupRepository("group", repo, repo3);
    Repository group2 = mockGroupRepository("group2", repo);

    when(repositoryManager.browse()).thenReturn(List.of(repo, repo2, repo3, group, group2));

    underTest.init(repositoryManager);
    Set<String> groups = underTest.getGroups("repo2");

    assertThat(groups, empty());

    when(group.facet(GroupFacet.class).allMembers()).thenReturn(List.of(repo2));

    underTest.on(new RepositoryUpdatedEvent(group, null));

    groups = underTest.getGroups("repo2");

    assertThat(groups, contains("group"));
  }

  @Test
  void testOnRepositoryUpdatedEvent_nonGroupRepo() throws InterruptedException {
    Repository repo = mockRepository("repo");

    underTest.init(repositoryManager);

    underTest.on(new RepositoryUpdatedEvent(repo, null));

    verify(repo).optionalFacet(GroupFacet.class);
    verify(repo).getName();
    verifyNoMoreInteractions(repo);
  }

  @Test
  void testOnRepositoryDeletedEvent() throws InterruptedException {
    Repository repo = mockRepository("repo");
    Repository repo2 = mockRepository("repo2");
    Repository group = mockGroupRepository("group", repo, repo2);

    when(repositoryManager.browse()).thenReturn(List.of(repo, repo2, group));

    underTest.init(repositoryManager);
    Set<String> groups = underTest.getGroups("repo2");

    assertThat(groups, contains("group"));

    underTest.on(new RepositoryDeletedEvent(group));

    groups = underTest.getGroups("repo2");

    assertThat(groups, empty());
    verify(repositoryManager, times(1)).browse();
  }

  @Test
  void testOnRepositoryDeletedEvent_nonGroupRepo() throws InterruptedException {
    Repository repo = mockRepository("repo");
    Repository group = mockGroupRepository("group", repo);

    when(repositoryManager.browse()).thenReturn(List.of(repo, group));

    underTest.init(repositoryManager);
    Set<String> groups = underTest.getGroups("repo");

    assertThat(groups, contains("group"));

    underTest.on(new RepositoryDeletedEvent(repo));

    groups = underTest.getGroups("repo");

    assertThat(groups, empty());
    verify(repositoryManager, times(1)).browse();
  }

  @Test
  void testWithRetry() {
    Supplier<String> supplier = mock();
    Supplier<String> defaultSupplier = mock();
    AtomicInteger counter = new AtomicInteger();
    when(supplier.get()).thenAnswer(i -> {
      if (counter.getAndAdd(1) <= 1) {
        throw new InvalidStateException("", new String[]{});
      }
      return "supplied";
    });

    String result = GroupMemberMappingCache.withRetry(supplier, defaultSupplier, 5);
    assertThat(result, is("supplied"));
    verify(supplier, times(3)).get();
    verifyNoInteractions(defaultSupplier);
  }

  @Test
  void testWithRetry_failsToDefault() {
    Supplier<String> supplier = mock();
    when(supplier.get()).thenThrow(new InvalidStateException(State.STOPPED, new String[]{}));
    Supplier<String> defaultSupplier = mock();
    when(defaultSupplier.get()).thenReturn("default");

    String result = GroupMemberMappingCache.withRetry(supplier, defaultSupplier, 5);
    assertThat(result, is("default"));
    verify(supplier, times(5)).get();
    verify(defaultSupplier).get();
  }

  @Test
  void testWithRetry_failureStatesDontRetry() {
    Supplier<String> supplier = mock();
    when(supplier.get()).thenAnswer(i -> {
      throw new InvalidStateException(State.DELETED, new String[]{});
    });
    Supplier<String> defaultSupplier = mock();
    when(defaultSupplier.get()).thenReturn("default");

    String result = GroupMemberMappingCache.withRetry(supplier, defaultSupplier, 5);
    assertThat(result, is("default"));
    verify(supplier, times(1)).get();
    verify(defaultSupplier).get();
  }

  private Repository mockRepository(final String name) {
    Repository repository = mock(Repository.class);

    lenient().when(repository.getName()).thenReturn(name);
    when(repository.optionalFacet(GroupFacet.class)).thenReturn(Optional.empty());

    lenient().when(repositoryManager.get(name)).thenReturn(repository);

    return repository;
  }

  private Repository mockGroupRepository(final String name, final Repository... members) {
    Repository repository = mock(Repository.class);

    when(repository.getName()).thenReturn(name);

    GroupFacet groupFacet = mock(GroupFacet.class);
    when(groupFacet.allMembers()).thenReturn(List.of(members));

    when(repository.optionalFacet(GroupFacet.class)).thenReturn(Optional.of(groupFacet));
    lenient().when(repository.facet(GroupFacet.class)).thenReturn(groupFacet);

    lenient().when(repositoryManager.get(name)).thenReturn(repository);

    return repository;
  }
}

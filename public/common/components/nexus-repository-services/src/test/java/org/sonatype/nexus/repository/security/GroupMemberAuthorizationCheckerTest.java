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
package org.sonatype.nexus.repository.security;

import java.util.HashMap;
import java.util.List;

import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.manager.RepositoryManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static java.util.Collections.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroupMemberAuthorizationCheckerTest
{
  @Mock
  private RepositoryManager repositoryManager;

  @Mock
  private RepositoryPermissionChecker repositoryPermissionChecker;

  private GroupMemberAuthorizationChecker underTest;

  @BeforeEach
  void setUp() {
    underTest = new GroupMemberAuthorizationChecker(repositoryManager, repositoryPermissionChecker);
    lenient().when(repositoryPermissionChecker.userCanReadOrBrowse(any())).thenReturn(true);
  }

  @Test
  void returnsEmptyWhenNoRequestedMembers() {
    assertThat(underTest.findUnauthorizedNewMembers("group", null), empty());
    assertThat(underTest.findUnauthorizedNewMembers("group", emptyList()), empty());
  }

  @Test
  void reportsNewMemberTheUserCannotReadOrBrowse() {
    Repository secret = mockRepository("secret");
    when(repositoryPermissionChecker.userCanReadOrBrowse(secret)).thenReturn(false);
    when(repositoryManager.get("group")).thenReturn(null);

    assertThat(underTest.findUnauthorizedNewMembers("group", List.of("secret")), contains("secret"));
  }

  @Test
  void allowsNewMemberTheUserCanReadOrBrowse() {
    Repository readable = mockRepository("readable");
    when(repositoryPermissionChecker.userCanReadOrBrowse(readable)).thenReturn(true);
    when(repositoryManager.get("group")).thenReturn(null);

    assertThat(underTest.findUnauthorizedNewMembers("group", List.of("readable")), empty());
  }

  @Test
  void grandfathersAlreadyStoredMemberEvenWhenUnreadable() {
    // a grandfathered member is never resolved or permission-checked, so it stays out of the result
    lenient().when(repositoryPermissionChecker.userCanReadOrBrowse(any())).thenReturn(false);
    mockGroup("group", List.of("secret"));

    assertThat(underTest.findUnauthorizedNewMembers("group", List.of("secret")), empty());
  }

  @Test
  void grandfatheringIgnoresCase() {
    lenient().when(repositoryPermissionChecker.userCanReadOrBrowse(any())).thenReturn(false);
    mockGroup("group", List.of("Secret"));

    assertThat(underTest.findUnauthorizedNewMembers("group", List.of("secret")), empty());
  }

  @Test
  void reportsUnresolvableMemberAsUnauthorized() {
    // Fail closed: a name that does not resolve to an existing repository must be rejected, never silently allowed.
    // The UI write paths do not pre-validate existence, so allowing it here would let a non-existent name be
    // persisted and then grandfathered forever (NEXUS-53747).
    lenient().when(repositoryPermissionChecker.userCanReadOrBrowse(any())).thenReturn(true);
    when(repositoryManager.get("group")).thenReturn(null);
    when(repositoryManager.get("ghost")).thenReturn(null);

    assertThat(underTest.findUnauthorizedNewMembers("group", List.of("ghost")), contains("ghost"));
  }

  private Repository mockRepository(final String name) {
    Repository repository = mock(Repository.class);
    when(repositoryManager.get(name)).thenReturn(repository);
    return repository;
  }

  private void mockGroup(final String name, final List<String> currentMemberNames) {
    Repository group = mock(Repository.class);
    Configuration configuration = mock(Configuration.class);
    NestedAttributesMap groupAttributes = new NestedAttributesMap("group", new HashMap<>());
    groupAttributes.set("memberNames", currentMemberNames);
    when(configuration.attributes("group")).thenReturn(groupAttributes);
    when(group.getConfiguration()).thenReturn(configuration);
    when(repositoryManager.get(name)).thenReturn(group);
  }
}

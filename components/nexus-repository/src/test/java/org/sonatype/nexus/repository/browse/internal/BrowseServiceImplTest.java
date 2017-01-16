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
package org.sonatype.nexus.repository.browse.internal;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.group.GroupFacet;
import org.sonatype.nexus.repository.security.ContentPermissionChecker;
import org.sonatype.nexus.repository.security.VariableResolverAdapterManager;
import org.sonatype.nexus.repository.storage.ComponentEntityAdapter;
import org.sonatype.nexus.repository.types.GroupType;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class BrowseServiceImplTest
    extends TestSupport
{
  @Mock
  ComponentEntityAdapter componentEntityAdapter;

  @Mock
  VariableResolverAdapterManager variableResolverAdapterManager;

  @Mock
  ContentPermissionChecker contentPermissionChecker;

  private BrowseServiceImpl underTest;

  @Before
  public void setup() {
    underTest = new BrowseServiceImpl(new GroupType(), componentEntityAdapter, variableResolverAdapterManager,
        contentPermissionChecker);
  }

  @Test
  public void testGetRepoToContainedGroupMap() {
    Repository repo1 = mock(Repository.class);
    when(repo1.getName()).thenReturn("repo1");
    when(repo1.optionalFacet(GroupFacet.class)).thenReturn(Optional.empty());
    Repository repo2 = mock(Repository.class);
    when(repo2.getName()).thenReturn("repo2");
    when(repo2.optionalFacet(GroupFacet.class)).thenReturn(Optional.empty());
    Repository group = mock(Repository.class);
    when(group.getName()).thenReturn("group");
    GroupFacet groupFacet = mock(GroupFacet.class);
    when(groupFacet.leafMembers()).thenReturn(Arrays.asList(repo1));
    when(group.optionalFacet(GroupFacet.class)).thenReturn(Optional.of(groupFacet));

    List<Repository> repositories = Arrays.asList(repo1, repo2, group);

    Map<String,List<String>> repoToContainedGroups = underTest.getRepoToContainedGroupMap(repositories);
    assertThat(repoToContainedGroups.size(), is(3));
    assertThat(repoToContainedGroups.get("repo1").size(), is(2));
    assertThat(repoToContainedGroups.get("repo1").get(0), is("repo1"));
    assertThat(repoToContainedGroups.get("repo1").get(1), is("group"));
    assertThat(repoToContainedGroups.get("repo2").size(), is(1));
    assertThat(repoToContainedGroups.get("repo2").get(0), is("repo2"));
    assertThat(repoToContainedGroups.get("group").size(), is(1));
    assertThat(repoToContainedGroups.get("group").get(0), is("group"));
  }
}

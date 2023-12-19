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
package org.sonatype.nexus.repository.content.fluent.constraints;

import java.util.Set;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.group.GroupFacet;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.content.fluent.constraints.GroupRepositoryConstraint.GroupRepositoryLocation.BOTH;
import static org.sonatype.nexus.repository.content.fluent.constraints.GroupRepositoryConstraint.GroupRepositoryLocation.LOCAL;
import static org.sonatype.nexus.repository.content.fluent.constraints.GroupRepositoryConstraint.GroupRepositoryLocation.MEMBERS;

public class GroupRepositoryConstraintTest
    extends TestSupport
{
  @Mock
  private Repository repository;

  @Mock
  private Repository leafMember1;

  @Mock
  private Repository leafMember2;

  @Mock
  private GroupFacet groupFacet;

  @Mock
  private ContentFacet groupContentFacet;

  @Mock
  private ContentFacet member1ContentFacet;

  @Mock
  private ContentFacet member2ContentFacet;

  private GroupRepositoryConstraint underTest;

  @Before
  public void setup() {
    when(repository.facet(GroupFacet.class)).thenReturn(groupFacet);
    when(repository.facet(ContentFacet.class)).thenReturn(groupContentFacet);
    when(groupFacet.leafMembers()).thenReturn(asList(leafMember1, leafMember2));
    when(leafMember1.facet(ContentFacet.class)).thenReturn(member1ContentFacet);
    when(leafMember2.facet(ContentFacet.class)).thenReturn(member2ContentFacet);
    when(groupContentFacet.contentRepositoryId()).thenReturn(1);
    when(member1ContentFacet.contentRepositoryId()).thenReturn(2);
    when(member2ContentFacet.contentRepositoryId()).thenReturn(3);
  }

  @Test
  public void testGetRepositoryIds_local() {
    underTest = new GroupRepositoryConstraint(LOCAL);
    Set<Integer> repositoryIds = underTest.getRepositoryIds(repository);

    assertThat(repositoryIds, contains(1));
  }

  @Test
  public void testGetRepositoryIds_members() {
    underTest = new GroupRepositoryConstraint(MEMBERS);
    Set<Integer> repositoryIds = underTest.getRepositoryIds(repository);

    assertThat(repositoryIds, contains(2, 3));
  }

  @Test
  public void testGetRepositoryIds_both() {
    underTest = new GroupRepositoryConstraint(BOTH);
    Set<Integer> repositoryIds = underTest.getRepositoryIds(repository);

    assertThat(repositoryIds, contains(1, 2, 3));
  }
}

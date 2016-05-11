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
package org.sonatype.nexus.repository.group;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;

import javax.validation.ConstraintViolation;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.group.GroupFacetImpl.Config;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.types.GroupType;
import org.sonatype.nexus.validation.ConstraintViolationFactory;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GroupFacetImplTest
    extends TestSupport
{
  private ConstraintViolationFactory constraintViolationFactory;

  private GroupType groupType = new GroupType();

  private RepositoryManager repositoryManager;

  private GroupFacetImpl groupFacetImpl;

  @Before
  public void setup() throws Exception {
    repositoryManager = mock(RepositoryManager.class);
    constraintViolationFactory = mock(ConstraintViolationFactory.class);
    groupFacetImpl = new GroupFacetImpl(repositoryManager, constraintViolationFactory, groupType);

    when(constraintViolationFactory.createViolation(anyString(), anyString())).thenReturn(mock(ConstraintViolation.class));

    Repository repositoryUnderTest = mock(Repository.class);
    Repository repository1 = mock(Repository.class);
    Repository repository2 = mock(Repository.class);
    Repository repository3 = mock(Repository.class);
    GroupFacet groupFacet1 = mock(GroupFacet.class);
    GroupFacet groupFacet2 = mock(GroupFacet.class);
    GroupFacet groupFacet3 = mock(GroupFacet.class);
    when(groupFacet1.members()).thenReturn(Collections.emptyList());
    when(groupFacet2.members()).thenReturn(Arrays.asList(repository3));
    when(groupFacet3.members()).thenReturn(Arrays.asList(repository1, repositoryUnderTest));
    when(repositoryUnderTest.facet(GroupFacet.class)).thenReturn(groupFacetImpl);
    when(repository1.facet(GroupFacet.class)).thenReturn(groupFacet1);
    when(repository2.facet(GroupFacet.class)).thenReturn(groupFacet2);
    when(repository3.facet(GroupFacet.class)).thenReturn(groupFacet3);
    when(repositoryUnderTest.getType()).thenReturn(groupType);
    when(repository1.getType()).thenReturn(groupType);
    when(repository2.getType()).thenReturn(groupType);
    when(repository3.getType()).thenReturn(groupType);
    when(repositoryUnderTest.getName()).thenReturn("repositoryUnderTest");
    when(repository1.getName()).thenReturn("repository1");
    when(repository2.getName()).thenReturn("repository2");
    when(repository3.getName()).thenReturn("repository3");

    when(repositoryManager.get("repositoryUnderTest")).thenReturn(repositoryUnderTest);
    when(repositoryManager.get("repository1")).thenReturn(repository1);
    when(repositoryManager.get("repository2")).thenReturn(repository2);
    when(repositoryManager.get("repository3")).thenReturn(repository3);

    groupFacetImpl.attach(repositoryUnderTest);
  }

  @Test
  public void testDoValidate_pass() {
    Config config = new Config();
    config.memberNames = new LinkedHashSet<>();
    config.memberNames.add("repository1");
    assertNull(groupFacetImpl.validateGroupDoesNotContainItself("repositoryUnderTest", config));
  }

  @Test
  public void testDoValidate_fail() {
    //the group contains itself
    Config config = new Config();
    config.memberNames = new LinkedHashSet<>();
    config.memberNames.add("repositoryUnderTest");
    assertNotNull(groupFacetImpl.validateGroupDoesNotContainItself("repositoryUnderTest", config));

    //the group contains a group that contains itself
    config.memberNames = new LinkedHashSet<>();
    config.memberNames.add("repository3");
    assertNotNull(groupFacetImpl.validateGroupDoesNotContainItself("repositoryUnderTest", config));

    //the group contains a group, which contains a group, which contains itself
    config.memberNames = new LinkedHashSet<>();
    config.memberNames.add("repository2");
    assertNotNull(groupFacetImpl.validateGroupDoesNotContainItself("repositoryUnderTest", config));
  }
}

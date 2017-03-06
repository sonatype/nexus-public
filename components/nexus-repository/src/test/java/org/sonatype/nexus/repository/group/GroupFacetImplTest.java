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
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.config.ConfigurationFacet;
import org.sonatype.nexus.repository.group.GroupFacetImpl.Config;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.types.GroupType;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.validation.ConstraintViolationFactory;

import org.junit.Before;
import org.junit.Test;

import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.ImmutableSet.of;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.group.GroupFacetImpl.CONFIG_KEY;

public class GroupFacetImplTest
    extends TestSupport
{
  private ConstraintViolationFactory constraintViolationFactory;

  private GroupType groupType = new GroupType();

  private RepositoryManager repositoryManager;

  private ConfigurationFacet configurationFacet;

  private Format format = mock(Format.class);

  private GroupFacetImpl groupFacetImpl;

  @Before
  public void setup() throws Exception {
    repositoryManager = mock(RepositoryManager.class);
    constraintViolationFactory = mock(ConstraintViolationFactory.class);
    configurationFacet = mock(ConfigurationFacet.class);
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
    when(repositoryUnderTest.facet(ConfigurationFacet.class)).thenReturn(configurationFacet);
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
    when(repositoryUnderTest.getFormat()).thenReturn(format);

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

  @Test
  public void testLeafMembers() throws Exception {
    Repository hosted1 = hostedRepository("hosted1");
    Repository hosted2 = hostedRepository("hosted2");
    Repository group1 = groupRepository("group1", hosted1);
    Config config = new Config();
    config.memberNames = of(hosted1.getName(), hosted2.getName(), group1.getName());
    Configuration configuration = new Configuration();
    configuration.attributes(CONFIG_KEY).set("memberNames", config.memberNames);
    when(configurationFacet.readSection(configuration, CONFIG_KEY, Config.class)).thenReturn(config);
    groupFacetImpl.doConfigure(configuration);
    assertThat(groupFacetImpl.leafMembers(), contains(hosted1, hosted2));
  }

  private Repository hostedRepository(final String name) {
    Repository hostedRepository = mock(Repository.class);
    when(hostedRepository.getType()).thenReturn(new HostedType());
    when(hostedRepository.getName()).thenReturn(name);
    when(hostedRepository.getFormat()).thenReturn(format);
    when(repositoryManager.get(name)).thenReturn(hostedRepository);
    return hostedRepository;
  }

  private Repository groupRepository(final String name, final Repository... repositories) {
    Repository groupRepository = mock(Repository.class);
    when(groupRepository.getType()).thenReturn(groupType);
    when(groupRepository.getName()).thenReturn(name);
    when(groupRepository.getFormat()).thenReturn(format);
    when(repositoryManager.get(name)).thenReturn(groupRepository);
    GroupFacet groupFacet = mock(GroupFacet.class);
    when(groupRepository.facet(GroupFacet.class)).thenReturn(groupFacet);
    when(groupFacet.leafMembers()).thenReturn(copyOf(repositories));
    return groupRepository;
  }
}

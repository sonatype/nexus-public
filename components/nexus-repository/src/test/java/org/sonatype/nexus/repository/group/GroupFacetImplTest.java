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

import java.util.Optional;

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

import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static com.google.common.collect.ImmutableList.copyOf;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.group.GroupFacetImpl.CONFIG_KEY;

public class GroupFacetImplTest
    extends TestSupport
{
  @Mock
  private RepositoryManager repositoryManager;

  @Mock
  private ConfigurationFacet configurationFacet;

  @Mock
  private Format format = mock(Format.class);

  private GroupType groupType = new GroupType();

  private GroupFacetImpl underTest;

  @Before
  public void setup() throws Exception {
    underTest = new GroupFacetImpl(repositoryManager, makeConstraintViolationFactory(), groupType);
    underTest.attach(makeRepositoryUnderTest());
  }

  @Test
  public void testDoValidate_pass() {
    Config config = new Config();
    config.memberNames = ImmutableSet.of("repository1");
    assertNull(underTest.validateGroupDoesNotContainItself("repositoryUnderTest", config));
  }

  @Test
  public void testDoValidate_fail_group_contains_itself() {
    Config config = new Config();
    config.memberNames = ImmutableSet.of("repositoryUnderTest");
    assertNotNull(underTest.validateGroupDoesNotContainItself("repositoryUnderTest", config));
  }

  @Test
  public void testDoValidate_fail_group_contains_a_group_that_contains_itself() {
    Config config = new Config();
    config.memberNames = ImmutableSet.of("repository3");
    assertNotNull(underTest.validateGroupDoesNotContainItself("repositoryUnderTest", config));
  }

  @Test
  public void testDoValidate_fail_group_contains_a_group_which_contains_a_group_which_contains_itself() {
    Config config = new Config();
    config.memberNames = ImmutableSet.of("repository2");
    assertNotNull(underTest.validateGroupDoesNotContainItself("repositoryUnderTest", config));
  }

  @Test
  public void testLeafMembers() throws Exception {
    Repository hosted1 = hostedRepository("hosted1");
    Repository hosted2 = hostedRepository("hosted2");
    Repository group1 = groupRepository("group1", hosted1);
    Config config = new Config();
    config.memberNames = ImmutableSet.of(hosted1.getName(), hosted2.getName(), group1.getName());
    Configuration configuration = new Configuration();
    configuration.attributes(CONFIG_KEY).set("memberNames", config.memberNames);
    when(configurationFacet.readSection(configuration, CONFIG_KEY, Config.class)).thenReturn(config);
    underTest.doConfigure(configuration);
    assertThat(underTest.leafMembers(), contains(hosted1, hosted2));
  }

  @Test
  public void testAllMembers() throws Exception {
    Repository hosted1 = hostedRepository("hosted1");
    Repository group1 = groupRepository("group1", hosted1);
    underTest.attach(group1);

    for (Repository repo : underTest.allMembers()) {
      System.out.println(repo.getName());
    }
    assertThat(underTest.allMembers(), contains(group1, hosted1));
  }

  private ConstraintViolationFactory makeConstraintViolationFactory() {
    ConstraintViolationFactory constraintViolationFactory = mock(ConstraintViolationFactory.class);
    doReturn(mock(ConstraintViolation.class))
        .when(constraintViolationFactory).createViolation(anyString(), anyString());
    return constraintViolationFactory;
  }

  private Repository makeRepositoryUnderTest() {
    Repository repositoryUnderTest = groupRepository("repositoryUnderTest");
    when(repositoryUnderTest.facet(GroupFacet.class)).thenReturn(underTest);
    when(repositoryUnderTest.facet(ConfigurationFacet.class)).thenReturn(configurationFacet);

    groupRepository("repository2",
        groupRepository("repository3",
            repositoryUnderTest,
            groupRepository("repository1")
        )
    );
    return repositoryUnderTest;
  }

  private Repository hostedRepository(final String name) {
    Repository hostedRepository = mock(Repository.class);
    when(hostedRepository.getType()).thenReturn(new HostedType());
    when(hostedRepository.getName()).thenReturn(name);
    when(hostedRepository.getFormat()).thenReturn(format);
    when(hostedRepository.optionalFacet(GroupFacet.class)).thenReturn(Optional.empty());
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
    when(groupRepository.optionalFacet(GroupFacet.class)).thenReturn(Optional.of(groupFacet));
    when(groupFacet.members()).thenReturn(copyOf(repositories));
    return groupRepository;
  }
}

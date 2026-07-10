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

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import jakarta.validation.ConstraintViolation;

import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.distributed.event.service.api.common.RepositoryCacheSyncTokenEvent;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.cache.CacheController;
import org.sonatype.nexus.repository.cache.CacheInfo;
import org.sonatype.nexus.repository.cache.RepositoryCacheInvalidationService;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.config.ConfigurationFacet;
import org.sonatype.nexus.repository.group.GroupFacetImpl.Config;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.manager.RepositoryAttributeService;
import org.sonatype.nexus.repository.types.GroupType;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.validation.ConstraintViolationFactory;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static com.google.common.collect.ImmutableList.copyOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.group.GroupFacetImpl.CONFIG_KEY;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.Silent.class)
public class GroupFacetImplTest
{
  @Mock
  private RepositoryManager repositoryManager;

  @Mock
  private ConfigurationFacet configurationFacet;

  @Mock
  private Format format = mock(Format.class);

  @Mock
  private Content content;

  @Mock
  private AttributesMap attributesMap;

  @Mock
  private CacheInfo cacheInfo;

  @Mock
  private RepositoryCacheInvalidationService repositoryCacheInvalidationService;

  @Mock
  private RepositoryAttributeService repositoryAttributeService;

  private Repository repository;

  private GroupType groupType = new GroupType();

  private GroupFacetImpl underTest;

  @Before
  public void setup() throws Exception {
    underTest = new GroupFacetImpl(repositoryManager, makeConstraintViolationFactory(), groupType,
        repositoryCacheInvalidationService);
    underTest.installDependencies(mock(EventManager.class));
    repository = makeRepositoryUnderTest();
    underTest.attach(repository);
  }

  @Test
  public void testDoValidate_pass() {
    Config config = new Config();
    config.memberNames = Set.of("repository1");
    assertNull(underTest.validateGroupDoesNotContainItself("repositoryUnderTest", config));
  }

  @Test
  public void testDoValidate_fail_group_contains_itself() {
    Config config = new Config();
    config.memberNames = Set.of("repositoryUnderTest");
    assertNotNull(underTest.validateGroupDoesNotContainItself("repositoryUnderTest", config));
  }

  @Test
  public void testDoValidate_fail_group_contains_a_group_that_contains_itself() {
    Config config = new Config();
    config.memberNames = Set.of("repository3");
    assertNotNull(underTest.validateGroupDoesNotContainItself("repositoryUnderTest", config));
  }

  @Test
  public void testDoValidate_fail_group_contains_a_group_which_contains_a_group_which_contains_itself() {
    Config config = new Config();
    config.memberNames = Set.of("repository2");
    assertNotNull(underTest.validateGroupDoesNotContainItself("repositoryUnderTest", config));
  }

  @Test
  public void testDoValidate_pass_when_member_repository_does_not_exist() {
    // Simulate HA race condition where a member repository is being deleted concurrently
    // The repository is not mocked, so repositoryManager.get() will return null
    Config config = new Config();
    config.memberNames = Set.of("non-existent-repository", "repository1");

    // Before the fix: this would throw NullPointerException
    // After the fix: this should return null (no violation) and skip the deleted repository
    assertNull(underTest.validateGroupDoesNotContainItself("repositoryUnderTest", config));
  }

  @Test
  public void testLeafMembers() throws Exception {
    Repository hosted1 = hostedRepository("hosted1");
    Repository hosted2 = hostedRepository("hosted2");
    Repository group1 = groupRepository("group1", hosted1);
    Config config = new Config();
    config.memberNames = Set.of(hosted1.getName(), hosted2.getName(), group1.getName());
    Configuration configuration = mock(Configuration.class);
    when(configuration.attributes(CONFIG_KEY)).thenReturn(new NestedAttributesMap(
        "dummy",
        Map.of("memberNames", config.memberNames)));
    when(configurationFacet.readSection(configuration, CONFIG_KEY, Config.class)).thenReturn(config);
    when(repository.getConfiguration()).thenReturn(configuration);
    when(repositoryAttributeService.getRepositoryAttribute(repository, "cacheToken", null)).thenReturn(null);
    underTest.setRepositoryAttributeService(repositoryAttributeService);
    underTest.init();
    underTest.start();
    assertThat(underTest.leafMembers(), containsInAnyOrder(hosted1, hosted2));
  }

  @Test
  public void testAllMembers() throws Exception {
    Repository hosted1 = hostedRepository("hosted1");
    Repository group1 = groupRepository("group1", hosted1);

    underTest = new GroupFacetImpl(repositoryManager, makeConstraintViolationFactory(), groupType,
        repositoryCacheInvalidationService);
    underTest.attach(group1);

    for (Repository repo : underTest.allMembers()) {
      System.out.println(repo.getName());
    }
    assertThat(underTest.allMembers(), contains(group1, hosted1));
  }

  @Test
  public void whenContentIsNullIsStale() {
    assertThat(underTest.isStale(null), is(true));
  }

  @Test
  public void whenCacheInfoIsNullThenIsStale() {
    when(content.getAttributes()).thenReturn(attributesMap);
    when(attributesMap.get(CacheInfo.class)).thenReturn(null);

    assertThat(underTest.isStale(content), is(true));
  }

  @Test
  public void whenCachePresentTheNotStale() {
    when(content.getAttributes()).thenReturn(attributesMap);
    when(attributesMap.get(CacheInfo.class)).thenReturn(cacheInfo);
    CacheController cacheController = mock(CacheController.class);
    underTest.cacheController = cacheController;
    when(cacheController.isStale(cacheInfo)).thenReturn(false);

    assertThat(underTest.isStale(content), is(false));
  }

  @Test
  public void testDoConfigure_WithStoredToken() throws Exception {
    String storedToken = "stored-cache-token";
    Config config = new Config();
    config.memberNames = Set.of("repository1");
    Configuration configuration = mock(Configuration.class);

    when(configurationFacet.readSection(configuration, CONFIG_KEY, Config.class)).thenReturn(config);
    when(repositoryAttributeService.getRepositoryAttribute(repository, "cacheToken", null)).thenReturn(storedToken);

    underTest.setRepositoryAttributeService(repositoryAttributeService);
    underTest.doConfigure(configuration);

    assertNotNull(underTest.cacheController);
    verify(repositoryAttributeService).getRepositoryAttribute(repository, "cacheToken", null);
  }

  @Test
  public void testDoConfigure_WithNullStoredToken() throws Exception {
    Config config = new Config();
    config.memberNames = Set.of("repository1");
    Configuration configuration = mock(Configuration.class);

    when(configurationFacet.readSection(configuration, CONFIG_KEY, Config.class)).thenReturn(config);
    when(repositoryAttributeService.getRepositoryAttribute(repository, "cacheToken", null)).thenReturn(null);

    underTest.setRepositoryAttributeService(repositoryAttributeService);
    underTest.doConfigure(configuration);

    assertNotNull(underTest.cacheController);
    verify(repositoryAttributeService).getRepositoryAttribute(repository, "cacheToken", null);
  }

  @Test
  public void testOnRepositoryCacheSyncTokenEventIgnoresDifferentRepository() {
    RepositoryCacheSyncTokenEvent event = mock(RepositoryCacheSyncTokenEvent.class);
    when(event.isLocal()).thenReturn(false);
    when(event.getRepositoryName()).thenReturn("different-repository");

    CacheController mockCacheController = mock(CacheController.class);
    underTest.cacheController = mockCacheController;

    underTest.on(event);

    verify(mockCacheController, org.mockito.Mockito.never()).setCache(org.mockito.ArgumentMatchers.any());
  }

  private static ConstraintViolationFactory makeConstraintViolationFactory() {
    ConstraintViolationFactory constraintViolationFactory = mock(ConstraintViolationFactory.class);
    doReturn(mock(ConstraintViolation.class))
        .when(constraintViolationFactory)
        .createViolation(anyString(), anyString());
    return constraintViolationFactory;
  }

  private Repository makeRepositoryUnderTest() {
    Repository repositoryUnderTest = groupRepository("repositoryUnderTest");
    when(repositoryUnderTest.facet(GroupFacet.class)).thenReturn(underTest);
    when(repositoryUnderTest.facet(ConfigurationFacet.class)).thenReturn(configurationFacet);

    groupRepository("repository2",
        groupRepository("repository3",
            repositoryUnderTest,
            groupRepository("repository1")));
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

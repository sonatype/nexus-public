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

import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.event.EventHelper;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.cache.CacheController;
import org.sonatype.nexus.repository.cache.CacheInfo;
import org.sonatype.nexus.repository.cache.RepositoryCacheInvalidationService;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.config.ConfigurationFacet;
import org.sonatype.nexus.repository.content.event.repository.ContentRepositoryAttributesDesEvent;
import org.sonatype.nexus.repository.group.GroupFacetImpl.Config;
import org.sonatype.nexus.repository.manager.RepositoryAttributeService;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.types.GroupType;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.validation.ConstraintViolationFactory;

import jakarta.validation.ConstraintViolation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.google.common.collect.ImmutableList.copyOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.group.GroupFacetImpl.CONFIG_KEY;

@ExtendWith(MockitoExtension.class)
public class GroupFacetImplTest
{
  @Mock
  private RepositoryManager repositoryManager;

  @Mock
  private ConfigurationFacet configurationFacet;

  @Mock
  private Format format;

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

  @BeforeEach
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
    lenient().when(configuration.attributes(CONFIG_KEY))
        .thenReturn(new NestedAttributesMap("dummy", Map.of("memberNames", config.memberNames)));
    when(configurationFacet.readSection(configuration, CONFIG_KEY, Config.class)).thenReturn(config);
    when(repository.getConfiguration()).thenReturn(configuration);

    when(repositoryAttributeService.getRepositoryAttribute(eq(repository),
        eq(FacetSupport.METADATA_CACHE_TOKEN_ATTRIBUTE), any())).thenReturn(null);
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
  public void testStart_WithStoredToken() throws Exception {
    String metadataStoredToken = "stored-metadata-cache-token";
    when(repositoryAttributeService.getRepositoryAttribute(eq(repository),
        eq(FacetSupport.METADATA_CACHE_TOKEN_ATTRIBUTE), any())).thenReturn(metadataStoredToken);

    underTest.init();
    underTest.start();

    assertNotNull(underTest.cacheController);
    assertThat(underTest.cacheController.current().getCacheToken(), is(metadataStoredToken));
  }

  @Test
  public void testStart_WithNullStoredToken() throws Exception {
    when(repositoryAttributeService.getRepositoryAttribute(eq(repository),
        eq(FacetSupport.METADATA_CACHE_TOKEN_ATTRIBUTE), any())).thenReturn(null);

    underTest.init();
    underTest.start();

    assertNotNull(underTest.cacheController);
    assertThat(underTest.cacheController.current().getCacheToken(), is(nullValue()));
  }

  @Test
  public void testOnRepositoryCacheSyncTokenEvent() {
    CacheController mockCacheController = mock();
    underTest.cacheController = mockCacheController;
    when(repositoryAttributeService.getRepositoryAttribute(eq(repository),
        eq(FacetSupport.METADATA_CACHE_TOKEN_ATTRIBUTE), any())).thenReturn("123");

    // Different repository
    EventHelper.asReplicating(
        () -> underTest.on(new ContentRepositoryAttributesDesEvent("different-repository", "cacheToken")));
    verify(mockCacheController, never()).setCache(any());

    // Local event
    underTest.on(new ContentRepositoryAttributesDesEvent("repositoryUnderTest", "cacheToken"));
    verify(mockCacheController, never()).setCache(any());

    // Different repository
    EventHelper.asReplicating(
        () -> underTest.on(new ContentRepositoryAttributesDesEvent("repositoryUnderTest", "cacheToken")));
    verify(mockCacheController).setCache("123");
  }

  private static ConstraintViolationFactory makeConstraintViolationFactory() {
    ConstraintViolationFactory constraintViolationFactory = mock(ConstraintViolationFactory.class);
    lenient().doReturn(mock(ConstraintViolation.class))
        .when(constraintViolationFactory)
        .createViolation(anyString(), anyString());
    return constraintViolationFactory;
  }

  private Repository makeRepositoryUnderTest() {
    Repository repositoryUnderTest = groupRepository("repositoryUnderTest");
    lenient().when(repositoryUnderTest.facet(GroupFacet.class)).thenReturn(underTest);
    lenient().when(repositoryUnderTest.facet(ConfigurationFacet.class)).thenReturn(configurationFacet);
    underTest.setRepositoryAttributeService(repositoryAttributeService);

    groupRepository("repository2",
        groupRepository("repository3",
            repositoryUnderTest,
            groupRepository("repository1")));
    return repositoryUnderTest;
  }

  private Repository hostedRepository(final String name) {
    Repository hostedRepository = mock(Repository.class);
    lenient().when(hostedRepository.getType()).thenReturn(new HostedType());
    lenient().when(hostedRepository.getName()).thenReturn(name);
    lenient().when(hostedRepository.getFormat()).thenReturn(format);
    lenient().when(hostedRepository.optionalFacet(GroupFacet.class)).thenReturn(Optional.empty());
    lenient().when(repositoryManager.get(name)).thenReturn(hostedRepository);
    return hostedRepository;
  }

  private Repository groupRepository(final String name, final Repository... repositories) {
    Repository groupRepository = mock(Repository.class);
    lenient().when(groupRepository.getType()).thenReturn(groupType);
    lenient().when(groupRepository.getName()).thenReturn(name);
    lenient().when(groupRepository.getFormat()).thenReturn(format);
    lenient().when(repositoryManager.get(name)).thenReturn(groupRepository);
    GroupFacet groupFacet = mock(GroupFacet.class);
    lenient().when(groupRepository.facet(GroupFacet.class)).thenReturn(groupFacet);
    lenient().when(groupRepository.optionalFacet(GroupFacet.class)).thenReturn(Optional.of(groupFacet));
    lenient().when(groupFacet.members()).thenReturn(copyOf(repositories));
    return groupRepository;
  }
}

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
package org.sonatype.nexus.repository.manager.internal

import javax.inject.Provider

import org.sonatype.goodies.testsupport.TestSupport
import org.sonatype.nexus.blobstore.api.BlobStoreManager
import org.sonatype.nexus.common.collect.NestedAttributesMap
import org.sonatype.nexus.common.entity.EntityMetadata
import org.sonatype.nexus.common.event.EventManager
import org.sonatype.nexus.common.node.NodeAccess
import org.sonatype.nexus.orient.freeze.DatabaseFreezeService
import org.sonatype.nexus.repository.Format
import org.sonatype.nexus.repository.Recipe
import org.sonatype.nexus.repository.Repository
import org.sonatype.nexus.repository.Type
import org.sonatype.nexus.repository.config.Configuration
import org.sonatype.nexus.repository.config.ConfigurationFacet
import org.sonatype.nexus.repository.config.internal.ConfigurationStore
import org.sonatype.nexus.repository.group.GroupFacet
import org.sonatype.nexus.repository.manager.DefaultRepositoriesContributor
import org.sonatype.nexus.repository.manager.RepositoryMetadataUpdatedEvent
import org.sonatype.nexus.repository.storage.internal.BucketUpdatedEvent

import com.google.common.collect.ImmutableMap
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mock

import static com.google.common.collect.Lists.asList
import static com.google.common.collect.Maps.newHashMap
import static java.util.Collections.emptyList
import static java.util.Collections.singletonList
import static java.util.UUID.randomUUID
import static java.util.stream.Collectors.toList
import static org.fest.assertions.api.Assertions.assertThat
import static org.hamcrest.Matchers.contains
import static org.hamcrest.Matchers.empty
import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.instanceOf
import static org.junit.Assert.assertFalse
import static org.mockito.Matchers.any
import static org.mockito.Matchers.isA
import static org.mockito.Mockito.atLeastOnce
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.never
import static org.mockito.Mockito.times
import static org.mockito.Mockito.verify
import static org.mockito.Mockito.when
import static org.sonatype.nexus.blobstore.api.BlobStoreManager.DEFAULT_BLOBSTORE_NAME
import static org.sonatype.nexus.repository.manager.internal.RepositoryManagerImpl.CLEANUP_ATTRIBUTES_KEY
import static org.sonatype.nexus.repository.manager.internal.RepositoryManagerImpl.CLEANUP_NAME_KEY

class RepositoryManagerImplTest
    extends TestSupport
{
  static final String GROUP_NAME = 'group'

  static final String PARENT_GROUP_NAME = 'parentGroup'

  static final String CYCLE_A_NAME = 'cycleA'

  static final String CYCLE_B_NAME = 'cycleB'

  static final String MAVEN_CENTRAL_NAME = 'maven-central'

  static final String APACHE_SNAPSHOTS_NAME = 'apache-snapshots'

  static final String THIRD_PARTY_NAME = 'third-party'

  static final String UNGROUPED_REPO_NAME = 'upgrouped'

  @Mock
  EventManager eventManager

  @Mock
  ConfigurationStore configurationStore

  @Mock
  DatabaseFreezeService databaseFreezeService

  @Mock
  RepositoryFactory repositoryFactory

  @Mock
  Provider<ConfigurationFacet> configurationFacetProvider

  @Mock
  RepositoryAdminSecurityContributor securityContributor

  List<DefaultRepositoriesContributor> defaultRepositoriesContributorList

  @Mock
  DefaultRepositoriesContributor defaultRepositoriesContributor

  @Mock
  Configuration mavenCentralConfiguration

  @Mock
  Repository mavenCentralRepository

  @Mock
  Configuration apacheSnapshotsConfiguration

  @Mock
  Repository apacheSnapshotsRepository

  @Mock
  Configuration thirdPartyConfiguration

  @Mock
  Repository thirdPartyRepository

  @Mock
  Configuration groupConfiguration

  @Mock
  Repository groupRepository

  @Mock
  Configuration parentGroupConfiguration

  @Mock
  Repository parentGroupRepository

  @Mock
  Configuration cycleGroupAConfiguration

  @Mock
  Repository cycleGroupA

  @Mock
  Configuration cycleGroupBConfiguration

  @Mock
  Repository cycleGroupB

  @Mock
  Configuration ungroupedRepoConfiguration

  @Mock
  Repository ungroupedRepository

  //Recipe for creating repositories
  @Mock
  Recipe recipe

  String recipeName = 'mockRecipe'

  @Mock
  Type type

  @Mock
  Type groupType

  @Mock
  Format format

  @Mock
  NodeAccess nodeAccess

  @Mock
  BlobStoreManager blobStoreManager

  @Mock
  EntityMetadata entityMetadata

  @Mock
  GroupMemberMappingCache groupMemberMappingCache

  //Subject of the test
  RepositoryManagerImpl repositoryManager

  @Before
  void setup() {
    setupRecipe()
    setupRepositories()
    blobstoreProvisionDefaults(false, false)
  }

  private void setupRecipe() {
    when(recipe.getType()).thenReturn(type)
    when(recipe.getFormat()).thenReturn(format)
  }

  private void setupRepositories() {
    when(defaultRepositoriesContributor.getRepositoryConfigurations()).
        thenReturn(asList(mavenCentralConfiguration, apacheSnapshotsConfiguration, thirdPartyConfiguration,
            groupConfiguration, parentGroupConfiguration, cycleGroupAConfiguration, cycleGroupBConfiguration,
            ungroupedRepoConfiguration))
    defaultRepositoriesContributorList = singletonList(defaultRepositoriesContributor)

    mockRepository(mavenCentralConfiguration, mavenCentralRepository, MAVEN_CENTRAL_NAME, 'default')
    mockRepository(apacheSnapshotsConfiguration, apacheSnapshotsRepository, APACHE_SNAPSHOTS_NAME, 'default')
    mockRepository(thirdPartyConfiguration, thirdPartyRepository, THIRD_PARTY_NAME, 'third-party')
    mockRepository(groupConfiguration, groupRepository, GROUP_NAME, 'group')
    mockRepository(parentGroupConfiguration, parentGroupRepository, PARENT_GROUP_NAME, 'group')
    mockRepository(cycleGroupAConfiguration, cycleGroupA, CYCLE_A_NAME, 'group')
    mockRepository(cycleGroupBConfiguration, cycleGroupB, CYCLE_B_NAME, 'group')
    mockRepository(ungroupedRepoConfiguration, ungroupedRepository, UNGROUPED_REPO_NAME, 'default')

    when(repositoryFactory.create(type, format)).
        thenReturn(mavenCentralRepository,
            apacheSnapshotsRepository,
            thirdPartyRepository,
            groupRepository,
            parentGroupRepository,
            cycleGroupA,
            cycleGroupB,
            ungroupedRepository)

    when(groupType.getValue()).thenReturn("group")
    setupGroupRepository(groupRepository, groupConfiguration, mavenCentralRepository, apacheSnapshotsRepository)
    setupGroupRepository(parentGroupRepository, parentGroupConfiguration, groupRepository)
    setupGroupRepository(cycleGroupA, cycleGroupAConfiguration, cycleGroupB, apacheSnapshotsRepository)
    setupGroupRepository(cycleGroupB, cycleGroupBConfiguration, cycleGroupA, apacheSnapshotsRepository)
  }

  private void setupGroupRepository(Repository repository, Configuration configuration, Repository... members) {
    List<Repository> memberRepos = Arrays.asList(members)
    GroupFacet facet = mock(GroupFacet.class)
    NestedAttributesMap attributesMap = mock(NestedAttributesMap.class)
    when(repository.optionalFacet(GroupFacet.class)).thenReturn(Optional.of(facet))
    when(repository.getType()).thenReturn(groupType)
    when(configuration.attributes("group")).thenReturn(attributesMap)
    List<String> memberNames = memberRepos.stream().map({it.name}).collect()
    when(attributesMap.get("memberNames", Collection.class)).thenReturn(memberNames)
    for (Repository memberRepo : memberRepos) {
      when(facet.member(memberRepo)).thenReturn(true)
    }
    when(facet.members()).thenReturn(memberRepos)
  }

  private void mockRepository(Configuration configuration, Repository repository, String name, String blobstoreName) {
    when(configuration.getRecipeName()).thenReturn(recipeName)
    when(configuration.getRepositoryName()).thenReturn(name)
    when(configuration.getAttributes()).thenReturn([storage: [blobStoreName: blobstoreName]])
    when(repository.getConfiguration()).thenReturn(configuration)
    when(repository.getName()).thenReturn(name)
    when(repository.optionalFacet(any(Class.class))).thenReturn(Optional.empty())
  }

  private RepositoryManagerImpl buildRepositoryManagerImpl(final boolean defaultsConfigured,
                                                           final boolean skipDefaultRepositories)
  {
    if (defaultsConfigured) {
      when(configurationStore.list()).
          thenReturn(asList(mavenCentralConfiguration, apacheSnapshotsConfiguration, thirdPartyConfiguration,
              groupConfiguration, parentGroupConfiguration, cycleGroupAConfiguration, cycleGroupBConfiguration,
              ungroupedRepoConfiguration))
    }

    return initializeAndStartRepositoryManager(skipDefaultRepositories)
  }

  private RepositoryManagerImpl initializeAndStartRepositoryManager(boolean skipDefaultRepositories) {
    repositoryManager = new RepositoryManagerImpl(eventManager, configurationStore, repositoryFactory,
        configurationFacetProvider, ImmutableMap.of(recipeName, recipe), securityContributor,
        defaultRepositoriesContributorList, databaseFreezeService, skipDefaultRepositories, blobStoreManager,
        groupMemberMappingCache)

    repositoryManager.doStart()
    return repositoryManager
  }

  private RepositoryManagerImpl buildRepositoryManagerImpl(final boolean defaultsConfigured) {
    return buildRepositoryManagerImpl(defaultsConfigured, false)
  }

  private void blobstoreProvisionDefaults(final boolean provisionDefaults, final boolean clustered) {
    when(blobStoreManager.exists(DEFAULT_BLOBSTORE_NAME)).thenReturn(provisionDefaults || !clustered)
  }

  @Test
  void 'it should correctly load existing configurations on startup'() {
    repositoryManager = buildRepositoryManagerImpl(true)
    when(configurationStore.list()).
        thenReturn(asList(mavenCentralConfiguration, apacheSnapshotsConfiguration, thirdPartyConfiguration))

    assertThat(repositoryManager.browse()).hasSize(8)

    verify(mavenCentralRepository).init(mavenCentralConfiguration)
    verify(mavenCentralRepository).start()

    verify(apacheSnapshotsRepository).init(apacheSnapshotsConfiguration)
    verify(apacheSnapshotsRepository).start()

    verify(thirdPartyRepository).init(thirdPartyConfiguration)
    verify(thirdPartyRepository).start()
  }

  @Test
  void 'it should correctly create default repositories if none are configured on startup'() {
    repositoryManager = buildRepositoryManagerImpl(false)

    verify(configurationStore).create(mavenCentralConfiguration)
    verify(configurationStore).create(apacheSnapshotsConfiguration)
    verify(configurationStore).create(thirdPartyConfiguration)
  }

  @Test
  void 'should not create default repositories even if none are present if skip defaults is true'() {
    repositoryManager = buildRepositoryManagerImpl(false, true)

    verify(configurationStore, times(0)).create(any(Configuration.class))
  }

  @Test
  void 'should not create default repositories if it is clustered'() {
    when(nodeAccess.isClustered()).thenReturn(true)
    blobstoreProvisionDefaults(false, true)
    repositoryManager = buildRepositoryManagerImpl(false)

    verify(configurationStore, times(0)).create(any(Configuration.class))
  }

  @Test
  void 'should still create default repositories if it is clustered and the default blobstore exists'() {
    when(nodeAccess.isClustered()).thenReturn(true)
    blobstoreProvisionDefaults(true, true)
    repositoryManager = buildRepositoryManagerImpl(false)

    verify(configurationStore).create(mavenCentralConfiguration)
    verify(configurationStore).create(apacheSnapshotsConfiguration)
    verify(configurationStore).create(thirdPartyConfiguration)
  }

  @Test
  void 'it should not create any repositories if no defaults are provided'() {
    when(defaultRepositoriesContributor.getRepositoryConfigurations()).thenReturn(emptyList())

    repositoryManager = buildRepositoryManagerImpl(false, false)

    verify(configurationStore, times(0)).create(any(Configuration.class))
  }

  @Test
  void 'exists checks name, is not case sensitive'() {
    repositoryManager = buildRepositoryManagerImpl(true)
    assertThat(repositoryManager.exists(MAVEN_CENTRAL_NAME)).isTrue()
    assertThat(repositoryManager.exists(MAVEN_CENTRAL_NAME.toUpperCase())).isTrue()
    assertThat(repositoryManager.exists('missing-repository')).isFalse()
  }

  @Test
  void 'blobstoreUsageCount returns number of repositories using a blob store'() {
    repositoryManager = buildRepositoryManagerImpl(true)
    assertThat(repositoryManager.blobstoreUsageCount("default")).isEqualTo(3)
    assertThat(repositoryManager.blobstoreUsageCount("third-party")).isEqualTo(1)
  }

  @Test
  void 'test delete checks unfrozen'() {
    repositoryManager = buildRepositoryManagerImpl(true)
    repositoryManager.delete("maven-central")
    verify(databaseFreezeService).checkUnfrozen("Unable to delete repository when database is frozen.")
  }

  @Test
  void 'remove repository from any group repository configurations on delete'() {
    buildRepositoryManagerImpl(true).delete(MAVEN_CENTRAL_NAME)
    assertFalse(((Collection)groupConfiguration.attributes("group").get("memberNames", Collection.class)).contains(mavenCentralRepository))
  }

  @Test
  void 'update group repository when a member repository is deleted'() {
    buildRepositoryManagerImpl(true).delete(MAVEN_CENTRAL_NAME)
    verify(configurationStore).update(groupConfiguration)
  }

  @Test
  void 'creating repositories concurrently should not fail'() {
    RepositoryManagerImpl repositoryManager = initializeAndStartRepositoryManager(true)
    repositoryManager.create(new Configuration(repositoryName: 'r1', recipeName: 'mockRecipe'))
    repositoryManager.create(new Configuration(repositoryName: 'r2', recipeName: 'mockRecipe'))

    // open an iterator to simulate concurrent access to the private repositories map in RepositoryManagerImpl
    def iterator = repositoryManager.repositories.iterator()
    iterator.next()
    repositoryManager.create(new Configuration(repositoryName: 'r3', recipeName: 'mockRecipe'))
    // this call will fail with ConcurrentModificationException if the private repositories map is not thread safe
    iterator.next()
  }

  @Test
  void 'post metadata-updated-event when bucket of existing repository is updated'() {
    repositoryManager = buildRepositoryManagerImpl(true)
    BucketUpdatedEvent bucketEvent = new BucketUpdatedEvent(entityMetadata, MAVEN_CENTRAL_NAME)
    repositoryManager.onBucketUpdated(bucketEvent)
    ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object)
    verify(eventManager, atLeastOnce()).post(eventCaptor.capture())
    Object repoEvent = eventCaptor.allValues.last()
    assertThat(repoEvent, is(instanceOf(RepositoryMetadataUpdatedEvent)))
    assertThat(repoEvent.repository, is(mavenCentralRepository))
  }

  @Test
  void 'do not post metadata-updated-event when bucket of deleted repository is updated'() {
    repositoryManager = buildRepositoryManagerImpl(true)
    BucketUpdatedEvent bucketEvent = new BucketUpdatedEvent(entityMetadata, 'some-deleted-repo$uuid')
    repositoryManager.onBucketUpdated(bucketEvent)
    verify(eventManager, never()).post(isA(RepositoryMetadataUpdatedEvent))
  }

  @Test
  void 'repository with cleanup policy is correctly loaded'() {
    repositoryManager = buildRepositoryManagerImpl(true)

    String name = randomUUID().toString().replace('-', '')

    Map<String, Object> cleanupAttributes = newHashMap()
    cleanupAttributes.put(CLEANUP_NAME_KEY, name)

    mavenCentralConfiguration.attributes.put(CLEANUP_ATTRIBUTES_KEY, cleanupAttributes)

    List<Repository> repositories = repositoryManager.browseForCleanupPolicy(name).collect(toList())

    assertThat(repositories.size(), equalTo(1))
    assertThat(repositories.get(0).configuration, equalTo(mavenCentralConfiguration))
    assertThat(repositories
        .get(0).configuration.attributes
        .get(CLEANUP_ATTRIBUTES_KEY)
        .get(CLEANUP_NAME_KEY).toString(),
        equalTo(name))
  }

  @Test
  void 'multi repository with same cleanup policy are correctly loaded'() {
    repositoryManager = buildRepositoryManagerImpl(true)

    String name = randomUUID().toString().replace('-', '')

    Map<String, Object> cleanupAttributes = newHashMap()
    cleanupAttributes.put(CLEANUP_NAME_KEY, name)

    mavenCentralConfiguration.attributes.put(CLEANUP_ATTRIBUTES_KEY, cleanupAttributes)
    apacheSnapshotsConfiguration.attributes.put(CLEANUP_ATTRIBUTES_KEY, cleanupAttributes)

    List<Repository> repositories = repositoryManager.browseForCleanupPolicy(name).collect(toList())

    assertThat(repositories.size(), equalTo(2))
    assertThat(repositories.get(0).configuration, equalTo(mavenCentralConfiguration))
    assertThat(repositories.get(1).configuration, equalTo(apacheSnapshotsConfiguration))
  }

  @Test
  void 'no repositories are loaded for an unknown cleanup policy'() {
    repositoryManager = buildRepositoryManagerImpl(true)

    def stream = repositoryManager.browseForCleanupPolicy(randomUUID().toString())

    assertThat(stream.count()).isEqualTo(0)
  }
}

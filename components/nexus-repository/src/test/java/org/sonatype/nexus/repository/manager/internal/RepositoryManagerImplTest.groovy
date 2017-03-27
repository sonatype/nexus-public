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
import org.sonatype.nexus.common.event.EventManager
import org.sonatype.nexus.common.node.NodeAccess
import org.sonatype.nexus.orient.freeze.DatabaseFreezeService
import org.sonatype.nexus.repository.Format
import org.sonatype.nexus.repository.Recipe
import org.sonatype.nexus.repository.Repository
import org.sonatype.nexus.repository.Type
import org.sonatype.nexus.repository.config.Configuration
import org.sonatype.nexus.repository.config.internal.ConfigurationStore
import org.sonatype.nexus.repository.group.GroupFacet
import org.sonatype.nexus.repository.manager.DefaultRepositoriesContributor

import com.google.common.collect.ImmutableMap
import org.junit.Before
import org.junit.Test
import org.mockito.Mock

import static com.google.common.collect.Lists.asList
import static com.google.common.collect.Lists.newArrayList
import static java.util.Collections.emptyList
import static java.util.Collections.singletonList
import static org.fest.assertions.api.Assertions.assertThat
import static org.junit.Assert.assertFalse
import static org.mockito.Matchers.any
import static org.mockito.Mockito.times
import static org.mockito.Mockito.verify
import static org.mockito.Mockito.when
import static org.sonatype.nexus.blobstore.api.BlobStoreManager.DEFAULT_BLOBSTORE_NAME

class RepositoryManagerImplTest
    extends TestSupport
{

  static final String MAVEN_CENTRAL_NAME = 'maven-central'

  static final String APACHE_SNAPSHOTS_NAME = 'apache-snapshots'

  List<String> groupMembers

  @Mock
  EventManager eventManager

  @Mock
  ConfigurationStore configurationStore

  @Mock
  DatabaseFreezeService databaseFreezeService

  @Mock
  RepositoryFactory repositoryFactory

  @Mock
  Provider<ConfigurationStore> configurationFacetProvider

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
  GroupFacet groupFacet

  @Mock
  NestedAttributesMap groupAttributesMap

  //Subject of the test
  RepositoryManagerImpl repositoryManager

  @Before
  void setup() {
    setupRecipe()
    setupRepositories()
  }

  private void setupRecipe() {
    when(recipe.getType()).thenReturn(type)
    when(recipe.getFormat()).thenReturn(format)
  }

  private void setupRepositories() {
    when(defaultRepositoriesContributor.getRepositoryConfigurations()).
        thenReturn(asList(mavenCentralConfiguration, apacheSnapshotsConfiguration, thirdPartyConfiguration,
            groupConfiguration))
    defaultRepositoriesContributorList = singletonList(defaultRepositoriesContributor)

    mockRepository(mavenCentralConfiguration, mavenCentralRepository, MAVEN_CENTRAL_NAME, 'default')
    mockRepository(apacheSnapshotsConfiguration, apacheSnapshotsRepository, APACHE_SNAPSHOTS_NAME, 'default')
    mockRepository(thirdPartyConfiguration, thirdPartyRepository, 'third-party', 'third-party')
    mockRepository(groupConfiguration, groupRepository, 'group', 'group')

    when(repositoryFactory.create(type, format)).
        thenReturn(mavenCentralRepository, apacheSnapshotsRepository, thirdPartyRepository, groupRepository)

    setupGroupRepository()
  }

  private void setupGroupRepository() {
    groupMembers = newArrayList(MAVEN_CENTRAL_NAME, "apache-snapshots")
    when(groupRepository.optionalFacet(GroupFacet.class)).thenReturn(Optional.of(groupFacet))
    when(groupFacet.member(mavenCentralRepository)).thenReturn(true)
    when(groupFacet.member(apacheSnapshotsRepository)).thenReturn(true)
    when(groupRepository.getType()).thenReturn(groupType)
    when(groupType.getValue()).thenReturn("group")
    when(groupConfiguration.attributes("group")).thenReturn(groupAttributesMap)
    when(groupAttributesMap.get("memberNames", Collection.class)).thenReturn(groupMembers)
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
              groupConfiguration))
    }

    return initializeAndStartRepositoryManager(skipDefaultRepositories)
  }

  private RepositoryManagerImpl initializeAndStartRepositoryManager(boolean skipDefaultRepositories) {
    repositoryManager = new RepositoryManagerImpl(eventManager, configurationStore, repositoryFactory,
        configurationFacetProvider, ImmutableMap.of(recipeName, recipe), securityContributor,
        defaultRepositoriesContributorList, databaseFreezeService, skipDefaultRepositories, nodeAccess,
        blobStoreManager)

    repositoryManager.doStart()
    return repositoryManager
  }

  private RepositoryManagerImpl buildRepositoryManagerImpl(final boolean defaultsConfigured) {
    return buildRepositoryManagerImpl(defaultsConfigured, false)
  }

  @Test
  void 'it should correctly load existing configurations on startup'() {
    repositoryManager = buildRepositoryManagerImpl(true)
    when(configurationStore.list()).
        thenReturn(asList(mavenCentralConfiguration, apacheSnapshotsConfiguration, thirdPartyConfiguration))

    assertThat(repositoryManager.browse()).hasSize(4)

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
    repositoryManager = buildRepositoryManagerImpl(false)

    verify(configurationStore, times(0)).create(any(Configuration.class))
  }

  @Test
  void 'should still create default repositories if it is clustered and the default blobstore exists'() {
    when(nodeAccess.isClustered()).thenReturn(true)
    when(blobStoreManager.exists(DEFAULT_BLOBSTORE_NAME)).thenReturn(true)
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
    assertThat(repositoryManager.blobstoreUsageCount("default")).isEqualTo(2)
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
    assertFalse(groupMembers.contains(MAVEN_CENTRAL_NAME))
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

}

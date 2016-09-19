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
import org.sonatype.nexus.common.event.EventBus
import org.sonatype.nexus.repository.Format
import org.sonatype.nexus.repository.Recipe
import org.sonatype.nexus.repository.Repository
import org.sonatype.nexus.repository.Type
import org.sonatype.nexus.repository.config.Configuration
import org.sonatype.nexus.repository.config.ConfigurationStore
import org.sonatype.nexus.repository.manager.DefaultRepositoriesContributor

import com.google.common.collect.ImmutableMap
import org.junit.Before
import org.junit.Test
import org.mockito.Mock

import static java.util.Arrays.asList
import static org.fest.assertions.api.Assertions.assertThat
import static org.mockito.Mockito.verify
import static org.mockito.Mockito.when

class RepositoryManagerImplTest
    extends TestSupport
{

  //Dependencies for RepositoryManagerImpl
  @Mock
  EventBus eventBus

  @Mock
  ConfigurationStore configurationStore

  @Mock
  RepositoryFactory repositoryFactory

  @Mock
  Provider<ConfigurationStore> configurationFacetProvider

  @Mock
  RepositoryAdminSecurityConfigurationResource repositoryAdminSecurityConfigurationResource

  @Mock
  List<DefaultRepositoriesContributor> defaultRepositoriesContributorList

  //Configurations and repositories
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

  //Recipe for creating repositories
  @Mock
  Recipe recipe

  String recipeName = 'mockRecipe'

  @Mock
  Type type

  @Mock
  Format format

  //Subject of the test
  RepositoryManagerImpl repositoryManager

  @Before
  public void setup() {
    when(configurationStore.list()).
        thenReturn(asList(mavenCentralConfiguration, apacheSnapshotsConfiguration, thirdPartyConfiguration))

    //recipe setup
    when(recipe.getType()).thenReturn(type)
    when(recipe.getFormat()).thenReturn(format)

    mockRepository(mavenCentralConfiguration, mavenCentralRepository, 'maven-central', 'default')
    mockRepository(apacheSnapshotsConfiguration, apacheSnapshotsRepository, 'apache-snapshots', 'default')
    mockRepository(thirdPartyConfiguration, thirdPartyRepository, 'third-party', 'third-party')

    when(repositoryFactory.create(type, format)).
        thenReturn(mavenCentralRepository, apacheSnapshotsRepository, thirdPartyRepository)

    //initialize and start the repository manager
    repositoryManager = new RepositoryManagerImpl(eventBus, configurationStore, repositoryFactory,
        configurationFacetProvider, ImmutableMap.of(recipeName, recipe), repositoryAdminSecurityConfigurationResource,
        defaultRepositoriesContributorList, false)

    repositoryManager.doStart()
  }

  private void mockRepository(Configuration configuration, Repository repository, String name, String blobstoreName) {
    when(configuration.getRecipeName()).thenReturn(recipeName)
    when(configuration.getAttributes()).thenReturn([storage: [blobStoreName: blobstoreName]])

    when(repository.getConfiguration()).thenReturn(configuration)
    when(repository.getName()).thenReturn(name)
  }

  @Test
  public void 'it should correctly load existing configurations on startup'() {
    assertThat(repositoryManager.browse()).hasSize(3)

    verify(mavenCentralRepository).init(mavenCentralConfiguration)
    verify(apacheSnapshotsRepository).init(apacheSnapshotsConfiguration)
    verify(thirdPartyRepository).init(thirdPartyConfiguration)
  }

  @Test
  public void 'exists checks name, is not case sensitive'() {
    assertThat(repositoryManager.exists('maven-central')).isTrue();
    assertThat(repositoryManager.exists('MAVEN-CENTRAL')).isTrue();
    assertThat(repositoryManager.exists('missing-repository')).isFalse();
  }

  @Test
  public void 'blobstoreUsageCount returns number of repositories using a blob store'() {
    assertThat(repositoryManager.blobstoreUsageCount("default")).isEqualTo(2);
    assertThat(repositoryManager.blobstoreUsageCount("third-party")).isEqualTo(1);
  }
}

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
package org.sonatype.nexus.coreui

import org.sonatype.goodies.testsupport.TestSupport
import org.sonatype.nexus.common.event.EventManager
import org.sonatype.nexus.extdirect.model.StoreLoadParameters
import org.sonatype.nexus.repository.Format
import org.sonatype.nexus.repository.Repository
import org.sonatype.nexus.repository.manager.RepositoryManager
import org.sonatype.nexus.repository.manager.internal.RepositoryImpl
import org.sonatype.nexus.repository.security.RepositoryPermissionChecker
import org.sonatype.nexus.repository.types.HostedType

import org.junit.Before
import org.junit.Test
import org.mockito.Mock

import static org.hamcrest.Matchers.is
import static org.hamcrest.MatcherAssert.assertThat
import static org.mockito.Mockito.verify
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when
import static org.mockito.Mockito.mock

/**
 * Test for {@link RepositoryComponent}
 */
class RepositoryComponentTest
    extends TestSupport
{
  @Mock
  Format format

  @Mock
  EventManager eventManager

  @Mock
  RepositoryManager repositoryManager

  @Mock
  RepositoryPermissionChecker repositoryPermissionChecker

  Repository repository

  RepositoryComponent underTest

  @Before
  void setup() {
    repository = repository()

    when(format.getValue()).thenReturn('format')
    when(repositoryManager.browse()).thenReturn([repository])

    underTest = new RepositoryComponent()
    underTest.repositoryManager = repositoryManager
    underTest.repositoryPermissionChecker = repositoryPermissionChecker
  }

  @Test
  void checkUserPermissionsOnFilter() {
    underTest.filter(new StoreLoadParameters(filter: []))
    verify(repositoryPermissionChecker).userCanBrowseRepositories([repository] as List<Repository>)
  }

  @Test
  void filterForAutocomplete() {
    List<Repository> repositories = getTestRepositories()
    StoreLoadParameters storeLoadParameters = new StoreLoadParameters(filter: [])
    storeLoadParameters.setQuery("nug");
    List<RepositoryReferenceXO> result = underTest.filterForAutocomplete(storeLoadParameters, repositories)
    assertThat(result, hasSize(2))
    assertThat(result.get(0).getName(), is("nuget-proxy"));
    assertThat(result.get(1).getName(), is("nuget-hosted"));
  }

  List<Repository> getTestRepositories() {
    Repository nugetRepoProxy = mock(Repository.class);
    when(nugetRepoProxy.getName()).thenReturn("nuget-proxy")
    Repository nugetRepoHosted = mock(Repository.class);
    when(nugetRepoHosted.getName()).thenReturn("nuget-hosted")
    Repository mavenRepoHosted = mock(Repository.class);
    when(mavenRepoHosted.getName()).thenReturn("maven-hosted")
    List<Repository> repositories = new ArrayList<>()
    repositories.add(nugetRepoProxy)
    repositories.add(nugetRepoHosted)
    repositories.add(mavenRepoHosted)
    return repositories
  }


  Repository repository() {
    def repository = new RepositoryImpl(eventManager, new HostedType(), format)
    repository.name = 'repository'
    repository
  }
}

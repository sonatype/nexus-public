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
package org.sonatype.nexus.repository.rest.internal.api

import org.sonatype.goodies.testsupport.TestSupport
import org.sonatype.nexus.repository.Facet
import org.sonatype.nexus.repository.Format
import org.sonatype.nexus.repository.Repository
import org.sonatype.nexus.repository.Type
import org.sonatype.nexus.repository.Recipe
import org.sonatype.nexus.repository.config.internal.ConfigurationData
import org.sonatype.nexus.repository.httpclient.HttpClientFacet
import org.sonatype.nexus.repository.httpclient.RemoteConnectionStatus
import org.sonatype.nexus.repository.manager.RepositoryManager
import org.sonatype.nexus.repository.rest.api.ApiRepositoryAdapter
import org.sonatype.nexus.repository.rest.api.AuthorizingRepositoryManager
import org.sonatype.nexus.repository.security.RepositoryPermissionChecker
import org.sonatype.nexus.repository.types.GroupType
import org.sonatype.nexus.repository.types.HostedType
import org.sonatype.nexus.repository.types.ProxyType

import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.mockito.Mock

import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when
import static org.sonatype.nexus.security.BreadActions.READ

class RepositoryInternalResourceTest
    extends TestSupport
{
  @Mock
  List<Format> formats

  @Mock
  RepositoryManager repositoryManager

  @Mock
  RepositoryPermissionChecker repositoryPermissionChecker

  @Mock
  List<Recipe> recipes

  @Mock
  AuthorizingRepositoryManager authorizingRepositoryManager

  @Mock
  Map<String, ApiRepositoryAdapter> convertersByFormat

  @Mock
  ApiRepositoryAdapter defaultAdapter

  ProxyType proxyType = new ProxyType()

  GroupType groupType = new GroupType()

  HostedType hostedType = new HostedType()

  RepositoryInternalResource underTest

  @Before
  void setup() {
    underTest = new RepositoryInternalResource(
        formats,
        repositoryManager,
        repositoryPermissionChecker,
        proxyType,
        recipes,
        authorizingRepositoryManager,
        convertersByFormat,
        defaultAdapter
    )
  }

  @Test
  void testGetRepositories() {
    def maven2 = new Format('maven2') {}
    def nuget = new Format('nuget') {}

    def mavenGroupRepository = mockRepository('maven-public', maven2, groupType, 'http://localhost:8081/repository/maven-public/', true)
    def mavenProxyRepository = mockRepository('maven-central', maven2, proxyType, 'http://localhost:8081/repository/maven-central/', true,
            [(HttpClientFacet): mockHttpFacet('Ready to Connect', null)])
    def nugetGroupRepository = mockRepository('nuget-group', nuget, groupType, 'http://localhost:8081/repository/nuget-group/', true)
    def nugetHostedRepository = mockRepository('nuget-hosted', nuget, hostedType, 'http://localhost:8081/repository/nuget-hosted/', true)
    def nugetProxyRepository = mockRepository('nuget.org-proxy', nuget, proxyType, 'http://localhost:8081/repository/nuget.org-proxy/', true,
            [(HttpClientFacet): mockHttpFacet('Ready to Connect', null)])

    def repositories = [
      nugetProxyRepository,
      mavenGroupRepository,
      mavenProxyRepository,
      nugetHostedRepository,
      nugetGroupRepository
    ]

    def sortedRepositories = [
      mavenProxyRepository,
      mavenGroupRepository,
      nugetGroupRepository,
      nugetHostedRepository,
      nugetProxyRepository
    ]

    when(repositoryPermissionChecker.userCanBrowseRepositories(repositories)).thenReturn(repositories)

    when(repositoryManager.browse()).thenReturn(repositories)

    def response = underTest.getRepositories(null, false, false, null);

    assert response[0].id == sortedRepositories[0].name
    assert response[0].name == sortedRepositories[0].name

    assert response[1].id == sortedRepositories[1].name
    assert response[1].name == sortedRepositories[1].name

    assert response[2].id == sortedRepositories[2].name
    assert response[2].name == sortedRepositories[2].name

    assert response[3].id == sortedRepositories[3].name
    assert response[3].name == sortedRepositories[3].name

    assert response[4].id == sortedRepositories[4].name
    assert response[4].name == sortedRepositories[4].name
  }

  @Ignore
  //TODO NEXUS-32555
  @Test
  void testGetDetails() {
    def maven2 = new Format('maven2') {}
    def nuget = new Format('nuget') {}
    def repositories = [
      mockRepository('maven-central', maven2, proxyType, 'http://localhost:8081/repository/maven-central/', true,
          [(HttpClientFacet): mockHttpFacet('Ready to Connect', null)]),
      mockRepository('maven-public', maven2, groupType, 'http://localhost:8081/repository/maven-public/', true),
      mockRepository('maven-releases', maven2, hostedType, 'http://localhost:8081/repository/maven-releases/', true),
      mockRepository('maven-snapshots', maven2, hostedType, 'http://localhost:8081/repository/maven-snapshots/', false),
      mockRepository('nuget-group', nuget, groupType, 'http://localhost:8081/repository/nuget-group/', true),
      mockRepository('nuget-hosted', nuget, hostedType, 'http://localhost:8081/repository/nuget-hosted/', true),
      mockRepository('nuget.org-proxy', nuget, proxyType, 'http://localhost:8081/repository/nuget.org-proxy/', true,
          [(HttpClientFacet): mockHttpFacet('Remote Auto Blocked and Unavailable',
              'java.net.UnknownHostException: api.example.org: nodename nor servname provided, or not known')])
    ]
    repositories.each { when(repositoryPermissionChecker.userHasRepositoryAdminPermission(it, READ)).thenReturn(true) }

    when(repositoryManager.browse()).thenReturn(repositories)

    def details = underTest.getRepositoryDetails()

    assert details[0].name == 'maven-central'
    assert details[0].type == 'proxy'
    assert details[0].format == 'maven2'
    assert details[0].url == 'http://localhost:8081/repository/maven-central/'
    assert details[0].status.online
    assert details[0].status.description == 'Ready to Connect'
    assert details[0].status.reason == null

    assert details[1].name == 'maven-public'
    assert details[1].type == 'group'
    assert details[1].format == 'maven2'
    assert details[1].url == 'http://localhost:8081/repository/maven-public/'
    assert details[1].status.online
    assert details[1].status.description == null
    assert details[1].status.reason == null

    assert details[2].name == 'maven-releases'
    assert details[2].type == 'hosted'
    assert details[2].format == 'maven2'
    assert details[2].url == 'http://localhost:8081/repository/maven-releases/'
    assert details[2].status.online
    assert details[2].status.description == null
    assert details[2].status.reason == null

    assert details[3].name == 'maven-snapshots'
    assert details[3].type == 'hosted'
    assert details[3].format == 'maven2'
    assert details[3].url == 'http://localhost:8081/repository/maven-snapshots/'
    assert !details[3].status.online
    assert details[3].status.description == null
    assert details[3].status.reason == null

    assert details[4].name == 'nuget-group'
    assert details[4].type == 'group'
    assert details[4].format == 'nuget'
    assert details[4].url == 'http://localhost:8081/repository/nuget-group/'
    assert details[4].status.online
    assert details[4].status.description == null
    assert details[4].status.reason == null

    assert details[5].name == 'nuget-hosted'
    assert details[5].type == 'hosted'
    assert details[5].format == 'nuget'
    assert details[5].url == 'http://localhost:8081/repository/nuget-hosted/'
    assert details[5].status.online
    assert details[5].status.description == null
    assert details[5].status.reason == null

    assert details[6].name == 'nuget.org-proxy'
    assert details[6].type == 'proxy'
    assert details[6].format == 'nuget'
    assert details[6].url == 'http://localhost:8081/repository/nuget.org-proxy/'
    assert details[6].status.online
    assert details[6].status.description == 'Remote Auto Blocked and Unavailable'
    assert details[6].status.reason ==
        'java.net.UnknownHostException: api.example.org: nodename nor servname provided, or not known'
  }

  Repository mockRepository(final String name,
                            final Format format,
                            final Type type,
                            final String url,
                            final boolean online,
                            final Map<Class, Facet> facets = [:]) {
    Repository repository = mock(Repository)
    when(repository.getName()).thenReturn(name)
    when(repository.getFormat()).thenReturn(format)
    when(repository.getType()).thenReturn(type)
    when(repository.getUrl()).thenReturn(url)
    def configuration = new ConfigurationData()
    when(repository.getConfiguration()).thenReturn(configuration)
    facets.each { clazz, facet ->
      when(repository.facet(clazz)).thenReturn(facet)
    }
    configuration.setOnline(online)
    return repository
  }

  Facet mockHttpFacet(final String description, final String reason) {
    HttpClientFacet facet = mock(HttpClientFacet)
    RemoteConnectionStatus status = mock(RemoteConnectionStatus)
    when(facet.getStatus()).thenReturn(status)
    when(status.getDescription()).thenReturn(description)
    when(status.getReason()).thenReturn(reason)
    return facet
  }
}

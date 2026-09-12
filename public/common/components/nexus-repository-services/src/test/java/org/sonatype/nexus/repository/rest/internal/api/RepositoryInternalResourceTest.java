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
package org.sonatype.nexus.repository.rest.internal.api;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.sonatype.nexus.common.app.GlobalComponentLookupHelper;
import org.sonatype.nexus.repository.Facet;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.MissingFacetException;
import org.sonatype.nexus.repository.Recipe;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.config.internal.ConfigurationData;
import org.sonatype.nexus.repository.httpclient.HttpClientFacet;
import org.sonatype.nexus.repository.httpclient.RemoteConnectionStatus;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.purge.PurgeUnusedFacet;
import org.sonatype.nexus.repository.rest.api.ApiRepositoryAdapter;
import org.sonatype.nexus.repository.rest.api.AuthorizingRepositoryManager;
import org.sonatype.nexus.repository.rest.api.RepositoryMetricsService;
import org.sonatype.nexus.repository.security.RepositoryPermissionChecker;
import org.sonatype.nexus.repository.types.GroupType;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.repository.types.ProxyType;
import org.sonatype.nexus.rest.WebApplicationMessageException;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension.WithUser;
import org.sonatype.nexus.testcommon.validation.ValidationExtension;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.security.BreadActions.READ;

@ExtendWith(MockitoExtension.class)
@ExtendWith(ValidationExtension.class)
@ExtendWith(AuthenticationExtension.class)
@WithUser
class RepositoryInternalResourceTest
{
  @Mock
  private List<Format> formats;

  @Mock
  private RepositoryManager repositoryManager;

  @Mock
  private RepositoryPermissionChecker repositoryPermissionChecker;

  @Mock
  private List<Recipe> recipes;

  @Mock
  private AuthorizingRepositoryManager authorizingRepositoryManager;

  @Mock
  private List<ApiRepositoryAdapter> convertersByFormat;

  @Mock
  private ApiRepositoryAdapter defaultAdapter;

  @Mock
  private RepositoryMetricsService repositoryMetricsService;

  @Mock
  private GlobalComponentLookupHelper componentLookupHelper;

  private final ProxyType proxyType = new ProxyType();

  private final GroupType groupType = new GroupType();

  private final HostedType hostedType = new HostedType();

  private RepositoryInternalResource underTest;

  @BeforeEach
  void setup() {
    lenient().when(repositoryMetricsService.list()).thenReturn(java.util.Collections.emptyList());
    underTest = new RepositoryInternalResource(
        formats,
        repositoryManager,
        repositoryPermissionChecker,
        proxyType,
        recipes,
        authorizingRepositoryManager,
        convertersByFormat,
        defaultAdapter,
        repositoryMetricsService,
        componentLookupHelper);
  }

  @Test
  void testGetRepositories() {
    Format maven2 = new Format("maven2")
    {
    };
    Format nuget = new Format("nuget")
    {
    };

    Repository mavenGroupRepository =
        mockRepository("maven-public", maven2, groupType, "http://localhost:8081/repository/maven-public/", true,
            Map.of());
    Repository mavenProxyRepository =
        mockRepository("maven-central", maven2, proxyType, "http://localhost:8081/repository/maven-central/", true,
            Map.of(HttpClientFacet.class, mockHttpFacet("Ready to Connect", null)));
    Repository nugetGroupRepository =
        mockRepository("nuget-group", nuget, groupType, "http://localhost:8081/repository/nuget-group/", true,
            Map.of());
    Repository nugetHostedRepository =
        mockRepository("nuget-hosted", nuget, hostedType, "http://localhost:8081/repository/nuget-hosted/", true,
            Map.of());
    Repository nugetProxyRepository =
        mockRepository("nuget.org-proxy", nuget, proxyType, "http://localhost:8081/repository/nuget.org-proxy/", true,
            Map.of(HttpClientFacet.class, mockHttpFacet("Ready to Connect", null)));

    List<Repository> repositories = List.of(
        nugetProxyRepository,
        mavenGroupRepository,
        mavenProxyRepository,
        nugetHostedRepository,
        nugetGroupRepository);

    List<Repository> sortedRepositories = List.of(
        mavenProxyRepository,
        mavenGroupRepository,
        nugetGroupRepository,
        nugetHostedRepository,
        nugetProxyRepository);

    when(repositoryPermissionChecker.userCanBrowseRepositories(repositories)).thenReturn(repositories);
    when(repositoryManager.browse()).thenReturn(repositories);

    List<RepositoryXO> response = underTest.getRepositories(null, false, false, null, null, null);

    assertThat(response.get(0).getName(), is(sortedRepositories.get(0).getName()));
    assertThat(response.get(1).getName(), is(sortedRepositories.get(1).getName()));
    assertThat(response.get(2).getName(), is(sortedRepositories.get(2).getName()));
    assertThat(response.get(3).getName(), is(sortedRepositories.get(3).getName()));
    assertThat(response.get(4).getName(), is(sortedRepositories.get(4).getName()));
  }

  @Test
  void testGetRepositoriesWithCommaSeparatedFormatFilter() {
    Format maven2 = new Format("maven2")
    {
    };
    Format nuget = new Format("nuget")
    {
    };
    Format npm = new Format("npm")
    {
    };

    Repository mavenProxyRepository =
        mockRepository("maven-central", maven2, proxyType, "http://localhost:8081/repository/maven-central/", true,
            Map.of(HttpClientFacet.class, mockHttpFacet("Ready to Connect", null)));
    Repository nugetProxyRepository =
        mockRepository("nuget.org-proxy", nuget, proxyType, "http://localhost:8081/repository/nuget.org-proxy/", true,
            Map.of(HttpClientFacet.class, mockHttpFacet("Ready to Connect", null)));
    Repository npmHostedRepository =
        mockRepository("npm-hosted", npm, hostedType, "http://localhost:8081/repository/npm-hosted/", true,
            Map.of());

    List<Repository> repositories = List.of(mavenProxyRepository, nugetProxyRepository, npmHostedRepository);

    when(repositoryPermissionChecker.userCanBrowseRepositories(repositories)).thenReturn(repositories);
    when(repositoryManager.browse()).thenReturn(repositories);

    // Filter by comma-separated format list: maven2,nuget (should exclude npm)
    List<RepositoryXO> response = underTest.getRepositories(null, false, false, "maven2,nuget", null, null);

    assertThat(response.size(), is(2));
    assertThat(response.get(0).getName(), is("maven-central"));
    assertThat(response.get(1).getName(), is("nuget.org-proxy"));
  }

  @Test
  void testGetRepositoriesExcludingReleaseVersionPolicy() {
    // Mirrors the descriptor for repository.maven.purge-unused-snapshots:
    // excludingAnyOfVersionPolicies(RELEASE) -> "!RELEASE". Repos without a versionPolicy
    // (non-Maven) must still pass the exclude filter to match the classic UI behavior.
    Format maven2 = new Format("maven2")
    {
    };
    Format npm = new Format("npm")
    {
    };

    Repository mavenSnapshots = mockRepository("maven-snapshots", maven2, hostedType,
        "http://localhost:8081/repository/maven-snapshots/", true, Map.of(), "SNAPSHOT");
    Repository mavenReleases = mockRepository("maven-releases", maven2, hostedType,
        "http://localhost:8081/repository/maven-releases/", true, Map.of(), "RELEASE");
    Repository mavenGroup = mockRepository("maven-public", maven2, groupType,
        "http://localhost:8081/repository/maven-public/", true, Map.of(), "MIXED");
    Repository npmHosted = mockRepository("npm-hosted", npm, hostedType,
        "http://localhost:8081/repository/npm-hosted/", true, Map.of()); // no versionPolicy

    List<Repository> repositories = List.of(mavenSnapshots, mavenReleases, mavenGroup, npmHosted);
    when(repositoryPermissionChecker.userCanBrowseRepositories(repositories)).thenReturn(repositories);
    when(repositoryManager.browse()).thenReturn(repositories);

    List<RepositoryXO> response = underTest.getRepositories(null, false, false, null, null, "!RELEASE");

    // maven-releases excluded; the rest pass (npm-hosted has no versionPolicy and is allowed
    // through exclude-only filters).
    assertThat(response.size(), is(3));
    assertThat(response.get(0).getName(), is("maven-public"));
    assertThat(response.get(1).getName(), is("maven-snapshots"));
    assertThat(response.get(2).getName(), is("npm-hosted"));
  }

  @Test
  void testGetRepositoriesIncludingSnapshotVersionPolicyOnly() {
    // Includes-only filter: only repos with versionPolicy=SNAPSHOT pass; non-Maven repos
    // without a versionPolicy are excluded because they cannot match an explicit include list.
    Format maven2 = new Format("maven2")
    {
    };
    Format npm = new Format("npm")
    {
    };

    Repository mavenSnapshots = mockRepository("maven-snapshots", maven2, hostedType,
        "http://localhost:8081/repository/maven-snapshots/", true, Map.of(), "SNAPSHOT");
    Repository mavenReleases = mockRepository("maven-releases", maven2, hostedType,
        "http://localhost:8081/repository/maven-releases/", true, Map.of(), "RELEASE");
    Repository npmHosted = mockRepository("npm-hosted", npm, hostedType,
        "http://localhost:8081/repository/npm-hosted/", true, Map.of()); // no versionPolicy

    List<Repository> repositories = List.of(mavenSnapshots, mavenReleases, npmHosted);
    when(repositoryPermissionChecker.userCanBrowseRepositories(repositories)).thenReturn(repositories);
    when(repositoryManager.browse()).thenReturn(repositories);

    List<RepositoryXO> response = underTest.getRepositories(null, false, false, null, null, "SNAPSHOT");

    assertThat(response.size(), is(1));
    assertThat(response.get(0).getName(), is("maven-snapshots"));
  }

  @Test
  void getRepositoriesResolvesFacetViaComponentLookupHelper() {
    // The facet class name is resolved via GlobalComponentLookupHelper (the same resolver the
    // Classic RepositoryUiService uses), so facet classes from any plugin resolve identically.
    Format maven2 = new Format("maven2")
    {
    };
    Repository withFacet = mockRepository("with-facet", maven2, hostedType,
        "http://localhost:8081/repository/with-facet/", true,
        Map.of(PurgeUnusedFacet.class, mock(PurgeUnusedFacet.class)));
    Repository withoutFacet = mockRepository("without-facet", maven2, hostedType,
        "http://localhost:8081/repository/without-facet/", true, Map.of());
    // Build the exception before stubbing: constructing it touches the mock, which would trip
    // Mockito's "unfinished stubbing" check if done inside thenThrow(...).
    MissingFacetException missing = new MissingFacetException(withoutFacet, PurgeUnusedFacet.class);
    when(withoutFacet.facet(PurgeUnusedFacet.class)).thenThrow(missing);

    List<Repository> repositories = List.of(withFacet, withoutFacet);
    when(repositoryManager.browse()).thenReturn(repositories);
    when(repositoryPermissionChecker.userCanBrowseRepositories(repositories)).thenReturn(repositories);
    Mockito.<Class<?>>when(componentLookupHelper.type("org.sonatype.nexus.repository.purge.PurgeUnusedFacet"))
        .thenReturn(PurgeUnusedFacet.class);

    List<RepositoryXO> response = underTest.getRepositories(
        null, false, false, null, "org.sonatype.nexus.repository.purge.PurgeUnusedFacet", null);

    assertThat(response.size(), is(1));
    assertThat(response.get(0).getName(), is("with-facet"));
  }

  @Test
  void getRepositoriesUnresolvedFacetReturnsNoRepos() {
    // A non-blank facet filter that resolves to nothing must exclude every repository (Classic:
    // an unknown facet type matches no repo), NOT fall through to "all repos".
    Format maven2 = new Format("maven2")
    {
    };
    Repository repo = mockRepository("maven-releases", maven2, hostedType,
        "http://localhost:8081/repository/maven-releases/", true, Map.of());

    List<Repository> repositories = List.of(repo);
    when(repositoryManager.browse()).thenReturn(repositories);
    when(repositoryPermissionChecker.userCanBrowseRepositories(repositories)).thenReturn(repositories);
    // Allowed package, but the class cannot be resolved -> the filter must exclude everything.
    when(componentLookupHelper.type("org.sonatype.nexus.repository.NonexistentFacet")).thenReturn(null);

    List<RepositoryXO> response =
        underTest.getRepositories(null, false, false, null, "org.sonatype.nexus.repository.NonexistentFacet", null);

    assertThat(response.isEmpty(), is(true));
  }

  @Test
  void getRepositoryReturnsNotFoundWhenRepositoryUnknown() {
    when(authorizingRepositoryManager.getRepositoryWithAdmin("unknown-repo")).thenReturn(Optional.empty());

    WebApplicationMessageException exception =
        assertThrows(WebApplicationMessageException.class, () -> underTest.getRepository("unknown-repo"));

    assertThat(exception.getResponse().getStatus(), is(404));
  }

  @Test
  void getSigningPassphraseReturnsNotFoundWhenRepositoryUnknown() {
    when(authorizingRepositoryManager.getRepositoryWithAdmin("unknown-repo")).thenReturn(Optional.empty());

    WebApplicationMessageException exception =
        assertThrows(WebApplicationMessageException.class, () -> underTest.getSigningPassphrase("unknown-repo"));

    assertThat(exception.getResponse().getStatus(), is(404));
  }

  @Test
  void testGetDetails() {
    Format maven2 = new Format("maven2")
    {
    };
    Format nuget = new Format("nuget")
    {
    };
    List<Repository> repositories = List.of(
        mockRepository("maven-central", maven2, proxyType, "http://localhost:8081/repository/maven-central/", true,
            Map.of(HttpClientFacet.class, mockHttpFacet("Ready to Connect", null))),
        mockRepository("maven-public", maven2, groupType, "http://localhost:8081/repository/maven-public/", true,
            Map.of()),
        mockRepository("maven-releases", maven2, hostedType, "http://localhost:8081/repository/maven-releases/", true,
            Map.of()),
        mockRepository("maven-snapshots", maven2, hostedType, "http://localhost:8081/repository/maven-snapshots/",
            false, Map.of()),
        mockRepository("nuget-group", nuget, groupType, "http://localhost:8081/repository/nuget-group/", true,
            Map.of()),
        mockRepository("nuget-hosted", nuget, hostedType, "http://localhost:8081/repository/nuget-hosted/", true,
            Map.of()),
        mockRepository("nuget.org-proxy", nuget, proxyType, "http://localhost:8081/repository/nuget.org-proxy/", true,
            Map.of(HttpClientFacet.class, mockHttpFacet("Remote Auto Blocked and Unavailable",
                "java.net.UnknownHostException: api.example.org: nodename nor servname provided, or not known"))));
    repositories.forEach(
        repo -> when(repositoryPermissionChecker.userHasRepositoryAdminPermission(repo, READ)).thenReturn(true));
    when(repositoryManager.browse()).thenReturn(repositories);

    List<RepositoryDetailXO> details = underTest.getRepositoryDetails();

    assertThat(details.get(0).getName(), is("maven-central"));
    assertThat(details.get(0).getType(), is("proxy"));
    assertThat(details.get(0).getFormat(), is("maven2"));
    assertThat(details.get(0).getUrl(), is("http://localhost:8081/repository/maven-central/"));
    assertThat(details.get(0).getStatus().isOnline(), is(true));
    assertThat(details.get(0).getStatus().getDescription(), is("Ready to Connect"));
    assertThat(details.get(0).getStatus().getReason(), is(nullValue()));

    assertThat(details.get(1).getName(), is("maven-public"));
    assertThat(details.get(1).getType(), is("group"));
    assertThat(details.get(1).getFormat(), is("maven2"));
    assertThat(details.get(1).getUrl(), is("http://localhost:8081/repository/maven-public/"));
    assertThat(details.get(1).getStatus().isOnline(), is(true));
    assertThat(details.get(1).getStatus().getDescription(), is(nullValue()));
    assertThat(details.get(1).getStatus().getReason(), is(nullValue()));

    assertThat(details.get(2).getName(), is("maven-releases"));
    assertThat(details.get(2).getType(), is("hosted"));
    assertThat(details.get(2).getFormat(), is("maven2"));
    assertThat(details.get(2).getUrl(), is("http://localhost:8081/repository/maven-releases/"));
    assertThat(details.get(2).getStatus().isOnline(), is(true));
    assertThat(details.get(2).getStatus().getDescription(), is(nullValue()));
    assertThat(details.get(2).getStatus().getReason(), is(nullValue()));

    assertThat(details.get(3).getName(), is("maven-snapshots"));
    assertThat(details.get(3).getType(), is("hosted"));
    assertThat(details.get(3).getFormat(), is("maven2"));
    assertThat(details.get(3).getUrl(), is("http://localhost:8081/repository/maven-snapshots/"));
    assertThat(details.get(3).getStatus().isOnline(), is(false));
    assertThat(details.get(3).getStatus().getDescription(), is(nullValue()));
    assertThat(details.get(3).getStatus().getReason(), is(nullValue()));

    assertThat(details.get(4).getName(), is("nuget-group"));
    assertThat(details.get(4).getType(), is("group"));
    assertThat(details.get(4).getFormat(), is("nuget"));
    assertThat(details.get(4).getUrl(), is("http://localhost:8081/repository/nuget-group/"));
    assertThat(details.get(4).getStatus().isOnline(), is(true));
    assertThat(details.get(4).getStatus().getDescription(), is(nullValue()));
    assertThat(details.get(4).getStatus().getReason(), is(nullValue()));

    assertThat(details.get(5).getName(), is("nuget-hosted"));
    assertThat(details.get(5).getType(), is("hosted"));
    assertThat(details.get(5).getFormat(), is("nuget"));
    assertThat(details.get(5).getUrl(), is("http://localhost:8081/repository/nuget-hosted/"));
    assertThat(details.get(5).getStatus().isOnline(), is(true));
    assertThat(details.get(5).getStatus().getDescription(), is(nullValue()));
    assertThat(details.get(5).getStatus().getReason(), is(nullValue()));

    assertThat(details.get(6).getName(), is("nuget.org-proxy"));
    assertThat(details.get(6).getType(), is("proxy"));
    assertThat(details.get(6).getFormat(), is("nuget"));
    assertThat(details.get(6).getUrl(), is("http://localhost:8081/repository/nuget.org-proxy/"));
    assertThat(details.get(6).getStatus().isOnline(), is(true));
    assertThat(details.get(6).getStatus().getDescription(), is("Remote Auto Blocked and Unavailable"));
    assertThat(details.get(6).getStatus().getReason(),
        is("java.net.UnknownHostException: api.example.org: nodename nor servname provided, or not known"));
  }

  @Test
  void shouldReturnEmptyDetailsWhenNoRepositoriesExist() {
    when(repositoryManager.browse()).thenReturn(Collections.emptyList());

    List<RepositoryDetailXO> details = underTest.getRepositoryDetails();

    assertThat(details.isEmpty(), is(true));
  }

  @Test
  void shouldLeaveSizeAndCountsNullWhenMetricsServiceHasNoEntryForRepository() {
    Format maven2 = new Format("maven2")
    {
    };
    Repository mavenHosted = mockRepository("maven-releases", maven2, hostedType,
        "http://localhost:8081/repository/maven-releases/", true, Map.of());
    when(repositoryPermissionChecker.userHasRepositoryAdminPermission(mavenHosted, READ)).thenReturn(true);
    when(repositoryManager.browse()).thenReturn(List.of(mavenHosted));
    // repositoryMetricsService.list() returns empty (see @BeforeEach), so the lookup
    // for "maven-releases" yields null and the DTO setters are never invoked.

    List<RepositoryDetailXO> details = underTest.getRepositoryDetails();

    assertThat(details.size(), is(1));
    assertThat(details.get(0).getSize(), is(nullValue()));
    assertThat(details.get(0).getComponentCount(), is(nullValue()));
    assertThat(details.get(0).getAssetCount(), is(nullValue()));
  }

  @Test
  void shouldLeaveSizeAndCountsNullWhenMetricsServiceAbsent() {
    // OSS distributions inject null for the optional RepositoryMetricsService; the resource
    // must still return details with size/count fields left unset rather than NPE.
    RepositoryInternalResource resourceWithoutMetrics = new RepositoryInternalResource(
        formats,
        repositoryManager,
        repositoryPermissionChecker,
        proxyType,
        recipes,
        authorizingRepositoryManager,
        convertersByFormat,
        defaultAdapter,
        null,
        componentLookupHelper);
    Format maven2 = new Format("maven2")
    {
    };
    Repository mavenHosted = mockRepository("maven-releases", maven2, hostedType,
        "http://localhost:8081/repository/maven-releases/", true, Map.of());
    when(repositoryPermissionChecker.userHasRepositoryAdminPermission(mavenHosted, READ)).thenReturn(true);
    when(repositoryManager.browse()).thenReturn(List.of(mavenHosted));

    List<RepositoryDetailXO> details = resourceWithoutMetrics.getRepositoryDetails();

    assertThat(details.size(), is(1));
    assertThat(details.get(0).getSize(), is(nullValue()));
    assertThat(details.get(0).getComponentCount(), is(nullValue()));
    assertThat(details.get(0).getAssetCount(), is(nullValue()));
  }

  private static Repository mockRepository(
      final String name,
      final Format format,
      final Type type,
      final String url,
      final boolean online,
      final Map<Class<? extends Facet>, Facet> facets)
  {
    return mockRepository(name, format, type, url, online, facets, null);
  }

  private static Repository mockRepository(
      final String name,
      final Format format,
      final Type type,
      final String url,
      final boolean online,
      final Map<Class<? extends Facet>, Facet> facets,
      final String versionPolicy)
  {
    Repository repository = mock(Repository.class);
    lenient().when(repository.getName()).thenReturn(name);
    lenient().when(repository.getFormat()).thenReturn(format);
    lenient().when(repository.getType()).thenReturn(type);
    lenient().when(repository.getUrl()).thenReturn(url);
    ConfigurationData configuration = new ConfigurationData();
    if (versionPolicy != null) {
      configuration.setAttributes(Map.of("maven", Map.of("versionPolicy", versionPolicy)));
    }
    lenient().when(repository.getConfiguration()).thenReturn(configuration);
    facets.forEach((clazz, facet) -> lenient().when(repository.facet(clazz)).thenAnswer(invocation -> facet));
    configuration.setOnline(online);
    return repository;
  }

  private static Facet mockHttpFacet(final String description, final String reason) {
    HttpClientFacet facet = mock(HttpClientFacet.class);
    RemoteConnectionStatus status = mock(RemoteConnectionStatus.class);
    lenient().when(facet.getStatus()).thenReturn(status);
    lenient().when(status.getDescription()).thenReturn(description);
    lenient().when(status.getReason()).thenReturn(reason);
    return facet;
  }
}

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
package org.sonatype.nexus.maven.tasks;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.sonatype.nexus.proxy.maven.MavenRepository;
import org.sonatype.nexus.proxy.maven.RepositoryPolicy;
import org.sonatype.nexus.proxy.maven.maven1.Maven1ContentClass;
import org.sonatype.nexus.proxy.maven.maven2.Maven2ContentClass;
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;
import org.sonatype.nexus.proxy.repository.GroupRepository;
import org.sonatype.nexus.proxy.repository.LocalStatus;
import org.sonatype.nexus.proxy.repository.ProxyRepository;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.repository.RepositoryKind;
import org.sonatype.nexus.proxy.targets.TargetRegistry;
import org.sonatype.nexus.proxy.walker.Walker;
import org.sonatype.sisu.litmus.testsupport.TestSupport;

import com.google.common.collect.Maps;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @since 2.5
 */
@RunWith(Parameterized.class)
public class DefaultReleaseRemoverTest extends TestSupport
{
  @Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][]{
        {false}, {true}
    });
  }

  @Parameter
  public boolean indexBackend;

  public static final String REPO_ID = "foo";

  private static final String TARGET_ID = "foo-target";

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  public static final Maven2ContentClass MAVEN_2_CONTENT_CLASS = new Maven2ContentClass();

  public static final Maven1ContentClass MAVEN_1_CONTENT_CLASS = new Maven1ContentClass();

  @Test
  public void shouldFailOnProxyRepositories()
      throws Exception
  {
    final RepositoryRegistry repositoryRegistry = mock(RepositoryRegistry.class);
    final TargetRegistry targetRegistry = mock(TargetRegistry.class);
    final Repository proxyRepository = mock(Repository.class);
    final RepositoryKind proxyRepositoryKind = mock(RepositoryKind.class);

    when(repositoryRegistry.getRepository(REPO_ID)).thenReturn(proxyRepository);
    when(proxyRepository.getRepositoryContentClass()).thenReturn(MAVEN_2_CONTENT_CLASS);
    when(proxyRepository.getLocalStatus()).thenReturn(LocalStatus.IN_SERVICE);
    when(proxyRepository.getRepositoryKind()).thenReturn(proxyRepositoryKind);
    when(proxyRepositoryKind.isFacetAvailable(ProxyRepository.class)).thenReturn(true);

    thrown.expect(IllegalArgumentException.class);
    final Map<String, ReleaseRemoverBackend> backendMap = Maps.newHashMap();
    new DefaultReleaseRemover(repositoryRegistry, targetRegistry, MAVEN_2_CONTENT_CLASS, backendMap)
        .removeReleases(new ReleaseRemovalRequest(REPO_ID, 1, indexBackend, null));
  }

  @Test
  public void shouldFailOnNonMaven2Repositories()
      throws Exception
  {
    final RepositoryRegistry repositoryRegistry = mock(RepositoryRegistry.class);
    final TargetRegistry targetRegistry = mock(TargetRegistry.class);
    final Repository repository = mock(Repository.class);

    when(repositoryRegistry.getRepository(REPO_ID)).thenReturn(repository);
    when(repository.getRepositoryContentClass()).thenReturn(MAVEN_1_CONTENT_CLASS);

    thrown.expect(IllegalArgumentException.class);
    final Map<String, ReleaseRemoverBackend> backendMap = Maps.newHashMap();
    new DefaultReleaseRemover(repositoryRegistry, targetRegistry, MAVEN_2_CONTENT_CLASS, backendMap)
        .removeReleases(new ReleaseRemovalRequest(REPO_ID, 1, indexBackend, null));
  }

  @Test
  public void shouldFailOnOutOfServiceRepositories()
      throws Exception
  {
    final RepositoryRegistry repositoryRegistry = mock(RepositoryRegistry.class);
    final TargetRegistry targetRegistry = mock(TargetRegistry.class);

    final Repository repository = mock(Repository.class);

    when(repositoryRegistry.getRepository(REPO_ID)).thenReturn(repository);
    when(repository.getRepositoryContentClass()).thenReturn(MAVEN_2_CONTENT_CLASS);
    when(repository.getLocalStatus()).thenReturn(LocalStatus.OUT_OF_SERVICE);

    thrown.expect(IllegalArgumentException.class);
    final Map<String, ReleaseRemoverBackend> backendMap = Maps.newHashMap();
    new DefaultReleaseRemover(repositoryRegistry, targetRegistry, MAVEN_2_CONTENT_CLASS, backendMap)
        .removeReleases(new ReleaseRemovalRequest(REPO_ID, 1, indexBackend, null));
  }

  @Test
  public void shouldFailOnGroupRepositories()
      throws Exception
  {
    final RepositoryRegistry repositoryRegistry = mock(RepositoryRegistry.class);
    final TargetRegistry targetRegistry = mock(TargetRegistry.class);
    final Repository repository = mock(Repository.class);
    final RepositoryKind repositoryKind = mock(RepositoryKind.class);

    when(repositoryRegistry.getRepository(REPO_ID)).thenReturn(repository);
    when(repository.getRepositoryContentClass()).thenReturn(MAVEN_2_CONTENT_CLASS);
    when(repository.getLocalStatus()).thenReturn(LocalStatus.IN_SERVICE);
    when(repository.getRepositoryKind()).thenReturn(repositoryKind);
    when(repositoryKind.isFacetAvailable(GroupRepository.class)).thenReturn(true);

    thrown.expect(IllegalArgumentException.class);
    final Map<String, ReleaseRemoverBackend> backendMap = Maps.newHashMap();
    new DefaultReleaseRemover(repositoryRegistry, targetRegistry, MAVEN_2_CONTENT_CLASS, backendMap)
        .removeReleases(new ReleaseRemovalRequest(REPO_ID, 1, indexBackend, null));
  }

  @Test
  public void shouldFailOnSnapshotRepositories()
      throws Exception
  {
    final RepositoryRegistry repositoryRegistry = mock(RepositoryRegistry.class);
    final TargetRegistry targetRegistry = mock(TargetRegistry.class);
    final Repository repository = mock(Repository.class);
    final RepositoryKind repositoryKind = mock(RepositoryKind.class);
    final MavenRepository mavenRepository = mock(MavenRepository.class);

    when(repositoryRegistry.getRepository(REPO_ID)).thenReturn(repository);
    when(repository.getRepositoryContentClass()).thenReturn(MAVEN_2_CONTENT_CLASS);
    when(repository.getLocalStatus()).thenReturn(LocalStatus.IN_SERVICE);
    when(repository.getRepositoryKind()).thenReturn(repositoryKind);
    when(repositoryKind.isFacetAvailable(GroupRepository.class)).thenReturn(false);
    when(repository.adaptToFacet(MavenRepository.class)).thenReturn(mavenRepository);
    when(mavenRepository.getRepositoryPolicy()).thenReturn(RepositoryPolicy.SNAPSHOT);

    thrown.expect(IllegalArgumentException.class);
    final Map<String, ReleaseRemoverBackend> backendMap = Maps.newHashMap();
    new DefaultReleaseRemover(repositoryRegistry, targetRegistry, MAVEN_2_CONTENT_CLASS, backendMap)
        .removeReleases(new ReleaseRemovalRequest(REPO_ID, 1, indexBackend, null));
  }

  @Test
  public void shouldFailOnMixedRepositories()
      throws Exception
  {
    final RepositoryRegistry repositoryRegistry = mock(RepositoryRegistry.class);
    final TargetRegistry targetRegistry = mock(TargetRegistry.class);
    final Repository repository = mock(Repository.class);
    final RepositoryKind repositoryKind = mock(RepositoryKind.class);
    final MavenRepository mavenRepository = mock(MavenRepository.class);

    when(repositoryRegistry.getRepository(REPO_ID)).thenReturn(repository);
    when(repository.getRepositoryContentClass()).thenReturn(MAVEN_1_CONTENT_CLASS);
    when(repository.getLocalStatus()).thenReturn(LocalStatus.IN_SERVICE);
    when(repository.getRepositoryKind()).thenReturn(repositoryKind);
    when(repositoryKind.isFacetAvailable(GroupRepository.class)).thenReturn(false);
    when(repository.adaptToFacet(MavenRepository.class)).thenReturn(mavenRepository);
    when(mavenRepository.getRepositoryPolicy()).thenReturn(RepositoryPolicy.MIXED);

    thrown.expect(IllegalArgumentException.class);
    final Map<String, ReleaseRemoverBackend> backendMap = Maps.newHashMap();
    new DefaultReleaseRemover(repositoryRegistry, targetRegistry, MAVEN_2_CONTENT_CLASS, backendMap)
        .removeReleases(new ReleaseRemovalRequest(REPO_ID, 1, indexBackend, null));
  }

  @Test
  public void shouldFailOnMissingTarget()
      throws Exception
  {
    final RepositoryRegistry repositoryRegistry = mock(RepositoryRegistry.class);
    final TargetRegistry targetRegistry = mock(TargetRegistry.class);
    final Repository repository = mock(Repository.class);

    when(repositoryRegistry.getRepository(REPO_ID)).thenReturn(repository);
    when(targetRegistry.getRepositoryTarget(TARGET_ID)).thenReturn(null);

    thrown.expect(IllegalStateException.class);
    final Map<String, ReleaseRemoverBackend> backendMap = Maps.newHashMap();
    new DefaultReleaseRemover(repositoryRegistry, targetRegistry, MAVEN_2_CONTENT_CLASS, backendMap)
        .removeReleases(new ReleaseRemovalRequest(REPO_ID, 1, indexBackend, TARGET_ID));
  }

  @Test
  public void testOnEmptyRepo()
      throws Exception
  {
    final RepositoryRegistry repositoryRegistry = mock(RepositoryRegistry.class);
    final TargetRegistry targetRegistry = mock(TargetRegistry.class);
    final Repository repository = mock(Repository.class);
    final RepositoryKind repositoryKind = mock(RepositoryKind.class);
    final MavenRepository mavenRepository = mock(MavenRepository.class);

    when(repositoryRegistry.getRepository(REPO_ID)).thenReturn(repository);
    when(targetRegistry.getRepositoryTarget(TARGET_ID)).thenReturn(null);
    when(repositoryRegistry.getRepository(REPO_ID)).thenReturn(repository);
    when(repository.getRepositoryContentClass()).thenReturn(MAVEN_2_CONTENT_CLASS);
    when(repository.getLocalStatus()).thenReturn(LocalStatus.IN_SERVICE);
    when(repository.getRepositoryKind()).thenReturn(repositoryKind);
    when(repositoryKind.isFacetAvailable(ProxyRepository.class)).thenReturn(false);
    when(repository.adaptToFacet(MavenRepository.class)).thenReturn(mavenRepository);
    when(mavenRepository.getRepositoryPolicy()).thenReturn(RepositoryPolicy.RELEASE);

    when(mavenRepository.getLocalStatus()).thenReturn(LocalStatus.IN_SERVICE);

    final Map<String, ReleaseRemoverBackend> backendMap = Maps.newHashMap();
    backendMap.put(ReleaseRemoverBackend.WALKER, mock(ReleaseRemoverBackend.class));
    backendMap.put(ReleaseRemoverBackend.INDEX, mock(ReleaseRemoverBackend.class));
    ReleaseRemovalResult releaseRemovalResult =
        new DefaultReleaseRemover(repositoryRegistry, targetRegistry, MAVEN_2_CONTENT_CLASS, backendMap)
            .removeReleases(new ReleaseRemovalRequest(REPO_ID, 1, indexBackend, ""));
    assertThat("Default state until after a 'real' walk should be failed", releaseRemovalResult.isSuccessful(),
        is(false));
    assertThat(releaseRemovalResult.getDeletedFileCount(), is(0));
  }
}

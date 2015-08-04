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

import org.sonatype.nexus.proxy.maven.MavenRepository;
import org.sonatype.nexus.proxy.maven.maven2.Maven2ContentClass;
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;
import org.sonatype.nexus.proxy.repository.GroupRepository;
import org.sonatype.nexus.proxy.repository.LocalStatus;
import org.sonatype.nexus.proxy.repository.ProxyRepository;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.repository.RepositoryKind;
import org.sonatype.nexus.proxy.walker.Walker;
import org.sonatype.sisu.litmus.testsupport.TestSupport;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @since 2.3
 */
public class DefaultSnapshotRemoverTest
    extends TestSupport
{

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  /**
   * NEXUS-5414: When a proxy repository is the repository to be removed expect to throw an exception.
   */
  @Test
  public void shouldFailWhenProxyRepositories()
      throws Exception
  {
    final Maven2ContentClass maven2ContentClass = new Maven2ContentClass();
    final RepositoryRegistry repositoryRegistry = mock(RepositoryRegistry.class);
    final Repository proxyRepository = mock(Repository.class);
    final RepositoryKind proxyRepositoryKind = mock(RepositoryKind.class);

    when(repositoryRegistry.getRepository("foo-proxy")).thenReturn(proxyRepository);
    when(proxyRepository.getRepositoryContentClass()).thenReturn(maven2ContentClass);
    when(proxyRepository.getLocalStatus()).thenReturn(LocalStatus.IN_SERVICE);
    when(proxyRepository.getRepositoryKind()).thenReturn(proxyRepositoryKind);
    when(proxyRepositoryKind.isFacetAvailable(ProxyRepository.class)).thenReturn(true);

    thrown.expect(IllegalArgumentException.class);
    new DefaultSnapshotRemover(repositoryRegistry, mock(Walker.class), maven2ContentClass)
    {
      @Override
      protected SnapshotRemovalRepositoryResult removeSnapshotsFromMavenRepository(
          final MavenRepository repository,
          final SnapshotRemovalRequest request)
      {
        return new SnapshotRemovalRepositoryResult(repository.getId(), false);
      }
    }.removeSnapshots(
        new SnapshotRemovalRequest("foo-proxy", 1, 1, true)
    );
  }

  /**
   * NEXUS-5414: When a proxy repository is the part of a group repository, is skipped.
   */
  @Test
  public void shouldSkipProxyRepositoriesWhenPartOfAGroup()
      throws Exception
  {
    final Maven2ContentClass maven2ContentClass = new Maven2ContentClass();
    final RepositoryRegistry repositoryRegistry = mock(RepositoryRegistry.class);
    final Repository proxyRepository = mock(Repository.class);
    final GroupRepository groupRepository = mock(GroupRepository.class);
    final RepositoryKind groupRepositoryKind = mock(RepositoryKind.class);
    final RepositoryKind proxyRepositoryKind = mock(RepositoryKind.class);

    when(repositoryRegistry.getRepository("foo-group")).thenReturn(groupRepository);
    when(groupRepository.getRepositoryContentClass()).thenReturn(maven2ContentClass);
    when(proxyRepository.getRepositoryContentClass()).thenReturn(maven2ContentClass);
    when(groupRepository.getLocalStatus()).thenReturn(LocalStatus.IN_SERVICE);
    when(proxyRepository.getLocalStatus()).thenReturn(LocalStatus.IN_SERVICE);
    when(groupRepository.getRepositoryKind()).thenReturn(groupRepositoryKind);
    when(proxyRepository.getRepositoryKind()).thenReturn(proxyRepositoryKind);
    when(groupRepositoryKind.isFacetAvailable(GroupRepository.class)).thenReturn(true);
    when(proxyRepositoryKind.isFacetAvailable(ProxyRepository.class)).thenReturn(true);
    when(groupRepository.adaptToFacet(GroupRepository.class)).thenReturn(groupRepository);
    when(groupRepository.getMemberRepositories()).thenReturn(Arrays.asList(proxyRepository));

    final SnapshotRemovalResult result =
        new DefaultSnapshotRemover(repositoryRegistry, mock(Walker.class), maven2ContentClass)
        {
          @Override
          protected SnapshotRemovalRepositoryResult removeSnapshotsFromMavenRepository(
              final MavenRepository repository,
              final SnapshotRemovalRequest request)
          {
            return new SnapshotRemovalRepositoryResult(repository.getId(), false);
          }
        }.removeSnapshots(
            new SnapshotRemovalRequest("foo-group", 1, 1, true)
        );
    assertThat(result.isSuccessful(), is(true));
    assertThat(result.getProcessedRepositories().isEmpty(), is(true));
  }

  /**
   * NEXUS-5414: When all repositories should be processed, proxy repositories are skipped
   */
  @Test
  public void shouldSkipProxyRepositoriesWhenProcessingAllRepositories()
      throws Exception
  {
    final Maven2ContentClass maven2ContentClass = new Maven2ContentClass();
    final RepositoryRegistry repositoryRegistry = mock(RepositoryRegistry.class);
    final Repository proxyRepository = mock(Repository.class);
    final RepositoryKind proxyRepositoryKind = mock(RepositoryKind.class);

    when(repositoryRegistry.getRepositories()).thenReturn(Arrays.asList(proxyRepository));
    when(proxyRepository.getRepositoryContentClass()).thenReturn(maven2ContentClass);
    when(proxyRepository.getLocalStatus()).thenReturn(LocalStatus.IN_SERVICE);
    when(proxyRepository.getRepositoryKind()).thenReturn(proxyRepositoryKind);
    when(proxyRepositoryKind.isFacetAvailable(ProxyRepository.class)).thenReturn(true);

    final SnapshotRemovalResult result =
        new DefaultSnapshotRemover(repositoryRegistry, mock(Walker.class), maven2ContentClass)
        {
          @Override
          protected SnapshotRemovalRepositoryResult removeSnapshotsFromMavenRepository(
              final MavenRepository repository,
              final SnapshotRemovalRequest request)
          {
            return new SnapshotRemovalRepositoryResult(repository.getId(), false);
          }
        }.removeSnapshots(
            new SnapshotRemovalRequest(null, 1, 1, true)
        );
    assertThat(result.isSuccessful(), is(true));
    assertThat(result.getProcessedRepositories().isEmpty(), is(true));
  }

}

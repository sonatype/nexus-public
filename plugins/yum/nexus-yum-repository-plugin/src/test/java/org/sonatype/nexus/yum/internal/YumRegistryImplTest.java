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
package org.sonatype.nexus.yum.internal;

import java.util.concurrent.TimeoutException;

import javax.inject.Inject;

import org.sonatype.nexus.proxy.maven.MavenHostedRepository;
import org.sonatype.nexus.proxy.maven.MavenRepository;
import org.sonatype.nexus.proxy.repository.HostedRepository;
import org.sonatype.nexus.proxy.repository.RepositoryKind;
import org.sonatype.nexus.scheduling.NexusScheduler;
import org.sonatype.nexus.yum.YumRegistry;
import org.sonatype.nexus.yum.internal.support.SchedulerYumNexusTestSupport;

import com.google.code.tempusfugit.temporal.Condition;
import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class YumRegistryImplTest
    extends SchedulerYumNexusTestSupport
{

  private static final String REPO_ID = "rpm-snapshots";

  @Inject
  private YumRegistry yumRegistry;

  @Inject
  private NexusScheduler nexusScheduler;

  @Test
  public void shouldScanRepository()
      throws Exception
  {
    final MavenHostedRepository repository = mock(MavenHostedRepository.class);
    when(repository.getId()).thenReturn(REPO_ID);
    when(repository.getLocalUrl()).thenReturn(rpmsDir().toURI().toASCIIString());
    when(repository.adaptToFacet(HostedRepository.class)).thenReturn(repository);
    when(repository.adaptToFacet(MavenRepository.class)).thenReturn(repository);
    when(repository.adaptToFacet(MavenHostedRepository.class)).thenReturn(repository);

    final RepositoryKind repositoryKind = mock(RepositoryKind.class);
    when(repository.getRepositoryKind()).thenReturn(repositoryKind);
    when(repositoryKind.isFacetAvailable(MavenRepository.class)).thenReturn(true);
    when(repositoryKind.isFacetAvailable(HostedRepository.class)).thenReturn(true);
    when(repositoryKind.isFacetAvailable(MavenHostedRepository.class)).thenReturn(true);

    yumRegistry.register(repository);

    waitForAllTasksToBeDone();

    Assert.assertNotNull(yumRegistry.get(REPO_ID));
  }

  @Test
  public void shouldUnregisterRepository()
      throws Exception
  {
    MavenRepository repository = createRepository(true);

    yumRegistry.register(repository);
    Assert.assertTrue(yumRegistry.isRegistered(repository.getId()));

    yumRegistry.unregister(repository.getId());
    Assert.assertFalse(yumRegistry.isRegistered(repository.getId()));
  }

  @Test
  public void shouldNotFindRepository()
      throws Exception
  {
    assertThat(yumRegistry.get("blablup"), is(nullValue()));
  }

  @Test
  public void shouldFindRepository()
      throws Exception
  {
    final MavenRepository repository = createRepository(true);
    yumRegistry.register(repository);
    assertThat(yumRegistry.get(repository.getId()), is(notNullValue()));
  }

  private void waitForAllTasksToBeDone()
      throws TimeoutException, InterruptedException
  {
    waitFor(new Condition()
    {
      @Override
      public boolean isSatisfied() {
        return nexusScheduler.getActiveTasks().isEmpty();
      }
    });
  }
}

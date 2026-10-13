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
package org.sonatype.nexus.repository.group.rendered;

import java.time.Duration;
import java.util.Collections;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.scheduling.TaskConfiguration;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link GroupRenderedViewCleanupTask}.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class GroupRenderedViewCleanupTaskTest
{
  @Mock
  private RepositoryManager repositoryManager;

  @Mock
  private GroupRenderedViewFacet facet;

  private GroupRenderedViewCleanupTask underTest;

  @Before
  public void setup() {
    underTest = new GroupRenderedViewCleanupTask(repositoryManager);
  }

  @Test
  public void execute_invokesPruneOnRepositoriesWithFacet() throws Exception {
    Repository repoWithFacet = mock(Repository.class);
    when(repoWithFacet.optionalFacet(GroupRenderedViewFacet.class)).thenReturn(java.util.Optional.of(facet));

    Repository repoWithoutFacet = mock(Repository.class);
    when(repoWithoutFacet.optionalFacet(GroupRenderedViewFacet.class)).thenReturn(java.util.Optional.empty());

    when(repositoryManager.browse()).thenReturn(java.util.Arrays.asList(repoWithFacet, repoWithoutFacet));

    // Set configuration with default days horizon
    TaskConfiguration configuration = new TaskConfiguration();
    configuration.setId("test");
    configuration.setTypeId(GroupRenderedViewCleanupTaskDescriptor.TYPE_ID);
    configuration.setInteger(GroupRenderedViewCleanupTaskDescriptor.DAYS_HORIZON_FIELD_ID, 14);
    underTest.configure(configuration);

    underTest.execute();

    verify(facet).pruneRenderedViews(eq(Duration.ofDays(14)));
  }

  @Test
  public void execute_skipsRepositoriesWithoutFacet() throws Exception {
    Repository repoWithoutFacet = mock(Repository.class);
    when(repoWithoutFacet.optionalFacet(GroupRenderedViewFacet.class)).thenReturn(java.util.Optional.empty());

    when(repositoryManager.browse()).thenReturn(Collections.singletonList(repoWithoutFacet));

    TaskConfiguration configuration = new TaskConfiguration();
    configuration.setId("test");
    configuration.setTypeId(GroupRenderedViewCleanupTaskDescriptor.TYPE_ID);
    configuration.setInteger(GroupRenderedViewCleanupTaskDescriptor.DAYS_HORIZON_FIELD_ID, 14);
    underTest.configure(configuration);

    underTest.execute();

    // No exception - just skips
    verify(facet, never()).pruneRenderedViews(any());
  }

  @Test
  public void execute_usesConfiguredDaysHorizon() throws Exception {
    Repository repo = mock(Repository.class);
    when(repo.optionalFacet(GroupRenderedViewFacet.class)).thenReturn(java.util.Optional.of(facet));
    when(repositoryManager.browse()).thenReturn(Collections.singletonList(repo));

    // Set custom days horizon
    TaskConfiguration configuration = new TaskConfiguration();
    configuration.setId("test");
    configuration.setTypeId(GroupRenderedViewCleanupTaskDescriptor.TYPE_ID);
    configuration.setInteger(GroupRenderedViewCleanupTaskDescriptor.DAYS_HORIZON_FIELD_ID, 30);
    underTest.configure(configuration);

    underTest.execute();

    verify(facet).pruneRenderedViews(eq(Duration.ofDays(30)));
  }

  @Test
  public void execute_oneRepositoryFailing_stillPrunesTheRest() throws Exception {
    GroupRenderedViewFacet failingFacet = mock(GroupRenderedViewFacet.class);
    when(failingFacet.pruneRenderedViews(any())).thenThrow(new RuntimeException("storage unreadable"));

    // Deliberately first in iteration order: without per-repository isolation the sweep would abort
    // here and leave every repository after it unpruned until the next scheduled run.
    Repository failingRepo = mock(Repository.class);
    when(failingRepo.getName()).thenReturn("broken-group");
    when(failingRepo.optionalFacet(GroupRenderedViewFacet.class)).thenReturn(java.util.Optional.of(failingFacet));

    Repository healthyRepo = mock(Repository.class);
    when(healthyRepo.optionalFacet(GroupRenderedViewFacet.class)).thenReturn(java.util.Optional.of(facet));

    when(repositoryManager.browse()).thenReturn(java.util.Arrays.asList(failingRepo, healthyRepo));

    underTest.execute();

    verify(facet).pruneRenderedViews(eq(Duration.ofDays(14)));
  }

  @Test
  public void execute_defaultDaysHorizonWhenNotConfigured() throws Exception {
    Repository repo = mock(Repository.class);
    when(repo.optionalFacet(GroupRenderedViewFacet.class)).thenReturn(java.util.Optional.of(facet));
    when(repositoryManager.browse()).thenReturn(Collections.singletonList(repo));

    // No configuration set - uses default
    underTest.execute();

    verify(facet).pruneRenderedViews(eq(Duration.ofDays(14)));
  }
}

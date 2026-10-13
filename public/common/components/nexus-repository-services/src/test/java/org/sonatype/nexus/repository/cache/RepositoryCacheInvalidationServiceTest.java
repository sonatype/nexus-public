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
package org.sonatype.nexus.repository.cache;

import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.distributed.event.service.api.common.RepositoryCacheInvalidationEvent;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.manager.RepositoryManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RepositoryCacheInvalidationServiceTest
{
  @Mock
  private RepositoryManager repositoryManager;

  @Mock
  private EventManager eventManager;

  @Mock
  private Repository repository;

  @Mock
  private Type type;

  private RepositoryCacheInvalidationService underTest;

  @Before
  public void setup() {
    underTest = new RepositoryCacheInvalidationService(repositoryManager, eventManager);
  }

  /**
   * Core fix: a cache-invalidation event for a repository not yet materialized on this node (the create->use
   * window under HA) must be skipped, not NPE. Before the fix this threw NPE on checkNotNull(repository)
   * inside invalidateCaches(...).
   */
  @Test
  public void testOn_repositoryNotMaterialized_skipsWithoutException() {
    when(repositoryManager.get("absent-repo")).thenReturn(null);

    // must not throw
    underTest.on(new RepositoryCacheInvalidationEvent("absent-repo"));

    verify(repositoryManager).get("absent-repo");
    verifyNoInteractions(repository);
  }

  @Test
  public void testOn_repositoryPresent_invalidatesCaches() {
    when(repositoryManager.get("present-repo")).thenReturn(repository);
    when(repository.getType()).thenReturn(type);
    when(type.getValue()).thenReturn("hosted"); // neither group nor proxy -> no facet interaction

    underTest.on(new RepositoryCacheInvalidationEvent("present-repo"));

    // proceeded into invalidateCaches(...), which inspects the repository type
    verify(repository, atLeastOnce()).getType();
  }
}

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
package org.sonatype.nexus.plugins.capabilities.internal.condition;

import org.sonatype.nexus.plugins.capabilities.EventBusTestSupport;
import org.sonatype.nexus.plugins.capabilities.support.condition.RepositoryConditions;
import org.sonatype.nexus.proxy.events.RepositoryRegistryEventAdd;
import org.sonatype.nexus.proxy.events.RepositoryRegistryEventRemove;
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;
import org.sonatype.nexus.proxy.repository.LocalStatus;
import org.sonatype.nexus.proxy.repository.Repository;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link RepositoryExistsCondition} UTs.
 *
 * @since capabilities 2.0
 */
public class RepositoryExistsConditionTest
    extends EventBusTestSupport
{

  static final String TEST_REPOSITORY = "test-repository";

  @Mock
  private Repository repository;

  @Mock
  private RepositoryRegistry repositoryRegistry;

  private RepositoryExistsCondition underTest;

  @Before
  public final void setUpRepositoryExistsCondition()
      throws Exception
  {
    final RepositoryConditions.RepositoryId repositoryId = mock(RepositoryConditions.RepositoryId.class);
    when(repositoryId.get()).thenReturn(TEST_REPOSITORY);

    when(repository.getId()).thenReturn(TEST_REPOSITORY);
    when(repository.getLocalStatus()).thenReturn(LocalStatus.IN_SERVICE);

    underTest = new RepositoryExistsCondition(eventBus, repositoryRegistry, repositoryId);
    underTest.bind();

    verify(eventBus).register(underTest);

    assertThat(underTest.isSatisfied(), is(false));

    underTest.handle(new RepositoryRegistryEventAdd(repositoryRegistry, repository));
  }

  /**
   * Condition should be satisfied initially (because mocking done in setup).
   */
  @Test
  public void satisfiedWhenRepositoryExists() {
    assertThat(underTest.isSatisfied(), is(true));
  }

  /**
   * Condition should become satisfied and notification sent when repository is added.
   */
  @Test
  public void satisfiedWhenRepositoryAdded() {
    assertThat(underTest.isSatisfied(), is(true));

    underTest.handle(new RepositoryRegistryEventRemove(repositoryRegistry, repository));
    underTest.handle(new RepositoryRegistryEventAdd(repositoryRegistry, repository));
    assertThat(underTest.isSatisfied(), is(true));

    verifyEventBusEvents(satisfied(underTest), unsatisfied(underTest), satisfied(underTest));
  }

  /**
   * Condition should become unsatisfied when repository is removed.
   */
  @Test
  public void repositoryIsRemoved() {
    assertThat(underTest.isSatisfied(), is(true));

    underTest.handle(new RepositoryRegistryEventRemove(repositoryRegistry, repository));
    assertThat(underTest.isSatisfied(), is(false));

    verifyEventBusEvents(satisfied(underTest), unsatisfied(underTest));
  }

  /**
   * Condition should remain satisfied when another repository is removed.
   */
  @Test
  public void noReactionWhenAnotherRepositoryIsRemoved() {
    assertThat(underTest.isSatisfied(), is(true));
    final Repository anotherRepository = mock(Repository.class);
    when(anotherRepository.getId()).thenReturn("another");
    underTest.handle(new RepositoryRegistryEventRemove(repositoryRegistry, anotherRepository));
    assertThat(underTest.isSatisfied(), is(true));
  }

  /**
   * Event bus handler is removed when releasing.
   */
  @Test
  public void releaseRemovesItselfAsHandler() {
    underTest.release();

    verify(eventBus).unregister(underTest);
  }

}

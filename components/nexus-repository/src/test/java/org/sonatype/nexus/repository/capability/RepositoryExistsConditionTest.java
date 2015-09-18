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
package org.sonatype.nexus.repository.capability;

import java.util.Collections;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.capability.RepositoryConditions.RepositoryName;
import org.sonatype.nexus.repository.manager.RepositoryCreatedEvent;
import org.sonatype.nexus.repository.manager.RepositoryDeletedEvent;
import org.sonatype.nexus.repository.manager.RepositoryManager;

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
  private RepositoryManager repositoryManager;

  private RepositoryExistsCondition underTest;

  @Before
  public final void setUpRepositoryExistsCondition()
      throws Exception
  {
    when(repositoryManager.browse()).thenReturn(Collections.<Repository>emptyList());

    final RepositoryName repositoryName = mock(RepositoryName.class);
    when(repositoryName.get()).thenReturn(TEST_REPOSITORY);

    when(repository.getName()).thenReturn(TEST_REPOSITORY);

    underTest = new RepositoryExistsCondition(eventBus, repositoryManager, repositoryName);
    underTest.bind();

    verify(eventBus).register(underTest);

    assertThat(underTest.isSatisfied(), is(false));

    underTest.handle(new RepositoryCreatedEvent(repository));
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

    underTest.handle(new RepositoryDeletedEvent(repository));
    underTest.handle(new RepositoryCreatedEvent(repository));
    assertThat(underTest.isSatisfied(), is(true));

    verifyEventBusEvents(satisfied(underTest), unsatisfied(underTest), satisfied(underTest));
  }

  /**
   * Condition should become unsatisfied when repository is removed.
   */
  @Test
  public void repositoryIsRemoved() {
    assertThat(underTest.isSatisfied(), is(true));

    underTest.handle(new RepositoryDeletedEvent(repository));
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
    when(anotherRepository.getName()).thenReturn("another");
    underTest.handle(new RepositoryDeletedEvent(anotherRepository));
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

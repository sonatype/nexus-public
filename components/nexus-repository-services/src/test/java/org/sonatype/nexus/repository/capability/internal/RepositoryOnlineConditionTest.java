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
package org.sonatype.nexus.repository.capability.internal;

import java.util.Collections;
import java.util.function.Supplier;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.manager.RepositoryCreatedEvent;
import org.sonatype.nexus.repository.manager.RepositoryDeletedEvent;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.manager.RepositoryUpdatedEvent;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link RepositoryOnlineCondition} UTs.
 *
 * @since capabilities 2.0
 */
public class RepositoryOnlineConditionTest
    extends EventManagerTestSupport
{

  static final String TEST_REPOSITORY = "test-repository";

  @Mock
  private Repository repository;

  @Mock
  private Configuration configuration;

  @Mock
  private RepositoryManager repositoryManager;

  private RepositoryOnlineCondition underTest;

  @Before
  public final void setUpRepositoryLocalStatusCondition()
      throws Exception
  {
    when(repositoryManager.browse()).thenReturn(Collections.<Repository>emptyList());

    final Supplier<String> repositoryName = () -> TEST_REPOSITORY;

    when(repository.getName()).thenReturn(TEST_REPOSITORY);
    when(repository.getConfiguration()).thenReturn(configuration);
    when(configuration.isOnline()).thenReturn(true);

    underTest = new RepositoryOnlineCondition(eventManager, repositoryManager, repositoryName);
    underTest.bind();

    verify(eventManager).register(underTest);

    assertThat(underTest.isSatisfied(), is(false));

    underTest.handle(new RepositoryCreatedEvent(repository));
  }

  /**
   * Condition should become unsatisfied and notification sent when repository is out of service.
   */
  @Test
  public void unsatisfiedWhenRepositoryIsOutOfService() {
    assertThat(underTest.isSatisfied(), is(true));

    when(configuration.isOnline()).thenReturn(false);
    underTest.handle(new RepositoryUpdatedEvent(repository, null));
    assertThat(underTest.isSatisfied(), is(false));

    verifyEventManagerEvents(satisfied(underTest), unsatisfied(underTest));
  }

  /**
   * Condition should become satisfied and notification sent when repository is back on service.
   */
  @Test
  public void satisfiedWhenRepositoryIsBackToService() {
    assertThat(underTest.isSatisfied(), is(true));

    when(configuration.isOnline()).thenReturn(false);
    underTest.handle(new RepositoryUpdatedEvent(repository, null));

    when(configuration.isOnline()).thenReturn(true);
    underTest.handle(new RepositoryUpdatedEvent(repository, null));
    assertThat(underTest.isSatisfied(), is(true));

    verifyEventManagerEvents(satisfied(underTest), unsatisfied(underTest), satisfied(underTest));
  }

  /**
   * Condition should become unsatisfied when repository is removed.
   */
  @Test
  public void unsatisfiedWhenRepositoryIsRemoved() {
    assertThat(underTest.isSatisfied(), is(true));

    underTest.handle(new RepositoryDeletedEvent(repository));
    assertThat(underTest.isSatisfied(), is(false));

    verifyEventManagerEvents(satisfied(underTest), unsatisfied(underTest));
  }

  /**
   * Event bus handler is removed when releasing.
   */
  @Test
  public void releaseRemovesItselfAsHandler() {
    underTest.release();

    verify(eventManager).unregister(underTest);
  }

}

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
import org.sonatype.nexus.proxy.events.RepositoryEventProxyModeChanged;
import org.sonatype.nexus.proxy.events.RepositoryRegistryEventAdd;
import org.sonatype.nexus.proxy.events.RepositoryRegistryEventRemove;
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;
import org.sonatype.nexus.proxy.repository.ProxyMode;
import org.sonatype.nexus.proxy.repository.ProxyRepository;
import org.sonatype.nexus.proxy.repository.RepositoryKind;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link RepositoryProxyModeCondition} UTs.
 *
 * @since capabilities 2.0
 */
public class RepositoryProxyModeConditionTest
    extends EventBusTestSupport
{

  static final String TEST_REPOSITORY = "test-repository";

  private RepositoryProxyModeCondition underTest;

  @Mock
  private ProxyRepository repository;

  @Mock
  private RepositoryRegistry repositoryRegistry;

  @Before
  public final void setUpRepositoryProxyModeCondition()
      throws Exception
  {
    final RepositoryConditions.RepositoryId repositoryId = mock(RepositoryConditions.RepositoryId.class);
    when(repositoryId.get()).thenReturn(TEST_REPOSITORY);

    final RepositoryKind repositoryKind = mock(RepositoryKind.class);
    when(repositoryKind.isFacetAvailable(ProxyRepository.class)).thenReturn(true);

    when(repository.getId()).thenReturn(TEST_REPOSITORY);
    when(repository.getRepositoryKind()).thenReturn(repositoryKind);
    when(repository.getProxyMode()).thenReturn(ProxyMode.ALLOW);
    when(repository.adaptToFacet(ProxyRepository.class)).thenReturn(repository);

    underTest = new RepositoryProxyModeCondition(
        eventBus, repositoryRegistry, ProxyMode.ALLOW, repositoryId
    );
    underTest.bind();

    verify(eventBus).register(underTest);

    assertThat(underTest.isSatisfied(), is(false));

    underTest.handle(new RepositoryRegistryEventAdd(repositoryRegistry, repository));
  }

  /**
   * Condition should become unsatisfied and notification sent when repository is auto blocked.
   */
  @Test
  public void repositoryIsAutoBlocked() {
    assertThat(underTest.isSatisfied(), is(true));

    when(repository.getProxyMode()).thenReturn(ProxyMode.BLOCKED_AUTO);
    underTest.handle(new RepositoryEventProxyModeChanged(
        repository, ProxyMode.ALLOW, ProxyMode.BLOCKED_AUTO, null
    ));
    assertThat(underTest.isSatisfied(), is(false));

    verifyEventBusEvents(satisfied(underTest), unsatisfied(underTest));
  }

  /**
   * Condition should become unsatisfied and notification sent when repository is manually blocked.
   */
  @Test
  public void satisfiedWhenRepositoryIsManuallyBlocked() {
    assertThat(underTest.isSatisfied(), is(true));

    when(repository.getProxyMode()).thenReturn(ProxyMode.BLOCKED_MANUAL);
    underTest.handle(new RepositoryEventProxyModeChanged(
        repository, ProxyMode.ALLOW, ProxyMode.BLOCKED_MANUAL, null
    ));
    assertThat(underTest.isSatisfied(), is(false));

    verifyEventBusEvents(satisfied(underTest), unsatisfied(underTest));
  }

  /**
   * Condition should become satisfied and notification sent when repository is not blocked anymore.
   */
  @Test
  public void satisfiedWhenRepositoryIsNotBlockingAnymore() {
    assertThat(underTest.isSatisfied(), is(true));

    when(repository.getProxyMode()).thenReturn(ProxyMode.BLOCKED_AUTO);
    underTest.handle(new RepositoryEventProxyModeChanged(
        repository, ProxyMode.ALLOW, ProxyMode.BLOCKED_AUTO, null
    ));

    when(repository.getProxyMode()).thenReturn(ProxyMode.ALLOW);
    underTest.handle(new RepositoryEventProxyModeChanged(
        repository, ProxyMode.BLOCKED_AUTO, ProxyMode.ALLOW, null
    ));
    assertThat(underTest.isSatisfied(), is(true));

    verifyEventBusEvents(satisfied(underTest), unsatisfied(underTest), satisfied(underTest));
  }

  /**
   * Condition should become unsatisfied when repository is removed.
   */
  @Test
  public void unsatisfiedWhenRepositoryIsRemoved() {
    assertThat(underTest.isSatisfied(), is(true));

    underTest.handle(new RepositoryRegistryEventRemove(repositoryRegistry, repository));
    assertThat(underTest.isSatisfied(), is(false));

    verifyEventBusEvents(satisfied(underTest), unsatisfied(underTest));
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

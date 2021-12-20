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
package org.sonatype.nexus.plugins.capabilities.support.condition;

import org.sonatype.nexus.plugins.capabilities.Condition;
import org.sonatype.nexus.plugins.capabilities.internal.condition.RepositoryExistsCondition;
import org.sonatype.nexus.plugins.capabilities.internal.condition.RepositoryLocalStatusCondition;
import org.sonatype.nexus.plugins.capabilities.internal.condition.RepositoryProxyModeCondition;
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;
import org.sonatype.sisu.goodies.eventbus.EventBus;
import org.sonatype.sisu.goodies.testsupport.TestSupport;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;

/**
 * {@link RepositoryConditions} UTs.
 *
 * @since capabilities 2.0
 */
public class RepositoryConditionsTest
    extends TestSupport
{

  private RepositoryConditions underTest;

  @Before
  public void setUpRepositoryConditions() {
    final EventBus eventBus = mock(EventBus.class);
    underTest = new RepositoryConditions(eventBus, mock(RepositoryRegistry.class));
  }

  /**
   * repositoryIsInService() factory method returns expected condition.
   */
  @Test
  public void repositoryIsInService() {
    assertThat(
        underTest.repositoryIsInService(mock(RepositoryConditions.RepositoryId.class)),
        is(Matchers.<Condition>instanceOf(RepositoryLocalStatusCondition.class))
    );
  }

  /**
   * repositoryIsNotBlocked() factory method returns expected condition.
   */
  @Test
  public void repositoryIsNotBlocked() {
    assertThat(
        underTest.repositoryIsNotBlocked(mock(RepositoryConditions.RepositoryId.class)),
        is(Matchers.<Condition>instanceOf(RepositoryProxyModeCondition.class))
    );
  }

  /**
   * repositoryExists() factory method returns expected condition.
   */
  @Test
  public void repositoryExists() {
    assertThat(
        underTest.repositoryExists(mock(RepositoryConditions.RepositoryId.class)),
        is(Matchers.<Condition>instanceOf(RepositoryExistsCondition.class))
    );
  }

}

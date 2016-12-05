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
package org.sonatype.nexus.capability.condition.internal;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.goodies.lifecycle.Lifecycle;
import org.sonatype.nexus.capability.condition.ConditionSupport;
import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.event.EventManager;

import com.google.common.annotations.VisibleForTesting;

import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.TASKS;

/**
 * A condition that is satisfied when nexus is active.
 *
 * @since capabilities 2.0
 */
@Named
@ManagedLifecycle(phase = TASKS)
@Priority(Integer.MAX_VALUE) // make sure this starts first
@Singleton
public class NexusIsActiveCondition
    extends ConditionSupport
    implements Lifecycle
{
  @VisibleForTesting
  public NexusIsActiveCondition(final EventManager eventManager) {
    super(eventManager, false);
    bind();
  }

  @Inject
  public NexusIsActiveCondition(final Provider<EventManager> eventManager) {
    super(eventManager, false);
    bind();
  }

  @Override
  public void start() {
    setSatisfied(true);
  }

  @Override
  public void stop() {
    setSatisfied(false);
  }

  @Override
  protected void doBind() {
    // do nothing
  }

  @Override
  protected void doRelease() {
    // do nothing
  }

  @Override
  public String toString() {
    return "Nexus is active";
  }

  @Override
  public String explainSatisfied() {
    return "Nexus is active";
  }

  @Override
  public String explainUnsatisfied() {
    return "Nexus is not active";
  }
}

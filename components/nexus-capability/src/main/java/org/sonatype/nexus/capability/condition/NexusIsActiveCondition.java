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
package org.sonatype.nexus.capability.condition;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.sonatype.nexus.capability.Condition;
import org.sonatype.nexus.common.app.NexusStartedEvent;
import org.sonatype.nexus.common.app.NexusStoppedEvent;
import org.sonatype.nexus.common.event.EventAware;
import org.sonatype.nexus.common.event.EventBus;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.eventbus.Subscribe;
import org.eclipse.sisu.EagerSingleton;

/**
 * A condition that is satisfied when nexus is active.
 *
 * @since capabilities 2.0
 */
@Named
@EagerSingleton
public class NexusIsActiveCondition
    extends ConditionSupport
    implements Condition, EventAware
{
  @VisibleForTesting
  public NexusIsActiveCondition(final EventBus eventBus) {
    super(eventBus, false);
    bind();
    getEventBus().register(this);
  }

  @Inject
  public NexusIsActiveCondition(final Provider<EventBus> eventBus) {
    super(eventBus, false);
    bind();
    // eventBus registration handled by container due to EventAware
  }

  @Subscribe
  public void handle(final NexusStartedEvent event) {
    setSatisfied(true);
  }

  @Subscribe
  public void handle(final NexusStoppedEvent event) {
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

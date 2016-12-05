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

import org.sonatype.nexus.capability.CapabilityContext;
import org.sonatype.nexus.capability.CapabilityContextAware;
import org.sonatype.nexus.capability.CapabilityEvent.CallbackFailure;
import org.sonatype.nexus.capability.CapabilityEvent.CallbackFailureCleared;
import org.sonatype.nexus.capability.condition.ConditionSupport;
import org.sonatype.nexus.common.event.EventManager;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

import static com.google.common.base.Preconditions.checkState;

/**
 * A condition that is satisfied as long as capability has no failures.
 *
 * @since 2.7
 */
public class CapabilityHasNoFailuresCondition
    extends ConditionSupport
    implements CapabilityContextAware
{

  private CapabilityContext context;

  private String failingAction;

  private Exception failure;

  public CapabilityHasNoFailuresCondition(final EventManager eventManager) {
    super(eventManager);
  }

  @Override
  public CapabilityHasNoFailuresCondition setContext(final CapabilityContext context) {
    checkState(!isActive(), "Cannot contextualize when already bounded");
    checkState(this.context == null, "Already contextualized with '" + this.context + "'");
    this.context = context;

    return this;
  }

  @Override
  protected void doBind() {
    checkState(context != null, "Not yet contextualized");
    getEventManager().register(this);
    failingAction = context.failingAction();
    failure = context.failure();
    setSatisfied(failure == null);
  }

  @Override
  public void doRelease() {
    getEventManager().unregister(this);
  }

  @AllowConcurrentEvents
  @Subscribe
  public void handle(final CallbackFailure event) {
    if (event.getReference().context().id().equals(context.id())) {
      failingAction = event.failingAction();
      failure = event.failure();
      setSatisfied(false);
    }
  }

  @AllowConcurrentEvents
  @Subscribe
  public void handle(final CallbackFailureCleared event) {
    if (event.getReference().context().id().equals(context.id())) {
      failingAction = null;
      failure = null;
      setSatisfied(true);
    }
  }

  @Override
  public String toString() {
    return "Has no failures: " + context.id();
  }

  @Override
  public String explainSatisfied() {
    return "Capability has not failures";
  }

  @Override
  public String explainUnsatisfied() {
    return failingAction + " failed: " + failure;
  }

}

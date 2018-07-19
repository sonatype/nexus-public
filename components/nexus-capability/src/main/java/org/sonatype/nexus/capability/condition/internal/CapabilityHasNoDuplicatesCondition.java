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
import org.sonatype.nexus.capability.CapabilityEvent;
import org.sonatype.nexus.capability.condition.ConditionSupport;
import org.sonatype.nexus.common.event.EventManager;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

import static com.google.common.base.Preconditions.checkState;

/**
 * A condition that is satisfied as long as the capability has no duplicates.
 *
 * @since 3.13
 */
public class CapabilityHasNoDuplicatesCondition
    extends ConditionSupport
    implements CapabilityContextAware
{
  private CapabilityContext context;

  public CapabilityHasNoDuplicatesCondition(final EventManager eventManager) {
    super(eventManager);
  }

  @Override
  public CapabilityHasNoDuplicatesCondition setContext(final CapabilityContext context) {
    checkState(!isActive(), "Cannot contextualize when already bounded");
    checkState(this.context == null, "Already contextualized with '" + this.context + "'");
    this.context = context;

    return this;
  }

  @Override
  protected void doBind() {
    checkState(context != null, "Not yet contextualized");
    checkForDuplicates();
    getEventManager().register(this);
  }

  @Override
  public void doRelease() {
    getEventManager().unregister(this);
  }

  @AllowConcurrentEvents
  @Subscribe
  public void handle(final CapabilityEvent.Created event) {
    checkForDuplicates(event);
  }

  @AllowConcurrentEvents
  @Subscribe
  public void handle(final CapabilityEvent.AfterUpdate event) {
    checkForDuplicates(event);
  }

  @AllowConcurrentEvents
  @Subscribe
  public void handle(final CapabilityEvent.AfterRemove event) {
    checkForDuplicates(event);
  }

  private void checkForDuplicates(final CapabilityEvent event) {
    if (event.getReference().context().type().equals(context.type())) {
      checkForDuplicates();
    }
  }

  private void checkForDuplicates() {
    setSatisfied(!context.descriptor().isDuplicated(context.id(), context.properties()));
  }

  @Override
  public String toString() {
    return "Has no duplicates: " + context.id();
  }

  @Override
  public String explainSatisfied() {
    return "Capability has no duplicates";
  }

  @Override
  public String explainUnsatisfied() {
    return "Capability is duplicated by another capability";
  }

}

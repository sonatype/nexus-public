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
package org.sonatype.nexus.internal.capability;

import javax.inject.Inject;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.capability.CapabilityContextAware;
import org.sonatype.nexus.capability.CapabilityRegistry;
import org.sonatype.nexus.capability.Condition;
import org.sonatype.nexus.capability.ConditionEvent;
import org.sonatype.nexus.capability.condition.Conditions;
import org.sonatype.nexus.common.event.EventManager;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import com.google.inject.assistedinject.Assisted;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Handles capability automatic removing by reacting to capability validity condition being satisfied/unsatisfied.
 *
 * @since capabilities 2.0
 */
public class ValidityConditionHandler
    extends ComponentSupport
{

  private final EventManager eventManager;

  private final DefaultCapabilityReference reference;

  private final CapabilityRegistry capabilityRegistry;

  private final Conditions conditions;

  private Condition nexusActiveCondition;

  private Condition validityCondition;

  @Inject
  ValidityConditionHandler(final EventManager eventManager,
                           final CapabilityRegistry capabilityRegistry,
                           final Conditions conditions,
                           @Assisted final DefaultCapabilityReference reference)
  {
    this.eventManager = checkNotNull(eventManager);
    this.capabilityRegistry = checkNotNull(capabilityRegistry);
    this.conditions = checkNotNull(conditions);
    this.reference = checkNotNull(reference);
  }

  @AllowConcurrentEvents
  @Subscribe
  public void handle(final ConditionEvent.Satisfied event) {
    if (event.getCondition() == nexusActiveCondition) {
      bindValidity();
    }
  }

  @AllowConcurrentEvents
  @Subscribe
  public void handle(final ConditionEvent.Unsatisfied event) {
    if (event.getCondition() == nexusActiveCondition) {
      releaseValidity();
    }
    else if (event.getCondition() == validityCondition) {
      reference.disable();
      try {
        capabilityRegistry.remove(reference.context().id());
      }
      catch (Exception e) {
        log.error("Failed to remove capability with id '{}'", reference.context().id(), e);
      }
    }
  }

  ValidityConditionHandler bind() {
    if (nexusActiveCondition == null) {
      nexusActiveCondition = conditions.nexus().active();
      nexusActiveCondition.bind();
      eventManager.register(this);
      if (nexusActiveCondition.isSatisfied()) {
        handle(new ConditionEvent.Satisfied(nexusActiveCondition));
      }
    }
    return this;
  }

  ValidityConditionHandler release() {
    if (nexusActiveCondition != null) {
      handle(new ConditionEvent.Unsatisfied(nexusActiveCondition));
      eventManager.unregister(this);
      nexusActiveCondition.release();
    }
    return this;
  }

  private ValidityConditionHandler bindValidity() {
    if (validityCondition == null) {
      try {
        validityCondition = reference.capability().validityCondition();
        if (validityCondition instanceof CapabilityContextAware) {
          ((CapabilityContextAware) validityCondition).setContext(reference.context());
        }
      }
      catch (Exception e) {
        validityCondition = conditions.always(
            "Always satisfied (failed to determine validity condition)"
        );
        log.error(
            "Could not get validation condition from capability {} ({}). Considering it as always valid",
            new Object[]{reference.capability(), reference.context().id(), e}
        );
      }
      if (validityCondition == null) {
        validityCondition = conditions.always("Always satisfied (capability has no validity condition)");
      }
      validityCondition.bind();
    }
    return this;
  }

  private ValidityConditionHandler releaseValidity() {
    if (validityCondition != null) {
      validityCondition.release();
      validityCondition = null;
    }
    return this;
  }

  @Override
  public String toString() {
    String condition = nexusActiveCondition.toString();
    if (validityCondition != null) {
      condition = validityCondition + " WHEN " + condition;
    }
    return String.format(
        "Watching '%s' condition to validate/invalidate capability '%s (id=%s)'",
        condition, reference.capability(), reference.context().id()
    );
  }

}

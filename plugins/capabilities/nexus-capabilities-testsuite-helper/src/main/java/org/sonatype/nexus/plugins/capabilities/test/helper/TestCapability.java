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
package org.sonatype.nexus.plugins.capabilities.test.helper;

import java.util.Date;

import org.sonatype.nexus.plugins.capabilities.Capability;
import org.sonatype.nexus.plugins.capabilities.CapabilityContext;
import org.sonatype.nexus.plugins.capabilities.Condition;
import org.sonatype.sisu.goodies.common.ComponentSupport;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

public abstract class TestCapability
    extends ComponentSupport
    implements Capability
{

  private CapabilityContext context;

  protected CapabilityContext context() {
    checkState(context != null, "Capability was not yet initialized");
    return context;
  }

  @Override
  public void init(final CapabilityContext context) {
    this.context = checkNotNull(context);
  }

  @Override
  public void onCreate() {
    log.info("Create capability with id {} and properties {}", context().id(), context().properties());
  }

  @Override
  public void onUpdate() {
    log.info("Update capability with id {} and properties {}", context().id(), context().properties());
  }

  @Override
  public void onLoad() {
    log.info("Load capability with id {} and properties {}", context().id(), context().properties());
  }

  @Override
  public void onRemove() {
    log.info("Remove capability with id {}", context().id());
  }

  @Override
  public void onActivate()
      throws Exception
  {
    // do nothing
  }

  @Override
  public void onPassivate()
      throws Exception
  {
    // do nothing
  }

  @Override
  public String status() {
    return "<h3>I'm well. Thanx! " + new Date().toString() + "</h3>";
  }

  @Override
  public String description() {
    return null;
  }

  @Override
  public Condition activationCondition() {
    return null;
  }

  @Override
  public Condition validityCondition() {
    return null;
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName();
  }

}

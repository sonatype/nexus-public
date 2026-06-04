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
package org.sonatype.nexus.internal.scheduling;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import org.sonatype.nexus.capability.CapabilitySupport;
import org.sonatype.nexus.common.template.TemplateParameters;
import org.sonatype.nexus.scheduling.spi.SchedulerSPI;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Scheduler capability.
 *
 * @since 3.0
 */
@Component(SchedulerCapabilityDescriptor.TYPE_ID)
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class SchedulerCapability
    extends CapabilitySupport<SchedulerCapabilityConfiguration>
{
  private final SchedulerSPI scheduler;

  private boolean pausedByUs = false;

  @Autowired
  public SchedulerCapability(final SchedulerSPI scheduler) {
    this.scheduler = checkNotNull(scheduler);
  }

  @Override
  protected SchedulerCapabilityConfiguration createConfig(final Map<String, String> properties) throws Exception {
    return new SchedulerCapabilityConfiguration(properties);
  }

  @Override
  protected void onActivate(final SchedulerCapabilityConfiguration config) throws Exception {
    if (pausedByUs) {
      pausedByUs = false;
      scheduler.resume();
    }
  }

  @Override
  protected void onPassivate(final SchedulerCapabilityConfiguration config) throws Exception {
    pausedByUs = true;
    scheduler.pause();
  }

  @Override
  protected String renderDescription() throws Exception {
    return scheduler.renderStatusMessage();
  }

  @Override
  protected String renderStatus() throws Exception {
    return render(SchedulerCapabilityDescriptor.TYPE_ID + "-status.vm", new TemplateParameters()
        .set("detail", scheduler.renderDetailMessage()));
  }
}

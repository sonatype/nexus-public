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
package org.sonatype.nexus.quartz.internal.capability;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.goodies.i18n.I18N;
import org.sonatype.goodies.i18n.MessageBundle;
import org.sonatype.nexus.capability.CapabilitySupport;
import org.sonatype.nexus.common.template.TemplateParameters;
import org.sonatype.nexus.quartz.internal.QuartzSupportImpl;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Quartz Scheduler capability.
 *
 * @since 3.0
 */
@Named(SchedulerCapabilityDescriptor.TYPE_ID)
public class SchedulerCapability
    extends CapabilitySupport<SchedulerCapabilityConfiguration>
{
  private interface Messages
      extends MessageBundle
  {
    @DefaultMessage("Scheduler disabled.")
    String disabledDescription();

    @DefaultMessage("Scheduler stand-by.")
    String deactivatedDescription();

    @DefaultMessage("Scheduler started.")
    String activatedDescription();
  }

  private static final Messages messages = I18N.create(Messages.class);

  private final QuartzSupportImpl quartzImpl;

  @Inject
  public SchedulerCapability(final QuartzSupportImpl quartzImpl) {
    this.quartzImpl = checkNotNull(quartzImpl);
  }

  @Override
  protected SchedulerCapabilityConfiguration createConfig(final Map<String, String> properties) throws Exception {
    return new SchedulerCapabilityConfiguration(properties);
  }

  @Override
  protected void onUpdate(final SchedulerCapabilityConfiguration config) throws Exception {
    quartzImpl.setActive(config.isActive());
    log.debug("Quartz Scheduler updated: {}", config);
  }

  @Override
  protected void onRemove(final SchedulerCapabilityConfiguration config) throws Exception {
    quartzImpl.setActive(true);
    log.debug("Quartz Scheduler capability removed: {}", config);
  }

  @Override
  protected String renderDescription() throws Exception {
    if (!context().isActive()) {
      return messages.disabledDescription();
    }
    else {
      if (!getConfig().isActive()) {
        return messages.deactivatedDescription();
      }
      else {
        return messages.activatedDescription();
      }
    }
  }

  @Override
  protected String renderStatus() throws Exception {
    if (!context().isActive()) {
      return messages.disabledDescription();
    }

    return render(SchedulerCapabilityDescriptor.TYPE_ID + "-status.vm", new TemplateParameters()
            .set("status", renderDescription())
            .set("active", quartzImpl.isActive())
            .set("threadPoolSize", quartzImpl.getThreadPoolSize())
    );
  }
}

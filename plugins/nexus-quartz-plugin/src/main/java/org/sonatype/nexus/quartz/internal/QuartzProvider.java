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
package org.sonatype.nexus.quartz.internal;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.nexus.quartz.QuartzSupport;

import org.quartz.Scheduler;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * {@link Provider} for Quartz's {@link Scheduler}, allowing direct injection of it if needed.
 *
 * @since 3.0
 * TODO: this is wrong, as if scheduler is stopped, getScheduler returns null!
 */
@Singleton
@Named
public class QuartzProvider
    implements Provider<Scheduler>
{
  private final QuartzSupport quartzSupport;

  @Inject
  public QuartzProvider(final QuartzSupport quartzSupport) {
    this.quartzSupport = checkNotNull(quartzSupport);
  }

  @Override
  public Scheduler get() {
    return quartzSupport.getScheduler();
  }
}

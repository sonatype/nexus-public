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

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.quartz.QuartzSupport;

import org.quartz.Scheduler;

/**
 * Component able to customize scheduler during it's creation and destroy. Usable to perform listener additions, that
 * are in contrast to Tasks and Triggers not persisted, and must be re-added. But, this customizer in limited to that
 * kind of configuration only.
 *
 * @since 3.0
 */
public abstract class QuartzCustomizer
    extends ComponentSupport
{
  /**
   * Invoked when scheduler is created and configured, but not yet started.
   */
  public void onCreated(QuartzSupport quartzSupport, Scheduler scheduler) {}

  /**
   * Invoked on running scheduler before being stopped.
   */
  public void onDestroyed(QuartzSupport quartzSupport, Scheduler scheduler) {}

  /**
   * Invoked on running scheduler before being put in stand-by mode.
   */
  public void onStandBy(QuartzSupport quartzSupport, Scheduler scheduler) {}

  /**
   * Invoked on scheduler before being started (might be in stand-by or not yet started).
   */
  public void onReady(QuartzSupport quartzSupport, Scheduler scheduler) {}
}

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
package org.sonatype.nexus.scheduling.internal;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.email.NexusPostOffice;
import org.sonatype.nexus.events.Asynchronous;
import org.sonatype.nexus.events.EventSubscriber;
import org.sonatype.nexus.scheduling.NexusTask;
import org.sonatype.nexus.scheduling.events.NexusTaskEventStoppedFailed;
import org.sonatype.sisu.goodies.common.ComponentSupport;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * {@link EventSubscriber} that will send alert email (if necessary) in case of a failing {@link NexusTask}.
 *
 * @author Alin Dreghiciu
 */
@Singleton
@Named
public class NexusTaskFailureAlertEmailSender
    extends ComponentSupport
    implements EventSubscriber, Asynchronous
{

  private final NexusPostOffice m_postOffice;

  @Inject
  public NexusTaskFailureAlertEmailSender(final NexusPostOffice m_postOffice) {
    this.m_postOffice = checkNotNull(m_postOffice);
  }

  /**
   * Sends alert emails if necessary. {@inheritDoc}
   */
  @Subscribe
  @AllowConcurrentEvents
  public void inspect(final NexusTaskEventStoppedFailed<?> failureEvent) {
    final NexusTask<?> failedTask = failureEvent.getNexusTask();
    if (failedTask == null || !failedTask.shouldSendAlertEmail()) {
      return;
    }
    m_postOffice.sendNexusTaskFailure(failedTask.getAlertEmail(), failedTask.getId(), failedTask.getName(),
        failureEvent.getFailureCause());
  }

}

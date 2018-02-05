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

import java.io.PrintWriter;
import java.io.StringWriter;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.event.EventAware;
import org.sonatype.nexus.common.event.EventAware.Asynchronous;
import org.sonatype.nexus.email.EmailManager;
import org.sonatype.nexus.scheduling.Task;
import org.sonatype.nexus.scheduling.TaskInfo;
import org.sonatype.nexus.scheduling.events.TaskEventStoppedFailed;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.SimpleEmail;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * {@link EventAware} that will send alert email (if necessary) in case of a failing {@link Task}.
 */
@Singleton
@Named
public class NexusTaskFailureAlertEmailSender
    extends ComponentSupport
    implements EventAware, Asynchronous
{
  private final Provider<EmailManager> emailManager;

  @Inject
  public NexusTaskFailureAlertEmailSender(final Provider<EmailManager> emailManager) {
    this.emailManager = checkNotNull(emailManager);
  }

  /**
   * Sends alert emails if necessary.
   */
  @Subscribe
  @AllowConcurrentEvents
  public void on(final TaskEventStoppedFailed event) {
    final TaskInfo taskInfo = event.getTaskInfo();
    if (taskInfo == null || taskInfo.getConfiguration().getAlertEmail() == null) {
      return;
    }

    try {
      sendEmail(
          taskInfo.getConfiguration().getAlertEmail(),
          taskInfo.getId(),
          taskInfo.getName(),
          event.getFailureCause()
      );
    }
    catch (Exception e) {
      log.warn("Failed to send email", e);
    }
  }

  private void sendEmail(final String address, final String taskId, final String taskName, final Throwable cause)
      throws Exception
  {
    Email mail = new SimpleEmail();
    mail.setSubject("Task execution failure");
    mail.addTo(address);

    // FIXME: This should ideally render a user-configurable template
    StringWriter buff = new StringWriter();
    PrintWriter out = new PrintWriter(buff);
    if (taskId != null) {
      out.format("Task ID: %s%n", taskId);
    }
    if (taskName != null) {
      out.format("Task Name: %s%n", taskName);
    }
    if (cause != null) {
      out.println("Stack-trace:");
      cause.printStackTrace(out);
    }
    mail.setMsg(buff.toString());

    emailManager.get().send(mail);
  }
}

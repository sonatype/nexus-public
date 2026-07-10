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

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.inject.Provider;
import org.sonatype.nexus.common.QualifierUtil;
import org.sonatype.nexus.common.event.EventAware;
import org.sonatype.nexus.common.event.EventAware.Asynchronous;
import org.sonatype.nexus.email.EmailManager;
import org.sonatype.nexus.scheduling.Task;
import org.sonatype.nexus.scheduling.TaskInfo;
import org.sonatype.nexus.scheduling.TaskNotificationCondition;
import org.sonatype.nexus.scheduling.TaskNotificationMessageGenerator;
import org.sonatype.nexus.scheduling.events.TaskEventStoppedDone;
import org.sonatype.nexus.scheduling.events.TaskEventStoppedFailed;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import org.apache.commons.mail2.jakarta.Email;
import org.apache.commons.mail2.jakarta.SimpleEmail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import org.springframework.stereotype.Component;

/**
 * {@link EventAware} that will send notification email (if necessary) in case of a completed or failed {@link Task}.
 */
@Component
public class NexusTaskNotificationEmailSender
    implements EventAware, Asynchronous
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  private final Provider<EmailManager> emailManager;

  private final Map<String, TaskNotificationMessageGenerator> taskNotificationMessageGenerators;

  @Autowired
  public NexusTaskNotificationEmailSender(
      final Provider<EmailManager> emailManager,
      final List<TaskNotificationMessageGenerator> taskNotificationMessageGeneratorsList)
  {
    this.emailManager = checkNotNull(emailManager);
    this.taskNotificationMessageGenerators =
        QualifierUtil.buildQualifierBeanMap(checkNotNull(taskNotificationMessageGeneratorsList));
  }

  /**
   * Sends alert emails if necessary on failure.
   */
  @Subscribe
  @AllowConcurrentEvents
  public void on(final TaskEventStoppedFailed event) {
    final TaskInfo taskInfo = event.getTaskInfo();
    if (!haveAlertEmail(taskInfo)) {
      return;
    }
    String body = taskNotificationMessageGenerator(taskInfo.getTypeId()).failed(taskInfo, event.getFailureCause());
    log.trace("sending message {}", body);
    sendEmail("Task execution failure", taskInfo.getConfiguration().getAlertEmail(), body);
  }

  /**
   * Sends alert emails on completion.
   */
  @Subscribe
  @AllowConcurrentEvents
  public void on(final TaskEventStoppedDone event) {
    final TaskInfo taskInfo = event.getTaskInfo();
    if (!haveAlertEmail(taskInfo) ||
        taskInfo.getConfiguration().getNotificationCondition() == TaskNotificationCondition.FAILURE) {
      return;
    }
    String body = taskNotificationMessageGenerator(taskInfo.getTypeId()).completed(taskInfo);
    log.trace("sending message {}", body);
    sendEmail("Task execution completed", taskInfo.getConfiguration().getAlertEmail(), body);
  }

  private boolean haveAlertEmail(final TaskInfo taskInfo) {
    return taskInfo != null && taskInfo.getConfiguration().getAlertEmail() != null;
  }

  private void sendEmail(final String subject, final String address, final String body) {
    try {
      Email mail = new SimpleEmail();
      mail.setSubject(subject);
      mail.addTo(address);
      mail.setMsg(emailManager.get().constructMessage(body));
      emailManager.get().send(mail);
    }
    catch (Exception e) {
      log.warn("Failed to send email", e);
    }
  }

  private TaskNotificationMessageGenerator taskNotificationMessageGenerator(final String typeId) {
    TaskNotificationMessageGenerator result = taskNotificationMessageGenerators.get(typeId);
    if (result == null) {
      result = taskNotificationMessageGenerators.get(DefaultTaskNotificationMessageGenerator.ID);
    }
    return result;
  }
}

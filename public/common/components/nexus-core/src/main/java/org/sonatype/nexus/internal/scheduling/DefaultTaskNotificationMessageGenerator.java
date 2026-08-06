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

import java.net.URL;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Autowired;
import org.sonatype.nexus.common.template.TemplateHelper;
import org.sonatype.nexus.common.template.TemplateParameters;
import org.sonatype.nexus.scheduling.TaskInfo;
import org.sonatype.nexus.scheduling.TaskNotificationMessageGenerator;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Generates notification messages for tasks with no class specific generator.
 *
 * @since 3.22
 */
@Component
@Qualifier(DefaultTaskNotificationMessageGenerator.ID)
@Primary
public class DefaultTaskNotificationMessageGenerator
    implements TaskNotificationMessageGenerator
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  public static final String ID = "DEFAULT";

  private final TemplateHelper templateHelper;

  @Autowired
  public DefaultTaskNotificationMessageGenerator(final TemplateHelper templateHelper) {
    this.templateHelper = checkNotNull(templateHelper);
  }

  public String completed(final TaskInfo taskInfo) {
    URL template = DefaultTaskNotificationMessageGenerator.class.getResource("task-completed.vm");
    TemplateParameters params = new TemplateParameters();

    // Use Duration to handle durations > 24 hours correctly
    Duration duration = Duration.ofMillis(taskInfo.getLastRunState().getRunDuration());
    String formattedDuration = String.format("%02d:%02d:%02d",
        duration.toHours(),
        duration.toMinutesPart(),
        duration.toSecondsPart());

    params.set("formattedDuration", formattedDuration);
    params.set("taskInfo", taskInfo);
    return templateHelper.render(template, params);
  }

  public String failed(final TaskInfo taskInfo, final Throwable cause) {
    URL template = DefaultTaskNotificationMessageGenerator.class.getResource("task-failed.vm");
    TemplateParameters params = new TemplateParameters();
    params.set("taskInfo", taskInfo);
    if (cause != null) {
      params.set("stackTrace", ExceptionUtils.getStackTrace(cause));
    }
    return templateHelper.render(template, params);
  }
}

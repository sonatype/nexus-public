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
package org.sonatype.nexus.repository.content.tasks;

import org.springframework.beans.factory.annotation.Autowired;

import org.sonatype.nexus.logging.task.TaskLogType;
import org.sonatype.nexus.logging.task.TaskLogging;
import org.sonatype.nexus.scheduling.Cancelable;
import org.sonatype.nexus.scheduling.TaskInterruptedException;
import org.sonatype.nexus.scheduling.TaskSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.app.FeatureFlags.DISABLE_CREATING_COMPONENT_INDEXES_TASK;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * System task to populate the {format}_component tables
 */
@Component
@TaskLogging(TaskLogType.TASK_LOG_ONLY_WITH_PROGRESS)
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class CreateComponentIndexTask
    extends TaskSupport
    implements Cancelable
{
  private static final Logger LOG = LoggerFactory.getLogger(CreateComponentIndexTask.class);

  private final CreateComponentIndexService createComponentIndexService;

  private final boolean disableTask;

  @Autowired
  public CreateComponentIndexTask(
      final CreateComponentIndexService createComponentIndexService,
      @Value("${" + DISABLE_CREATING_COMPONENT_INDEXES_TASK + ":false}") final boolean disableTask)
  {
    this.createComponentIndexService = checkNotNull(createComponentIndexService);
    this.disableTask = disableTask;
  }

  @Override
  public String getMessage() {
    return "Create new indexes for querying the various format component tables by full coordinates";
  }

  @Override
  protected Object execute() throws Exception {
    if (disableTask) {
      throw new TaskInterruptedException("The component table index task was disabled", disableTask);
    }

    createComponentIndexService.recreateComponentIndexes();

    return null;
  }
}

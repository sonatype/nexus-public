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
package org.sonatype.nexus.api.extdirect.selfhosted.tasks;

import org.sonatype.nexus.api.extdirect.common.tasks.TaskComponentSupport;
import org.sonatype.nexus.scheduling.TaskScheduler;
import org.sonatype.nexus.scheduling.TaskUtils;

import com.softwarementors.extjs.djn.config.annotations.DirectAction;
import jakarta.inject.Provider;
import jakarta.validation.Validator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@DirectAction(action = "coreui_Task")
public class TaskComponent
    extends TaskComponentSupport
{
  @Autowired
  public TaskComponent(
      final TaskScheduler taskScheduler,
      final Provider<Validator> validatorProvider,
      @Value("${nexus.scripts.allowCreation:false}") final boolean allowScriptCreation,
      final TaskUtils taskUtils)
  {
    super(taskScheduler, validatorProvider, allowScriptCreation, taskUtils);
  }
}

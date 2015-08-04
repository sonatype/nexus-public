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
package org.sonatype.scheduling;

import java.util.HashMap;
import java.util.Map;

public class SimpleTaskConfigManager
    implements TaskConfigManager
{
  private Map<String, ScheduledTask<?>> tasks;

  public SimpleTaskConfigManager() {
    super();

    tasks = new HashMap<String, ScheduledTask<?>>();
  }

  public void initializeTasks(Scheduler scheduler) {
    // nothing here, it is not persistent
  }

  public <T> void addTask(ScheduledTask<T> task) {
    tasks.put(task.getId(), task);
  }

  public <T> void removeTask(ScheduledTask<T> task) {
    tasks.remove(task.getId());
  }

  public SchedulerTask<?> createTaskInstance(String taskType)
      throws IllegalArgumentException
  {
    return null;
  }

  public <T> T createTaskInstance(Class<T> taskType)
      throws IllegalArgumentException
  {
    return null;
  }
}

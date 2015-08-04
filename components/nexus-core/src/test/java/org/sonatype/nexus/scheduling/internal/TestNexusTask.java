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

import java.util.List;
import java.util.Map;

import org.sonatype.nexus.scheduling.NexusTask;
import org.sonatype.scheduling.AbstractSchedulerTask;
import org.sonatype.scheduling.ScheduledTask;

public class TestNexusTask
    extends AbstractSchedulerTask<Object>
    implements NexusTask<Object>
{

  @Override
  public Object call()
      throws Exception
  {
    return null;
  }

  @Override
  public String getId() {
    return TestNexusTask.class.getName();
  }

  @Override
  public String getName() {
    return TestNexusTask.class.getName();
  }

  @Override
  public boolean isExposed() {
    return true;
  }

  @Override
  public boolean shouldSendAlertEmail() {
    return false;
  }

  @Override
  public String getAlertEmail() {
    return null;
  }

  @Override
  public boolean allowConcurrentSubmission(final Map<String, List<ScheduledTask<?>>> currentActiveTasks) {
    return false;
  }

  @Override
  public boolean allowConcurrentExecution(final Map<String, List<ScheduledTask<?>>> currentActiveTasks) {
    return false;
  }

}

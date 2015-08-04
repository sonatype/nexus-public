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
package org.sonatype.nexus.scheduling;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sonatype.scheduling.AbstractSchedulerTask;
import org.sonatype.scheduling.ScheduledTask;

public class DummyWaitingNexusTask
    extends AbstractSchedulerTask<Object>
    implements NexusTask<Object>
{
  private boolean allowConcurrentSubmission = false;

  private boolean allowConcurrentExecution = false;

  private long sleepTime = 10000;

  private Map<String, String> parameters;

  private Object result;

  public boolean isExposed() {
    return true;
  }

  public void addParameter(String key, String value) {
    getParameters().put(key, value);
  }

  public String getParameter(String key) {
    return getParameters().get(key);
  }

  public Map<String, String> getParameters() {
    if (parameters == null) {
      parameters = new HashMap<String, String>();
    }

    return parameters;
  }

  public boolean allowConcurrentSubmission(Map<String, List<ScheduledTask<?>>> activeTasks) {
    return allowConcurrentSubmission;
  }

  public boolean allowConcurrentExecution(Map<String, List<ScheduledTask<?>>> activeTasks) {
    return allowConcurrentExecution;
  }

  public void setAllowConcurrentSubmission(boolean allowConcurrentSubmission) {
    this.allowConcurrentSubmission = allowConcurrentSubmission;
  }

  public void setAllowConcurrentExecution(boolean allowConcurrentExecution) {
    this.allowConcurrentExecution = allowConcurrentExecution;
  }

  public long getSleepTime() {
    return sleepTime;
  }

  public void setSleepTime(long sleepTime) {
    this.sleepTime = sleepTime;
  }

  public void setResult(Object resul) {
    this.result = resul;
  }

  public Object call()
      throws Exception
  {
    System.out.println("BEFORE SLEEP");
    Thread.sleep(getSleepTime());
    System.out.println("AFTER SLEEP");

    return result;
  }

  public String getId() {
    return "dummyId";
  }

  public String getName() {
    return "dummyName";
  }

  public boolean shouldSendAlertEmail() {
    return false;
  }

  public String getAlertEmail() {
    return null;
  }
}

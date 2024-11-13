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
package org.sonatype.nexus.scheduling.api;

import java.util.Date;

import org.sonatype.nexus.scheduling.ExternalTaskState;
import org.sonatype.nexus.scheduling.TaskInfo;

import org.apache.commons.lang3.StringUtils;

/**
 * Task transfer object for REST APIs.
 *
 * @since 3.6
 */
public class TaskXO
{
  private String id;

  private String name;

  private String type;

  private String message;

  private String currentState;

  private String lastRunResult;

  private Date nextRun;

  private Date lastRun;

  public static TaskXO fromTaskInfo(final TaskInfo taskInfo, ExternalTaskState externalTaskState) {
    TaskXO taskXO = new TaskXO();

    taskXO.setId(taskInfo.getId());
    taskXO.setName(taskInfo.getName());
    taskXO.setType(taskInfo.getTypeId());
    taskXO.setMessage(taskInfo.getMessage());
    if (externalTaskState.getState().isRunning() && StringUtils.isNotBlank(externalTaskState.getProgress())) {
      taskXO.setCurrentState(externalTaskState.getState().toString() + ": " + externalTaskState.getProgress());
    }
    else {
      taskXO.setCurrentState(externalTaskState.getState().toString());
    }

    taskXO.setNextRun(taskInfo.getCurrentState().getNextRun());
    if(externalTaskState.getLastEndState() != null) {
      taskXO.setLastRunResult(externalTaskState.getLastEndState().toString());
    }
    taskXO.setLastRun(externalTaskState.getLastRunStarted());
    return taskXO;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public String getCurrentState() {
    return currentState;
  }

  public void setCurrentState(String currentState) {
    this.currentState = currentState;
  }

  public String getLastRunResult() {
    return lastRunResult;
  }

  public void setLastRunResult(String lastRunResult) {
    this.lastRunResult = lastRunResult;
  }

  public Date getNextRun() {
    return nextRun;
  }

  public void setNextRun(Date nextRun) {
    this.nextRun = nextRun;
  }

  public Date getLastRun() {
    return lastRun;
  }

  public void setLastRun(Date lastRun) {
    this.lastRun = lastRun;
  }

  @Override
  public String toString() {
    return "TaskXO(" +
        "id:'" + id + '\'' +
        ", name:'" + name + '\'' +
        ", type:'" + type + '\'' +
        ", message:'" + message + '\'' +
        ", currentState:'" + currentState + '\'' +
        ", lastRunResult:'" + lastRunResult + '\'' +
        ", nextRun:" + nextRun +
        ", lastRun:" + lastRun +
        ')';
  }
}

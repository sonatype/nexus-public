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
package org.sonatype.nexus.scheduling.internal.upgrade.datastore;

import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class UpgradeTaskData
{
  private Integer id;

  private String taskId;

  private String status;

  private Map<String, String> configuration;

  public UpgradeTaskData() {
    // deserialization
  }

  public UpgradeTaskData(final String taskId, final Map<String, String> configuration) {
    this.taskId = checkNotNull(taskId);
    this.configuration = checkNotNull(configuration);
  }

  /**
   * The configuration of the task to be run
   */
  public Map<String, String> getConfiguration() {
    return configuration;
  }

  public Integer getId() {
    return id;
  }

  /**
   * A string indicating the last task status
   */
  public String getStatus() {
    return status;
  }

  /**
   * Matches the ID from TaskInfo
   */
  public String getTaskId() {
    return taskId;
  }

  public void setConfiguration(final Map<String, String> configuration) {
    this.configuration = configuration;
  }

  public void setId(final Integer id) {
    this.id = id;
  }

  public void setStatus(final String status) {
    this.status = status;
  }

  public void setTaskId(final String taskId) {
    this.taskId = taskId;
  }
}

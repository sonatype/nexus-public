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
package org.sonatype.nexus.common.log;

import java.io.Serializable;

/**
 * Request to generate a support ZIP file.
 */
public class SupportZipGeneratorRequest
    implements Serializable
{
  private static final long serialVersionUID = 6382836653403012753L;

  /**
   * Include system information report.
   */
  private boolean systemInformation;

  /**
   * Include thread dump.
   */
  private boolean threadDump;

  /**
   * Include metrics.
   */
  private boolean metrics;

  /**
   * Include configuration files.
   */
  private boolean configuration;

  /**
   * Include security files.
   */
  private boolean security;

  /**
   * Include log files.
   */
  private boolean log;

  /**
   * Include task log files.
   */
  private boolean taskLog;

  /**
   * Include audit log files.
   *
   * @since 3.16
   */
  private boolean auditLog;

  /**
   * Include JMX information.
   */
  private boolean jmx;

  /**
   * Include Replication information.
   */
  private boolean replication;

  /**
   * Limit the size of files included in the ZIP.
   */
  private boolean limitFileSizes;

  /**
   * Limit the total size of the generated ZIP file.
   */
  private boolean limitZipSize;

  /**
   * Include archived logs up to 3, 5, 7 days
   */
  private int archivedLog;

  private String hostname;

  public boolean isSystemInformation() {
    return systemInformation;
  }

  public void setSystemInformation(final boolean systemInformation) {
    this.systemInformation = systemInformation;
  }

  public boolean isThreadDump() {
    return threadDump;
  }

  public void setThreadDump(final boolean threadDump) {
    this.threadDump = threadDump;
  }

  public boolean isMetrics() {
    return metrics;
  }

  public void setMetrics(final boolean metrics) {
    this.metrics = metrics;
  }

  public boolean isConfiguration() {
    return configuration;
  }

  public void setConfiguration(final boolean configuration) {
    this.configuration = configuration;
  }

  public boolean isSecurity() {
    return security;
  }

  public void setSecurity(final boolean security) {
    this.security = security;
  }

  public boolean isLog() {
    return log;
  }

  public void setLog(final boolean log) {
    this.log = log;
  }

  public boolean isTaskLog() {
    return taskLog;
  }

  public void setTaskLog(final boolean taskLog) {
    this.taskLog = taskLog;
  }

  public boolean isAuditLog() {
    return auditLog;
  }

  public void setAuditLog(final boolean auditLog) {
    this.auditLog = auditLog;
  }

  public boolean isJmx() {
    return jmx;
  }

  public void setJmx(final boolean jmx) {
    this.jmx = jmx;
  }

  public boolean isReplication() {
    return replication;
  }

  public void setReplication(final boolean replication) {
    this.replication = replication;
  }

  public boolean isLimitFileSizes() {
    return limitFileSizes;
  }

  public void setLimitFileSizes(final boolean limitFileSizes) {
    this.limitFileSizes = limitFileSizes;
  }

  public boolean isLimitZipSize() {
    return limitZipSize;
  }

  public void setLimitZipSize(final boolean limitZipSize) {
    this.limitZipSize = limitZipSize;
  }

  public String getHostname() {
    return this.hostname;
  }

  public void setHostname(final String hostname) {
    this.hostname = hostname;
  }

  public void setArchivedLog(final int archivedLog) {
    this.archivedLog = archivedLog;
  }

  public int getArchivedLog() {
    return this.archivedLog;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "systemInformation=" + systemInformation +
        ", threadDump=" + threadDump +
        ", metrics=" + metrics +
        ", configuration=" + configuration +
        ", security=" + security +
        ", log=" + log +
        ", tasklog=" + taskLog +
        ", auditlog=" + auditLog +
        ", jmx=" + jmx +
        ", replication=" + replication +
        ", limitFileSizes=" + limitFileSizes +
        ", limitZipSize=" + limitZipSize +
        ", hostname=" + hostname +
        ", archivedLog=" + archivedLog +
        '}';
  }
}

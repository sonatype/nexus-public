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
package org.sonatype.nexus.supportzip;

import java.io.OutputStream;

/**
 * Generates a support ZIP file.
 *
 * @since 2.7
 */
public interface SupportZipGenerator
{
  /**
   * Request to generate a support ZIP file.
   */
  class Request
  {
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
     * @since 3.next
     */
    private boolean auditLog;

    /**
     * Include JMX information.
     */
    private boolean jmx;

    /**
     * Limit the size of files included in the ZIP.
     */
    private boolean limitFileSizes;

    /**
     * Limit the total size of the generated ZIP file.
     */
    private boolean limitZipSize;

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
          ", limitFileSizes=" + limitFileSizes +
          ", limitZipSize=" + limitZipSize +
          '}';
    }
  }

  /**
   * Result of support ZIP generate request.
   */
  class Result
  {
    /**
     * True if the ZIP or any of its contents had been truncated.
     */
    private boolean truncated;

    /**
     * The name of the generated ZIP file.
     */
    private String filename;

    /**
     * The local path of the generated ZIP file.
     */
    private String localPath;

    /**
     * The size of the generated ZIP file.
     */
    private long size;

    public boolean isTruncated() {
      return truncated;
    }

    public void setTruncated(final boolean truncated) {
      this.truncated = truncated;
    }

    public String getFilename() {
      return filename;
    }

    public void setFilename(final String filename) {
      this.filename = filename;
    }

    public String getLocalPath() {
      return localPath;
    }

    public void setLocalPath(final String localPath) {
      this.localPath = localPath;
    }

    public long getSize() {
      return size;
    }

    public void setSize(final long size) {
      this.size = size;
    }

    @Override
    public String toString() {
      return getClass().getSimpleName() + "{" +
          "truncated=" + truncated +
          ", filename=" + filename +
          ", localPath=" + localPath +
          ", size=" + size +
          '}';
    }
  }

  /**
   * Generate a support ZIP for the given request.
   */
  Result generate(Request request);

  /**
   * Generate a support ZIP.
   * @param outputStream the output stream to write the ZIP to.
   * @param prefix directory prefix applied to files in the ZIP.
   * @return true if the zip was truncated.
   * @since 3.5
   */
  boolean generate(Request request, String prefix, OutputStream outputStream);
}

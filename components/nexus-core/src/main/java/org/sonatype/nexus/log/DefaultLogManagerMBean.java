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
package org.sonatype.nexus.log;

import java.io.IOException;

import javax.management.StandardMBean;

import com.google.common.base.Preconditions;

/**
 * Default implementation of LogManager MBean interface.
 *
 * @author cstamas
 * @since 2.1
 */
public class DefaultLogManagerMBean
    extends StandardMBean
    implements LogManagerMBean
{
  private final LogManager logManager;

  public DefaultLogManagerMBean(final LogManager logManager) {
    super(LogManagerMBean.class, false);
    this.logManager = Preconditions.checkNotNull(logManager, "Managed LogManager instance cannot be null!");
  }

  @Override
  public String getRootLoggerLevel()
      throws IOException
  {
    final LogConfiguration logConfiguration = logManager.getConfiguration();
    return logConfiguration.getRootLoggerLevel();
  }

  /**
   * @since 2.7
   */
  @Override
  public void makeRootLoggerLevelOff()
      throws IOException
  {
    setRootLoggerLevel(LoggerLevel.OFF);
  }

  @Override
  public void makeRootLoggerLevelTrace()
      throws IOException
  {
    setRootLoggerLevel(LoggerLevel.TRACE);
  }

  @Override
  public void makeRootLoggerLevelDebug()
      throws IOException
  {
    setRootLoggerLevel(LoggerLevel.DEBUG);
  }

  @Override
  public void makeRootLoggerLevelInfo()
      throws IOException
  {
    setRootLoggerLevel(LoggerLevel.INFO);
  }

  @Override
  public void makeRootLoggerLevelWarn()
      throws IOException
  {
    setRootLoggerLevel(LoggerLevel.WARN);
  }

  /**
   * @since 2.7
   */
  @Override
  public void makeRootLoggerLevelError()
      throws IOException
  {
    setRootLoggerLevel(LoggerLevel.ERROR);
  }

  @Override
  public void makeRootLoggerLevelDefault()
      throws IOException
  {
    makeRootLoggerLevelInfo();
  }

  protected void setRootLoggerLevel(final LoggerLevel value)
      throws IOException
  {
    final LogConfiguration oldConfiguration = logManager.getConfiguration();
    final DefaultLogConfiguration newConfiguration = new DefaultLogConfiguration();

    newConfiguration.setFileAppenderLocation(oldConfiguration.getFileAppenderLocation());
    newConfiguration.setFileAppenderPattern(oldConfiguration.getFileAppenderPattern());
    newConfiguration.setRootLoggerAppenders(oldConfiguration.getRootLoggerAppenders());
    newConfiguration.setRootLoggerLevel(value.name());
    logManager.setConfiguration(newConfiguration);
  }
}

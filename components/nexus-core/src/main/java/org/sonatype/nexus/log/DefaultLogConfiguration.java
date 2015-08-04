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

/**
 * @author adreghiciu@gmail.com
 */
public class DefaultLogConfiguration
    implements LogConfiguration
{

  private String rootLoggerLevel;

  private String rootLoggerAppenders;

  private String fileAppenderPattern;

  private String fileAppenderLocation;

  public DefaultLogConfiguration() {
    this(null);
  }

  public DefaultLogConfiguration(LogConfiguration config) {
    if (config != null) {
      rootLoggerLevel = config.getRootLoggerLevel();
      rootLoggerAppenders = config.getRootLoggerAppenders();
      fileAppenderPattern = config.getFileAppenderPattern();
      fileAppenderLocation = config.getFileAppenderLocation();
    }
  }

  public String getRootLoggerLevel() {
    return rootLoggerLevel;
  }

  public void setRootLoggerLevel(String rootLoggerLevel) {
    this.rootLoggerLevel = rootLoggerLevel;
  }

  public String getRootLoggerAppenders() {
    return rootLoggerAppenders;
  }

  public void setRootLoggerAppenders(String rootLoggerAppenders) {
    this.rootLoggerAppenders = rootLoggerAppenders;
  }

  public String getFileAppenderPattern() {
    return fileAppenderPattern;
  }

  public void setFileAppenderPattern(String fileAppenderPattern) {
    this.fileAppenderPattern = fileAppenderPattern;
  }

  public String getFileAppenderLocation() {
    return fileAppenderLocation;
  }

  public void setFileAppenderLocation(String fileAppenderLocation) {
    this.fileAppenderLocation = fileAppenderLocation;
  }

}

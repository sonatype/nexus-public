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
package org.sonatype.nexus.client.core;

import java.util.Date;

/**
 * Status of the remote Nexus instance.
 *
 * @since 2.1
 */
public class NexusStatus
{

  /**
   * The application name.
   */
  private final String appName;

  /**
   * The formatted application name (html formatting!).
   */
  private final String formattedAppName;

  /**
   * The version of the nexus instance.
   */
  private final String version;

  /**
   * The version of the core nexus instance.
   */
  private final String apiVersion;

  /**
   * The long version of the nexus edition (i.e. Open Source or Professional).
   */
  private final String editionLong;

  /**
   * The short version of the nexus edition (i.e. OSS or PRO).
   */
  private final String editionShort;

  /**
   * The state of the nexus instance.
   */
  private final String state;

  /**
   * The date the instance was first initialized.
   */
  private final Date initializedAt;

  /**
   * The date the instance was last started.
   */
  private final Date startedAt;

  /**
   * The last time the nexus configuration was updated.
   */
  private final Date lastConfigChange;

  /**
   * The generation of the nexus configuration.
   */
  private final long lastConfigGeneration;

  /**
   * Flag that states if this is the first time nexus was started.
   */
  private final boolean firstStart;

  /**
   * Flag that states if the nexus instance has been upgraded.
   */
  private final boolean instanceUpgraded;

  /**
   * Flag that states if the nexus configuration has been upgraded
   */
  private final boolean configurationUpgraded;

  /**
   * The base url of the nexus instance.
   */
  private final String baseUrl;

  public NexusStatus(final String appName, final String formattedAppName, final String version,
                     final String apiVersion, final String editionLong, final String editionShort,
                     final String state, final Date initializedAt, final Date startedAt,
                     final Date lastConfigChange, final long lastConfigGeneration, final boolean firstStart,
                     final boolean instanceUpgraded, final boolean configurationUpgraded, final String baseUrl)
  {
    this.appName = appName;
    this.formattedAppName = formattedAppName;
    this.version = version;
    this.apiVersion = apiVersion;
    this.editionLong = editionLong;
    this.editionShort = editionShort;
    this.state = state;
    this.initializedAt = initializedAt;
    this.startedAt = startedAt;
    this.lastConfigChange = lastConfigChange;
    this.lastConfigGeneration = lastConfigGeneration;
    this.firstStart = firstStart;
    this.instanceUpgraded = instanceUpgraded;
    this.configurationUpgraded = configurationUpgraded;
    this.baseUrl = baseUrl;
  }

  public String getAppName() {
    return appName;
  }

  public String getFormattedAppName() {
    return formattedAppName;
  }

  public String getVersion() {
    return version;
  }

  public String getApiVersion() {
    return apiVersion;
  }

  public String getEditionLong() {
    return editionLong;
  }

  public String getEditionShort() {
    return editionShort;
  }

  public String getState() {
    return state;
  }

  public Date getInitializedAt() {
    return initializedAt;
  }

  public Date getStartedAt() {
    return startedAt;
  }

  public Date getLastConfigChange() {
    return lastConfigChange;
  }

  public long getLastConfigGeneration() {
    return lastConfigGeneration;
  }

  public boolean isFirstStart() {
    return firstStart;
  }

  public boolean isInstanceUpgraded() {
    return instanceUpgraded;
  }

  public boolean isConfigurationUpgraded() {
    return configurationUpgraded;
  }

  public String getBaseUrl() {
    return baseUrl;
  }

  @Override
  public String toString() {
    return "NexusStatus [appName=" + appName + ", version=" + version + ", editionShort=" + editionShort + "]";
  }
}

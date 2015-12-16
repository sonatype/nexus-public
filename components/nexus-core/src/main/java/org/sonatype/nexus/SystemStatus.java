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
package org.sonatype.nexus;

import java.util.Date;

/**
 * Nexus system state object. It gives small amount of important infos about Nexus Application.
 *
 * @author cstamas
 * @author damian
 */
public class SystemStatus
{
  /**
   * The Application Name
   */
  private String appName = "Nexus Repository Manager";

  /**
   * The Formatted Application Name, used whenever possible
   */
  private String formattedAppName = "Nexus Repository Manager";

  /**
   * The Nexus Application version.
   */
  private String version = "unknown";

  /**
   * The Nexus Java API version (not the REST API!).
   */
  private String apiVersion = "unknown";

  /**
   * The Nexus Application edition for display in UI.
   */
  private String editionLong = "";

  /**
   * The Nexus Application edition for user agent
   */
  private String editionShort = "OSS";

  /**
   * The Nexus attributions url
   */
  private String attributionsURL = "http://links.sonatype.com/products/nexus/oss/attributions";

  /**
   * The Nexus attributions url
   */
  private String purchaseURL = "http://links.sonatype.com/products/nexus/oss/store";

  /**
   * The Nexus attributions url
   */
  private String userLicenseURL = "http://links.sonatype.com/products/nexus/oss/EULA";

  /**
   * The Nexus Application state.
   */
  private SystemState state;

  /**
   * The time this instance of Nexus was started.
   */
  private Date initializedAt;

  /**
   * The time this instance of Nexus was started.
   */
  private Date startedAt;

  /**
   * The timestamp of last config change.
   */
  private Date lastConfigChange;

  /**
   * Is this 1st start of Nexus?
   */
  private boolean firstStart;

  /**
   * Was it an instance upgrade?
   */
  private boolean instanceUpgraded;

  /**
   * If instanceUpgraded, was there also a configuration upgrade?
   */
  private boolean configurationUpgraded;

  /**
   * Other error cause that blocked startup.
   */
  private Throwable errorCause;

  /**
   * True if a license is installed, false otherwise. For OSS always return false.
   */
  private boolean licenseInstalled = false;

  /**
   * True if license is expired, false otherwise. For OSS always return false.
   */
  private boolean licenseExpired = false;

  /**
   * True if installed license is a trial license, false otherwise. For OSS always return false.
   */
  private boolean trialLicense = false;

  public String getAppName() {
    return appName;
  }

  public void setAppName(String appName) {
    this.appName = appName;
  }

  public String getFormattedAppName() {
    return formattedAppName;
  }

  public void setFormattedAppName(String formattedAppName) {
    this.formattedAppName = formattedAppName;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public String getApiVersion() {
    return apiVersion;
  }

  public void setApiVersion(String version) {
    this.apiVersion = version;
  }

  public String getEditionLong() {
    return editionLong;
  }

  public void setEditionLong(String editionUI) {
    this.editionLong = editionUI;
  }

  public String getEditionShort() {
    return editionShort;
  }

  public void setEditionShort(String editionUserAgent) {
    this.editionShort = editionUserAgent;
  }

  public String getAttributionsURL() {
    return attributionsURL;
  }

  public void setAttributionsURL(String attributionsURL) {
    this.attributionsURL = attributionsURL;
  }

  public String getPurchaseURL() {
    return purchaseURL;
  }

  public void setPurchaseURL(String purchaseURL) {
    this.purchaseURL = purchaseURL;
  }

  public String getUserLicenseURL() {
    return userLicenseURL;
  }

  public void setUserLicenseURL(String userLicenseURL) {
    this.userLicenseURL = userLicenseURL;
  }

  public SystemState getState() {
    return state;
  }

  public void setState(SystemState status) {
    this.state = status;
  }

  public Date getInitializedAt() {
    return initializedAt;
  }

  public void setInitializedAt(Date initializedAt) {
    this.initializedAt = initializedAt;
  }

  public Date getStartedAt() {
    return startedAt;
  }

  public void setStartedAt(Date startedAt) {
    this.startedAt = startedAt;
  }

  public Date getLastConfigChange() {
    return lastConfigChange;
  }

  public void setLastConfigChange(Date lastConfigChange) {
    this.lastConfigChange = lastConfigChange;
  }

  public Throwable getErrorCause() {
    return errorCause;
  }

  public void setErrorCause(Throwable errorCause) {
    this.errorCause = errorCause;
  }

  public boolean isFirstStart() {
    return firstStart;
  }

  public void setFirstStart(boolean firstStart) {
    this.firstStart = firstStart;
  }

  public boolean isInstanceUpgraded() {
    return instanceUpgraded;
  }

  public void setInstanceUpgraded(boolean instanceUpgraded) {
    this.instanceUpgraded = instanceUpgraded;
  }

  public boolean isConfigurationUpgraded() {
    return configurationUpgraded;
  }

  public void setConfigurationUpgraded(boolean configurationUpgraded) {
    this.configurationUpgraded = configurationUpgraded;
  }

  public boolean isNexusStarted() {
    return SystemState.STARTED.equals(getState());
  }

  public boolean isLicenseInstalled() {
    return licenseInstalled;
  }

  public void setLicenseInstalled(final boolean licenseInstalled) {
    this.licenseInstalled = licenseInstalled;
  }

  public boolean isLicenseExpired() {
    return licenseExpired;
  }

  public void setLicenseExpired(final boolean licenseExpired) {
    this.licenseExpired = licenseExpired;
  }

  public boolean isTrialLicense() {
    return trialLicense;
  }

  public void setTrialLicense(final boolean trialLicense) {
    this.trialLicense = trialLicense;
  }

}

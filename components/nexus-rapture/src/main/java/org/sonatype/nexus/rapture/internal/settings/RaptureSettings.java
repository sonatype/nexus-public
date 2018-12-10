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
package org.sonatype.nexus.rapture.internal.settings;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotBlank;

/**
 * Rapture settings.
 *
 * @since 3.0
 */
public class RaptureSettings
{
  public static final String DEFAULT_TITLE = "Nexus Repository Manager";

  public static final boolean DEFAULT_DEBUG_ALLOWED = true;

  // FIXME: Use Goodies Time

  public static final int DEFAULT_STATUS_INTERVAL_AUTHENTICATED = 5; // seconds

  public static final int DEFAULT_STATUS_INTERVAL_ANONYMOUS = 60; // seconds

  public static final int DEFAULT_SESSION_TIMEOUT = 30; // minutes

  public static final int MIN_REQUEST_TIMEOUT = 30; // seconds

  public static final int DEFAULT_REQUEST_TIMEOUT = 60; // seconds

  public static final int DEFAULT_LONG_REQUEST_TIMEOUT = 3 * 60; // seconds

  private boolean debugAllowed = DEFAULT_DEBUG_ALLOWED;

  @NotNull
  @Min(0)
  private Integer statusIntervalAuthenticated = DEFAULT_STATUS_INTERVAL_AUTHENTICATED;

  @NotNull
  @Min(0)
  private Integer statusIntervalAnonymous = DEFAULT_STATUS_INTERVAL_ANONYMOUS;

  @NotNull
  @Min(0)
  private Integer sessionTimeout = DEFAULT_SESSION_TIMEOUT;

  @NotNull
  @Min(MIN_REQUEST_TIMEOUT)
  private Integer requestTimeout = DEFAULT_REQUEST_TIMEOUT;

  @NotNull
  @Min(DEFAULT_LONG_REQUEST_TIMEOUT)
  private Integer longRequestTimeout = DEFAULT_LONG_REQUEST_TIMEOUT;

  @NotBlank
  private String title;

  public boolean isDebugAllowed() {
    return debugAllowed;
  }

  public void setDebugAllowed(final boolean debugAllowed) {
    this.debugAllowed = debugAllowed;
  }

  public int getStatusIntervalAuthenticated() {
    return statusIntervalAuthenticated;
  }

  public void setStatusIntervalAuthenticated(final int statusInterval) {
    this.statusIntervalAuthenticated = statusInterval;
  }

  public int getStatusIntervalAnonymous() {
    return statusIntervalAnonymous;
  }

  public void setStatusIntervalAnonymous(final int statusInterval) {
    this.statusIntervalAnonymous = statusInterval;
  }

  public int getSessionTimeout() {
    return sessionTimeout;
  }

  public void setSessionTimeout(final int sessionTimeout) {
    this.sessionTimeout = sessionTimeout;
  }

  public int getRequestTimeout() {
    return requestTimeout;
  }

  public void setRequestTimeout(final int requestTimeout) {
    this.requestTimeout = requestTimeout;
  }

  public int getLongRequestTimeout() {
    return longRequestTimeout;
  }

  public void setLongRequestTimeout(final Integer longRequestTimeout) {
    this.longRequestTimeout = longRequestTimeout;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(final String title) {
    this.title = title;
  }
}

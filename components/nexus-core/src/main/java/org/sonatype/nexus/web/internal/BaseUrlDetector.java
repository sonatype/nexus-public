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
package org.sonatype.nexus.web.internal;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;

import org.sonatype.nexus.configuration.application.GlobalRestApiSettings;
import org.sonatype.nexus.web.BaseUrlHolder;
import org.sonatype.sisu.goodies.common.ComponentSupport;

import com.google.common.base.Strings;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Detect application base-url.
 *
 * @see BaseUrlHolder
 * @see BaseUrlHolderFilter
 * @since 2.11.3
 */
@Named
@Singleton
public class BaseUrlDetector
    extends ComponentSupport
{
  private final GlobalRestApiSettings settings;

  private final Provider<HttpServletRequest> requestProvider;

  @Inject
  public BaseUrlDetector(final GlobalRestApiSettings settings,
                         final Provider<HttpServletRequest> requestProvider)
  {
    this.settings = checkNotNull(settings);
    this.requestProvider = checkNotNull(requestProvider);
  }

  /**
   * Return the current HTTP servlet-request if there is one in the current scope.
   */
  @Nullable
  private HttpServletRequest httpRequest() {
    try {
      return requestProvider.get();
    }
    catch (Exception e) {
      log.trace("Unable to resolve HTTP servlet-request", e);
      return null;
    }
  }

  /**
   * Detect base-url from forced settings, request or non-forced settings.
   */
  @Nullable
  public String detect() {
    // force base-url always wins if set
    if (settings.isEnabled() && settings.isForceBaseUrl() && !Strings.isNullOrEmpty(settings.getBaseUrl())) {
      return settings.getBaseUrl();
    }

    // attempt to detect from HTTP request
    HttpServletRequest request = httpRequest();
    if (request != null) {
      StringBuilder buff = new StringBuilder();
      String requestUrl = request.getRequestURL().toString();
      String pathInfo = request.getPathInfo();
      if (!Strings.isNullOrEmpty(pathInfo)) {
        requestUrl = requestUrl.substring(0, requestUrl.length() - pathInfo.length());
      }

      String servletPath = request.getServletPath();
      if (!Strings.isNullOrEmpty(servletPath)) {
        requestUrl = requestUrl.substring(0, requestUrl.length() - servletPath.length());
      }
      buff.append(requestUrl);

      return buff.toString();
    }

    // no request in context, non-forced base-url
    if (!Strings.isNullOrEmpty(settings.getBaseUrl())) {
      return settings.getBaseUrl();
    }

    // unable to determine base-url
    return null;
  }

  /**
   * Detect and set (if non-null) the base-url.
   */
  public void set() {
    String url = detect();
    if (url != null) {
      BaseUrlHolder.set(url);
    }
  }
}

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
package org.sonatype.nexus.internal.app;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.app.BaseUrlHolder;
import org.sonatype.nexus.common.app.BaseUrlManager;

import com.google.common.base.Strings;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default {@link BaseUrlManager}.
 *
 * @since 3.0
 */
@Named
@Singleton
public class BaseUrlManagerImpl
  extends ComponentSupport
  implements BaseUrlManager
{
  private final Provider<HttpServletRequest> requestProvider;

  private volatile String url;

  private volatile boolean force;

  @Inject
  public BaseUrlManagerImpl(final Provider<HttpServletRequest> requestProvider,
                            @Named("${org.sonatype.nexus.internal.app.BaseUrlManagerImpl.force:-false}") final boolean force) {
    this.requestProvider = checkNotNull(requestProvider);
    this.force = force;
    log.debug("Force: {}", force);
  }

  @Override
  public void setUrl(final String url) {
    this.url = url;
  }

  @Override
  public String getUrl() {
    return url;
  }

  @Override
  public boolean isForce() {
    return force;
  }

  @Override
  public void setForce(final boolean force) {
    this.force = force;
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
  @Override
  public String detectUrl() {
    // force base-url always wins if set
    if (force && !Strings.isNullOrEmpty(url)) {
      return url;
    }

    // attempt to detect from HTTP request
    HttpServletRequest request = httpRequest();
    if (request != null) {
      StringBuffer url = request.getRequestURL();
      String uri = request.getRequestURI();
      String ctx = request.getContextPath();
      return url.substring(0, url.length() - uri.length() + ctx.length());
    }

    // no request in context, non-forced base-url
    if (!Strings.isNullOrEmpty(url)) {
      return url;
    }

    // unable to determine base-url
    return null;
  }

  /**
   * Detect and set (if non-null) the base-url.
   */
  @Override
  public void detectAndHoldUrl() {
    String url = detectUrl();
    if (url != null) {
      BaseUrlHolder.set(url);
    }
  }
}

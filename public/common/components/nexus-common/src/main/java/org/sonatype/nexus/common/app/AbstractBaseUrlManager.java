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
package org.sonatype.nexus.common.app;

import javax.annotation.Nullable;
import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;

import com.google.common.base.Strings;
import jakarta.inject.Provider;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class AbstractBaseUrlManager
    implements BaseUrlManager
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  private final Provider<HttpServletRequest> requestProvider;

  protected volatile String url;

  @Autowired
  protected AbstractBaseUrlManager(final Provider<HttpServletRequest> requestProvider) {
    this.requestProvider = checkNotNull(requestProvider);
  }

  @Override
  public String getOrDetect() {
    if (Strings.isNullOrEmpty(url)) {
      log.debug("Base-url not set, attempting to detect");
      return detectUrl();
    }
    return url;
  }

  @Nullable
  @Override
  public String detectUrl() {
    // attempt to detect from HTTP request
    HttpServletRequest request = httpRequest();
    if (request != null) {
      StringBuffer url = request.getRequestURL();
      String uri = request.getRequestURI();
      String ctx = request.getContextPath();
      log.debug("Request url {{}} uri {{}} context path {{}} and scheme {{}}", url, uri, ctx, request.getScheme());
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
   * Detect base-url from forced settings, request or non-forced settings.
   */
  @Nullable
  public String detectRelativePath() {
    // attempt to detect from HTTP request
    HttpServletRequest request = httpRequest();
    if (request != null) {
      String contextPath = null;
      String requestUri = null;
      if (DispatcherType.FORWARD == request.getDispatcherType()) {
        contextPath = (String) request.getAttribute(RequestDispatcher.FORWARD_CONTEXT_PATH);
        requestUri = (String) request.getAttribute(RequestDispatcher.FORWARD_REQUEST_URI);
        log.debug("Request uri and context path from FORWARD dispatcher: {} {}", requestUri, contextPath);
      }
      else if (DispatcherType.ERROR == request.getDispatcherType()) {
        requestUri = (String) request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        log.debug("Request uri from ERROR dispatcher: {}", requestUri);
      }
      contextPath = contextPath == null ? request.getContextPath() : contextPath;
      requestUri = requestUri == null ? request.getRequestURI() : requestUri;
      // Remove the context path
      String path = requestUri.substring(contextPath.length());
      log.debug("Request uri and context path: {} {}", path, contextPath);
      return createRelativePath(countSlashes(path));
    }

    log.debug("Unable to detect relative path from request");
    // unable to determine base-url
    return "";
  }

  /**
   * Detect and set (if non-null) the base-url.
   */
  @Override
  public void detectAndHoldUrl() {
    String url = detectUrl();
    if (url != null) {
      final String relativePath = detectRelativePath();
      log.debug("Detected base-url: {} and relative path {}", url, relativePath);
      BaseUrlHolder.set(url, relativePath);
    }
    else {
      log.debug("Unable to detect base-url");
    }
  }

  /**
   * Return the current HTTP servlet-request if there is one in the current scope.
   */
  @Nullable
  protected HttpServletRequest httpRequest() {
    try {
      return requestProvider.get();
    }
    catch (Exception e) {
      log.trace("Unable to resolve HTTP servlet-request", e);
      return null;
    }
  }

  protected static String createRelativePath(final int length) {
    if (length == 0) {
      return ".";
    }

    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < length; i++) {
      sb.append("../");
    }
    // guarantee it does not end in a slash
    return sb.substring(0, sb.length() - 1);
  }

  protected static int countSlashes(final String path) {
    int count = 0;
    // we start at 1 to avoid leading slashes
    int previousIndex = 0;
    for (int i = 1; i < path.length(); i++) {
      if (path.charAt(i) == '/') {
        // skip double slashes
        if (previousIndex != (i - 1)) {
          ++count;
        }
        previousIndex = i;
      }
    }
    return count;
  }
}

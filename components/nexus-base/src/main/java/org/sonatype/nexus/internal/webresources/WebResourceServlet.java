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
package org.sonatype.nexus.internal.webresources;

import java.io.IOException;
import java.io.InputStream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sonatype.goodies.common.Time;
import org.sonatype.nexus.common.app.BaseUrlHolder;
import org.sonatype.nexus.internal.security.XFrameOptions;
import org.sonatype.nexus.servlet.ServletHelper;
import org.sonatype.nexus.webresources.WebResource;
import org.sonatype.nexus.webresources.WebResource.Prepareable;
import org.sonatype.nexus.webresources.WebResourceService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.net.HttpHeaders.CACHE_CONTROL;
import static com.google.common.net.HttpHeaders.CONTENT_LENGTH;
import static com.google.common.net.HttpHeaders.CONTENT_TYPE;
import static com.google.common.net.HttpHeaders.IF_MODIFIED_SINCE;
import static com.google.common.net.HttpHeaders.LAST_MODIFIED;
import static com.google.common.net.HttpHeaders.X_FRAME_OPTIONS;
import static com.google.common.net.HttpHeaders.X_XSS_PROTECTION;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_NOT_MODIFIED;

/**
 * Provides access to resources via configured {@link WebResourceService}.
 *
 * @since 2.8
 */
@Singleton
@Named
public class WebResourceServlet
    extends HttpServlet
{
  private static final Logger log = LoggerFactory.getLogger(WebResourceServlet.class);

  private final WebResourceService webResources;

  private final long maxAgeSeconds;

  private final XFrameOptions xframeOptions;

  private static final String INDEX_PATH = "/index.html";

  @Inject
  public WebResourceServlet(final WebResourceService webResources,
                            final XFrameOptions xframeOptions,
                            @Named("${nexus.webresources.maxAge:-30days}") final Time maxAge)
  {
    this.webResources = checkNotNull(webResources);
    this.maxAgeSeconds = checkNotNull(maxAge.toSeconds());
    this.xframeOptions = checkNotNull(xframeOptions);
    log.info("Max-age: {} ({} seconds)", maxAge, maxAgeSeconds);
  }

  @Override
  protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
      throws ServletException, IOException
  {
    String path = request.getPathInfo();

    // default-page handling
    if ("".equals(path) || "/".equals(path)) {
      path = INDEX_PATH;
    }
    else if (path.endsWith("/")) {
      path += "index.html";
    }
    else if (INDEX_PATH.equals(path)) {
      response.sendRedirect(BaseUrlHolder.get()); // prevent browser from sending XHRs to incorrect URL - NEXUS-14593
      return;
    }

    WebResource resource = webResources.getResource(path);
    if (resource == null) {
      // if there is an index.html for the requested path, redirect to it
      if (webResources.getResource(path + INDEX_PATH) != null) {
        String location = String.format("%s%s/", BaseUrlHolder.get(), path);
        log.debug("Redirecting: {} -> {}", path, location);
        response.sendRedirect(location);
      }
      else {
        response.sendError(SC_NOT_FOUND);
      }
      return;
    }

    serveResource(resource, request, response);
  }

  private void serveResource(WebResource resource,
                             final HttpServletRequest request,
                             final HttpServletResponse response)
      throws IOException
  {
    log.trace("Serving resource: {}", resource);

    // NEXUS-6569 Add X-Frame-Options header
    response.setHeader(X_FRAME_OPTIONS, xframeOptions.getValueForPath(request.getPathInfo()));
    response.setHeader(X_XSS_PROTECTION, "1; mode=block");

    // support resources which need to be prepared before serving
    if (resource instanceof Prepareable) {
      resource = ((Prepareable) resource).prepare();
      checkState(resource != null, "Prepared resource is null");
    }
    assert resource != null;

    String contentType = resource.getContentType();
    if (contentType == null) {
      contentType = WebResource.UNKNOWN_CONTENT_TYPE;
    }
    response.setHeader(CONTENT_TYPE, contentType);
    response.setDateHeader(LAST_MODIFIED, resource.getLastModified());

    // set content-length, complain if invalid
    long size = resource.getSize();
    if (size < 0) {
      log.warn("Resource {} has invalid size: {}", resource.getPath(), size);
    }
    response.setHeader(CONTENT_LENGTH, String.valueOf(size));

    // set max-age if cacheable
    if (resource.isCacheable()) {
      response.setHeader(CACHE_CONTROL, "max-age=" + maxAgeSeconds);
    }
    else {
      ServletHelper.addNoCacheResponseHeaders(response);
    }

    // honor if-modified-since GETs
    long ifModifiedSince = request.getDateHeader(IF_MODIFIED_SINCE);
    // handle conditional GETs
    if (ifModifiedSince > -1 && resource.getLastModified() <= ifModifiedSince) {
      // this is a conditional GET using time-stamp, and resource is not modified
      response.setStatus(SC_NOT_MODIFIED);
    }
    else {
      // send the content only if needed (this method will be called for HEAD requests too)
      if ("GET".equalsIgnoreCase(request.getMethod())) {
        try (InputStream in = resource.getInputStream()) {
          ServletHelper.sendContent(in, response);
        }
      }
    }
  }
}

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
package org.sonatype.nexus.webresources.internal;

import java.io.IOException;
import java.io.InputStream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sonatype.nexus.web.ErrorStatusException;
import org.sonatype.nexus.web.WebResource;
import org.sonatype.nexus.web.WebResource.Prepareable;
import org.sonatype.nexus.web.WebUtils;
import org.sonatype.nexus.webresources.WebResourceService;
import org.sonatype.sisu.goodies.common.Time;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
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

  private final WebUtils webUtils;

  private final long maxAgeSeconds;

  @Inject
  public WebResourceServlet(final WebResourceService webResources,
                            final WebUtils webUtils,
                            final @Named("${nexus.webresources.maxAge:-30days}") Time maxAge)
  {
    this.webResources = checkNotNull(webResources);
    this.webUtils = checkNotNull(webUtils);
    this.maxAgeSeconds = checkNotNull(maxAge.toSeconds());
    log.info("Max-age: {} ({} seconds)", maxAge, maxAgeSeconds);
  }

  @Override
  protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
      throws ServletException, IOException
  {
    String path = request.getPathInfo();

    // default-page handling
    if ("".equals(path) || "/".equals(path)) {
      path = "/index.html";
    }

    WebResource resource = webResources.getResource(path);
    if (resource == null) {
      throw new ErrorStatusException(SC_NOT_FOUND, "Not Found", "Resource not found");
    }

    serveResource(resource, request, response);
  }

  private void serveResource(WebResource resource,
                             final HttpServletRequest request,
                             final HttpServletResponse response)
      throws IOException
  {
    log.trace("Serving resource: {}", resource);

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
    response.setHeader("Content-Type", contentType);
    response.setDateHeader("Last-Modified", resource.getLastModified());

    // set content-length, complain if invalid
    long size = resource.getSize();
    if (size < 0) {
      log.warn("Resource {} has invalid size: {}", resource.getPath(), size);
    }
    response.setHeader("Content-Length", String.valueOf(size));

    // set max-age if cacheable
    if (resource.isCacheable()) {
      response.setHeader("Cache-Control", "max-age=" + maxAgeSeconds);
    }
    else {
      webUtils.addNoCacheResponseHeaders(response);
    }

    // honor if-modified-since GETs
    long ifModifiedSince = request.getDateHeader("if-modified-since");
    // handle conditional GETs
    if (ifModifiedSince > -1 && resource.getLastModified() <= ifModifiedSince) {
      // this is a conditional GET using time-stamp, and resource is not modified
      response.setStatus(SC_NOT_MODIFIED);
    }
    else {
      // send the content only if needed (this method will be called for HEAD requests too)
      if ("GET".equalsIgnoreCase(request.getMethod())) {
        try (InputStream in = resource.getInputStream()) {
          webUtils.sendContent(in, response);
        }
      }
    }
  }
}

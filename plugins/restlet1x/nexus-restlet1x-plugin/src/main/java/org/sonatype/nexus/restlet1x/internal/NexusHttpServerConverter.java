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
package org.sonatype.nexus.restlet1x.internal;

import com.noelios.restlet.http.HttpResponse;
import com.noelios.restlet.http.HttpServerConverter;
import org.restlet.Context;
import org.restlet.data.Parameter;
import org.restlet.util.Series;

/**
 * Custom {@link HttpServerConverter} that prevents restlet from adding general headers
 * that would duplicate those added elsewhere.
 *
 * @since 2.11.2
 */
public class NexusHttpServerConverter extends HttpServerConverter
{
  /**
   * Constructor.
   *
   * @param context The client context.
   */
  public NexusHttpServerConverter(final Context context) {
    super(context);
  }

  /**
   * Manipulate Restlet response headers.
   *
   * This implementation removes the Date and Server headers normally added by Restlet.
   *
   * @param response
   */
  @Override
  protected void addResponseHeaders(final HttpResponse response) {
    super.addResponseHeaders(response);
    final Series<Parameter> responseHeaders = response.getHttpCall()
        .getResponseHeaders();
    // our servlet container is adding a Date header for all responses
    // duplicate Date headers are not allowed by HTTP spec
    responseHeaders.removeFirst("Date");
    // Nexus is in charge of setting the main Server header
    // Restlet would normally add another Server header instead of amending existing one with all products as values
    responseHeaders.removeFirst("Server");
  }

}

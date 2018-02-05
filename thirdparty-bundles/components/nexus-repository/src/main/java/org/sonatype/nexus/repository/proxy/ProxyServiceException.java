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
package org.sonatype.nexus.repository.proxy;

import javax.annotation.Nonnull;

import org.apache.http.HttpResponse;

/**
 * A format-neutral proxy service exception thrown in cases like proxy with misconfiguration or remote down.
 *
 * @since 3.0
 */
public class ProxyServiceException
    extends RuntimeException
{
  private final HttpResponse httpResponse;

  public ProxyServiceException(final HttpResponse httpResponse) {
    super(httpResponse.getStatusLine().toString());
    this.httpResponse = httpResponse;
  }

  /**
   * Returns the {@link HttpResponse} but with a <b>consumed entity</b>, to be able to inspect response status and
   * headers, if needed.
   */
  @Nonnull
  public HttpResponse getHttpResponse() {
    return httpResponse;
  }
}

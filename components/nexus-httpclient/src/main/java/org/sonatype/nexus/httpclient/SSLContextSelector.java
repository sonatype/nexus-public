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
package org.sonatype.nexus.httpclient;

import javax.net.ssl.SSLContext;

import org.apache.http.protocol.HttpContext;

/**
 * Selects SSLContext to be used for given HTTP context.
 *
 * @since 2.8
 */
public interface SSLContextSelector
{
  // TODO: If this is only ever used as context attribute name, consider better naming strategy.

  /**
   * {@link HttpContext} attribute to be used to enable usage of Nexus Repository Truststore.
   *
   * @since 3.0
   */
  String USE_TRUST_STORE = "nexus.httpclient.ssl.trustStore";

  /**
   * Returns the desired {@link SSLContext} to be used or {@code null} if no selection possible (or available).
   *
   * In this case, HTTP client will use the "default" SSL context.
   */
  SSLContext select(HttpContext context);
}

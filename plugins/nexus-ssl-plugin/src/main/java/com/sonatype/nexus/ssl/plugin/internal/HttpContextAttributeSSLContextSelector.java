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
package com.sonatype.nexus.ssl.plugin.internal;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.net.ssl.SSLContext;

import org.sonatype.nexus.ssl.TrustStore;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.httpclient.SSLContextSelector;

import org.apache.http.protocol.HttpContext;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An {@link SSLContextSelector} that will make use of Nexus SSL TrustStore depending on value of
 * {@link SSLContextSelector#USE_TRUST_STORE} attribute.
 *
 * @since 3.0
 */
@Named
@Singleton
public class HttpContextAttributeSSLContextSelector
    extends ComponentSupport
    implements SSLContextSelector
{
  private final TrustStore trustStore;

  @Inject
  public HttpContextAttributeSSLContextSelector(final TrustStore trustStore) {
    this.trustStore = checkNotNull(trustStore);
  }

  @Override
  public SSLContext select(final HttpContext context) {
    Object useTrustStore = context.getAttribute(SSLContextSelector.USE_TRUST_STORE);
    if (Boolean.TRUE.equals(useTrustStore)) {
      return trustStore.getSSLContext();
    }
    return null;
  }
}

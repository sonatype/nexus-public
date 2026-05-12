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

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import javax.net.ssl.SSLContext;

import org.sonatype.nexus.httpclient.SSLContextSelector;
import org.sonatype.nexus.ssl.TrustStore;

import org.apache.http.protocol.HttpContext;
import org.springframework.context.annotation.Lazy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import org.springframework.stereotype.Component;

/**
 * An {@link SSLContextSelector} that will make use of Nexus SSL TrustStore depending on value of
 * {@link SSLContextSelector#USE_TRUST_STORE} attribute.
 *
 * @since 3.0
 */
@Component
@Singleton
public class HttpContextAttributeSSLContextSelector
    implements SSLContextSelector
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  private final TrustStore trustStore;

  @Inject
  public HttpContextAttributeSSLContextSelector(@Lazy final TrustStore trustStore) {
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

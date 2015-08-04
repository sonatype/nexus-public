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
package org.sonatype.nexus.apachehttpclient;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.http.client.HttpClient;

/**
 * A @Inject provider for the HttpClient instances. Word of warning: while this class might make it look like
 * HttpClient injection is doable and is fine, that should not be done in case of Nexus! The contract with Hc4Provider
 * is to ask for a client instance as late as possible (really to perform the request only), and toss it away,
 * do not keep the instance. By always asking for new instance, you are safe and error prone to configuration
 * changes, and posiblity to end up with stale client with regard some global settings like global HTTP Proxy is,
 * and to have your local client defunct. Hence, it is recommended to inject this Provider instead, and execute
 * a #get() method whenever is needed to have a new client instance. Implementation detail: this class is present
 * for "global" use cases only. Whenever you have a {@link org.sonatype.nexus.proxy.storage.remote.RemoteStorageContext}
 * accessibly, you should NOT use this provider, but use {@link Hc4Provider} instead!
 *
 * @since 2.3
 */
@Singleton
@Named
public class HttpClientProvider
    implements Provider<HttpClient>
{

  private final Hc4Provider hc4Provider;

  @Inject
  public HttpClientProvider(final Hc4Provider hc4Provider) {
    this.hc4Provider = hc4Provider;
  }

  @Override
  public HttpClient get() {
    return hc4Provider.createHttpClient();
  }
}

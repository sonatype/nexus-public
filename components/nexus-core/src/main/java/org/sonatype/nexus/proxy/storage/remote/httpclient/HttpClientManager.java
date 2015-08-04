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
package org.sonatype.nexus.proxy.storage.remote.httpclient;

import org.sonatype.nexus.proxy.repository.ProxyRepository;
import org.sonatype.nexus.proxy.storage.remote.RemoteStorageContext;

import org.apache.http.client.HttpClient;

/**
 * Component responsible for maintaining HTTP CLient instances for Proxy repositories, used by singleton
 * {@link HttpClientRemoteStorage}.
 *
 * @author cstamas
 * @since 2.2
 */
public interface HttpClientManager
{
  /**
   * Creates specifically configured {@link HttpClient} instance for given {@link ProxyRepository} repository. Note:
   * {@link RemoteStorageContext} is passed in as 2nd parameter, as this call happens usually during context update
   * triggered by stale context. The call {@link ProxyRepository#getRemoteStorageContext()} would cause endless loop
   * in this case. If calling this method outside of context update step, the previous call is fine and will not
   * loop.
   *
   * @return the pre-configured {@link HttpClient} to be used.
   */
  HttpClient create(final ProxyRepository proxyRepository, final RemoteStorageContext ctx);

  /**
   * Releases the {@link HttpClient} for given {@link ProxyRepository}. To be called whenever the instance needs to
   * be
   * dropped or recreated.
   */
  void release(final ProxyRepository proxyRepository, final RemoteStorageContext ctx);
}

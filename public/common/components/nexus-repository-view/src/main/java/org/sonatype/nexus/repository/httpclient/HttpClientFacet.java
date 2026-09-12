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
package org.sonatype.nexus.repository.httpclient;

import java.io.IOException;

import javax.annotation.Nullable;

import org.sonatype.nexus.repository.Facet;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.client.HttpClient;

/**
 * HTTP client facet.
 *
 * @since 3.0
 */
@Facet.Exposed
public interface HttpClientFacet
    extends Facet
{
  HttpClient getHttpClient();

  RemoteConnectionStatus getStatus();

  void setStatus(RemoteConnectionStatus status);

  Header createBasicAuthHeader();

  /**
   * @since 3.20
   */
  @Nullable
  String getBearerToken();

  /**
   * Records a connection failure that occurred against the remote for this repository's
   * configured HttpClient. Intended for use by facets that issue their own HTTP requests
   * outside of {@link #getHttpClient()} (e.g. OCI's bearer-token flow) so that auto-block
   * state stays accurate.
   *
   * <p>
   * The {@code target} is used both to display the failing URL and as the host the
   * reconnect-probe HEAD is issued against, so callers should pass the repository's
   * configured remote — not a helper endpoint on a different host (e.g. an OAuth realm
   * distinct from the registry).
   *
   * @param failure the IOException raised by the failing remote call
   * @param target the remote host the failed call was directed at; should be the repository's
   *          configured remote
   */
  void recordConnectionFailure(IOException failure, HttpHost target);
}

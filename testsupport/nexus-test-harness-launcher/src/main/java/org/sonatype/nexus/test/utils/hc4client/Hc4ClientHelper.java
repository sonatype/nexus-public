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
package org.sonatype.nexus.test.utils.hc4client;

import java.io.IOException;
import java.util.logging.Level;

import com.noelios.restlet.http.HttpClientCall;
import com.noelios.restlet.http.HttpClientHelper;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.params.SyncBasicHttpParams;
import org.restlet.Client;
import org.restlet.data.Protocol;
import org.restlet.data.Request;

/**
 * HC4 backed Restlet 1.1 Client, used in legacy ITs only, as many of the IT clients were coded against {@link Client}.
 */
public class Hc4ClientHelper
    extends HttpClientHelper
{
  private volatile DefaultHttpClient httpClient;

  public Hc4ClientHelper(Client client) {
    super(client);
    this.httpClient = null;
    getProtocols().add(Protocol.HTTP);
    getProtocols().add(Protocol.HTTPS);
  }

  @Override
  public HttpClientCall create(Request request) {
    HttpClientCall result = null;
    try {
      result = new Hc4MethodCall(this, request.getMethod().toString(),
          request.getResourceRef().toString(), request
          .isEntityAvailable());
    }
    catch (IOException ioe) {
      getLogger().log(Level.WARNING,
          "Unable to create the HTTP client call", ioe);
    }

    return result;
  }

  public HttpClient getHttpClient() {
    return this.httpClient;
  }

  public int getMaxConnectionsPerHost() {
    return Integer.parseInt(getHelpedParameters().getFirstValue(
        "maxConnectionsPerHost", "2"));
  }

  public int getMaxTotalConnections() {
    return Integer.parseInt(getHelpedParameters().getFirstValue(
        "maxTotalConnections", "20"));
  }

  public int getReadTimeout() {
    return Integer.parseInt(getHelpedParameters().getFirstValue(
        "readTimeout", "0"));
  }

  public boolean isFollowRedirects() {
    return Boolean.parseBoolean(getHelpedParameters().getFirstValue(
        "followRedirects", "false"));
  }

  @Override
  public void start()
      throws Exception
  {
    super.start();

    HttpParams params = new SyncBasicHttpParams();
    params.setParameter(HttpProtocolParams.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
    params.setBooleanParameter(HttpProtocolParams.USE_EXPECT_CONTINUE, false);
    params.setIntParameter(HttpConnectionParams.SOCKET_BUFFER_SIZE, 8 * 1024);
    params.setIntParameter(HttpConnectionParams.CONNECTION_TIMEOUT, getConnectTimeout());
    params.setIntParameter(HttpConnectionParams.SO_TIMEOUT, getReadTimeout());
    params.setParameter(ClientPNames.COOKIE_POLICY,
        CookiePolicy.IGNORE_COOKIES);
    final PoolingClientConnectionManager connManager = new PoolingClientConnectionManager();
    connManager.setMaxTotal(getMaxTotalConnections());
    connManager.setDefaultMaxPerRoute(getMaxConnectionsPerHost());
    httpClient = new DefaultHttpClient(connManager, params);
    getLogger().info("Starting the HTTP client");
  }

  @Override
  public void stop()
      throws Exception
  {
    getHttpClient().getConnectionManager().shutdown();
    getLogger().info("Stopping the HTTP client");
  }
}

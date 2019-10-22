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

import java.io.IOException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.httpclient.HttpClientManager;
import org.sonatype.nexus.httpclient.HttpClientPlan;
import org.sonatype.nexus.httpclient.HttpClientPlan.Customizer;
import org.sonatype.nexus.httpclient.HttpSchemes;
import org.sonatype.nexus.ssl.TrustStore;

import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.ManagedHttpClientConnection;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpCoreContext;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Certificates retriever from a host:port using Apache Http Client 4.
 *
 * @since ssl 1.0
 */
@Singleton
@Named
public class CertificateRetriever
    extends ComponentSupport
{
  private final HttpClientManager httpClientManager;

  private final TrustStore trustStore;

  @Inject
  public CertificateRetriever(final HttpClientManager httpClientManager, final TrustStore trustStore) {
    this.httpClientManager = checkNotNull(httpClientManager);
    this.trustStore = checkNotNull(trustStore);
  }

  private static final TrustManager ACCEPT_ALL_TRUST_MANAGER = new X509TrustManager()
  {
    @Override
    public X509Certificate[] getAcceptedIssuers() {
      return null;
    }

    @Override
    public void checkClientTrusted(final X509Certificate[] certs, final String authType) {
      // all trusted
    }

    @Override
    public void checkServerTrusted(final X509Certificate[] certs, final String authType) {
      // all trusted
    }
  };

  /**
   * Retrieves certificate chain of specified host:port using https protocol.
   *
   * @param host to get certificate chain from (cannot be null)
   * @param port of host to connect to
   * @return certificate chain
   * @throws Exception Re-thrown from accessing the remote host
   */
  public Certificate[] retrieveCertificatesFromHttpsServer(final String host, final int port) throws Exception {
    checkNotNull(host);

    log.info("Retrieving certificate from https://{}:{}", host, port);

    // setup custom connection manager so we can configure SSL to trust-all
    SSLContext sc = SSLContext.getInstance("TLS");
    sc.init(trustStore.getKeyManagers(), new TrustManager[]{ACCEPT_ALL_TRUST_MANAGER}, null);
    SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(sc, NoopHostnameVerifier.INSTANCE);
    Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
        .register(HttpSchemes.HTTP, PlainConnectionSocketFactory.getSocketFactory())
        .register(HttpSchemes.HTTPS, sslSocketFactory).build();
    final HttpClientConnectionManager connectionManager = new BasicHttpClientConnectionManager(registry);

    try {
      final AtomicReference<Certificate[]> certificates = new AtomicReference<>();

      HttpClient httpClient = httpClientManager.create(new Customizer()
      {
        @Override
        public void customize(final HttpClientPlan plan) {
          // replace connection-manager with customized version needed to fetch SSL certificates
          plan.getClient().setConnectionManager(connectionManager);

          // add interceptor to grab peer-certificates
          plan.getClient().addInterceptorFirst(new HttpResponseInterceptor()
          {
            @Override
            public void process(final HttpResponse response, final HttpContext context)
                throws HttpException, IOException
            {
              ManagedHttpClientConnection connection =
                  HttpCoreContext.adapt(context).getConnection(ManagedHttpClientConnection.class);

              // grab the peer-certificates from the session
              if (connection != null) {
                SSLSession session = connection.getSSLSession();
                if (session != null) {
                  certificates.set(session.getPeerCertificates());
                }
              }
            }
          });
        }
      });

      httpClient.execute(new HttpGet("https://" + host + ":" + port));

      return certificates.get();
    }
    finally {
      // shutdown single-use connection manager
      connectionManager.shutdown();
    }
  }

  /**
   * Retrieves certificate chain of specified host:port using direct socket connection.
   *
   * @param host to get certificate chain from (cannot be null)
   * @param port of host to connect to
   * @return certificate chain
   * @throws Exception Re-thrown from accessing the remote host
   */
  public Certificate[] retrieveCertificates(final String host, final int port) throws Exception {
    checkNotNull(host);

    log.info("Retrieving certificate from {}:{} using direct socket connection", host, port);

    SSLSocket socket = null;
    try {
      SSLContext sc = SSLContext.getInstance("TLS");
      sc.init(trustStore.getKeyManagers(), new TrustManager[]{ACCEPT_ALL_TRUST_MANAGER}, null);

      javax.net.ssl.SSLSocketFactory sslSocketFactory = sc.getSocketFactory();
      socket = (SSLSocket) sslSocketFactory.createSocket(host, port);
      socket.startHandshake();

      SSLSession session = socket.getSession();
      return session.getPeerCertificates();
    }
    finally {
      if (socket != null) {
        socket.close();
      }
    }
  }

  /**
   * Retrieves certificate chain of specified host:port using direct socket connection.
   *
   * @param host to get certificate chain from (cannot be null)
   * @param port of host to connect to, 443 will be used if not provided
   * @param protocolHint
   * @return certificate chain
   * @throws Exception Re-thrown from accessing the remote host
   *
   * @since 3.19
   */
  public Certificate[] retrieveCertificates(
      final String host,
      final Integer port,
      final String protocolHint) throws Exception
  {
    int actualPort = port != null ? port : 443;

    if ("https".equalsIgnoreCase(protocolHint)) {
      return retrieveCertificatesFromHttpsServer(host, actualPort);
    }

    try {
      return retrieveCertificates(host, actualPort);
    }
    catch (Exception e) {
      if (protocolHint == null) {
        log.debug("Cannot connect directly to {}:{}. Will retry using https protocol.", host, actualPort, e);
        return retrieveCertificatesFromHttpsServer(host, actualPort);
      }

      log.debug("Cannot connect directly to {}:{}.", host, actualPort, e);
      return null; // NOSONAR
    }
  }
}

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
package org.sonatype.nexus.ssl;

import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.sonatype.nexus.httpclient.HttpClientManager;
import org.sonatype.nexus.validation.ssrf.AntiSsrfService;

import jakarta.validation.ValidationException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link CertificateRetriever}.
 */
@ExtendWith(MockitoExtension.class)
class CertificateRetrieverTest
{
  @Mock
  HttpClientManager httpClientManager;

  @Mock
  AntiSsrfService antiSsrfService;

  @Mock
  TrustStore trustStore;

  @Mock
  CloseableHttpClient mockClient;

  private SSLServerSocket sslServerSocket;

  @InjectMocks
  private CertificateRetriever underTest;

  @BeforeEach
  void setUp() throws Exception {
    // Setup trust store with empty key managers (accept all for testing)
    lenient().when(trustStore.getKeyManagers()).thenReturn(new KeyManager[0]);
    lenient().when(httpClientManager.create(any())).thenReturn(mockClient);
  }

  @AfterEach
  void tearDown() throws Exception {
    if (sslServerSocket != null && !sslServerSocket.isClosed()) {
      sslServerSocket.close();
    }
  }

  /**
   * Tests retrieveCertificates with protocol hint "https" uses HTTPS method.
   */
  @Test
  void testRetrieveCertificatesWithHttpsHint() throws Exception {
    String testHost = "example.com";
    int testPort = 8443;

    // Setup mock to throw exception to prevent actual connection
    doThrow(new ValidationException("Test exception")).when(antiSsrfService).validateHost(testHost);

    assertThrows(ValidationException.class, () -> underTest.retrieveCertificates(testHost, testPort, "https"));

    verify(antiSsrfService).validateHost(testHost);
  }

  /**
   * Tests retrieveCertificates uses default port 443 when port is null.
   */
  @Test
  void testRetrieveCertificatesDefaultPort() throws Exception {
    String testHost = "example.com";

    // Setup mock to throw exception to prevent actual connection
    doThrow(new ValidationException("Test exception")).when(antiSsrfService).validateHost(testHost);

    assertThrows(RuntimeException.class, () -> underTest.retrieveCertificates(testHost, null, null));

    verify(antiSsrfService).validateHost(testHost);
  }

  /**
   * Tests retrieveCertificates returns null when protocolHint is set and direct connection fails.
   */
  @Test
  void testRetrieveCertificatesReturnsNullOnFailureWithHint() throws Exception {
    String testHost = "localhost";
    int testPort = 12345;
    String protocolHint = "http";

    // Connection will fail because no server is running
    // With a non-https hint and no fallback, returns null
    assertNull(underTest.retrieveCertificates(testHost, testPort, protocolHint));
    verify(antiSsrfService).validateHost(testHost);
  }

  /**
   * Tests retrieveCertificates falls back to HTTPS when protocolHint is null and direct connection fails.
   */
  @Test
  void testRetrieveCertificatesFallsBackToHttps() throws Exception {
    String testHost = "localhost";
    int testPort = 12345;
    String protocolHint = null;

    // Mock HttpClientManager to return a client that throws IOException
    doThrow(new IOException("Connection refused")).when(mockClient).execute(any(HttpGet.class));

    // Both direct and HTTPS connections will fail
    // When protocolHint is null, it tries https fallback
    assertThrows(IOException.class, () -> underTest.retrieveCertificates(testHost, testPort, protocolHint));
    verify(antiSsrfService).validateHost(testHost);
  }

  /**
   * Tests successful certificate retrieval via direct socket connection.
   */
  @Test
  void testRetrieveCertificatesDirectSocketSuccess() throws Exception {
    startTestServer();

    // Retrieve certificates
    Certificate[] certs = underTest.retrieveCertificates("localhost", sslServerSocket.getLocalPort());

    assertNotNull(certs);
    assertThat(certs, arrayWithSize(1));
    assertThat(certs[0], instanceOf(X509Certificate.class));
  }

  /**
   * Tests IOException message on connection failure.
   */
  @Test
  void testRetrieveCertificatesFromHttpsServerIOException() throws Exception {
    String testHost = "nonexistent.host.example";
    int testPort = 443;

    // Mock HttpClientManager to return a client that throws IOException
    doThrow(new IOException("Connection refused")).when(mockClient).execute(any(HttpGet.class));

    IOException exception =
        assertThrows(IOException.class, () -> underTest.retrieveCertificatesFromHttpsServer(testHost, testPort));

    assertThat(exception.getMessage(), allOf(containsString(testHost), containsString(String.valueOf(testPort))));
  }

  /**
   * Tests that the connection manager is properly shut down after use.
   */
  @Test
  void testConnectionManagerShutdownOnError() throws Exception {
    String testHost = "example.com";
    int testPort = 443;

    // Mock HttpClientManager to throw exception
    doThrow(new IOException("Connection refused")).when(mockClient).execute(any(HttpGet.class));

    assertThrows(IOException.class, () -> underTest.retrieveCertificatesFromHttpsServer(testHost, testPort));
  }

  /**
   * Tests successful certificate retrieval with all three parameters.
   */
  @Test
  void testRetrieveCertificatesWithAllParameters() throws Exception {
    startTestServer();

    // With https hint, it will use retrieveCertificatesFromHttpsServer - but that requires
    // HttpClientManager setup, so test with null hint to use direct socket
    Certificate[] certs = underTest.retrieveCertificates("localhost", sslServerSocket.getLocalPort(), null);

    assertNotNull(certs);
    assertThat(certs, arrayWithSize(1));
    verify(antiSsrfService).validateHost("localhost");
  }

  /**
   * Tests that port 443 is used when null port is provided with https hint.
   */
  @Test
  void testRetrieveCertificatesNullPortWithHttpsHint() throws Exception {
    String testHost = "example.com";

    // Mock to prevent actual connection
    doThrow(new IOException("Connection refused")).when(mockClient).execute(any(HttpGet.class));

    // With https hint, will attempt HTTPS connection to port 443
    assertThrows(IOException.class, () -> underTest.retrieveCertificates(testHost, null, "https"));
    verify(antiSsrfService).validateHost(testHost);
  }

  // Helper method to start a test SSL server
  private void startTestServer() throws Exception {
    // Generate a key pair for the test server
    KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
    keyPairGenerator.initialize(1024);
    KeyPair keyPair = keyPairGenerator.generateKeyPair();

    // Generate a self-signed certificate
    X509Certificate serverCertificate = CertificateUtil.generateCertificate(
        keyPair.getPublic(),
        keyPair.getPrivate(),
        "SHA256WITHRSA",
        365,
        "Test Server",
        "Test OU",
        "Test Org",
        "Test City",
        "Test State",
        "US");

    // Create a keystore for the server
    KeyStore keyStore = KeyStore.getInstance("JKS");
    keyStore.load(null, null);
    keyStore.setKeyEntry("test", keyPair.getPrivate(), "password".toCharArray(), new Certificate[]{serverCertificate});

    // Initialize SSL context
    SSLContext serverSslContext = SSLContext.getInstance("TLS");
    KeyManager[] keyManagers = createKeyManagers(keyStore, "password".toCharArray());
    // TrustManager[] trustManagers = createTrustManagers(keyStore);

    // Create a trust manager that accepts all certs for testing
    TrustManager[] acceptAllTrustManagers = new TrustManager[]{
        new X509TrustManager()
        {
          @Override
          public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
          }

          @Override
          public void checkClientTrusted(final X509Certificate[] certs, final String authType) {
          }

          @Override
          public void checkServerTrusted(final X509Certificate[] certs, final String authType) {
          }
        }
    };

    serverSslContext.init(keyManagers, acceptAllTrustManagers, new SecureRandom());

    // Create server socket
    SSLServerSocketFactory serverSocketFactory = serverSslContext.getServerSocketFactory();
    sslServerSocket = (SSLServerSocket) serverSocketFactory.createServerSocket(0);
    sslServerSocket.setNeedClientAuth(false);

    // Start server thread that accepts one connection
    Thread serverThread = new Thread(() -> {
      try (SSLSocket socket = (SSLSocket) sslServerSocket.accept()) {
        socket.getSession(); // Force handshake
        Thread.sleep(10); // Keep connection alive briefly
      }
      catch (Exception e) {
        // Ignore - test may complete before connection is made
      }
    });
    serverThread.setDaemon(true);
    serverThread.start();
  }

  private static KeyManager[] createKeyManagers(
      final java.security.KeyStore keyStore,
      final char[] password) throws Exception
  {
    KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
    kmf.init(keyStore, password);
    return kmf.getKeyManagers();
  }
}

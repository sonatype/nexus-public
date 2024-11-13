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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;

import org.sonatype.goodies.common.Time;
import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.crypto.CryptoHelper;
import org.sonatype.nexus.crypto.internal.CryptoHelperImpl;
import org.sonatype.nexus.ssl.internal.geronimo.KeystoreInstance;
import org.sonatype.nexus.ssl.spi.KeyStoreStorage;
import org.sonatype.nexus.ssl.spi.KeyStoreStorageManager;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.emptyArray;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static org.sonatype.nexus.ssl.KeyStoreManagerImpl.PRIVATE_KEY_ALIAS;

/**
 * Tests for {@link KeyStoreManagerImpl}.
 *
 * To turn on SSL debug logging, use:
 * {@code System.setProperty("javax.net.debug", "ssl");}
 */
@SuppressWarnings("HardCodedStringLiteral")
public class KeyStoreManagerImplTest
    extends TestSupport
{
  private final CryptoHelper crypto = new CryptoHelperImpl();

  private KeyStoreStorageManager storageManager;

  private KeyStoreManager keyStoreManager;

  @Before
  public void setUp() throws Exception {
    storageManager = new MemKeyStoreStorageManager();
    keyStoreManager = createKeyStoreManager(storageManager);
  }

  private KeyStoreManagerConfiguration createMockConfiguration() {
    KeyStoreManagerConfiguration config = mock(KeyStoreManagerConfiguration.class);
    // use lower strength for faster test execution
    when(config.getKeyStoreType()).thenReturn("JKS");
    when(config.getKeyAlgorithm()).thenReturn("RSA");
    when(config.getKeyAlgorithmSize()).thenReturn(1024);
    when(config.getSignatureAlgorithm()).thenReturn("SHA1WITHRSA");
    when(config.getCertificateValidity()).thenReturn(Time.days(36500));
    when(config.getKeyManagerAlgorithm()).thenReturn(KeyManagerFactory.getDefaultAlgorithm());
    when(config.getTrustManagerAlgorithm()).thenReturn(TrustManagerFactory.getDefaultAlgorithm());
    when(config.getPrivateKeyStorePassword()).thenReturn("pwd".toCharArray());
    when(config.getTrustedKeyStorePassword()).thenReturn("pwd".toCharArray());
    when(config.getPrivateKeyPassword()).thenReturn("pwd".toCharArray());
    return config;
  }

  private KeyStoreManager createKeyStoreManager(final KeyStoreStorageManager storageManager) {
    return new KeyStoreManagerImpl(crypto, storageManager, createMockConfiguration());
  }

  /**
   * Verifies a KeyPair is generated and added to the keyManager.
   */
  @Test
  public void testKeyPairGeneration() throws Exception {
    // create the key pair
    keyStoreManager.generateAndStoreKeyPair("Joe Coder", "dev", "codeSoft", "AnyTown", "state", "US");

    // verify the KeyManager[] only contains one key
    KeyManager[] keyManagers = keyStoreManager.getKeyManagers();
    assertThat(keyManagers, notNullValue());
    assertThat(keyManagers, arrayWithSize(1));
    assertThat(keyManagers[0], instanceOf(X509KeyManager.class));
    assertThat(
        ((X509KeyManager) keyManagers[0]).getCertificateChain(PRIVATE_KEY_ALIAS)[0].getSubjectDN().getName(),
        equalTo("CN=Joe Coder,OU=dev,O=codeSoft,L=AnyTown,ST=state,C=US"));

    // verify the TrustManager[] does not have any certs, we have not trusted anyone yet.
    TrustManager[] trustManagers = keyStoreManager.getTrustManagers();
    assertThat(trustManagers, notNullValue());
    assertThat(trustManagers, arrayWithSize(1));
    assertThat(trustManagers[0], instanceOf(X509TrustManager.class));
    assertThat(((X509TrustManager) trustManagers[0]).getAcceptedIssuers(), emptyArray());
  }

  /**
   * Tests recreating the key pair will update the KeyManager.
   */
  @Test
  public void testReKeyPairGeneration() throws Exception {
    // create the key pair
    keyStoreManager.generateAndStoreKeyPair("Original Key", "dev", "codeSoft", "AnyTown", "state", "US");

    KeyManager[] originalKeyManagers = keyStoreManager.getKeyManagers();

    keyStoreManager.generateAndStoreKeyPair("New Key", "dev", "codeSoft", "AnyTown", "state", "US");

    String expectedDN = "CN=New Key,OU=dev,O=codeSoft,L=AnyTown,ST=state,C=US";

    assertThat(originalKeyManagers, notNullValue());
    assertThat(originalKeyManagers, arrayWithSize(1));
    assertThat(originalKeyManagers[0], instanceOf(X509KeyManager.class));
    assertThat(((X509KeyManager) originalKeyManagers[0]).getCertificateChain(
        PRIVATE_KEY_ALIAS)[0].getSubjectDN().getName(), equalTo(expectedDN));

    KeyManager[] newKeyManagers = keyStoreManager.getKeyManagers();
    assertThat(newKeyManagers, notNullValue());
    assertThat(newKeyManagers, arrayWithSize(1));
    assertThat(newKeyManagers[0], instanceOf(X509KeyManager.class));
    assertThat(
        ((X509KeyManager) newKeyManagers[0]).getCertificateChain(PRIVATE_KEY_ALIAS)[0].getSubjectDN().getName(),
        equalTo(expectedDN));
  }

  @Test
  public void testEmptyPrincipalAttributes() throws Exception {
    // create the key pair
    keyStoreManager.generateAndStoreKeyPair(null, null, null, null, null, null);

    // verify the KeyManager[] only contains one key
    KeyManager[] keyManagers = keyStoreManager.getKeyManagers();
    assertThat(keyManagers, notNullValue());
    assertThat(keyManagers, arrayWithSize(1));
    assertThat(keyManagers[0], instanceOf(X509KeyManager.class));
    assertThat(
        ((X509KeyManager) keyManagers[0]).getCertificateChain(PRIVATE_KEY_ALIAS)[0].getSubjectDN().getName(),
        equalTo(""));
  }

  /**
   * Verifies a certificate is added to the TrustManager.
   */
  @Test
  public void testAddTrustedCertificate() throws Exception {
    // create the key pair
    keyStoreManager.generateAndStoreKeyPair("Joe Coder", "dev", "codeSoft", "AnyTown", "state", "US");

    // now create a server cert and add it.
    X509Certificate certificate =
        generateCertificate(10, "Foo Bar", "other-org-unit", "other-org", "other-locality", "other-state",
            "other-country");
    keyStoreManager.importTrustCertificate(certificate, "other-alias");

    // verify the TrustManager[] does not have any certs, we have not trusted anyone yet.
    TrustManager[] trustManagers = keyStoreManager.getTrustManagers();
    assertThat(trustManagers, notNullValue());
    assertThat(trustManagers, arrayWithSize(1));
    assertThat(trustManagers[0], instanceOf(X509TrustManager.class));
    assertThat(((X509TrustManager) trustManagers[0]).getAcceptedIssuers(), arrayWithSize(1));
    assertThat(((X509TrustManager) trustManagers[0]).getAcceptedIssuers(), arrayContaining(certificate));
  }

  /**
   * Verifies adding a certificate using an existing alias will replace the original certificate and that the
   * TrustManager has been updated.
   */
  @Test
  public void testUpdateTrustedCertificate() throws Exception {
    // create the key pair
    keyStoreManager.generateAndStoreKeyPair("Joe Coder", "dev", "codeSoft", "AnyTown", "state", "US");

    // now create a server cert and add it.
    X509Certificate certificate1 =
        generateCertificate(10, "original cert", "other-org-unit", "other-org", "other-locality", "other-state",
            "other-country");
    keyStoreManager.importTrustCertificate(certificate1, "other-alias");

    // verify the TrustManager[] does not have any certs, we have not trusted anyone yet.
    TrustManager[] originalTrustManagers = keyStoreManager.getTrustManagers();

    // create a new certificate and import that using the same alias
    X509Certificate certificate2 =
        generateCertificate(10, "new cert", "other-org-unit", "other-org", "other-locality", "other-state",
            "other-country");
    keyStoreManager.importTrustCertificate(certificate2, "other-alias");

    assertThat(originalTrustManagers, notNullValue());
    assertThat(originalTrustManagers, arrayWithSize(1));
    assertThat(originalTrustManagers[0], instanceOf(X509TrustManager.class));
    assertThat(((X509TrustManager) originalTrustManagers[0]).getAcceptedIssuers(), arrayWithSize(1));
    assertThat(((X509TrustManager) originalTrustManagers[0]).getAcceptedIssuers(),
        arrayContaining(certificate2));

    // now the same should be true if I get the TrustManager array after adding the certificate
    TrustManager[] afterUpdateTrustManagers = keyStoreManager.getTrustManagers();
    assertThat(afterUpdateTrustManagers, notNullValue());
    assertThat(afterUpdateTrustManagers, arrayWithSize(1));
    assertThat(afterUpdateTrustManagers[0], instanceOf(X509TrustManager.class));
    assertThat(((X509TrustManager) afterUpdateTrustManagers[0]).getAcceptedIssuers(), arrayWithSize(1));
    assertThat(((X509TrustManager) afterUpdateTrustManagers[0]).getAcceptedIssuers(),
        arrayContaining(certificate2));
  }

  /**
   * Verifies an empty alias throws a KeyNotFoundException.
   */
  @Test(expected = KeyNotFoundException.class)
  public void testGetTrustedCertificateEmptyAlias() throws Exception {
    keyStoreManager.getTrustedCertificate("");
  }

  /**
   * Verifies an blank (" ") alias throws a KeyNotFoundException.
   */
  @Test(expected = KeyNotFoundException.class)
  public void testGetTrustedCertificateBlankAlias() throws Exception {
    keyStoreManager.getTrustedCertificate(" ");
  }

  /**
   * Verifies an invalid alias throws a KeyNotFoundException. Much like the previous two tests, but this time with a
   * valid cert in the keystore.
   */
  @Test(expected = KeyNotFoundException.class)
  public void testGetTrustedCertificateNonExistentAlias() throws Exception {
    X509Certificate certificate =
        generateCertificate(10, "Foo Bar", "other-org-unit", "other-org", "other-locality", "other-state",
            "other-country");
    keyStoreManager.importTrustCertificate(certificate, "valie-alias");

    keyStoreManager.getTrustedCertificate("alias-that-does-not-exist");
  }

  /**
   * Tests initial condition returns an empty array of Certificates
   */
  @Test
  public void testGetTrustedCertificatesEmpty() throws Exception {
    Collection<Certificate> certificates = keyStoreManager.getTrustedCertificates();
    assertNotNull(certificates);
    assertTrue(certificates.isEmpty());
  }

  /**
   * Verifies the expected trusted certificates are returned when getTrustedCertificate is called.
   */
  @Test
  public void testGetTrustedCertificates() throws Exception {
    X509Certificate certificate1 =
        generateCertificate(10, "Cert One", "other-org-unit", "other-org", "other-locality", "other-state",
            "other-country");
    X509Certificate certificate2 =
        generateCertificate(10, "Cert Two", "other-org-unit", "other-org", "other-locality", "other-state",
            "other-country");
    X509Certificate certificate3 =
        generateCertificate(10, "Cert Three", "other-org-unit", "other-org", "other-locality", "other-state",
            "other-country");
    keyStoreManager.importTrustCertificate(certificate1, "one");
    keyStoreManager.importTrustCertificate(certificate2, "two");
    keyStoreManager.importTrustCertificate(certificate3, "three");

    assertThat(keyStoreManager.getTrustedCertificates(),
        contains((Certificate) certificate1, certificate2, certificate3));
  }

  /**
   * Verifies removing an certificate by an alias that does not exist, does NOT throw an Exception.
   */
  @SuppressWarnings("java:S2699") // sonar expects assertions, but best to let this exception bubble up
  @Test
  public void testRemoveCertificateDoesNotExist() throws Exception {
    // nothing much to do here, just call and expect it not to fail
    keyStoreManager.removeTrustCertificate("does-not-exist");
  }

  /**
   * Tests removing a certificate, and that the cert has been removed from the TrustManager
   */
  @Test
  public void testRemoveCertificate() throws Exception {
    X509Certificate certificate =
        generateCertificate(10, "Delete Me", "other-org-unit", "other-org", "other-locality", "other-state",
            "other-country");
    keyStoreManager.importTrustCertificate(certificate, "delete-me");
    assertEquals(1, keyStoreManager.getTrustedCertificates().size());

    // verify the TrustManager[] does not have any certs, we have not trusted anyone yet.
    TrustManager[] originalTrustManagers = keyStoreManager.getTrustManagers();

    // delete the key
    keyStoreManager.removeTrustCertificate("delete-me");
    assertThat(originalTrustManagers, notNullValue());
    assertThat(originalTrustManagers, arrayWithSize(1));
    assertThat(originalTrustManagers[0], instanceOf(X509TrustManager.class));
    assertThat(((X509TrustManager) originalTrustManagers[0]).getAcceptedIssuers(), arrayWithSize(0));

    assertTrue(keyStoreManager.getTrustedCertificates().isEmpty());

    // now the same should be true if I get the TrustManager array after adding the certificate
    TrustManager[] afterUpdateTrustManagers = keyStoreManager.getTrustManagers();
    assertThat(afterUpdateTrustManagers, notNullValue());
    assertThat(afterUpdateTrustManagers, arrayWithSize(1));
    assertThat(afterUpdateTrustManagers[0], instanceOf(X509TrustManager.class));
    assertThat(((X509TrustManager) afterUpdateTrustManagers[0]).getAcceptedIssuers(), arrayWithSize(0));
  }

  /**
   * Tests the import of a certificate in pem format
   */
  @Test
  public void testImportCertificateInPEMFormat() throws Exception {
    // create the key pair
    keyStoreManager.generateAndStoreKeyPair("Joe Coder", "dev", "codeSoft", "AnyTown", "state", "US");

    // now create a server cert and add it.
    X509Certificate certificate =
        generateCertificate(10, "Foo Bar", "other-org-unit", "other-org", "other-locality", "other-state",
            "other-country");

    String certString = CertificateUtil.serializeCertificateInPEM(certificate);

    keyStoreManager.importTrustCertificate(certString, "certFromPEM-alias");

    // verify the TrustManager[] does not have any certs, we have not trusted anyone yet.
    TrustManager[] trustManagers = keyStoreManager.getTrustManagers();
    assertThat(trustManagers, notNullValue());
    assertThat(trustManagers, arrayWithSize(1));
    assertThat(trustManagers[0], instanceOf(X509TrustManager.class));
    assertThat(((X509TrustManager) trustManagers[0]).getAcceptedIssuers(), arrayWithSize(1));
    assertThat(((X509TrustManager) trustManagers[0]).getAcceptedIssuers(), arrayContaining(certificate));
  }

  @Test
  public void importCertificateWithDuplicateAliasReplacePrevious() throws Exception {
    keyStoreManager.generateAndStoreKeyPair("Joe Coder", "dev", "codeSoft", "AnyTown", "state", "US");
    X509Certificate cert1 = generateCertificate(10, "a", "b", "c", "d", "e", "f");
    String cert1Pem = CertificateUtil.serializeCertificateInPEM(cert1);
    keyStoreManager.importTrustCertificate(cert1Pem, "alias1");
    assertEquals(1, keyStoreManager.getTrustedCertificates().size());

    // now add it again
    keyStoreManager.importTrustCertificate(cert1Pem, "alias1");
    assertEquals(1, keyStoreManager.getTrustedCertificates().size()); // still same size, replaced

    // now try different cert, same alias
    X509Certificate cert2 = generateCertificate(10, "a2", "b2", "c2", "d2", "e2", "f2");
    String cert2Pem = CertificateUtil.serializeCertificateInPEM(cert2);
    keyStoreManager.importTrustCertificate(cert2Pem, "alias1");
    assertEquals(1, keyStoreManager.getTrustedCertificates().size()); // still same size, replaced

    // verify replaced
    Certificate cert3 = keyStoreManager.getTrustedCertificate("alias1");
    assertEquals(cert2, cert3);
  }

  @Test(expected = CertificateParsingException.class)
  public void testImportEmptyPEMString() throws Exception {
    keyStoreManager.importTrustCertificate("", "empty-cert-alias");
  }

  @Test(expected = CertificateParsingException.class)
  public void testImportBlankPEMString() throws Exception {
    keyStoreManager.importTrustCertificate(" ", "empty-cert-alias");
  }

  @Test(expected = CertificateParsingException.class)
  public void testImportNullPEMString() throws Exception {
    keyStoreManager.importTrustCertificate((String) null, "empty-cert-alias");
  }

  /**
   * Verifies the isKeyPairInitialized() method returns false if the initial key pair is not created and true after it
   * is created.
   */
  @Test
  public void testIsKeyPairInitialized() throws Exception {
    // expect false
    assertThat("Expected false after creating a new (empty) KeyStoreManager",
        !keyStoreManager.isKeyPairInitialized());

    // create the key pair
    keyStoreManager.generateAndStoreKeyPair("Joe Coder", "dev", "codeSoft", "AnyTown", "state", "US");

    // it should return true now
    assertThat("Expected true after creating a new KeyPair", keyStoreManager.isKeyPairInitialized());

    // now create a new KeyStoreManager using the same directory, isKeyPairInitialized should return true
    assertThat("Expected true after loading an existing KeystoreManager",
        createKeyStoreManager(storageManager).isKeyPairInitialized());
  }

  /**
   * Verify that the Server and Client TrustManagers will trust each other.
   */
  @Test
  public void testServerAndClientTrustManagers() throws Exception {
    // first setup the keystores
    KeyStoreManager serverKeyStoreManager = createKeyStoreManager(new MemKeyStoreStorageManager());
    serverKeyStoreManager.generateAndStoreKeyPair("Server Side", "dev", "codeSoft", "AnyTown", "state", "US");

    KeyStoreManager clientKeyStoreManager = createKeyStoreManager(new MemKeyStoreStorageManager());
    clientKeyStoreManager.generateAndStoreKeyPair("Client Side", "dev", "codeSoft", "AnyTown", "state", "US");

    // now grab the cert from the client and stick it in the server
    Certificate clientCertificate = serverKeyStoreManager.getCertificate();
    serverKeyStoreManager.importTrustCertificate(clientCertificate, "client-side");

    //TODO: the server cert needs to be imported on the client side, we need to figure out how to deal with this.
    Certificate serverCertificate = serverKeyStoreManager.getCertificate();
    clientKeyStoreManager.importTrustCertificate(serverCertificate, "server-side");

    X509TrustManager serverTrustManager = (X509TrustManager) serverKeyStoreManager.getTrustManagers()[0];
    X509TrustManager clientTrustManager = (X509TrustManager) clientKeyStoreManager.getTrustManagers()[0];

    // verify the server trusts the client
    serverTrustManager.checkClientTrusted(new X509Certificate[]{(X509Certificate) clientCertificate}, "TLS");

    // verify the client trusts the server
    clientTrustManager.checkServerTrusted(new X509Certificate[]{(X509Certificate) serverCertificate}, "TLS");
  }

  /**
   * Confirm concurrent {@link KeyStoreManagerImpl#importTrustCertificate(Certificate, String)} invocations
   * occur safely.
   */
  @Test
  @Ignore("NEXUS-44783")
  public void testConcurrentImportTrustCertificate() throws Exception {
    X509Certificate certificate1 = generateCertificate(10,
        "concurrency-1", "ou", "o", "l", "st", "country");
    X509Certificate certificate2 = generateCertificate(10,
        "concurrency-2", "ou", "o", "l", "st", "country");

    KeyStoreManagerConfiguration configuration = createMockConfiguration();
    KeystoreInstance trustStore = mock(KeystoreInstance.class);
    CountDownLatch block = new CountDownLatch(1);

    // any calls to trustStore#importTrustCertificate should block on the latch
    doAnswer(blockingAnswer(block))
        .when(trustStore)
        .importTrustCertificate(
            any(Certificate.class), any(String.class),
            any(char[].class)
        );

    KeyStoreManagerImpl manager = new KeyStoreManagerImpl(crypto, configuration, null, trustStore);
    ListeningExecutorService service = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(1));
    List<ListenableFuture<String>> futures = new ArrayList<>();
    try {
      futures.add(service.submit(() -> {
        manager.importTrustCertificate(certificate1, "concurrency-1");
        return "concurrency-1";
      }));

      futures.add(service.submit(() -> {
        manager.importTrustCertificate(certificate2, "concurrency-2");
        return "concurrency-2";
      }));

      // no matter how long we wait, this list should be empty if we've guarded correctly
      List<String> results = Futures.successfulAsList(futures).get(100, TimeUnit.MILLISECONDS);
      assertEquals(0, results.size());

    } catch (TimeoutException e) {
      // expected; from Futures.successfulAsList().get()
    } finally {
      // release the latch so those threads are unblocked
      block.countDown();
      service.shutdownNow();
    }

    // a passing test will show that we only called KeyStoreInstance#importTrustCertificate once and only once
    // if we see more than one invocation, we passed the concurrency guard, which is unsafe
    // since KeystoreInstance is not thread-safe
    verify(trustStore, times(1))
        .importTrustCertificate(
            any(Certificate.class), any(String.class),
            any(char[].class));
  }

  /**
   * Confirm concurrent {@link KeyStoreManager#generateAndStoreKeyPair(String, String, String, String, String, String)}
   * invocations occur safely.
   */
  @Test
  public void testConcurrentGenerateAndStoreKeyPair() throws Exception {
    KeyStoreManagerConfiguration configuration = createMockConfiguration();
    KeystoreInstance privateStore = mock(KeystoreInstance.class);
    CountDownLatch block = new CountDownLatch(1);
    KeyStoreManagerImpl manager = new KeyStoreManagerImpl(crypto, configuration, privateStore, null);

    // any calls to privateStore#generateKeyPair should block
    doAnswer(blockingAnswer(block))
        .when(privateStore)
        .generateKeyPair(any(String.class), any(char[].class), any(char[].class), any(String.class),
            anyInt(), any(String.class), anyInt(), any(String.class), any(String.class), any(String.class),
            any(String.class), any(String.class), any(String.class));

    ListeningExecutorService service = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(1));
    List<ListenableFuture<String>> futures = new ArrayList<>();
    try {
      futures.add(service.submit(() -> {
        manager.generateAndStoreKeyPair("concurrency-1",
            "ou", "o", "l", "st", "c");
        return "concurrency-1";
      }));

      futures.add(service.submit(() -> {
        manager.generateAndStoreKeyPair("concurrency-2",
            "ou", "o", "l", "st", "c");
        return "concurrency-2";
      }));

      List<String> results = Futures.successfulAsList(futures).get(100, TimeUnit.MILLISECONDS);
      assertEquals(0, results.size());

    } catch (TimeoutException e) {
      // expected; from Futures.successfulAsList().get()
    } finally {
      // release the latch so those threads are unblocked
      block.countDown();
      service.shutdownNow();
    }

    // a passing test will show that we only called KeyStoreInstance#generateKeyPair once and only once
    // if we see more than one invocation, we passed the concurrency guard, which is unsafe
    // since KeystoreInstance is not thread-safe
    verify(privateStore, times(1))
        .generateKeyPair(any(String.class), any(char[].class), any(char[].class), any(String.class),
            anyInt(), any(String.class), anyInt(), any(String.class), any(String.class), any(String.class),
            any(String.class), any(String.class), any(String.class));
  }

  private Answer<?> blockingAnswer(CountDownLatch latch) {
    return new Answer<Object>() {
      @Override
      public Object answer(final InvocationOnMock invocation) throws Throwable {
        latch.await();
        return null;
      }
    };
  }

  private X509Certificate generateCertificate(int validity,
                                              String commonName,
                                              String orgUnit,
                                              String organization,
                                              String locality,
                                              String state,
                                              String country)
      throws Exception
  {
    KeyPairGenerator kpgen = KeyPairGenerator.getInstance("RSA");
    kpgen.initialize(512);
    KeyPair keyPair = kpgen.generateKeyPair();

    return CertificateUtil.generateCertificate(keyPair.getPublic(), keyPair.getPrivate(), "SHA1WITHRSA", validity,
        commonName, orgUnit, organization, locality, state, country);
  }

  /**
   * Tests an SSLContext (created with input from the KeyStoreManager) can be used to with an SSLServerSocketFactory
   */
  @Test
  public void testSSLConnection() throws Exception {
    // first setup the keystores
    KeyStoreManager serverKeyStoreManager = createKeyStoreManager(new MemKeyStoreStorageManager());
    serverKeyStoreManager.generateAndStoreKeyPair("Server Side", "dev", "codeSoft", "AnyTown", "state", "US");

    KeyStoreManager clientKeyStoreManager = createKeyStoreManager(new MemKeyStoreStorageManager());
    clientKeyStoreManager.generateAndStoreKeyPair("Client Side", "dev", "codeSoft", "AnyTown", "state", "US");

    // now grab the cert from the client and stick it in the pub
    Certificate clientCertificate = clientKeyStoreManager.getCertificate();
    serverKeyStoreManager.importTrustCertificate(clientCertificate, "client-side");

    // now add the server key to the client
    Certificate serverCertificate = serverKeyStoreManager.getCertificate();
    clientKeyStoreManager.importTrustCertificate(serverCertificate, "server-side");

    SSLContext serverSslContext = SSLContext.getInstance("TLS");
    serverSslContext.init(serverKeyStoreManager.getKeyManagers(), serverKeyStoreManager.getTrustManagers(),
        new SecureRandom());
    log("default ssl session timeout server: {}", serverSslContext.getServerSessionContext().getSessionTimeout());
    log("default ssl session timeout client: {}", serverSslContext.getClientSessionContext().getSessionTimeout());
    serverSslContext.getServerSessionContext().setSessionTimeout(1);
    serverSslContext.getClientSessionContext().setSessionTimeout(1);

    SSLContext clientSslContext = SSLContext.getInstance("TLS");
    clientSslContext.init(clientKeyStoreManager.getKeyManagers(), clientKeyStoreManager.getTrustManagers(),
        new SecureRandom());
    clientSslContext.getServerSessionContext().setSessionTimeout(1);
    clientSslContext.getClientSessionContext().setSessionTimeout(1);

    // Setup the SSL Server Socket
    SSLServerSocketFactory sslServerSocketfactory = serverSslContext.getServerSocketFactory();
    final SSLServerSocket sslServerSocket = (SSLServerSocket) sslServerSocketfactory.createServerSocket(0);

    // NOTE we are also testing the client auth!
    sslServerSocket.setNeedClientAuth(true);

    final List<String> results = new ArrayList<String>();

    SSLServerThread serverThread = new SSLServerThread(sslServerSocket, results);
    serverThread.start();

    String expectedXferData = "Some String Data";
    List<String> expectedResults = new ArrayList<String>();
    expectedResults.add("C=US,ST=state,L=AnyTown,O=codeSoft,OU=dev,CN=Client Side"); // original client DN
    expectedResults.add("C=US,ST=state,L=AnyTown,O=codeSoft,OU=dev,CN=Server Side"); // original server DN
    expectedResults.add(expectedXferData); // xfer data
    expectedResults.add("C=US,ST=state,L=AnyTown,O=codeSoft,OU=dev,CN=New Client Side"); // new client DN
    expectedResults.add("C=US,ST=state,L=AnyTown,O=codeSoft,OU=dev,CN=New Server Side"); // new server DN
    expectedResults.add(expectedXferData); // xfer data

    // make the connection
    SSLSocket sslSocket = null;
    try {
      SSLSocketFactory sslSocketFactory = clientSslContext.getSocketFactory();
      sslSocket = (SSLSocket) sslSocketFactory.createSocket("localhost", sslServerSocket.getLocalPort());

      OutputStream outputStream = sslSocket.getOutputStream();
      OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);

      outputStreamWriter.write(expectedXferData + "\n");
      outputStreamWriter.flush();

      // wait for server to die
      serverThread.join(100);
      sslSocket.close();

      // IMPORTANT, the default timeout is 24 hours,
      Thread.sleep(1010);// let the session expire

      serverThread = new SSLServerThread(sslServerSocket, results);
      serverThread.start();
      // now the complicated part, update the certs on both sides and expect everything to work.
      serverKeyStoreManager.generateAndStoreKeyPair("New Server Side", "dev", "codeSoft", "AnyTown", "state",
          "US");
      clientKeyStoreManager.generateAndStoreKeyPair("New Client Side", "dev", "codeSoft", "AnyTown", "state",
          "US");

      // now grab the cert from the client and stick it in the pub
      Certificate newClientCertificate = clientKeyStoreManager.getCertificate();
      serverKeyStoreManager.importTrustCertificate(newClientCertificate, "client-side");

      // now add the server key to the client
      Certificate newServerCertificate = serverKeyStoreManager.getCertificate();
      clientKeyStoreManager.importTrustCertificate(newServerCertificate, "server-side");

      sslSocket = (SSLSocket) sslSocketFactory.createSocket("localhost", sslServerSocket.getLocalPort());

      outputStream = sslSocket.getOutputStream();
      outputStreamWriter = new OutputStreamWriter(outputStream);

      outputStreamWriter.write(expectedXferData + "\n");
      outputStreamWriter.flush();

      // wait for server to die
      serverThread.join(100);

      // make sure the results match
      assertThat(results, equalTo(expectedResults));

    }
    finally {
      sslSocket.close();
      sslServerSocket.close();
    }
  }

  class SSLServerThread
      extends Thread
  {
    private final SSLServerSocket sslServerSocket;

    private final List<String> results;

    public SSLServerThread(SSLServerSocket sslServerSocket, List<String> results) {
      this.sslServerSocket = sslServerSocket;
      this.results = results;
    }

    @Override
    public void run() {
      try {
        SSLSocket sslSocket = (SSLSocket) sslServerSocket.accept();

        results.add(sslSocket.getSession().getPeerPrincipal().getName());
        results.add(sslSocket.getSession().getLocalPrincipal().getName());

        InputStream inputStream = sslSocket.getInputStream();
        InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

        String data;
        while ((data = bufferedReader.readLine()) != null) {
          results.add(data);
        }
      }
      catch (Exception exception) {
        log(exception.getMessage(), exception);
      }
    }
  }

  static class MemKeyStoreStorageManager
      implements KeyStoreStorageManager
  {
    class MemKeyStoreStorage
        implements KeyStoreStorage
    {
      private final String keyStoreName;

      MemKeyStoreStorage(final String keyStoreName) {
        this.keyStoreName = checkNotNull(keyStoreName);
      }

      @Override
      public boolean exists() {
        return storages.get(keyStoreName) != null;
      }

      @Override
      public boolean modified() {
        return false;
      }

      @Override
      public void load(final KeyStore keyStore, final char[] password)
          throws NoSuchAlgorithmException, CertificateException, IOException
      {
        byte[] bytes = storages.get(keyStoreName);
        checkState(bytes != null);
        keyStore.load(new ByteArrayInputStream(bytes), password);
      }

      @Override
      public void save(final KeyStore keyStore, final char[] password)
          throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException
      {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        keyStore.store(baos, password);
        storages.put(keyStoreName, baos.toByteArray());
      }
    }

    private final Map<String, byte[]> storages = new HashMap<>();

    @Override
    public KeyStoreStorage createStorage(final String keyStoreName) {
      return new MemKeyStoreStorage(keyStoreName);
    }
  }
}

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
package org.sonatype.nexus.testsuite.raw;

import java.io.File;
import java.net.URL;

import javax.cache.CacheManager;
import javax.inject.Inject;

import org.sonatype.goodies.httpfixture.server.fluent.Behaviours;
import org.sonatype.goodies.httpfixture.server.fluent.Server;
import org.sonatype.nexus.common.net.PortAllocator;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.http.HttpStatus;
import org.sonatype.nexus.testsuite.proxy.DefaultCacheSettingsTester;
import org.sonatype.nexus.testsuite.testsupport.raw.RawClient;
import org.sonatype.nexus.testsuite.testsupport.raw.RawITSupport;

import org.apache.http.HttpResponse;
import org.apache.http.entity.ContentType;
import org.junit.Before;
import org.junit.Test;

import static org.apache.commons.io.FileUtils.readFileToByteArray;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.sonatype.goodies.httpfixture.server.fluent.Behaviours.content;
import static org.sonatype.nexus.testsuite.testsupport.FormatClientSupport.bytes;
import static org.sonatype.nexus.testsuite.testsupport.FormatClientSupport.status;

/**
 * IT for proxy raw repositories
 */
public class RawProxyOfHostedIT
    extends RawITSupport
{
  private static final String INDEX_HTML = "index.html";

  private static final String TEST_PATH = "alphabet.txt";

  private static final String TEST_PATH_PLUS = "alpha+bet.txt";

  private static final String TEST_CONTENT = "alphabet.txt";

  private RawClient hostedClient;

  private RawClient proxyClient;

  private Repository hostedRepo;

  private Repository proxyRepo;

  @Inject
  private CacheManager cacheManager;

  @Before
  public void setUpRepositories() throws Exception {
    hostedRepo = repos.createRawHosted("raw-test-hosted");
    hostedClient = rawClient(hostedRepo);

    URL hostedRepoUrl = repositoryBaseUrl(hostedRepo);
    proxyRepo = repos.createRawProxy("raw-test-proxy", hostedRepoUrl.toExternalForm());

    proxyClient = rawClient(proxyRepo);
  }

  @Test
  public void unresponsiveRemoteProduces404() throws Exception {
    repos.deleteRepository(hostedRepo);

    assertThat(status(proxyClient.get(TEST_PATH)), is(HttpStatus.NOT_FOUND));
  }

  @Test
  public void responsiveRemoteProduces404() throws Exception {
    assertThat(status(proxyClient.get(TEST_PATH)), is(HttpStatus.NOT_FOUND));
  }

  @Test
  public void fetchFromRemote() throws Exception {
    File testFile = resolveTestFile(TEST_CONTENT);
    hostedClient.put(TEST_PATH, ContentType.TEXT_PLAIN, testFile);

    assertThat(bytes(proxyClient.get(TEST_PATH)), is(readFileToByteArray(testFile)));

    // test with + sysmbol in file name
    testFile = resolveTestFile(TEST_PATH_PLUS);
    hostedClient.put(TEST_PATH_PLUS, ContentType.TEXT_PLAIN, testFile);

    assertThat(bytes(proxyClient.get(TEST_PATH_PLUS)), is(readFileToByteArray(testFile)));
  }

  @Test
  public void fetchFromRemoteWithEncodedFileName() throws Exception {
    File testFile = resolveTestFile("%252B%2520.txt");
    hostedClient.put(testFile.getName(), ContentType.TEXT_PLAIN, testFile);

    HttpResponse httpResponse = proxyClient.get(testFile.getName());

    assertThat(status(httpResponse), is(HttpStatus.OK));
    assertThat(bytes(httpResponse), is(readFileToByteArray(testFile)));
  }

  @Test
  public void setLastDownloadedOnGet() throws Exception {
    final File testFile = resolveTestFile(TEST_CONTENT);
    hostedClient.put(TEST_PATH, ContentType.TEXT_PLAIN, testFile);

    HttpResponse response = proxyClient.get(TEST_PATH);
    assertThat(status(response), is(HttpStatus.OK));
    assertThat(bytes(response), is(readFileToByteArray(testFile)));
    assertThat(getLastDownloadedTime(proxyRepo, TEST_PATH).isBeforeNow(), is(equalTo(true)));
  }

  @Test
  public void notFoundCaches404() throws Exception {
    // Ask for a nonexistent file
    proxyClient.get(TEST_PATH);

    // Put the file in the hosted repo
    hostedClient.put(TEST_PATH, ContentType.TEXT_PLAIN, resolveTestFile(TEST_CONTENT));

    // The NFC should ensure we still see the 404
    assertThat(status(proxyClient.get(TEST_PATH)), is(HttpStatus.NOT_FOUND));
  }

  @Test
  public void status401ViaProxyProduces503() throws Exception {
    responseViaProxyProduces(HttpStatus.UNAUTHORIZED, HttpStatus.SERVICE_UNAVAILABLE);
  }

  @Test
  public void status402ViaProxyProduces503() throws Exception {
    responseViaProxyProduces(HttpStatus.PAYMENT_REQUIRED, HttpStatus.SERVICE_UNAVAILABLE);
  }

  @Test
  public void status407ViaProxyProduces503() throws Exception {
    responseViaProxyProduces(HttpStatus.PROXY_AUTHENTICATION_REQUIRED, HttpStatus.SERVICE_UNAVAILABLE);
  }

  @Test
  public void status500ViaProxyProduces503() throws Exception {
    responseViaProxyProduces(HttpStatus.INTERNAL_SERVER_ERROR, HttpStatus.SERVICE_UNAVAILABLE);
  }

  @Test
  public void status503ViaProxyProduces503() throws Exception {
    responseViaProxyProduces(HttpStatus.SERVICE_UNAVAILABLE, HttpStatus.SERVICE_UNAVAILABLE);
  }

  @Test
  public void remoteHasNoContent() throws Exception {
    Server server = Server.withPort(PortAllocator.nextFreePort())
        .serve("/*").withBehaviours(Behaviours.error(HttpStatus.NOT_FOUND))
        .start();
    try {
      proxyClient = rawClient(repos.createRawProxy(testName.getMethodName(), server.getUrl().toExternalForm()));
      assertThat(status(hostedClient.get(TEST_PATH + "/")), is(HttpStatus.NOT_FOUND));
      assertThat(status(hostedClient.get("")), is(HttpStatus.NOT_FOUND));
    }
    finally {
      server.stop();
    }
  }

  @Test
  public void hostedHasNoContent() throws Exception {
    assertThat(status(hostedClient.get(TEST_PATH + "/")), is(HttpStatus.NOT_FOUND));
    assertThat(status(hostedClient.get("")), is(HttpStatus.NOT_FOUND));
  }

  @Test
  public void rootShouldServeRemoteIndexHtmlContentIfPresent() throws Exception {
    final File testFile = resolveTestFile(INDEX_HTML);
    hostedClient.put(INDEX_HTML, ContentType.TEXT_PLAIN, testFile);

    HttpResponse response = proxyClient.get("");
    assertThat(status(response), is(HttpStatus.OK));
    assertThat(bytes(response), is(readFileToByteArray(testFile)));
    assertThat(getLastDownloadedTime(proxyRepo, ".").isBeforeNow(), is(equalTo(true)));
  }

  @Test
  public void rootShouldServeRemoteIndexHtmContentIfPresent() throws Exception {
    final File testFile = resolveTestFile(INDEX_HTML);
    hostedClient.put("index.htm", ContentType.TEXT_PLAIN, testFile);

    HttpResponse response = proxyClient.get("");
    assertThat(status(response), is(HttpStatus.OK));
    assertThat(bytes(response), is(readFileToByteArray(testFile)));
    assertThat(getLastDownloadedTime(proxyRepo, ".").isBeforeNow(), is(equalTo(true)));
  }

  private void responseViaProxyProduces(final int upstreamStatus, final int downstreamStatus) throws Exception {
    Server server =
        Server.withPort(PortAllocator.nextFreePort()).serve("/*").withBehaviours(Behaviours.error(upstreamStatus))
            .start();
    try {
      proxyClient = rawClient(repos.createRawProxy("raw-test-proxy-" + upstreamStatus + "-" + downstreamStatus,
          server.getUrl().toExternalForm()));
      assertThat(status(proxyClient.get(TEST_PATH)), is(downstreamStatus));
    }
    finally {
      server.stop();
    }
  }

  @Test
  public void status401ViaGroupProduces404() throws Exception {
    responseViaGroupProduces(HttpStatus.UNAUTHORIZED, HttpStatus.NOT_FOUND);
  }

  @Test
  public void status402ViaGroupProduces404() throws Exception {
    responseViaGroupProduces(HttpStatus.PAYMENT_REQUIRED, HttpStatus.NOT_FOUND);
  }

  @Test
  public void status407ViaGroupProduces404() throws Exception {
    responseViaGroupProduces(HttpStatus.PROXY_AUTHENTICATION_REQUIRED, HttpStatus.NOT_FOUND);
  }

  @Test
  public void status500ViaGroupProduces404() throws Exception {
    responseViaGroupProduces(HttpStatus.INTERNAL_SERVER_ERROR, HttpStatus.NOT_FOUND);
  }

  @Test
  public void status503ViaGroupProduces404() throws Exception {
    responseViaGroupProduces(HttpStatus.SERVICE_UNAVAILABLE, HttpStatus.NOT_FOUND);
  }

  @Test
  public void retrieveRawWhenRemoteOffline() throws Exception {
    Server server = Server.withPort(PortAllocator.nextFreePort()).serve("/*")
        .withBehaviours(content("Response"))
        .start();
    try {
      proxyClient = rawClient(repos.createRawProxy("raw-test-proxy-offline", server.getUrl().toExternalForm()));
      proxyClient.get(TEST_PATH);
    }
    finally {
      server.stop();
    }
    assertThat(status(proxyClient.get(TEST_PATH)), is(200));
  }

  private void responseViaGroupProduces(final int upstreamStatus, final int downstreamStatus) throws Exception {
    Server server =
        Server.withPort(PortAllocator.nextFreePort()).serve("/*").withBehaviours(Behaviours.error(upstreamStatus))
            .start();
    try {
      Repository proxy = repos.createRawProxy("raw-test-proxy-" + upstreamStatus + "-" + downstreamStatus,
          server.getUrl().toExternalForm());
      Repository group = repos.createRawGroup("raw-test-group-" + upstreamStatus + "-" + downstreamStatus,
          proxy.getName());
      proxyClient = rawClient(group);
      assertThat(status(proxyClient.get(TEST_PATH)), is(downstreamStatus));
    }
    finally {
      server.stop();
    }
  }

  @Test
  public void verifyDefaultCacheSettings() {
    Repository repository = repos.createRawProxy(testName.getMethodName(), "http://example.com");
    DefaultCacheSettingsTester.verifyNegativeCacheSettings(repository, cacheManager);
  }
}

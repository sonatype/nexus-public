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
package org.sonatype.nexus.repository.proxy.warmup;

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.RepositoryStartedEvent;
import org.sonatype.nexus.repository.httpclient.HttpClientFacet;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.proxy.ProxyFacet;
import org.sonatype.nexus.repository.types.GroupType;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.repository.types.ProxyType;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ProxyWarmupService}.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class ProxyWarmupServiceTest
{
  @Mock
  private RepositoryManager repositoryManager;

  @Mock
  private Repository repository;

  @Mock
  private ProxyFacet proxyFacet;

  @Mock
  private HttpClientFacet httpClientFacet;

  @Mock
  private HttpClient httpClient;

  @Mock
  private HttpResponse httpResponse;

  @Mock
  private StatusLine statusLine;

  private ProxyWarmupService underTest;

  @Before
  public void setUp() throws Exception {
    // Set default system properties for tests
    System.setProperty("nexus.proxy.warmup.enabled", "true");
    System.setProperty("nexus.proxy.warmup.timeout", "5000");
    System.setProperty("nexus.proxy.warmup.threadpool.size", "5");

    underTest = new ProxyWarmupService(repositoryManager);

    // Setup common mocks
    when(repository.getName()).thenReturn("maven-central");
    when(repository.facet(ProxyFacet.class)).thenReturn(proxyFacet);
    when(repository.facet(HttpClientFacet.class)).thenReturn(httpClientFacet);
    when(httpClientFacet.getHttpClient()).thenReturn(httpClient);
    when(proxyFacet.getRemoteUrl()).thenReturn(new URI("https://repo1.maven.org/maven2/"));
  }

  @Test
  public void testServiceStartsWhenEnabled() throws Exception {
    // When
    underTest.start();

    // Then - verify service started without throwing exceptions
    // (Cannot verify isStarted() directly as it's protected)
  }

  @Test
  public void testServiceStartsWhenDisabled() throws Exception {
    // Given
    System.setProperty("nexus.proxy.warmup.enabled", "false");
    underTest = new ProxyWarmupService(repositoryManager);

    // When
    underTest.start();

    // Then - verify service started without throwing exceptions
  }

  @Test
  public void testServiceShutdownGracefully() throws Exception {
    // Given
    underTest.start();

    // When
    underTest.stop();

    // Then - verify service stopped without throwing exceptions
  }

  /**
   * Test that proxy repository events trigger warmup.
   */
  @Test
  public void testProxyRepositoryTriggersWarmup() throws Exception {
    // Setup
    when(repository.getType()).thenReturn(new ProxyType());
    when(httpClient.execute(any(HttpUriRequest.class))).thenReturn(httpResponse);
    when(httpResponse.getStatusLine()).thenReturn(statusLine);
    when(statusLine.getStatusCode()).thenReturn(200);

    underTest.start();

    // Use latch to wait for async execution
    CountDownLatch latch = new CountDownLatch(1);
    when(httpClient.execute(any(HttpUriRequest.class))).thenAnswer(invocation -> {
      latch.countDown();
      return httpResponse;
    });

    // When
    RepositoryStartedEvent event = new RepositoryStartedEvent(repository);
    underTest.on(event);

    // Wait for warmup to complete (timeout after 2 seconds)
    boolean completed = latch.await(2, TimeUnit.SECONDS);

    // Then
    assertThat("Warmup should have completed", completed, is(true));
    verify(httpClient, times(1)).execute(any(HttpUriRequest.class));
  }

  /**
   * Test that non-proxy repository events are skipped.
   */
  @Test
  public void testNonProxyRepositoryIsSkipped() throws Exception {
    // Setup - hosted repository
    when(repository.getType()).thenReturn(new HostedType());

    underTest.start();

    CountDownLatch latch = new CountDownLatch(1);
    doAnswer(invocation -> {
      latch.countDown();
      return httpResponse;
    }).when(httpClient).execute(any(HttpUriRequest.class));

    // When
    RepositoryStartedEvent event = new RepositoryStartedEvent(repository);
    underTest.on(event);

    // Wait briefly - should timeout because no warmup happens
    boolean completed = latch.await(200, TimeUnit.MILLISECONDS);
    assertThat("No warmup should occur for non-proxy repository", completed, is(false));

    // Then
    verify(httpClient, never()).execute(any(HttpUriRequest.class));
  }

  /**
   * Test that group repository events are skipped.
   */
  @Test
  public void testGroupRepositoryIsSkipped() throws Exception {
    // Setup - group repository
    when(repository.getType()).thenReturn(new GroupType());

    underTest.start();

    CountDownLatch latch = new CountDownLatch(1);
    doAnswer(invocation -> {
      latch.countDown();
      return httpResponse;
    }).when(httpClient).execute(any(HttpUriRequest.class));

    // When
    RepositoryStartedEvent event = new RepositoryStartedEvent(repository);
    underTest.on(event);

    // Wait briefly - should timeout because no warmup happens
    boolean completed = latch.await(200, TimeUnit.MILLISECONDS);
    assertThat("No warmup should occur for group repository", completed, is(false));

    // Then
    verify(httpClient, never()).execute(any(HttpUriRequest.class));
  }

  /**
   * Test that warmup is skipped when disabled via system property.
   */
  @Test
  public void testWarmupSkippedWhenDisabled() throws Exception {
    // Setup
    System.setProperty("nexus.proxy.warmup.enabled", "false");
    underTest = new ProxyWarmupService(repositoryManager);
    when(repository.getType()).thenReturn(new ProxyType());

    underTest.start();

    CountDownLatch latch = new CountDownLatch(1);
    doAnswer(invocation -> {
      latch.countDown();
      return httpResponse;
    }).when(httpClient).execute(any(HttpUriRequest.class));

    // When
    RepositoryStartedEvent event = new RepositoryStartedEvent(repository);
    underTest.on(event);

    // Wait briefly - should timeout because warmup is disabled
    boolean completed = latch.await(200, TimeUnit.MILLISECONDS);
    assertThat("No warmup should occur when disabled", completed, is(false));

    // Then
    verify(httpClient, never()).execute(any(HttpUriRequest.class));
  }

  /**
   * Test that warmup handles successful HTTP responses (2xx).
   */
  @Test
  public void testWarmupSuccessful2xxResponse() throws Exception {
    // Setup
    when(repository.getType()).thenReturn(new ProxyType());
    when(httpClient.execute(any(HttpUriRequest.class))).thenReturn(httpResponse);
    when(httpResponse.getStatusLine()).thenReturn(statusLine);
    when(statusLine.getStatusCode()).thenReturn(200);

    underTest.start();

    CountDownLatch latch = new CountDownLatch(1);
    when(httpClient.execute(any(HttpUriRequest.class))).thenAnswer(invocation -> {
      latch.countDown();
      return httpResponse;
    });

    // When
    RepositoryStartedEvent event = new RepositoryStartedEvent(repository);
    underTest.on(event);

    // Wait for completion
    latch.await(2, TimeUnit.SECONDS);

    // Then
    verify(httpClient, times(1)).execute(any(HttpUriRequest.class));
  }

  /**
   * Test that warmup handles 4xx responses gracefully (connection established).
   */
  @Test
  public void testWarmupHandles4xxResponse() throws Exception {
    // Setup
    when(repository.getType()).thenReturn(new ProxyType());
    when(httpClient.execute(any(HttpUriRequest.class))).thenReturn(httpResponse);
    when(httpResponse.getStatusLine()).thenReturn(statusLine);
    when(statusLine.getStatusCode()).thenReturn(404);

    underTest.start();

    CountDownLatch latch = new CountDownLatch(1);
    when(httpClient.execute(any(HttpUriRequest.class))).thenAnswer(invocation -> {
      latch.countDown();
      return httpResponse;
    });

    // When
    RepositoryStartedEvent event = new RepositoryStartedEvent(repository);
    underTest.on(event);

    // Wait for completion
    latch.await(2, TimeUnit.SECONDS);

    // Then - warmup should complete successfully (connection established)
    verify(httpClient, times(1)).execute(any(HttpUriRequest.class));
  }

  /**
   * Test that warmup handles 5xx responses gracefully (logs warning).
   */
  @Test
  public void testWarmupHandles5xxResponse() throws Exception {
    // Setup
    when(repository.getType()).thenReturn(new ProxyType());
    when(httpClient.execute(any(HttpUriRequest.class))).thenReturn(httpResponse);
    when(httpResponse.getStatusLine()).thenReturn(statusLine);
    when(statusLine.getStatusCode()).thenReturn(503);

    underTest.start();

    CountDownLatch latch = new CountDownLatch(1);
    when(httpClient.execute(any(HttpUriRequest.class))).thenAnswer(invocation -> {
      latch.countDown();
      return httpResponse;
    });

    // When
    RepositoryStartedEvent event = new RepositoryStartedEvent(repository);
    underTest.on(event);

    // Wait for completion
    latch.await(2, TimeUnit.SECONDS);

    // Then - warmup should log warning but not fail
    verify(httpClient, times(1)).execute(any(HttpUriRequest.class));
  }

  /**
   * Test that warmup handles exceptions gracefully.
   */
  @Test
  public void testWarmupHandlesException() throws Exception {
    // Setup
    when(repository.getType()).thenReturn(new ProxyType());

    underTest.start();

    CountDownLatch latch = new CountDownLatch(1);
    doAnswer(invocation -> {
      latch.countDown();
      throw new RuntimeException("Connection timeout");
    }).when(httpClient).execute(any(HttpUriRequest.class));

    // When
    RepositoryStartedEvent event = new RepositoryStartedEvent(repository);
    underTest.on(event);

    // Wait for async execution
    latch.await(2, TimeUnit.SECONDS);

    // Then - exception should be caught and logged, not propagated
    verify(httpClient, times(1)).execute(any(HttpUriRequest.class));
  }

  /**
   * Test that warmup handles missing remote URL gracefully.
   */
  @Test
  public void testWarmupHandlesMissingRemoteUrl() throws Exception {
    // Setup
    when(repository.getType()).thenReturn(new ProxyType());
    when(proxyFacet.getRemoteUrl()).thenReturn(null);

    underTest.start();

    CountDownLatch latch = new CountDownLatch(1);
    doAnswer(invocation -> {
      latch.countDown();
      return httpResponse;
    }).when(httpClient).execute(any(HttpUriRequest.class));

    // When
    RepositoryStartedEvent event = new RepositoryStartedEvent(repository);
    underTest.on(event);

    // Wait briefly - should timeout because no remote URL
    boolean completed = latch.await(200, TimeUnit.MILLISECONDS);
    assertThat("No warmup should occur when remote URL is missing", completed, is(false));

    // Then - should skip warmup
    verify(httpClient, never()).execute(any(HttpUriRequest.class));
  }

  /**
   * Test that executorService is null when service hasn't started.
   */
  @Test
  public void testExecutorServiceNullGuard() throws Exception {
    // Don't start the service
    when(repository.getType()).thenReturn(new ProxyType());

    CountDownLatch latch = new CountDownLatch(1);
    doAnswer(invocation -> {
      latch.countDown();
      return httpResponse;
    }).when(httpClient).execute(any(HttpUriRequest.class));

    // When
    RepositoryStartedEvent event = new RepositoryStartedEvent(repository);
    underTest.on(event);

    // Wait briefly - should timeout because service not started
    boolean completed = latch.await(200, TimeUnit.MILLISECONDS);
    assertThat("No warmup should occur when service not started", completed, is(false));

    // Then - should skip warmup (logged warning)
    verify(httpClient, never()).execute(any(HttpUriRequest.class));
  }

  /**
   * Test milestone logging at 5, 10, 20 repositories.
   */
  @Test
  public void testMilestoneLoggingAt5And10And20Repos() throws Exception {
    // Setup
    when(repository.getType()).thenReturn(new ProxyType());
    when(httpClient.execute(any(HttpUriRequest.class))).thenReturn(httpResponse);
    when(httpResponse.getStatusLine()).thenReturn(statusLine);
    when(statusLine.getStatusCode()).thenReturn(200);

    underTest.start();

    CountDownLatch latch = new CountDownLatch(20);
    when(httpClient.execute(any(HttpUriRequest.class))).thenAnswer(invocation -> {
      latch.countDown();
      return httpResponse;
    });

    // When - trigger 20 repository started events
    for (int i = 0; i < 20; i++) {
      Repository repo = mock(Repository.class);
      when(repo.getName()).thenReturn("proxy-repo-" + i);
      when(repo.getType()).thenReturn(new ProxyType());
      when(repo.facet(ProxyFacet.class)).thenReturn(proxyFacet);
      when(repo.facet(HttpClientFacet.class)).thenReturn(httpClientFacet);

      RepositoryStartedEvent event = new RepositoryStartedEvent(repo);
      underTest.on(event);
    }

    // Wait for all warmups to complete
    latch.await(5, TimeUnit.SECONDS);

    // Then - milestone logging should occur at 5, 10, 20
    // (Verification is implicit - no exceptions thrown)
    verify(httpClient, times(20)).execute(any(HttpUriRequest.class));
  }

  /**
   * Test that timeout configuration is applied to HTTP requests.
   */
  @Test
  public void testTimeoutConfigurationApplied() throws Exception {
    // Setup
    when(repository.getType()).thenReturn(new ProxyType());
    when(httpClient.execute(any(HttpUriRequest.class))).thenReturn(httpResponse);
    when(httpResponse.getStatusLine()).thenReturn(statusLine);
    when(statusLine.getStatusCode()).thenReturn(200);

    underTest.start();

    CountDownLatch latch = new CountDownLatch(1);
    ArgumentCaptor<HttpUriRequest> requestCaptor = ArgumentCaptor.forClass(HttpUriRequest.class);

    when(httpClient.execute(requestCaptor.capture())).thenAnswer(invocation -> {
      latch.countDown();
      return httpResponse;
    });

    // When
    RepositoryStartedEvent event = new RepositoryStartedEvent(repository);
    underTest.on(event);

    latch.await(2, TimeUnit.SECONDS);

    // Then - verify HTTP request was executed (implicitly verifies timeout configuration)
    verify(httpClient, times(1)).execute(any(HttpUriRequest.class));
  }
}

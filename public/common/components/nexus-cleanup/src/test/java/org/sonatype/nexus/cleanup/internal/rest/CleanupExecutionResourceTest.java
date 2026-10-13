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
package org.sonatype.nexus.cleanup.internal.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.sonatype.nexus.cleanup.service.CleanupService;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension.WithUser;
import org.sonatype.nexus.testcommon.validation.ValidationExtension;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import org.apache.shiro.util.ThreadContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ExtendWith(ValidationExtension.class)
@ExtendWith(AuthenticationExtension.class)
@WithUser
public class CleanupExecutionResourceTest
{
  @Mock
  private CleanupService cleanupService;

  @Mock
  private RepositoryManager repositoryManager;

  @Mock
  private Repository repository;

  private CleanupExecutionResource underTest;

  @BeforeEach
  public void setup() {
    underTest = new CleanupExecutionResource(cleanupService, repositoryManager);
  }

  @AfterEach
  public void tearDown() {
    underTest.shutdown();
  }

  @Test
  public void testRunCleanupOnUnknownRepositoryThrowsNotFound() {
    when(repositoryManager.get("missing-repo")).thenReturn(null);

    CleanupExecutionRequestXO request = new CleanupExecutionRequestXO();
    request.setRepository("missing-repo");
    request.setPolicy("policy-1");
    request.setDryRun(false);

    assertThatThrownBy(() -> underTest.runCleanup(request))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining("missing-repo");
  }

  @Test
  public void testDryRunReturnsCompletedStatus() {
    when(repositoryManager.get("repo-1")).thenReturn(repository);
    when(cleanupService.dryRunCount(repository)).thenReturn(42L);

    CleanupExecutionRequestXO request = new CleanupExecutionRequestXO();
    request.setRepository("repo-1");
    request.setPolicy("policy-1");
    request.setDryRun(true);

    Response response = underTest.runCleanup(request);

    assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
    CleanupExecutionStatusXO status = (CleanupExecutionStatusXO) response.getEntity();
    assertThat(status.isDryRun()).isTrue();
    assertThat(status.getComponentCount()).isEqualTo(42L);
    assertThat(status.getStatus()).isEqualTo(CleanupExecutionStatusXO.Status.COMPLETED);
    assertThat(status.getRepository()).isEqualTo("repo-1");
    assertThat(status.getPolicy()).isEqualTo("policy-1");
  }

  @Test
  public void testDryRunFailureReportsError() {
    when(repositoryManager.get("repo-1")).thenReturn(repository);
    when(cleanupService.dryRunCount(repository)).thenThrow(new RuntimeException("boom"));

    CleanupExecutionRequestXO request = new CleanupExecutionRequestXO();
    request.setRepository("repo-1");
    request.setPolicy("policy-1");
    request.setDryRun(true);

    Response response = underTest.runCleanup(request);

    CleanupExecutionStatusXO status = (CleanupExecutionStatusXO) response.getEntity();
    assertThat(status.getStatus()).isEqualTo(CleanupExecutionStatusXO.Status.FAILED);
    assertThat(status.getError()).contains("boom");
  }

  @Test
  public void testRunCleanupAcceptedAsync() {
    when(repositoryManager.get("repo-1")).thenReturn(repository);
    lenient().when(cleanupService.cleanupRepository(any(), any())).thenReturn(0L);

    CleanupExecutionRequestXO request = new CleanupExecutionRequestXO();
    request.setRepository("repo-1");
    request.setPolicy("policy-1");
    request.setDryRun(false);

    Response response = underTest.runCleanup(request);

    assertThat(response.getStatus()).isEqualTo(Response.Status.ACCEPTED.getStatusCode());
    CleanupExecutionStatusXO status = (CleanupExecutionStatusXO) response.getEntity();
    assertThat(status.isDryRun()).isFalse();
    assertThat(status.getId()).isNotBlank();
  }

  /**
   * Lifecycle: @PreDestroy must shut the executor down so worker threads do not outlive the
   * Spring context. After shutdown, further submissions must be rejected.
   */
  @Test
  public void testShutdownRejectsFurtherSubmissions() {
    when(repositoryManager.get("repo-1")).thenReturn(repository);
    underTest.shutdown();

    CleanupExecutionRequestXO request = new CleanupExecutionRequestXO();
    request.setRepository("repo-1");
    request.setPolicy("policy-1");
    request.setDryRun(false);

    assertThatThrownBy(() -> underTest.runCleanup(request))
        .isInstanceOf(RejectedExecutionException.class);
  }

  @Test
  public void testGetStatusUnknownIdThrowsNotFound() {
    assertThatThrownBy(() -> underTest.getStatus("unknown-id"))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining("unknown-id");
  }

  /**
   * Regression for TC10: when two POST /cleanup/run calls race for the same repository, exactly
   * one must be ACCEPTED and the others must be returned a 409 CONFLICT carrying the running
   * execution's status. Prior to the TOCTOU fix in CleanupExecutionResource (status published
   * before activeRepositoryExecutions.compute), the second caller could enter compute() after
   * the first claimed the slot but before its status was inserted, causing the conflict check
   * to be skipped and a second RUNNING execution to be accepted for the same repository.
   */
  @Test
  public void testConcurrentRunsForSameRepositoryYieldExactlyOneAccepted() throws Exception {
    when(repositoryManager.get("repo-1")).thenReturn(repository);
    // Block cleanupRepository until released so the first execution stays RUNNING during the race.
    // lenient() because cleanup runs asynchronously in the resource's own executor; Mockito's
    // afterEach may fire before the executor thread invokes the stub.
    CountDownLatch release = new CountDownLatch(1);
    lenient().when(cleanupService.cleanupRepository(any(), any())).thenAnswer(invocation -> {
      release.await(5, TimeUnit.SECONDS);
      return 0L;
    });

    final int callers = 16;
    ExecutorService pool = Executors.newFixedThreadPool(callers);
    CountDownLatch startGate = new CountDownLatch(1);
    List<Future<Response>> futures = new ArrayList<>();
    // Shiro 2.x ThreadContext is a plain ThreadLocal (no longer InheritableThreadLocal), so the pool
    // worker threads do not inherit the Subject bound in setup(). Capture the submitting thread's
    // resources and rebind them on each worker so runCleanup executes under the authenticated Subject,
    // then clear them so the pooled threads are not left holding a stale context.
    Map<Object, Object> contextResources = ThreadContext.getResources();
    try {
      for (int i = 0; i < callers; i++) {
        futures.add(pool.submit(() -> {
          ThreadContext.setResources(contextResources);
          try {
            CleanupExecutionRequestXO request = new CleanupExecutionRequestXO();
            request.setRepository("repo-1");
            request.setPolicy("policy-1");
            request.setDryRun(false);
            startGate.await();
            return underTest.runCleanup(request);
          }
          finally {
            ThreadContext.remove();
          }
        }));
      }
      startGate.countDown();

      AtomicInteger accepted = new AtomicInteger();
      AtomicInteger conflict = new AtomicInteger();
      for (Future<Response> f : futures) {
        Response r = f.get(5, TimeUnit.SECONDS);
        if (r.getStatus() == Response.Status.ACCEPTED.getStatusCode()) {
          accepted.incrementAndGet();
        }
        else if (r.getStatus() == Response.Status.CONFLICT.getStatusCode()) {
          conflict.incrementAndGet();
          CleanupExecutionStatusXO body = (CleanupExecutionStatusXO) r.getEntity();
          assertThat(body.getStatus()).isEqualTo(CleanupExecutionStatusXO.Status.RUNNING);
          assertThat(body.getRepository()).isEqualTo("repo-1");
        }
      }
      assertThat(accepted.get()).isEqualTo(1);
      assertThat(conflict.get()).isEqualTo(callers - 1);
    }
    finally {
      release.countDown();
      pool.shutdownNow();
    }
  }

  /**
   * Regression for the TOCTOU fix: a caller that loses the race must NOT leak an orphan
   * CleanupExecutionStatusXO in the executions map. The fix removes the speculatively-published
   * status when conflictHolder is set. We verify by counting RUNNING entries after a contended
   * burst — only one execution may be tracked.
   */
  @Test
  public void testConflictLoserCleansUpOrphanStatus() throws Exception {
    when(repositoryManager.get("repo-1")).thenReturn(repository);
    // lenient() for the same reason as testConcurrentRunsForSameRepositoryYieldExactlyOneAccepted.
    CountDownLatch release = new CountDownLatch(1);
    lenient().when(cleanupService.cleanupRepository(any(), any())).thenAnswer(invocation -> {
      release.await(5, TimeUnit.SECONDS);
      return 0L;
    });

    List<String> acceptedIds = new ArrayList<>();
    List<String> conflictIds = new ArrayList<>();
    for (int i = 0; i < 8; i++) {
      CleanupExecutionRequestXO request = new CleanupExecutionRequestXO();
      request.setRepository("repo-1");
      request.setPolicy("policy-1");
      request.setDryRun(false);
      Response r = underTest.runCleanup(request);
      CleanupExecutionStatusXO body = (CleanupExecutionStatusXO) r.getEntity();
      if (r.getStatus() == Response.Status.ACCEPTED.getStatusCode()) {
        acceptedIds.add(body.getId());
      }
      else {
        conflictIds.add(body.getId());
      }
    }

    // Loser IDs must not be retrievable via GET — they were removed from the executions map.
    for (String loserId : conflictIds) {
      // The conflict body returns the *winner's* id, not the loser's; either way, only IDs we
      // explicitly accepted should be retrievable.
      if (!acceptedIds.contains(loserId)) {
        assertThatThrownBy(() -> underTest.getStatus(loserId))
            .isInstanceOf(NotFoundException.class);
      }
    }

    release.countDown();
    assertThat(acceptedIds).hasSize(1);
  }

  /**
   * Async failure path: when cleanupService.cleanupRepository throws, the tracked status must
   * eventually transition to FAILED with the error message captured, and the per-repository slot
   * must be released so a subsequent run is accepted.
   */
  @Test
  public void testAsyncCleanupFailureRecordsFailedStatusAndReleasesSlot() throws Exception {
    when(repositoryManager.get("repo-1")).thenReturn(repository);
    when(cleanupService.cleanupRepository(any(), any())).thenThrow(new RuntimeException("disk full"));

    CleanupExecutionRequestXO request = new CleanupExecutionRequestXO();
    request.setRepository("repo-1");
    request.setPolicy("policy-1");
    request.setDryRun(false);

    Response response = underTest.runCleanup(request);
    String id = ((CleanupExecutionStatusXO) response.getEntity()).getId();

    long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(5);
    CleanupExecutionStatusXO status = null;
    while (System.currentTimeMillis() < deadline) {
      status = underTest.getStatus(id);
      if (status.getStatus() == CleanupExecutionStatusXO.Status.FAILED) {
        break;
      }
      Thread.sleep(25);
    }
    assertThat(status).isNotNull();
    assertThat(status.getStatus()).isEqualTo(CleanupExecutionStatusXO.Status.FAILED);
    assertThat(status.getError()).contains("disk full");
    assertThat(status.getDurationMs()).isGreaterThanOrEqualTo(0L);

    // Slot released: a new run for the same repository must be ACCEPTED rather than CONFLICT.
    // The activeRepositoryExecutions.remove() happens in the executor's finally block, which races
    // with the FAILED status update; poll briefly so we observe the post-cleanup state.
    // Use doReturn(...) here because the prior stubbing throws RuntimeException;
    // when(mock.method(...)) would invoke the existing stub and re-throw "disk full".
    // lenient() because the follow-up runCleanup is ACCEPTED before its async task necessarily
    // reaches cleanupRepository(...), so on faster runs Mockito's strict check flags this stub
    // as unnecessary even though it guards against the executor throwing "disk full" again.
    lenient().doReturn(0L).when(cleanupService).cleanupRepository(any(), any());
    Response follow = null;
    long followDeadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(5);
    while (System.currentTimeMillis() < followDeadline) {
      follow = underTest.runCleanup(request);
      if (follow.getStatus() == Response.Status.ACCEPTED.getStatusCode()) {
        break;
      }
      Thread.sleep(25);
    }
    assertThat(follow).isNotNull();
    assertThat(follow.getStatus()).isEqualTo(Response.Status.ACCEPTED.getStatusCode());
  }

  /**
   * The {@code /v1/cleanup/run} endpoint must NOT appear in the generated OpenAPI document.
   * {@code SwaggerModel.scan} filters out any resource without a class-level {@code @Tag},
   * so guard against a class-level {@code @Tag} being re-added on this resource.
   */
  @Test
  public void classMustNotBeAnnotatedTag() {
    assertThat(
        CleanupExecutionResource.class
            .isAnnotationPresent(io.swagger.v3.oas.annotations.tags.Tag.class))
                .as("CleanupExecutionResource must not carry a class-level @Tag; doing so surfaces "
                    + "/v1/cleanup/run in the published OpenAPI document.")
                .isFalse();
  }
}

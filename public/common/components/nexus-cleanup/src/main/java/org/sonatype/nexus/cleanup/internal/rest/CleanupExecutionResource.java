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

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.PreDestroy;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.sonatype.nexus.cleanup.service.CleanupService;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.rest.Resource;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import static com.google.common.base.Preconditions.checkNotNull;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.sonatype.nexus.cleanup.internal.rest.CleanupExecutionResource.RESOURCE_URI;
import static org.sonatype.nexus.rest.APIConstants.V1_API_PREFIX;

/**
 * REST resource for executing cleanup operations on repositories.
 */
@Component
@Tag(name = "Cleanup policies")
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path(RESOURCE_URI)
public class CleanupExecutionResource
    implements Resource
{
  protected static final String RESOURCE_URI = V1_API_PREFIX + "/cleanup/run";

  private static final Logger log = LoggerFactory.getLogger(CleanupExecutionResource.class);

  private final CleanupService cleanupService;

  private final RepositoryManager repositoryManager;

  private final ExecutorService executorService = Executors.newCachedThreadPool();

  /**
   * Bounded, time-expiring store of execution statuses. Terminal entries (COMPLETED/FAILED) need
   * to remain reachable via GET /cleanup/run/{id} long enough for clients to poll, but must not
   * accumulate forever on a long-running server. 24h TTL gives pollers ample time; max size of
   * 500 caps memory in burst scenarios.
   */
  private final Cache<String, CleanupExecutionStatusXO> executions = CacheBuilder.newBuilder()
      .maximumSize(500)
      .expireAfterWrite(24, TimeUnit.HOURS)
      .build();

  private final ConcurrentMap<String, String> activeRepositoryExecutions = new ConcurrentHashMap<>();

  @Autowired
  public CleanupExecutionResource(
      final CleanupService cleanupService,
      final RepositoryManager repositoryManager)
  {
    this.cleanupService = checkNotNull(cleanupService);
    this.repositoryManager = checkNotNull(repositoryManager);
  }

  @POST
  @RequiresAuthentication
  @RequiresPermissions("nexus:*")
  @Operation(summary = "Run cleanup on a repository (dry run or async execution)",
      description = "Runs cleanup on the specified `repository` against the policies attached to it. "
          + "Behavior depends on the `dryRun` flag in the request body:\n\n"
          + "- `dryRun: true` — Synchronously evaluates the policies and returns **200 OK** with a "
          + "`CleanupExecutionStatusXO` whose `componentCount` is the number of components that "
          + "*would* be deleted. No components are removed.\n"
          + "- `dryRun: false` — Schedules an asynchronous deletion and returns **202 Accepted** "
          + "immediately with a `CleanupExecutionStatusXO` containing the generated execution `id` "
          + "and `status=RUNNING`. Poll `GET /service/rest/v1/cleanup/run/{id}` to observe the "
          + "terminal `COMPLETED` or `FAILED` state and the final `componentsDeleted` count. "
          + "Execution records are retained for 24 hours.\n\n"
          + "At most one non-dry-run cleanup may be `RUNNING` per repository. A second non-dry-run "
          + "submitted while one is in progress is rejected with **409 Conflict** and the body of "
          + "the existing execution's status.")
  @ApiResponses({
      @ApiResponse(responseCode = "200",
          description = "Dry run completed; `componentCount` reports how many components match",
          content = @Content(schema = @Schema(implementation = CleanupExecutionStatusXO.class))),
      @ApiResponse(responseCode = "202",
          description = "Async cleanup accepted; use the returned `id` with GET /cleanup/run/{id} to poll for status",
          content = @Content(schema = @Schema(implementation = CleanupExecutionStatusXO.class))),
      @ApiResponse(responseCode = "400", description = "Invalid request body (missing `repository`)"),
      @ApiResponse(responseCode = "401", description = "Authentication required"),
      @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
      @ApiResponse(responseCode = "404", description = "The specified repository does not exist"),
      @ApiResponse(responseCode = "409",
          description = "A non-dry-run cleanup is already RUNNING for this repository; "
              + "the response body is the existing execution's status",
          content = @Content(schema = @Schema(implementation = CleanupExecutionStatusXO.class)))
  })
  public Response runCleanup(@Valid final CleanupExecutionRequestXO request) {
    String repositoryName = request.getRepository();
    Repository repository = repositoryManager.get(repositoryName);

    if (repository == null) {
      throw new NotFoundException("Repository '" + repositoryName + "' not found.");
    }

    if (request.isDryRun()) {
      return handleDryRun(repository, request);
    }

    // Publish the status into the executions map BEFORE attempting to claim the per-repository
    // slot. This closes a TOCTOU race where a concurrent caller could enter the compute() block
    // after another caller had claimed the slot but before that caller had inserted its status,
    // causing executions.get(existingId) to return null and the conflict check to be skipped.
    final String executionId = UUID.randomUUID().toString();
    CleanupExecutionStatusXO status = new CleanupExecutionStatusXO();
    status.setId(executionId);
    status.setRepository(repositoryName);
    status.setPolicy(request.getPolicy());
    status.setStatus(CleanupExecutionStatusXO.Status.RUNNING);
    status.setDryRun(false);
    executions.put(executionId, status);

    final CleanupExecutionStatusXO[] conflictHolder = new CleanupExecutionStatusXO[1];
    activeRepositoryExecutions.compute(repositoryName, (key, existingId) -> {
      if (existingId != null) {
        CleanupExecutionStatusXO existing = executions.getIfPresent(existingId);
        if (existing != null && CleanupExecutionStatusXO.Status.RUNNING == existing.getStatus()) {
          conflictHolder[0] = existing;
          return existingId;
        }
      }
      return executionId;
    });

    if (conflictHolder[0] != null) {
      // We lost the race; remove the orphaned status we speculatively published.
      executions.invalidate(executionId);
      return Response.status(Status.CONFLICT)
          .entity(conflictHolder[0])
          .build();
    }

    executorService.submit(() -> {
      long startTime = System.currentTimeMillis();
      try {
        long deleted = cleanupService.cleanupRepository(repository, () -> false);
        status.setComponentsDeleted(deleted);
        status.setStatus(CleanupExecutionStatusXO.Status.COMPLETED);
      }
      catch (Exception e) {
        log.error("Cleanup execution failed for repository '{}'", repositoryName, e);
        status.setStatus(CleanupExecutionStatusXO.Status.FAILED);
        status.setError(e.getMessage());
      }
      finally {
        status.setDurationMs(System.currentTimeMillis() - startTime);
        activeRepositoryExecutions.remove(repositoryName);
      }
    });

    return Response.accepted(status).build();
  }

  /**
   * Shut down the executor when the bean is destroyed (server restart / context teardown) so that
   * worker threads do not outlive the Spring context. {@code shutdownNow()} is used so an orderly
   * restart is not blocked by long-running cleanup tasks; the cancellation check passed to
   * {@code cleanupRepository(...)} would otherwise need to honor an interrupt signal.
   */
  @PreDestroy
  public void shutdown() {
    executorService.shutdownNow();
  }

  @GET
  @Path("{id}")
  @RequiresAuthentication
  @RequiresPermissions("nexus:*")
  @Operation(hidden = true)
  public CleanupExecutionStatusXO getStatus(@PathParam("id") final String executionId) {
    CleanupExecutionStatusXO status = executions.getIfPresent(executionId);
    if (status == null) {
      throw new NotFoundException("Execution '" + executionId + "' not found.");
    }
    return status;
  }

  private Response handleDryRun(final Repository repository, final CleanupExecutionRequestXO request) {
    long startTime = System.currentTimeMillis();
    CleanupExecutionStatusXO status = new CleanupExecutionStatusXO();
    status.setId(UUID.randomUUID().toString());
    status.setRepository(request.getRepository());
    status.setPolicy(request.getPolicy());
    status.setDryRun(true);

    try {
      long count = cleanupService.dryRunCount(repository);
      status.setComponentCount(count);
      status.setStatus(CleanupExecutionStatusXO.Status.COMPLETED);
    }
    catch (Exception e) {
      log.error("Dry run failed for repository '{}'", request.getRepository(), e);
      status.setStatus(CleanupExecutionStatusXO.Status.FAILED);
      status.setError(e.getMessage());
    }
    finally {
      status.setDurationMs(System.currentTimeMillis() - startTime);
    }

    return Response.ok(status).build();
  }
}

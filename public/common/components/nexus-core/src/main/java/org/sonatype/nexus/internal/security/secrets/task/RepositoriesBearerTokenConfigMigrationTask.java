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
package org.sonatype.nexus.internal.security.secrets.task;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import javax.annotation.Nullable;

import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.text.Strings2;
import org.sonatype.nexus.kv.KeyValueStore;
import org.sonatype.nexus.logging.task.TaskLogType;
import org.sonatype.nexus.logging.task.TaskLogging;
import org.sonatype.nexus.node.datastore.NodeHeartbeatManager;
import org.sonatype.nexus.node.upgrade.MigrationTaskSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.types.ProxyType;
import org.sonatype.nexus.scheduling.CancelableHelper;

import org.apache.commons.lang3.stream.Streams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import static java.util.Objects.requireNonNull;
import static org.sonatype.nexus.repository.manager.internal.HttpAuthenticationSecretEncoder.BEARER_TOKEN_MIGRATION_STARTED;

/**
 * Task to migrate repository configurations from old bearer token key to new bearerTokenId key.
 */
@Component
@TaskLogging(TaskLogType.TASK_LOG_ONLY)
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class RepositoriesBearerTokenConfigMigrationTask
    extends MigrationTaskSupport
{

  private static final String OLD_BEARER_TOKEN_KEY = "bearerToken";

  private static final String NEW_BEARER_TOKEN_KEY = "bearerTokenId";

  private static final String HTTPCLIENT_KEY = "httpclient";

  private static final String AUTHENTICATION_KEY = "authentication";

  public static final String TIMEOUT_REACHED_MESSAGE =
      "Bearer token configuration migration failed: timeout of %d minutes reached while waiting for old nodes to stop";

  private final RepositoryManager repositoryManager;

  private final KeyValueStore kv;

  private final Duration waitTimeout;

  private final Duration checkInterval;

  @Autowired
  public RepositoriesBearerTokenConfigMigrationTask(
      @Nullable final NodeHeartbeatManager nodeHeartbeatManager,
      final RepositoryManager repositoryManager,
      final KeyValueStore kv,
      @Value("${nexus.bearer.token.migration.maxWaitTimeMinutes:15m}") final Duration waitTimeout,
      @Value("${nexus.bearer.token.migration.pollIntervalSeconds:30s}") final Duration checkInterval)
  {
    super(nodeHeartbeatManager);
    this.repositoryManager = requireNonNull(repositoryManager);
    this.kv = requireNonNull(kv);
    this.waitTimeout = requireNonNull(waitTimeout);
    this.checkInterval = requireNonNull(checkInterval);
  }

  @Override
  public String getMessage() {
    return "Migrate repository bearer token configurations to use new key format";
  }

  @Override
  protected Object execute() throws Exception {
    waitForOldNodesToStop();
    return migrateRepositoryConfigs();
  }

  private void waitForOldNodesToStop() throws InterruptedException {
    Instant startTime = Instant.now();
    Instant deadline = startTime.plus(waitTimeout);

    log.info("Checking for old nodes before running bearer token configuration migration");

    while (oldNodesRunning()) {
      CancelableHelper.checkCancellation();

      if (Instant.now().isAfter(deadline)) {
        String errorMsg = TIMEOUT_REACHED_MESSAGE.formatted(waitTimeout.toMinutes());
        throw new IllegalStateException(errorMsg);
      }

      Duration elapsed = Duration.between(startTime, Instant.now());
      Duration remaining = Duration.between(Instant.now(), deadline);

      log.info("Old nodes still running, waiting {} (elapsed: {}, remaining: {})", formatDuration(checkInterval),
          formatDuration(elapsed), formatDuration(remaining));

      Thread.sleep(checkInterval.toMillis());
    }

    log.info("No old nodes detected, proceeding with bearer token configuration migration");
  }

  private Object migrateRepositoryConfigs() {
    log.info("Starting bearer token configuration migration for repositories");

    kv.setBoolean(BEARER_TOKEN_MIGRATION_STARTED, true);

    boolean hasFailures = Streams.of(repositoryManager.browse())
        .filter(repository -> repository.getType() instanceof ProxyType)
        .map(Repository::getConfiguration)
        .map(Configuration::copy)
        .filter(this::migrateRepositoryConfig)
        .map(this::updateRepository)
        .anyMatch(result -> !result.success());

    if (hasFailures) {
      throw new IllegalStateException("Bearer token configuration migration failed for one or more repositories");
    }

    log.info("Finished bearer token configuration migration");
    return null;
  }

  private MigrationResult updateRepository(final Configuration config) {
    CancelableHelper.checkCancellation();
    String repositoryName = config.getRepositoryName();
    try {
      repositoryManager.update(config);
      log.info("Migrated bearer token configuration for repository: {}", repositoryName);
      return MigrationResult.ofSuccess();
    }
    catch (Exception e) {
      log.error("Failed to migrate bearer token configuration for repository: {}", repositoryName, e);
      return MigrationResult.ofFailure();
    }
  }

  private record MigrationResult(boolean success)
  {
    static MigrationResult ofSuccess() {
      return new MigrationResult(true);
    }

    static MigrationResult ofFailure() {
      return new MigrationResult(false);
    }
  }

  private boolean migrateRepositoryConfig(final Configuration config) {
    NestedAttributesMap httpClientAttrs = config.attributes(HTTPCLIENT_KEY);
    if (httpClientAttrs == null || httpClientAttrs.isEmpty()) {
      return false;
    }

    NestedAttributesMap authAttrs = httpClientAttrs.child(AUTHENTICATION_KEY);
    if (authAttrs == null || authAttrs.isEmpty()) {
      return false;
    }

    Map<String, Object> authMap = authAttrs.backing();

    // Check if old key exists
    if (!authMap.containsKey(OLD_BEARER_TOKEN_KEY)) {
      return false;
    }

    String oldValue = authMap.get(OLD_BEARER_TOKEN_KEY).toString();
    log.debug("Found old bearer token key in repository: {}, migrating to new key", config.getRepositoryName());

    authMap.remove(OLD_BEARER_TOKEN_KEY);

    // If the new key doesn't exist, and we have a non-blank value from the old key, migrate it
    if (!authMap.containsKey(NEW_BEARER_TOKEN_KEY) && !Strings2.isEmpty(oldValue)) {
      authMap.put(NEW_BEARER_TOKEN_KEY, oldValue);
      log.debug("Migrated bearer token value to new key for repository: {}", config.getRepositoryName());
    }
    else {
      log.debug("Removed old bearer token key for repository: {}", config.getRepositoryName());
    }

    return true;
  }

  private static String formatDuration(Duration duration) {
    long min = duration.toMinutes();
    long sec = duration.toSecondsPart();
    return min > 0 ? "%dm %ds".formatted(min, sec) : "%ds".formatted(sec);
  }
}

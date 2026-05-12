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
package org.sonatype.nexus.repository.manager.internal;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Tracks repositories that failed to initialize or start during Nexus startup.
 * This allows the UI to display failed repositories and enables administrators
 * to fix configuration issues without direct database access.
 *
 * Thread-safe: uses ConcurrentHashMap for all operations.
 */
@Named
@Singleton
public class FailedRepositoryTracker
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  private final Map<String, RepositoryFailure> failures = new ConcurrentHashMap<>();

  /**
   * Records a repository initialization/startup failure.
   *
   * @param repositoryName the name of the failed repository
   * @param cause the exception that caused the failure
   */
  public void recordFailure(final String repositoryName, final Exception cause) {
    checkNotNull(repositoryName);
    checkNotNull(cause);

    String lcName = repositoryName.toLowerCase();
    String reason = cause.getMessage() != null ? cause.getMessage() : cause.getClass().getSimpleName();

    RepositoryFailure failure = new RepositoryFailure(repositoryName, reason, Instant.now());
    failures.put(lcName, failure);

    log.warn("Recorded failure for repository '{}': {}", repositoryName, reason);
  }

  /**
   * Clears a recorded failure, typically after successful recovery.
   *
   * @param repositoryName the name of the repository
   */
  public void clearFailure(final String repositoryName) {
    checkNotNull(repositoryName);

    String lcName = repositoryName.toLowerCase();
    RepositoryFailure removed = failures.remove(lcName);

    if (removed != null) {
      log.info("Cleared failure record for repository '{}'", repositoryName);
    }
  }

  /**
   * Gets the failure record for a repository, if any.
   *
   * @param repositoryName the name of the repository
   * @return the failure record, or empty if the repository hasn't failed
   */
  public Optional<RepositoryFailure> getFailure(final String repositoryName) {
    checkNotNull(repositoryName);
    return Optional.ofNullable(failures.get(repositoryName.toLowerCase()));
  }

  /**
   * Checks if a repository has a recorded failure.
   *
   * @param repositoryName the name of the repository
   * @return true if the repository has failed
   */
  public boolean hasFailed(final String repositoryName) {
    checkNotNull(repositoryName);
    return failures.containsKey(repositoryName.toLowerCase());
  }

  /**
   * Gets all recorded failures.
   *
   * @return unmodifiable collection of all failures
   */
  public Collection<RepositoryFailure> getAllFailures() {
    return Collections.unmodifiableCollection(failures.values());
  }

  /**
   * Gets the count of failed repositories.
   *
   * @return number of failed repositories
   */
  public int getFailureCount() {
    return failures.size();
  }

  /**
   * Record of a repository failure.
   */
  public static class RepositoryFailure
  {
    private final String name;

    private final String reason;

    private final Instant failedAt;

    public RepositoryFailure(final String name, final String reason, final Instant failedAt) {
      this.name = checkNotNull(name);
      this.reason = checkNotNull(reason);
      this.failedAt = checkNotNull(failedAt);
    }

    public String getName() {
      return name;
    }

    public String getReason() {
      return reason;
    }

    public Instant getFailedAt() {
      return failedAt;
    }

    @Override
    public String toString() {
      return "RepositoryFailure{" +
          "name='" + name + '\'' +
          ", reason='" + reason + '\'' +
          ", failedAt=" + failedAt +
          '}';
    }
  }
}

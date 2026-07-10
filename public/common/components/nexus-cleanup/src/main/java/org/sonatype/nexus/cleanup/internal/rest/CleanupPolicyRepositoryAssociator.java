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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response.Status;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.rest.ValidationErrorsException;
import org.sonatype.nexus.rest.WebApplicationMessageException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Objects.requireNonNull;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.sonatype.nexus.cleanup.internal.content.service.CleanupServiceImpl.CLEANUP_ATTRIBUTES_KEY;
import static org.sonatype.nexus.cleanup.internal.content.service.CleanupServiceImpl.CLEANUP_NAME_KEY;

/**
 * Shared helper for attaching and detaching cleanup policies to/from repositories.
 *
 * <p>
 * Centralizes the attribute-mutation logic so the internal REST API
 * ({@link CleanupPolicyResource}) and the Pro V1 API stay behaviorally identical.
 */
@Component
public class CleanupPolicyRepositoryAssociator
{
  private static final Logger log = LoggerFactory.getLogger(CleanupPolicyRepositoryAssociator.class);

  private final RepositoryManager repositoryManager;

  @Autowired
  public CleanupPolicyRepositoryAssociator(final RepositoryManager repositoryManager) {
    this.repositoryManager = requireNonNull(repositoryManager);
  }

  /**
   * Returns true if the repository currently has the named cleanup policy attached.
   */
  @SuppressWarnings("unchecked")
  public boolean repositoryHasPolicy(final Repository repository, final String policyName) {
    return Optional.ofNullable(repository.getConfiguration().getAttributes())
        .map(attrs -> attrs.get(CLEANUP_ATTRIBUTES_KEY))
        .map(cleanup -> (Collection<String>) cleanup.get(CLEANUP_NAME_KEY))
        .map(names -> names.contains(policyName))
        .orElse(false);
  }

  /**
   * Returns the names of repositories currently attached to {@code policyName} whose
   * format matches {@code policyFormat}.
   */
  public Set<String> getRepositoriesForPolicy(final String policyName, final String policyFormat) {
    Set<String> current = new HashSet<>();
    for (Repository repo : repositoryManager.browse()) {
      if (repositoryHasPolicy(repo, policyName) && policyFormat.equals(repo.getFormat().getValue())) {
        current.add(repo.getName());
      }
    }
    return current;
  }

  /**
   * Reconciles attachments so the final attached set equals {@code requested}.
   *
   * <p>
   * Validates that every requested repository exists and has format {@code policyFormat}
   * BEFORE making any change. If validation fails, no mutation occurs. If a mutation
   * fails partway through, already-applied changes are rolled back on a best-effort basis.
   *
   * @param policyName name of the cleanup policy
   * @param policyFormat format of the cleanup policy (used to reject mismatched repositories)
   * @param requested full desired set of attached repository names; {@code null} treated as empty
   */
  public void updateRepositoriesForPolicy(
      final String policyName,
      final String policyFormat,
      final Set<String> requested)
  {
    requireNonNull(policyName);
    requireNonNull(policyFormat);
    Set<String> req = requested == null ? new HashSet<>() : new HashSet<>(requested);

    // Up-front validation so we never half-apply. Validated repositories are cached so the
    // mutation loop below does not need to re-resolve them via repositoryManager.get(...).
    Map<String, Repository> validated = new HashMap<>();
    for (String repoName : req) {
      Repository repo = repositoryManager.get(repoName);
      if (repo == null) {
        throw new NotFoundException("Repository '" + repoName + "' not found.");
      }
      if (!policyFormat.equals(repo.getFormat().getValue())) {
        throw new ValidationErrorsException("repository",
            "Repository '" + repoName + "' format does not match policy format '" + policyFormat + "'.");
      }
      validated.put(repoName, repo);
    }

    Set<String> current = getRepositoriesForPolicy(policyName, policyFormat);

    Set<String> toAdd = new HashSet<>(req);
    toAdd.removeAll(current);

    Set<String> toRemove = new HashSet<>(current);
    toRemove.removeAll(req);

    Set<String> applied = new HashSet<>();
    Set<String> detached = new HashSet<>();
    try {
      for (String repoName : toAdd) {
        addPolicyToRepository(validated.get(repoName), policyName);
        applied.add(repoName);
      }
      for (String repoName : toRemove) {
        Repository repo = repositoryManager.get(repoName);
        if (repo != null) {
          removePolicyFromRepository(repo, policyName);
          detached.add(repoName);
        }
      }
    }
    catch (RuntimeException e) {
      // Best-effort rollback of in-flight mutations. Rollback failures imply that the repository
      // attribute state has diverged from the policy's intended state and may require manual
      // intervention; surface them as ERROR and attach as suppressed exceptions on the primary
      // failure so they are visible in stack traces and error reporters rather than only logs.
      for (String repoName : applied) {
        try {
          Repository repo = repositoryManager.get(repoName);
          if (repo != null) {
            removePolicyFromRepository(repo, policyName);
          }
        }
        catch (Exception inner) {
          e.addSuppressed(inner);
          log.error("Rollback: failed to detach policy '{}' from repository '{}'; manual intervention may be required",
              policyName, repoName, inner);
        }
      }
      for (String repoName : detached) {
        try {
          Repository repo = repositoryManager.get(repoName);
          if (repo != null) {
            addPolicyToRepository(repo, policyName);
          }
        }
        catch (Exception inner) {
          e.addSuppressed(inner);
          log.error("Rollback: failed to re-attach policy '{}' to repository '{}'; manual intervention may be required",
              policyName, repoName, inner);
        }
      }
      throw e;
    }
  }

  /**
   * Detaches the named policy from every repository that currently has it.
   * Best-effort: failures are logged but not rethrown. Intended as a cleanup
   * step when policy creation must be rolled back.
   */
  public void detachAll(final String policyName) {
    for (Repository repo : repositoryManager.browse()) {
      if (repositoryHasPolicy(repo, policyName)) {
        try {
          removePolicyFromRepository(repo, policyName);
        }
        catch (Exception e) {
          log.warn("Failed to detach policy '{}' from repository '{}' during rollback",
              policyName, repo.getName(), e);
        }
      }
    }
  }

  @SuppressWarnings("unchecked")
  private void addPolicyToRepository(final Repository repository, final String policyName) {
    Map<String, Map<String, Object>> attributes = repository.getConfiguration().getAttributes();
    Map<String, Object> cleanupAttr = attributes.computeIfAbsent(CLEANUP_ATTRIBUTES_KEY, k -> new HashMap<>());
    Collection<String> policyNames =
        (Collection<String>) cleanupAttr.computeIfAbsent(CLEANUP_NAME_KEY, k -> new ArrayList<>());
    if (!policyNames.contains(policyName)) {
      policyNames.add(policyName);
    }
    try {
      repositoryManager.update(repository.getConfiguration());
    }
    catch (Exception e) {
      log.error("Failed to update repository '{}'", repository.getName(), e);
      throw new WebApplicationMessageException(Status.INTERNAL_SERVER_ERROR,
          "Failed to update repository '" + repository.getName() + "'", APPLICATION_JSON);
    }
  }

  @SuppressWarnings("unchecked")
  private void removePolicyFromRepository(final Repository repository, final String policyName) {
    Map<String, Map<String, Object>> attributes = repository.getConfiguration().getAttributes();
    Map<String, Object> cleanupAttr = attributes.get(CLEANUP_ATTRIBUTES_KEY);
    if (cleanupAttr != null) {
      Collection<String> policyNames = (Collection<String>) cleanupAttr.get(CLEANUP_NAME_KEY);
      if (policyNames != null) {
        policyNames.remove(policyName);
      }
    }
    try {
      repositoryManager.update(repository.getConfiguration());
    }
    catch (Exception e) {
      log.error("Failed to update repository '{}'", repository.getName(), e);
      throw new WebApplicationMessageException(Status.INTERNAL_SERVER_ERROR,
          "Failed to update repository '" + repository.getName() + "'", APPLICATION_JSON);
    }
  }
}

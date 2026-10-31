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
package org.sonatype.nexus.repository.manager;

import java.util.Collections;
import java.util.List;

import org.sonatype.nexus.common.app.softdelete.SoftDeleteContext;
import org.sonatype.nexus.common.app.softdelete.SoftDeleteFormatGate;
import org.sonatype.nexus.common.app.softdelete.SoftDeleteSchemaVersions;
import org.sonatype.nexus.common.app.softdelete.SoftDeletedEntityService;
import org.sonatype.nexus.common.db.DatabaseCheck;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.security.RepositoryAdminPermission;
import org.apache.shiro.authz.AuthorizationException;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.security.BreadActions;
import org.sonatype.nexus.security.SecurityHelper;

import org.apache.shiro.subject.Subject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static java.util.Objects.requireNonNull;

/**
 * Single decision point for user-requested repository deletion.
 *
 * <p>
 * Centralizes the soft-delete-vs-hard-delete decision <em>and</em> the per-repository
 * {@code DELETE} authorization so that every human-facing caller — the REST API, the ExtDirect UI,
 * and the cloud UI — gets identical behavior. Previously each transport re-implemented the decision
 * inline, which (a) risked drift when the rule changed and (b) let the soft-delete branch bypass the
 * per-repository delete permission that the hard-delete path enforces (NEXUS-49147 / NEXUS-51733).
 * </p>
 *
 * <p>
 * System- and policy-driven deletions (cleanup policies, scheduled tasks, proxy cache expiry) must
 * continue to call {@link RepositoryManager#delete(String)} directly and are intentionally NOT routed
 * through here: only human-initiated deletes are eligible for soft-delete.
 * </p>
 */
@Component
public class RepositoryDeletionService
{
  private final RepositoryManager repositoryManager;

  private final SecurityHelper securityHelper;

  /**
   * The repository {@link SoftDeletedEntityService}, or {@code null} when the soft-delete plugin is
   * not present (e.g. OSS builds). When {@code null}, deletion always falls back to hard-delete.
   */
  private final SoftDeletedEntityService repositorySoftDeleteService;

  private final SoftDeleteFormatGate softDeleteFormatGate;

  private final DatabaseCheck databaseCheck;

  @Autowired
  public RepositoryDeletionService(
      final RepositoryManager repositoryManager,
      final SecurityHelper securityHelper,
      final List<SoftDeletedEntityService> softDeletedEntityServices,
      final SoftDeleteFormatGate softDeleteFormatGate,
      final DatabaseCheck databaseCheck)
  {
    this.repositoryManager = requireNonNull(repositoryManager);
    this.securityHelper = requireNonNull(securityHelper);
    this.repositorySoftDeleteService = requireNonNull(softDeletedEntityServices).stream()
        .filter(service -> "repository".equals(service.getEntityType()))
        .findFirst()
        .orElse(null);
    this.softDeleteFormatGate = requireNonNull(softDeleteFormatGate);
    this.databaseCheck = requireNonNull(databaseCheck);
  }

  /**
   * Delete a repository as requested by a user, applying the configured policy: soft-delete when the
   * feature is enabled and the repository is hosted, otherwise hard-delete.
   *
   * <p>
   * The per-repository {@code DELETE} permission is enforced once here, regardless of which path is
   * taken, so soft-delete and hard-delete can never diverge on authorization.
   * </p>
   *
   * @param name the repository name
   */
  public void delete(final String name) throws Exception {
    requireNonNull(name);

    Repository repository = repositoryManager.get(name);
    if (repository == null) {
      // Do not reveal whether the repository exists to a caller that may lack DELETE permission: fail exactly
      // as the not-permitted case below does (both raise AuthorizationException), so a missing repository and a
      // forbidden one are indistinguishable. This preserves the deliberate "forbidden vs. not found" conflation
      // the previous coreui safelyGetRepository provided, and keeps the authorization decision in one place
      // (NEXUS-51733).
      throw new AuthorizationException("Not authorized to delete repository '" + name + "', or it does not exist.");
    }
    securityHelper.ensurePermitted(new RepositoryAdminPermission(
        repository.getFormat().getValue(), name, Collections.singletonList(BreadActions.DELETE)));

    if (shouldSoftDelete(repository)) {
      repositorySoftDeleteService.softDelete(name, SoftDeleteContext.of(currentPrincipal()));
    }
    else {
      repositoryManager.delete(name);
    }
  }

  private String currentPrincipal() {
    Subject subject = securityHelper.subject();
    return subject != null && subject.getPrincipal() != null ? subject.getPrincipal().toString() : "system";
  }

  private boolean shouldSoftDelete(final Repository repository) {
    // Require the repository deleted_on column to actually exist (schema >= 2.171), not just the flag: under
    // ZDU a new-version node can run with the flag on before the deferred migration adds the column, and the
    // repository soft-delete write has no missing-column fallback (unlike the reads). Hard-delete until it exists.
    return softDeleteFormatGate.isEnabled()
        && databaseCheck.isAtLeast(SoftDeleteSchemaVersions.REPOSITORY)
        && repositorySoftDeleteService != null
        && repository != null
        && HostedType.NAME.equals(repository.getType().getValue())
        && softDeleteFormatGate.isEnabledForFormat(repository.getFormat().getValue());
  }
}

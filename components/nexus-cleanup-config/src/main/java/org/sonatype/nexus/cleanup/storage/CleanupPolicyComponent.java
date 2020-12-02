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
package org.sonatype.nexus.cleanup.storage;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.extdirect.DirectComponent;
import org.sonatype.nexus.extdirect.DirectComponentSupport;
import org.sonatype.nexus.extdirect.model.StoreLoadParameters;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.security.RepositoryAdminPermission;
import org.sonatype.nexus.repository.security.RepositoryPermissionChecker;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.softwarementors.extjs.djn.config.annotations.DirectAction;
import com.softwarementors.extjs.djn.config.annotations.DirectMethod;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.sonatype.nexus.security.BreadActions.ADD;
import static org.sonatype.nexus.security.BreadActions.READ;

/**
 * Cleanup policies config {@link DirectComponent}.
 *
 * @since 3.14
 */
@Named
@Singleton
@DirectAction(action = "cleanup_CleanupPolicy")
public class CleanupPolicyComponent
    extends DirectComponentSupport
{
  private final CleanupPolicyStorage cleanupPolicyStorage;

  private final RepositoryManager repositoryManager;

  private final RepositoryPermissionChecker repositoryPermissionChecker;

  @Inject
  public CleanupPolicyComponent(final CleanupPolicyStorage cleanupPolicyStorage,
                                final RepositoryManager repositoryManager,
                                final RepositoryPermissionChecker repositoryPermissionChecker)
  {
    this.cleanupPolicyStorage = checkNotNull(cleanupPolicyStorage);
    this.repositoryManager = checkNotNull(repositoryManager);
    this.repositoryPermissionChecker = repositoryPermissionChecker;
  }

  /**
   * Retrieve {@link CleanupPolicy}s by format.
   *
   * @return a list of {@link CleanupPolicy}s
   */
  @DirectMethod
  @Timed
  @ExceptionMetered
  public List<CleanupPolicyXO> readByFormat(final StoreLoadParameters parameters) {
    return ofNullable(parameters.getFilter("format"))
        .map(format -> {
          ensureUserHasPermissionToCleanupPolicyByFormat(format);
          return format;
        })
        .map(this::getAllByFormat)
        .orElse(emptyList());
  }

  private List<CleanupPolicyXO> getAllByFormat(final String format) {
    return cleanupPolicyStorage.getAllByFormat(format).stream()
        .map(CleanupPolicyXO::fromCleanupPolicy)
        .collect(toList());
  }

  private void ensureUserHasPermissionToCleanupPolicyByFormat(final String format) {
    RepositoryAdminPermission permission = new RepositoryAdminPermission(format, "*", singletonList(ADD));
    repositoryPermissionChecker.ensureUserHasAnyPermissionOrAdminAccess(
        singletonList(permission),
        READ,
        repositoryManager.browse()
    );
  }
}

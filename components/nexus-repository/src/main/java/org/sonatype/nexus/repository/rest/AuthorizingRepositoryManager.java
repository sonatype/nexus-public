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

package org.sonatype.nexus.repository.rest;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.security.RepositoryPermissionChecker;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.security.BreadActions.DELETE;

/**
 * @since 3.next
 */
public class AuthorizingRepositoryManager
{
  private final RepositoryManager repositoryManager;

  private final RepositoryPermissionChecker repositoryPermissionChecker;

  @Inject
  public AuthorizingRepositoryManager(
      final RepositoryManager repositoryManager,
      final RepositoryPermissionChecker repositoryPermissionChecker)
  {
    this.repositoryManager = checkNotNull(repositoryManager);
    this.repositoryPermissionChecker = checkNotNull(repositoryPermissionChecker);
  }

  public boolean delete(@Nonnull final String name) throws Exception {
    Repository repository = repositoryManager.get(name);
    if (repository != null) {
      repositoryPermissionChecker.ensureUserCanAdmin(DELETE, repository);
      repositoryManager.delete(repository.getName());
      return true;
    }
    return false;
  }
}

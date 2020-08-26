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
package org.sonatype.nexus.repository.rest.api;

import java.util.List;
import java.util.Optional;

import javax.annotation.Nonnull;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.config.Configuration;

/**
 * A repository manager which limits access to repositories based on the current user's permissions.
 *
 * @since 3.20
 */
public interface AuthorizingRepositoryManager
{
  void create(@Nonnull Configuration configuration) throws Exception;

  boolean update(@Nonnull Configuration configuration) throws Exception;

  boolean delete(@Nonnull String name) throws Exception;

  /**
   * Returns the repositories which the user has an administrative read privilege.
   */
  List<Repository> getRepositoriesWithAdmin();

  /**
   * Returns the repository if the user has administrative read privileges.
   *
   * @since 3.27
   */
  Optional<Repository> getRepositoryWithAdmin(String repositoryName);

  /**
   * Trigger rebuild index task for given repository.
   *
   * @throws RepositoryNotFoundException if repository does not exists
   * @throws IncompatibleRepositoryException if is not hosted or proxy type
   */
  @SuppressWarnings("squid:S1160") // suppress warning about two checked exceptions
  void rebuildSearchIndex(@Nonnull String name) throws RepositoryNotFoundException, IncompatibleRepositoryException;

  /**
   * Invalidate cache of a given repository.
   *
   * @throws RepositoryNotFoundException if repository does not exists
   * @throws IllegalStateException if is not proxy or group type
   */
  @SuppressWarnings("squid:S1160") // suppress warning about two checked exceptions
  void invalidateCache(@Nonnull String name) throws RepositoryNotFoundException, IncompatibleRepositoryException;
}

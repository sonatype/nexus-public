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
package org.sonatype.nexus.proxy.registry;

import java.util.List;

import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.nexus.proxy.repository.GroupRepository;
import org.sonatype.nexus.proxy.repository.Repository;

/**
 * The Interface RepositoryRegistry. It holds the Repositories.
 *
 * @author cstamas
 */
public interface RepositoryRegistry
{
  /**
   * Adds single repository.
   *
   * @param repository the repository
   */
  void addRepository(Repository repository);

  /**
   * Removes a single repository from the current registry. This does NOT permanently delete the repository. Use
   * DefaultNexusConfiguration#deleteRepository , which calls this method internally, to do that.
   *
   * @param repoId the repo id
   * @throws NoSuchRepositoryException the no such repository exception
   */
  void removeRepository(String repoId)
      throws NoSuchRepositoryException;

  /**
   * Removes "silently" single repository: no events will be emitted.
   *
   * @param repoId the repo id
   * @throws NoSuchRepositoryException the no such repository exception
   */
  void removeRepositorySilently(String repoId)
      throws NoSuchRepositoryException;

  /**
   * Returns the list of Repositories that serves Proximity. The repo order within list follows repo rank, so
   * processing is possible by simply iterating over resulting list.
   *
   * @return a List<Repository>
   */
  List<Repository> getRepositories();

  /**
   * Returns the list of Repositories that serves Proximity and have facets as T available. The repo order within
   * list
   * follows repo rank, so processing is possible by simply iterating over resulting list.
   *
   * @return a List<T>
   */
  <T> List<T> getRepositoriesWithFacet(Class<T> f);

  /**
   * Returns the requested Repository by ID.
   *
   * @param repoId the repo id
   * @return the repository
   * @throws NoSuchRepositoryException the no such repository exception
   */
  Repository getRepository(String repoId)
      throws NoSuchRepositoryException;

  /**
   * Returns the requested Repository by ID.
   *
   * @param repoId the repo id
   * @return the repository
   * @throws NoSuchRepositoryException the no such repository exception
   */
  <T> T getRepositoryWithFacet(String repoId, Class<T> f)
      throws NoSuchRepositoryException;

  /**
   * Checks for the existence of given repositoryId within this registry.
   *
   * @param repositoryId the repository id
   * @return boolean
   */
  boolean repositoryIdExists(String repositoryId);

  /**
   * Collect the groupIds where repository is member.
   *
   * @param repositoryId the repository id
   * @return list of groupId's where the repo appears as member
   */
  List<String> getGroupsOfRepository(String repositoryId);

  /**
   * Collect the groupIds where repository is member.
   *
   * @param repository the repository
   * @return list of group's where the repo appears as member
   */
  List<GroupRepository> getGroupsOfRepository(Repository repository);
}

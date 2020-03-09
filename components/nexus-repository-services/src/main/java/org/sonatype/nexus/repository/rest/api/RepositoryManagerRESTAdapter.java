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

import org.sonatype.nexus.repository.Repository;

/**
 * A component to share common functionality for interacting with {@link
 * org.sonatype.nexus.repository.manager.RepositoryManager}.
 * between API resources
 *
 * @since 3.4
 */
public interface RepositoryManagerRESTAdapter
{
  /**
   * Retrieve a repository. Will throw a {@link javax.ws.rs.WebApplicationException} with status code 422 if the
   * supplied  repository id is null, and throws a {@link javax.ws.rs.NotFoundException} if no repository with the
   * supplied id exists.
   */
  Repository getRepository(String repositoryId);

  /**
   * Retrieve all repositories that the user access to.
   */
  List<Repository> getRepositories();
}

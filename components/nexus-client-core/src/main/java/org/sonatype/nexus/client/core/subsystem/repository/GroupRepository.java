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
package org.sonatype.nexus.client.core.subsystem.repository;

import java.util.List;

/**
 * A Nexus group {@link Repository}.
 *
 * @since 2.3
 */
public interface GroupRepository<T extends GroupRepository>
    extends Repository<T, RepositoryStatus>
{

  List<String> memberRepositories();

  /**
   * Configures member repositories. Provided member repositories will replace exiting members (if any).
   *
   * @param memberRepositoryIds ids of member repositories (cannot be null)
   * @return itself, for fluent api usage
   */
  T ofRepositories(String... memberRepositoryIds);

  /**
   * Adds member repositories at the end of list of current repositories.
   *
   * @param memberRepositoryIds ids of member repositories to be added (cannot be null)
   * @return itself, for fluent api usage
   */
  T addMember(String... memberRepositoryIds);

  /**
   * Removed member repositories from the list of current repositories.
   *
   * @param memberRepositoryIds ids of member repositories to be removed (cannot be null)
   * @return itself, for fluent api usage
   */
  T removeMember(String... memberRepositoryIds);

}

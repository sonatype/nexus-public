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

/**
 * A Nexus repository.
 *
 * @since 2.3
 */
public interface Repository<T extends Repository, U extends RepositoryStatus>
{

  /**
   * @return repository id (never null)
   */
  String id();

  /**
   * @return repository name
   */
  String name();

  /**
   * @return content URI (null when repository does not publish its URI)
   */
  String contentUri();

  /**
   * @return repository status.
   */
  U status();

  /**
   * Sets repository name.
   *
   * @param name repository name
   * @return itself, for fluent api usage
   */
  T withName(String name);

  /**
   * Directly puts repository out of service (no save required).
   *
   * @return itself, for fluent api usage
   */
  T putOutOfService();

  /**
   * Directly puts repository in service (no save required).
   *
   * @return itself, for fluent api usage
   */
  T putInService();

  /**
   * Refreshes repository, replacing any current changes.
   *
   * @return itself, for fluent api usage
   */
  T refresh();

  /**
   * Saves current changes.
   *
   * @return itself, for fluent api usage
   */
  T save();

  /**
   * Removes the repository.
   *
   * @return itself, for fluent api usage
   */
  T remove();

  /**
   * @return {@code true} if the repository is exposed, {@code false} otherwise.
   * @since 2.5
   */
  boolean isExposed();
}

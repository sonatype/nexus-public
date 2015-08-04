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

import java.util.Collection;

/**
 * Repositories subsystem.
 *
 * @since 2.3
 */
public interface Repositories
{

  /**
   * Retrieves an existing repository by id.
   *
   * @param id of repository to be retrieved (cannot be null)
   * @return repository with specified id
   */
  <R extends Repository> R get(String id);

  /**
   * Retrieves an existing repository by id.
   *
   * @param type expected type of retrieved repository (cannot be null)
   * @param id   of repository to be retrieved (cannot be null)
   * @return repository with specified id
   * @throws ClassCastException - if repository with specified id is not to expected type
   */
  <R extends Repository> R get(Class<R> type, String id);

  /**
   * Retrieves all existing repositories.
   *
   * @return all exiting repositories (never null)
   */
  Collection<Repository> get();

  /**
   * Retrieves all existing repositories of specified type.
   *
   * @param type expected type of retrieved repository (cannot be null)
   * @return all exiting repositories of expected type (never null)
   */
  <R extends Repository> Collection<R> get(Class<R> type);

  /**
   * Creates a new repository of specified type / id.
   *
   * @param type of created repository
   * @param id   of new repository
   * @return created repository (never null)
   */
  <R extends Repository> R create(Class<R> type, String id);

}

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
 * A Nexus virtual {@link Repository}.
 *
 * @since 2.3
 */
public interface VirtualRepository<T extends VirtualRepository>
    extends Repository<T, RepositoryStatus>
{

  /**
   * @return id of virtualized repository
   */
  String virtualizedRepository();

  /**
   * Specify the id of virtualized repository.
   *
   * @param repositoryId of virtualized repository
   * @return itself, for fluent api usage
   */
  T ofRepository(String repositoryId);

}

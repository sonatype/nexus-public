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
package org.sonatype.nexus.client.core.spi.subsystem.repository;

import org.sonatype.nexus.client.core.subsystem.repository.Repository;
import org.sonatype.nexus.client.rest.jersey.JerseyNexusClient;
import org.sonatype.nexus.rest.model.RepositoryBaseResource;

/**
 * A {@link Repository} factory, that can create a new one or from a REST resource.
 *
 * @since 2.3
 */
public interface RepositoryFactory<R extends Repository>
{

  /**
   * @param resource to be converted (never null)
   * @return a score if this factory can adapt the resource into a {@link Repository}. Factory with higher score
   *         will be used. A value <= 0 will be mean that this factory cannot adapt
   */
  int canAdapt(RepositoryBaseResource resource);

  /**
   * Adapts a resource to a {@link Repository}.
   *
   * @param nexusClient current Nexus client
   * @param resource    to be adapted
   * @return {@link Repository} created from resource
   */
  R adapt(JerseyNexusClient nexusClient, RepositoryBaseResource resource);

  /**
   * Whether or not this factory can create a {@link Repository} of specified type
   *
   * @param type to be created
   * @return true if it can create, false otherwise
   */
  boolean canCreate(Class<? extends Repository> type);

  /**
   * Creates a {@link Repository} with specified id
   *
   * @param nexusClient current Nexus client (not null)
   * @param id          of repository to be created (not null / not empty)
   * @return created {@link Repository}
   */
  R create(JerseyNexusClient nexusClient, String id);

}

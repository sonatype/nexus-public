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
package org.sonatype.nexus.client.core.subsystem.routing;

import org.sonatype.nexus.client.core.exception.NexusClientErrorResponseException;
import org.sonatype.nexus.client.core.exception.NexusClientNotFoundException;

/**
 * Client subsystem for Nexus Automatic Routing feature.
 *
 * @author cstamas
 * @since 2.4
 */
public interface Routing
{
  /**
   * Returns the status of the Automatic Routing of a Maven Repository (proxy, hosted or group).
   *
   * @param mavenRepositoryId the ID of the Maven repository you want get status for.
   * @return the status for given repository.
   * @throws NexusClientNotFoundException if the passed in ID does not exists.
   */
  Status getStatus(String mavenRepositoryId);

  /**
   * Perform a forced update of the prefix file of a Maven repository. This method returns immediately, but it spawns
   * a background operation on Nexus that will perform the update and it's outcome will be reflected in status when
   * update is done.
   *
   * @param mavenRepositoryId the ID of the Maven Repository you want update the prefix file for (see throws for what
   *                          kind of repositories this call is allowed).
   * @throws NexusClientErrorResponseException
   *                                      if the passed in ID is not a Maven Proxy repository.
   * @throws NexusClientNotFoundException if the passed in ID does not exists.
   */
  void updatePrefixFile(String mavenRepositoryId)
      throws NexusClientErrorResponseException, NexusClientNotFoundException;

  /**
   * Returns the {@link DiscoveryConfiguration} for given Maven Proxy repository.
   *
   * @param mavenProxyRepositoryId the ID of the Maven Proxy repository you want configuration for.
   * @return the configuration entity.
   * @throws NexusClientErrorResponseException
   *                                      if the passed in ID is not a Maven Proxy repository.
   * @throws NexusClientNotFoundException if the passed in ID does not exists.
   */
  DiscoveryConfiguration getDiscoveryConfigurationFor(String mavenProxyRepositoryId)
      throws NexusClientErrorResponseException, NexusClientNotFoundException;

  /**
   * Sets the {@link DiscoveryConfiguration} for given Maven Proxy repository.
   *
   * @param mavenProxyRepositoryId the ID of the Maven Proxy repository you want configuration for.
   * @param configuration          the configuration.
   * @throws NexusClientErrorResponseException
   *                                      if the passed in ID is not a Maven Proxy repository, or, the
   *                                      configuration is invalid (interval zero or negative).
   * @throws NexusClientNotFoundException if the passed in ID does not exists.
   */
  void setDiscoveryConfigurationFor(String mavenProxyRepositoryId, DiscoveryConfiguration configuration)
      throws NexusClientErrorResponseException, NexusClientNotFoundException;

}

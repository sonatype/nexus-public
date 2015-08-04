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
package org.sonatype.nexus.configuration.application;

import java.io.IOException;
import java.util.List;

import org.sonatype.configuration.ConfigurationException;
import org.sonatype.configuration.validation.InvalidConfigurationException;
import org.sonatype.nexus.configuration.model.CRepository;
import org.sonatype.nexus.proxy.AccessDeniedException;
import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.nexus.proxy.registry.RepositoryTypeDescriptor;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.tasks.descriptors.ScheduledTaskDescriptor;

public interface MutableConfiguration
{
  // ----------------------------------------------------------------------------------------------------------
  // Security (TODO: this should be removed, security has to be completely "paralell" and not interleaved!)
  // ----------------------------------------------------------------------------------------------------------

  boolean isAnonymousAccessEnabled();

  /**
   * Configures anonymous access in atomic way.
   *
   * @param enabled  {@code true} to enable and {@code false} to disable it.
   * @param username the username of the user to be used as "anonymous" user. If {@code enabled} parameter is
   *                 {@code true}, this value must be non-null.
   * @param password the password of the user to be used as "anonymous" user. If {@code enabled} parameter is
   *                 {@code true}, this value must be non-null.
   * @throws InvalidConfigurationException if {@code enabled} parameter is {@code true}, but passed in username or
   *                                       password parameters are empty ({@code null} or empty string).
   */
  void setAnonymousAccess(boolean enabled, String username, String password)
      throws InvalidConfigurationException;

  String getAnonymousUsername();

  String getAnonymousPassword();

  List<String> getRealms();

  void setRealms(List<String> realms)
      throws InvalidConfigurationException;

  // ----------------------------------------------------------------------------
  // Scheduled Tasks
  // ----------------------------------------------------------------------------

  List<ScheduledTaskDescriptor> listScheduledTaskDescriptors();

  ScheduledTaskDescriptor getScheduledTaskDescriptor(String id);

  // ----------------------------------------------------------------------------------------------------------
  // Repositories
  // ----------------------------------------------------------------------------------------------------------

  /**
   * Sets the default (applied to all that has no exceptions set with {
   * {@link #setRepositoryMaxInstanceCount(RepositoryTypeDescriptor, int)} method) maxInstanceCount. Any positive
   * integer limits the max count of live instances, any less then 0 integer removes the limitation. Note: setting
   * limitations on already booted instance will not "enforce" the limitation!
   */
  void setDefaultRepositoryMaxInstanceCount(int count);

  /**
   * Limits the maxInstanceCount for the passed in repository type. Any positive integer limits the max count of live
   * instances, any less then 0 integer removes the limitation. Note: setting limitations on already booted instance
   * will not "enforce" the limitation!
   */
  void setRepositoryMaxInstanceCount(RepositoryTypeDescriptor rtd, int count);

  /**
   * Returns the count limit for the passed in repository type.
   */
  int getRepositoryMaxInstanceCount(RepositoryTypeDescriptor rtd);

  // CRepository: CRUD

  /**
   * Creates a repository live instance out of the passed in model. It validates, registers it with repository
   * registry and puts it into configuration. And finally saves configuration.
   *
   * @return the repository instance.
   */
  Repository createRepository(CRepository settings)
      throws ConfigurationException, IOException;

  /**
   * Drops a user managed repository.
   *
   * @see #deleteRepository(String, boolean)
   */
  public void deleteRepository(String id)
      throws NoSuchRepositoryException, IOException, ConfigurationException, AccessDeniedException;

  /**
   * Drops a repository, can only delete user managed repository unless force parameter is {@code true}.
   *
   * @throws AccessDeniedException when try to delete a non-user-managed repository and without force enabled
   */
  public void deleteRepository(String id, boolean force)
      throws NoSuchRepositoryException, IOException, ConfigurationException, AccessDeniedException;

  // FIXME: This will be removed: NEXUS-2363 vvvvv
  // CRemoteNexusInstance

  // FIXME: This will be removed: NEXUS-2363 ^^^^^

}

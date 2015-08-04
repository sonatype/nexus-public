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

import java.io.File;
import java.io.IOException;

import javax.annotation.Nullable;

import org.sonatype.nexus.configuration.model.Configuration;
import org.sonatype.nexus.proxy.storage.local.LocalStorageContext;
import org.sonatype.nexus.proxy.storage.remote.RemoteStorageContext;

/**
 * ApplicationConfiguration is the main component to have and maintain configuration.
 */
public interface ApplicationConfiguration
{
  /**
   * Return the directory where Nexus is installed.  This may be null for war deployments.
   *
   * @since 2.7
   *
   * @deprecated Use {@link ApplicationDirectories}
   */
  @Nullable
  @Deprecated
  File getInstallDirectory();

  /**
   * Gets the working directory as file. The directory is created if needed and is guaranteed to exists.
   *
   * @deprecated Use {@link ApplicationDirectories}
   */
  @Deprecated
  File getWorkingDirectory();

  /**
   * Gets the working directory with some subpath. The directory is created and is guaranteed to exists.
   *
   * @deprecated Use {@link ApplicationDirectories}
   */
  @Deprecated
  File getWorkingDirectory(String key);

  /**
   * Gets the working directory with some subpath. The directory is created if needed.
   *
   * @param key            the subpath you want to have access to
   * @param createIfNeeded set to {@code true} if you want to have it created, {@code false} otherwise.
   * @since 2.0
   *
   * @deprecated Use {@link ApplicationDirectories}
   */
  @Deprecated
  File getWorkingDirectory(String key, boolean createIfNeeded);

  /**
   * Returns the configuration directory. It defaults to $NEXUS_WORK/conf.
   */
  File getConfigurationDirectory();

  /**
   * Returns the temporary directory.
   *
   * @deprecated Use {@link ApplicationDirectories}
   */
  @Deprecated
  File getTemporaryDirectory();

  /**
   * Gets the top level local storage context.
   */
  LocalStorageContext getGlobalLocalStorageContext();

  /**
   * Gets the top level remote storage context.
   */
  RemoteStorageContext getGlobalRemoteStorageContext();

  /**
   * Saves the configuration.
   */
  void saveConfiguration() throws IOException;

  /**
   * Gets the Configuration object.
   *
   * @deprecated you should use setters/getters directly on Configurable instances, and not tampering with
   *             Configuration model directly!
   */
  Configuration getConfigurationModel();
}

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
package org.sonatype.nexus.common.app;

import java.io.File;

import javax.annotation.Nullable;

/**
 * Provides access to application directories.
 *
 * @since 2.8
 */
public interface ApplicationDirectories
{
  // TODO: Add nio path equivalents

  /**
   * Installation directory.
   *
   * Null for WAR-based deployments.
   */
  @Nullable
  File getInstallDirectory();

  /**
   * Temporary directory.
   */
  File getTemporaryDirectory();

  // TODO: Add getTemporaryDirectory(String path) helper, seems to be common use so add helper

  /**
   * Application directory.
   * 
   * WEB-INF/ for WAR-based deployments.
   * 
   * @since 3.0
   */
  File getAppDirectory();

  /**
   * Application sub-directory.
   * 
   * @since 3.0
   */
  File getAppDirectory(String path);

  /**
   * Work directory.
   */
  File getWorkDirectory();

  /**
   * Work sub-directory.
   *
   * @param path   Sub-directory path.
   * @param create True to create the directory if it does not exist.
   */
  File getWorkDirectory(String path, boolean create);

  /**
   * Work sub-directory.
   */
  File getWorkDirectory(String path);
}

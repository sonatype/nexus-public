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
package org.sonatype.nexus.bundle.launcher;

import java.io.File;
import java.net.URL;

import org.sonatype.sisu.bl.WebBundle;

/**
 * An Nexus bundle that can be created, started, stopped based on a provided configuration.
 *
 * @since 2.0
 */
public interface NexusBundle
    extends WebBundle<NexusBundle, NexusBundleConfiguration>
{

  /**
   * Returns Nexus directory (absolute path) of this bundle. This is the directory named {@code nexus-<version>} in
   * any Nexus bundle archive.
   *
   * @return nexus directory (absolute path). Never null.
   * @since 2.2
   */
  File getNexusDirectory();

  /**
   * Returns Nexus work directory (absolute path) of this bundle. This is the directory named
   * {@code sonatype-work/nexus} in any Nexus bundle archive.
   *
   * @return nexus work directory (absolute path). Never null.
   */
  File getWorkDirectory();

  /**
   * Returns Nexus log file.
   *
   * @return nexus log file. Never null.
   */
  File getNexusLog();

  /**
   * Returns launcher log file (wrapper.log).
   *
   * @return launcher log file. Never null.
   */
  File getLauncherLog();

  /**
   * Returns the SSL port if HTTPS support is enabled, otherwise -1.
   */
  int getSslPort();

  /**
   * @return the Secure URL bundle is running on, or null if not yet configured
   * @since 2.11.2
   */
  URL getSecureUrl();

}

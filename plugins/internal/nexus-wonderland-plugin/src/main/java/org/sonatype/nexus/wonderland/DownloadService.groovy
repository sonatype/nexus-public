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
package org.sonatype.nexus.wonderland

/**
 * Download service.
 *
 * @since 2.8
 */
interface DownloadService
{

  /**
   * @return directory where files to be downloaded are stored
   */
  File getDirectory()

  /**
   * @param fileName of file to be downloaded
   * @param authTicket authentication ticket
   * @return specified file, if file exists in downloads directory, null otherwise
   */
  File get(String fileName, String authTicket)

  /**
   * Moves specified file to downloads, using specified name
   * @param source to be moved
   * @param name name of file in downloads dir
   * @return moved file (residing in downloads)
   */
  File move(File source, String name)

  /**
   * Generate a unique file prefix.
   */
  String uniqueName(String prefix)

}
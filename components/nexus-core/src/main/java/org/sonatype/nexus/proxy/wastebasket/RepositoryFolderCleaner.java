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
package org.sonatype.nexus.proxy.wastebasket;

import java.io.IOException;

import org.sonatype.nexus.proxy.repository.Repository;

/**
 * A component doing "cleanup" (after a repository removal) of anything needed, for example removing directories or so.
 *
 * @author cstamas
 */
public interface RepositoryFolderCleaner
{
  /**
   * Performs the needed cleanup after repository already removed from system, that had ID as passed in.
   *
   * @param repository    the repository removed (WARNING: RepositoryRegistry does not contains it anymore!).
   * @param deleteForever true if removal wanted, false if just "move to trash".
   */
  void cleanRepositoryFolders(Repository repository, boolean deleteForever)
      throws IOException;
}

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
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.sisu.goodies.common.ComponentSupport;

@Named
@Singleton
public class DefaultRepositoryFolderRemover
    extends ComponentSupport
    implements RepositoryFolderRemover
{
  private final Map<String, RepositoryFolderCleaner> cleaners;

  @Inject
  public DefaultRepositoryFolderRemover(final Map<String, RepositoryFolderCleaner> cleaners) {
    this.cleaners = cleaners;
  }

  public void deleteRepositoryFolders(final Repository repository, final boolean deleteForever)
      throws IOException
  {
    log.debug("Removing folders of repository \"{}\" (ID={})", repository.getName(), repository.getId());

    for (RepositoryFolderCleaner cleaner : cleaners.values()) {
      try {
        cleaner.cleanRepositoryFolders(repository, deleteForever);
      }
      catch (Exception e) {
        log.warn("Got exception during execution of RepositoryFolderCleaner {}, continuing.",
            cleaner.getClass().getName(), e);
      }
    }
  }
}

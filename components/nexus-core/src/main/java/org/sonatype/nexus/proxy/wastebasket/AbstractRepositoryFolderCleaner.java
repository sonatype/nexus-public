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

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import javax.inject.Inject;

import org.sonatype.nexus.configuration.application.ApplicationConfiguration;
import org.sonatype.nexus.util.file.DirSupport;
import org.sonatype.sisu.goodies.common.ComponentSupport;

public abstract class AbstractRepositoryFolderCleaner
    extends ComponentSupport
    implements RepositoryFolderCleaner
{
  public static final String GLOBAL_TRASH_KEY = "trash";

  private ApplicationConfiguration applicationConfiguration;

  @Inject
  public void setApplicationConfiguration(final ApplicationConfiguration applicationConfiguration) {
    this.applicationConfiguration = applicationConfiguration;
  }

  protected ApplicationConfiguration getApplicationConfiguration() {
    return applicationConfiguration;
  }

  /**
   * Delete the file forever, or just keep it by renaming it (hence, will not be used anymore).
   *
   * @param file          file to be deleted
   * @param deleteForever if it's true, delete the file forever, if it's false, move the file to trash
   */
  protected void delete(final File file, final boolean deleteForever)
      throws IOException
  {
    Path basketPath =
        new File(getApplicationConfiguration().getWorkingDirectory(GLOBAL_TRASH_KEY), file.getName()).toPath();
    if (!deleteForever) {
      // if trash already has this named path (whatever is), rename it
      DirSupport.moveIfExists(
          basketPath,
          basketPath.getParent().resolve(file.getName() + "__" + System.currentTimeMillis())
      );
      // move to trash
      DirSupport.moveIfExists(
          file.toPath(),
          basketPath
      );
    }
    else {
      DirSupport.deleteIfExists(file.toPath());
    }
  }
}

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

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.proxy.repository.Repository;

@Named("core-storage")
@Singleton
public class CoreRepositoryFolderCleaner
    extends AbstractRepositoryFolderCleaner
{
  public void cleanRepositoryFolders(final Repository repository, boolean deleteForever)
      throws IOException
  {
    File defaultStorageFolder =
        new File(new File(getApplicationConfiguration().getWorkingDirectory(), "storage"), repository.getId());

    String defaultStorageURI = defaultStorageFolder.toURI().toURL().toString();
    defaultStorageURI = defaultStorageURI.endsWith("/") ? defaultStorageURI : defaultStorageURI + "/";

    String localURI = repository.getLocalUrl();

    localURI = localURI.endsWith("/") ? localURI : localURI + "/";

    boolean defaultLocation = defaultStorageURI.equals(localURI);

    // we do this _only_ if storage is not user-customized
    if (defaultLocation) {
      delete(defaultStorageFolder, deleteForever);
    }
  }
}

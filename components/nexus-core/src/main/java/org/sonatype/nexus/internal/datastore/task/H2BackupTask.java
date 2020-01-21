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
package org.sonatype.nexus.internal.datastore.task;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.common.app.ApplicationDirectories;
import org.sonatype.nexus.datastore.api.DataStore;
import org.sonatype.nexus.datastore.api.DataStoreManager;
import org.sonatype.nexus.scheduling.Task;
import org.sonatype.nexus.scheduling.TaskSupport;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A {@link Task} for backing up an embedded H2 datastore.
 *
 * @since 3.21
 */
@Named
public class H2BackupTask
    extends TaskSupport
{
  private final DataStoreManager dataStoreManager;

  private final ApplicationDirectories applicationDirectories;

  @Inject
  public H2BackupTask(final DataStoreManager dataStoreManager, final ApplicationDirectories applicationDirectories) {
    this.dataStoreManager = checkNotNull(dataStoreManager);
    this.applicationDirectories = checkNotNull(applicationDirectories);
  }

  @Override
  public String getMessage() {
    return "Backup embedded h2 database to the specified location";
  }

  @Override
  protected Object execute() throws Exception {
    String dataStoreName =
        checkNotNull(getConfiguration().getString(H2BackupTaskDescriptor.DATASTORE), "DataStore name not configured");
    String location =
        checkNotNull(getConfiguration().getString(H2BackupTaskDescriptor.LOCATION), "Backup location not configured");

    Optional<DataStore<?>> dataStore = dataStoreManager.get(dataStoreName);

    if (!dataStore.isPresent()) {
      throw new RuntimeException("Unable to locate datastore with name " + dataStoreName);
    }

    location = handleRelative(location);
    location = interpolate(location);

    log.info("Starting backup of {} to {}", dataStoreName, location);

    long start = System.currentTimeMillis();

    dataStore.get().backup(location);

    log.info("Completed backup of {} in {} ms", dataStoreName, System.currentTimeMillis() - start);

    return null;
  }

  private String handleRelative(final String location) {
    Path path = Paths.get(location);
    if (path.isAbsolute()) {
      return location;
    }
    else {
      return applicationDirectories.getWorkDirectory().toPath().resolve(path).toString();
    }
  }

  private static String interpolate(final String location) {
    if (location.contains("{datetime}")) {
      SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd_HHmmss");

      return location.replaceAll("\\{datetime\\}", formatter.format(new Date()));
    }
    return location;
  }
}

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
package org.sonatype.nexus.orient.restore;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.app.ApplicationVersion;
import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.app.VersionComparator;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.orient.DatabaseManager;
import org.sonatype.nexus.orient.DatabaseRestorer;

import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.RESTORE;
import static org.sonatype.nexus.orient.DatabaseInstanceNames.DATABASE_NAMES;

/**
 * Service that manages the RESTORE {@link org.sonatype.nexus.common.app.ManagedLifecycle.Phase}.
 *
 * Intended to require that restore for the schemas that are interdependent happens as a single unit.
 */
@Named
@ManagedLifecycle(phase = RESTORE)
@Singleton
public class RestoreServiceImpl
    extends StateGuardLifecycleSupport
    implements RestoreService
{
  private final DatabaseRestorer databaseRestorer;

  private final DatabaseManager databaseManager;

  private final ApplicationVersion applicationVersion;

  @Inject
  public RestoreServiceImpl(final DatabaseRestorer databaseRestorer, final DatabaseManager databaseManager,
                            final ApplicationVersion applicationVersion) {
    this.databaseRestorer = databaseRestorer;
    this.databaseManager = databaseManager;
    this.applicationVersion = applicationVersion;
  }

  @Override
  protected void doStart() throws Exception {
    Map<String, RestoreFile> pending = checkCompleteness();

    checkVersions(pending);

    Set<String> timestamps = pending.values().stream().map(f -> f.getTimestamp()).collect(Collectors.toSet());
    if (timestamps.size() > 1) {
      log.warn(
          "Found mismatched timestamps {} in restore file names; will proceed with restore for backwards compatibility",
          timestamps);
    }

    log.debug("passed all restore tests, restore state: " + pending);
    // let this class create/and register instances for DATABASE_NAMES; will trigger create and restore (if necessary)
    DATABASE_NAMES.parallelStream().forEach(databaseManager::instance);
  }

  /**
   * Check that we have a complete set of databases.
   *
   * @return a model of the available restore files, keyed on database name
   * @throws IOException if there was a problem locating restore files
   * @throws IllegalStateException if one or more files are missing
   */
  private Map<String, RestoreFile> checkCompleteness() throws IOException {
    Map<String, RestoreFile> pending = new HashMap<>();
    for (String name: DATABASE_NAMES) {
      RestoreFile restoreFile = databaseRestorer.getPendingRestore(name);
      if (restoreFile != null) {
        log.debug("found pending restore for {}", name);
        pending.put(name, restoreFile);
      }
    }

    if (!pending.isEmpty() && !pending.keySet().equals(DATABASE_NAMES)) {
      throw new IllegalStateException("Found pending database restore files for " + pending +
          ", but some are missing; to restore you must have files for " + DATABASE_NAMES);
    }
    return pending;
  }

  /**
   * If versions are present in the filenames, confirm that they are equal, and less than or equal to the current
   * application version.
   *
   * @param pending the available restore files, keyed by database name
   */
  private void checkVersions(final Map<String, RestoreFile> pending) {
    final String current = applicationVersion.getVersion().replace("-SNAPSHOT", "");
    Set<String> restoreFileVersions = pending.values().stream()
        .filter(r -> r.getVersion() != null)
        .map(r -> r.getVersion())
        .collect(Collectors.toSet());

    if (restoreFileVersions.size() > 1) {
      throw new IllegalStateException("Found mismatched versions " + restoreFileVersions + " amongst restore files" +
          "; to restore you must use matching versions (preferably " + current + ")");
    }

    if (!restoreFileVersions.isEmpty()) {
      String restoreVersion = restoreFileVersions.iterator().next();
      if(restoreVersion != null && new VersionComparator().compare(current, restoreVersion) < 0) {
        throw new IllegalStateException("Found version " + restoreVersion + " amongst restore files; to restore you" +
            " must use files from version " + current + " or earlier");
      }
    }
  }
}

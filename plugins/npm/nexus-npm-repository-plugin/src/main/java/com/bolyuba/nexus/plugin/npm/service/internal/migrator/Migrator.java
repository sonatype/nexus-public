/*
 * Copyright (c) 2007-2014 Sonatype, Inc. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */
package com.bolyuba.nexus.plugin.npm.service.internal.migrator;

import java.util.List;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.sisu.goodies.common.ComponentSupport;

import com.bolyuba.nexus.plugin.npm.hosted.NpmHostedRepository;
import com.bolyuba.nexus.plugin.npm.service.PackageRoot;
import com.bolyuba.nexus.plugin.npm.service.internal.MetadataStore;
import com.bolyuba.nexus.plugin.npm.service.internal.NxMetadataStore;
import com.bolyuba.nexus.plugin.npm.service.internal.orient.OrientMetadataStore;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Migration helper, migrates npm metadata from one {@link MetadataStore} to another.
 */
@Singleton
@Named
public class Migrator
    extends ComponentSupport
{
  private final OrientMetadataStore source;

  private final NxMetadataStore target;

  private int migrationsInProgress;

  @Inject
  public Migrator(@Nullable final OrientMetadataStore source,
                  final NxMetadataStore target) {
    this.source = source; // Orient is optional
    this.target = checkNotNull(target);
    this.migrationsInProgress = 0;
  }

  /**
   * Performs a migration of npm metadata for given hosted npm repository.
   */
  public void migrate(final NpmHostedRepository npmHostedRepository) {
    checkState(source != null, "Orient metadata store not available");
    checkNotNull(npmHostedRepository);

    try {
      mayStartOrientMetadataStore();
      doMigrate(npmHostedRepository);
    }
    finally {
      mayStopOrientMetadataStore();
    }
  }

  /**
   * Applies lifecycle to Orient storage, as it should not be started at all. The startOnce method can be called
   * multiple times. Also maintains a counter how many times start was invoked.
   */
  private synchronized void mayStartOrientMetadataStore() {
    migrationsInProgress++;
    source.startOnce();
  }

  /**
   * Closes Orient storage if this is last stop invocation, based on the counter.
   */
  private synchronized void mayStopOrientMetadataStore() {
    migrationsInProgress--;
    if (migrationsInProgress == 0) {
      try {
        source.stop();
      }
      catch (Exception e) {
        log.warn("Could not stop OrientMetadataStore, ignoring", e);
      }
    }
  }

  /**
   * Migrates package metadata from Orient to Nx storage.
   */
  private void doMigrate(final NpmHostedRepository npmHostedRepository) {
    List<String> packageNames = source.listPackageNames(npmHostedRepository);
    log.info("Migrating {} package in npm hosted repository {}", packageNames.size(), npmHostedRepository.getId());
    for (String packageName : packageNames) {
      PackageRoot packageRoot = source.getPackageByName(npmHostedRepository, packageName);
      if (packageRoot != null) {
        target.replacePackage(npmHostedRepository, packageRoot);
      }
    }
    log.info("Done with migrating npm hosted repository {}", npmHostedRepository.getId());
  }
}

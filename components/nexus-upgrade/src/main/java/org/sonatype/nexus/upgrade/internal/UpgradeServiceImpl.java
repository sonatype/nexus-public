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
package org.sonatype.nexus.upgrade.internal;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.lifecycle.LifecycleSupport;
import org.sonatype.nexus.common.app.ApplicationDirectories;
import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.upgrade.Checkpoint;
import org.sonatype.nexus.common.upgrade.Upgrade;
import org.sonatype.nexus.common.upgrade.Upgrades;
import org.sonatype.nexus.upgrade.UpgradeService;

import com.google.common.base.Throwables;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.UPGRADE;
import static org.sonatype.nexus.upgrade.internal.UpgradeManager.checkpoints;
import static org.sonatype.nexus.upgrade.internal.UpgradeManager.upgrades;

/**
 * Default {@link UpgradeService}.
 * 
 * @since 3.1
 */
@Named
@ManagedLifecycle(phase = UPGRADE)
@Singleton
public class UpgradeServiceImpl
    extends LifecycleSupport
    implements UpgradeService
{
  private static final String BANNER =
      "\n- - - - - - - - - - - - - - - - - - - - - - - - -\n" +
      "{}" +
      "\n- - - - - - - - - - - - - - - - - - - - - - - - -";

  private final UpgradeManager upgradeManager;

  private final ModelVersionStore modelVersionStore;

  private Map<String, String> modelVersions;

  private final Path componentDatabase;

  @Inject
  public UpgradeServiceImpl(final ApplicationDirectories applicationDirectories,
                            final UpgradeManager upgradeManager,
                            final ModelVersionStore modelVersionStore)
  {
    this.upgradeManager = checkNotNull(upgradeManager);
    this.modelVersionStore = checkNotNull(modelVersionStore);

    File dbDirectory = applicationDirectories.getWorkDirectory("db");
    componentDatabase = dbDirectory.toPath().resolve("component/component.pcl");
  }

  @Override
  protected void doStart() throws Exception {
    modelVersionStore.start();

    modelVersions = modelVersionStore.load();

    List<Upgrade> upgrades = upgradeManager.plan(modelVersions);
    if (upgrades.isEmpty()) {
      return; // nothing to upgrade
    }

    try {
      if (firstTimeInstall()) {
        doInventory(upgrades);
      }
      else {
        doUpgrade(upgrades);
      }
    }
    catch (RuntimeException e) {
      // attempt to unwrap detailed exception from runtime wrapper
      Throwables.propagateIfPossible(e.getCause(), Exception.class);
      throw e;
    }

    modelVersionStore.save(modelVersions);
  }

  @Override
  protected void doStop() throws Exception {
    modelVersionStore.stop();
  }

  private boolean firstTimeInstall() {
    return !Files.exists(componentDatabase); // this won't exist out-of-the-box
  }

  /**
   * Takes an inventory of all upgrades bundled into this first-time installation.
   */
  private void doInventory(List<Upgrade> upgrades) {
    upgrades.stream().map(UpgradeManager::upgrades)
        .forEach(upgrade -> modelVersions.put(upgrade.model(), upgrade.to()));
  }

  /**
   * Attempts to upgrade an existing installation keeping track of what was upgraded.
   */
  private void doUpgrade(List<Upgrade> upgrades) {
    List<Checkpoint> checkpoints = upgradeManager.prepare(upgrades);

    log.info(BANNER, "Begin upgrade");
    checkpoints.forEach(begin());
    try {
      log.info(BANNER, "Apply upgrade");
      upgrades.forEach(apply());
      log.info(BANNER, "Commit upgrade");
      checkpoints.forEach(commit());
    }
    catch (Throwable e) {
      log.warn(BANNER, "Rollback upgrade");
      checkpoints.forEach(rollback());
      log.warn(BANNER, "Upgrade failed");

      throw Throwables.propagate(e);
    }
    checkpoints.forEach(end());
    log.info(BANNER, "Upgrade complete");
  }

  private Consumer<Checkpoint> begin() {
    return checkpoint -> {
      String model = checkpoints(checkpoint).model();
      try {
        log.info("Checkpoint {}", model);
        checkpoint.begin(modelVersions.getOrDefault(model, "1.0"));
      }
      catch (Throwable e) {
        log.warn("Problem checkpointing {}", model, e);
        throw Throwables.propagate(e);
      }
    };
  }

  private Consumer<Upgrade> apply() {
    return upgrade -> {
      Upgrades upgrades = upgrades(upgrade);
      String detail = String.format("%s from %s to %s", upgrades.model(), upgrades.from(), upgrades.to());
      try {
        log.info("Upgrade {}", detail);
        upgrade.apply();

        // keep track of which upgrades we've applied so far
        modelVersions.put(upgrades.model(), upgrades.to());
      }
      catch (Throwable e) {
        log.warn("Problem upgrading {}", detail, e);
        throw Throwables.propagate(e);
      }
    };
  }

  private Consumer<Checkpoint> commit() {
    return checkpoint -> {
      String model = checkpoints(checkpoint).model();
      try {
        log.info("Commit {}", model);
        checkpoint.commit();
      }
      catch (Throwable e) {
        log.warn("Problem committing {}", model, e);
        throw Throwables.propagate(e);
      }
    };
  }

  private Consumer<Checkpoint> rollback() {
    return checkpoint -> {
      String model = checkpoints(checkpoint).model();
      try {
        log.info("Rolling back {}", model);
        checkpoint.rollback();
      }
      catch (Throwable e) {
        log.warn("Problem rolling back {}", model, e);
        // continue rolling back other checkpoints...
      }
    };
  }

  private Consumer<Checkpoint> end() {
    return checkpoint -> {
      String model = checkpoints(checkpoint).model();
      try {
        log.info("Cleaning up {}", model);
        checkpoint.end();
      }
      catch (Throwable e) { // NOSONAR
        log.warn("Problem cleaning up {}", model, e);
        // continue cleaning up other checkpoints...
      }
    };
  }
}

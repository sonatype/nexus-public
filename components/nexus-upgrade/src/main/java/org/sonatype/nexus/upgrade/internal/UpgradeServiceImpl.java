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

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.app.VersionComparator;
import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.common.upgrade.Checkpoint;
import org.sonatype.nexus.common.upgrade.Upgrade;
import org.sonatype.nexus.common.upgrade.Upgrades;
import org.sonatype.nexus.upgrade.UpgradeService;

import com.google.common.base.Throwables;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.UPGRADE;

/**
 * Default {@link UpgradeService}.
 * 
 * @since 3.1
 */
@Named
@ManagedLifecycle(phase = UPGRADE)
@Singleton
public class UpgradeServiceImpl
    extends StateGuardLifecycleSupport
    implements UpgradeService
{
  private static final String BANNER =
      "\n- - - - - - - - - - - - - - - - - - - - - - - - -\n" +
      "{}" +
      "\n- - - - - - - - - - - - - - - - - - - - - - - - -";

  private final UpgradeManager upgradeManager;

  private final ModelVersionStore modelVersionStore;

  private final NodeAccess nodeAccess;

  private Map<String, String> modelVersions;

  @Inject
  public UpgradeServiceImpl(final UpgradeManager upgradeManager,
                            final ModelVersionStore modelVersionStore,
                            final NodeAccess nodeAccess)
  {
    this.upgradeManager = checkNotNull(upgradeManager);
    this.modelVersionStore = checkNotNull(modelVersionStore);
    this.nodeAccess = checkNotNull(nodeAccess);
  }

  @Override
  protected void doStart() throws Exception {
    modelVersionStore.start();

    modelVersions = validate(modelVersionStore.load());

    // if we're joining an existing cluster then only consider local upgrades
    boolean localOnly = !nodeAccess.isOldestNode();
    List<Upgrade> upgrades = upgradeManager.selectUpgrades(modelVersions, localOnly);
    if (upgrades.isEmpty()) {
      return; // nothing to upgrade
    }

    try {
      if (modelVersionStore.isNewInstance()) {
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

  /**
   * Takes an inventory of all upgrades bundled into this first-time installation.
   */
  private void doInventory(List<Upgrade> upgrades) {
    upgrades.stream().map(upgradeManager::getMetadata)
        .forEach(upgrade -> modelVersions.put(upgrade.model(), upgrade.to()));
  }

  /**
   * Attempts to upgrade an existing installation keeping track of what was upgraded.
   */
  private void doUpgrade(List<Upgrade> upgrades) {
    List<Checkpoint> checkpoints = upgradeManager.selectCheckpoints(upgrades);

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

      Throwables.throwIfUnchecked(e);
      throw new RuntimeException(e);
    }
    checkpoints.forEach(end());
    log.info(BANNER, "Upgrade complete");
  }

  /**
   * Ensures the stored model versions are valid for this version of nexus.
   */
  private Map<String, String> validate(final Map<String, String> modelVersions) {
    boolean failed = false;
    for (Map.Entry<String, String> entry : upgradeManager.latestKnownModelVersions().entrySet()) {
      String current = modelVersions.getOrDefault(entry.getKey(), "1.0");
      String latest = entry.getValue();
      if (VersionComparator.INSTANCE.compare(latest, current) < 0) {
        log.error("The database model for {} is {}, but the latest supported by this version of nexus is {}",
            entry.getKey(), current, latest);
        failed = true;
      }
    }
    if (failed) {
      throw new IllegalStateException("Incompatible sonatype-work database model detected. Will result in failure " +
          "to launch. Shutting down.");
    }
    return modelVersions;
  }

  private Consumer<Checkpoint> begin() {
    return checkpoint -> {
      String model = upgradeManager.getModel(checkpoint);
      try {
        log.info("Checkpoint {}", model);
        checkpoint.begin(modelVersions.getOrDefault(model, "1.0"));
      }
      catch (Throwable e) {
        log.warn("Problem checkpointing {}", model, e);
        Throwables.throwIfUnchecked(e);
        throw new RuntimeException(e);
      }
    };
  }

  private Consumer<Upgrade> apply() {
    return upgrade -> {
      Upgrades metadata = upgradeManager.getMetadata(upgrade);
      String detail = String.format("%s from %s to %s", metadata.model(), metadata.from(), metadata.to());
      try {
        log.info("Upgrade {}", detail);
        upgrade.apply();

        // keep track of which upgrades we've applied so far
        modelVersions.put(metadata.model(), metadata.to());
      }
      catch (Throwable e) {
        log.warn("Problem upgrading {}", detail, e);
        Throwables.throwIfUnchecked(e);
        throw new RuntimeException(e);
      }
    };
  }

  private Consumer<Checkpoint> commit() {
    return checkpoint -> {
      String model = upgradeManager.getModel(checkpoint);
      try {
        log.info("Commit {}", model);
        checkpoint.commit();
      }
      catch (Throwable e) {
        log.warn("Problem committing {}", model, e);
        Throwables.throwIfUnchecked(e);
        throw new RuntimeException(e);
      }
    };
  }

  private Consumer<Checkpoint> rollback() {
    return checkpoint -> {
      String model = upgradeManager.getModel(checkpoint);
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
      String model = upgradeManager.getModel(checkpoint);
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

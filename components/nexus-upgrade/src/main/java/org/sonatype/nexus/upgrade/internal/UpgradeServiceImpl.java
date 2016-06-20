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
import java.util.function.Consumer;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.lifecycle.LifecycleSupport;
import org.sonatype.nexus.common.app.ApplicationDirectories;
import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.property.PropertiesFile;
import org.sonatype.nexus.common.upgrade.Checkpoint;
import org.sonatype.nexus.common.upgrade.Upgrade;
import org.sonatype.nexus.common.upgrade.Upgrades;
import org.sonatype.nexus.upgrade.UpgradeService;

import com.google.common.base.Throwables;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Maps.fromProperties;
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
  private static final String BEGIN_BANNER = banner("Begin upgrade");

  private static final String APPLY_BANNER = banner("Apply upgrade");

  private static final String COMMIT_BANNER = banner("Commit upgrade");

  private static final String ROLLBACK_BANNER = banner("Rolling back upgrade");

  private final UpgradeManager upgradeManager;

  private final PropertiesFile modelProperties;

  private final Path componentDatabase;

  @Inject
  public UpgradeServiceImpl(final ApplicationDirectories applicationDirectories, final UpgradeManager upgradeManager) {
    this.upgradeManager = checkNotNull(upgradeManager);

    File dbDirectory = applicationDirectories.getWorkDirectory("db");
    modelProperties = new PropertiesFile(new File(dbDirectory, "model.properties"));
    componentDatabase = dbDirectory.toPath().resolve("component/component.pcl");
  }

  @Override
  protected void doStart() throws Exception {

    if (modelProperties.getFile().exists()) {
      modelProperties.load();
    }

    List<Upgrade> upgrades = upgradeManager.plan(fromProperties(modelProperties));
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

    modelProperties.store();
  }

  private boolean firstTimeInstall() {
    return !Files.exists(componentDatabase); // this won't exist out-of-the-box
  }

  /**
   * Takes an inventory of all upgrades bundled into this first-time installation.
   */
  private void doInventory(List<Upgrade> upgrades) {
    upgrades.stream().map(UpgradeManager::upgrades)
        .forEach(upgrade -> modelProperties.put(upgrade.model(), upgrade.to()));
  }

  /**
   * Attempts to upgrade an existing installation keeping track of what was upgraded.
   */
  private void doUpgrade(List<Upgrade> upgrades) {
    List<Checkpoint> checkpoints = upgradeManager.prepare(upgrades);

    log.info(BEGIN_BANNER);
    checkpoints.forEach(begin());
    try {
      log.info(APPLY_BANNER);
      upgrades.forEach(apply());

      log.info(COMMIT_BANNER);
      checkpoints.forEach(commit());
    }
    catch (Throwable e) {
      log.warn(ROLLBACK_BANNER, e);
      checkpoints.forEach(rollback());
      throw Throwables.propagate(e);
    }
  }

  private Consumer<Checkpoint> begin() {
    return checkpoint -> {
      String detail = checkpoints(checkpoint).model();
      try {
        log.info("Checkpoint {}", detail);
        checkpoint.begin();
      }
      catch (Throwable e) {
        log.warn("Problem checkpointing {}", detail, e);
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
        modelProperties.put(upgrades.model(), upgrades.to());
      }
      catch (Throwable e) {
        log.warn("Problem upgrading {}", detail, e);
        throw Throwables.propagate(e);
      }
    };
  }

  private Consumer<Checkpoint> commit() {
    return checkpoint -> {
      String detail = checkpoints(checkpoint).model();
      try {
        log.info("Commit {}", detail);
        checkpoint.commit();
      }
      catch (Throwable e) {
        log.warn("Problem committing {}", detail, e);
        throw Throwables.propagate(e);
      }
    };
  }

  private Consumer<Checkpoint> rollback() {
    return checkpoint -> {
      String detail = checkpoints(checkpoint).model();
      try {
        log.info("Rolling back {}", detail);
        checkpoint.rollback();
      }
      catch (Throwable e) {
        log.warn("Problem rolling back {}", detail, e);
        // continue rolling back other checkpoints...
      }
    };
  }

  private static String banner(final String message) {
    return new StringBuilder()
        .append("\n- - - - - - - - - - - - - - - - - - - - - - - - -\n")
        .append(message)
        .append("\n- - - - - - - - - - - - - - - - - - - - - - - - -")
        .toString();
  }
}

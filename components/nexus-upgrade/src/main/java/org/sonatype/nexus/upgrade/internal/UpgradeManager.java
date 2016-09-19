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

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.upgrade.Checkpoint;
import org.sonatype.nexus.common.upgrade.Checkpoints;
import org.sonatype.nexus.common.upgrade.Upgrade;
import org.sonatype.nexus.common.upgrade.Upgrades;
import org.sonatype.nexus.upgrade.UpgradeService;
import org.sonatype.nexus.upgrade.plan.DependencyResolver;
import org.sonatype.nexus.upgrade.plan.DependencySource;

import com.google.common.collect.ImmutableList;
import org.eclipse.aether.util.version.GenericVersionScheme;
import org.eclipse.aether.version.InvalidVersionSpecificationException;
import org.eclipse.aether.version.Version;
import org.eclipse.aether.version.VersionScheme;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;

/**
 * Manages {@link Upgrade}s and {@link Checkpoint}s for the {@link UpgradeService}.
 * 
 * @since 3.1
 */
public class UpgradeManager
    extends ComponentSupport
{
  private static final VersionScheme VERSION_SCHEME = new GenericVersionScheme();

  private final List<Checkpoint> managedCheckpoints;

  private final List<Upgrade> managedUpgrades;

  @Inject
  public UpgradeManager(final List<Checkpoint> managedCheckpoints, final List<Upgrade> managedUpgrades) {
    this.managedCheckpoints = checkNotNull(managedCheckpoints);
    this.managedUpgrades = checkNotNull(managedUpgrades);
  }

  public Set<String> getLocalModels() {
    return getModels(true);
  }

  public Set<String> getClusteredModels() {
    return getModels(false);
  }

  private Set<String> getModels(boolean local) {
    return managedCheckpoints.stream()
        .map(UpgradeManager::checkpoints)
        .filter(checkpoints -> checkpoints.local() == local)
        .map(checkpoints -> checkpoints.model())
        .collect(toCollection(HashSet::new));
  }

  /**
   * Returns ordered list of upgrades that should be applied to the current installation.
   * 
   * @param modelVersions The models and versions currently installed
   * @return ordered list of upgrades that should be applied
   */
  public List<Upgrade> plan(final Map<String, String> modelVersions) {
    return order(modelVersions, managedUpgrades.stream().filter(upgrade -> applies(modelVersions, upgrade)));
  }

  /**
   * Returns list of checkpoints that should be taken before applying the scheduled upgrades.
   * 
   * @param upgrades The scheduled upgrades
   * @return list of checkpoints that should be taken
   */
  public List<Checkpoint> prepare(final List<Upgrade> upgrades) {
    Set<String> models = upgrades.stream()
        .map(upgrade -> upgrades(upgrade).model())
        .collect(toCollection(HashSet::new));

    List<Checkpoint> checkpoints = managedCheckpoints.stream()
        .filter(checkpoint -> models.remove(checkpoints(checkpoint).model()))
        .collect(toList());

    checkArgument(models.isEmpty(), "Checkpoint(s) missing for %s", models);

    return checkpoints;
  }

  /**
   * Orders the given upgrades so any dependent upgrades appear earlier on in the sequence.
   */
  private static List<Upgrade> order(final Map<String, String> modelVersions, final Stream<Upgrade> upgrades) {
    DependencyResolver<DependencySource<UpgradePoint>> resolver = new DependencyResolver<>();

    resolver.add(ImmutableList.of(new InitialStep(modelVersions)));
    resolver.add(upgrades.map(UpgradeStep::new).sorted(UpgradeManager::byVersion).collect(toList()));

    return resolver.resolve()
        .getOrdered().stream()
        .map(UpgradeStep::unwrap)
        .filter(Objects::nonNull)
        .collect(toList());
  }

  /**
   * @return {@code true} if the upgrade applies on top of the current installation
   */
  private static boolean applies(final Map<String, String> modelVersions, final Upgrade upgrade) {
    Upgrades upgrades = upgrades(upgrade);

    String current = modelVersions.getOrDefault(upgrades.model(), "1.0");
    try {
      Version currentVersion = VERSION_SCHEME.parseVersion(current);
      Version fromVersion = VERSION_SCHEME.parseVersion(upgrades.from());
      Version toVersion = VERSION_SCHEME.parseVersion(upgrades.to());

      // sanity check the upgrade increases the version
      checkArgument(toVersion.compareTo(fromVersion) > 0,
          "%s upgrade version '%s' is not after '%s'",
          upgrade.getClass(), upgrades.to(), upgrades.from());

      // does the upgrade go past the current version?
      return toVersion.compareTo(currentVersion) > 0;
    }
    catch (final InvalidVersionSpecificationException e) {
      throw new IllegalArgumentException(e);
    }
  }

  /**
   * @return Metadata about this {@link Checkpoint}
   */
  static Checkpoints checkpoints(final Checkpoint checkpoint) {
    Checkpoints checkpoints = checkpoint.getClass().getAnnotation(Checkpoints.class);
    checkArgument(checkpoints != null, "%s is not annotated with @Checkpoints", checkpoint.getClass());
    return checkpoints;
  }

  /**
   * @return Metadata about this {@link Upgrade}
   */
  static Upgrades upgrades(final Upgrade upgrade) {
    Upgrades upgrades = upgrade.getClass().getAnnotation(Upgrades.class);
    checkArgument(upgrades != null, "%s is not annotated with @Upgrades", upgrade.getClass());
    return upgrades;
  }

  /**
   * Partial ordering for independent/unrelated {@link UpgradeStep}s (earliest first).
   */
  static int byVersion(final UpgradeStep lhs, final UpgradeStep rhs) {
    try {
      return VERSION_SCHEME.parseVersion(lhs.getVersion())
          .compareTo(VERSION_SCHEME.parseVersion(rhs.getVersion()));
    }
    catch (InvalidVersionSpecificationException e) {
      throw new IllegalArgumentException(e);
    }
  }
}

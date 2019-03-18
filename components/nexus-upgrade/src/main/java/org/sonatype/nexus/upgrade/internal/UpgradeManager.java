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

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.app.VersionComparator;
import org.sonatype.nexus.common.upgrade.Checkpoint;
import org.sonatype.nexus.common.upgrade.Checkpoints;
import org.sonatype.nexus.common.upgrade.DependsOn;
import org.sonatype.nexus.common.upgrade.Upgrade;
import org.sonatype.nexus.common.upgrade.Upgrades;
import org.sonatype.nexus.upgrade.UpgradeService;
import org.sonatype.nexus.upgrade.plan.DependencyResolver;
import org.sonatype.nexus.upgrade.plan.DependencySource;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import static com.google.common.base.Predicates.alwaysTrue;
import static com.google.common.collect.ImmutableBiMap.toImmutableBiMap;
import static com.google.common.collect.Sets.difference;
import static java.lang.System.lineSeparator;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.maxBy;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

/**
 * Manages {@link Upgrade}s and {@link Checkpoint}s for the {@link UpgradeService}.
 * 
 * @since 3.1
 */
@Named
@Singleton
public class UpgradeManager
    extends ComponentSupport
{
  private final BiMap<String, Checkpoint> checkpointIndex;

  private final BiMap<Upgrades, Upgrade> upgradeIndex;

  private final boolean warnOnMissingDependencies;

  private final Set<String> localModels;

  private final Set<String> clusteredModels;

  @Inject
  public UpgradeManager(final List<Checkpoint> managedCheckpoints,
                        final List<Upgrade> managedUpgrades,
                        @Named("${nexus.upgrade.warnOnMissingDependencies:-false}")
                        final boolean warnOnMissingDependencies)
  {
    final List<String> problems = new ArrayList<>();

    this.checkpointIndex = indexCheckpoints(managedCheckpoints, problems);
    this.upgradeIndex = indexUpgrades(managedUpgrades, problems);
    this.warnOnMissingDependencies = warnOnMissingDependencies;

    if (!problems.isEmpty()) {
      String message = String.format("Found %d problem(s) with upgrades:%n%s",
          problems.size(), problems.stream().collect(joining(lineSeparator())));
      throw new IllegalStateException(message);
    }

    // find any models that have local checkpoints
    this.localModels = checkpointIndex.values().stream()
        .map(c -> c.getClass().getAnnotation(Checkpoints.class))
        .filter(Checkpoints::local)
        .map(Checkpoints::model)
        .collect(toSet());

    // all other models are assumed to be clustered
    this.clusteredModels = Stream.concat(
        checkpointIndex.keySet().stream(),
        upgradeIndex.keySet().stream()
            .map(Upgrades::model))
        .filter(m -> !localModels.contains(m))
        .collect(toSet());
  }

  /**
   * Models that are considered local (non-clustered).
   */
  public Set<String> getLocalModels() {
    return localModels;
  }

  /**
   * Models that may be distributed across the cluster.
   */
  public Set<String> getClusteredModels() {
    return clusteredModels;
  }

  /**
   * Returns ordered list of upgrades that should be applied to the current installation.
   * 
   * @param modelVersions The models and versions currently installed
   * @param localOnly Whether only local upgrades should be selected
   * @return ordered list of upgrades that should be applied
   */
  public List<Upgrade> selectUpgrades(final Map<String, String> modelVersions, final boolean localOnly) {

    List<Upgrade> upgrades = upgradeIndex.entrySet().stream()
        .filter(e -> applies(modelVersions, e.getKey()))
        .map(e -> e.getValue())
        .collect(toList());

    List<String> problems = new ArrayList<>();
    upgrades.forEach(u -> checkUpgrade(u, problems));
    if (!problems.isEmpty()) {
      String message = String.format("Found %d problem(s) with upgrades:%n%s",
          problems.size(), problems.stream().collect(joining(lineSeparator())));
      throw new IllegalStateException(message);
    }

    Stream<UpgradeStep> upgradeSteps = order(modelVersions, upgrades);
    if (localOnly) {
      upgradeSteps = upgradeSteps.filter(step -> localModels.contains(step.getModel()));
    }

    return upgradeSteps
        .map(UpgradeStep::getUpgrade)
        .collect(toList());
  }

  /**
   * Returns list of checkpoints that should be taken before applying the scheduled upgrades.
   * 
   * @param upgrades The scheduled upgrades
   * @return list of checkpoints that should be taken
   */
  public List<Checkpoint> selectCheckpoints(final List<Upgrade> upgrades) {
    // used to avoid duplicates while maintaining incoming model order
    Set<String> candidates = new HashSet<>(checkpointIndex.keySet());

    return upgrades.stream()
        // widen to include dependencies explicitly marked as needing a checkpoint for the upgrade,
        // for example when upgrading a private model that's persisted inside a more generic model
        .flatMap(u -> filterModelDependencies(u, DependsOn::checkpoint))
        .filter(candidates::remove)
        .map(checkpointIndex::get)
        .collect(toList());
  }

  /**
   * Returns a map of model names to the most recent known version of that model.
   *
   * @since 3.4
   */
  public Map<String, String> latestKnownModelVersions() {
    return upgradeIndex.keySet().stream()
        .collect(
            groupingBy(
                Upgrades::model,
                collectingAndThen(
                    maxBy(UpgradeManager::byVersion),
                    step -> step.get().to())));
  }

  /**
   * Returns the model name for the given checkpoint.
   *
   * @since 3.13
   */
  public String getModel(final Checkpoint checkpoint) {
    return checkpointIndex.inverse().get(checkpoint);
  }

  /**
   * Returns the declared metadata for the given upgrade.
   *
   * @since 3.13
   */
  public Upgrades getMetadata(final Upgrade upgrade) {
    return upgradeIndex.inverse().get(upgrade);
  }

  /**
   * Indexes each {@link Checkpoint} registered in the system.
   */
  private BiMap<String, Checkpoint> indexCheckpoints(final List<Checkpoint> checkpoints,
                                                     final List<String> problems)
  {
    Map<String, List<Checkpoint>> byName = checkpoints.stream()
        .collect(groupingBy(c -> c.getClass().isAnnotationPresent(Checkpoints.class)
            ? c.getClass().getAnnotation(Checkpoints.class).model() : null));

    if (byName.containsKey(null)) {
      byName.remove(null).stream()
          .map(c -> String.format("Checkpoint %s is not annotated with @Checkpoints", className(c)))
          .collect(toCollection(() -> problems));
    }

    byName.entrySet().stream()
        .filter(e -> e.getValue().size() > 1)
        .map(e -> String.format("Checkpoint of model: %s duplicated by classes: %s",
            e.getKey(), classNames(e.getValue())))
        .collect(toCollection(() -> problems));

    return byName.entrySet().stream()
        .collect(toImmutableBiMap(e -> e.getKey(), e -> e.getValue().get(0)));
  }

  /**
   * Indexes each {@link Upgrade} registered in the system.
   */
  private BiMap<Upgrades, Upgrade> indexUpgrades(final List<Upgrade> upgrades,
                                                 final List<String> problems)
  {
    Map<Upgrades, List<Upgrade>> byAnnotation = upgrades.stream()
        .collect(groupingBy(c -> c.getClass().getAnnotation(Upgrades.class)));

    if (byAnnotation.containsKey(null)) {
      byAnnotation.remove(null).stream()
          .map(c -> String.format("Upgrade step %s is not annotated with @Upgrades", className(c)))
          .collect(toCollection(() -> problems));
    }

    byAnnotation.entrySet().stream()
        .filter(e -> e.getValue().size() > 1)
        .map(e -> String.format("Upgrade of model: %s from: %s to: %s duplicated by classes: %s",
            e.getKey().model(), e.getKey().from(), e.getKey().to(), classNames(e.getValue())))
        .collect(toCollection(() -> problems));

    return byAnnotation.entrySet().stream()
        .collect(toImmutableBiMap(e -> e.getKey(), e -> e.getValue().get(0)));
  }

  /**
   * Checks the given upgrade is consistent and triggers at least one checkpoint.
   */
  private void checkUpgrade(final Upgrade upgrade, final List<String> problems) {
    Upgrades metadata = getMetadata(upgrade);

    if (VersionComparator.INSTANCE.compare(metadata.to(), metadata.from()) <= 0) {
      problems.add(String.format("Upgrade step %s has invalid version: %s is not after %s",
          className(upgrade), metadata.to(), metadata.from()));
    }

    Set<String> modelDependencies = findModelDependencies(upgrade);
    Set<String> injectedDependencies = findInjectedDependencies(upgrade);

    Set<String> undeclaredModels = difference(injectedDependencies, modelDependencies);

    if (!undeclaredModels.isEmpty()) {
      String message = String.format("Upgrade step %s has undeclared model dependencies: %s",
          className(upgrade), undeclaredModels);

      if (warnOnMissingDependencies) {
        log.warn(message);
      }
      else {
        problems.add(message);
      }
    }

    if (selectCheckpoints(asList(upgrade)).isEmpty()) {
      problems.add(String.format("Upgrade step %s does not trigger a checkpoint", className(upgrade)));
    }
  }

  /**
   * Returns the names of any models explicitly declared as dependencies with {@link DependsOn}
   * as well as the particular model being upgraded, which is treated as an implicit dependency.
   */
  private Set<String> findModelDependencies(final Upgrade upgrade) {
    return filterModelDependencies(upgrade, alwaysTrue()).collect(toSet());
  }

  /**
   * Returns the names of models explicitly declared in matching {@link DependsOn} dependencies,
   * as well as the particular model being upgraded, which is treated as an implicit dependency.
   */
  private Stream<String> filterModelDependencies(final Upgrade upgrade, final Predicate<DependsOn> dependsOnFilter) {
    return Stream.concat(
        Stream.of(getMetadata(upgrade).model()),
        Stream.of(upgrade.getClass().getAnnotationsByType(DependsOn.class))
            .filter(dependsOnFilter)
            .map(DependsOn::model));
  }

  /**
   * Returns the names of any injected {@link Named} constructor parameters that match known models.
   */
  private Set<String> findInjectedDependencies(final Upgrade upgrade) {
    Optional<Constructor<?>> injectedConstructor = findInjectedConstructor(upgrade);
    if (!injectedConstructor.isPresent()) {
      return ImmutableSet.of(); // nothing injected
    }

    return Stream.of(injectedConstructor.get().getParameters())
        .filter(p -> p.isAnnotationPresent(Named.class))
        .map(p -> p.getAnnotation(Named.class).value())
        .filter(d -> clusteredModels.contains(d) || localModels.contains(d))
        .collect(toSet());
  }

  /**
   * Returns the optional constructor annotated with {@link Inject}.
   */
  private Optional<Constructor<?>> findInjectedConstructor(final Upgrade upgrade) {
    return Stream.of(upgrade.getClass().getDeclaredConstructors())
        .filter(c -> c.isAnnotationPresent(Inject.class))
        .findFirst();
  }

  /**
   * Orders the given upgrades so any dependent upgrades appear earlier on in the sequence.
   */
  private Stream<UpgradeStep> order(final Map<String, String> modelVersions, final List<Upgrade> upgrades)  {
    DependencyResolver<DependencySource<UpgradePoint>> resolver = new DependencyResolver<>();
    resolver.setWarnOnMissingDependencies(warnOnMissingDependencies);

    resolver.add(ImmutableList.of(new InitialStep(modelVersions)));
    resolver.add(upgrades.stream()
        .map(UpgradeStep::new)
        .sorted(UpgradeManager::byVersion)
        .collect(toList()));

    return resolver.resolve()
        .getOrdered().stream()
        .map(UpgradeStep::unwrap)
        .filter(Objects::nonNull);
  }

  /**
   * @return {@code true} if the upgrade applies on top of the current installation
   */
  private static boolean applies(final Map<String, String> modelVersions, final Upgrades metadata) {
    String current = modelVersions.getOrDefault(metadata.model(), "1.0");
    return VersionComparator.INSTANCE.compare(metadata.to(), current) > 0;
  }

  /**
   * Partial ordering for independent/unrelated {@link UpgradeStep}s (earliest first).
   */
  private static int byVersion(final UpgradeStep lhs, final UpgradeStep rhs) {
    return VersionComparator.INSTANCE.compare(lhs.getVersion(), rhs.getVersion());
  }

  /**
   * Partial ordering for independent/unrelated {@link Upgrades} (earliest first).
   */
  private static int byVersion(final Upgrades lhs, final Upgrades rhs) {
    return VersionComparator.INSTANCE.compare(lhs.to(), rhs.to());
  }

  /**
   * Returns the class name of the given element for logging.
   */
  private static String className(final Object element) {
    return element.getClass().getCanonicalName();
  }

  /**
   * Returns the class names of the given elements for logging.
   */
  private static String classNames(final List<?> elements) {
    return elements.stream()
        .map(UpgradeManager::className)
        .collect(joining(", "));
  }
}

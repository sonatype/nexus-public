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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Nullable;

import org.sonatype.nexus.common.upgrade.DependsOn;
import org.sonatype.nexus.common.upgrade.Upgrade;
import org.sonatype.nexus.common.upgrade.Upgrades;
import org.sonatype.nexus.upgrade.plan.Dependency;
import org.sonatype.nexus.upgrade.plan.DependencySource;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toCollection;

/**
 * Represents a single {@link Upgrade} step and its dependencies.
 * 
 * @since 3.1
 */
public class UpgradeStep
    implements UpgradePoint, DependencySource<UpgradePoint>
{
  private final Upgrade upgrade;

  private final Upgrades upgrades;

  private final DependsOn[] dependsOn;

  public UpgradeStep(final Upgrade upgrade) {
    this.upgrade = checkNotNull(upgrade);

    upgrades = upgrade.getClass().getAnnotation(Upgrades.class);
    dependsOn = upgrade.getClass().getAnnotationsByType(DependsOn.class);

    checkArgument(upgrades != null, "%s is not annotated with @Upgrades", upgrade.getClass());
  }

  @Nullable
  public static UpgradeStep unwrap(final DependencySource<UpgradePoint> source) {
    return source instanceof UpgradeStep ? (UpgradeStep) source : null;
  }

  public String getModel() {
    return upgrades.model();
  }

  public String getVersion() {
    return upgrades.to();
  }

  /**
   * @since 3.16
   */
  public Upgrade getUpgrade() {
    return upgrade;
  }

  @Override
  public List<Dependency<UpgradePoint>> getDependencies() {

    // explicit dependencies declared with @DependsOn
    List<Dependency<UpgradePoint>> dependencies = stream(dependsOn)
        .map(dep -> new UpgradeDependency(dep.model(), dep.version()))
        .collect(toCollection(ArrayList::new));

    // implicit base dependency implied by @Upgrades
    dependencies.add(new UpgradeDependency(upgrades.model(), upgrades.from()));

    return dependencies;
  }

  @Override
  public boolean satisfies(final String model, final String version) {
    return model.equals(getModel()) && version.equals(getVersion());
  }

  @Override
  public String toString() {
    return upgrade.getClass().getSimpleName() + '{' +
        "upgrades=" + upgrades +
        ", dependsOn=" + Arrays.toString(dependsOn) +
        '}';
  }
}

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

import org.sonatype.nexus.upgrade.plan.Dependency;
import org.sonatype.nexus.upgrade.plan.DependencyResolver;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Upgrade {@link Dependency} that can be resolved by the {@link DependencyResolver}.
 * 
 * @since 3.1
 */
public class UpgradeDependency
    implements Dependency<UpgradePoint>
{
  private final String model;

  private final String version;

  public UpgradeDependency(final String model, final String version) {
    this.model = checkNotNull(model);
    this.version = checkNotNull(version);
  }

  @Override
  public boolean satisfiedBy(final UpgradePoint upgradePoint) {
    return upgradePoint.satisfies(model, version);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "model='" + model + '\'' +
        ", version='" + version + '\'' +
        '}';
  }
}

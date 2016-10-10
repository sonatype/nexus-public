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

import org.sonatype.nexus.upgrade.plan.Dependency;
import org.sonatype.nexus.upgrade.plan.DependencySource;

import com.google.common.collect.ImmutableList;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Represents the models and versions in the current Nexus installation.
 * 
 * If a specific model version is not provided then it defaults to "1.0".
 * 
 * @since 3.1
 */
public class InitialStep
    implements UpgradePoint, DependencySource<UpgradePoint>
{
  private final Map<String, String> modelVersions;

  public InitialStep(final Map<String, String> modelVersions) {
    this.modelVersions = checkNotNull(modelVersions);
  }

  @Override
  public List<Dependency<UpgradePoint>> getDependencies() {
    return ImmutableList.of();
  }

  @Override
  public boolean satisfies(final String model, final String version) {
    return VersionComparator.INSTANCE.compare(modelVersions.getOrDefault(model, "1.0"), version) >= 0;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "modelVersions=" + modelVersions +
        '}';
  }
}

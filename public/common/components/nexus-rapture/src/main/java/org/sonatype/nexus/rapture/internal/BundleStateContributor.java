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
package org.sonatype.nexus.rapture.internal;

import java.util.List;
import java.util.Map;

import org.sonatype.nexus.bootstrap.entrypoint.edition.NexusEditionSelector;
import org.sonatype.nexus.rapture.StateContributor;

import com.google.common.collect.ImmutableMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class BundleStateContributor
    implements StateContributor
{
  public static final String STATE_ID = "activeBundles";

  public static final String NEXUS_MODULE_PREFIX = "nexus-";

  private List<String> modules;

  private final NexusEditionSelector nexusEditionSelector;

  @Autowired
  public BundleStateContributor(final NexusEditionSelector nexusEditionSelector) {
    this.nexusEditionSelector = nexusEditionSelector;
  }

  @Override
  public Map<String, Object> getState() {
    return ImmutableMap.of(STATE_ID, getModules());
  }

  private List<String> getModules() {
    if (modules == null) {
      modules = nexusEditionSelector.getCurrent()
          .getModules()
          .stream()
          .filter(BundleStateContributor::isInternalModule)
          .toList();
    }
    return modules;
  }

  private static boolean isInternalModule(final String moduleName) {
    // Filter to include only internal Nexus modules (exclude external dependencies)
    return moduleName.startsWith(NEXUS_MODULE_PREFIX);
  }
}

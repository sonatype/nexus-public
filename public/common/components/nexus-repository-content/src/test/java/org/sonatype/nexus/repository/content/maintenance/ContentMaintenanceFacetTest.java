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
package org.sonatype.nexus.repository.content.maintenance;

import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.Component;

import org.junit.Test;

import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;

/**
 * Verifies the Trash/restore lifecycle hooks on {@link ContentMaintenanceFacet} default to a no-op so a format
 * that does not maintain aggregate metadata (or a reference index) is unaffected by trash, restore and purge
 * (NEXUS-55233). Formats that need to react override these; the base contract must simply not throw.
 */
public class ContentMaintenanceFacetTest
{
  private final ContentMaintenanceFacet underTest = mock(ContentMaintenanceFacet.class, CALLS_REAL_METHODS);

  @Test
  public void defaultLifecycleHooks_areNoOps() {
    Asset asset = mock(Asset.class);
    Component component = mock(Component.class);

    // None of these must throw for a format that opts out of trash-aware metadata maintenance.
    underTest.afterAssetTrashed(asset);
    underTest.afterAssetRestored(asset);
    underTest.afterAssetPurged(asset);
    underTest.afterComponentTrashed(component);
    underTest.afterComponentRestored(component);
  }
}

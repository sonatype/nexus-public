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
package org.sonatype.nexus.cleanup.preview;

import java.util.Map;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonatype.nexus.common.app.FeatureFlags.CLEANUP_PREVIEW_ENABLED;
import static org.sonatype.nexus.common.app.FeatureFlags.CLEANUP_RETAIN_ALL_FORMATS;

public class CleanupPreviewStateContributorTest
{
  @Test
  public void testGetStateBothFlagsTrue() {
    CleanupPreviewStateContributor underTest = new CleanupPreviewStateContributor(true, true);

    Map<String, Object> state = underTest.getState();

    assertThat(state).containsEntry(CLEANUP_PREVIEW_ENABLED, true);
    assertThat(state).containsEntry(CLEANUP_RETAIN_ALL_FORMATS, true);
  }

  @Test
  public void testGetStateBothFlagsFalse() {
    CleanupPreviewStateContributor underTest = new CleanupPreviewStateContributor(false, false);

    Map<String, Object> state = underTest.getState();

    assertThat(state).containsEntry(CLEANUP_PREVIEW_ENABLED, false);
    assertThat(state).containsEntry(CLEANUP_RETAIN_ALL_FORMATS, false);
  }

  @Test
  public void testGetStateMixedFlags() {
    CleanupPreviewStateContributor underTest = new CleanupPreviewStateContributor(true, false);

    Map<String, Object> state = underTest.getState();

    assertThat(state).containsEntry(CLEANUP_PREVIEW_ENABLED, true);
    assertThat(state).containsEntry(CLEANUP_RETAIN_ALL_FORMATS, false);
  }

  @Test
  public void testGetStateContainsOnlyExpectedKeys() {
    CleanupPreviewStateContributor underTest = new CleanupPreviewStateContributor(true, true);

    Map<String, Object> state = underTest.getState();

    assertThat(state).hasSize(2);
    assertThat(state).containsOnlyKeys(CLEANUP_PREVIEW_ENABLED, CLEANUP_RETAIN_ALL_FORMATS);
  }
}

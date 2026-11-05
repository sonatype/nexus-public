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
package org.sonatype.nexus.repository.content.store;

import java.util.List;

import org.sonatype.nexus.common.app.trash.TrashSchemaVersions;
import org.sonatype.nexus.common.db.DatabaseCheck;
import org.sonatype.nexus.common.upgrade.events.UpgradeCompletedEvent;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.Silent.class)
public class TrashSchemaStateTest
{
  @Mock
  private DatabaseCheck databaseCheck;

  private UpgradeCompletedEvent upgradeTo(final String version) {
    return new UpgradeCompletedEvent("admin", version, List.of());
  }

  @Test
  public void flagOff_isAlwaysFalse() {
    TrashSchemaState state = new TrashSchemaState(false, databaseCheck);

    assertThat(state.isTrashAvailable(), is(false));
  }

  @Test
  public void isSchemaReached_ignoresFeatureFlag() {
    // NEXUS-55233: read filtering keys on schema presence, independent of the feature flag, so Trashed content
    // stays hidden even when the flag is off.
    TrashSchemaState state = new TrashSchemaState(false, databaseCheck);
    when(databaseCheck.isAtLeast(TrashSchemaVersions.CONTENT)).thenReturn(true);

    assertThat(state.isSchemaReached(), is(true));
    // ...while isTrashAvailable() (flag-gated) remains false with the flag off
    assertThat(state.isTrashAvailable(), is(false));
  }

  @Test
  public void isSchemaReached_belowAnchor_isFalse() {
    TrashSchemaState state = new TrashSchemaState(false, databaseCheck);
    when(databaseCheck.isAtLeast(TrashSchemaVersions.CONTENT)).thenReturn(false);

    assertThat(state.isSchemaReached(), is(false));
  }

  @Test
  public void upgradeEvent_flipsSchemaReached_evenWhenFlagOff() {
    // A ZDU node with the flag off must start hiding Trashed content once the columns are migrated.
    TrashSchemaState state = new TrashSchemaState(false, databaseCheck);
    when(databaseCheck.isAtLeast(TrashSchemaVersions.CONTENT)).thenReturn(false);
    assertThat(state.isSchemaReached(), is(false));

    state.on(upgradeTo(TrashSchemaVersions.CONTENT));

    assertThat(state.isSchemaReached(), is(true));
  }

  @Test
  public void flagOn_schemaBelowAnchor_isFalse() {
    TrashSchemaState state = new TrashSchemaState(true, databaseCheck);
    when(databaseCheck.isAtLeast(TrashSchemaVersions.CONTENT)).thenReturn(false);

    assertThat(state.isTrashAvailable(), is(false));
  }

  @Test
  public void flagOn_schemaAtAnchor_isTrue() {
    TrashSchemaState state = new TrashSchemaState(true, databaseCheck);
    when(databaseCheck.isAtLeast(TrashSchemaVersions.CONTENT)).thenReturn(true);

    assertThat(state.isTrashAvailable(), is(true));
  }

  @Test
  public void upgradeEvent_flipsFromPayload_withoutRequeryingDatabaseCheck() {
    TrashSchemaState state = new TrashSchemaState(true, databaseCheck);
    when(databaseCheck.isAtLeast(TrashSchemaVersions.CONTENT)).thenReturn(false);

    // initial lazy read caches false (one DatabaseCheck call)
    assertThat(state.isTrashAvailable(), is(false));

    // completing an upgrade at/after the anchor flips it purely from the event's target version
    state.on(upgradeTo(TrashSchemaVersions.CONTENT));

    assertThat(state.isTrashAvailable(), is(true));
    // no re-query on the event path or subsequent reads -> only the single initial lazy read happened
    verify(databaseCheck, times(1)).isAtLeast(TrashSchemaVersions.CONTENT);
  }

  @Test
  public void upgradeEvent_belowAnchor_staysFalse() {
    TrashSchemaState state = new TrashSchemaState(true, databaseCheck);

    state.on(upgradeTo("2.171"));

    assertThat(state.isTrashAvailable(), is(false));
  }

  @Test
  public void upgradeEvent_belowAnchor_afterReached_doesNotFlipBack() {
    // Schema presence is monotonic: once a node has reached the anchor, a later event carrying a version below the
    // anchor (e.g. a lagging/partial ZDU event) must NOT flip schemaReached back to false and momentarily resurface
    // Trashed content (NEXUS-55410).
    TrashSchemaState state = new TrashSchemaState(true, databaseCheck);
    state.on(upgradeTo(TrashSchemaVersions.CONTENT));
    assertThat(state.isTrashAvailable(), is(true));

    state.on(upgradeTo("2.171"));

    assertThat(state.isSchemaReached(), is(true));
    assertThat(state.isTrashAvailable(), is(true));
  }

  @Test
  public void upgradeEvent_withoutSchemaVersion_leavesStateUnchanged() {
    TrashSchemaState state = new TrashSchemaState(true, databaseCheck);
    when(databaseCheck.isAtLeast(TrashSchemaVersions.CONTENT)).thenReturn(false);
    assertThat(state.isTrashAvailable(), is(false));

    // an event carrying no target version (e.g. first system start) must not change the cached value
    state.on(new UpgradeCompletedEvent("admin", null, List.of()));

    assertThat(state.isTrashAvailable(), is(false));
  }
}

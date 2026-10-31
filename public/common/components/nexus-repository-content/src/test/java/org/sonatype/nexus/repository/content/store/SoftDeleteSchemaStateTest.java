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

import org.sonatype.nexus.common.db.DatabaseCheck;
import org.sonatype.nexus.common.upgrade.events.UpgradeCompletedEvent;
import org.sonatype.nexus.repository.content.upgrades.SoftDeleteColumnsMigrationStep_2_172;

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
public class SoftDeleteSchemaStateTest
{
  @Mock
  private DatabaseCheck databaseCheck;

  private UpgradeCompletedEvent upgradeTo(final String version) {
    return new UpgradeCompletedEvent("admin", version, List.of());
  }

  @Test
  public void flagOff_isAlwaysFalse() {
    SoftDeleteSchemaState state = new SoftDeleteSchemaState(false, databaseCheck);

    assertThat(state.isSoftDeleteAvailable(), is(false));
  }

  @Test
  public void flagOn_schemaBelowAnchor_isFalse() {
    SoftDeleteSchemaState state = new SoftDeleteSchemaState(true, databaseCheck);
    when(databaseCheck.isAtLeast(SoftDeleteColumnsMigrationStep_2_172.VERSION)).thenReturn(false);

    assertThat(state.isSoftDeleteAvailable(), is(false));
  }

  @Test
  public void flagOn_schemaAtAnchor_isTrue() {
    SoftDeleteSchemaState state = new SoftDeleteSchemaState(true, databaseCheck);
    when(databaseCheck.isAtLeast(SoftDeleteColumnsMigrationStep_2_172.VERSION)).thenReturn(true);

    assertThat(state.isSoftDeleteAvailable(), is(true));
  }

  @Test
  public void upgradeEvent_flipsFromPayload_withoutRequeryingDatabaseCheck() {
    SoftDeleteSchemaState state = new SoftDeleteSchemaState(true, databaseCheck);
    when(databaseCheck.isAtLeast(SoftDeleteColumnsMigrationStep_2_172.VERSION)).thenReturn(false);

    // initial lazy read caches false (one DatabaseCheck call)
    assertThat(state.isSoftDeleteAvailable(), is(false));

    // completing an upgrade at/after the anchor flips it purely from the event's target version
    state.on(upgradeTo(SoftDeleteColumnsMigrationStep_2_172.VERSION));

    assertThat(state.isSoftDeleteAvailable(), is(true));
    // no re-query on the event path or subsequent reads -> only the single initial lazy read happened
    verify(databaseCheck, times(1)).isAtLeast(SoftDeleteColumnsMigrationStep_2_172.VERSION);
  }

  @Test
  public void upgradeEvent_belowAnchor_staysFalse() {
    SoftDeleteSchemaState state = new SoftDeleteSchemaState(true, databaseCheck);

    state.on(upgradeTo("2.171"));

    assertThat(state.isSoftDeleteAvailable(), is(false));
  }

  @Test
  public void upgradeEvent_withoutSchemaVersion_leavesStateUnchanged() {
    SoftDeleteSchemaState state = new SoftDeleteSchemaState(true, databaseCheck);
    when(databaseCheck.isAtLeast(SoftDeleteColumnsMigrationStep_2_172.VERSION)).thenReturn(false);
    assertThat(state.isSoftDeleteAvailable(), is(false));

    // an event carrying no target version (e.g. first system start) must not change the cached value
    state.on(new UpgradeCompletedEvent("admin", null, List.of()));

    assertThat(state.isSoftDeleteAvailable(), is(false));
  }
}

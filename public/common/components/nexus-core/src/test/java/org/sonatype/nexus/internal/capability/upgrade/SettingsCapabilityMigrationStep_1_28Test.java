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
package org.sonatype.nexus.internal.capability.upgrade;

import java.util.HashMap;
import java.util.Map;

import org.sonatype.nexus.capability.CapabilityIdentity;
import org.sonatype.nexus.internal.capability.storage.CapabilityStorageItem;
import org.sonatype.nexus.internal.capability.storage.CapabilityStorageItemData;
import org.sonatype.nexus.internal.capability.storage.upgrade.UpgradeCapabilityStorage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SettingsCapabilityMigrationStep_1_28Test
{
  @Mock
  private UpgradeCapabilityStorage capabilityStorage;

  @Captor
  private ArgumentCaptor<CapabilityStorageItem> itemCaptor;

  private SettingsCapabilityMigrationStep_1_28 underTest;

  @BeforeEach
  void setUp() {
    underTest = new SettingsCapabilityMigrationStep_1_28(capabilityStorage);
  }

  @Test
  void migrate_addsDefaultTimeoutsWhenMissing() throws Exception {
    CapabilityIdentity id = new CapabilityIdentity("id-1");
    when(capabilityStorage.getAll())
        .thenReturn(singletonMap(id, item("rapture.settings", new HashMap<>())));

    underTest.migrate(null);

    verify(capabilityStorage).update(eq(id), itemCaptor.capture());
    assertThat(itemCaptor.getValue().getProperties())
        .containsEntry("requestTimeout", "60")
        .containsEntry("longRequestTimeout", "180");
  }

  @Test
  void migrate_doesNotOverwriteExistingTimeouts() throws Exception {
    CapabilityIdentity id = new CapabilityIdentity("id-1");
    Map<String, String> existing = new HashMap<>();
    existing.put("requestTimeout", "99");
    when(capabilityStorage.getAll())
        .thenReturn(singletonMap(id, item("rapture.settings", existing)));

    underTest.migrate(null);

    verify(capabilityStorage).update(eq(id), itemCaptor.capture());
    assertThat(itemCaptor.getValue().getProperties())
        .containsEntry("requestTimeout", "99")
        .containsEntry("longRequestTimeout", "180");
  }

  @Test
  void migrate_noRaptureSettings_doesNothing() throws Exception {
    when(capabilityStorage.getAll())
        .thenReturn(singletonMap(new CapabilityIdentity("id-2"), item("other.capability", new HashMap<>())));

    underTest.migrate(null);

    verify(capabilityStorage, never()).update(any(), any());
  }

  private static CapabilityStorageItem item(final String type, final Map<String, String> properties) {
    CapabilityStorageItemData data = new CapabilityStorageItemData();
    data.setVersion(1);
    data.setType(type);
    data.setEnabled(true);
    data.setNotes("notes");
    data.setProperties(properties);
    return data;
  }
}

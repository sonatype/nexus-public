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
package org.sonatype.nexus.internal.capability.storage.upgrade;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.sonatype.nexus.capability.CapabilityIdentity;
import org.sonatype.nexus.internal.capability.storage.CapabilityStorageItem;
import org.sonatype.nexus.internal.capability.storage.CapabilityStorageItemDAO;
import org.sonatype.nexus.internal.capability.storage.CapabilityStorageItemTestSupport;
import org.sonatype.nexus.testdb.DataSessionConfiguration;
import org.sonatype.nexus.testdb.DatabaseTest;
import org.sonatype.nexus.testdb.TestDataSessionSupplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Real-database tests for {@link UpgradeCapabilityStorage} against the {@code capability_storage_item}
 * table.
 */
class UpgradeCapabilityStorageTest
{
  @DataSessionConfiguration(daos = {CapabilityStorageItemDAO.class})
  TestDataSessionSupplier dataSessionSupplier;

  private UpgradeCapabilityStorage store() {
    return new UpgradeCapabilityStorage(dataSessionSupplier);
  }

  @DatabaseTest
  void getAll_whenEmptyTable_returnsEmpty() {
    assertThat(store().getAll()).isEmpty();
  }

  @DatabaseTest
  void getAll_whenRowPresent_mapsColumns() throws Exception {
    UUID id = CapabilityStorageItemTestSupport.insert(dataSessionSupplier, "rapture.settings",
        Map.of("requestTimeout", "60"));

    Map<CapabilityIdentity, CapabilityStorageItem> all = store().getAll();

    assertThat(all).hasSize(1);
    CapabilityStorageItem item = all.get(new CapabilityIdentity(id.toString()));
    assertThat(item).isNotNull();
    assertThat(item.getType()).isEqualTo("rapture.settings");
    assertThat(item.isEnabled()).isTrue();
    assertThat(item.getProperties()).containsEntry("requestTimeout", "60");
  }

  @DatabaseTest
  void update_whenPropertiesChanged_persists() throws Exception {
    UUID id = CapabilityStorageItemTestSupport.insert(dataSessionSupplier, "rapture.settings",
        new HashMap<>(Map.of("a", "1")));
    CapabilityIdentity identity = new CapabilityIdentity(id.toString());

    CapabilityStorageItem item = store().getAll().get(identity);
    item.getProperties().put("b", "2");
    assertThat(store().update(identity, item)).isTrue();

    CapabilityStorageItem reread = store().getAll().get(identity);
    assertThat(reread.getProperties()).containsEntry("a", "1").containsEntry("b", "2");
  }

  @DatabaseTest
  void remove_whenPresent_deletesRow() throws Exception {
    UUID id = CapabilityStorageItemTestSupport.insert(dataSessionSupplier, "test", Map.of());
    CapabilityIdentity identity = new CapabilityIdentity(id.toString());

    assertThat(store().remove(identity)).isTrue();
    assertThat(store().getAll()).isEmpty();
    assertThat(store().remove(identity)).isFalse();
  }

  @DatabaseTest
  void removeAll_batchesDeletesAndCountsOnlyExistingRows() throws Exception {
    UUID a = CapabilityStorageItemTestSupport.insert(dataSessionSupplier, "test", Map.of("k", "a"));
    UUID b = CapabilityStorageItemTestSupport.insert(dataSessionSupplier, "test", Map.of("k", "b"));
    UUID c = CapabilityStorageItemTestSupport.insert(dataSessionSupplier, "test", Map.of("k", "c"));

    // Remove a and b plus one absent id in a single batch: the count reflects only rows actually deleted.
    int removed = store().removeAll(List.of(
        new CapabilityIdentity(a.toString()),
        new CapabilityIdentity(b.toString()),
        new CapabilityIdentity(UUID.randomUUID().toString())));

    assertThat(removed).isEqualTo(2);
    Map<CapabilityIdentity, CapabilityStorageItem> remaining = store().getAll();
    assertThat(remaining).hasSize(1);
    assertThat(remaining).containsKey(new CapabilityIdentity(c.toString()));
  }

  @DatabaseTest
  void removeAll_emptyCollection_isNoOp() throws Exception {
    CapabilityStorageItemTestSupport.insert(dataSessionSupplier, "test", Map.of());
    assertThat(store().removeAll(List.of())).isZero();
    assertThat(store().getAll()).hasSize(1);
  }
}

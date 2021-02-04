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
package org.sonatype.nexus.internal.capability;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.sonatype.nexus.capability.CapabilityIdentity;
import org.sonatype.nexus.common.entity.EntityUUID;
import org.sonatype.nexus.internal.capability.storage.CapabilityStorage;
import org.sonatype.nexus.internal.capability.storage.CapabilityStorageItem;
import org.sonatype.nexus.internal.capability.storage.CapabilityStorageItemData;
import org.sonatype.nexus.supportzip.datastore.JsonExporter;

import com.google.common.collect.ImmutableMap;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests validity of Serialization/Deserialization CapabilityStorage by {@link CapabilityStorageExport}
 */
public class CapabilityStorageExportTest
{
  private final JsonExporter jsonExporter = new JsonExporter();

  private File binFile;

  @Before
  public void setup() throws IOException {
    binFile = File.createTempFile("CapabilityStorage", ".json");
  }

  @After
  public void tearDown() {
    binFile.delete();
  }

  @Test
  public void testExportImportToJson() throws Exception {
    Map<CapabilityIdentity, CapabilityStorageItem> capabilities = new HashMap<>(2);
    capabilities.put(new CapabilityIdentity("one"), createCapabilityWithSensitiveData());
    capabilities.put(new CapabilityIdentity("two"), createCapabilityWithoutSensitiveData());

    CapabilityStorage capabilityStorage = mock(CapabilityStorage.class);
    when(capabilityStorage.getAll()).thenReturn(capabilities);

    CapabilityStorageExport exporter = new CapabilityStorageExport(capabilityStorage);
    exporter.export(binFile);
    List<CapabilityStorageItemData> importedItems =
        jsonExporter.importFromJson(binFile, CapabilityStorageItemData.class);

    assertThat(importedItems.stream().map(CapabilityStorageItem::getVersion).collect(toList()),
        containsInAnyOrder(1, 2));
    assertThat(importedItems.stream().map(CapabilityStorageItem::getType).collect(toList()),
        containsInAnyOrder("testing 1", "testing 2"));
    assertThat(importedItems.stream().map(CapabilityStorageItem::isEnabled).collect(toList()),
        containsInAnyOrder(true, false));
    assertThat(importedItems.stream().map(CapabilityStorageItem::getNotes).collect(toList()),
        containsInAnyOrder("notes 1", "notes 2"));
    List<Map<String, String>> attributes =
        importedItems.stream().map(CapabilityStorageItem::getProperties).collect(toList());
    assertThat(attributes.toString(), allOf(containsString("Capability"), containsString("Testing")));
    // make sure sensitive data is not serialized
    assertThat(attributes.toString(), not(containsString("admin123")));
  }

  private CapabilityStorageItemData createCapabilityWithSensitiveData() {
    CapabilityStorageItemData item = new CapabilityStorageItemData();
    Map<String, String> sensitiveData = ImmutableMap.of(
        "password", "admin123",
        "secret", "admin123",
        "bearerToken", "admin123");
    item.setId(new EntityUUID(UUID.randomUUID()));
    item.setVersion(2);
    item.setType("testing 2");
    item.setEnabled(false);
    item.setNotes("notes 2");
    item.setProperties(sensitiveData);

    return item;
  }

  private CapabilityStorageItemData createCapabilityWithoutSensitiveData() {
    CapabilityStorageItemData item = new CapabilityStorageItemData();
    Map<String, String> sensitiveData = ImmutableMap.of(
        "title", "Capability",
        "name", "Testing");
    item.setId(new EntityUUID(UUID.randomUUID()));
    item.setVersion(1);
    item.setType("testing 1");
    item.setEnabled(true);
    item.setNotes("notes 1");
    item.setProperties(sensitiveData);

    return item;
  }
}

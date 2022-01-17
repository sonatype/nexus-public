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
package org.sonatype.nexus.repository.internal.blobstore;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.common.entity.EntityUUID;
import org.sonatype.nexus.repository.blobstore.BlobStoreConfigurationStore;
import org.sonatype.nexus.supportzip.datastore.JsonExporter;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests validity of Serialization/Deserialization {@link BlobStoreConfiguration}
 * by {@link BlobStoreConfigurationExport}
 */
public class BlobStoreConfigurationExportTest
{
  private final JsonExporter jsonExporter = new JsonExporter();

  private File jsonFile;

  @Before
  public void setup() throws IOException {
    jsonFile = File.createTempFile("BlobStoreConfiguration", ".json");
  }

  @After
  public void tearDown() {
    jsonFile.delete();
  }

  @Test
  public void testExportImportToJson() throws Exception {
    List<BlobStoreConfiguration> configurationData = Arrays.asList(
        generateConfigData("test1", "TEST_1"),
        generateConfigData("test2", "TEST_2"));

    BlobStoreConfigurationStore configurationStore = mock(BlobStoreConfigurationStore.class);
    when(configurationStore.list()).thenReturn(configurationData);

    BlobStoreConfigurationExport exporter = new BlobStoreConfigurationExport(configurationStore);
    exporter.export(jsonFile);
    List<BlobStoreConfigurationData> importedData =
        jsonExporter.importFromJson(jsonFile, BlobStoreConfigurationData.class);

    assertThat(importedData.stream().map(BlobStoreConfiguration::getName).collect(Collectors.toList()),
        containsInAnyOrder("test1", "test2"));
    assertThat(importedData.stream().map(BlobStoreConfiguration::getType).collect(Collectors.toList()),
        containsInAnyOrder("TEST_1", "TEST_2"));
    assertThat(importedData.stream().map(BlobStoreConfiguration::isWritable).collect(Collectors.toList()),
        not(contains(false)));

    List<Map<String, Map<String, Object>>> serializedAttrs = importedData.stream()
        .map(BlobStoreConfiguration::getAttributes)
        .collect(Collectors.toList());
    assertThat(serializedAttrs.toString(), allOf(
        containsString("metadata"),
        containsString("size"),
        containsString("10")));
    // make sure sensitive data is not serialized
    assertThat(serializedAttrs.toString(), not(containsString("admin123")));
  }

  private BlobStoreConfiguration generateConfigData(final String name, final String type) {
    BlobStoreConfigurationData configuration = new BlobStoreConfigurationData();
    configuration.setId(new EntityUUID(UUID.randomUUID()));
    configuration.setName(name);
    configuration.setType(type);
    configuration.setWritable(true);
    Map<String, Map<String, Object>> attributes = new HashMap<>();
    attributes.put("login", Collections.singletonMap("password", "admin123"));
    attributes.put("user", Collections.singletonMap("secret", "admin123"));
    attributes.put("metadata", Collections.singletonMap("size", 10));
    configuration.setAttributes(attributes);

    return configuration;
  }
}

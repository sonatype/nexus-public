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
package org.sonatype.nexus.internal.selector;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.sonatype.nexus.selector.SelectorConfiguration;
import org.sonatype.nexus.selector.SelectorConfigurationStore;
import org.sonatype.nexus.supportzip.datastore.JsonExporter;

import com.google.common.collect.ImmutableMap;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests validity of Serialization/Deserialization {@link SelectorConfiguration} by {@link SelectorConfigurationExport}
 */
public class SelectorConfigurationExportTest
{
  private final JsonExporter jsonExporter = new JsonExporter();

  private File jsonFile;

  @Before
  public void setup() throws IOException {
    jsonFile = File.createTempFile("SamlUser", ".json");
  }

  @After
  public void tearDown() {
    jsonFile.delete();
  }

  @Test
  public void testExportImportToJson() throws Exception {
    SelectorConfiguration config1 = createSelectorConfiguration("config_1");
    SelectorConfiguration config2 = createSelectorConfiguration("config_2");
    List<SelectorConfiguration> selectorConfigurations = Arrays.asList(config1, config2);

    SelectorConfigurationStore store = mock(SelectorConfigurationStore.class);
    when(store.browse()).thenReturn(selectorConfigurations);

    SelectorConfigurationExport exporter = new SelectorConfigurationExport(store);
    exporter.export(jsonFile);
    List<SelectorConfigurationData> importedData =
        jsonExporter.importFromJson(jsonFile, SelectorConfigurationData.class);

    assertThat(importedData.size(), is(2));
    SelectorConfigurationData importedData1 = importedData.get(0);
    SelectorConfigurationData importedData2 = importedData.get(0);
    assertThat(importedData1.equals(config1) || importedData1.equals(config2), is(true));
    assertThat(importedData2.equals(config1) || importedData2.equals(config2), is(true));
  }

  private SelectorConfiguration createSelectorConfiguration(final String name) {
    SelectorConfigurationData configurationData = new SelectorConfigurationData();
    configurationData.setName(name);
    configurationData.setDescription("Description");
    configurationData.setType("type");
    configurationData.setAttributes(ImmutableMap.of("prop1", "value1", "prop2", "value2"));

    return configurationData;
  }
}

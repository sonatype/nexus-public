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
package org.sonatype.nexus.repository.config;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.sonatype.nexus.common.entity.EntityUUID;
import org.sonatype.nexus.repository.config.internal.ConfigurationData;
import org.sonatype.nexus.supportzip.datastore.JsonExporter;

import com.google.common.collect.ImmutableList;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests validity of Serialization/Deserialization ConfigurationData by {@link ConfigurationExport}
 */
public class ConfigurationExportTest
{
  private final JsonExporter jsonExporter = new JsonExporter();

  private File binFile;

  @Before
  public void setup() throws IOException {
    binFile = File.createTempFile("ConfigurationData", ".json");
  }

  @After
  public void tearDown() {
    binFile.delete();
  }

  @Test
  public void testExportImportToJson() throws Exception {
    EntityUUID routingRuleId = new EntityUUID();
    List<Configuration> configurationData = ImmutableList.of(
        generateConfigData("hosted-repo", "hosted", routingRuleId),
        generateConfigData("proxy-repo", "proxy", routingRuleId),
        generateConfigData("group-repo", "group", routingRuleId));

    ConfigurationStore configurationStore = mock(ConfigurationStore.class);
    when(configurationStore.list()).thenReturn(configurationData);

    ConfigurationExport exportConfigurationData = new ConfigurationExport(configurationStore);

    exportConfigurationData.export(binFile);
    List<ConfigurationData> importedConfigurationData = jsonExporter.importFromJson(binFile, ConfigurationData.class);
    assertThat(importedConfigurationData.stream().map(Configuration::getRepositoryName).collect(Collectors.toList()),
        containsInAnyOrder("hosted-repo", "proxy-repo", "group-repo"));
    assertThat(importedConfigurationData.stream().map(Configuration::getRecipeName).collect(Collectors.toList()),
        containsInAnyOrder("hosted", "proxy", "group"));
    assertThat(importedConfigurationData.stream().map(Configuration::isOnline).collect(Collectors.toList()),
        not(contains(false)));
    importedConfigurationData.forEach(data -> assertThat(data.getRoutingRuleId(), is(routingRuleId)));

    List<Map<String, Map<String, Object>>> serializedAttrs = importedConfigurationData.stream()
        .map(Configuration::getAttributes)
        .collect(Collectors.toList());
    assertThat(serializedAttrs.toString(), allOf(
        containsString("metadata"),
        containsString("size"),
        containsString("10")));
    // make sure sensitive data is not serialized
    assertThat(serializedAttrs.toString(), not(containsString("admin123")));
  }

  private Configuration generateConfigData(final String name, final String recipe, final EntityUUID routingRuleId) {
    Map<String, Map<String, Object>> attributes = new HashMap<>();
    attributes.put("login", Collections.singletonMap("password", "admin123"));
    attributes.put("user", Collections.singletonMap("secret", "admin123"));
    attributes.put("metadata", Collections.singletonMap("size", 10));
    ConfigurationData configurationData = new ConfigurationData();
    configurationData.setId(new EntityUUID(UUID.randomUUID()));
    configurationData.setName(name);
    configurationData.setRecipeName(recipe);
    configurationData.setOnline(true);
    configurationData.setAttributes(attributes);
    configurationData.setRoutingRuleId(routingRuleId);

    return configurationData;
  }
}

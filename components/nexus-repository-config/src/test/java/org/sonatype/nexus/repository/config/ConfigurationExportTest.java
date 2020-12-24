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
import java.util.Optional;
import java.util.UUID;

import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.common.entity.EntityUUID;
import org.sonatype.nexus.repository.config.ConfigurationExport.Repository;
import org.sonatype.nexus.repository.config.internal.ConfigurationData;
import org.sonatype.nexus.repository.routing.RoutingMode;
import org.sonatype.nexus.repository.routing.RoutingRule;
import org.sonatype.nexus.repository.routing.RoutingRuleStore;
import org.sonatype.nexus.repository.routing.internal.RoutingRuleData;
import org.sonatype.nexus.supportzip.datastore.JsonExporter;

import com.google.common.collect.ImmutableList;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
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
    RoutingRule rule1 = createRoutingRule("RULE_1");
    RoutingRule rule2 = createRoutingRule("RULE_2");
    RoutingRule rule3 = createRoutingRule("RULE_3");
    List<RoutingRule> routingRules = ImmutableList.of(rule1, rule2, rule3);

    RoutingRuleStore routingRuleStore = mock(RoutingRuleStore.class);
    when(routingRuleStore.list()).thenReturn(routingRules);

    Configuration hostedConfig = generateConfigData("hosted-repo", "hosted", rule1.id());
    Configuration proxyConfig = generateConfigData("proxy-repo", "proxy", rule2.id());
    Configuration groupConfig = generateConfigData("group-repo", "group", null);
    List<Configuration> configurations = ImmutableList.of(hostedConfig, proxyConfig, groupConfig);

    ConfigurationStore configurationStore = mock(ConfigurationStore.class);
    when(configurationStore.list()).thenReturn(configurations);

    ConfigurationExport exportConfigurationData = new ConfigurationExport(configurationStore, routingRuleStore);

    exportConfigurationData.export(binFile);
    List<ConfigurationExport.Repository> importedConfigurationData =
        jsonExporter.importFromJson(binFile, ConfigurationExport.Repository.class);

    // check hosted repository data
    Optional<Repository> hostedRepoOpt = importedConfigurationData.stream()
        .filter(data -> hostedConfig.getRepositoryName().equals(data.getConfiguration().getRepositoryName()))
        .findFirst();
    assertTrue(hostedRepoOpt.isPresent());
    Repository hostedRepository = hostedRepoOpt.get();
    Configuration hostedConfiguration = hostedRepository.getConfiguration();
    assertThat(hostedConfiguration.getRecipeName(), is(hostedConfig.getRecipeName()));
    assertThat(hostedConfiguration.getRoutingRuleId(), is(rule1.id()));
    assertNotNull(hostedConfiguration.getAttributes());
    assertConfigurationAttributes(hostedConfiguration.getAttributes());

    RoutingRule hostedRoutingRule = hostedRepository.getRoutingRule();
    assertThat(hostedRoutingRule.name(), is(rule1.name()));
    assertThat(hostedRoutingRule.description(), is(rule1.description()));
    assertThat(hostedRoutingRule.matchers(), containsInAnyOrder("matcher_1", "matcher_2"));

    // check proxy repository data
    Optional<Repository> proxyRepoOpt = importedConfigurationData.stream()
        .filter(data -> proxyConfig.getRepositoryName().equals(data.getConfiguration().getRepositoryName()))
        .findFirst();
    assertTrue(proxyRepoOpt.isPresent());
    Repository proxyRepository = proxyRepoOpt.get();
    Configuration proxyConfiguration = proxyRepository.getConfiguration();
    assertThat(proxyConfiguration.getRecipeName(), is(proxyConfig.getRecipeName()));
    assertThat(proxyConfiguration.getRoutingRuleId(), is(rule2.id()));
    assertNotNull(proxyConfiguration.getAttributes());
    assertConfigurationAttributes(proxyConfiguration.getAttributes());

    RoutingRule proxyRoutingRule = proxyRepository.getRoutingRule();
    assertThat(proxyRoutingRule.name(), is(rule2.name()));
    assertThat(proxyRoutingRule.description(), is(rule2.description()));
    assertThat(proxyRoutingRule.matchers(), containsInAnyOrder("matcher_1", "matcher_2"));

    // check group repository data
    Optional<Repository> groupRepoOpt = importedConfigurationData.stream()
        .filter(data -> groupConfig.getRepositoryName().equals(data.getConfiguration().getRepositoryName()))
        .findFirst();
    assertTrue(groupRepoOpt.isPresent());
    Repository groupRepository = groupRepoOpt.get();
    Configuration groupConfiguration = groupRepository.getConfiguration();
    assertThat(groupConfiguration.getRecipeName(), is(groupConfig.getRecipeName()));
    assertNull(groupConfiguration.getRoutingRuleId());
    assertNotNull(groupConfiguration.getAttributes());
    assertConfigurationAttributes(groupConfiguration.getAttributes());

    // check routing rule without repository
    Optional<Repository> repositoryOpt =
        importedConfigurationData.stream().filter(data -> data.getConfiguration() == null).findFirst();
    assertTrue(repositoryOpt.isPresent());
    RoutingRule routingRule = repositoryOpt.get().getRoutingRule();
    assertThat(routingRule.name(), is(rule3.name()));
    assertThat(routingRule.description(), is(rule3.description()));
    assertThat(routingRule.matchers(), containsInAnyOrder("matcher_1", "matcher_2"));
  }

  private void assertConfigurationAttributes(final Map<String, Map<String, Object>> attributes) {
    assertThat(attributes.toString(), allOf(
        containsString("metadata"),
        containsString("size"),
        containsString("10")));
    // make sure sensitive data is not serialized
    assertThat(attributes.toString(), not(containsString("admin123")));
  }

  private Configuration generateConfigData(final String name, final String recipe, final EntityId routingRuleId) {
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

  private RoutingRule createRoutingRule(final String name) {
    RoutingRuleData routingRule = new RoutingRuleData();
    routingRule.setId(new EntityUUID());
    routingRule.setName(name);
    routingRule.description("Description");
    routingRule.matchers(ImmutableList.of("matcher_1", "matcher_2"));
    routingRule.mode(RoutingMode.ALLOW);

    return routingRule;
  }
}

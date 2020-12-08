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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.config.internal.ConfigurationData;
import org.sonatype.nexus.repository.routing.RoutingRule;
import org.sonatype.nexus.repository.routing.RoutingRuleStore;
import org.sonatype.nexus.repository.routing.internal.RoutingRuleData;
import org.sonatype.nexus.supportzip.ExportConfigData;
import org.sonatype.nexus.supportzip.ImportData;
import org.sonatype.nexus.supportzip.datastore.JsonExporter;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Write/Read {@link Configuration} and {@link RoutingRule} data to/from a JSON file.
 *
 * @since 3.29
 */
@Named("configurationExport")
@Singleton
public class ConfigurationExport
    extends JsonExporter
    implements ExportConfigData, ImportData
{
  private final ConfigurationStore configurationStore;

  private final RoutingRuleStore routingRuleStore;

  @Inject
  public ConfigurationExport(final ConfigurationStore configurationStore, final RoutingRuleStore routingRuleStore) {
    this.configurationStore = checkNotNull(configurationStore);
    this.routingRuleStore = checkNotNull(routingRuleStore);
  }

  @Override
  public void export(final File file) throws IOException {
    log.debug("Export Configuration and RoutingRule data to {}", file);
    List<Configuration> configurations = configurationStore.list();
    List<RoutingRule> routingRules = routingRuleStore.list();
    Map<String, RoutingRule> routingRuleById = routingRules.stream()
        .collect(Collectors.toMap(routing -> routing.id().getValue(), routing -> routing));
    List<Repository> repositories = new ArrayList<>();
    for (Configuration configuration : configurations) {
      Repository repository = new Repository();
      repository.setConfiguration(configuration);
      if (configuration.getRoutingRuleId() != null) {
        String routingRuleId = configuration.getRoutingRuleId().getValue();
        repository.setRoutingRule(routingRuleById.get(routingRuleId));
      }
      repositories.add(repository);
    }

    // put routing rules which don't have reference to the repository
    Set<String> configRoutingRules = configurations.stream()
        .filter(config -> config.getRoutingRuleId() != null)
        .map(config -> config.getRoutingRuleId().getValue())
        .collect(Collectors.toSet());
    List<RoutingRule> routingRulesWithoutRepo = routingRules.stream()
        .filter(routingRule -> !configRoutingRules.contains(routingRule.id().getValue()))
        .collect(Collectors.toList());
    for (RoutingRule routingRule : routingRulesWithoutRepo) {
      Repository repository = new Repository();
      repository.setRoutingRule(routingRule);
      repositories.add(repository);
    }
    exportToJson(repositories, file);
  }

  @Override
  public void restore(final File file) throws IOException {
    log.debug("Restoring Configuration and RoutingRule data from {}", file);
    List<Repository> repositories = importFromJson(file, Repository.class);
    for (Repository repository : repositories) {
      Configuration configuration = repository.getConfiguration();
      RoutingRule routingRule = repository.getRoutingRule();
      if (configuration != null) {
        saveConfiguration(configuration, routingRule);
      }
      // save routing rules which don't have reference to the repository
      if (configuration == null && routingRule != null) {
        ((RoutingRuleData) routingRule).setId(null);
        routingRuleStore.create(routingRule);
      }
    }
  }

  private void saveConfiguration(final Configuration configuration, final RoutingRule routingRule) {
    if (routingRule instanceof RoutingRuleData) {
      ((RoutingRuleData) routingRule).setId(null);
      RoutingRule storedRoutingRule = routingRuleStore.getByName(routingRule.name());
      if (storedRoutingRule != null) {
        configuration.setRoutingRuleId(storedRoutingRule.id());
      }
      else {
        RoutingRule newRoutingRule = routingRuleStore.create(routingRule);
        configuration.setRoutingRuleId(newRoutingRule.id());
      }
    }
    configurationStore.create(configuration);
  }

  // This class is used only for Serialization/Deserialization
  public static class Repository
  {
    @JsonProperty
    @JsonDeserialize(as = ConfigurationData.class)
    private Configuration configuration;

    @JsonProperty
    @JsonDeserialize(as = RoutingRuleData.class)
    private RoutingRule routingRule;

    public Configuration getConfiguration() {
      return configuration;
    }

    public void setConfiguration(final Configuration configuration) {
      this.configuration = configuration;
    }

    public RoutingRule getRoutingRule() {
      return routingRule;
    }

    public void setRoutingRule(final RoutingRule routingRule) {
      this.routingRule = routingRule;
    }
  }
}

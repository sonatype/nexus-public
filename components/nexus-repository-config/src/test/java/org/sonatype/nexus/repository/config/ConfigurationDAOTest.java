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

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.datastore.api.DataSession;
import org.sonatype.nexus.repository.config.internal.ConfigurationData;
import org.sonatype.nexus.repository.routing.RoutingMode;
import org.sonatype.nexus.repository.routing.internal.RoutingRuleDAO;
import org.sonatype.nexus.repository.routing.internal.RoutingRuleData;
import org.sonatype.nexus.testdb.DataSessionRule;

import com.google.common.collect.ImmutableSet;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;
import static org.sonatype.nexus.repository.routing.RoutingMode.ALLOW;

public class ConfigurationDAOTest
    extends TestSupport
{
  @Rule
  public DataSessionRule sessionRule =
      new DataSessionRule().access(RoutingRuleDAO.class).access(ConfigurationDAO.class);

  private DataSession<?> session;

  private ConfigurationDAO dao;

  private RoutingRuleDAO routingRuleDAO;

  private EntityId id1, id2, id3;

  @Before
  public void setup() {
    session = sessionRule.openSession(DEFAULT_DATASTORE_NAME);
    dao = session.access(ConfigurationDAO.class);
    routingRuleDAO = session.access(RoutingRuleDAO.class);

    RoutingRuleData routingRule = routingRule("foo", ALLOW, "desc1", "a", "b", "c");
    routingRuleDAO.create(routingRule);

    routingRule = routingRule("bar", ALLOW, "desc2", "d", "e", "f");
    routingRuleDAO.create(routingRule);

    routingRule = routingRule("baz", ALLOW, "desc1", "a", "b", "c");
    routingRuleDAO.create(routingRule);

    id1 = routingRuleDAO.readByName("foo").get().getId();
    id2 = routingRuleDAO.readByName("bar").get().getId();
    id3 = routingRuleDAO.readByName("baz").get().getId();
  }

  @After
  public void cleanup() {
    session.close();
  }

  @Test
  public void testCRUD() {
    ConfigurationData configuration = configurationData("foo", "bar", true, Map.of("baz", Map.of("buzz", "booz")), id1);

    dao.create(configuration);

    ConfigurationData read = dao.readByName(configuration.getName()).orElse(null);

    assertThat(read.getName(), is(configuration.getName()));
    assertThat(read.getRecipeName(), is(configuration.getRecipeName()));
    assertThat(read.isOnline(), is(configuration.isOnline()));
    assertThat(read.getRoutingRuleId(), is(configuration.getRoutingRuleId()));
    assertThat(read.getAttributes(), is(configuration.getAttributes()));

    // it is updated
    configuration.setRecipeName("notBar");
    configuration.setOnline(false);
    configuration.setRoutingRuleId(id2);
    configuration.setAttributes(Map.of("baz2", Map.of("buzz2", "booz2")));
    dao.update(configuration);

    // it is read back
    ConfigurationData update = dao.readByName(configuration.getName()).orElse(null);

    // the read value matches the update
    assertThat(update.getName(), is(configuration.getName()));
    assertThat(update.isOnline(), is(configuration.isOnline()));
    assertThat(update.getRoutingRuleId(), is(configuration.getRoutingRuleId()));
    assertThat(update.getAttributes(), is(configuration.getAttributes()));

    // recipe name is not changed
    assertThat(update.getRecipeName(), is("bar"));

    // it is deleted
    dao.deleteByName(configuration.getName());

    // no configuration is found by that name
    assertFalse(dao.readByName(configuration.getName()).isPresent());
  }

  @Test
  public void testPasswordAttribute() {
    ConfigurationData configuration =
        configurationData("foo", "bar", true, Map.of("baz", Map.of("userpassword", "booz")), id1);

    dao.create(configuration);

    ConfigurationData read = dao.readByName(configuration.getName()).orElse(null);

    assertThat(read.getAttributes().get("baz").get("userpassword"), is("booz"));
  }

  @Test
  public void testReadByNames() {
    ConfigurationData configuration1 =
        configurationData("foo", "foo", true, Map.of("baz", Map.of("buzz", "booz")), id1);

    ConfigurationData configuration2 =
        configurationData("barr", "barr", true, Map.of("bar", Map.of("burr", "foo")), id2);

    ConfigurationData configuration3 =
        configurationData("bazz", "bazz", true, Map.of("baz", Map.of("bazz", "bar")), id3);

    dao.create(configuration1);
    dao.create(configuration2);
    dao.create(configuration3);

    Collection<Configuration> results = dao.readByNames(ImmutableSet.of("_oo", "b%z_"));

    assertThat(results, hasSize(2));

    List<String> names = results.stream().map(Configuration::getRepositoryName).toList();
    assertThat(names, containsInAnyOrder(configuration1.getName(), configuration3.getName()));
  }

  @Test
  public void testReadByRecipe() {
    ConfigurationData conanProxyConfig1 =
        configurationData("conan-proxy-1", "conan-proxy", true, Map.of("baz", Map.of("buzz", "booz")), id1);
    ConfigurationData conanProxyConfig2 =
        configurationData("conan-proxy-2", "conan-proxy", true, Map.of("baz", Map.of("buzz", "booz")), id1);
    ConfigurationData conanProxyConfig3 =
        configurationData("conan-proxy-3", "conan-proxy", true, Map.of("baz", Map.of("buzz", "booz")), id1);
    ConfigurationData conanProxyConfig4 =
        configurationData("conan-proxy-4", "conan-proxy", true, Map.of("baz", Map.of("buzz", "booz")), id1);

    ConfigurationData anotherConfig1 =
        configurationData("foo", "foo", true, Map.of("baz", Map.of("buzz", "booz")), id1);
    ConfigurationData anotherConfig2 =
        configurationData("barr", "barr", true, Map.of("bar", Map.of("burr", "foo")), id2);
    ConfigurationData anotherConfig3 =
        configurationData("bazz", "bazz", true, Map.of("baz", Map.of("bazz", "bar")), id3);

    dao.create(conanProxyConfig1);
    dao.create(conanProxyConfig2);
    dao.create(conanProxyConfig3);
    dao.create(conanProxyConfig4);
    dao.create(anotherConfig1);
    dao.create(anotherConfig2);
    dao.create(anotherConfig3);

    Collection<Configuration> results = dao.readByRecipe("conan-proxy");

    assertThat(results, hasSize(4));
  }

  private static ConfigurationData configurationData(
      final String name,
      final String recipeName,
      final boolean online,
      final Map<String, Map<String, Object>> attributes,
      final EntityId id)
  {
    ConfigurationData configuration = new ConfigurationData();
    configuration.setName(name);
    configuration.setRecipeName(recipeName);
    configuration.setOnline(online);
    configuration.setAttributes(attributes);
    configuration.setRoutingRuleId(id);

    return configuration;
  }

  private static RoutingRuleData routingRule(
      final String name,
      final RoutingMode mode,
      final String description,
      final String... matchers)
  {
    return new RoutingRuleData().name(name).description(description).mode(mode).matchers(Arrays.asList(matchers));
  }
}

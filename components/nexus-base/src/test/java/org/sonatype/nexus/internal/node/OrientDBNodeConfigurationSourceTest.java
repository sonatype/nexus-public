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
package org.sonatype.nexus.internal.node;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.node.NodeConfiguration;
import org.sonatype.nexus.common.node.NodeConfigurationSource;
import org.sonatype.nexus.orient.testsupport.DatabaseInstanceRule;

import com.google.common.collect.Lists;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Tests {@link OrientDBNodeConfigurationSource}
 */
public class OrientDBNodeConfigurationSourceTest
    extends TestSupport
{
  @Rule
  public DatabaseInstanceRule database = DatabaseInstanceRule.inMemory("test");

  private NodeConfigurationEntityAdapter entityAdapter = new NodeConfigurationEntityAdapter();

  private NodeConfigurationSource underTest;

  @Before
  public void setUp() throws Exception {
    OrientDBNodeConfigurationSource configSrc =
        new OrientDBNodeConfigurationSource(database.getInstanceProvider(), entityAdapter);
    configSrc.start();
    underTest = configSrc;
  }

  @After
  public void tearDown() throws Exception {
    underTest.loadAll().forEach(entity -> underTest.delete(entity.getId()));
    OrientDBNodeConfigurationSource configSrc = (OrientDBNodeConfigurationSource) underTest;
    configSrc.stop();
  }

  @Test
  public void testCreate() throws Exception {
    // create it
    String createdId = underTest.create(new NodeConfiguration("id", "jack torrance"));
    assertThat(createdId, notNullValue());
    // pull it down and make sure it matches
    Optional<NodeConfiguration> retrieved = underTest.getById(createdId);
    assertThat(retrieved.isPresent(), is(true));
    assertThat(retrieved.get().getId(), equalTo("id"));
    assertThat(retrieved.get().getFriendlyNodeName(), equalTo("jack torrance"));
  }

  @Test
  public void testLoadAll() throws Exception {
    // make a handful of them
    List<String> ids = IntStream.range(0, 10)
        .mapToObj(index -> underTest.create(new NodeConfiguration("id" + index, "pennywise " + index)))
        .collect(Collectors.toList());
    // pull them from the database
    List<NodeConfiguration> nodeConfigs = Lists.newLinkedList(underTest.loadAll());
    // have to sort because order is not guaranteed on the query
    nodeConfigs.sort(Comparator.comparing(NodeConfiguration::getId));
    // check that they were stored as they were created
    assertThat(ids, hasSize(10));
    assertThat(nodeConfigs, hasSize(10));
    IntStream.range(0, 10).forEach(index -> {
      assertThat(ids.get(index), equalTo("id" + index));
      NodeConfiguration entity = nodeConfigs.get(index);
      assertThat(entity, notNullValue());
      assertThat(entity.getId(), equalTo("id" + index));
      assertThat(entity.getFriendlyNodeName(), equalTo("pennywise " + index));
    });
  }

  @Test
  public void testUpdate() throws Exception {
    // make a config
    String createdId = underTest.create(new NodeConfiguration("id", "randall flagg"));
    // get it and update it
    NodeConfiguration got = underTest.getById(createdId).get();
    got.setFriendlyNodeName("george stark");
    boolean updated = underTest.update(got);
    assertThat(updated, equalTo(true));
    // make sure updates took
    Optional<NodeConfiguration> retrieved = underTest.getById(createdId);
    assertThat(retrieved.isPresent(), is(true));
    assertThat(retrieved.get().getId(), equalTo("id"));
    assertThat(retrieved.get().getFriendlyNodeName(), equalTo("george stark"));
  }

  @Test
  public void testDelete() throws Exception {
    // make one
    String createdId = underTest.create(new NodeConfiguration("id", "leland gaunt"));
    assertThat(createdId, notNullValue());
    // delete it
    underTest.delete("id");
    // make sure that no entities exist
    assertThat(underTest.loadAll(), hasSize(0));
  }

  @Test
  public void testGetById() throws Exception {
    // create it
    String createdId = underTest.create(new NodeConfiguration("id", "gary barkovitch"));
    assertThat(createdId, notNullValue());
    assertThat(createdId, equalTo("id"));
    // pull it down and make sure it matches
    Optional<NodeConfiguration> retrieved = underTest.getById("id");
    assertThat(retrieved.isPresent(), is(true));
    assertThat(retrieved.get().getId(), equalTo("id"));
    assertThat(retrieved.get().getFriendlyNodeName(), equalTo("gary barkovitch"));
  }

  @Test
  public void testFriendlyNames() throws Exception {
    // make a config
    String createdId = underTest.create(new NodeConfiguration("id", "jack mort"));
    // set the friendly name
    underTest.setFriendlyName("id", "percy wetmore");
    // make sure update succeeded
    Optional<NodeConfiguration> retrieved = underTest.getById(createdId);
    assertThat(retrieved.isPresent(), is(true));
    assertThat(retrieved.get().getId(), equalTo("id"));
    assertThat(retrieved.get().getFriendlyNodeName(), equalTo("percy wetmore"));
  }
}

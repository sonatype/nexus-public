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

import java.util.Map;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.capability.CapabilityIdentity;
import org.sonatype.nexus.internal.capability.storage.CapabilityStorageItem;
import org.sonatype.nexus.internal.capability.storage.CapabilityStorageItemEntityAdapter;
import org.sonatype.nexus.internal.capability.storage.OrientCapabilityStorage;
import org.sonatype.nexus.orient.DatabaseInstanceRule;
import org.sonatype.nexus.orient.HexRecordIdObfuscator;

import com.google.common.collect.Maps;
import com.google.inject.util.Providers;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Tests for {@link OrientCapabilityStorage}.
 */
public class OrientCapabilityStorageTest
  extends TestSupport
{
  @Rule
  public DatabaseInstanceRule database = new DatabaseInstanceRule("test");

  private OrientCapabilityStorage underTest;

  @Before
  public void setUp() throws Exception {
    CapabilityStorageItemEntityAdapter entityAdapter = new CapabilityStorageItemEntityAdapter();
    entityAdapter.installDependencies(new HexRecordIdObfuscator());

    this.underTest = new OrientCapabilityStorage(
        Providers.of(database.getInstance()),
        entityAdapter
    );

    underTest.start();
  }

  @After
  public void tearDown() throws Exception {
    if (underTest != null) {
      underTest.stop();
      underTest = null;
    }
  }

  @Test
  public void getAll_empty() throws Exception {
    Map<CapabilityIdentity, CapabilityStorageItem> items = underTest.getAll();
    assertThat(items, notNullValue());
    assertThat(items.isEmpty(), is(true));
  }

  @Test
  public void basicLifecycle() throws Exception {
    CapabilityStorageItem item1 = new CapabilityStorageItem();
    item1.setType("test");
    item1.setVersion(0);
    item1.setEnabled(false);
    item1.setNotes("hello");
    Map<String,String> props = Maps.newHashMap();
    props.put("foo", "bar");
    item1.setProperties(props);
    log("Item: {}", item1);

    // add
    CapabilityIdentity id = underTest.add(item1);
    log("Id: {}", id);

    // list
    Map<CapabilityIdentity, CapabilityStorageItem> items1 = underTest.getAll();
    log("Items: {}", items1);
    assertThat(items1, notNullValue());
    assertThat(items1.entrySet(), hasSize(1));

    // verify that item has same attributes as original
    CapabilityStorageItem item2 = items1.values().iterator().next();
    assertThat(item2.getType(), is(item1.getType()));
    assertThat(item2.getVersion(), is(item1.getVersion()));
    assertThat(item2.getNotes(), is(item1.getNotes()));
    assertThat(item2.isEnabled(), is(item1.isEnabled()));
    assertThat(item2.getProperties(), hasEntry("foo", "bar"));

    // update
    item2.setEnabled(true);
    boolean updated = underTest.update(id, item2);
    assertThat("failed to update", updated, is(true));

    // verify that all is the same, except for enabled flag
    CapabilityStorageItem item3 = underTest.getAll().values().iterator().next();
    assertThat(item3, notNullValue());
    assertThat(item3.getType(), is(item1.getType()));
    assertThat(item3.getVersion(), is(item1.getVersion()));
    assertThat(item3.getNotes(), is(item1.getNotes()));
    assertThat(item3.isEnabled(), is(true));
    assertThat(item3.getProperties(), hasEntry("foo", "bar"));

    // delete
    boolean removed = underTest.remove(id);
    assertThat("failed to remove", removed, is(true));

    Map<CapabilityIdentity, CapabilityStorageItem> items2 = underTest.getAll();
    log("Items: {}", items2);
    assertThat(items2, notNullValue());
    assertThat(items2.isEmpty(), is(true));
  }
}

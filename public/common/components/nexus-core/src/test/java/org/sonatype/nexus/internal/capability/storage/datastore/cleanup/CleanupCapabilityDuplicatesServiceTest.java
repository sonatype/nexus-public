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
package org.sonatype.nexus.internal.capability.storage.datastore.cleanup;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.sonatype.nexus.capability.CapabilityIdentity;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.internal.capability.storage.CapabilityStorageImpl;
import org.sonatype.nexus.internal.capability.storage.CapabilityStorageItem;
import org.sonatype.nexus.internal.capability.storage.CapabilityStorageItemDAO;
import org.sonatype.nexus.testdb.DataSessionRule;
import org.sonatype.nexus.transaction.UnitOfWork;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;

import static java.util.Collections.emptyMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Tests for {@link CleanupCapabilityDuplicatesService}
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class CleanupCapabilityDuplicatesServiceTest
{
  @Rule
  public DataSessionRule sessionRule = new DataSessionRule().access(CapabilityStorageItemDAO.class);

  @Mock
  private EventManager eventManager;

  private CapabilityStorageImpl capabilityStorage;

  private CleanupCapabilityDuplicatesService underTest;

  @Before
  public void start() {
    capabilityStorage = new CapabilityStorageImpl(sessionRule);
    capabilityStorage.setDependencies(eventManager);
    underTest = new CleanupCapabilityDuplicatesService(capabilityStorage);

    UnitOfWork.beginBatch(() -> sessionRule.openSession(DEFAULT_DATASTORE_NAME));
  }

  @After
  public void tearDown() {
    UnitOfWork.end();
  }

  @Test
  public void testCleanup() throws Exception {
    prepareTestCapabilitiesWithDuplicates();

    assertThat(capabilityStorage.getAll(), aMapWithSize(23));

    Map<CapabilityStorageItem, List<CapabilityIdentity>> duplicates = underTest.browseCapabilityDuplicates();
    assertThat(duplicates.keySet(), hasSize(5));
    assertCapabilitiesCount(duplicates, 22);

    underTest.doCleanup();

    assertThat(capabilityStorage.getAll(), aMapWithSize(6));
    duplicates = underTest.browseCapabilityDuplicates();
    assertCapabilitiesCount(duplicates, 0);
  }

  @Test
  public void testCleanupDoesNotTouchUniqueRecords() throws Exception {
    prepareUniqueTestCapabilities();

    assertThat(capabilityStorage.getAll(), aMapWithSize(5));

    assertTrue(underTest.browseCapabilityDuplicates().isEmpty());

    underTest.doCleanup();

    assertThat(capabilityStorage.getAll(), aMapWithSize(5));
  }

  @Test
  public void testCleanupNotNeeded() throws Exception {
    underTest.doCleanup();

    Map<CapabilityStorageItem, List<CapabilityIdentity>> duplicates = underTest.browseCapabilityDuplicates();
    assertCapabilitiesCount(duplicates, 0);
  }

  private static void assertCapabilitiesCount(
      final Map<CapabilityStorageItem, List<CapabilityIdentity>> capabilities,
      final int expectedCount)
  {
    AtomicInteger count = new AtomicInteger();
    capabilities.keySet().forEach(type -> {
      count.addAndGet(capabilities.get(type).size());
    });

    assertThat(count.get(), is(expectedCount));
  }

  private void prepareTestCapabilitiesWithDuplicates() throws SQLException {
    try (Connection connection = sessionRule.openConnection(DEFAULT_DATASTORE_NAME)) {
      String sql = "DROP INDEX IF EXISTS uk_capability_storage_item_type_props";
      connection.prepareStatement(sql).executeUpdate();
    }

    createTestCapabilities(5, "test-capability-1", emptyMap());

    createTestCapabilities(5, "test-capability-2", new HashMap<String, String>()
    {
      {
        put("repository", "maven-central");
      }
    });
    createTestCapabilities(1, "test-capability-2", new HashMap<String, String>()
    {
      {
        put("repository", "maven-proxy");
      }
    });

    createTestCapabilities(5, "test-capability-3", new HashMap<String, String>()
    {
      {
        put("repository", "nuget-proxy");
      }
    });
    createTestCapabilities(5, "test-capability-3", new HashMap<String, String>()
    {
      {
        put("repository", "nuget-group");
      }
    });

    createTestCapabilities(2, "test-capability-4", new HashMap<String, String>()
    {
      {
        put("repository", "nuget-group");
        put("auth", "false");
      }
    });
  }

  private void prepareUniqueTestCapabilities() {
    createTestCapabilities(1, "test-capability-1", emptyMap());
    createTestCapabilities(1, "test-capability-1", new HashMap<String, String>()
    {
      {
        put("repository", "maven-central");
      }
    });

    createTestCapabilities(1, "test-capability-2", emptyMap());
    createTestCapabilities(1, "test-capability-2", new HashMap<String, String>()
    {
      {
        put("repository", "maven-central");
        put("test", "test");
      }
    });

    createTestCapabilities(1, "test-capability-3", emptyMap());
  }

  private void createTestCapabilities(
      final int count,
      final String typeId,
      final Map<String, String> properties)
  {
    // duplicate
    for (int i = 0; i < count; i++) {
      capabilityStorage.add(
          capabilityStorage.newStorageItem(
              1,
              typeId,
              true,
              "notes",
              properties));
    }
  }
}

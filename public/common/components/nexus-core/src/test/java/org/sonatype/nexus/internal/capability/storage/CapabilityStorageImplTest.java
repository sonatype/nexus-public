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
package org.sonatype.nexus.internal.capability.storage;

import java.util.UUID;

import org.sonatype.nexus.capability.CapabilityIdentity;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.internal.capability.storage.datastore.CapabilityStorageItemCreatedEventImpl;
import org.sonatype.nexus.internal.capability.storage.datastore.CapabilityStorageItemDeletedEventImpl;
import org.sonatype.nexus.internal.capability.storage.datastore.CapabilityStorageItemUpdatedEventImpl;
import org.sonatype.nexus.testdb.DataSessionRule;
import org.sonatype.nexus.common.entity.EntityUUID;
import org.sonatype.nexus.datastore.api.DuplicateKeyException;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Verifies {@link CapabilityStorageImpl} publishes a create/update/delete event only <em>after</em> the
 * underlying write actually succeeds, so a swallowed duplicate insert or a no-op update/delete never
 * broadcasts a spurious event to peers. Backed by a real in-memory config datastore.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class CapabilityStorageImplTest
{
  @Rule
  public DataSessionRule sessionRule = new DataSessionRule().access(CapabilityStorageItemDAO.class);

  @Mock
  private EventManager eventManager;

  private CapabilityStorageImpl underTest;

  @Before
  public void setup() {
    underTest = new CapabilityStorageImpl(sessionRule);
    underTest.setDependencies(eventManager);
  }

  private CapabilityStorageItem newItem() {
    return underTest.newStorageItem(1, "type", true, "notes", ImmutableMap.of("k", "v"));
  }

  @Test
  public void getAllByType_returnsOnlyCapabilitiesOfThatType() {
    CapabilityIdentity a =
        underTest.add(underTest.newStorageItem(1, "typeA", true, "a", ImmutableMap.of("k", "v")));
    underTest.add(underTest.newStorageItem(1, "typeB", true, "b", ImmutableMap.of("k", "v")));

    var typeA = underTest.getAllByType("typeA");
    assertEquals(1, typeA.size());
    assertTrue(typeA.containsKey(a));
    assertEquals("typeA", typeA.values().iterator().next().getType());

    assertEquals(1, underTest.getAllByType("typeB").size());
    assertTrue(underTest.getAllByType("missing").isEmpty());
  }

  @Test
  public void add_firesCreatedEventOnSuccessfulInsert() {
    underTest.add(newItem());

    verify(eventManager, times(1)).post(any(CapabilityStorageItemCreatedEventImpl.class));
  }

  @Test
  public void add_duplicateInsertIsSwallowedAndFiresNoEvent() {
    // A real PK duplicate is unreachable via add() (the datastore assigns a fresh id per insert and rejects a
    // pre-identified entity), so drive the framework-translated DuplicateKeyException directly: it must be
    // swallowed and must fire no created-event. A real session is used so the @Transactional aspect is satisfied.
    CapabilityStorageItemDAO throwingDao = mock(CapabilityStorageItemDAO.class);
    doThrow(new DuplicateKeyException(new RuntimeException("dup"))).when(throwingDao).create(any());
    CapabilityStorageImpl storeWithThrowingDao = new CapabilityStorageImpl(sessionRule)
    {
      @Override
      protected CapabilityStorageItemDAO dao() {
        return throwingDao;
      }
    };
    storeWithThrowingDao.setDependencies(eventManager);

    // Pre-set the id: the mocked create() never runs the framework's id assignment, and add() reads the id
    // for its return value. This must not throw, and must fire no created-event.
    CapabilityStorageItemData item = (CapabilityStorageItemData) newItem();
    item.setId(new EntityUUID(UUID.randomUUID()));
    storeWithThrowingDao.add(item);

    verify(eventManager, never()).post(any(CapabilityStorageItemCreatedEventImpl.class));
  }

  @Test
  public void update_firesUpdatedEventWhenRowChanged() {
    CapabilityStorageItem item = newItem();
    CapabilityIdentity id = underTest.add(item);

    boolean updated = underTest.update(id, item);

    assertTrue(updated);
    verify(eventManager, times(1)).post(any(CapabilityStorageItemUpdatedEventImpl.class));
  }

  @Test
  public void update_noOpFiresNoEvent() {
    // no row exists for this id -> update affects 0 rows -> no updated event
    boolean updated = underTest.update(new CapabilityIdentity(UUID.randomUUID().toString()), newItem());

    assertFalse(updated);
    verify(eventManager, never()).post(any(CapabilityStorageItemUpdatedEventImpl.class));
  }

  @Test
  public void remove_firesDeletedEventWhenRowDeleted() {
    CapabilityStorageItem item = newItem();
    CapabilityIdentity id = underTest.add(item);

    boolean deleted = underTest.remove(id);

    assertTrue(deleted);
    verify(eventManager, times(1)).post(any(CapabilityStorageItemDeletedEventImpl.class));
  }

  @Test
  public void remove_noOpFiresNoEvent() {
    boolean deleted = underTest.remove(new CapabilityIdentity(UUID.randomUUID().toString()));

    assertFalse(deleted);
    verify(eventManager, never()).post(any(CapabilityStorageItemDeletedEventImpl.class));
  }
}

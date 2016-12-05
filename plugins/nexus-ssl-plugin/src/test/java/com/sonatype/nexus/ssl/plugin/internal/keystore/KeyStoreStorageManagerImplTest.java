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
package com.sonatype.nexus.ssl.plugin.internal.keystore;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.orient.DatabaseInstance;
import org.sonatype.nexus.ssl.spi.KeyStoreStorage;

import com.google.inject.util.Providers;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class KeyStoreStorageManagerImplTest
    extends TestSupport
{
  private static final String KEY_STORE_NAME = "test.ks";

  @Mock
  private DatabaseInstance db;

  @Mock
  private ODatabaseDocumentTx tx;

  @Mock
  private KeyStoreDataEntityAdapter entityAdapter;

  @Mock
  private EventManager eventManager;

  private KeyStoreStorageManagerImpl storageManager;

  @Before
  public void setUp() {
    when(db.acquire()).thenReturn(tx);
    storageManager = new KeyStoreStorageManagerImpl(Providers.of(db), entityAdapter, eventManager);
  }

  @Test
  public void testCreateStorage() {
    KeyStoreStorage storage = storageManager.createStorage(KEY_STORE_NAME);
    assertThat(storage, is(instanceOf(OrientKeyStoreStorage.class)));
    assertThat(((OrientKeyStoreStorage) storage).getKeyStoreName(), is("ssl/" + KEY_STORE_NAME));
    verify(eventManager).register(storage);
  }

  @Test
  public void testStop_UnregisterFromEventManager() throws Exception {
    KeyStoreStorage storage = storageManager.createStorage(KEY_STORE_NAME);
    storageManager.doStop();
    verify(eventManager).unregister(storage);
  }

  @Test
  public void testLoad_NonExistentData() {
    assertThat(storageManager.load(KEY_STORE_NAME), is(nullValue()));
    verify(entityAdapter).load(tx, KEY_STORE_NAME);
  }

  @Test
  public void testLoad_ExistentData() {
    KeyStoreData entity = new KeyStoreData();
    when(entityAdapter.load(tx, KEY_STORE_NAME)).thenReturn(entity);
    assertThat(storageManager.load(KEY_STORE_NAME), is(entity));
  }

  @Test
  public void testSave() {
    KeyStoreData entity = new KeyStoreData();
    storageManager.save(entity);
    verify(entityAdapter).save(tx, entity);
  }
}

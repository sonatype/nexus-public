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
package org.sonatype.nexus.internal.node.datastore;

import java.io.File;
import java.security.cert.Certificate;
import java.sql.Connection;
import java.util.UUID;

import javax.inject.Provider;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.crypto.internal.CryptoHelperImpl;
import org.sonatype.nexus.datastore.api.DataSessionSupplier;
import org.sonatype.nexus.internal.node.KeyStoreManagerConfigurationImpl;
import org.sonatype.nexus.internal.node.KeyStoreManagerImpl;
import org.sonatype.nexus.internal.node.KeyStoreStorageManagerImpl;
import org.sonatype.nexus.internal.node.NodeIdEncoding;
import org.sonatype.nexus.node.datastore.NodeIdStore;
import org.sonatype.nexus.ssl.KeyStoreManager;
import org.sonatype.nexus.testdb.DataSessionRule;
import org.sonatype.nexus.transaction.TransactionModule;

import com.google.inject.Guice;
import com.google.inject.Provides;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class NodeIdUpgradeStep_1_14_Test
    extends TestSupport
{
  @Rule
  public DataSessionRule sessionRule = new DataSessionRule().access(NodeIdDAO.class);

  @Mock
  private EventManager eventManager;

  private KeyStoreManager keyStoreManager;

  private Provider<KeyStoreManager> keyStoreProvider = () -> keyStoreManager;

  private NodeIdStore nodeIdStore;

  private NodeIdUpgradeStep_1_14 underTest;

  @Before
  public void setup() {
    File dir = util.createTempDir("keystores");
    KeyStoreManagerConfigurationImpl config = new KeyStoreManagerConfigurationImpl();
    // use lower strength for faster test execution
    config.setKeyAlgorithmSize(512);
    keyStoreManager = spy(new KeyStoreManagerImpl(new CryptoHelperImpl(), new KeyStoreStorageManagerImpl(dir), config));

    nodeIdStore = Guice.createInjector(new TransactionModule()
    {
      @Provides
      DataSessionSupplier getDataSessionSupplier() {
        return sessionRule;
      }

      @Provides
      EventManager getEventManager() {
        return eventManager;
      }
    }).getInstance(NodeIdStoreImpl.class);

    underTest = new NodeIdUpgradeStep_1_14(keyStoreProvider, nodeIdStore);
  }

  @After
  public void tearDown() throws Exception {
    keyStoreManager.removePrivateKey();
  }

  @Test
  public void testMigrate_newInstall() {
    when(keyStoreManager.isKeyPairInitialized()).thenReturn(false);
    inTx(underTest::migrate);

    assertThat(nodeIdStore.get().isPresent(), is(true));
  }

  @Test
  public void testMigrate_alreadyMigrated() {
    String originalKey = nodeIdStore.getOrCreate();

    inTx(underTest::migrate);

    verify(keyStoreManager, never()).isKeyPairInitialized();

    assertThat(nodeIdStore.get().get(), is(originalKey));
  }

  @Test
  public void testMigrate() throws Exception {
    UUID cn = UUID.randomUUID();
    keyStoreManager.generateAndStoreKeyPair(
        cn.toString(),
        "Nexus",
        "Sonatype",
        "Silver Spring",
        "MD",
        "US");

    inTx(underTest::migrate);

    Certificate cert = keyStoreManager.getCertificate();
    assertThat(nodeIdStore.get().get(), is(NodeIdEncoding.nodeIdForCertificate(cert)));
  }

  private void inTx(final ThrowingConsumer<Connection> consumer) {
    try (Connection conn = sessionRule.openConnection("nexus")) {
      consumer.accept(conn);
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @FunctionalInterface
  private interface ThrowingConsumer<E> {
    void accept(E e) throws Exception;
  }
}

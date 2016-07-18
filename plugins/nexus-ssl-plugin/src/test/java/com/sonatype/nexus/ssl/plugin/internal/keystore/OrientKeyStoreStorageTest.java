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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.cert.Certificate;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.entity.DetachedEntityId;
import org.sonatype.nexus.common.entity.DetachedEntityMetadata;
import org.sonatype.nexus.common.entity.DetachedEntityVersion;
import org.sonatype.nexus.common.entity.EntityMetadata;
import org.sonatype.nexus.ssl.CertificateUtil;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class OrientKeyStoreStorageTest
    extends TestSupport
{
  private static final String KEY_STORE_NAME = "test.ks";

  private static final char[] STORE_PASSWORD = "very-secret".toCharArray();

  private static final String CERT_ALIAS = "test-cert";

  @Mock
  private KeyStoreStorageManagerImpl storageManager;

  private OrientKeyStoreStorage storage;

  private KeyStore newKeyStore() throws Exception {
    KeyStore keyStore = KeyStore.getInstance("JKS");
    keyStore.load(null, STORE_PASSWORD);
    return keyStore;
  }

  private KeyStore newKeyStoreWithData() throws Exception {
    KeyStore keyStore = newKeyStore();
    KeyPairGenerator kpgen = KeyPairGenerator.getInstance("RSA");
    kpgen.initialize(512);
    KeyPair keyPair = kpgen.generateKeyPair();
    Certificate cert = CertificateUtil.generateCertificate(keyPair.getPublic(), keyPair.getPrivate(), "SHA1WITHRSA", 7,
        "testing", "Nexus", "Sonatype", "Fulton", "MD", "USA");
    keyStore.setCertificateEntry(CERT_ALIAS, cert);
    return keyStore;
  }

  private byte[] serialize(KeyStore keyStore) throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    keyStore.store(baos, STORE_PASSWORD);
    return baos.toByteArray();
  }

  private EntityMetadata newEntityMetadata(String version) {
    return new DetachedEntityMetadata(new DetachedEntityId("id"), new DetachedEntityVersion(version));
  }

  @Before
  public void setUp() {
    storage = new OrientKeyStoreStorage(storageManager, KEY_STORE_NAME);
  }

  @Test
  public void testExists() {
    assertThat(storage.exists(), is(false));
    when(storageManager.load(KEY_STORE_NAME)).thenReturn(new KeyStoreData());
    assertThat(storage.exists(), is(true));
  }

  @Test
  public void testModified() {
    assertThat(storage.modified(), is(false));
    when(storageManager.load(KEY_STORE_NAME)).thenReturn(new KeyStoreData());
    assertThat(storage.modified(), is(true));
  }

  @Test
  public void testOnKeyStoreDataUpdated() throws Exception {
    KeyStoreData entity = new KeyStoreData();
    entity.setName(KEY_STORE_NAME);
    entity.setBytes(serialize(newKeyStore()));
    entity.setEntityMetadata(newEntityMetadata("1"));
    when(storageManager.load(KEY_STORE_NAME)).thenReturn(entity);
    KeyStore keyStore = newKeyStore();
    storage.load(keyStore, STORE_PASSWORD);
    assertThat(storage.modified(), is(false));
    storage.onKeyStoreDataUpdated(new KeyStoreDataUpdatedEvent(newEntityMetadata("2"), "another" + KEY_STORE_NAME));
    assertThat(storage.modified(), is(false));
    storage.onKeyStoreDataUpdated(new KeyStoreDataUpdatedEvent(newEntityMetadata("1"), KEY_STORE_NAME));
    assertThat(storage.modified(), is(false));
    storage.onKeyStoreDataUpdated(new KeyStoreDataUpdatedEvent(newEntityMetadata("2"), KEY_STORE_NAME));
    assertThat(storage.modified(), is(true));
  }

  @Test
  public void testLoad() throws Exception {
    KeyStoreData entity = new KeyStoreData();
    entity.setName(KEY_STORE_NAME);
    entity.setBytes(serialize(newKeyStoreWithData()));
    entity.setEntityMetadata(newEntityMetadata("1"));
    when(storageManager.load(KEY_STORE_NAME)).thenReturn(entity);
    KeyStore keyStore = newKeyStore();
    storage.load(keyStore, STORE_PASSWORD);
    assertThat(storage.modified(), is(false));
    assertThat(keyStore.containsAlias(CERT_ALIAS), is(true));
  }

  @Test
  public void testSave() throws Exception {
    doAnswer(invoc -> {
      KeyStoreData entity = (KeyStoreData) invoc.getArguments()[0];
      entity.setEntityMetadata(newEntityMetadata("1"));
      return null;
    }).when(storageManager).save(anyObject());
    KeyStore keyStore = newKeyStoreWithData();
    storage.save(keyStore, STORE_PASSWORD);
    assertThat(storage.modified(), is(false));
    ArgumentCaptor<KeyStoreData> entityCaptor = ArgumentCaptor.forClass(KeyStoreData.class);
    verify(storageManager).save(entityCaptor.capture());
    KeyStoreData entity = entityCaptor.getValue();
    assertThat(entity.getName(), is(KEY_STORE_NAME));
    assertThat(entity.getBytes(), is(notNullValue()));
    keyStore = newKeyStore();
    keyStore.load(new ByteArrayInputStream(entity.getBytes()), STORE_PASSWORD);
    assertThat(keyStore.containsAlias(CERT_ALIAS), is(true));
  }
}

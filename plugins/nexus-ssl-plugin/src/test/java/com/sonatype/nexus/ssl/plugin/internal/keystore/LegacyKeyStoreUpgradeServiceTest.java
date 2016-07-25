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

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.app.ApplicationDirectories;
import org.sonatype.nexus.orient.testsupport.DatabaseInstanceRule;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OProperty;
import com.orientechnologies.orient.core.metadata.schema.OSchema;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

public class LegacyKeyStoreUpgradeServiceTest
    extends TestSupport
{
  @Rule
  public DatabaseInstanceRule database = DatabaseInstanceRule.inMemory("test");

  @Mock
  private ApplicationDirectories appDirs;

  private File keystoresDir;

  private LegacyKeyStoreUpgradeService service;

  @Before
  public void setUp() {
    keystoresDir = util.createTempDir();
    when(appDirs.getWorkDirectory("keystores", false)).thenReturn(keystoresDir);
    service = new LegacyKeyStoreUpgradeService(database::getInstance, appDirs);
  }

  @Test
  public void testUpgradeSchema_TypeDoesNotYetExist() throws Exception {
    service.upgradeSchema();
    try (ODatabaseDocumentTx db = database.getInstance().connect()) {
      OSchema schema = db.getMetadata().getSchema();
      OClass type = schema.getClass("key_store");
      assertThat(type, is(notNullValue()));
      OProperty prop = type.getProperty("name");
      assertThat(prop, is(notNullValue()));
      assertThat(prop.isMandatory(), is(true));
      assertThat(prop.isNotNull(), is(true));
      assertThat(prop.getType(), is(OType.STRING));
      prop = type.getProperty("bytes");
      assertThat(prop, is(notNullValue()));
      assertThat(prop.isMandatory(), is(true));
      assertThat(prop.isNotNull(), is(true));
      assertThat(prop.getType(), is(OType.BINARY));
      assertThat(type.getInvolvedIndexes("name"), hasSize(1));
      assertThat(type.getInvolvedIndexes("name").iterator().next().getType(), is("UNIQUE"));
    }
  }

  @Test
  public void testUpgradeSchema_TypeAlreadyExists() throws Exception {
    try (ODatabaseDocumentTx db = database.getInstance().connect()) {
      OSchema schema = db.getMetadata().getSchema();
      OClass type = schema.createClass("key_store");
      type.createProperty("test_property", OType.STRING);
    }
    service.upgradeSchema();
    try (ODatabaseDocumentTx db = database.getInstance().connect()) {
      OSchema schema = db.getMetadata().getSchema();
      OClass type = schema.getClass("key_store");
      assertThat(type, is(notNullValue()));
      assertThat(type.getProperty("test_property"), is(notNullValue()));
      assertThat(type.getProperty("name"), is(nullValue()));
    }
  }

  @Test
  public void testImportKeyStoreFiles_NoLegacyFiles() throws Exception {
    service.upgradeSchema();
    service.importKeyStoreFiles();
    try (ODatabaseDocumentTx db = database.getInstance().connect()) {
      assertThat(db.browseClass("key_store").hasNext(), is(false));
    }
  }

  @Test
  public void testImportKeyStoreFiles_KeyStoresDoNotYetExist() throws Exception {
    Path dir = new File(keystoresDir, "ssl").toPath();
    Files.createDirectory(dir);
    byte[] trustedKeys = "trusted-keys".getBytes(StandardCharsets.UTF_8);
    Files.write(dir.resolve("trusted.ks"), trustedKeys);
    byte[] privateKeys = "private-keys".getBytes(StandardCharsets.UTF_8);
    Files.write(dir.resolve("private.ks"), privateKeys);
    service.upgradeSchema();
    service.importKeyStoreFiles();
    try (ODatabaseDocumentTx db = database.getInstance().connect()) {
      boolean empty = true;
      for (ODocument doc : db.browseClass("key_store")) {
        empty = false;
        String name = doc.field("name");
        byte[] bytes = doc.field("bytes");
        if ("ssl/trusted.ks".equals(name)) {
          assertThat(bytes, is(trustedKeys));
        }
        else if ("ssl/private.ks".equals(name)) {
          assertThat(bytes, is(privateKeys));
        }
        else {
          fail("Unexpected key store entity " + name);
        }
      }
      assertThat(empty, is(false));
    }
  }

  @Test
  public void testImportKeyStoreFiles_KeyStoresAlreadyExist() throws Exception {
    Path dir = new File(keystoresDir, "ssl").toPath();
    Files.createDirectory(dir);
    byte[] trustedKeys = "trusted-keys".getBytes(StandardCharsets.UTF_8);
    Files.write(dir.resolve("trusted.ks"), new byte[0]);
    byte[] privateKeys = "private-keys".getBytes(StandardCharsets.UTF_8);
    Files.write(dir.resolve("private.ks"), new byte[0]);
    service.upgradeSchema();
    try (ODatabaseDocumentTx db = database.getInstance().connect()) {
      ODocument doc = db.newInstance("key_store");
      doc.field("name", "ssl/trusted.ks");
      doc.field("bytes", trustedKeys);
      doc.save();
      doc = db.newInstance("key_store");
      doc.field("name", "ssl/private.ks");
      doc.field("bytes", privateKeys);
      doc.save();
    }
    service.importKeyStoreFiles();
    try (ODatabaseDocumentTx db = database.getInstance().connect()) {
      for (ODocument doc : db.browseClass("key_store")) {
        String name = doc.field("name");
        byte[] bytes = doc.field("bytes");
        if ("ssl/trusted.ks".equals(name)) {
          assertThat(bytes, is(trustedKeys));
        }
        else if ("ssl/private.ks".equals(name)) {
          assertThat(bytes, is(privateKeys));
        }
        else {
          fail("Unexpected key store entity " + name);
        }
      }
    }
  }

  @Test
  public void testDeleteKeyStoreFiles() throws Exception {
    File dir = new File(keystoresDir, "ssl");
    assertThat(dir.mkdirs(), is(true));
    assertThat(new File(dir, "trusted.ks").createNewFile(), is(true));
    assertThat(new File(dir, "private.ks").createNewFile(), is(true));
    service.deleteKeyStoreFiles();
    assertThat(dir.exists(), is(false));
  }
}

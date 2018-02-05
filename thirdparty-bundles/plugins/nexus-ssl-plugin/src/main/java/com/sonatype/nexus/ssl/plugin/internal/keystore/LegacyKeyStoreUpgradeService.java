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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.app.ApplicationDirectories;
import org.sonatype.nexus.orient.DatabaseInstance;
import org.sonatype.nexus.orient.DatabaseInstanceNames;
import org.sonatype.nexus.orient.OClassNameBuilder;
import org.sonatype.nexus.orient.OIndexNameBuilder;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OClass.INDEX_TYPE;
import com.orientechnologies.orient.core.metadata.schema.OSchema;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Supports the upgrade of the SSL key stores from the filesystem to the database.
 * 
 * @since 3.1
 */
@Named
@Singleton
public class LegacyKeyStoreUpgradeService
    extends ComponentSupport
{
  private static final String TRUSTED_KEYS_FILENAME = "trusted.ks";

  private static final String PRIVATE_KEYS_FILENAME = "private.ks";

  private static final String DB_CLASS = new OClassNameBuilder().type("key_store").build();

  private static final String P_NAME = "name";

  private static final String P_BYTES = "bytes";

  private static final String I_NAME = new OIndexNameBuilder().type(DB_CLASS).property(P_NAME).build();

  private final Provider<DatabaseInstance> databaseInstance;

  private final Path keyStoreBasedir;

  @Inject
  public LegacyKeyStoreUpgradeService(@Named(DatabaseInstanceNames.CONFIG) final Provider<DatabaseInstance> databaseInstance,
                                      final ApplicationDirectories appDirectories)
  {
    this.databaseInstance = checkNotNull(databaseInstance);
    keyStoreBasedir = new File(appDirectories.getWorkDirectory("keystores", false), KeyStoreManagerImpl.NAME).toPath();
  }

  public void upgradeSchema() throws Exception {
    log.debug("Updgrading schema for trust store");
    try (ODatabaseDocumentTx db = databaseInstance.get().connect()) {
      OSchema schema = db.getMetadata().getSchema();
      OClass type = schema.getClass(DB_CLASS);
      if (type == null) {
        type = schema.createClass(DB_CLASS);
        type.createProperty(P_NAME, OType.STRING).setMandatory(true).setNotNull(true);
        type.createProperty(P_BYTES, OType.BINARY).setMandatory(true).setNotNull(true);
        type.createIndex(I_NAME, INDEX_TYPE.UNIQUE, P_NAME);
        log.debug("Created schema type {}: properties={}, indexes={}", type, type.properties(), type.getIndexes());
      }
      else {
        // NOTE: Upgrade steps run on each node but within a cluster, another node might already have upgraded the db
        log.debug("Skipped creating existing schema type {}: properties={}, indexes={}", type, type.properties(),
            type.getIndexes());
      }
    }
  }

  public void importKeyStoreFiles() throws Exception {
    log.debug("Importing legacy trust store from {}", keyStoreBasedir);
    try (ODatabaseDocumentTx db = databaseInstance.get().connect()) {
      for (String filename : Arrays.asList(TRUSTED_KEYS_FILENAME, PRIVATE_KEYS_FILENAME)) {
        String keyStoreName = "ssl/" + filename;
        String query = "SELECT FROM " + DB_CLASS + " WHERE " + P_NAME + " = ?";
        List<ODocument> results = db.command(new OSQLSynchQuery<>(query)).execute(keyStoreName);
        if (!results.isEmpty()) {
          log.debug("Skipped import of existing legacy key store {}", results.get(0));
          // NOTE: Upgrade steps run on each node but within a cluster, another node might already have upgraded the db
          continue;
        }
        Path keyStorePath = keyStoreBasedir.resolve(filename);
        if (!Files.isRegularFile(keyStorePath)) {
          continue;
        }
        ODocument doc = db.newInstance(DB_CLASS);
        doc.field(P_NAME, keyStoreName);
        doc.field(P_BYTES, Files.readAllBytes(keyStorePath));
        doc.save();
        log.debug("Imported legacy key store {}", doc);
      }
    }
  }

  public void deleteKeyStoreFiles() {
    log.debug("Deleting legacy trust store from {}", keyStoreBasedir);
    try {
      for (String filename : Arrays.asList(TRUSTED_KEYS_FILENAME, PRIVATE_KEYS_FILENAME)) {
        Files.deleteIfExists(keyStoreBasedir.resolve(filename));
      }
      Files.deleteIfExists(keyStoreBasedir);
    }
    catch (IOException e) { // NOSONAR
      log.warn("Could not delete obsolete trust store from {}. Error: {}", keyStoreBasedir, e.toString());
    }
  }
}

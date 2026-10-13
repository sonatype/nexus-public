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
import java.util.List;

import org.sonatype.nexus.supportzip.ExportSecurityData;
import org.sonatype.nexus.supportzip.ImportData;
import org.sonatype.nexus.supportzip.datastore.JsonExporter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import static java.util.Objects.requireNonNull;

/**
 * Export/Import {@link KeyStoreData} data to/from JSON file for support zip.
 */
@Component
@Qualifier("keyStoreDataExport")
public class KeyStoreDataExport
    extends JsonExporter
    implements ExportSecurityData, ImportData
{
  private final PersistentKeyStoreStorageManager keyStoreStorageManager;

  @Autowired
  public KeyStoreDataExport(final PersistentKeyStoreStorageManager keyStoreStorageManager) {
    this.keyStoreStorageManager = requireNonNull(keyStoreStorageManager);
  }

  @Override
  public void export(final File file) throws IOException {
    log.debug("Export KeyStoreData to {}", file);
    List<KeyStoreData> keyStoreDataList = keyStoreStorageManager.browse();
    exportToJson(keyStoreDataList, file);
  }

  @Override
  public void restore(final File file) throws IOException {
    log.debug("Restoring KeyStoreData from {}", file);
    List<KeyStoreData> keyStoreDataList = importFromJson(file, KeyStoreData.class);
    for (KeyStoreData keyStoreData : keyStoreDataList) {
      keyStoreStorageManager.save(keyStoreData);
    }
  }
}

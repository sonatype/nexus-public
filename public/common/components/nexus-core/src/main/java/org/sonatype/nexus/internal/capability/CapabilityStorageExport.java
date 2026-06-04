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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.sonatype.nexus.internal.capability.storage.CapabilityStorage;
import org.sonatype.nexus.internal.capability.storage.CapabilityStorageItem;
import org.sonatype.nexus.internal.capability.storage.CapabilityStorageItemData;
import org.sonatype.nexus.supportzip.ExportConfigData;
import org.sonatype.nexus.supportzip.ImportData;
import org.sonatype.nexus.supportzip.datastore.JsonExporter;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * Write/Read {@link CapabilityStorageItem} data to/from a JSON file.
 *
 * @since 3.29
 */
@Component
@Qualifier("capabilityStorageExport")
public class CapabilityStorageExport
    extends JsonExporter
    implements ExportConfigData, ImportData
{
  private final CapabilityStorage capabilityStorage;

  @Autowired
  public CapabilityStorageExport(final CapabilityStorage capabilityStorage) {
    this.capabilityStorage = capabilityStorage;
  }

  @Override
  public void export(final File file) throws IOException {
    log.debug("Export CapabilityStorage data to {}", file);
    List<CapabilityStorageItem> capabilities = new ArrayList<>(capabilityStorage.getAll().values());
    exportToJson(capabilities, file);
  }

  @Override
  public void restore(final File file) throws IOException {
    log.debug("Restoring CapabilityStorage data from {}", file);
    importFromJson(file, CapabilityStorageItemData.class).forEach(capabilityStorage::add);
  }
}

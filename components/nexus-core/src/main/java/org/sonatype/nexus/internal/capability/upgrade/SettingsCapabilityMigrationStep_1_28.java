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
package org.sonatype.nexus.internal.capability.upgrade;

import java.sql.Connection;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.internal.capability.storage.CapabilityStorage;
import org.sonatype.nexus.internal.capability.storage.CapabilityStorageItem;
import org.sonatype.nexus.upgrade.datastore.DatabaseMigrationStep;

import static com.google.common.base.Preconditions.checkNotNull;

@Named
@Singleton
public class SettingsCapabilityMigrationStep_1_28
    implements DatabaseMigrationStep
{
  private final CapabilityStorage capabilityStorage;

  @Inject
  public SettingsCapabilityMigrationStep_1_28(final CapabilityStorage capabilityStorage) {
    this.capabilityStorage = checkNotNull(capabilityStorage);
  }

  @Override
  public Optional<String> version() {
    return Optional.of("1.28");
  }

  @Override
  public void migrate(final Connection connection) throws Exception {
    capabilityStorage.getAll().entrySet().stream()
        .filter(e -> e.getValue().getType().equals("rapture.settings"))
        .findFirst()
        .ifPresent(entry -> {
          CapabilityStorageItem capabilityStorageItem = entry.getValue();
          Map<String, String> properties = capabilityStorageItem.getProperties();
          properties.putIfAbsent("requestTimeout", "60");
          properties.putIfAbsent("longRequestTimeout", "180");
          capabilityStorage.update(entry.getKey(), capabilityStorageItem);
        });
  }
}

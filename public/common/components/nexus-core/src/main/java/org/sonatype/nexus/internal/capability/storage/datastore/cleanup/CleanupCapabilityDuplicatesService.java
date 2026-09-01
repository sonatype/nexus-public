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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.sonatype.nexus.capability.CapabilityIdentity;
import org.sonatype.nexus.internal.capability.storage.CapabilityStorageItem;
import org.sonatype.nexus.internal.capability.storage.upgrade.UpgradeCapabilityStorage;

import com.google.common.annotations.VisibleForTesting;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Remove all capability duplicate records from storage.
 */
@Component
public class CleanupCapabilityDuplicatesService
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  private final UpgradeCapabilityStorage capabilityStorage;

  @Autowired
  public CleanupCapabilityDuplicatesService(final UpgradeCapabilityStorage capabilityStorage) {
    this.capabilityStorage = checkNotNull(capabilityStorage);
  }

  public void doCleanup() {
    Map<CapabilityStorageItem, List<CapabilityIdentity>> duplicateCapabilities = browseCapabilityDuplicates();
    if (duplicateCapabilities.isEmpty()) {
      log.debug("No capabilities duplicates found.");
      return;
    }

    List<CapabilityIdentity> toRemove = new ArrayList<>();
    duplicateCapabilities.forEach((item, duplicates) -> {
      log.info("Cleaning up {} duplicates for {} capability", duplicates.size() - 1, item.getType());

      duplicates.stream()
          .skip(1) // leave one capability in the storage
          .forEach(toRemove::add);
    });

    // Remove all duplicates in a single batched transaction rather than one connection acquisition + commit
    // (fsync on PostgreSQL) per row, which on instances with many duplicates is the startup-outage failure
    // mode NEXUS-53442 targets.
    int removed = capabilityStorage.removeAll(toRemove);
    log.debug("Removed {} capability duplicate(s)", removed);
  }

  /**
   * Find capability duplicates.
   *
   * @return duplicates capability identities grouped by capability
   */
  @VisibleForTesting
  Map<CapabilityStorageItem, List<CapabilityIdentity>> browseCapabilityDuplicates() {
    return capabilityStorage.getAll()
        .entrySet()
        .stream()
        .collect(Collectors.groupingBy(Entry::getValue))
        .entrySet()
        .stream()
        .filter(f -> f.getValue().size() > 1)
        .collect(Collectors.toMap(
            Entry::getKey,
            entry -> entry.getValue()
                .stream()
                .map(Entry::getKey)
                .collect(Collectors.toList())));
  }
}

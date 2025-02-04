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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.internal.capability.storage.CapabilityStorage;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Remove all capability duplicate records from storage.
 */
@Named
@Singleton
public class CleanupCapabilityDuplicatesService
    extends ComponentSupport
{
  private final CapabilityStorage capabilityStorage;

  @Inject
  public CleanupCapabilityDuplicatesService(final CapabilityStorage capabilityStorage) {
    this.capabilityStorage = checkNotNull(capabilityStorage);
  }

  public void doCleanup() {
    if (!capabilityStorage.isDuplicatesFound()) {
      log.debug("No capabilities duplicates found.");
      return;
    }

    capabilityStorage.browseCapabilityDuplicates().forEach((typeId, duplicates) -> {
      log.info("Cleaning up {} duplicates for {} capability", duplicates.size() - 1, typeId);

      duplicates.stream()
          .skip(1) // left one capability in the storage
          .forEach(identity -> {
            if (capabilityStorage.remove(identity)) {
              log.debug("Capability duplicate {} removed", identity);
            }
          });
    });
  }
}

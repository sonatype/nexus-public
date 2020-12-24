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
package org.sonatype.nexus.cleanup.storage;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.cleanup.internal.storage.CleanupPolicyData;
import org.sonatype.nexus.supportzip.ExportConfigData;
import org.sonatype.nexus.supportzip.ImportData;
import org.sonatype.nexus.supportzip.datastore.JsonExporter;

/**
 * Write/Read {@link CleanupPolicy} data to/from a JSON file.
 *
 * @since 3.29
 */
@Named("cleanupPolicyExport")
@Singleton
public class CleanupPolicyExport
    extends JsonExporter
    implements ExportConfigData, ImportData
{
  private final CleanupPolicyStorage policyStorage;

  @Inject
  public CleanupPolicyExport(final CleanupPolicyStorage policyStorage) {
    this.policyStorage = policyStorage;
  }

  @Override
  public void export(final File file) throws IOException {
    log.debug("Export CleanupPolicy data to {}", file);
    List<CleanupPolicy> cleanupPolicies = policyStorage.getAll();
    exportToJson(cleanupPolicies, file);
  }

  @Override
  public void restore(final File file) throws IOException {
    log.debug("Restoring CleanupPolicy data from {}", file);
    importFromJson(file, CleanupPolicyData.class).forEach(policyStorage::add);
  }
}

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
package org.sonatype.nexus.blobstore.internal.metrics;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.upgrade.AvailabilityVersion;
import org.sonatype.nexus.formfields.ComboboxFormField;
import org.sonatype.nexus.scheduling.TaskDescriptorSupport;

import static org.sonatype.nexus.blobstore.common.BlobStoreTaskSupport.BLOBSTORE_NAME_FIELD_ID;
import static org.sonatype.nexus.formfields.FormField.MANDATORY;

@AvailabilityVersion(from = "2.0")
@Named
@Singleton
public class BlobStoreMetricsMigrationTaskDescriptor
    extends TaskDescriptorSupport
{
  private static final String EXPOSED_FLAG = "${nexus.blobstore.metrics.migration.task.expose:-false}";

  @Inject
  public BlobStoreMetricsMigrationTaskDescriptor(@Named(EXPOSED_FLAG) final boolean exposed) {
    super(BlobStoreMetricsMigrationTask.TYPE_ID,
        BlobStoreMetricsMigrationTask.class,
        "Migration - Move blobstore metrics to the database",
        VISIBLE,
        exposed,
        new ComboboxFormField<String>(
            BLOBSTORE_NAME_FIELD_ID,
            "Blob store",
            "Select the blob store(s) to obtain metrics for",
            MANDATORY
          )
          .withStoreApi("coreui_Blobstore.ReadNoneGroupEntriesIncludingEntryForAll")
          .withIdMapping("name"));
  }
}

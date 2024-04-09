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
package org.sonatype.nexus.blobstore.metrics.reconcile;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.formfields.ComboboxFormField;
import org.sonatype.nexus.scheduling.TaskDescriptorSupport;

import static org.sonatype.nexus.common.app.FeatureFlags.RECALCULATE_BLOBSTORE_SIZE_TASK_ENABLED_NAMED;
import static org.sonatype.nexus.formfields.FormField.MANDATORY;

/**
 * Task descriptor for  {@link RecalculateBlobStoreSizeTask}
 */
@Named
@Singleton
public class RecalculateBlobStoreSizeTaskDescriptor
    extends TaskDescriptorSupport
{
  public static final String TYPE_ID = "blobstore.metrics.reconcile";

  public static final String BLOB_STORE_NAME_FIELD_ID = "blobstoreName";

  @Inject
  public RecalculateBlobStoreSizeTaskDescriptor(
      @Named(RECALCULATE_BLOBSTORE_SIZE_TASK_ENABLED_NAMED) final boolean taskEnabled)
  {
    super(TYPE_ID,
        RecalculateBlobStoreSizeTask.class,
        "Repair - Recalculate blob store storage",
        VISIBLE,
        taskEnabled,
        new ComboboxFormField<String>(
            BLOB_STORE_NAME_FIELD_ID,
            "Blob store",
            "Select the blob store(s) to recalculate",
            MANDATORY
        ).withStoreApi("coreui_Blobstore.ReadNoneGroupEntriesIncludingEntryForAll")
            .withIdMapping("name")
    );
  }
}

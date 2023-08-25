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
package org.sonatype.nexus.blobstore.deletetemp;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.formfields.ComboboxFormField;
import org.sonatype.nexus.formfields.NumberTextFormField;
import org.sonatype.nexus.scheduling.TaskDescriptorSupport;

import static org.sonatype.nexus.formfields.FormField.MANDATORY;
import static org.sonatype.nexus.formfields.FormField.OPTIONAL;

@Named
@Singleton
public class DeleteBlobstoreTempFilesTaskDescriptor
    extends TaskDescriptorSupport
{
  public static final String TYPE_ID = "blobstore.delete-temp-files";

  public static final String BLOB_STORE_NAME_FIELD_ID = "blobstoreName";

  public static final String DAYS_OLDER_THAN = "olderThanDays";

  @Inject
  public DeleteBlobstoreTempFilesTaskDescriptor() {
    super(TYPE_ID,
        DeleteBlobstoreTempFilesTask.class,
        "Admin - Delete blob store temporary files",
        VISIBLE,
        EXPOSED,
        new ComboboxFormField<String>(
            BLOB_STORE_NAME_FIELD_ID,
            "Blob store",
            "Select the blob store to delete temporary files",
            MANDATORY
        ).withStoreApi("coreui_Blobstore.read").withIdMapping("name"),
        new NumberTextFormField(DAYS_OLDER_THAN,
            "Delete Files Older Than X days",
            "Delete files that are older than X days", OPTIONAL).withMinimumValue(0)
    );
  }
}

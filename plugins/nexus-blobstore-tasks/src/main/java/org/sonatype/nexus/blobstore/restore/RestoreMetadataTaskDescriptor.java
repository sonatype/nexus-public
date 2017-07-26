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
package org.sonatype.nexus.blobstore.restore;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.formfields.ComboboxFormField;
import org.sonatype.nexus.scheduling.TaskDescriptorSupport;

import static org.sonatype.nexus.formfields.FormField.MANDATORY;

/**
 * @since 3.4
 */
@Named
@Singleton
public class RestoreMetadataTaskDescriptor
    extends TaskDescriptorSupport
{
  static final String TYPE_ID = "blobstore.rebuildComponentDB";

  static final String BLOB_STORE_NAME_FIELD_ID = "blobstoreName";

  RestoreMetadataTaskDescriptor() {
    super(TYPE_ID,
        RestoreMetadataTask.class,
        "Restore Asset/Component metadata from Blob Store",
        VISIBLE,
        EXPOSED,
        new ComboboxFormField<String>(
            BLOB_STORE_NAME_FIELD_ID,
            "Blob store",
            "Select the blob store to restore",
            MANDATORY
        ).withStoreApi("coreui_Blobstore.read").withIdMapping("name"));
  }
}

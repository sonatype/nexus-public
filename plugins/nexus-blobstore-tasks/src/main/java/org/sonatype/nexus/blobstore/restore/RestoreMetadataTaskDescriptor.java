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

import org.sonatype.goodies.i18n.I18N;
import org.sonatype.goodies.i18n.MessageBundle;
import org.sonatype.nexus.formfields.CheckboxFormField;
import org.sonatype.nexus.formfields.ComboboxFormField;
import org.sonatype.nexus.scheduling.TaskDescriptorSupport;

import static org.sonatype.nexus.formfields.FormField.MANDATORY;
import static org.sonatype.nexus.formfields.FormField.OPTIONAL;

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

  static final String RESTORE_BLOBS = "restoreBlobs";

  static final String UNDELETE_BLOBS = "undeleteBlobs";

  static final String INTEGRITY_CHECK = "integrityCheck";

  static final String DRY_RUN = "dryRun";

  private interface Messages extends MessageBundle {
    @DefaultMessage("Repair - Reconcile component database from blob store")
    String name();

    @DefaultMessage("Blob store")
    String blobstoreNameLabel();

    @DefaultMessage("Select the blob store to restore")
    String blobstoreNameHelpText();

    @DefaultMessage("Restore blob metadata")
    String restoreBlobsLabel();

    @DefaultMessage("Restore missing metadata for blobs found in the blob store.")
    String restoreBlobsHelpText();

    @DefaultMessage("Un-delete referenced blobs")
    String undeleteBlobsLabel();

    @DefaultMessage("Un-delete blobs which still have metadata referencing them.")
    String undeleteBlobsHelpText();

    @DefaultMessage("Dry Run")
    String dryRunLabel();

    @DefaultMessage("Log actions, but make no changes.")
    String dryRunHelpText();

    @DefaultMessage("Integrity check")
    String integrityCheckLabel();

    @DefaultMessage("Verify integrity between asset metadata and blob properties")
    String integrityCheckHelpText();
  }

  private static final Messages messages = I18N.create(Messages.class);

  RestoreMetadataTaskDescriptor() {
    super(TYPE_ID,
        RestoreMetadataTask.class,
        messages.name(),
        VISIBLE,
        EXPOSED,
        new ComboboxFormField<String>(
            BLOB_STORE_NAME_FIELD_ID,
            messages.blobstoreNameLabel(),
            messages.blobstoreNameHelpText(),
            MANDATORY
        ).withStoreApi("coreui_Blobstore.read").withIdMapping("name"),
        new CheckboxFormField(DRY_RUN,
            messages.dryRunLabel(),
            messages.dryRunHelpText(), OPTIONAL).withInitialValue(false),
        new CheckboxFormField(RESTORE_BLOBS,
            messages.restoreBlobsLabel(),
            messages.restoreBlobsHelpText(), OPTIONAL).withInitialValue(true),
        new CheckboxFormField(UNDELETE_BLOBS,
            messages.undeleteBlobsLabel(),
            messages.undeleteBlobsHelpText(), OPTIONAL).withInitialValue(true),
        new CheckboxFormField(INTEGRITY_CHECK,
            messages.integrityCheckLabel(),
            messages.integrityCheckHelpText(), OPTIONAL).withInitialValue(true)
    );
  }
}

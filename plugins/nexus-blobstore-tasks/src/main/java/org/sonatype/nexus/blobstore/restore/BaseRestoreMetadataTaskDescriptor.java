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

import org.sonatype.goodies.i18n.I18N;
import org.sonatype.goodies.i18n.MessageBundle;
import org.sonatype.nexus.blobstore.restore.datastore.RestoreMetadataTask;
import org.sonatype.nexus.formfields.CheckboxFormField;
import org.sonatype.nexus.formfields.ComboboxFormField;
import org.sonatype.nexus.formfields.NumberTextFormField;
import org.sonatype.nexus.scheduling.TaskDescriptorSupport;

import static org.sonatype.nexus.formfields.FormField.MANDATORY;
import static org.sonatype.nexus.formfields.FormField.OPTIONAL;

/**
 * @since 3.29
 */
public abstract class BaseRestoreMetadataTaskDescriptor
    extends TaskDescriptorSupport
{
  public static final String TYPE_ID = "blobstore.rebuildComponentDB";

  public static final String BLOB_STORE_NAME_FIELD_ID = "blobstoreName";

  public static final String RESTORE_BLOBS = "restoreBlobs";

  public static final String UNDELETE_BLOBS = "undeleteBlobs";

  public static final String INTEGRITY_CHECK = "integrityCheck";

  public static final String DRY_RUN = "dryRun";

  public static final String SINCE_DAYS = "sinceDays";

  private static final Messages messages = I18N.create(Messages.class);

  public BaseRestoreMetadataTaskDescriptor(final boolean exposed) {
    super(TYPE_ID,
        RestoreMetadataTask.class,
        messages.name(),
        VISIBLE,
        exposed,
        new ComboboxFormField<String>(
            BLOB_STORE_NAME_FIELD_ID,
            messages.blobstoreNameLabel(),
            messages.blobstoreNameHelpText(),
            MANDATORY
        ).withStoreApi("coreui_Blobstore.read").withIdMapping("name"),
        new CheckboxFormField(DRY_RUN,
            messages.dryRunLabel(),
            messages.dryRunHelpText(), OPTIONAL).withInitialValue(false),
        new NumberTextFormField(SINCE_DAYS,
            messages.sinceDaysLabel(),
            messages.sinceDaysHelpText(), OPTIONAL).withMinimumValue(0),
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

  private interface Messages
      extends MessageBundle
  {
    @DefaultMessage("Repair - Reconcile component database from blob store")
    String name();

    @DefaultMessage("Blob store")
    String blobstoreNameLabel();

    @DefaultMessage("Select the blob store to restore")
    String blobstoreNameHelpText();

    @DefaultMessage("Restore blob metadata")
    String restoreBlobsLabel();

    @DefaultMessage("Restore missing metadata for blobs found in the blob store")
    String restoreBlobsHelpText();

    @DefaultMessage("Un-delete referenced blobs")
    String undeleteBlobsLabel();

    @DefaultMessage("Un-delete blobs which still have metadata referencing them")
    String undeleteBlobsHelpText();

    @DefaultMessage("Dry Run")
    String dryRunLabel();

    @DefaultMessage("Log actions, but make no changes.")
    String dryRunHelpText();

    @DefaultMessage("Integrity check")
    String integrityCheckLabel();

    @DefaultMessage("Verify integrity between asset metadata and blob properties")
    String integrityCheckHelpText();

    @DefaultMessage("Only blobs created since X days ago")
    String sinceDaysLabel();

    @DefaultMessage("Attempt to reconcile blobs only created within specified last number of days (inclusive). " +
        "Leave empty to reconcile all blobs (this may take a very long time to finish)")
    String sinceDaysHelpText();
  }
}

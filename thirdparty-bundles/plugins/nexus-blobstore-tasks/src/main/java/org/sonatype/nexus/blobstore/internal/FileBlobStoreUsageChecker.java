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
package org.sonatype.nexus.blobstore.internal;

import java.util.List;
import java.util.function.Supplier;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.nexus.blobstore.api.BlobId;
import org.sonatype.nexus.blobstore.api.BlobRef;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreUsageChecker;
import org.sonatype.nexus.orient.DatabaseInstance;
import org.sonatype.nexus.repository.storage.ComponentDatabase;

import com.codahale.metrics.annotation.Timed;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;

/**
 * Check if a blob is reference in the corresponding metadata.
 *
 * @since 3.6
 */
@Named
@Singleton
public class FileBlobStoreUsageChecker
    implements BlobStoreUsageChecker
{
  private static final String ANY_NODE = "%";

  private final OSQLSynchQuery<ODocument> assetBlobRefQuery = new OSQLSynchQuery<>(
      "SELECT FROM asset WHERE name = ? AND blob_ref LIKE ? LIMIT 1"
  );

  private final Supplier<ODatabaseDocumentTx> txSupplier;

  @Inject
  public FileBlobStoreUsageChecker(@Named(ComponentDatabase.NAME) final Provider<DatabaseInstance> databaseInstanceProvider)
  {
    this.txSupplier = () -> databaseInstanceProvider.get().acquire();
  }

  @Override
  @Timed
  public boolean test(final BlobStore blobStore, final BlobId blobId, final String blobName) {
    BlobRef blobRef = new BlobRef(ANY_NODE, blobStore.getBlobStoreConfiguration().getName(), blobId.asUniqueString());
    try (ODatabaseDocumentTx tx = txSupplier.get()) {
      tx.begin();
      List<ODocument> results = tx.command(assetBlobRefQuery).execute(blobName, blobRef.toString());
      return !results.isEmpty();
    }
  }
}

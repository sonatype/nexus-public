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
package org.sonatype.nexus.repository.internal.blobstore.upgrade;

import java.util.List;
import java.util.Optional;

import org.sonatype.nexus.blobstore.BlobStoreDescriptor;
import org.sonatype.nexus.crypto.secrets.SecretsService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Re-runs the blob store credential encryption sweep for blob store types that only began declaring
 * their credentials sensitive after {@link BlobStoreCredentialEncryptionMigrationStep_2_160} shipped.
 *
 * <p>
 * A migration step never runs again once its version is recorded, but the work this sweep performs is
 * not fixed at the time the step is written: it encrypts whichever fields each
 * {@link org.sonatype.nexus.blobstore.BlobStoreDescriptor#getSensitiveConfigurationFields()} declares
 * when the step actually executes. NEXUS-54313 added that declaration to the Azure descriptor, so on any
 * instance that had already recorded 2.160 the Azure {@code accountKey} would have been passed over and
 * left in plaintext indefinitely. This step closes that window.
 * </p>
 *
 * <p>
 * The sweep is idempotent — credentials encrypted by 2.160 are stored as {@code _<id>} secret references
 * and are skipped on sight — so running it a second time re-encrypts nothing and only picks up values
 * that were still plaintext.
 * </p>
 */
@Component
public class BlobStoreCredentialEncryptionMigrationStep_2_165
    extends BlobStoreCredentialEncryptionMigrationStepSupport
{
  @Autowired
  public BlobStoreCredentialEncryptionMigrationStep_2_165(
      final SecretsService secretsService,
      final List<BlobStoreDescriptor> blobStoreDescriptors)
  {
    super(secretsService, blobStoreDescriptors);
  }

  @Override
  public Optional<String> version() {
    return Optional.of("2.165");
  }
}

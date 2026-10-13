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
 * One-time migration that encrypts any plaintext credentials in blob store configurations.
 *
 * <p>
 * The sweep itself lives in {@link BlobStoreCredentialEncryptionMigrationStepSupport}; see
 * {@link BlobStoreCredentialEncryptionMigrationStep_2_165} for why a second version-numbered step
 * running the same sweep exists.
 * </p>
 */
@Component
public class BlobStoreCredentialEncryptionMigrationStep_2_160
    extends BlobStoreCredentialEncryptionMigrationStepSupport
{
  @Autowired
  public BlobStoreCredentialEncryptionMigrationStep_2_160(
      final SecretsService secretsService,
      final List<BlobStoreDescriptor> blobStoreDescriptors)
  {
    super(secretsService, blobStoreDescriptors);
  }

  @Override
  public Optional<String> version() {
    return Optional.of("2.160");
  }
}

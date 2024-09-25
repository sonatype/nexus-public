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
package org.sonatype.nexus.repository.internal.blobstore.secrets.migration;

import java.util.Map;
import java.util.Objects;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.crypto.secrets.Secret;
import org.sonatype.nexus.crypto.secrets.SecretsService;
import org.sonatype.nexus.scheduling.CancelableHelper;
import org.sonatype.nexus.security.secrets.SecretMigrationException;
import org.sonatype.nexus.security.secrets.SecretsMigrator;

import com.google.common.annotations.VisibleForTesting;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * migrates existing values from the legacy secret format to the new secret format.
 */
@Named
public class BlobStoreConfigSecretsMigrator
    implements SecretsMigrator
{
   @VisibleForTesting
   final static String S3_TYPE = "s3";

   @VisibleForTesting
   static final String SECRET_ACCESS_KEY_KEY = "secretAccessKey";

  private final BlobStoreManager blobStoreManager;

  private final SecretsService secretsService;

  @Inject
  public BlobStoreConfigSecretsMigrator(final BlobStoreManager blobStoreManager, final SecretsService secretsService) {
    this.blobStoreManager = checkNotNull(blobStoreManager);
    this.secretsService = checkNotNull(secretsService);
  }

  @Override
  public void migrate() throws SecretMigrationException {
    for (BlobStore blobStore : blobStoreManager.browse()) {
      CancelableHelper.checkCancellation();

      try {
        BlobStoreConfiguration currentConfig = blobStore.getBlobStoreConfiguration();

        if (S3_TYPE.equalsIgnoreCase(currentConfig.getType())) {
          maybeMigrateSecret(currentConfig);
        }
      }
      catch (Exception e) {
        throw new SecretMigrationException(
            "unable to migrate secrets for blobstore: " + blobStore.getBlobStoreConfiguration().getName(), e);
      }
    }
  }

  private void maybeMigrateSecret(final BlobStoreConfiguration blobStoreConfiguration) throws Exception {
    BlobStoreConfiguration blobStoreConfigurationCopy = blobStoreConfiguration.copy(blobStoreConfiguration.getName());
    Map<String, Object> s3Attributes = blobStoreConfigurationCopy.getAttributes().get(S3_TYPE);

    if (Objects.nonNull(s3Attributes) && s3Attributes.containsKey(SECRET_ACCESS_KEY_KEY)) {
      String secretAccessKey = (String) s3Attributes.get(SECRET_ACCESS_KEY_KEY);
      Secret existing = secretsService.from(secretAccessKey);

      if (isLegacyEncryptedString(existing)) {
        //put decrypted value in the map, so that it can be encrypted and saved back
        s3Attributes.put(SECRET_ACCESS_KEY_KEY, new String(existing.decrypt()));
        blobStoreManager.update(blobStoreConfigurationCopy);
      }
    }
  }
}

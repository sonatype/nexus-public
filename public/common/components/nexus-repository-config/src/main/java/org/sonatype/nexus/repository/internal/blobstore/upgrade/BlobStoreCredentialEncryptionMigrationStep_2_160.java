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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.sonatype.nexus.blobstore.BlobStoreDescriptor;
import org.sonatype.nexus.common.QualifierUtil;
import org.sonatype.nexus.crypto.secrets.Secret;
import org.sonatype.nexus.crypto.secrets.SecretsService;
import org.sonatype.nexus.upgrade.datastore.DatabaseMigrationStep;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * One-time migration that encrypts any plaintext credentials in blob store configurations.
 *
 * <p>
 * Prior to NEXUS-54061, sensitive blob store credentials (like AWS secret keys, Azure account keys)
 * were stored as plaintext in the database. This migration scans all blob store configurations,
 * identifies plaintext values in fields declared as sensitive by their descriptors, and encrypts
 * them using the {@link SecretsService}.
 * </p>
 *
 * <p>
 * The migration is idempotent: values already stored as secret ID references (starting with {@code _})
 * are skipped. It is also safe to re-run on configurations that were partially migrated before a failure.
 * </p>
 *
 * @since 3.96
 */
@Component
public class BlobStoreCredentialEncryptionMigrationStep_2_160
    implements DatabaseMigrationStep
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  private static final String BLOBSTORE_CONFIG = "blobstore-config";

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final SecretsService secretsService;

  private final List<BlobStoreDescriptor> blobStoreDescriptors;

  @Autowired
  public BlobStoreCredentialEncryptionMigrationStep_2_160(
      final SecretsService secretsService,
      final List<BlobStoreDescriptor> blobStoreDescriptors)
  {
    this.secretsService = secretsService;
    this.blobStoreDescriptors = blobStoreDescriptors;
  }

  @Override
  public Optional<String> version() {
    return Optional.of("2.160");
  }

  @Override
  public void migrate(final Connection connection) throws Exception {
    log.info("Starting blob store credential encryption migration");

    // Build descriptor map keyed by type
    Map<String, BlobStoreDescriptor> descriptors = QualifierUtil.buildQualifierBeanMap(blobStoreDescriptors);
    if (descriptors == null || descriptors.isEmpty()) {
      log.debug("No blob store descriptors registered; nothing to encrypt");
      return;
    }

    int encryptedCount = 0;

    // Read all blob store configurations
    try (PreparedStatement stmt = connection.prepareStatement(
        "SELECT id, name, type, attributes FROM blob_store_configuration")) {
      ResultSet rs = stmt.executeQuery();

      while (rs.next()) {
        String id = rs.getString("id");
        String name = rs.getString("name");
        String type = rs.getString("type");
        String attributesJson = rs.getString("attributes");

        Map<String, Object> attributes = OBJECT_MAPPER.readValue(attributesJson, Map.class);
        int result = encryptPlaintextCredentials(connection, id, name, type, attributes, descriptors);
        encryptedCount += result;
      }
    }

    log.info("Blob store credential encryption migration complete. Encrypted {} credential(s)", encryptedCount);
  }

  /**
   * Encrypts any plaintext credentials in a single blob store configuration.
   *
   * @return the number of credentials encrypted
   */
  private int encryptPlaintextCredentials(
      final Connection connection,
      final String configId,
      final String configName,
      final String typeKey,
      final Map<String, Object> attributes,
      final Map<String, BlobStoreDescriptor> descriptors) throws Exception
  {
    if (typeKey == null) {
      return 0;
    }

    BlobStoreDescriptor descriptor = descriptors.get(typeKey);
    if (descriptor == null) {
      log.debug("No descriptor for blob store type '{}' (config '{}'); skipping", typeKey, configName);
      return 0;
    }

    List<String> sensitiveAttributes = descriptor.getSensitiveConfigurationFields();
    if (sensitiveAttributes == null || sensitiveAttributes.isEmpty()) {
      return 0;
    }

    String typeLower = typeKey.toLowerCase();
    Object typeDataObj = attributes.get(typeLower);
    if (!(typeDataObj instanceof Map)) {
      return 0;
    }

    @SuppressWarnings("unchecked")
    Map<String, Object> typeData = (Map<String, Object>) typeDataObj;

    List<Secret> secretsCreated = new ArrayList<>();
    int encryptedCount = 0;

    for (String sensitiveAttrKey : sensitiveAttributes) {
      Object valueObj = typeData.get(sensitiveAttrKey);
      if (!(valueObj instanceof String)) {
        continue;
      }
      String value = (String) valueObj;

      // Skip if already a secret ID reference (starts with _ followed by a digit)
      if (isSecretIdReference(value)) {
        log.debug("Blob store '{}' attribute '{}/{}' already encrypted; skipping",
            configName, typeKey, sensitiveAttrKey);
        continue;
      }

      // Check if the value is actually plaintext by attempting decryption
      // If decryption returns a different value, it's encrypted legacy PBE
      char[] decrypted;
      try {
        decrypted = secretsService.from(value).decrypt();
      }
      catch (Exception e) {
        log.error("Failed to inspect credential for blob store '{}' attribute '{}/{}'; leaving as-is",
            configName, typeKey, sensitiveAttrKey, e);
        continue;
      }

      // If decrypted differs from original, it was legacy PBE encrypted - leave it alone
      if (!Arrays.equals(decrypted, value.toCharArray())) {
        log.debug("Blob store '{}' attribute '{}/{}' appears to be legacy PBE encrypted; skipping",
            configName, typeKey, sensitiveAttrKey);
        continue;
      }

      // It's plaintext - encrypt it
      try {
        log.info("Encrypting plaintext credential for blob store '{}' attribute '{}/{}'",
            configName, typeKey, sensitiveAttrKey);

        // Use null userId since migration runs before Shiro is available
        Secret newSecret = secretsService.encryptMaven(
            BLOBSTORE_CONFIG,
            decrypted,
            null);

        typeData.put(sensitiveAttrKey, newSecret.getId());
        secretsCreated.add(newSecret);
        encryptedCount++;
      }
      catch (Exception e) {
        log.error("Failed to encrypt plaintext credential for blob store '{}' attribute '{}/{}'",
            configName, typeKey, sensitiveAttrKey, e);
      }
    }

    // Persist the changes if any credentials were encrypted
    if (!secretsCreated.isEmpty()) {
      try {
        String updatedAttributes = OBJECT_MAPPER.writeValueAsString(attributes);
        boolean h2 = isH2(connection);

        try (PreparedStatement updateStmt = connection.prepareStatement(
            "UPDATE blob_store_configuration SET attributes = ? WHERE id = ?")) {
          // Use the default method from DatabaseMigrationStep interface
          setJsonParameter(updateStmt, 1, updatedAttributes.getBytes("UTF-8"), h2);
          updateStmt.setString(2, configId);
          updateStmt.executeUpdate();
        }
      }
      catch (Exception e) {
        log.error("Failed to persist encrypted credentials for blob store '{}'; rolling back {} secret(s)",
            configName, secretsCreated.size(), e);

        // Rollback by removing the orphaned secrets
        for (Secret orphan : secretsCreated) {
          try {
            secretsService.remove(orphan);
          }
          catch (Exception removeError) {
            log.error("Failed to cleanup orphan secret {} for blob store '{}'",
                orphan.getId(), configName, removeError);
          }
        }
        return 0;
      }
    }

    return encryptedCount;
  }

  /**
   * Returns true if the value appears to be a secret ID reference (starts with {@code _} followed by a digit).
   */
  private boolean isSecretIdReference(final String value) {
    return value != null && value.startsWith("_") && value.length() > 1 && Character.isDigit(value.charAt(1));
  }
}

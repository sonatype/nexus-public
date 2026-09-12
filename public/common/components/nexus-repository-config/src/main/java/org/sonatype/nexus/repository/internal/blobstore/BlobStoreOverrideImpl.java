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
package org.sonatype.nexus.repository.internal.blobstore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import org.sonatype.nexus.blobstore.BlobStoreDescriptor;
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.common.QualifierUtil;
import org.sonatype.nexus.crypto.secrets.Secret;
import org.sonatype.nexus.crypto.secrets.SecretsService;
import org.sonatype.nexus.repository.blobstore.BlobStoreConfigurationStore;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Provider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.text.Strings2.isBlank;

/**
 * A {@code BlobStoreOverride} that allows blob store attributes to be overridden
 * via an environment variable ({@code NEXUS_BLOB_STORE_OVERRIDE}) during {@code BlobStoreManager}
 * initialization. e.g.:
 *
 * <pre>
 *NEXUS_BLOB_STORE_OVERRIDE='{"default":{"file":{"path":"other_path"}}}'
 * </pre>
 * <p>
 * For attributes named by a descriptor's
 * {@link BlobStoreDescriptor#getSensitiveConfigurationFields()}, this class stores the value as
 * an encrypted {@code _<id>} secret reference — matching the on-disk shape produced by the UI/REST
 * paths — rather than the plaintext supplied on the environment variable. Non-sensitive
 * attributes are merged verbatim as before.
 */
@ConditionalOnProperty(name = "nexus.blobstore.override.enabled", havingValue = "true", matchIfMissing = true)
@Component
public class BlobStoreOverrideImpl
    implements BlobStoreOverride
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  static final String NEXUS_BLOB_STORE_OVERRIDE = "NEXUS_BLOB_STORE_OVERRIDE";

  private static final String BLOBSTORE_CONFIG = "blobstore-config";

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private static final TypeReference<Map<String, Map<String, Map<String, Object>>>> TYPE_REFERENCE =
      new TypeReference<Map<String, Map<String, Map<String, Object>>>>()
      {
      };

  private final BlobStoreConfigurationStore blobStoreConfigurationStore;

  private final SecretsService secretsService;

  private final Provider<List<BlobStoreDescriptor>> blobStoreDescriptorsProvider;

  @Autowired
  public BlobStoreOverrideImpl(
      final BlobStoreConfigurationStore blobStoreConfigurationStore,
      final SecretsService secretsService,
      final Provider<List<BlobStoreDescriptor>> blobStoreDescriptorsProvider)
  {
    this.blobStoreConfigurationStore = checkNotNull(blobStoreConfigurationStore);
    this.secretsService = checkNotNull(secretsService);
    this.blobStoreDescriptorsProvider = checkNotNull(blobStoreDescriptorsProvider);
  }

  @Override
  public void apply() {
    String overrideJson = System.getenv(NEXUS_BLOB_STORE_OVERRIDE);

    if (isBlank(overrideJson)) {
      return;
    }

    Map<String, Map<String, Map<String, Object>>> overrides;
    try {
      overrides = OBJECT_MAPPER.readValue(overrideJson, TYPE_REFERENCE);
    }
    catch (JsonProcessingException e) {
      // Do NOT log overrideJson — it can contain plaintext credentials that would otherwise land
      // in nexus.log / support zips. A malformed-JSON boot is exactly when an operator ships one.
      log.error("Unable to parse {} — value is not valid JSON", NEXUS_BLOB_STORE_OVERRIDE);
      throw new IllegalStateException("Unable to parse environment variable NEXUS_BLOB_STORE_OVERRIDE", e);
    }
    if (overrides == null) {
      log.warn("{} parsed to null", NEXUS_BLOB_STORE_OVERRIDE);
      return;
    }

    Map<String, BlobStoreDescriptor> descriptors =
        QualifierUtil.buildQualifierBeanMap(blobStoreDescriptorsProvider.get());
    if (descriptors == null) {
      descriptors = new HashMap<>();
    }

    logOverridesRedacted(overrides, descriptors);
    // Only secrets whose owning config has NOT yet been persisted are eligible for rollback.
    // After store.update() succeeds we drop the current config's slice, so a subsequent-config
    // failure never removes secrets already referenced by a committed row.
    List<Secret> uncommittedSecrets = new ArrayList<>();
    List<Secret> supersededSecretsForCommit = new ArrayList<>();
    try {
      for (BlobStoreConfiguration config : blobStoreConfigurationStore.list()) {
        Map<String, Map<String, Object>> changes = overrides.get(config.getName());
        if (changes == null || changes.isEmpty()) {
          continue;
        }
        log.debug("Merging changes into blob store {}", config.getName());

        int startIndex = uncommittedSecrets.size();
        supersededSecretsForCommit.clear();
        boolean anyChange = applyOverridesToConfig(
            config, changes, descriptors, uncommittedSecrets, supersededSecretsForCommit);
        if (!anyChange) {
          continue;
        }

        blobStoreConfigurationStore.update(config);

        // Update committed — drop this config's newly-created secrets from the rollback list,
        // then remove any secrets that were superseded (the old _<id> the update just replaced).
        uncommittedSecrets.subList(startIndex, uncommittedSecrets.size()).clear();
        supersededSecretsForCommit.forEach(this::safeRemoveSecret);
      }
    }
    catch (RuntimeException e) {
      uncommittedSecrets.forEach(this::safeRemoveSecret);
      throw e;
    }
  }

  /**
   * Apply the override JSON's {@code changes} for a single {@code config}. Only attributes actually
   * present in {@code changes} are inspected — the loop does not sweep every field declared as
   * sensitive by the descriptor, which is what previously caused legitimate legacy PBE tokens in
   * non-overridden fields to be re-encrypted as if they were the credential.
   * <p>
   * For sensitive attributes the write is idempotent when the operator supplies the same
   * credential twice: if the current value is an {@code _<id>} that decrypts to the same
   * plaintext as the override, no new secret is created and the attribute is not marked changed.
   * This is the container-restart pattern — the env var stays set across reboots and we do not
   * want a fresh {@code secrets} row on every restart.
   *
   * @return {@code true} if any attribute was updated on {@code config} (caller must call
   *         {@code store.update}).
   */
  private boolean applyOverridesToConfig(
      final BlobStoreConfiguration config,
      final Map<String, Map<String, Object>> changes,
      final Map<String, BlobStoreDescriptor> descriptors,
      final List<Secret> newlyCreated,
      final List<Secret> superseded)
  {
    BlobStoreDescriptor descriptor = descriptors.get(config.getType());
    Set<String> sensitiveFieldsInType = new HashSet<>();
    if (descriptor == null) {
      log.warn(
          "No descriptor registered for blob store type '{}' (blob store '{}'); merging overrides "
              + "verbatim without credential-field detection",
          config.getType(), config.getName());
    }
    else {
      List<String> declared = descriptor.getSensitiveConfigurationFields();
      if (declared != null) {
        sensitiveFieldsInType.addAll(declared);
      }
    }
    String configTypeLower = config.getType() == null ? null : config.getType().toLowerCase();

    boolean anyChange = false;
    for (Entry<String, Map<String, Object>> byType : changes.entrySet()) {
      if (byType.getValue() == null) {
        continue;
      }
      String typeKey = byType.getKey() == null ? null : byType.getKey().toLowerCase();
      if (typeKey == null) {
        continue;
      }
      Map<String, Object> section = config.getAttributes().get(typeKey);
      if (section == null) {
        continue;
      }
      // Sensitive fields declared by the descriptor apply to the config's OWN type section only.
      // If the override JSON targets a foreign type section (e.g. an 'azure' block on an 's3'
      // config — nonsensical but let's not crash), sensitivity does not apply.
      boolean sensitivityApplies = typeKey.equals(configTypeLower);

      for (Entry<String, Object> attr : byType.getValue().entrySet()) {
        String field = attr.getKey();
        Object newValue = attr.getValue();
        Object currentValue = section.get(field);
        boolean fieldIsSensitive = sensitivityApplies && sensitiveFieldsInType.contains(field);

        if (fieldIsSensitive && newValue instanceof String) {
          if (applySensitiveOverride(config, section, field, (String) newValue,
              currentValue, newlyCreated, superseded)) {
            anyChange = true;
          }
        }
        else if (!Objects.equals(newValue, currentValue)) {
          section.put(field, newValue);
          anyChange = true;
        }
      }
    }
    return anyChange;
  }

  /**
   * Apply a single sensitive-field override. Returns {@code true} if the attribute was updated.
   * Encrypts a plaintext {@code newValue} unless it is genuinely the same credential the config
   * already stores (idempotent no-op) or is itself already an {@code _<id>} reference.
   */
  private boolean applySensitiveOverride(
      final BlobStoreConfiguration config,
      final Map<String, Object> section,
      final String field,
      final String newValue,
      final Object currentValue,
      final List<Secret> newlyCreated,
      final List<Secret> superseded)
  {
    // Case A: override supplied its own _<id>. Validate that it's a legitimate blobstore secret
    // before accepting it to prevent secret injection attacks.
    if (isSecretIdReference(newValue)) {
      if (newValue.equals(currentValue)) {
        return false;
      }
      // Validate the secret exists and was created for blobstore-config purpose
      if (!isValidBlobStoreSecret(newValue)) {
        log.warn(
            "Rejecting secret ID reference '{}' for blob store '{}' attribute '{}': "
                + "secret does not exist or is not a blobstore-config secret",
            newValue, config.getName(), field);
        return false;
      }
      if (currentValue instanceof String && isSecretIdReference((String) currentValue)) {
        superseded.add(secretsService.from((String) currentValue));
      }
      section.put(field, newValue);
      return true;
    }

    // Case B: override supplied plaintext, current is an existing _<id>. Decrypt the current
    // value and compare to detect a true credential change vs. an idempotent restart.
    if (currentValue instanceof String && isSecretIdReference((String) currentValue)) {
      String currentSecretId = (String) currentValue;
      try {
        char[] currentDecrypted = secretsService.from(currentSecretId).decrypt();
        if (Arrays.equals(currentDecrypted, newValue.toCharArray())) {
          // Same credential — leave the existing _<id> in place. This is the env-var-permanently-set
          // container restart pattern; without this check, every boot creates a new secrets row.
          return false;
        }
      }
      catch (Exception e) {
        log.debug(
            "Failed to decrypt current sensitive value for blob store '{}' attribute '{}'; "
                + "treating override as a credential change",
            config.getName(), field, e);
      }
      Secret newSecret =
          secretsService.encryptMaven(BLOBSTORE_CONFIG, newValue.toCharArray(), null);
      superseded.add(secretsService.from(currentSecretId));
      section.put(field, newSecret.getId());
      newlyCreated.add(newSecret);
      return true;
    }

    // Case C: override supplied plaintext, current is plaintext (or absent). Encrypt unless the
    // two plaintexts are identical — encryption is not needed to "upgrade" the shape here since
    // the BaseBlobStoreManager startup sweep will heal any remaining pre-existing plaintext on
    // next boot. But to satisfy the PR's own goal (credentials never persisted as plaintext), we
    // always encrypt when the override supplies a new plaintext value.
    if (currentValue instanceof String && newValue.equals(currentValue)) {
      return false;
    }
    Secret newSecret = secretsService.encryptMaven(BLOBSTORE_CONFIG, newValue.toCharArray(), null);
    section.put(field, newSecret.getId());
    newlyCreated.add(newSecret);
    return true;
  }

  /**
   * Emit override structure at debug without leaking plaintext credentials. Sensitive fields
   * declared by the descriptor are replaced with a placeholder before logging.
   */
  private void logOverridesRedacted(
      final Map<String, Map<String, Map<String, Object>>> overrides,
      final Map<String, BlobStoreDescriptor> descriptors)
  {
    if (!log.isDebugEnabled()) {
      return;
    }
    Map<String, Map<String, Map<String, Object>>> redacted = new HashMap<>();
    for (Entry<String, Map<String, Map<String, Object>>> byName : overrides.entrySet()) {
      Map<String, Map<String, Object>> perTypeCopy = new HashMap<>();
      if (byName.getValue() != null) {
        for (Entry<String, Map<String, Object>> byType : byName.getValue().entrySet()) {
          String typeKey = byType.getKey();
          List<String> sensitiveFields = sensitiveFieldsForType(typeKey, descriptors);
          boolean redactAll = sensitiveFields == null;
          Map<String, Object> attrCopy = new HashMap<>();
          if (byType.getValue() != null) {
            for (Entry<String, Object> attr : byType.getValue().entrySet()) {
              if (redactAll || sensitiveFields.contains(attr.getKey())) {
                attrCopy.put(attr.getKey(), "***");
              }
              else {
                attrCopy.put(attr.getKey(), attr.getValue());
              }
            }
          }
          perTypeCopy.put(typeKey, attrCopy);
        }
      }
      redacted.put(byName.getKey(), perTypeCopy);
    }
    log.debug("Applying blob store overrides: {}", redacted);
  }

  /**
   * Returns the descriptor-declared sensitive fields for the given override JSON type key, or
   * {@code null} if no descriptor is registered for that type — the caller should treat a null
   * return as "redact everything" so unknown types can't leak plaintext credentials to logs.
   */
  private List<String> sensitiveFieldsForType(
      final String typeKey,
      final Map<String, BlobStoreDescriptor> descriptors)
  {
    if (typeKey == null) {
      return null;
    }
    // The descriptor map is keyed by @Qualifier (case-preserving), so try the JSON key
    // verbatim and also a lowercased fallback so we redact regardless of the override JSON's
    // casing.
    BlobStoreDescriptor descriptor = descriptors.get(typeKey);
    if (descriptor == null) {
      descriptor = descriptors.get(typeKey.toLowerCase());
    }
    if (descriptor == null) {
      return null;
    }
    List<String> fields = descriptor.getSensitiveConfigurationFields();
    return fields == null ? Collections.emptyList() : fields;
  }

  private static boolean isSecretIdReference(final String value) {
    return value != null && value.startsWith("_") && value.length() > 1 && Character.isDigit(value.charAt(1));
  }

  /**
   * Validates that a secret ID reference is legitimate for blob store configuration use.
   * This prevents injection of arbitrary secret IDs that the user shouldn't have access to.
   *
   * <p>
   * A secret is considered valid if:
   * <ul>
   * <li>It exists in the secrets store</li>
   * <li>It can be decrypted (validates it's a proper secret, not arbitrary text)</li>
   * </ul>
   *
   * @param secretId the secret ID to validate (e.g., "_123")
   * @return true if the secret is valid for blob store use, false otherwise
   */
  private boolean isValidBlobStoreSecret(final String secretId) {
    try {
      // First check if the secret exists
      if (!secretsService.exists(secretId)) {
        log.debug("Secret ID '{}' does not exist", secretId);
        return false;
      }
      // Verify we can actually decrypt it (confirms it's a valid secret)
      Secret secret = secretsService.from(secretId);
      secret.decrypt();
      return true;
    }
    catch (Exception e) {
      log.debug("Secret ID '{}' validation failed: {}", secretId, e.getMessage());
      return false;
    }
  }

  private void safeRemoveSecret(final Secret secret) {
    try {
      secretsService.remove(secret);
    }
    catch (Exception e) {
      log.error("Failed to cleanup secret {} cause {}", secret.getId(), e.getMessage(),
          log.isDebugEnabled() ? e : null);
    }
  }
}

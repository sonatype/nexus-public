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
package org.sonatype.nexus.crypto.secrets.internal;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Optional;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.crypto.secrets.internal.EncryptionKeyList.SecretEncryptionKey;

import com.fasterxml.jackson.databind.ObjectMapper;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.app.FeatureFlags.SECRETS_FILE;

/**
 * Default implementation for  {@link EncryptionKeySource} This implementation uses a JSON file to read the secret keys
 * expected to be used in nexus
 */
@Named
@Singleton
public class EncryptionKeySourceImpl
    extends ComponentSupport
    implements EncryptionKeySource
{
  private final ObjectMapper objectMapper;

  private final String secretsFilePath;

  private EncryptionKeyList configuredKeys;

  private Optional<SecretEncryptionKey> activeKey;

  private boolean pristine = true;

  @Inject
  public EncryptionKeySourceImpl(
      @Nullable @Named("${" + SECRETS_FILE + "}") final String secretsFilePath,
      final ObjectMapper objectMapper)
  {
    this.objectMapper = checkNotNull(objectMapper);
    this.secretsFilePath = secretsFilePath;
    this.activeKey = Optional.empty();

    checkFilePath();
  }

  private void checkFilePath() {
    if (secretsFilePath != null && !new File(secretsFilePath).exists()) {
      log.warn("The configured secrets file does not exist");
    }
  }

  /**
   * Reads encryption keys from the given file and loads it into local variables
   *
   * @throws UncheckedIOException if nexus is not able to read the file path
   */
  private void readFile() {
    if (secretsFilePath == null) {
      log.debug("no path configured for custom secrets");
      return;
    }

    File secretsFile = new File(secretsFilePath);

    if (!secretsFile.exists()) {
      log.debug("configured secrets file path is missing");
      return;
    }

    log.debug("reading secrets file from path : {}", secretsFilePath);

    try {
      configuredKeys = objectMapper.readValue(new File(secretsFilePath), EncryptionKeyList.class);
      String activeKeyId = configuredKeys.getActive();

      if (pristine) {
        activeKey = configuredKeys
            .getKeys()
            .stream()
            .filter(s -> s.getId().equals(activeKeyId))
            .findFirst();

        if (!activeKey.isPresent() && activeKeyId != null) {
          log.error("unable to find encryption key with id '{}'", activeKeyId);
        }
      }

      pristine = false;
    }
    catch (IOException e) {
      throw new UncheckedIOException(
          String.format("Unable to read secret encryption keys from '%s'. Cause: %s", secretsFilePath,
              e.getMessage()), e);
    }
  }

  /**
   * Retrieves an encryption key in the in-memory stored list
   *
   * @param keyId the encryption key id to be retrieved
   * @return an {@link Optional} with the {@link SecretEncryptionKey} instance if found
   */
  private Optional<SecretEncryptionKey> findKey(final String keyId) {
    return Optional.ofNullable(configuredKeys)
        .flatMap(existing -> existing.getKeys()
            .stream()
            .filter(kv -> keyId.equals(kv.getId()))
            .findFirst());
  }

  @Override
  public Optional<SecretEncryptionKey> getActiveKey() {
    if (pristine) {
      readFile();
    }

    return activeKey;
  }

  @Override
  public Optional<SecretEncryptionKey> getKey(final String keyId) {
    checkNotNull(keyId);
    if (!findKey(keyId).isPresent()) {
      readFile();
    }

    return findKey(keyId);
  }

  @Override
  public void setActiveKey(final String keyId) {
    checkNotNull(keyId);
    Optional<SecretEncryptionKey> inMemory = findKey(keyId);

    if (inMemory.isPresent()) {
      activeKey = inMemory;
    }
    else {
      readFile();
      activeKey = findKey(keyId);
    }
  }
}

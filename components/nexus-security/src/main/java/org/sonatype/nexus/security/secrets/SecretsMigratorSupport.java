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
package org.sonatype.nexus.security.secrets;

import java.util.Arrays;
import java.util.List;

import javax.annotation.Nullable;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.crypto.secrets.Secret;
import org.sonatype.nexus.crypto.secrets.SecretsService;
import org.sonatype.nexus.security.UserIdHelper;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Support for {@link SecretsMigrator} implementations.
 */
public abstract class SecretsMigratorSupport
    extends ComponentSupport
    implements SecretsMigrator
{
  protected final SecretsService secretsService;

  protected SecretsMigratorSupport(final SecretsService secretsService) {
    this.secretsService = checkNotNull(secretsService);
  }

  /**
   * Persists a secret and validates that decrypting the new secret matches the original
   */
  protected Secret createSecret(final String purpose, final Secret oldSecret, @Nullable final String context) {
    char[] pw = oldSecret.decrypt();

    Secret newSecret = secretsService.encrypt(purpose, pw, UserIdHelper.get());

    if (Arrays.equals(pw, newSecret.decrypt())) {
      return newSecret;
    }

    if (context != null) {
      throw new SecretMigrationException("Re-encrypted secret does not match for " + context);
    }

    throw new SecretMigrationException("Re-encrypted secret does not match");
  }

  /**
   * Removes persisted secrets, failures are logged not thrown
   */
  protected void quietlyRemove(final List<Secret> secrets) {
    for (Secret secret : secrets) {
      try {
        secretsService.remove(secret);
      }
      catch (Exception e) {
        if (isLegacyEncryptedString(secret)) {
          // This should not happen without bugs elsewhere but we want to ensure we don't accidentally log secrets
          log.error("Failed to cleanup cause {}", e.getMessage(), log.isDebugEnabled() ? e : null);
        }
        else {
          log.error("Failed to cleanup secret {} cause {}", secret.getId(), e.getMessage(),
              log.isDebugEnabled() ? e : null);
        }
      }
    }
  }
}


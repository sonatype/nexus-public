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
package org.sonatype.nexus.repository.security.internal.secrets.migration;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.crypto.secrets.Secret;
import org.sonatype.nexus.crypto.secrets.SecretsService;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.types.ProxyType;
import org.sonatype.nexus.scheduling.CancelableHelper;
import org.sonatype.nexus.security.secrets.SecretMigrationException;
import org.sonatype.nexus.security.secrets.SecretsMigratorSupport;

import com.google.common.annotations.VisibleForTesting;

import static com.google.common.base.Preconditions.checkNotNull;

@Named
public class RepositoriesSecretsMigrator
    extends SecretsMigratorSupport
{
  @VisibleForTesting
  static final String HTTP_CLIENT_KEY = "httpclient";

  @VisibleForTesting
  static final String AUTHENTICATION_KEY = "authentication";

  @VisibleForTesting
  static final String BEARER_TOKEN_KEY = "bearerToken";

  @VisibleForTesting
  static final String PASSWORD_KEY = "password";

  private final RepositoryManager repositoryManager;

  @Inject
  public RepositoriesSecretsMigrator(final SecretsService secretsService, final RepositoryManager repositoryManager)
  {
    super(secretsService);
    this.repositoryManager = checkNotNull(repositoryManager);
  }

  @Override
  public void migrate() {
    for (Repository repository : repositoryManager.browse()) {
      CancelableHelper.checkCancellation();

      if (repository.getType() instanceof ProxyType) {
        migrateProxy(repository);
      }
    }
  }

  private void migrateProxy(final Repository repository) {
    Configuration configuration = repository.getConfiguration().copy();
    boolean needUpdate = false;

    Map<String, Object> authConfig = Optional.ofNullable(configuration.getAttributes())
        .map(global -> global.get(HTTP_CLIENT_KEY))
        .map(http -> (Map<String, Object>) http.get(AUTHENTICATION_KEY))
        .orElse(Collections.emptyMap());

    // These are really either-or but for impl it doesn't matter
    Secret bearerToken = Optional.ofNullable((String) authConfig.get(BEARER_TOKEN_KEY))
        .map(secretsService::from)
        .orElse(null);
    if (bearerToken != null && isLegacyEncryptedString(bearerToken)) {
      needUpdate = true;
      authConfig.put(BEARER_TOKEN_KEY, new String(bearerToken.decrypt()));
    }

    Secret passwordKey = Optional.ofNullable((String) authConfig.get(PASSWORD_KEY))
        .map(secretsService::from)
        .orElse(null);
    if (passwordKey != null && isLegacyEncryptedString(passwordKey)) {
      needUpdate = true;
      authConfig.put(PASSWORD_KEY, new String(passwordKey.decrypt()));
    }

    if (needUpdate) {
      save(configuration);
    }
  }

  /*
   * Updates a repository configuration, if a failure occurs then secrets will be removed
   */
  private void save(final Configuration configuration) {
    try {
      // repository manager encrypts and handles removal in case of failure
      repositoryManager.update(configuration);
    }
    catch (Exception e) {
      throw new SecretMigrationException("Failed to migrate repository: " + configuration.getRepositoryName(), e);
    }
  }
}

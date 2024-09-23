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
package org.sonatype.nexus.internal.security.apikey.store;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.crypto.secrets.Secret;
import org.sonatype.nexus.crypto.secrets.SecretsService;
import org.sonatype.nexus.datastore.ConfigStoreSupport;
import org.sonatype.nexus.datastore.api.DataSessionSupplier;
import org.sonatype.nexus.datastore.api.DuplicateKeyException;
import org.sonatype.nexus.internal.security.apikey.ApiKeyInternal;
import org.sonatype.nexus.transaction.Transactional;

import com.google.common.annotations.VisibleForTesting;
import org.apache.shiro.subject.PrincipalCollection;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An {@link ApiKeyStore} implementation which makes use of {@link SecretsService}
 */
@Named("v2")
@Singleton
public class ApiKeyStoreV2Impl
    extends ConfigStoreSupport<ApiKeyV2DAO>
    implements ApiKeyStore
{
  private final SecretsService secretsService;

  @Inject
  public ApiKeyStoreV2Impl(
      final DataSessionSupplier sessionSupplier,
      final SecretsService secretsService)
  {
    super(sessionSupplier);
    this.secretsService = checkNotNull(secretsService);
  }

  @Transactional
  @Override
  public Collection<ApiKeyInternal> browse(final String domain) {
    return dao().browse(domain);
  }

  @Transactional
  @Override
  public Collection<ApiKeyInternal> browseByCreatedDate(final String domain, final OffsetDateTime date) {
    return dao().browseCreatedBefore(domain, date);
  }

  @Transactional
  @Override
  public Collection<ApiKeyInternal> browsePaginated(final String domain, final int page, final int pageSize) {
    return dao().browsePaginated(domain, page, pageSize);
  }

  @Transactional
  @Override
  public int count(final String domain) {
    return dao().count(domain);
  }

  @Override
  public int deleteApiKey(final String domain, final PrincipalCollection principals) {
    return findApiKey(domain, principals)
        .map(this::deleteApiKey)
        .orElse(0);
  }

  @Override
  public int deleteApiKeys(final OffsetDateTime expiration) {
    return findCreatedBefore(expiration).stream()
        .map(ApiKeyV2Data.class::cast)
        .mapToInt(this::deleteApiKey)
        .sum();
  }

  @Override
  public int deleteApiKeys(final PrincipalCollection principals) {
    return findApiKeysForUser(principals)
        .mapToInt(this::deleteApiKey)
        .sum();
  }

  @Override
  public int deleteApiKeys(final String domain) {
    return browse(domain).stream()
        .map(ApiKeyV2Data.class::cast)
        .mapToInt(this::deleteApiKey)
        .sum();
  }

  @Transactional
  @Override
  public Optional<ApiKeyInternal> getApiKey(final String domain, final PrincipalCollection principals) {
    checkNotNull(domain);
    checkNotNull(principals);

    return findApiKey(domain, principals)
        .map(ApiKeyInternal.class::cast);
  }

  @Transactional
  @Override
  public Optional<ApiKeyInternal> getApiKeyByToken(final String domain, final char[] apiKey) {
    return dao().findPrincipals(domain, accessKey(apiKey))
        .filter(key -> Arrays.equals(((ApiKeyV2Data) key).getSecret().decrypt(), secret(apiKey)));
  }

  @Override
  public void persistApiKey(
      final String domain,
      final PrincipalCollection principals,
      final char[] apiKey,
      final OffsetDateTime created)
  {
    ApiKeyV2Data token = (ApiKeyV2Data) newApiKey(domain, principals, apiKey, created);

    try {
      persistApiKey(token, 1);
    }
    catch (RuntimeException e) {
      log.debug("Failed to save key, cleaning up secret", e);

      secretsService.remove(token.getSecret());

      throw e;
    }
  }

  private ApiKeyInternal newApiKey(
      final String domain,
      final PrincipalCollection principals,
      final char[] apiKey,
      final OffsetDateTime created)
  {
    Secret encrypted = secretsService.encrypt(domain, secret(apiKey), principals.getPrimaryPrincipal().toString());

    return new ApiKeyV2Data(domain, principals, accessKey(apiKey), encrypted, created);
  }

  private void persistApiKey(final ApiKeyV2Data token, final int retryCount) {
    try {
      doSave(token);
    }
    catch (DuplicateKeyException e) {
      if (log.isDebugEnabled()) {
        log.debug("Failed to persist token for {} in domain {} remaining retries {}", token.getPrimaryPrincipal(),
            token.getDomain(), retryCount);
      }

      if (retryCount == 0 || token.getCreated() != null && getApiKey(token.getDomain(), token.getPrincipals())
          .map(ApiKeyInternal::getCreated)
          .filter(token.getCreated()::isBefore)
          .isPresent()) {
        throw e;
      }

      if (log.isDebugEnabled()) {
        log.debug("Replacing existing key for {} in {}", token.getPrimaryPrincipal(), token.getDomain());
      }
      deleteApiKey(token.getDomain(), token.getPrincipals());
      persistApiKey(token, retryCount - 1);
    }
  }

  @Transactional
  @Override
  public void updateApiKey(final ApiKeyInternal from, final PrincipalCollection newPrincipal) {
    ApiKeyV2Data fromToken = (ApiKeyV2Data) from;

    dao().updatePrincipal(new ApiKeyV2Data(from.getDomain(), newPrincipal, fromToken.getAccessKey(),
        fromToken.getSecret(), fromToken.getCreated()));
  }

  @Transactional
  @Override
  public Iterable<PrincipalCollection> browsePrincipals() {
    return dao().browsePrincipals();
  }

  protected int deleteApiKey(final ApiKeyV2Data apiTokenData) {
    int count = doDelete(apiTokenData);

    secretsService.remove(apiTokenData.getSecret());

    return count;
  }

  /*
   * This method should only be called if the associated secret has been deleted.
   */
  @Transactional
  protected int doDelete(final ApiKeyV2Data apiTokenData) {
    return dao().deleteApiKey(apiTokenData);
  }

  @Transactional
  protected Collection<ApiKeyV2Data> findCreatedBefore(final OffsetDateTime date) {
    return dao().browseCreatedBefore(date);
  }

  @Transactional
  protected Optional<ApiKeyV2Data> findApiKey(final String domain, final PrincipalCollection principals) {
    checkNotNull(domain);
    checkNotNull(principals);

    return dao().findApiKey(domain, principals.getPrimaryPrincipal().toString()).stream()
        .filter(principalMatcher(principals))
        .findFirst();
  }

  @Transactional
  protected Stream<ApiKeyV2Data> findApiKeysForUser(final PrincipalCollection principals) {
    checkNotNull(principals);

    return dao().findApiKeysForUser(principals.getPrimaryPrincipal().toString()).stream()
        .filter(principalMatcher(principals));
  }

  @Transactional
  protected void doSave(final ApiKeyV2Data token) {
    dao().save(token);
  }

  private static Predicate<ApiKeyV2Data> principalMatcher(final PrincipalCollection collection) {
    return apiKeyData -> apiKeyData.getPrincipals().equals(collection);
  }

  @VisibleForTesting
  static String accessKey(final char[] apiKey) {
    return new String(apiKey,  0, apiKey.length / 2);
  }

  @VisibleForTesting
  static char[] secret(final char[] apiKey) {
    return Arrays.copyOfRange(apiKey, apiKey.length / 2, apiKey.length);
  }
}

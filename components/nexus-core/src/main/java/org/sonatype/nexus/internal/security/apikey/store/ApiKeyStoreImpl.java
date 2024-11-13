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
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.event.EventAware;
import org.sonatype.nexus.datastore.ConfigStoreSupport;
import org.sonatype.nexus.datastore.api.DataSessionSupplier;
import org.sonatype.nexus.internal.security.apikey.ApiKeyInternal;
import org.sonatype.nexus.transaction.Transactional;

import org.apache.shiro.subject.PrincipalCollection;

/**
 * Legacy {@link ApiKeyStore} implementation
 *
 * @since 3.21
 */
@Deprecated
@Named("v1")
@Singleton
public class ApiKeyStoreImpl
    extends ConfigStoreSupport<ApiKeyDAO>
    implements ApiKeyStore, EventAware
{
  @Inject
  public ApiKeyStoreImpl(final DataSessionSupplier sessionSupplier) {
    super(sessionSupplier);
  }

  @Transactional
  @Override
  public void persistApiKey(
      final String domain,
      final PrincipalCollection principals,
      final char[] apiKey,
      final OffsetDateTime created)
  {
    ApiKeyData apiKeyData = new ApiKeyData();
    apiKeyData.setDomain(domain);
    apiKeyData.setPrincipals(principals);
    apiKeyData.setApiKey(apiKey);
    apiKeyData.setCreated(created);
    dao().save(apiKeyData);
  }

  @Transactional
  @Override
  public Optional<ApiKeyInternal> getApiKey(
      final String domain,
      final PrincipalCollection principals)
  {
    return findApiKey(domain, principals);
  }

  @Transactional
  @Override
  public Optional<ApiKeyInternal> getApiKeyByToken(
      final String domain,
      final char[] apiKey)
  {
    return dao().findPrincipals(domain, new ApiKeyToken(apiKey));
  }

  @Transactional
  @Override
  public int deleteApiKey(final String domain, final PrincipalCollection principals) {
    return findApiKey(domain, principals)
        .map(ApiKeyInternal::getApiKey)
        .map(ApiKeyToken::new)
        .map(token -> dao().deleteKey(domain, token))
        .orElse(0);
  }

  @Transactional
  @Override
  public int deleteApiKeys(final PrincipalCollection principals) {
    return dao().findApiKeysForPrimary(principals.getPrimaryPrincipal().toString()).stream()
        .filter(principalMatches(principals))
        .map(key -> dao().deleteKey(key.getDomain(), new ApiKeyToken(key.getApiKey())))
        .mapToInt(Integer::intValue)
        .sum();
  }

  @Override
  @Transactional
  public Iterable<PrincipalCollection> browsePrincipals() {
    return dao().browsePrincipals();
  }

  @Transactional
  @Override
  public Collection<ApiKeyInternal> browse(final String domain) {
    return dao().browse(domain);
  }

  @Transactional
  @Override
  public Collection<ApiKeyInternal> browseByCreatedDate(final String domain, final OffsetDateTime date) {
    return dao().browseByCreatedDate(domain, date);
  }

  @Transactional
  @Override
  public int count(final String domain) {
    return dao().count(domain);
  }

  @Transactional
  @Override
  public int deleteApiKeys(final String domain) {
    return dao().deleteApiKeysByDomain(domain);
  }

  @Transactional
  @Override
  public int deleteApiKeys(final OffsetDateTime expiration) {
    return dao().deleteApiKeyByExpirationDate(expiration);
  }

  @Transactional
  @Override
  public void updateApiKey(
      final ApiKeyInternal from,
      final PrincipalCollection principalCollection)
  {
    dao().update(
        new ApiKeyData(from.getDomain(), principalCollection, new ApiKeyToken(from.getApiKey()), from.getCreated()));
  }

  @Transactional
  @Override
  public Collection<ApiKeyInternal> browsePaginated(
      final String domain,
      final int page,
      final int pageSize)
  {
    return dao().browsePaginated(domain, (page - 1) * pageSize, pageSize);
  }

  @Transactional
  public Collection<ApiKeyData> browseAllSince(final OffsetDateTime last, final int pageSize) {
    return dao().browseAllSince(last, pageSize);
  }

  /*
   * Finds ApiKey records for the provided username, and ensures the realm is the same
   */
  private Optional<ApiKeyInternal> findApiKey(final String domain, final PrincipalCollection principals) {
    return dao().findApiKeys(domain, principals.getPrimaryPrincipal().toString()).stream()
        .filter(principalMatches(principals))
        .findAny();
  }

  /*
   * Creates a Predicate which ensures the principal of the tested ApiKey is equal to the provided PrincipalCollection
   */
  private Predicate<ApiKeyInternal> principalMatches(final PrincipalCollection principals) {
    String primaryPrincipal = principals.getPrimaryPrincipal().toString();
    Set<String> realms = principals.getRealmNames();
    return key -> key.getPrincipals().getRealmNames().equals(realms)
        && key.getPrincipals().getPrimaryPrincipal().equals(primaryPrincipal);
  }
}

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

import javax.annotation.Nullable;

import org.sonatype.nexus.internal.security.apikey.ApiKeyInternal;

import org.apache.shiro.subject.PrincipalCollection;

public interface ApiKeyStore
{
  /**
   * Browse tokens in the domain
   */
  Collection<ApiKeyInternal> browse(String domain);

  /**
   * Browse tokens in the domain created after the provided date
   */
  Collection<ApiKeyInternal> browseByCreatedDate(String domain, OffsetDateTime date);

  /**
   * Browse all tokens across all domains (paginated).
   *
   * @param page 0-based page number
   * @param pageSize maximum number of results per page
   */
  Collection<ApiKeyInternal> browseAll(int page, int pageSize);

  /**
   * Browse tokens in the domain (paginated)
   */
  Collection<ApiKeyInternal> browsePaginated(String domain, int page, int pageSize);

  /**
   * Count all the keys for the provided domain
   */
  int count(String domain);

  /**
   * Deletes the API-Key associated with the given principals in given domain.
   */
  int deleteApiKey(String domain, PrincipalCollection principals);

  /**
   * Remove all expired API-Keys
   */
  int deleteApiKeys(OffsetDateTime expiration);

  /**
   * Remove all expired API-Keys for the specified domain
   */
  int deleteApiKeys(String domain, OffsetDateTime expiration);

  /**
   * Deletes every API-Key associated with the given principals in every domain.
   */
  int deleteApiKeys(PrincipalCollection principals);

  /**
   * Deletes all API-Keys for the specified domain
   */
  int deleteApiKeys(String domain);

  /**
   * Gets the current API-Key assigned to the given principals in given domain.
   *
   * @return {@code null} if no key has been assigned
   */
  Optional<ApiKeyInternal> getApiKey(String domain, PrincipalCollection principals);

  /**
   * Retrieves the principals associated with the given API-Key in given domain.
   *
   * @return {@code null} if the key is invalid or stale
   */
  Optional<ApiKeyInternal> getApiKeyByToken(String domain, char[] apiKey);

  /**
   * Persists an API-Key with a predetermined value.
   *
   * @since 3.1
   */
  default void persistApiKey(final String domain, final PrincipalCollection principals, final char[] apiKey) {
    persistApiKey(domain, principals, apiKey, null);
  }

  /**
   * Persists an API-Key with a predetermined value.
   */
  void persistApiKey(String domain, PrincipalCollection principals, char[] apiKey, @Nullable OffsetDateTime created);

  /**
   * Persists an API-Key with creator tracking.
   */
  default void persistApiKey(
      String domain,
      PrincipalCollection principals,
      char[] apiKey,
      @Nullable OffsetDateTime created,
      @Nullable String creatorUserId,
      @Nullable String creatorRealm)
  {
    persistApiKey(domain, principals, apiKey, created);
  }

  /**
   * Updates an existing API-key.
   */
  void updateApiKey(ApiKeyInternal from, PrincipalCollection newPrincipal);

  Iterable<PrincipalCollection> browsePrincipals();

  /**
   * Browse tokens in the domain with filtering and pagination.
   *
   * @param domain the domain
   * @param realm optional realm filter (null for all realms)
   * @param username optional username filter (null for all users)
   * @param skip number of records to skip
   * @param limit maximum number of records to return
   * @return filtered and paginated collection
   */
  Collection<ApiKeyInternal> browseFiltered(
      String domain,
      @Nullable String realm,
      @Nullable String username,
      int skip,
      int limit);

  /**
   * Count tokens in the domain matching the filters.
   *
   * @param domain the domain
   * @param realm optional realm filter (null for all realms)
   * @param username optional username filter (null for all users)
   * @return count of matching tokens
   */
  int countFiltered(String domain, @Nullable String realm, @Nullable String username);
}

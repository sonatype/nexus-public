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
package org.sonatype.nexus.security.authc.apikey;

import java.time.OffsetDateTime;
import java.util.Collection;

import javax.annotation.Nullable;

import org.apache.shiro.subject.PrincipalCollection;

/**
 * Exposes lower level APIs of {@link ApiKeyService} which shouldn't be widely used.
 */
public interface ApiKeyLowLevelService
    extends ApiKeyService
{
  /**
   * Browse all tokens across all domains (paginated)
   */
  Collection<ApiKey> browseAll(int page, int pageSize);

  /**
   * Browse tokens in the domain
   */
  Collection<ApiKey> browse(String domain);

  /**
   * Browse tokens in the domain created after the provided date
   */
  Collection<ApiKey> browseByCreatedDate(String domain, OffsetDateTime date);

  /**
   * Browse tokens in the domain (paginated)
   */
  Collection<ApiKey> browsePaginated(String domain, int page, int pageSize);

  /**
   * Persists an API-Key with a predetermined value.
   */
  default void persistApiKey(final String domain, final PrincipalCollection principals, final char[] apiKey) {
    persistApiKey(domain, principals, apiKey, null);
  }

  /**
   * Persists an API-Key with a predetermined value.
   */
  void persistApiKey(String domain, PrincipalCollection principals, char[] apiKey, @Nullable OffsetDateTime created);

  /**
   * Persists an API-Key with a predetermined value and creator tracking.
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
  void updateApiKeyRealm(ApiKey from, PrincipalCollection newPrincipal);

  /**
   * Browse tokens in the domain with filtering and pagination support.
   *
   * @param domain the domain
   * @param realm optional realm filter (null for all realms)
   * @param username optional username filter (null for all users)
   * @param skip number of records to skip
   * @param limit maximum number of records to return
   * @return filtered and paginated collection
   */
  default Collection<ApiKey> browseFiltered(
      String domain,
      @Nullable String realm,
      @Nullable String username,
      int skip,
      int limit)
  {
    // Default implementation: fall back to browse with in-memory filtering
    return browse(domain).stream()
        .filter(key -> username == null || username.equals(key.getPrincipals().getPrimaryPrincipal()))
        .skip(skip)
        .limit(limit)
        .toList();
  }

  /**
   * Count tokens in the domain matching the filters.
   *
   * @param domain the domain
   * @param realm optional realm filter (null for all realms)
   * @param username optional username filter (null for all users)
   * @return count of matching tokens
   */
  default int countFiltered(String domain, @Nullable String realm, @Nullable String username) {
    // Default implementation: fall back to browse with in-memory filtering
    return (int) browse(domain).stream()
        .filter(key -> username == null || username.equals(key.getPrincipals().getPrimaryPrincipal()))
        .count();
  }
}

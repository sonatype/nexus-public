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

import org.sonatype.nexus.datastore.api.DataAccess;
import org.sonatype.nexus.internal.security.apikey.ApiKeyInternal;

import org.apache.ibatis.annotations.Param;
import org.apache.shiro.subject.PrincipalCollection;

/**
 * {@link ApiKeyData} access.
 *
 * @since 3.21
 * @deprecated legacy implementation which does not use secrets
 */
@Deprecated
public interface ApiKeyDAO
    extends DataAccess
{
  Iterable<PrincipalCollection> browsePrincipals();

  /**
   * Find {@link ApiKeyInternal} records in the domain with the specified primary principal.
   *
   * NOTE that callers must verify that this matches the PrincipalCollection of the keys.
   *
   * @param domain the domain for the token (e.g. NuGetApiKey)
   * @param primaryPrincipal the primary principal to locatte
   */
  Collection<ApiKeyInternal> findApiKeys(@Param("domain") String domain, @Param("primaryPrincipal") String primaryPrincipal);

  /**
   * Find {@link ApiKeyInternal} records across all domains with the specified primary principal.
   *
   * NOTE that callers must verify that this matches the PrincipalCollection of the keys.
   *
   * @param primaryPrincipal the primary principal to locatte
   */
  Collection<ApiKeyInternal> findApiKeysForPrimary(@Param("primaryPrincipal") String primaryPrincipal);

  /**
   *
   * @param domain the domain
   * @param token token
   */
  Optional<ApiKeyInternal> findPrincipals(@Param("domain") String domain, @Param("token") ApiKeyToken token);

  void save(ApiKeyData apiKeyData);

  /**
   * Delete an {@link ApiKeyInternal} in the specified domain.
   *
   * @param domain the domain for the token (e.g. NuGetApiKey)
   * @param token the token
   */
  int deleteKey(@Param("domain") String domain, @Param("token") ApiKeyToken token);

  /**
   * Browse all API Keys in the specified domain
   *
   * @param domain the domain, e.g. npm keys, nuget keys
   */
  Collection<ApiKeyInternal> browse(@Param("domain") String domain);

  /**
   * Browse all API Keys in the specified domain
   *
   * @param domain the domain, e.g. npm keys, nuget keys
   * @param created the date created
   */
  Collection<ApiKeyInternal> browseByCreatedDate(@Param("domain") String domain, @Param("created") OffsetDateTime created);

  /**
   * Count the number of API Keys in the specified domain
   *
   * @param domain the domain, e.g. npm keys, nuget keys
   */
  int count(@Param("domain") String domain);

  /**
   * Remove all API Keys in the specified domain
   *
   * @param domain the domain, e.g. npm keys, nuget keys
   */
  int deleteApiKeysByDomain(@Param("domain") String domain);

  /**
   * Remove all expired API Keys
   *
   * @param expiration the date of expiration
   */
  int deleteApiKeyByExpirationDate(@Param("expiration") OffsetDateTime expiration);

  /**
   * Updates an existing {@link ApiKeyInternal}
   */
  void update(ApiKeyData toUpdate);

  /**
   * Browse all API Keys in the specified domain (paginated)
   *
   * @param domain the domain, e.g. npm keys, nuget keys
   * @param skip   the amount of records to skip/offset
   * @param limit  the amount of records to limit the query to
   */
  Collection<ApiKeyInternal> browsePaginated(
      @Param("domain") String domain,
      @Param("skip") int skip,
      @Param("limit") int limit);

  /**
   * @param created a date to use as the starting point for the pagination
   * @param limit  the number of records
   *
   * @deprecated exists only for migration
   */
  @Deprecated
  Collection<ApiKeyData> browseAllSince(@Param("created") OffsetDateTime created, @Param("limit") int limit);
}

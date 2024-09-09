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
import org.sonatype.nexus.datastore.api.DuplicateKeyException;
import org.sonatype.nexus.internal.security.apikey.ApiKeyInternal;

import org.apache.ibatis.annotations.Param;
import org.apache.shiro.subject.PrincipalCollection;

/**
 * {@link ApiKeyV2Data} access.
 */
public interface ApiKeyV2DAO
    extends DataAccess
{
  /**
   * Browse all API Keys in the specified domain
   *
   * @param domain the domain, e.g. npm keys, nuget keys
   */
  Collection<ApiKeyInternal> browse(@Param("domain") String domain);

  /**
   * Browse all API Keys across all domains
   *
   * @param created the date created
   */
  Collection<ApiKeyV2Data> browseCreatedBefore(@Param("created") OffsetDateTime created);

  /**
   * Browse all API Keys in the specified domain
   *
   * @param domain the domain, e.g. npm keys, nuget keys
   * @param created the date created
   */
  Collection<ApiKeyInternal> browseCreatedBefore(@Param("domain") String domain, @Param("created") OffsetDateTime created);

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

  Iterable<PrincipalCollection> browsePrincipals();

  /**
   * Count the number of API Keys in the specified domain
   *
   * @param domain the domain, e.g. npm keys, nuget keys
   */
  int count(@Param("domain") String domain);

  /**
   * Remove the api key for the specified user in the domain. The associated secret should also be removed.
   *
   * @param apiTokenData the token to remove
   */
  int deleteApiKey(ApiKeyV2Data apiTokenData);

  /**
   * Find {@link ApiKeyInternal} record in the domain for the specified realm & user
   *
   * @param domain the domain for the token (e.g. NuGetApiKey)
   * @param realm the realm the user belongs to
   * @param user  the user name to locate
   */
  Optional<ApiKeyV2Data> findApiKey(
      @Param("domain") String domain,
      @Param("realm") String realm,
      @Param("username") String user);

  /**
   * Find {@link ApiKeyInternal} records across all domains with the specified realm and user.
   *
   * @param realm the realm the user belongs to
   * @param user  the user name to locate
   */
  Collection<ApiKeyV2Data> findApiKeysForUser(@Param("realm") String realm, @Param("username") String user);

  /**
   * Find an api key with the matching access key, callers will need to validate the secret matches the expected
   *
   * @param domain the domain
   * @param accessKey access key
   */
  Optional<ApiKeyInternal> findPrincipals(@Param("domain") String domain, @Param("accessKey") String accessKey);

  /**
   * Save an API token
   *
   * @param token the token
   * @throws DuplicateKeyException
   */
  void save(ApiKeyV2Data token);

  /**
   * Changes the realm associated with a specific token
   * @param domain
   * @param username
   * @param fromRealm
   * @param toRealm
   */
  void updateRealm(
      @Param("domain") String domain,
      @Param("username") String username,
      @Param("accessKey") String accessKey,
      @Param("fromRealm") String fromRealm,
      @Param("toRealm") String toRealm);
}

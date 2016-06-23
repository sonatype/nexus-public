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
package org.sonatype.nexus.client.core.subsystem.repository;

import org.sonatype.nexus.client.core.subsystem.repository.maven.MavenProxyRepository;

/**
 * A Nexus proxy {@link Repository}.
 *
 * @since 2.3
 */
public interface ProxyRepository<T extends ProxyRepository>
    extends BaseRepository<T, ProxyRepositoryStatus>
{

  /**
   * @return proxied URI
   */
  String proxyUri();

  /**
   * Configures repository policy (RELEASES/SNAPSHOTS/MIXED).
   *
   * @param policy repository policy
   * @return itself, for fluent api usage
   */
  T withRepoPolicy(final String policy);

  /**
   * Sets the URI of proxied repository.
   *
   * @param remoteUri of proxied repository
   * @return itself, for fluent api usage
   */
  T asProxyOf(String remoteUri);

  /**
   * Auto block the repository if proxied repository is unresponsive.
   *
   * @return itself, for fluent api usage
   */
  T autoBlock();

  /**
   * Do not auto block the repository if proxied repository is unresponsive.
   *
   * @return itself, for fluent api usage
   */
  T doNotAutoBlock();

  /**
   * @return {@code true} if auto-blocking is enabled, {@code false} otherwise.
   * @since 2.5
   */
  boolean isAutoBlocking();

  /**
   * Directly blocks the repository (no save required).
   *
   * @return itself, for fluent api usage
   */
  T block();

  /**
   * Directly unblocks the repository (no save required).
   *
   * @return itself, for fluent api usage
   */
  T unblock();

  /**
   * Sets the number of minutes that not found items will be kept in NFC.
   *
   * @param minutes not found cache TTL in minutes
   * @return itself, for fluent api usage
   */
  T withNotFoundCacheTTL(int minutes);

  /**
   * Configures number of minutes items will be cached.
   *
   * @param minutes to be cached
   * @return itself, for fluent api usage
   */
  T withItemMaxAge(int minutes);

  /**
   * Configures number of minutes artifacts will be cached.
   *
   * @param minutes to be cached
   * @return itself, for fluent api usage
   */
  T withArtifactMaxAge(int minutes);

  /**
   * Configures number of minutes artifact metadata will be cached.
   *
   * @param minutes to be cached
   * @return itself, for fluent api usage
   */
  T withMetadataMaxAge(int minutes);

  /**
   * @return the repository's max item age.
   * @since 2.5
   */
  int itemMaxAge();

  /**
   * @return the repository's max artifact age.
   * @since 2.5
   */
  int artifactMaxAge();

  /**
   * @return the repository's max metadata age.
   * @since 2.5
   */
  int metadataMaxAge();

  /**
   * Configure username authentication for remote proxy
   *
   * @param username username to access the remote url
   * @param password password to access the remote url
   * @return itself, for fluent api usage
   */
  T withUsernameAuthentication(final String username, final String password);

  /**
   * Configure NTLM authentication for remote proxy
   *
   * @param username username to access the remote url
   * @param password password to access the remote url
   * @param host the ntlm host
   * @param domain the ntlm domain
   * @return itself, for fluent api usage
   */
  T withNtlmAuthentication(final String username, final String password, final String host, final String domain);

  /**
   * Configure the remote proxy connection settings
   *
   * @param timeout connection timeout (in seconds)
   * @param retryCount number of times to retry requests
   * @param query custom query string to be added to outgoing requests
   * @param userAgent string to customize the user agent in requests
   * @return itself, for fluent api usage
   */
  T withRemoteConnectionSettings(int timeout, int retryCount, String query, String userAgent);
}

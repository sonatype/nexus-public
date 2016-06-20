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
package org.sonatype.nexus.client.internal.rest.jersey.subsystem.repository;

import org.sonatype.nexus.client.core.subsystem.repository.ProxyRepository;
import org.sonatype.nexus.client.core.subsystem.repository.ProxyRepositoryStatus;
import org.sonatype.nexus.client.rest.jersey.JerseyNexusClient;
import org.sonatype.nexus.rest.model.AuthenticationSettings;
import org.sonatype.nexus.rest.model.RemoteConnectionSettings;
import org.sonatype.nexus.rest.model.RepositoryProxyResource;
import org.sonatype.nexus.rest.model.RepositoryResourceRemoteStorage;
import org.sonatype.nexus.rest.model.RepositoryStatusResource;

/**
 * Jersey based {@link ProxyRepository} implementation.
 *
 * @since 2.3
 */
public class JerseyProxyRepository<T extends ProxyRepository>
    extends JerseyRepository<T, RepositoryProxyResource, ProxyRepositoryStatus>
    implements ProxyRepository<T>
{

  static final String REPO_TYPE = "proxy";

  static final String PROVIDER_ROLE = "org.sonatype.nexus.proxy.repository.Repository";

  public JerseyProxyRepository(final JerseyNexusClient nexusClient, final String id) {
    super(nexusClient, id);
  }

  public JerseyProxyRepository(final JerseyNexusClient nexusClient, final RepositoryProxyResource settings) {
    super(nexusClient, settings);
  }

  @Override
  protected RepositoryProxyResource createSettings() {
    final RepositoryProxyResource settings = new RepositoryProxyResource();

    settings.setRepoType(REPO_TYPE);
    settings.setProviderRole(PROVIDER_ROLE);
    settings.setExposed(true);
    settings.setWritePolicy("READ_ONLY");
    settings.setBrowseable(true);
    settings.setIndexable(false);
    settings.setNotFoundCacheTTL(1440);
    settings.setRepoPolicy("MIXED");
    settings.setChecksumPolicy("WARN");
    settings.setDownloadRemoteIndexes(true);
    settings.setFileTypeValidation(true);
    settings.setArtifactMaxAge(-1);
    settings.setMetadataMaxAge(1440);
    settings.setItemMaxAge(1440);
    settings.setAutoBlockActive(true);

    return settings;
  }

  @SuppressWarnings("unchecked")
  private T me() {
    return (T) this;
  }

  @Override
  ProxyRepositoryStatus convertStatus(final RepositoryStatusResource status) {
    if (status == null) {
      return new ProxyRepositoryStatusImpl(false, false, false);
    }
    return new ProxyRepositoryStatusImpl(
        "IN_SERVICE".equals(status.getLocalStatus()),
        !"ALLOW".equals(status.getProxyMode()),
        "BLOCKED_AUTO".equals(status.getProxyMode())
    );
  }

  @Override
  public String proxyUri() {
    final RepositoryResourceRemoteStorage remoteStorage = settings().getRemoteStorage();
    if (remoteStorage == null) {
      return null;
    }
    return remoteStorage.getRemoteStorageUrl();
  }

  @Override
  public T withRepoPolicy(final String policy) {
    settings().setRepoPolicy(policy);
    return me();
  }

  @Override
  public T asProxyOf(final String remoteUri) {
    getRemoteStorage().setRemoteStorageUrl(remoteUri);
    return me();
  }

  @Override
  public T withUsernameAuthentication(final String username, final String password) {
    getAuthenticationSettings().setUsername(username);
    getAuthenticationSettings().setPassword(password);
    return me();
  }

  @Override
  public T withNtlmAuthentication(final String username,
                                  final String password,
                                  final String host,
                                  final String domain)
  {
    getAuthenticationSettings().setUsername(username);
    getAuthenticationSettings().setPassword(password);
    getAuthenticationSettings().setNtlmHost(host);
    getAuthenticationSettings().setNtlmDomain(domain);
    return me();
  }

  @Override
  public T withRemoteConnectionSettings(int timeout, int retryCount, String query, String userAgent) {
    getRemoteConnectionSettings().setConnectionTimeout(timeout);
    getRemoteConnectionSettings().setRetrievalRetryCount(retryCount);
    getRemoteConnectionSettings().setQueryString(query);
    getRemoteConnectionSettings().setUserAgentString(userAgent);
    return me();
  }

  private RemoteConnectionSettings getRemoteConnectionSettings() {
    RemoteConnectionSettings conn = getRemoteStorage().getConnectionSettings();
    if (conn == null) {
      conn = new RemoteConnectionSettings();
      getRemoteStorage().setConnectionSettings(conn);
    }
    return conn;
  }

  private RepositoryResourceRemoteStorage getRemoteStorage() {
    RepositoryResourceRemoteStorage remoteStorage = settings().getRemoteStorage();
    if (remoteStorage == null) {
      remoteStorage = new RepositoryResourceRemoteStorage();
      settings().setRemoteStorage(remoteStorage);
    }
    return remoteStorage;
  }

  private AuthenticationSettings getAuthenticationSettings() {
    AuthenticationSettings auth = getRemoteStorage().getAuthentication();
    if (auth == null) {
      auth = new AuthenticationSettings();
      getRemoteStorage().setAuthentication(auth);
    }
    return auth;
  }

  @Override
  public T withNotFoundCacheTTL(final int minutes) {
    settings().setNotFoundCacheTTL(minutes);
    return me();
  }

  @Override
  public T withItemMaxAge(final int minutes) {
    settings().setItemMaxAge(minutes);
    return me();
  }

  @Override
  public int itemMaxAge() {
    return settings().getItemMaxAge();
  }

  @Override
  public T autoBlock() {
    settings().setAutoBlockActive(true);
    return me();
  }

  @Override
  public T doNotAutoBlock() {
    settings().setAutoBlockActive(false);
    return me();
  }

  @Override
  public boolean isAutoBlocking() {
    return settings().isAutoBlockActive();
  }

  @Override
  public T block() {
    final RepositoryStatusResource newStatus = doGetStatus();
    newStatus.setProxyMode("BLOCKED_MANUAL");
    doUpdateStatus(newStatus);
    return me();
  }

  @Override
  public T unblock() {
    final RepositoryStatusResource newStatus = doGetStatus();
    newStatus.setProxyMode("ALLOW");
    doUpdateStatus(newStatus);
    return me();
  }

  @Override
  public T enableBrowsing() {
    settings().setBrowseable(true);
    return me();
  }

  @Override
  public T disableBrowsing() {
    settings().setBrowseable(false);
    return me();
  }

  @Override
  public boolean isBrowsable() {
    return settings().isBrowseable();
  }
}

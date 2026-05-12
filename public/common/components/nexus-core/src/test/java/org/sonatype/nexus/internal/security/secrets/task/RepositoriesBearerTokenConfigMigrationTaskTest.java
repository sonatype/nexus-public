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
package org.sonatype.nexus.internal.security.secrets.task;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.kv.KeyValueStore;
import org.sonatype.nexus.node.datastore.NodeHeartbeatManager;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.repository.types.ProxyType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.manager.internal.HttpAuthenticationSecretEncoder.BEARER_TOKEN_MIGRATION_STARTED;

@ExtendWith(MockitoExtension.class)
class RepositoriesBearerTokenConfigMigrationTaskTest
{
  @Mock
  private NodeHeartbeatManager nodeHeartbeatManager;

  @Mock
  private RepositoryManager repositoryManager;

  @Mock
  private KeyValueStore kv;

  private RepositoriesBearerTokenConfigMigrationTask underTest;

  @BeforeEach
  void setUp() {
    lenient().when(nodeHeartbeatManager.getActiveNodeHeartbeatData()).thenReturn(Collections.emptyList());

    underTest = new RepositoriesBearerTokenConfigMigrationTask(
        nodeHeartbeatManager,
        repositoryManager,
        kv,
        Duration.ofMinutes(1), // maxWaitTimeMinutes
        Duration.ofSeconds(1) // pollIntervalSeconds
    );
  }

  @Test
  void shouldMigrateRepositoryWithOldBearerTokenKey() throws Exception {
    Repository proxyRepo = createProxyRepository("test-proxy", "old-bearer-token-value", null);
    when(repositoryManager.browse()).thenReturn(List.of(proxyRepo));

    underTest.execute();

    verify(repositoryManager).update(any(Configuration.class));
    verify(kv).setBoolean(BEARER_TOKEN_MIGRATION_STARTED, true);
  }

  @Test
  void shouldNotMigrateRepositoryWithoutOldBearerTokenKey() throws Exception {
    Repository proxyRepo = createProxyRepository("test-proxy", null, "new-bearer-token-value");
    when(repositoryManager.browse()).thenReturn(List.of(proxyRepo));

    underTest.execute();

    verify(repositoryManager, never()).update(any(Configuration.class));
    verify(kv).setBoolean(BEARER_TOKEN_MIGRATION_STARTED, true);
  }

  @Test
  void shouldSkipHostedRepositories() throws Exception {
    Repository hostedRepo = createHostedRepository("test-hosted");
    when(repositoryManager.browse()).thenReturn(List.of(hostedRepo));

    underTest.execute();

    verify(repositoryManager, never()).update(any(Configuration.class));
    verify(kv).setBoolean(BEARER_TOKEN_MIGRATION_STARTED, true);
  }

  @Test
  void shouldMigrateOldValueToNewKeyWhenNewKeyDoesNotExist() throws Exception {
    String oldTokenValue = "old-bearer-token-value";
    Map<String, Object> authMap = new HashMap<>();
    authMap.put("bearerToken", oldTokenValue);

    Repository proxyRepo = createProxyRepositoryWithAuthMap("test-proxy", authMap);
    when(repositoryManager.browse()).thenReturn(List.of(proxyRepo));

    underTest.execute();

    // Verify old key was removed and new key was added
    verify(repositoryManager).update(any(Configuration.class));
    assertThat(authMap.containsKey("bearerToken"), is(false));
    assertThat(authMap.get("bearerTokenId"), is(equalTo(oldTokenValue)));
  }

  @Test
  void shouldOnlyRemoveOldKeyWhenNewKeyAlreadyExists() throws Exception {
    String oldTokenValue = "old-bearer-token-value";
    String newTokenValue = "new-bearer-token-value";
    Map<String, Object> authMap = new HashMap<>();
    authMap.put("bearerToken", oldTokenValue);
    authMap.put("bearerTokenId", newTokenValue);

    Repository proxyRepo = createProxyRepositoryWithAuthMap("test-proxy", authMap);
    when(repositoryManager.browse()).thenReturn(List.of(proxyRepo));

    underTest.execute();

    // Verify old key was removed but new key value preserved
    verify(repositoryManager).update(any(Configuration.class));
    assertThat(authMap.containsKey("bearerToken"), is(false));
    assertThat(authMap.get("bearerTokenId"), is(equalTo(newTokenValue)));
  }

  @Test
  void shouldHandleRepositoryWithNoHttpClientConfig() throws Exception {
    Repository proxyRepo = createProxyRepositoryWithoutHttpClient("test-proxy");
    when(repositoryManager.browse()).thenReturn(List.of(proxyRepo));

    underTest.execute();

    verify(repositoryManager, never()).update(any(Configuration.class));
    verify(kv).setBoolean(BEARER_TOKEN_MIGRATION_STARTED, true);
  }

  @Test
  void shouldHandleRepositoryWithNoAuthenticationConfig() throws Exception {
    Repository proxyRepo = createProxyRepositoryWithoutAuthentication("test-proxy");
    when(repositoryManager.browse()).thenReturn(List.of(proxyRepo));

    underTest.execute();

    verify(repositoryManager, never()).update(any(Configuration.class));
    verify(kv).setBoolean(BEARER_TOKEN_MIGRATION_STARTED, true);
  }

  private Repository createProxyRepository(String name, String oldBearerToken, String newBearerToken) {
    Map<String, Object> authMap = new HashMap<>();
    if (oldBearerToken != null) {
      authMap.put("bearerToken", oldBearerToken);
    }
    if (newBearerToken != null) {
      authMap.put("bearerTokenId", newBearerToken);
    }
    return createProxyRepositoryWithAuthMap(name, authMap);
  }

  private Repository createProxyRepositoryWithAuthMap(String name, Map<String, Object> authMap) {
    Repository repository = mock(Repository.class);
    Configuration configuration = mock(Configuration.class);

    when(repository.getType()).thenReturn(new ProxyType());
    when(repository.getConfiguration()).thenReturn(configuration);
    lenient().when(configuration.getRepositoryName()).thenReturn(name);
    when(configuration.copy()).thenReturn(configuration);

    Map<String, Object> httpClientMap = new HashMap<>();
    httpClientMap.put("authentication", authMap);

    NestedAttributesMap httpClientAttrs = new NestedAttributesMap("httpclient", httpClientMap);
    when(configuration.attributes("httpclient")).thenReturn(httpClientAttrs);

    return repository;
  }

  private Repository createProxyRepositoryWithoutHttpClient(String name) {
    Repository repository = mock(Repository.class);
    Configuration configuration = mock(Configuration.class);

    when(repository.getType()).thenReturn(new ProxyType());
    when(repository.getConfiguration()).thenReturn(configuration);
    lenient().when(configuration.getRepositoryName()).thenReturn(name);
    when(configuration.copy()).thenReturn(configuration);
    when(configuration.attributes("httpclient")).thenReturn(new NestedAttributesMap("httpclient", new HashMap<>()));

    return repository;
  }

  private Repository createProxyRepositoryWithoutAuthentication(String name) {
    Repository repository = mock(Repository.class);
    Configuration configuration = mock(Configuration.class);

    when(repository.getType()).thenReturn(new ProxyType());
    when(repository.getConfiguration()).thenReturn(configuration);
    lenient().when(configuration.getRepositoryName()).thenReturn(name);
    when(configuration.copy()).thenReturn(configuration);

    Map<String, Object> httpClientMap = new HashMap<>();
    // No authentication key
    NestedAttributesMap httpClientAttrs = new NestedAttributesMap("httpclient", httpClientMap);
    when(configuration.attributes("httpclient")).thenReturn(httpClientAttrs);

    return repository;
  }

  private Repository createHostedRepository(String name) {
    Repository repository = mock(Repository.class);
    Configuration configuration = mock(Configuration.class);

    when(repository.getType()).thenReturn(new HostedType());
    lenient().when(repository.getConfiguration()).thenReturn(configuration);
    lenient().when(configuration.getRepositoryName()).thenReturn(name);

    return repository;
  }
}

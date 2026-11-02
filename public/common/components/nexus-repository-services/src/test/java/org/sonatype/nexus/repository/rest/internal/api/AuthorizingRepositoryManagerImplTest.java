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
package org.sonatype.nexus.repository.rest.internal.api;

import java.util.HashMap;
import java.util.Map;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.cache.RepositoryCacheInvalidationService;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.config.internal.ConfigurationData;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.security.RepositoryPermissionChecker;
import org.sonatype.nexus.scheduling.TaskScheduler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthorizingRepositoryManagerImplTest
{
  @Mock
  private RepositoryManager repositoryManager;

  @Mock
  private RepositoryPermissionChecker repositoryPermissionChecker;

  @Mock
  private TaskScheduler taskScheduler;

  @Mock
  private RepositoryCacheInvalidationService repositoryCacheInvalidationService;

  @Mock
  private Repository repository;

  @Test
  void updateRestoresStoredPasswordBeforeCallingRepositoryManager() throws Exception {
    AuthorizingRepositoryManagerImpl manager = new AuthorizingRepositoryManagerImpl(
        repositoryManager, repositoryPermissionChecker, taskScheduler, repositoryCacheInvalidationService);

    Configuration storedConfig = new ConfigurationData();
    Map<String, Object> storedAuth = new HashMap<>();
    storedAuth.put("password", "_stored");
    Map<String, Object> storedHttpclient = new HashMap<>();
    storedHttpclient.put("authentication", storedAuth);
    Map<String, Object> storedProxy = new HashMap<>();
    storedProxy.put("remoteUrl", "http://a/");
    Map<String, Map<String, Object>> storedAttrs = new HashMap<>();
    storedAttrs.put("httpclient", storedHttpclient);
    storedAttrs.put("proxy", storedProxy);
    storedConfig.setAttributes(storedAttrs);

    when(repository.getConfiguration()).thenReturn(storedConfig);
    when(repositoryManager.get("test")).thenReturn(repository);

    Configuration incoming = new ConfigurationData();
    incoming.setRepositoryName("test");
    Map<String, Object> incomingAuth = new HashMap<>();
    incomingAuth.put("password", ProxySecretPlaceholders.PLACEHOLDER);
    Map<String, Object> incomingHttpclient = new HashMap<>();
    incomingHttpclient.put("authentication", incomingAuth);
    Map<String, Object> incomingProxy = new HashMap<>();
    incomingProxy.put("remoteUrl", "http://a/");
    Map<String, Map<String, Object>> incomingAttrs = new HashMap<>();
    incomingAttrs.put("httpclient", incomingHttpclient);
    incomingAttrs.put("proxy", incomingProxy);
    incoming.setAttributes(incomingAttrs);

    boolean result = manager.update(incoming);

    ArgumentCaptor<Configuration> captor = ArgumentCaptor.forClass(Configuration.class);
    verify(repositoryManager).update(captor.capture());
    @SuppressWarnings("unchecked")
    Map<String, Object> auth = (Map<String, Object>) captor.getValue()
        .getAttributes()
        .get("httpclient")
        .get("authentication");
    assertThat(auth.get("password"), is("_stored"));
    assertThat(result, is(true));
  }

  @Test
  void updateRejectsUrlChangeCarryingPlaceholder() throws Exception {
    AuthorizingRepositoryManagerImpl manager = new AuthorizingRepositoryManagerImpl(
        repositoryManager, repositoryPermissionChecker, taskScheduler, repositoryCacheInvalidationService);

    Configuration storedConfig = new ConfigurationData();
    Map<String, Object> storedAuth = new HashMap<>();
    storedAuth.put("password", "_stored");
    Map<String, Object> storedHttpclient = new HashMap<>();
    storedHttpclient.put("authentication", storedAuth);
    Map<String, Object> storedProxy = new HashMap<>();
    storedProxy.put("remoteUrl", "http://a/");
    Map<String, Map<String, Object>> storedAttrs = new HashMap<>();
    storedAttrs.put("httpclient", storedHttpclient);
    storedAttrs.put("proxy", storedProxy);
    storedConfig.setAttributes(storedAttrs);

    when(repository.getConfiguration()).thenReturn(storedConfig);
    when(repositoryManager.get("test")).thenReturn(repository);

    Configuration incoming = new ConfigurationData();
    incoming.setRepositoryName("test");
    Map<String, Object> incomingAuth = new HashMap<>();
    incomingAuth.put("password", ProxySecretPlaceholders.PLACEHOLDER);
    Map<String, Object> incomingHttpclient = new HashMap<>();
    incomingHttpclient.put("authentication", incomingAuth);
    Map<String, Object> incomingProxy = new HashMap<>();
    incomingProxy.put("remoteUrl", "http://b/");
    Map<String, Map<String, Object>> incomingAttrs = new HashMap<>();
    incomingAttrs.put("httpclient", incomingHttpclient);
    incomingAttrs.put("proxy", incomingProxy);
    incoming.setAttributes(incomingAttrs);

    assertThrows(IllegalArgumentException.class, () -> manager.update(incoming));

    verify(repositoryManager, never()).update(any());
  }

  @Test
  void updateWithNewPasswordPassesItThrough() throws Exception {
    AuthorizingRepositoryManagerImpl manager = new AuthorizingRepositoryManagerImpl(
        repositoryManager, repositoryPermissionChecker, taskScheduler, repositoryCacheInvalidationService);

    Configuration storedConfig = new ConfigurationData();
    Map<String, Object> storedAuth = new HashMap<>();
    storedAuth.put("password", "_stored");
    Map<String, Object> storedHttpclient = new HashMap<>();
    storedHttpclient.put("authentication", storedAuth);
    Map<String, Object> storedProxy = new HashMap<>();
    storedProxy.put("remoteUrl", "http://a/");
    Map<String, Map<String, Object>> storedAttrs = new HashMap<>();
    storedAttrs.put("httpclient", storedHttpclient);
    storedAttrs.put("proxy", storedProxy);
    storedConfig.setAttributes(storedAttrs);

    when(repository.getConfiguration()).thenReturn(storedConfig);
    when(repositoryManager.get("test")).thenReturn(repository);

    Configuration incoming = new ConfigurationData();
    incoming.setRepositoryName("test");
    Map<String, Object> incomingAuth = new HashMap<>();
    incomingAuth.put("password", "newPassword");
    Map<String, Object> incomingHttpclient = new HashMap<>();
    incomingHttpclient.put("authentication", incomingAuth);
    Map<String, Object> incomingProxy = new HashMap<>();
    incomingProxy.put("remoteUrl", "http://a/");
    Map<String, Map<String, Object>> incomingAttrs = new HashMap<>();
    incomingAttrs.put("httpclient", incomingHttpclient);
    incomingAttrs.put("proxy", incomingProxy);
    incoming.setAttributes(incomingAttrs);

    boolean result = manager.update(incoming);

    ArgumentCaptor<Configuration> captor = ArgumentCaptor.forClass(Configuration.class);
    verify(repositoryManager).update(captor.capture());
    @SuppressWarnings("unchecked")
    Map<String, Object> auth = (Map<String, Object>) captor.getValue()
        .getAttributes()
        .get("httpclient")
        .get("authentication");
    assertThat(auth.get("password"), is("newPassword"));
    assertThat(result, is(true));
  }

  @Test
  void updateReturnsFalseWhenRepositoryMissing() throws Exception {
    AuthorizingRepositoryManagerImpl manager = new AuthorizingRepositoryManagerImpl(
        repositoryManager, repositoryPermissionChecker, taskScheduler, repositoryCacheInvalidationService);

    Configuration incoming = new ConfigurationData();
    incoming.setRepositoryName("missing");
    when(repositoryManager.get("missing")).thenReturn(null);

    boolean result = manager.update(incoming);

    assertThat(result, is(false));
  }

  @Test
  void updateRestoresStoredEcrSessionTokenBeforeCallingRepositoryManager() throws Exception {
    AuthorizingRepositoryManagerImpl manager = new AuthorizingRepositoryManagerImpl(
        repositoryManager, repositoryPermissionChecker, taskScheduler, repositoryCacheInvalidationService);

    Configuration storedConfig = new ConfigurationData();
    Map<String, Object> storedEcrAuth = new HashMap<>();
    storedEcrAuth.put("sessionToken", "_storedToken");
    Map<String, Object> storedDockerProxy = new HashMap<>();
    storedDockerProxy.put("ecrAuth", storedEcrAuth);
    Map<String, Object> storedProxy = new HashMap<>();
    storedProxy.put("remoteUrl", "http://a/");
    Map<String, Map<String, Object>> storedAttrs = new HashMap<>();
    storedAttrs.put("dockerProxy", storedDockerProxy);
    storedAttrs.put("proxy", storedProxy);
    storedConfig.setAttributes(storedAttrs);

    when(repository.getConfiguration()).thenReturn(storedConfig);
    when(repositoryManager.get("test")).thenReturn(repository);

    Configuration incoming = new ConfigurationData();
    incoming.setRepositoryName("test");
    Map<String, Object> incomingEcrAuth = new HashMap<>();
    incomingEcrAuth.put("sessionToken", ProxySecretPlaceholders.PLACEHOLDER);
    Map<String, Object> incomingDockerProxy = new HashMap<>();
    incomingDockerProxy.put("ecrAuth", incomingEcrAuth);
    Map<String, Object> incomingProxy = new HashMap<>();
    incomingProxy.put("remoteUrl", "http://a/");
    Map<String, Map<String, Object>> incomingAttrs = new HashMap<>();
    incomingAttrs.put("dockerProxy", incomingDockerProxy);
    incomingAttrs.put("proxy", incomingProxy);
    incoming.setAttributes(incomingAttrs);

    boolean result = manager.update(incoming);

    ArgumentCaptor<Configuration> captor = ArgumentCaptor.forClass(Configuration.class);
    verify(repositoryManager).update(captor.capture());
    @SuppressWarnings("unchecked")
    Map<String, Object> ecrAuth = (Map<String, Object>) captor.getValue()
        .getAttributes()
        .get("dockerProxy")
        .get("ecrAuth");
    assertThat(ecrAuth.get("sessionToken"), is("_storedToken"));
    assertThat(result, is(true));
  }
}

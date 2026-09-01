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
package org.sonatype.nexus.repository.security.internal.secrets.migration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

import org.sonatype.nexus.crypto.secrets.Secret;
import org.sonatype.nexus.crypto.secrets.SecretsService;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.config.internal.ConfigurationData;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.types.ProxyType;
import org.sonatype.nexus.security.UserIdHelper;
import org.sonatype.nexus.security.secrets.SecretMigrationException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.security.internal.secrets.migration.RepositoriesSecretsMigrator.AUTHENTICATION_KEY;
import static org.sonatype.nexus.repository.security.internal.secrets.migration.RepositoriesSecretsMigrator.CLEANUP_KEY;
import static org.sonatype.nexus.repository.security.internal.secrets.migration.RepositoriesSecretsMigrator.CLEANUP_POLICY_NAME_KEY;
import static org.sonatype.nexus.repository.security.internal.secrets.migration.RepositoriesSecretsMigrator.HTTP_CLIENT_KEY;
import static org.sonatype.nexus.repository.security.internal.secrets.migration.RepositoriesSecretsMigrator.PASSWORD_KEY;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.Silent.class)
public class RepositoriesSecretsMigratorTest
{
  @Mock
  private RepositoryManager repositoryManager;

  @Mock
  private SecretsService secretsService;

  private MockedStatic<UserIdHelper> userIdHelperMock;

  private RepositoriesSecretsMigrator underTest;

  @Before
  public void setUp() {
    underTest = new RepositoriesSecretsMigrator(secretsService, repositoryManager);

    userIdHelperMock = mockStatic(UserIdHelper.class);

    userIdHelperMock.when(UserIdHelper::get).thenReturn("system");

    mockSecretsServiceFrom();
  }

  @After
  public void teardown() {
    userIdHelperMock.close();
  }

  @Test
  public void testMigrate_proxy() throws Exception {
    mockRepositoryManager(mockProxy(null), mockProxy("my-password"));

    underTest.migrate();

    verify(secretsService).from("my-password");
    verify(repositoryManager, times(1)).update(any(Configuration.class));
  }

  @Test
  public void testMigrate_proxy_notRequired() throws Exception {
    mockRepositoryManager(mockProxy(null));

    underTest.migrate();

    verify(repositoryManager, never()).update(any());
  }

  @Test
  public void testMigrate_proxy_alreadyMigrated() throws Exception {
    mockRepositoryManager(mockProxy(null), mockProxy("_2"));

    underTest.migrate();

    verify(secretsService, never()).encrypt(any(), any(), any());
    verify(repositoryManager, never()).update(any());
  }

  @Test
  public void testMigrate_proxy_trimsRemoteUrl() throws Exception {
    // Test for NEXUS-51248: remoteUrl with trailing whitespace should be trimmed
    Repository proxyWithWhitespace = mockProxyWithRemoteUrl("https://repo1.maven.org/maven2/ ");

    mockRepositoryManager(proxyWithWhitespace);

    underTest.migrate();

    // Verify that update was called with the trimmed remoteUrl
    ArgumentCaptor<Configuration> captor = ArgumentCaptor.forClass(Configuration.class);
    verify(repositoryManager, times(1)).update(captor.capture());
    assertThat(captor.getValue().attributes("proxy").get("remoteUrl", String.class))
        .isEqualTo("https://repo1.maven.org/maven2/");
  }

  @Test
  public void testMigrate_proxy_removesStaleCleanupPolicyAndRetries() throws Exception {
    // NEXUS-53457: proxy references a cleanup policy that does not exist ("string")
    Repository proxy = mockProxyWithCleanup("my-password", "string");
    mockRepositoryManager(proxy);

    ConstraintViolationException cleanupViolation = cleanupViolation("Cleanup Policy 'string' does not exist.");
    when(repositoryManager.update(any(Configuration.class)))
        .thenThrow(cleanupViolation)
        .thenReturn(proxy);

    // should NOT throw - the stale reference is stripped and the update retried
    underTest.migrate();

    ArgumentCaptor<Configuration> captor = ArgumentCaptor.forClass(Configuration.class);
    verify(repositoryManager, times(2)).update(captor.capture());
    // after sanitization the (only) stale policy is gone, so the cleanup attribute is removed entirely
    assertThat(captor.getValue().getAttributes().get(CLEANUP_KEY)).isNull();
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testMigrate_proxy_removesOnlyMissingCleanupPolicy() throws Exception {
    // a valid policy ("keep-me") must survive; only the missing one ("string") is stripped
    Repository proxy = mockProxyWithCleanup("my-password", "keep-me", "string");
    mockRepositoryManager(proxy);

    ConstraintViolationException violation = cleanupViolation("Cleanup Policy 'string' does not exist.");
    when(repositoryManager.update(any(Configuration.class)))
        .thenThrow(violation)
        .thenReturn(proxy);

    underTest.migrate();

    ArgumentCaptor<Configuration> captor = ArgumentCaptor.forClass(Configuration.class);
    verify(repositoryManager, times(2)).update(captor.capture());
    Map<String, Object> cleanup = captor.getValue().getAttributes().get(CLEANUP_KEY);
    assertThat((Collection<String>) cleanup.get(CLEANUP_POLICY_NAME_KEY)).containsExactly("keep-me");
  }

  @Test
  public void testMigrate_proxy_nonCleanupFailure_isNotRetried() throws Exception {
    Repository proxy = mockProxyWithCleanup("my-password", "string");
    mockRepositoryManager(proxy);

    // a failure unrelated to a missing cleanup policy must still abort (no silent retry)
    ConstraintViolationException violation = cleanupViolation("Invalid proxy URL format");
    when(repositoryManager.update(any(Configuration.class)))
        .thenThrow(violation);

    assertThatThrownBy(() -> underTest.migrate())
        .isInstanceOf(SecretMigrationException.class)
        .hasMessageContaining("proxy-with-cleanup");

    verify(repositoryManager, times(1)).update(any(Configuration.class));
  }

  @Test
  public void testMigrate_proxy_retryStillFailing_throws() throws Exception {
    Repository proxy = mockProxyWithCleanup("my-password", "string");
    mockRepositoryManager(proxy);

    // cleanup violation on both attempts (e.g. another validator also fails) -> still surfaces failure
    ConstraintViolationException first = cleanupViolation("Cleanup Policy 'string' does not exist.");
    ConstraintViolationException second = cleanupViolation("Cleanup Policy 'string' does not exist.");
    when(repositoryManager.update(any(Configuration.class)))
        .thenThrow(first)
        .thenThrow(second);

    assertThatThrownBy(() -> underTest.migrate())
        .isInstanceOf(SecretMigrationException.class);

    verify(repositoryManager, times(2)).update(any(Configuration.class));
  }

  private void mockRepositoryManager(final Repository... repositories) {
    when(repositoryManager.browse()).thenReturn(Arrays.asList(repositories));
  }

  private static ConstraintViolationException cleanupViolation(final String... messages) {
    Set<ConstraintViolation<?>> violations = new HashSet<>();
    for (String message : messages) {
      ConstraintViolation<?> violation = mock(ConstraintViolation.class);
      when(violation.getMessage()).thenReturn(message);
      violations.add(violation);
    }
    return new ConstraintViolationException(violations);
  }

  private static Repository mockProxyWithCleanup(final String passwordKey, final String... policyNames) {
    Configuration configuration = new ConfigurationData();
    Repository repository = mock(Repository.class);
    when(repository.getConfiguration()).thenReturn(configuration);
    when(repository.getType()).thenReturn(new ProxyType());

    configuration.setRepositoryName("proxy-with-cleanup");
    configuration.setAttributes(new HashMap<>());
    configuration.attributes(HTTP_CLIENT_KEY).child(AUTHENTICATION_KEY).set(PASSWORD_KEY, passwordKey);
    configuration.attributes(CLEANUP_KEY).set(CLEANUP_POLICY_NAME_KEY, new ArrayList<>(Arrays.asList(policyNames)));

    return repository;
  }

  private void mockSecretsServiceFrom() {
    when(secretsService.from(any())).then(i -> {
      Secret secret = mock(Secret.class);
      when(secret.decrypt()).thenReturn(i.getArgument(0, String.class).toCharArray());
      when(secret.getId()).thenReturn(i.getArgument(0, String.class));
      return secret;
    });
  }

  private static Repository mockProxy(final String passwordKey) {
    Configuration configuration = new ConfigurationData();
    Repository repository = mock(Repository.class);
    when(repository.getConfiguration()).thenReturn(configuration);
    when(repository.getType()).thenReturn(new ProxyType());

    configuration.setRepositoryName(passwordKey);

    configuration.setAttributes(new HashMap<>());

    if (passwordKey == null) {
      return repository;
    }

    if (passwordKey != null) {
      configuration.attributes(HTTP_CLIENT_KEY)
          .child(AUTHENTICATION_KEY)
          .set(PASSWORD_KEY, passwordKey);
    }

    return repository;
  }

  private static Repository mockProxyWithRemoteUrl(final String remoteUrl) {
    Configuration configuration = new ConfigurationData();
    Repository repository = mock(Repository.class);
    when(repository.getConfiguration()).thenReturn(configuration);
    when(repository.getType()).thenReturn(new ProxyType());

    configuration.setRepositoryName("test-proxy");
    configuration.setAttributes(new HashMap<>());

    if (remoteUrl != null) {
      configuration.attributes("proxy").set("remoteUrl", remoteUrl);
    }

    return repository;
  }
}

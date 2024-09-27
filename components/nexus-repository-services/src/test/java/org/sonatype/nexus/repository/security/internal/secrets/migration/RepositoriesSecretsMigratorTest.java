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

import java.util.Arrays;
import java.util.HashMap;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.crypto.secrets.Secret;
import org.sonatype.nexus.crypto.secrets.SecretsService;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.config.internal.ConfigurationData;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.types.ProxyType;
import org.sonatype.nexus.security.UserIdHelper;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.security.internal.secrets.migration.RepositoriesSecretsMigrator.AUTHENTICATION_KEY;
import static org.sonatype.nexus.repository.security.internal.secrets.migration.RepositoriesSecretsMigrator.HTTP_CLIENT_KEY;
import static org.sonatype.nexus.repository.security.internal.secrets.migration.RepositoriesSecretsMigrator.PASSWORD_KEY;

public class RepositoriesSecretsMigratorTest
    extends TestSupport
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

  private void mockRepositoryManager(final Repository... repositories) {
    when(repositoryManager.browse()).thenReturn(Arrays.asList(repositories));
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
}

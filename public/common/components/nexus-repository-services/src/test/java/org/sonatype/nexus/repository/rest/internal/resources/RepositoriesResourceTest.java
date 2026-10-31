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
package org.sonatype.nexus.repository.rest.internal.resources;

import java.util.List;
import java.util.Map;

import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.rest.api.RepositoryManagerRESTAdapter;
import org.sonatype.nexus.repository.rest.api.RepositoryXO;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.repository.types.ProxyType;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension.WithUser;

import org.apache.shiro.authz.UnauthenticatedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, AuthenticationExtension.class})
public class RepositoriesResourceTest
{
  private static final String REPOSITORY_1_NAME = "repoOneName";

  private static final Format REPOSITORY_1_FORMAT = new Format("repoOneFormat")
  {
  };

  private static final Type REPOSITORY_1_TYPE = new ProxyType();

  private static final String REPOSITORY_1_URL = "repoOneUrl";

  private static final String REPOSITORY_1_REMOTE_URL = "repoOneRemoteUrl";

  private static final RepositoryXO REPOSITORY_XO_1 = RepositoryXO.builder()
      .name(REPOSITORY_1_NAME)
      .format(REPOSITORY_1_FORMAT.getValue())
      .type(REPOSITORY_1_TYPE.getValue())
      .url(REPOSITORY_1_URL)
      .attributes(Map.of("proxy", Map.of("remoteUrl", REPOSITORY_1_REMOTE_URL)))
      .build();

  private static final String REPOSITORY_2_NAME = "repoTwoName";

  private static final Format REPOSITORY_2_FORMAT = new Format("repoTwoFormat")
  {
  };

  private static final Type REPOSITORY_2_TYPE = new HostedType();

  private static final String REPOSITORY_2_URL = "repoTwoUrl";

  private static final RepositoryXO REPOSITORY_XO_2 = RepositoryXO.builder()
      .name(REPOSITORY_2_NAME)
      .format(REPOSITORY_2_FORMAT.getValue())
      .type(REPOSITORY_2_TYPE.getValue())
      .url(REPOSITORY_2_URL)
      .attributes(Map.of())
      .build();

  @Mock
  private RepositoryManagerRESTAdapter repositoryManagerRESTAdapter;

  private RepositoriesResource underTest;

  @BeforeEach
  public void setup() {
    underTest = new RepositoriesResource(repositoryManagerRESTAdapter);
  }

  @WithUser(isAuthenticated = false)
  @Test
  public void testGetRepositories() {
    when(repositoryManagerRESTAdapter.getRepositories()).thenReturn(List.of(REPOSITORY_XO_1, REPOSITORY_XO_2));
    assertThat(underTest.getRepositories(), is(List.of(REPOSITORY_XO_1, REPOSITORY_XO_2)));
  }

  @Test
  void testGetRepositories_unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> underTest.getRepositories());
    verifyNoInteractions(repositoryManagerRESTAdapter);
  }

  @WithUser(isAuthenticated = false)
  @Test
  public void testGetRepository() {
    Repository repository1 = createMockRepository(REPOSITORY_1_NAME, REPOSITORY_1_FORMAT, REPOSITORY_1_TYPE,
        REPOSITORY_1_URL, REPOSITORY_1_REMOTE_URL);
    Repository repository2 =
        createMockRepository(REPOSITORY_2_NAME, REPOSITORY_2_FORMAT, REPOSITORY_2_TYPE, REPOSITORY_2_URL, null);

    when(repositoryManagerRESTAdapter.getRepository(REPOSITORY_1_NAME)).thenReturn(repository1);
    when(repositoryManagerRESTAdapter.getRepository(REPOSITORY_2_NAME)).thenReturn(repository2);

    assertThat(underTest.getRepository(REPOSITORY_1_NAME), is(REPOSITORY_XO_1));
    assertThat(underTest.getRepository(REPOSITORY_2_NAME), is(REPOSITORY_XO_2));
  }

  @Test
  void testGetRepository_unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> underTest.getRepository(REPOSITORY_1_NAME));
    verifyNoInteractions(repositoryManagerRESTAdapter);
  }

  private static Repository createMockRepository(
      final String name,
      final Format format,
      final Type type,
      final String url,
      final String remoteUrl)
  {
    Repository repository = mock(Repository.class);
    when(repository.getName()).thenReturn(name);
    when(repository.getFormat()).thenReturn(format);
    when(repository.getType()).thenReturn(type);
    when(repository.getUrl()).thenReturn(url);
    Configuration configuration = mock(Configuration.class);
    if (type instanceof ProxyType) {
      when(configuration.attributes("proxy")).thenReturn(
          new NestedAttributesMap("proxy", Map.of("remoteUrl", remoteUrl)));
    }
    when(repository.getConfiguration()).thenReturn(configuration);
    return repository;
  }
}

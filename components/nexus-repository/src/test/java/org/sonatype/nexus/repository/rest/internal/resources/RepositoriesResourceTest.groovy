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
package org.sonatype.nexus.repository.rest.internal.resources

import org.sonatype.goodies.testsupport.TestSupport
import org.sonatype.nexus.repository.Format
import org.sonatype.nexus.repository.Repository
import org.sonatype.nexus.repository.Type
import org.sonatype.nexus.repository.config.Configuration
import org.sonatype.nexus.repository.rest.api.RepositoryXO
import org.sonatype.nexus.repository.types.HostedType
import org.sonatype.nexus.repository.types.ProxyType

import org.junit.Before
import org.junit.Test
import org.mockito.Mock

import static org.hamcrest.Matchers.is
import static org.junit.Assert.assertThat
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

class RepositoriesResourceTest
    extends TestSupport
{
  private static final String REPOSITORY_1_NAME = 'repoOneName'

  private static final Format REPOSITORY_1_FORMAT = new Format('repoOneFormat') {}

  private static final Type REPOSITORY_1_TYPE = new ProxyType()

  private static final String REPOSITORY_1_URL = 'repoOneUrl'

  private static final String REPOSITORY_1_REMOTE_URL = 'repoOneRemoteUrl'

  private static final RepositoryXO REPOSITORY_XO_1 =
      new RepositoryXO(name: REPOSITORY_1_NAME, format: REPOSITORY_1_FORMAT.getValue(),
          type: REPOSITORY_1_TYPE.getValue(), url: REPOSITORY_1_URL,
          attributes: [proxy: [remoteUrl: REPOSITORY_1_REMOTE_URL]])

  private static final String REPOSITORY_2_NAME = 'repoTwoName'

  private static final Format REPOSITORY_2_FORMAT = new Format('repoTwoFormat') {}

  private static final Type REPOSITORY_2_TYPE = new HostedType()

  private static final String REPOSITORY_2_URL = 'repoTwoUrl'

  private static final RepositoryXO REPOSITORY_XO_2 =
      new RepositoryXO(name: REPOSITORY_2_NAME, format: REPOSITORY_2_FORMAT.getValue(),
          type: REPOSITORY_2_TYPE.getValue(), url: REPOSITORY_2_URL, attributes: [:])

  @Mock
  private RepositoryManagerRESTAdapter repositoryManagerRESTAdapter

  private RepositoriesResource underTest

  @Before
  void setup() {
    Repository repository1 =
        createMockRepository(REPOSITORY_1_NAME, REPOSITORY_1_FORMAT, REPOSITORY_1_TYPE, REPOSITORY_1_URL,
            REPOSITORY_1_REMOTE_URL)
    Repository repository2 =
        createMockRepository(REPOSITORY_2_NAME, REPOSITORY_2_FORMAT, REPOSITORY_2_TYPE, REPOSITORY_2_URL, null)

    when(repositoryManagerRESTAdapter.getRepositories()).thenReturn([repository1, repository2])

    underTest = new RepositoriesResource(repositoryManagerRESTAdapter)
  }

  @Test
  void testGetRepositories() {
    assertThat(underTest.getRepositories(), is([REPOSITORY_XO_1, REPOSITORY_XO_2]))
  }

  private static Repository createMockRepository(final String name, final Format format, final Type type,
                                                 final String url, final String remoteUrl)
  {
    Repository repository = mock(Repository.class)
    when(repository.getName()).thenReturn(name)
    when(repository.getFormat()).thenReturn(format)
    when(repository.getType()).thenReturn(type)
    when(repository.getUrl()).thenReturn(url)
    def configuration = new Configuration()
    if(type instanceof ProxyType) {
      configuration.setAttributes([proxy: [remoteUrl: remoteUrl]])
    }
    when(repository.getConfiguration()).thenReturn(configuration)
    return repository
  }
}

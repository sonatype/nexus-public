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
package org.sonatype.nexus.coreui;

import java.util.Arrays;
import java.util.List;

import org.sonatype.goodies.testsupport.Test5Support;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.rest.api.AuthorizingRepositoryManager;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.repository.types.ProxyType;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension.WithUser;
import org.sonatype.nexus.bootstrap.validation.ValidationConfiguration;
import org.sonatype.nexus.testcommon.validation.ValidationExtension;
import org.sonatype.nexus.testcommon.validation.ValidationExtension.ValidationExecutor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import javax.validation.Validator;
import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorFactoryImpl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.config.ConfigurationConstants.COMPONENT;
import static org.sonatype.nexus.repository.config.ConfigurationConstants.PROPRIETARY_COMPONENTS;

@ExtendWith(ValidationExtension.class)
@ExtendWith(AuthenticationExtension.class)
@WithUser
class ProprietaryRepositoriesComponentTest
    extends Test5Support
{
  @ValidationExecutor
  private final Validator validator =
      new ValidationConfiguration().validatorFactory(new ConstraintValidatorFactoryImpl()).getValidator();

  @Mock
  private AuthorizingRepositoryManager repositoryManager;

  private ProprietaryRepositoriesComponent underTest;

  @BeforeEach
  void setUp() {
    underTest = new ProprietaryRepositoriesComponent(repositoryManager);
  }

  @Test
  void testReadPossibleRepos_excludesDockerHostedRepositories() {
    Repository mavenHosted = createRepository("maven-hosted", "maven2", HostedType.NAME, false);
    Repository npmHosted = createRepository("npm-hosted", "npm", HostedType.NAME, false);
    Repository dockerHosted = createRepository("docker-hosted", "docker", HostedType.NAME, false);
    Repository dockerProxy = createRepository("docker-proxy", "docker", ProxyType.NAME, false);

    when(repositoryManager.getRepositoriesWithAdmin())
        .thenReturn(Arrays.asList(mavenHosted, npmHosted, dockerHosted, dockerProxy));

    List<ReferenceXO> result = underTest.readPossibleRepos();

    assertThat(result, hasSize(2));
    assertThat(result.stream().map(ReferenceXO::getName).toList(),
        contains("maven-hosted", "npm-hosted"));
  }

  @Test
  public void testReadPossibleRepos_includesNonDockerHostedRepositories() {
    Repository mavenHosted = createRepository("maven-hosted", "maven2", HostedType.NAME, false);
    Repository pypiHosted = createRepository("pypi-hosted", "pypi", HostedType.NAME, false);
    Repository rawHosted = createRepository("raw-hosted", "raw", HostedType.NAME, false);

    when(repositoryManager.getRepositoriesWithAdmin())
        .thenReturn(Arrays.asList(mavenHosted, pypiHosted, rawHosted));

    List<ReferenceXO> result = underTest.readPossibleRepos();

    assertThat(result, hasSize(3));
    assertThat(result.stream().map(ReferenceXO::getName).toList(),
        contains("maven-hosted", "pypi-hosted", "raw-hosted"));
  }

  @Test
  public void testReadPossibleRepos_excludesNonHostedRepositories() {
    Repository mavenHosted = createRepository("maven-hosted", "maven2", HostedType.NAME, false);
    Repository mavenProxy = createRepository("maven-proxy", "maven2", ProxyType.NAME, false);

    when(repositoryManager.getRepositoriesWithAdmin())
        .thenReturn(Arrays.asList(mavenHosted, mavenProxy));

    List<ReferenceXO> result = underTest.readPossibleRepos();

    assertThat(result, hasSize(1));
    assertThat(result.stream().map(ReferenceXO::getName).toList(),
        contains("maven-hosted"));
  }

  @Test
  public void testReadPossibleRepos_returnsEmptyWhenOnlyDockerHosted() {
    Repository dockerHosted = createRepository("docker-hosted", "docker", HostedType.NAME, false);

    when(repositoryManager.getRepositoriesWithAdmin())
        .thenReturn(Arrays.asList(dockerHosted));

    List<ReferenceXO> result = underTest.readPossibleRepos();

    assertThat(result, empty());
  }

  @Test
  public void testRead_excludesDockerHostedFromProprietaryList() {
    // read() should also exclude Docker repos to keep counts consistent
    Repository mavenHosted = createRepository("maven-hosted", "maven2", HostedType.NAME, true);
    Repository dockerHosted = createRepository("docker-hosted", "docker", HostedType.NAME, true);
    Repository pypiHosted = createRepository("pypi-hosted", "pypi", HostedType.NAME, true);

    when(repositoryManager.getRepositoriesWithAdmin())
        .thenReturn(Arrays.asList(mavenHosted, dockerHosted, pypiHosted));

    ProprietaryRepositoriesXO result = underTest.read();

    assertThat(result.getEnabledRepositories(), hasSize(2));
    assertThat(result.getEnabledRepositories(), contains("maven-hosted", "pypi-hosted"));
    assertThat(result.getEnabledRepositories(), not(contains("docker-hosted")));
  }

  @Test
  public void testUpdate_excludesDockerRepositoriesFromProcessing() throws Exception {
    // update() should filter out Docker repos to prevent API bypass
    Repository mavenHosted = createRepository("maven-hosted", "maven2", HostedType.NAME, false);
    Repository npmHosted = createRepository("npm-hosted", "npm", HostedType.NAME, false);
    Repository dockerHosted = createRepository("docker-hosted", "docker", HostedType.NAME, false);

    when(repositoryManager.getRepositoriesWithAdmin())
        .thenReturn(Arrays.asList(mavenHosted, npmHosted, dockerHosted));

    ProprietaryRepositoriesXO request = new ProprietaryRepositoriesXO();
    request.setEnabledRepositories(Arrays.asList("maven-hosted", "npm-hosted", "docker-hosted"));

    underTest.update(request);

    // Verify repositoryManager.update was called only for maven and npm, NOT docker
    verify(repositoryManager, times(2)).update(any(Configuration.class));
  }

  private Repository createRepository(String name, String format, String type, boolean isProprietary) {
    Repository repository = mock(Repository.class);
    Configuration configuration = mock(Configuration.class);
    Configuration copiedConfiguration = mock(Configuration.class);
    Format formatObj = mock(Format.class);
    Type typeObj = mock(Type.class);
    NestedAttributesMap componentAttributes = mock(NestedAttributesMap.class);
    NestedAttributesMap copiedComponentAttributes = mock(NestedAttributesMap.class);

    lenient().when(repository.getName()).thenReturn(name);
    lenient().when(repository.getFormat()).thenReturn(formatObj);
    lenient().when(formatObj.getValue()).thenReturn(format);
    lenient().when(repository.getType()).thenReturn(typeObj);
    lenient().when(typeObj.getValue()).thenReturn(type);
    lenient().when(repository.getConfiguration()).thenReturn(configuration);
    lenient().when(configuration.attributes(COMPONENT)).thenReturn(componentAttributes);
    lenient().when(componentAttributes.get(PROPRIETARY_COMPONENTS, Boolean.class)).thenReturn(isProprietary);
    lenient().when(configuration.copy()).thenReturn(copiedConfiguration);
    lenient().when(copiedConfiguration.attributes(COMPONENT)).thenReturn(copiedComponentAttributes);

    return repository;
  }
}

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
package org.sonatype.nexus.coreui.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.app.GlobalComponentLookupHelper;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.coreui.RepositoryReferenceXO;
import org.sonatype.nexus.coreui.RepositoryXO;
import org.sonatype.nexus.extdirect.model.StoreLoadParameters;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Recipe;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.cache.RepositoryCacheInvalidationService;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.config.ConfigurationStore;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.security.RepositoryPermissionChecker;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.scheduling.TaskScheduler;
import org.sonatype.nexus.security.SecurityHelper;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test for {@link RepositoryUiServiceImpl}
 */
public class RepositoryUiServiceTest
    extends TestSupport
{
  @Mock
  private RepositoryCacheInvalidationService repositoryCacheInvalidationService;

  @Mock
  private RepositoryManager repositoryManager;

  @Mock
  private ConfigurationStore configurationStore;

  @Mock
  private SecurityHelper securityHelper;

  @Mock
  private Map<String, Recipe> recipes;

  @Mock
  private TaskScheduler taskScheduler;

  @Mock
  private GlobalComponentLookupHelper typeLookup;

  @Mock
  private List<Format> formats;

  @Mock
  private RepositoryPermissionChecker repositoryPermissionChecker;

  @Mock
  private Format format;

  @Mock
  private EventManager eventManager;

  @Mock
  private Repository repository;

  @Mock
  private RepositoryXO repositoryXO;

  @Mock
  private Configuration configuration;

  private RepositoryUiService underTest;

  @Before
  public void setup() {
    mockRepository();
    when(format.getValue()).thenReturn("format");
    when(repositoryManager.browse()).thenReturn(Collections.singleton(repository));
    when(configurationStore.list()).thenReturn(Collections.singletonList(configuration));
    when(repositoryManager.get(anyString())).thenReturn(repository);
    when(repository.getConfiguration()).thenReturn(configuration);
    when(configuration.copy()).thenReturn(configuration);

    underTest = new RepositoryUiService(repositoryCacheInvalidationService, repositoryManager, configurationStore,
        securityHelper, recipes, taskScheduler, typeLookup, formats, repositoryPermissionChecker)
    {
      @Override
      RepositoryXO asRepository(final Repository input) {
        return repositoryXO;
      }
    };
  }

  @Test
  public void checkUserPermissionsOnFilter() {
    underTest.filter(createParameters());
    verify(repositoryPermissionChecker).userCanBrowseRepositories(configuration);
  }

  @Test
  public void filterForAutocomplete() {
    List<RepositoryReferenceXO> repositories = getTestRepositories();
    StoreLoadParameters storeLoadParameters = createParameters();
    storeLoadParameters.setQuery("nug");
    List<RepositoryReferenceXO> result =
        RepositoryUiService.filterForAutocomplete(storeLoadParameters, repositories);
    assertThat(result, hasSize(2));
    assertThat(result.get(0).getName(), is("nuget-proxy"));
    assertThat(result.get(1).getName(), is("nuget-hosted"));
  }

  @Test
  public void testRoutingRuleSet() throws Exception {
    when(repositoryXO.getName()).thenReturn("test");
    when(repositoryXO.getFormat()).thenReturn("format");

    Map<String, Map<String, Object>> testAttributes = new HashMap<>();
    when(repositoryXO.getOnline()).thenReturn(true);
    when(repositoryXO.getRoutingRuleId()).thenReturn("test");
    when(repositoryXO.getAttributes()).thenReturn(testAttributes);

    underTest.update(repositoryXO);

    verify(configuration).setOnline(true);
    verify(configuration).setRoutingRuleId(any(EntityId.class));
    verify(configuration).setAttributes(testAttributes);
  }

  @Test
  public void testRoutingRuleCleared() throws Exception {
    when(repositoryXO.getName()).thenReturn("test");
    when(repositoryXO.getFormat()).thenReturn("format");

    Map<String, Map<String, Object>> testAttributes = new HashMap<>();
    when(repositoryXO.getOnline()).thenReturn(true);
    when(repositoryXO.getRoutingRuleId()).thenReturn(null);
    when(repositoryXO.getAttributes()).thenReturn(testAttributes);

    underTest.update(repositoryXO);

    verify(configuration).setOnline(true);
    verify(configuration).setRoutingRuleId(null);
    verify(configuration).setAttributes(testAttributes);
  }

  private List<RepositoryReferenceXO> getTestRepositories() {
    RepositoryReferenceXO nugetRepoProxy = mock(RepositoryReferenceXO.class);
    when(nugetRepoProxy.getName()).thenReturn("nuget-proxy");
    RepositoryReferenceXO nugetRepoHosted = mock(RepositoryReferenceXO.class);
    when(nugetRepoHosted.getName()).thenReturn("nuget-hosted");
    RepositoryReferenceXO mavenRepoHosted = mock(RepositoryReferenceXO.class);
    when(mavenRepoHosted.getName()).thenReturn("maven-hosted");
    List<RepositoryReferenceXO> repositories = new ArrayList<>();
    repositories.add(nugetRepoProxy);
    repositories.add(nugetRepoHosted);
    repositories.add(mavenRepoHosted);
    return repositories;
  }

  private void mockRepository() {
    when(repository.getName()).thenReturn("repository");
    when(repository.getType()).thenReturn(new HostedType());
    when(repository.getFormat()).thenReturn(format);
  }

  private static StoreLoadParameters createParameters() {
    StoreLoadParameters params = new StoreLoadParameters();
    params.setFilter(Collections.emptyList());
    return params;
  }
}

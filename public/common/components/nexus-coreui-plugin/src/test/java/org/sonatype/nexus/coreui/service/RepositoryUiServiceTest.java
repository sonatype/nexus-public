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
import java.util.Objects;
import java.util.Optional;

import org.sonatype.nexus.common.QualifierUtil;
import org.sonatype.nexus.common.app.BaseUrlHolder;
import org.sonatype.nexus.common.app.GlobalComponentLookupHelper;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.coreui.RepositoryReferenceXO;
import org.sonatype.nexus.coreui.RepositoryStatusXO;
import org.sonatype.nexus.coreui.RepositoryXO;
import org.sonatype.nexus.extdirect.model.StoreLoadParameters;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Recipe;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.cache.RepositoryCacheInvalidationService;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.config.ConfigurationStore;
import org.sonatype.nexus.repository.config.SimpleConfiguration;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.rest.api.RepositoryMetricsDTO;
import org.sonatype.nexus.repository.rest.api.RepositoryMetricsService;
import org.sonatype.nexus.repository.security.RepositoryPermissionChecker;
import org.sonatype.nexus.common.stateguard.InvalidStateException;
import org.sonatype.nexus.repository.httpclient.HttpClientFacet;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.repository.types.ProxyType;
import org.sonatype.nexus.scheduling.TaskScheduler;
import org.sonatype.nexus.security.SecurityHelper;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension.WithUser;
import org.sonatype.nexus.testcommon.validation.ValidationExtension;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test for {@link RepositoryUiService}
 */
@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
@ExtendWith(ValidationExtension.class)
@ExtendWith(AuthenticationExtension.class)
@WithUser
class RepositoryUiServiceTest
{
  @Mock
  private RepositoryCacheInvalidationService repositoryCacheInvalidationService;

  @Mock
  private RepositoryManager repositoryManager;

  @Mock
  private RepositoryMetricsService repositoryMetricsService;

  @Mock
  private ConfigurationStore configurationStore;

  @Mock
  private SecurityHelper securityHelper;

  @Mock
  private Map<String, Recipe> recipes;

  @Mock
  private Recipe recipe;

  @Mock
  private TaskScheduler taskScheduler;

  @Mock
  private GlobalComponentLookupHelper typeLookup;

  @Mock
  private List<Format> formats;

  @Mock
  private RepositoryPermissionChecker repositoryPermissionChecker;

  @Mock
  private org.sonatype.nexus.repository.manager.internal.FailedRepositoryTracker failedRepositoryTracker;

  @Mock
  private org.sonatype.nexus.repository.manager.internal.HttpAuthenticationSecretEncoder httpAuthenticationSecretEncoder;

  @Mock
  private Format format;

  @Mock
  private Repository repository;

  @Mock
  private RepositoryXO repositoryXO;

  @Mock
  private Configuration configuration;

  private RepositoryUiService underTest;

  private MockedStatic<QualifierUtil> mockedStatic;

  @Mock
  private Map<String, Map<String, Object>> attributes;

  @Mock
  private Map<String, Object> storage;

  @BeforeEach
  void setup() {
    mockedStatic = Mockito.mockStatic(QualifierUtil.class);
    mockRepository();
    mockRecipes();
    BaseUrlHolder.set("http://nexus-url", "");
    when(format.getValue()).thenReturn("format");
    when(repositoryManager.browse()).thenReturn(Collections.singleton(repository));
    when(configurationStore.list()).thenReturn(Collections.singletonList(configuration));
    when(repositoryManager.get(anyString())).thenReturn(repository);
    when(repository.getConfiguration()).thenReturn(configuration);
    when(configuration.copy()).thenReturn(configuration);
    List<Recipe> recipeList = mock(List.class);
    when(QualifierUtil.buildQualifierBeanMap(recipeList)).thenReturn(recipes);

    underTest = Mockito.spy(new RepositoryUiService(repositoryCacheInvalidationService, repositoryManager,
        repositoryMetricsService,
        configurationStore, securityHelper, recipeList, taskScheduler, typeLookup, formats, repositoryPermissionChecker,
        failedRepositoryTracker, httpAuthenticationSecretEncoder)
    {
      @Override
      RepositoryXO asRepository(final Repository input) {
        return repositoryXO;
      }
    });
  }

  @AfterEach
  void teardown() {
    mockedStatic.close();
  }

  @Test
  void checkUserPermissionsOnFilter() {
    underTest.filter(createParameters());
    verify(repositoryPermissionChecker).userCanBrowseRepositories(configuration);
  }

  @Test
  void filterForAutocomplete() {
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
  void testRoutingRuleSet() throws Exception {
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
  void testRoutingRuleCleared() throws Exception {
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

  @Test
  void testPasswordPlaceholderIsRestoredOnUpdate() throws Exception {
    when(repositoryXO.getName()).thenReturn("test");
    when(repositoryXO.getOnline()).thenReturn(true);
    when(repositoryXO.getRoutingRuleId()).thenReturn(null);

    Map<String, Map<String, Object>> testAttributes = new HashMap<>();
    Map<String, Object> httpclient = new HashMap<>();
    Map<String, Object> authentication = new HashMap<>();
    authentication.put("type", "username");
    authentication.put("username", "testuser");
    authentication.put("password", "#~NXRM~PLACEHOLDER~PASSWORD~#");
    httpclient.put("authentication", authentication);
    testAttributes.put("httpclient", httpclient);

    Map<String, Map<String, Object>> storedAttributes = new HashMap<>();
    Map<String, Object> storedHttpclient = new HashMap<>();
    Map<String, Object> storedAuthentication = new HashMap<>();
    storedAuthentication.put("password", "_123");
    storedHttpclient.put("authentication", storedAuthentication);
    storedAttributes.put("httpclient", storedHttpclient);

    when(repositoryXO.getAttributes()).thenReturn(testAttributes);
    when(configuration.getAttributes()).thenReturn(storedAttributes);

    underTest.update(repositoryXO);

    assertThat(authentication.get("password"), is("_123"));
  }

  @Test
  void testBearerTokenPlaceholderIsRestoredOnUpdate() throws Exception {
    when(repositoryXO.getName()).thenReturn("test");
    when(repositoryXO.getOnline()).thenReturn(true);
    when(repositoryXO.getRoutingRuleId()).thenReturn(null);

    Map<String, Map<String, Object>> testAttributes = new HashMap<>();
    Map<String, Object> httpclient = new HashMap<>();
    Map<String, Object> authentication = new HashMap<>();
    authentication.put("type", "bearerToken");
    authentication.put("bearerTokenId", "#~NXRM~PLACEHOLDER~PASSWORD~#");
    httpclient.put("authentication", authentication);
    testAttributes.put("httpclient", httpclient);

    Map<String, Map<String, Object>> storedAttributes = new HashMap<>();
    Map<String, Object> storedHttpclient = new HashMap<>();
    Map<String, Object> storedAuthentication = new HashMap<>();
    storedAuthentication.put("bearerTokenId", "_456");
    storedHttpclient.put("authentication", storedAuthentication);
    storedAttributes.put("httpclient", storedHttpclient);

    when(repositoryXO.getAttributes()).thenReturn(testAttributes);
    when(configuration.getAttributes()).thenReturn(storedAttributes);

    underTest.update(repositoryXO);

    assertThat(authentication.get("bearerTokenId"), is("_456"));
  }

  @Test
  void testNewPasswordIsNotReplacedOnUpdate() throws Exception {
    when(repositoryXO.getName()).thenReturn("test");
    when(repositoryXO.getOnline()).thenReturn(true);
    when(repositoryXO.getRoutingRuleId()).thenReturn(null);

    Map<String, Map<String, Object>> testAttributes = new HashMap<>();
    Map<String, Object> httpclient = new HashMap<>();
    Map<String, Object> authentication = new HashMap<>();
    authentication.put("type", "username");
    authentication.put("username", "testuser");
    authentication.put("password", "newPassword123");
    httpclient.put("authentication", authentication);
    testAttributes.put("httpclient", httpclient);

    when(repositoryXO.getAttributes()).thenReturn(testAttributes);

    underTest.update(repositoryXO);

    assertThat(authentication.get("password"), is("newPassword123"));
  }

  @Test
  void testNewBearerTokenIsNotReplacedOnUpdate() throws Exception {
    when(repositoryXO.getName()).thenReturn("test");
    when(repositoryXO.getOnline()).thenReturn(true);
    when(repositoryXO.getRoutingRuleId()).thenReturn(null);

    Map<String, Map<String, Object>> testAttributes = new HashMap<>();
    Map<String, Object> httpclient = new HashMap<>();
    Map<String, Object> authentication = new HashMap<>();
    authentication.put("type", "bearerToken");
    authentication.put("bearerTokenId", "ghp_newToken123456");
    httpclient.put("authentication", authentication);
    testAttributes.put("httpclient", httpclient);

    when(repositoryXO.getAttributes()).thenReturn(testAttributes);

    underTest.update(repositoryXO);

    assertThat(authentication.get("bearerTokenId"), is("ghp_newToken123456"));
  }

  @Test
  void testBearerTokenPlaceholderFallsBackToOldKeyForPreMigrationRepositories() throws Exception {
    when(repositoryXO.getName()).thenReturn("test");
    when(repositoryXO.getOnline()).thenReturn(true);
    when(repositoryXO.getRoutingRuleId()).thenReturn(null);

    // UI sends bearerTokenId with placeholder (because filterAttributes replaces with placeholder)
    Map<String, Map<String, Object>> testAttributes = new HashMap<>();
    Map<String, Object> httpclient = new HashMap<>();
    Map<String, Object> authentication = new HashMap<>();
    authentication.put("type", "bearerToken");
    authentication.put("bearerTokenId", "#~NXRM~PLACEHOLDER~PASSWORD~#");
    httpclient.put("authentication", authentication);
    testAttributes.put("httpclient", httpclient);

    // Pre-migration repository only has the old bearerToken key
    Map<String, Map<String, Object>> storedAttributes = new HashMap<>();
    Map<String, Object> storedHttpclient = new HashMap<>();
    Map<String, Object> storedAuthentication = new HashMap<>();
    storedAuthentication.put("bearerToken", "_789_old_token");
    storedHttpclient.put("authentication", storedAuthentication);
    storedAttributes.put("httpclient", storedHttpclient);

    when(repositoryXO.getAttributes()).thenReturn(testAttributes);
    when(configuration.getAttributes()).thenReturn(storedAttributes);

    underTest.update(repositoryXO);

    // Should fallback to the old bearerToken value
    assertThat(authentication.get("bearerTokenId"), is("_789_old_token"));
  }

  @Test
  void testReadContainsRepoSize() {
    String repoName = "testRepo";
    String recipeName = "testRecipe";
    Long repoSize = 123456L;
    RepositoryMetricsDTO repoMetrics = new RepositoryMetricsDTO(repoName, repoSize);

    when(configuration.getRecipeName()).thenReturn(recipeName);
    when(repositoryMetricsService.get(anyString())).thenReturn(Optional.of(repoMetrics));
    when(configuration.getRepositoryName()).thenReturn(repoName);
    when(repositoryPermissionChecker.userHasRepositoryAdminPermissionFor(any(Iterable.class), anyString()))
        .thenReturn(Collections.singletonList(configuration));
    List<RepositoryXO> repos = underTest.read();
    assertEquals(1, repos.size());
    RepositoryXO repoXo = repos.get(0);
    assertEquals(repoName, repoXo.getName());
    assertEquals(Long.valueOf(123456), repoXo.getSize());
    assertEquals("maven2", repoXo.getFormat());
    assertEquals("hosted", repoXo.getType());
  }

  @Test
  void testReadReferencesContainsExpectedInfo() {
    List<Configuration> repoConfigurations = givenRepoConfigurations();
    StoreLoadParameters parameters = createParameters();
    doReturn(repoConfigurations).when(underTest).filter(parameters);

    List<RepositoryReferenceXO> result = underTest.readReferences(parameters);

    assertThat(result, hasSize(repoConfigurations.size()));

    for (Configuration repoConfiguration : repoConfigurations) {
      RepositoryReferenceXO reference = result.stream()
          .filter(value -> Objects.equals(repoConfiguration.getRepositoryName(), value.getName()))
          .findAny()
          .orElseThrow(() -> new AssertionError(
              String.format("Repository %s not found in results", repoConfiguration.getRepositoryName())));
      Recipe recipe = recipes.get(repoConfiguration.getRecipeName());

      assertThat(reference.getId(), is(repoConfiguration.getRepositoryName()));
      assertThat(reference.getFormat(), is(recipe.getFormat().getValue()));
      assertThat(reference.getBlobStoreName(), is(getAttribute(repoConfiguration, "storage.blobStoreName")));
      assertThat(reference.getVersionPolicy(), is(getAttribute(repoConfiguration, "maven.versionPolicy")));
      assertThat(reference.getType(), is(recipe.getType().getValue()));
      assertThat(reference.getUrl(),
          is(String.format("%s/repository/%s/", BaseUrlHolder.get(), repoConfiguration.getRepositoryName())));
      assertThat(reference.getStatus().getRepositoryName(), is(repoConfiguration.getRepositoryName()));
      assertThat(reference.getStatus().isOnline(), is(repoConfiguration.isOnline()));
    }
  }

  private List<Configuration> givenRepoConfigurations() {
    return List.of(
        givenRepoConfiguration("repo1", "raw", "rawRecipe", "blobStore1", "strict", "testType", true),
        givenRepoConfiguration("repo2", "maven", "mavenRecipe", "blobStore2", "none", "otherType", false));
  }

  private Configuration givenRepoConfiguration(
      final String repoName,
      final String formatValue,
      final String recipeName,
      final String blobStoreName,
      final String versionPolicy,
      final String typeValue,
      final boolean isOnline)
  {
    Configuration c = new SimpleConfiguration();
    Recipe recipe = Mockito.mock(Recipe.class);
    Type type = Mockito.mock(Type.class);
    Format format = Mockito.mock(Format.class);

    c.setRepositoryName(repoName);
    c.setRecipeName(recipeName);
    c.setOnline(isOnline);
    c.setAttributes(ImmutableMap.of(
        "storage", ImmutableMap.of("blobStoreName", blobStoreName),
        "maven", ImmutableMap.of("versionPolicy", versionPolicy)));

    doReturn(type).when(recipe).getType();
    doReturn(typeValue).when(type).getValue();
    doReturn(format).when(recipe).getFormat();
    doReturn(formatValue).when(format).getValue();

    underTest.addRecipe(recipeName, recipe);
    recipes.put(recipeName, recipe);

    return c;
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

  private void mockRecipes() {
    when(recipe.getType()).thenReturn(new HostedType());
    when(recipe.getFormat()).thenReturn(new Format("maven2")
    {
    });
    recipes = new HashMap<>();
    recipes.put("testRecipe", recipe);
  }

  private static StoreLoadParameters createParameters() {
    StoreLoadParameters params = new StoreLoadParameters();
    params.setFilter(Collections.emptyList());
    return params;
  }

  private static Object getAttribute(final Configuration repository, final String path) {
    Object currentValue = repository.getAttributes();
    String[] parts = path.split("\\.");

    for (String part : parts) {
      if (currentValue instanceof Map) {
        currentValue = ((Map<?, ?>) currentValue).get(part);
      }
      else {
        throw new IllegalArgumentException();
      }
    }

    return currentValue;
  }

  @Test
  void testUpdateFailedRepository_successfulRecovery() throws Exception {
    // Setup: Repository has failed and is being updated
    String repoName = "failed-repo";
    Type type = mock(Type.class);
    when(repositoryXO.getName()).thenReturn(repoName);
    when(repositoryXO.getFormat()).thenReturn("format");
    when(repositoryXO.getOnline()).thenReturn(true);
    when(repositoryXO.getRoutingRuleId()).thenReturn(null);

    Map<String, Map<String, Object>> testAttributes = new HashMap<>();
    when(repositoryXO.getAttributes()).thenReturn(testAttributes);

    // Mock configuration
    Configuration storedConfiguration = mock(Configuration.class);
    when(storedConfiguration.getRepositoryName()).thenReturn(repoName);
    when(storedConfiguration.getRecipeName()).thenReturn("mockRecipe");
    when(storedConfiguration.getAttributes()).thenReturn(new HashMap<>());
    when(storedConfiguration.copy()).thenReturn(storedConfiguration);
    when(storedConfiguration.isOnline()).thenReturn(true);

    // Mock configuration store
    when(configurationStore.list()).thenReturn(Collections.singletonList(storedConfiguration));

    // Setup recipe for QualifierUtil
    Recipe testRecipe = mock(Recipe.class);
    when(testRecipe.getFormat()).thenReturn(format);
    when(testRecipe.getType()).thenReturn(type);
    when(format.getValue()).thenReturn("format");

    // Create recipe list and mock QualifierUtil to return recipe map
    List<Recipe> testRecipeList = Collections.singletonList(testRecipe);
    Map<String, Recipe> testRecipesMap = new HashMap<>();
    testRecipesMap.put("mockRecipe", testRecipe);
    when(QualifierUtil.buildQualifierBeanMap(testRecipeList)).thenReturn(testRecipesMap);

    // Mock failed repository tracker
    when(failedRepositoryTracker.hasFailed(repoName)).thenReturn(true);

    // Mock BaseRepositoryManager with successful retry
    org.sonatype.nexus.repository.manager.internal.BaseRepositoryManager baseRepositoryManager =
        mock(org.sonatype.nexus.repository.manager.internal.BaseRepositoryManager.class);
    Repository recoveredRepo = mock(Repository.class);
    when(recoveredRepo.getName()).thenReturn(repoName);
    when(recoveredRepo.getType()).thenReturn(type);
    when(recoveredRepo.getFormat()).thenReturn(format);
    when(recoveredRepo.getConfiguration()).thenReturn(storedConfiguration);
    when(baseRepositoryManager.retryFailedRepository(repoName)).thenReturn(Optional.of(recoveredRepo));

    // Use real RepositoryUiService instance for this test
    RepositoryUiService testService = new RepositoryUiService(repositoryCacheInvalidationService,
        baseRepositoryManager, repositoryMetricsService, configurationStore, securityHelper,
        testRecipeList, taskScheduler, typeLookup, formats, repositoryPermissionChecker,
        failedRepositoryTracker, httpAuthenticationSecretEncoder);

    // Execute: Update the failed repository
    RepositoryXO result = testService.update(repositoryXO);

    // Verify: Configuration was updated
    verify(storedConfiguration).setOnline(true);
    verify(storedConfiguration).setAttributes(testAttributes);
    verify(configurationStore).update(storedConfiguration);

    // Verify: Retry was attempted
    verify(baseRepositoryManager).retryFailedRepository(repoName);
  }

  @Test
  void testUpdateFailedRepository_stillFailing() throws Exception {
    // Setup: Repository has failed and update doesn't fix it
    String repoName = "failed-repo";
    Type type = mock(Type.class);
    when(repositoryXO.getName()).thenReturn(repoName);
    when(repositoryXO.getFormat()).thenReturn("format");
    when(repositoryXO.getOnline()).thenReturn(false);
    when(repositoryXO.getRoutingRuleId()).thenReturn(null);

    Map<String, Map<String, Object>> testAttributes = new HashMap<>();
    when(repositoryXO.getAttributes()).thenReturn(testAttributes);

    // Mock configuration
    Configuration storedConfiguration = mock(Configuration.class);
    when(storedConfiguration.getRepositoryName()).thenReturn(repoName);
    when(storedConfiguration.getRecipeName()).thenReturn("mockRecipe");
    when(storedConfiguration.getAttributes()).thenReturn(new HashMap<>());
    when(storedConfiguration.copy()).thenReturn(storedConfiguration);
    when(storedConfiguration.isOnline()).thenReturn(false);

    // Mock configuration store
    when(configurationStore.list()).thenReturn(Collections.singletonList(storedConfiguration));

    // Setup recipe for QualifierUtil
    Recipe testRecipe = mock(Recipe.class);
    when(testRecipe.getFormat()).thenReturn(format);
    when(testRecipe.getType()).thenReturn(type);
    when(format.getValue()).thenReturn("format");

    // Create recipe list and mock QualifierUtil to return recipe map
    List<Recipe> testRecipeList = Collections.singletonList(testRecipe);
    Map<String, Recipe> testRecipesMap = new HashMap<>();
    testRecipesMap.put("mockRecipe", testRecipe);
    when(QualifierUtil.buildQualifierBeanMap(testRecipeList)).thenReturn(testRecipesMap);

    // Mock failed repository tracker
    when(failedRepositoryTracker.hasFailed(repoName)).thenReturn(true);

    // Mock BaseRepositoryManager with failed retry (returns empty)
    org.sonatype.nexus.repository.manager.internal.BaseRepositoryManager baseRepositoryManager =
        mock(org.sonatype.nexus.repository.manager.internal.BaseRepositoryManager.class);
    when(baseRepositoryManager.retryFailedRepository(repoName)).thenReturn(Optional.empty());

    // Use real RepositoryUiService instance for this test
    RepositoryUiService testService = new RepositoryUiService(repositoryCacheInvalidationService,
        baseRepositoryManager, repositoryMetricsService, configurationStore, securityHelper,
        testRecipeList, taskScheduler, typeLookup, formats, repositoryPermissionChecker,
        failedRepositoryTracker, httpAuthenticationSecretEncoder);

    // Execute: Update the failed repository
    RepositoryXO result = testService.update(repositoryXO);

    // Verify: Configuration was updated
    verify(storedConfiguration).setOnline(false);
    verify(storedConfiguration).setAttributes(testAttributes);
    verify(configurationStore).update(storedConfiguration);

    // Verify: Retry was attempted but failed
    verify(baseRepositoryManager).retryFailedRepository(repoName);
  }

  @Test
  void testBuildStatusForConfigurationHandlesInvalidStateException() throws Exception {
    when(recipe.getType()).thenReturn(new ProxyType());
    when(configuration.getRecipeName()).thenReturn("testRecipe");
    when(configuration.getRepositoryName()).thenReturn("proxy-repo");
    when(configuration.isOnline()).thenReturn(true);
    when(failedRepositoryTracker.getFailure(anyString())).thenReturn(Optional.empty());
    when(repositoryPermissionChecker.userHasRepositoryAdminPermissionFor(any(Iterable.class), anyString()))
        .thenReturn(Collections.singletonList(configuration));
    HttpClientFacet httpClientFacet = mock(HttpClientFacet.class);
    doReturn(httpClientFacet).when(repository).facet(HttpClientFacet.class);
    when(httpClientFacet.getStatus()).thenThrow(new InvalidStateException("STOPPED", new String[]{"STARTED"}));

    List<RepositoryStatusXO> result = underTest.readStatus(Collections.emptyMap());

    assertThat(result, hasSize(1));
    assertThat(result.get(0).getRepositoryName(), is("proxy-repo"));
    assertThat(result.get(0).isOnline(), is(true));
  }

  @Test
  void testBuildStatusForRepositoryHandlesInvalidStateException() throws Exception {
    List<Recipe> freshRecipeList = Collections.singletonList(recipe);
    when(QualifierUtil.buildQualifierBeanMap(freshRecipeList)).thenReturn(recipes);
    RepositoryUiService service = new RepositoryUiService(repositoryCacheInvalidationService, repositoryManager,
        repositoryMetricsService, configurationStore, securityHelper, freshRecipeList, taskScheduler,
        typeLookup, formats, repositoryPermissionChecker, failedRepositoryTracker,
        httpAuthenticationSecretEncoder);
    when(repository.getType()).thenReturn(new ProxyType());
    when(configuration.isOnline()).thenReturn(true);
    HttpClientFacet httpClientFacet = mock(HttpClientFacet.class);
    doReturn(httpClientFacet).when(repository).facet(HttpClientFacet.class);
    when(httpClientFacet.getStatus()).thenThrow(new InvalidStateException("STOPPED", new String[]{"STARTED"}));

    RepositoryXO result = service.asRepository(repository);

    assertThat(result.getName(), is("repository"));
    assertThat(result.getStatus().getRepositoryName(), is("repository"));
  }

  @Test
  void testUpdateFailedRepository_nullRecipe() throws Exception {
    String repoName = "failed-repo";

    // Setup: Configuration with a recipe name that doesn't exist in recipes map
    Configuration config = mock(Configuration.class);
    when(config.getRepositoryName()).thenReturn(repoName);
    when(config.getRecipeName()).thenReturn("nonexistent-recipe");
    when(config.copy()).thenReturn(config);
    when(config.getAttributes()).thenReturn(attributes);

    when(configurationStore.list()).thenReturn(Collections.singletonList(config));
    when(failedRepositoryTracker.hasFailed(repoName.toLowerCase())).thenReturn(true);

    // Note: recipes HashMap naturally returns null for "nonexistent-recipe" since mockRecipes() only adds "testRecipe"

    // Create a minimal RepositoryXO
    RepositoryXO xo = new RepositoryXO();
    xo.setName(repoName);
    xo.setOnline(true);
    xo.setAttributes(new HashMap<>());

    // Execute: Should throw BadRequestException for null recipe
    try {
      underTest.update(xo);
      throw new AssertionError("Expected BadRequestException for null recipe");
    }
    catch (org.sonatype.nexus.repository.BadRequestException e) {
      assertThat(e.getMessage(), is("Recipe not found for repository: failed-repo"));
    }
  }
}

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
import org.sonatype.nexus.common.stateguard.InvalidStateException;
import org.sonatype.nexus.coreui.RepositoryReferenceXO;
import org.sonatype.nexus.coreui.RepositoryStatusXO;
import org.sonatype.nexus.coreui.RepositoryXO;
import org.sonatype.nexus.extdirect.model.StoreLoadParameters;
import org.sonatype.nexus.repository.BadRequestException;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Recipe;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.cache.RepositoryCacheInvalidationService;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.config.ConfigurationStore;
import org.sonatype.nexus.repository.config.SimpleConfiguration;
import org.sonatype.nexus.repository.config.UniqueRepositoryNameValidator;
import org.sonatype.nexus.repository.httpclient.HttpClientFacet;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.manager.internal.BaseRepositoryManager;
import org.sonatype.nexus.repository.manager.internal.FailedRepositoryTracker;
import org.sonatype.nexus.repository.rest.api.RepositoryMetricsDTO;
import org.sonatype.nexus.repository.rest.api.RepositoryMetricsService;
import org.sonatype.nexus.repository.security.RepositoryAdminPermission;
import org.sonatype.nexus.repository.security.RepositoryPermissionChecker;
import org.sonatype.nexus.repository.types.GroupType;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.repository.types.ProxyType;
import org.sonatype.nexus.scheduling.TaskScheduler;
import org.sonatype.nexus.security.SecurityHelper;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension.WithUser;
import org.sonatype.nexus.testcommon.validation.ValidationExtension;
import org.sonatype.nexus.testcommon.validation.ValidationExtension.RegisterValidator;

import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.Permission;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test for {@link RepositoryUiService}
 */
@ExtendWith({MockitoExtension.class, ValidationExtension.class, AuthenticationExtension.class})
@WithUser
class RepositoryUiServiceTest
{
  private static final String REMOTE_URL_CHANGE_ERROR_MESSAGE =
      "Remote URL has changed. " +
          "Authentication credentials have been reset and must be re-entered.";

  @RegisterValidator
  @Mock
  UniqueRepositoryNameValidator nameValidator;

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
  private Configuration configuration;

  private RepositoryXO repositoryXO = repositoryXO();

  private MockedStatic<QualifierUtil> mockedStatic;

  private RepositoryUiService underTest;

  @BeforeEach
  void setup() {
    lenient().when(nameValidator.isValid(any(), any())).thenReturn(true);
    mockedStatic = mockStatic(QualifierUtil.class);
    mockRepository();
    mockRecipes();
    BaseUrlHolder.set("http://nexus-url", "");
    lenient().when(format.getValue()).thenReturn("format");
    lenient().when(repositoryManager.browse()).thenReturn(List.of(repository));
    lenient().when(configurationStore.list()).thenReturn(List.of(configuration));
    lenient().when(repositoryManager.get(anyString())).thenReturn(repository);
    lenient().when(repository.getConfiguration()).thenReturn(configuration);
    lenient().when(configuration.copy()).thenReturn(configuration);
    List<Recipe> recipeList = mock();
    when(QualifierUtil.buildQualifierBeanMap(recipeList)).thenReturn(recipes);
    // read() resolves sizes through RepositoryMetricsService.listByName(); run the real default so
    // per-test list() stubs continue to drive it (NEXUS-53722).
    lenient().when(repositoryMetricsService.listByName()).thenCallRealMethod();

    underTest = spy(new RepositoryUiService(repositoryCacheInvalidationService, repositoryManager,
        repositoryMetricsService, configurationStore, securityHelper, recipeList, taskScheduler, typeLookup, formats,
        repositoryPermissionChecker, failedRepositoryTracker, httpAuthenticationSecretEncoder)
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
    BaseUrlHolder.unset();
  }

  @Test
  void checkUserPermissionsOnFilter() {
    underTest.filter(createParameters());
    verify(repositoryPermissionChecker).userCanBrowseRepositories(any(Configuration[].class));
  }

  @Test
  void create_deniedWhenUserLacksPermissionForRecipeFormat() throws Exception {
    // Given: securityHelper rejects a permission check for the recipe's actual format
    doThrow(new AuthorizationException("Unauthorized"))
        .when(securityHelper)
        .ensurePermitted(any(Permission[].class));

    assertThrows(AuthorizationException.class, () -> underTest.create(repositoryXO));
  }

  @Test
  void create_permissionCheckedAgainstRecipeFormat_notClientSuppliedFormat() throws Exception {
    // Given: a user who supplies format=raw (their permitted format) but recipe=testRecipe (which is maven2)
    repositoryXO("my-repo", "raw", // attacker-controlled DTO field
        "testRecipe" // testRecipe resolves to maven2 via mockRecipes()
    );

    when(repositoryManager.newConfiguration()).thenReturn(configuration);
    when(repositoryManager.create(configuration)).thenReturn(repository);

    underTest.create(repositoryXO);

    // The permission check must use the format from the recipe registry ("maven2"), not the DTO field ("raw")
    ArgumentCaptor<Permission[]> permCaptor = ArgumentCaptor.forClass(Permission[].class);
    verify(securityHelper).ensurePermitted(permCaptor.capture());
    Permission[] captured = permCaptor.getValue();
    assertThat(captured.length, is(1));
    assertThat(captured[0] instanceof RepositoryAdminPermission, is(true));
    assertThat(((RepositoryAdminPermission) captured[0]).getFormat(), is("maven2"));
  }

  @Test
  void create_throwsBadRequestForUnknownRecipe() throws Exception {
    // Given: the recipe name in the DTO does not exist in the registry
    repositoryXO.setRecipe("nonexistent-recipe");

    BadRequestException ex = assertThrows(BadRequestException.class, () -> underTest.create(repositoryXO));
    assertThat(ex.getMessage(), is("Recipe not found: nonexistent-recipe"));
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
  void testBearerTokenPlaceholderFallsBackToOldKeyForPreMigrationRepositories() throws Exception {
    repositoryXO = repositoryXO("test", "format", "recipe");

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

    repositoryXO.setAttributes(testAttributes);
    when(configuration.getAttributes()).thenReturn(storedAttributes);

    underTest.update(repositoryXO);

    // Should fallback to the old bearerToken value
    assertThat(authentication.get("bearerTokenId"), is("_789_old_token"));
  }

  @Test
  void testBearerTokenPlaceholderIsRestoredOnUpdate() throws Exception {
    repositoryXO = repositoryXO("test", "format", "recipe");

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

    repositoryXO.setAttributes(testAttributes);
    when(configuration.getAttributes()).thenReturn(storedAttributes);

    underTest.update(repositoryXO);

    assertThat(authentication.get("bearerTokenId"), is("_456"));
  }

  /**
   * Test: buildStatus() returns proper status for failed repository
   * Tests via readStatus public API
   */
  @Test
  void testBuildStatus_returnsFailedStatusForFailedRepository() {
    when(configuration.getRepositoryName()).thenReturn("failed-repo");

    FailedRepositoryTracker.RepositoryFailure failure =
        mock(org.sonatype.nexus.repository.manager.internal.FailedRepositoryTracker.RepositoryFailure.class);
    when(failure.getReason()).thenReturn("Recipe not found");
    when(failedRepositoryTracker.getFailure("failed-repo")).thenReturn(Optional.of(failure));
    when(repositoryPermissionChecker.userHasRepositoryAdminPermissionFor(any(Iterable.class), anyString()))
        .thenReturn(List.of(configuration));

    // Execute via readStatus public API
    Map<String, String> params = new HashMap<>();
    List<RepositoryStatusXO> result = underTest.readStatus(params);

    // Verify: Status shows failed state
    assertThat(result.size(), is(1));
    RepositoryStatusXO status = result.get(0);
    assertThat(status.getRepositoryName(), is("failed-repo"));
    assertThat(status.isOnline(), is(false));
    assertThat(status.getDescription(), is("Failed to Initialize"));
    assertThat(status.getReason(), is("Recipe not found"));
  }

  @Test
  void testBuildStatusForConfigurationHandlesInvalidStateException() throws Exception {
    when(recipe.getType()).thenReturn(new ProxyType());
    when(configuration.getRecipeName()).thenReturn("testRecipe");
    when(configuration.getRepositoryName()).thenReturn("proxy-repo");
    when(configuration.isOnline()).thenReturn(true);
    when(failedRepositoryTracker.getFailure(anyString())).thenReturn(Optional.empty());
    when(repositoryPermissionChecker.userHasRepositoryAdminPermissionFor(any(Iterable.class), anyString()))
        .thenReturn(List.of(configuration));
    HttpClientFacet httpClientFacet = mock(HttpClientFacet.class);
    doReturn(httpClientFacet).when(repository).facet(HttpClientFacet.class);
    when(httpClientFacet.getStatus()).thenThrow(new InvalidStateException("STOPPED", new String[]{"STARTED"}));

    List<RepositoryStatusXO> result = underTest.readStatus(Map.of());

    assertThat(result, hasSize(1));
    assertThat(result.get(0).getRepositoryName(), is("proxy-repo"));
    assertThat(result.get(0).isOnline(), is(true));
  }

  @Test
  void testBuildStatusForRepositoryHandlesInvalidStateException() throws Exception {
    List<Recipe> freshRecipeList = List.of(recipe);
    mockedStatic.when(() -> QualifierUtil.buildQualifierBeanMap(freshRecipeList)).thenReturn(recipes);
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

  /**
   * Test: buildStatus() handles configuration with missing recipe gracefully via readReferences
   * Tests private method indirectly through public API
   */
  @Test
  void testBuildStatusHandlesMissingRecipeGracefully() {
    Configuration config = invalidConfig();

    when(configurationStore.list()).thenReturn(List.of(config));
    // Should return empty list since the config with missing recipe is filtered out before this call
    when(repositoryPermissionChecker.userCanBrowseRepositories(any(Configuration[].class)))
        .thenReturn(List.of());

    // Execute: Should NOT throw NPE, config should be filtered out by filter()
    // This tests both filter() and buildStatus() handling of missing recipes
    List<RepositoryReferenceXO> result = underTest.readReferences(params("format", "maven2"));

    // Verify: Empty result because missing recipe config is filtered out
    assertThat(result.size(), is(0));
  }

  // NEXUS-52779: ExtDirect create must derive format from the recipe registry, not the caller-supplied DTO field

  /**
   * Test: All configurations have missing recipes (edge case)
   */
  @Test
  void testFilterAllConfigurationsHaveMissingRecipes() {
    Configuration invalid2 = mock();
    when(invalid2.getRecipeName()).thenReturn("missing-recipe-2");

    List<Configuration> allConfigs = new ArrayList<>();
    allConfigs.add(invalidConfig());
    allConfigs.add(invalid2);

    when(configurationStore.list()).thenReturn(allConfigs);

    Iterable<Configuration> result = underTest.filter(params("format", "maven2"));

    List<Configuration> resultList = new ArrayList<>();
    result.forEach(resultList::add);
    assertThat(resultList.size(), is(0));
  }

  /**
   * Test: filter() excludes configurations with missing recipes
   */
  @Test
  void testFilterConfigurationsWithMissingRecipes() {
    Configuration validConfig = validConfig();

    List<Configuration> allConfigs = new ArrayList<>();
    allConfigs.add(validConfig);
    allConfigs.add(invalidConfig());

    when(configurationStore.list()).thenReturn(allConfigs);
    when(repositoryPermissionChecker.userCanBrowseRepositories(any(Configuration[].class)))
        .thenReturn(List.of(validConfig));

    Iterable<Configuration> result = underTest.filter(null);

    List<Configuration> resultList = new ArrayList<>();
    result.forEach(resultList::add);
    assertThat(resultList.size(), is(1));
    assertThat(resultList.get(0).getRepositoryName(), is("valid-repo"));
  }

  /**
   * Test: filter() with multiple format filters
   */
  @Test
  void testFilterMultipleFormatsConfigurationsWithMissingRecipes() {
    Configuration mavenConfig = mock();
    when(mavenConfig.getRepositoryName()).thenReturn("maven-repo");
    when(mavenConfig.getRecipeName()).thenReturn("testRecipe");

    Configuration nugetConfig = mock();
    when(nugetConfig.getRecipeName()).thenReturn("nuget-recipe");

    List<Configuration> allConfigs = new ArrayList<>();
    allConfigs.add(mavenConfig);
    allConfigs.add(invalidConfig());
    allConfigs.add(nugetConfig);

    when(configurationStore.list()).thenReturn(allConfigs);
    when(repositoryPermissionChecker.userCanBrowseRepositories(any(Configuration[].class)))
        .thenReturn(List.of(mavenConfig));

    Iterable<Configuration> result = underTest.filter(params("format", "maven2,nuget"));

    List<Configuration> resultList = new ArrayList<>();
    result.forEach(resultList::add);
    assertThat(resultList.size(), is(1)); // Only maven, nuget recipe doesn't exist
    assertThat(resultList.get(0).getRepositoryName(), is("maven-repo"));
  }

  /**
   * Test: filter() with type filter excludes configurations with missing recipes
   */
  @Test
  void testFilterTypeFilterConfigurationsWithMissingRecipes() {
    Configuration valid = validConfig();
    List<Configuration> allConfigs = List.of(valid, invalidConfig());

    when(configurationStore.list()).thenReturn(allConfigs);
    when(repositoryPermissionChecker.userCanBrowseRepositories(any(Configuration[].class)))
        .thenReturn(List.of(valid));

    Iterable<Configuration> result = underTest.filter(params("type", "hosted"));

    List<Configuration> resultList = new ArrayList<>();
    result.forEach(resultList::add);
    assertThat(resultList.size(), is(1));
    assertThat(resultList.get(0).getRepositoryName(), is("valid-repo"));
  }

  /**
   * Test: filter() with format parameter excludes configurations with missing recipes
   */
  @Test
  void testFilterWithFormatFilterConfigurationsWithMissingRecipes() {
    Configuration validConfig = validConfig();

    List<Configuration> allConfigs = new ArrayList<>();
    allConfigs.add(validConfig);
    allConfigs.add(invalidConfig());

    when(configurationStore.list()).thenReturn(allConfigs);
    when(repositoryPermissionChecker.userCanBrowseRepositories(any(Configuration[].class)))
        .thenReturn(List.of(validConfig));

    Iterable<Configuration> result = underTest.filter(params("format", "maven2"));

    // Verify: Only valid repository is returned
    List<Configuration> resultList = new ArrayList<>();
    result.forEach(resultList::add);
    assertThat(resultList.size(), is(1));
    assertThat(resultList.get(0).getRepositoryName(), is("valid-repo"));
  }

  /**
   * Test: getFormat() and getType() handle missing recipes gracefully via readReferences
   * Tests private methods indirectly through public API
   */
  @Test
  void testGetFormatAndTypeHandleMissingRecipesGracefully() {
    Configuration validConfig = validConfig();
    when(validConfig.getAttributes()).thenReturn(attributes());

    List<Configuration> allConfigs = new ArrayList<>();
    allConfigs.add(validConfig);
    allConfigs.add(invalidConfig());

    when(configurationStore.list()).thenReturn(allConfigs);
    when(repositoryPermissionChecker.userCanBrowseRepositories(any(Configuration[].class)))
        .thenReturn(List.of(validConfig)); // Only valid config passes permission check

    StoreLoadParameters params = new StoreLoadParameters();
    params.setFilter(List.of());

    // Execute: Should NOT throw NPE, invalid config should be filtered out
    List<RepositoryReferenceXO> result = underTest.readReferences(params);

    // Verify: Only valid repository is returned with correct format and type
    assertThat(result.size(), is(1));
    RepositoryReferenceXO ref = result.get(0);
    assertThat(ref.getName(), is("valid-repo"));
    assertThat(ref.getFormat(), is("maven2")); // getFormat returned correctly
    assertThat(ref.getType(), is("hosted")); // getType returned correctly
  }

  @Test
  void testIsRemoteUrlChangedDetectsChange() {
    Map<String, Map<String, Object>> oldAttrs = Map.of("proxy", Map.of("remoteUrl", "https://old.example.com/v1/"));
    Map<String, Map<String, Object>> newAttrs = Map.of("proxy", Map.of("remoteUrl", "https://new.example.com/v1/"));

    assertThat(RepositoryUiService.isRemoteUrlChanged(oldAttrs, newAttrs), is(true));
  }

  @Test
  void testIsRemoteUrlChangedDifferentPath() {
    Map<String, Map<String, Object>> oldAttrs = Map.of("proxy", Map.of("remoteUrl", "https://example.com/v1/"));
    Map<String, Map<String, Object>> newAttrs = Map.of("proxy", Map.of("remoteUrl", "https://example.com/v2/"));

    assertThat(RepositoryUiService.isRemoteUrlChanged(oldAttrs, newAttrs), is(true));
  }

  @Test
  void testIsRemoteUrlChangedNoProxyAttributes() {
    Map<String, Map<String, Object>> oldAttrs = Map.of("storage", Map.of("blobStoreName", "default"));
    Map<String, Map<String, Object>> newAttrs = Map.of("storage", Map.of("blobStoreName", "default"));

    assertThat(RepositoryUiService.isRemoteUrlChanged(oldAttrs, newAttrs), is(false));
  }

  @Test
  void testIsRemoteUrlChangedSameUrl() {
    Map<String, Map<String, Object>> oldAttrs = Map.of("proxy", Map.of("remoteUrl", "https://example.com/v1/"));
    Map<String, Map<String, Object>> newAttrs = Map.of("proxy", Map.of("remoteUrl", "https://example.com/v1/"));

    assertThat(RepositoryUiService.isRemoteUrlChanged(oldAttrs, newAttrs), is(false));
  }

  @Test
  void testNewBearerTokenIsNotReplacedOnUpdate() throws Exception {
    repositoryXO = repositoryXO("test", "format", "recipe");

    Map<String, Map<String, Object>> testAttributes = new HashMap<>();
    Map<String, Object> httpclient = new HashMap<>();
    Map<String, Object> authentication = new HashMap<>();
    authentication.put("type", "bearerToken");
    authentication.put("bearerTokenId", "ghp_newToken123456");
    httpclient.put("authentication", authentication);
    testAttributes.put("httpclient", httpclient);

    repositoryXO.setAttributes(testAttributes);

    underTest.update(repositoryXO);

    assertThat(authentication.get("bearerTokenId"), is("ghp_newToken123456"));
  }

  @Test
  void testNewPasswordIsNotReplacedOnUpdate() throws Exception {
    repositoryXO = repositoryXO("test", "format", "recipe");

    Map<String, Map<String, Object>> testAttributes = new HashMap<>();
    Map<String, Object> httpclient = new HashMap<>();
    Map<String, Object> authentication = new HashMap<>();
    authentication.put("type", "username");
    authentication.put("username", "testuser");
    authentication.put("password", "newPassword123");
    httpclient.put("authentication", authentication);
    testAttributes.put("httpclient", httpclient);

    repositoryXO.setAttributes(testAttributes);

    underTest.update(repositoryXO);

    assertThat(authentication.get("password"), is("newPassword123"));
  }

  @Test
  void testNoProxyAttributesNoValidation() throws Exception {
    Map<String, Map<String, Object>> storedAttributes = new HashMap<>();
    storedAttributes.put("storage", Map.of("blobStoreName", "default"));

    when(configuration.getAttributes()).thenReturn(storedAttributes);
    when(configuration.copy()).thenReturn(configuration);

    Map<String, Map<String, Object>> newAttributes = new HashMap<>();
    newAttributes.put("storage", Map.of("blobStoreName", "default"));

    repositoryXO = repositoryXO("test", "format", "recipe");
    repositoryXO.setAttributes(newAttributes);

    // Should NOT throw - not a proxy repository
    underTest.update(repositoryXO);
  }

  @Test
  void testPasswordPlaceholderIsRestoredOnUpdate() throws Exception {
    repositoryXO = repositoryXO("test", "format", "recipe");

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

    repositoryXO.setAttributes(testAttributes);
    when(configuration.getAttributes()).thenReturn(storedAttributes);

    underTest.update(repositoryXO);

    assertThat(authentication.get("password"), is("_123"));
  }

  @Test
  void testReadContainsRepoSize() {
    String repoName = "testRepo";
    String recipeName = "testRecipe";
    Long repoSize = 123456L;
    RepositoryMetricsDTO repoMetrics = new RepositoryMetricsDTO(repoName, repoSize);

    when(configuration.getRecipeName()).thenReturn(recipeName);
    when(repositoryMetricsService.list()).thenReturn(Collections.singletonList(repoMetrics));
    when(configuration.getRepositoryName()).thenReturn(repoName);
    when(repositoryPermissionChecker.userHasRepositoryAdminPermissionFor(any(Iterable.class), anyString()))
        .thenReturn(List.of(configuration));
    List<RepositoryXO> repos = underTest.read();
    assertEquals(1, repos.size());
    RepositoryXO repoXo = repos.get(0);
    assertEquals(repoName, repoXo.getName());
    assertEquals(Long.valueOf(123456), repoXo.getSize());
    assertEquals("maven2", repoXo.getFormat());
    assertEquals("hosted", repoXo.getType());
  }

  /**
   * Test: readReferences() with format filter excludes configurations with missing recipes (NEXUS-53342)
   */
  @Test
  void testReadReferences_withFormatFilter_excludesConfigurationsWithMissingRecipes() {
    // Setup: Similar to above but with format filter
    Configuration validConfig = validConfig();

    when(validConfig.getAttributes()).thenReturn(attributes());

    List<Configuration> allConfigs = new ArrayList<>();
    allConfigs.add(validConfig);
    allConfigs.add(invalidConfig());

    when(configurationStore.list()).thenReturn(allConfigs);
    when(repositoryPermissionChecker.userCanBrowseRepositories(any(Configuration[].class)))
        .thenReturn(List.of(validConfig));

    // Execute: Should NOT throw NPE
    List<RepositoryReferenceXO> result = underTest.readReferences(params("format", "maven2"));

    // Verify: Only valid repository is returned
    assertThat(result.size(), is(1));
    assertThat(result.get(0).getName(), is("valid-repo"));
  }

  /**
   * Test: readReferences() excludes configurations with missing recipes
   */
  @Test
  void testReadReferencesConfigurationsWithMissingRecipes() {
    Configuration validConfig = validConfig();
    when(validConfig.getAttributes()).thenReturn(attributes());

    List<Configuration> allConfigs = new ArrayList<>();
    allConfigs.add(validConfig);
    allConfigs.add(invalidConfig());

    when(configurationStore.list()).thenReturn(allConfigs);
    when(repositoryPermissionChecker.userCanBrowseRepositories(any(Configuration[].class)))
        .thenReturn(List.of(validConfig));

    StoreLoadParameters params = new StoreLoadParameters();
    params.setFilter(List.of());

    // Execute: Should NOT throw NPE
    List<RepositoryReferenceXO> result = underTest.readReferences(params);

    // Verify: Only valid repository is returned
    assertThat(result.size(), is(1));
    assertThat(result.get(0).getName(), is("valid-repo"));
  }

  /**
   * NEXUS-53722: {@link RepositoryUiService#read()} must fetch repository size metrics with a single
   * bulk {@link RepositoryMetricsService#list()} query rather than one
   * {@link RepositoryMetricsService#get(String)} call per repository (an N+1 query that made the
   * classic UI repository list slow with large numbers of repositories).
   */
  @Test
  void testReadFetchesMetricsInBulkNotPerRepository() {
    Configuration repo1 = givenMutableRepoConfiguration(underTest, "repo1", "rawRecipe", "raw");
    Configuration repo2 = givenMutableRepoConfiguration(underTest, "repo2", "mavenRecipe", "maven2");
    when(repositoryPermissionChecker.userHasRepositoryAdminPermissionFor(any(Iterable.class), anyString()))
        .thenReturn(List.of(repo1, repo2));
    when(repositoryMetricsService.list()).thenReturn(List.of(
        new RepositoryMetricsDTO("repo1", 111L),
        new RepositoryMetricsDTO("repo2", 222L)));

    List<RepositoryXO> repos = underTest.read();

    assertThat(repos, hasSize(2));
    Map<String, Long> sizeByName = new HashMap<>();
    repos.forEach(repo -> sizeByName.put(repo.getName(), repo.getSize()));
    assertEquals(Long.valueOf(111), sizeByName.get("repo1"));
    assertEquals(Long.valueOf(222), sizeByName.get("repo2"));

    // Metrics must be fetched once in bulk, never per-repository (N+1 guard).
    verify(repositoryMetricsService, times(1)).list();
    verify(repositoryMetricsService, never()).get(anyString());
  }

  /**
   * NEXUS-53722: a repository absent from the bulk metrics result must receive a {@code null} size
   * (rather than fail), while other repositories still get their size.
   */
  @Test
  void testReadSetsNullSizeWhenRepositoryHasNoMetricsEntry() {
    Configuration repo1 = givenMutableRepoConfiguration(underTest, "repo1", "rawRecipe", "raw");
    Configuration repo2 = givenMutableRepoConfiguration(underTest, "repo2", "mavenRecipe", "maven2");
    when(repositoryPermissionChecker.userHasRepositoryAdminPermissionFor(any(Iterable.class), anyString()))
        .thenReturn(List.of(repo1, repo2));
    // Bulk result contains metrics for repo1 only.
    when(repositoryMetricsService.list()).thenReturn(List.of(new RepositoryMetricsDTO("repo1", 111L)));

    List<RepositoryXO> repos = underTest.read();

    Map<String, Long> sizeByName = new HashMap<>();
    repos.forEach(repo -> sizeByName.put(repo.getName(), repo.getSize()));
    assertEquals(Long.valueOf(111), sizeByName.get("repo1"));
    assertNull(sizeByName.get("repo2"));
    verify(repositoryMetricsService, times(1)).list();
    verify(repositoryMetricsService, never()).get(anyString());
  }

  /**
   * NEXUS-53722: an empty bulk metrics result leaves every repository size {@code null} while still
   * issuing exactly one bulk query.
   */
  @Test
  void testReadSetsNullSizesWhenMetricsListEmpty() {
    Configuration repo1 = givenMutableRepoConfiguration(underTest, "repo1", "rawRecipe", "raw");
    Configuration repo2 = givenMutableRepoConfiguration(underTest, "repo2", "mavenRecipe", "maven2");
    when(repositoryPermissionChecker.userHasRepositoryAdminPermissionFor(any(Iterable.class), anyString()))
        .thenReturn(List.of(repo1, repo2));
    when(repositoryMetricsService.list()).thenReturn(Collections.emptyList());

    List<RepositoryXO> repos = underTest.read();

    assertThat(repos, hasSize(2));
    repos.forEach(repo -> assertNull(repo.getSize()));
    verify(repositoryMetricsService, times(1)).list();
    verify(repositoryMetricsService, never()).get(anyString());
  }

  /**
   * NEXUS-53722: if the bulk metrics result ever contains duplicate repository names (a stale or
   * inconsistent metrics store), read() must keep one entry rather than throw
   * {@link IllegalStateException} and fail the entire repository list.
   */
  @Test
  void testReadToleratesDuplicateMetricsEntries() {
    Configuration repo1 = givenMutableRepoConfiguration(underTest, "repo1", "rawRecipe", "raw");
    when(repositoryPermissionChecker.userHasRepositoryAdminPermissionFor(any(Iterable.class), anyString()))
        .thenReturn(List.of(repo1));
    // Two metrics entries share the same repository name; toMap must keep the first, not throw.
    when(repositoryMetricsService.list()).thenReturn(List.of(
        new RepositoryMetricsDTO("repo1", 111L),
        new RepositoryMetricsDTO("repo1", 222L)));

    List<RepositoryXO> repos = underTest.read();

    assertThat(repos, hasSize(1));
    assertEquals(Long.valueOf(111), repos.get(0).getSize());
    verify(repositoryMetricsService, times(1)).list();
  }

  /**
   * NEXUS-53722: when no {@link RepositoryMetricsService} is bound (e.g. the CORE edition), read()
   * must not attempt any metrics query and every repository size is {@code null}.
   */
  @Test
  void testReadSetsNullSizesWhenMetricsServiceUnavailable() {
    List<Recipe> recipeList = mock(List.class);
    when(QualifierUtil.buildQualifierBeanMap(recipeList)).thenReturn(recipes);
    RepositoryUiService serviceWithoutMetrics = spy(new RepositoryUiService(
        repositoryCacheInvalidationService, repositoryManager, null, configurationStore, securityHelper,
        recipeList, taskScheduler, typeLookup, formats, repositoryPermissionChecker, failedRepositoryTracker,
        httpAuthenticationSecretEncoder));
    Configuration repo = givenMutableRepoConfiguration(serviceWithoutMetrics, "repoNoMetrics", "rawRecipe", "raw");
    when(repositoryPermissionChecker.userHasRepositoryAdminPermissionFor(any(Iterable.class), anyString()))
        .thenReturn(List.of(repo));

    List<RepositoryXO> repos = serviceWithoutMetrics.read();

    // CORE edition has no RepositoryMetricsService bound: its absence is asserted via the outcome
    // (no NPE + null size), not via interaction verification on the (absent) service.
    assertThat(repos, hasSize(1));
    assertNull(repos.get(0).getSize());
  }

  /**
   * NEXUS-53722: the single-repository path still resolves size via the per-repository
   * {@link RepositoryMetricsService#get(String)} lookup and must not trigger the bulk
   * {@link RepositoryMetricsService#list()} query. Specifically this exercises the
   * failed-repository sub-path: {@code hasFailed} is stubbed {@code true}, so {@code update()}
   * routes through {@code updateFailedRepository()} -> {@code asRepository(Configuration)} ->
   * {@code get()}.
   */
  @Test
  void testSingleRepositoryPathUsesPerRepositoryGetNotBulkList() throws Exception {
    String repoName = "failed-repo";
    Type type = mock(Type.class);
    // 'repositoryXO' is a real POJO field on this class; build a valid input for the @Validate'd
    // update() call (name/online/attributes must satisfy bean-validation constraints).
    RepositoryXO repositoryXO = repositoryXO(repoName, "format", "mockRecipe");
    repositoryXO.setOnline(false);

    Configuration storedConfiguration = mock(Configuration.class);
    when(storedConfiguration.getRepositoryName()).thenReturn(repoName);
    when(storedConfiguration.getRecipeName()).thenReturn("mockRecipe");
    when(storedConfiguration.getAttributes()).thenReturn(new HashMap<>());
    when(storedConfiguration.copy()).thenReturn(storedConfiguration);
    when(storedConfiguration.isOnline()).thenReturn(false);
    when(configurationStore.list()).thenReturn(Collections.singletonList(storedConfiguration));

    Recipe testRecipe = mock(Recipe.class);
    when(testRecipe.getFormat()).thenReturn(format);
    when(testRecipe.getType()).thenReturn(type);
    when(format.getValue()).thenReturn("format");
    List<Recipe> testRecipeList = Collections.singletonList(testRecipe);
    Map<String, Recipe> testRecipesMap = new HashMap<>();
    testRecipesMap.put("mockRecipe", testRecipe);
    when(QualifierUtil.buildQualifierBeanMap(testRecipeList)).thenReturn(testRecipesMap);

    when(failedRepositoryTracker.hasFailed(repoName)).thenReturn(true);
    BaseRepositoryManager baseRepositoryManager = mock(BaseRepositoryManager.class);
    when(baseRepositoryManager.retryFailedRepository(repoName)).thenReturn(Optional.empty());

    // Per-repository metric supplied via get(); list() must never be used on this path.
    when(repositoryMetricsService.get(repoName))
        .thenReturn(Optional.of(new RepositoryMetricsDTO(repoName, 9999L)));

    RepositoryUiService testService = new RepositoryUiService(repositoryCacheInvalidationService,
        baseRepositoryManager, repositoryMetricsService, configurationStore, securityHelper,
        testRecipeList, taskScheduler, typeLookup, formats, repositoryPermissionChecker,
        failedRepositoryTracker, httpAuthenticationSecretEncoder);

    RepositoryXO result = testService.update(repositoryXO);

    assertEquals(repoName, result.getName());
    assertEquals(Long.valueOf(9999), result.getSize());
    verify(repositoryMetricsService).get(repoName);
    verify(repositoryMetricsService, never()).list();
  }

  /**
   * Builds a {@link Configuration} backed by mutable attribute maps so that the real
   * {@link RepositoryUiService#asRepository(Configuration)} path (which mutates attributes via
   * filterAttributes/normalizeContentDisposition) can run against it.
   */
  private Configuration givenMutableRepoConfiguration(
      final RepositoryUiService service,
      final String repoName,
      final String recipeName,
      final String formatValue)
  {
    Configuration c = new SimpleConfiguration();
    c.setRepositoryName(repoName);
    c.setRecipeName(recipeName);
    c.setOnline(true);

    Map<String, Object> storage = new HashMap<>();
    storage.put("blobStoreName", "default");
    Map<String, Map<String, Object>> attrs = new HashMap<>();
    attrs.put("storage", storage);
    c.setAttributes(attrs);

    Recipe mockRecipe = mock(Recipe.class);
    Format mockFormat = mock(Format.class);
    doReturn(new HostedType()).when(mockRecipe).getType();
    doReturn(mockFormat).when(mockRecipe).getFormat();
    doReturn(formatValue).when(mockFormat).getValue();
    service.addRecipe(recipeName, mockRecipe);

    return c;
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

  @Test
  void testRemoteUrlHostChangeAllowedWithEmptyCredentials() throws Exception {
    // Stored proxy config with remoteUrl and credentials
    Map<String, Map<String, Object>> storedAttributes = new HashMap<>();
    Map<String, Object> storedProxy = new HashMap<>();
    storedProxy.put("remoteUrl", "https://repo.example.com/v1/");
    storedAttributes.put("proxy", storedProxy);
    Map<String, Object> storedHttpclient = new HashMap<>();
    Map<String, Object> storedAuthentication = new HashMap<>();
    storedAuthentication.put("password", "oldSecret123");
    storedHttpclient.put("authentication", storedAuthentication);
    storedAttributes.put("httpclient", storedHttpclient);

    // New attributes with host changed AND no authentication (user cleared it intentionally)
    Map<String, Map<String, Object>> newAttributes = new HashMap<>();
    Map<String, Object> newProxy = new HashMap<>();
    newProxy.put("remoteUrl", "https://new-repo.example.com/v1/"); // Different host
    newAttributes.put("proxy", newProxy);
    Map<String, Object> newHttpclient = new HashMap<>();
    Map<String, Object> newAuthentication = new HashMap<>();
    // Empty authentication = no authentication for new repo
    newHttpclient.put("authentication", newAuthentication);
    newAttributes.put("httpclient", newHttpclient);

    repositoryXO.setAttributes(newAttributes);
    when(configuration.getAttributes()).thenReturn(storedAttributes);

    // Should NOT throw - user intentionally cleared credentials
    underTest.update(repositoryXO);
  }

  @Test
  void testRemoteUrlHostChangeAllowedWithNewBearerToken() throws Exception {
    repositoryXO = repositoryXO("test", "format", "recipe");

    // Stored proxy config with bearer token
    Map<String, Map<String, Object>> storedAttributes = new HashMap<>();
    Map<String, Object> storedProxy = new HashMap<>();
    storedProxy.put("remoteUrl", "https://repo.example.com/v1/");
    storedAttributes.put("proxy", storedProxy);
    Map<String, Object> storedHttpclient = new HashMap<>();
    Map<String, Object> storedAuthentication = new HashMap<>();
    storedAuthentication.put("bearerTokenId", "oldToken123");
    storedHttpclient.put("authentication", storedAuthentication);
    storedAttributes.put("httpclient", storedHttpclient);

    // New attributes with host changed AND new bearer token
    Map<String, Map<String, Object>> newAttributes = new HashMap<>();
    Map<String, Object> newProxy = new HashMap<>();
    newProxy.put("remoteUrl", "https://new-repo.example.com/v1/"); // Different host
    newAttributes.put("proxy", newProxy);
    Map<String, Object> newHttpclient = new HashMap<>();
    Map<String, Object> newAuthentication = new HashMap<>();
    newAuthentication.put("bearerTokenId", "newToken456"); // New token
    newHttpclient.put("authentication", newAuthentication);
    newAttributes.put("httpclient", newHttpclient);

    repositoryXO.setAttributes(newAttributes);
    when(configuration.getAttributes()).thenReturn(storedAttributes);

    // Should NOT throw - user provided new bearer token
    underTest.update(repositoryXO);

    // Verify new token was kept
    assertThat(newAuthentication.get("bearerTokenId"), is("newToken456"));
  }

  @Test
  void testRemoteUrlHostChangeAllowedWithNewCredentials() throws Exception {
    Map<String, Map<String, Object>> storedAttributes = new HashMap<>();
    Map<String, Object> storedProxy = new HashMap<>();
    storedProxy.put("remoteUrl", "https://repo.example.com/v1/");
    storedAttributes.put("proxy", storedProxy);
    Map<String, Object> storedHttpclient = new HashMap<>();
    Map<String, Object> storedAuthentication = new HashMap<>();
    storedAuthentication.put("type", "username");
    storedAuthentication.put("password", "oldSecret123");
    storedHttpclient.put("authentication", storedAuthentication);
    storedAttributes.put("httpclient", storedHttpclient);

    when(configuration.getAttributes()).thenReturn(storedAttributes);
    when(configuration.copy()).thenReturn(configuration);

    // New attributes with host changed AND new credentials (legitimate change)
    Map<String, Map<String, Object>> newAttributes = new HashMap<>();
    Map<String, Object> newProxy = new HashMap<>();
    newProxy.put("remoteUrl", "https://new-repo.example.com/v1/"); // Different host
    newAttributes.put("proxy", newProxy);
    Map<String, Object> newHttpclient = new HashMap<>();
    Map<String, Object> newAuthentication = new HashMap<>();
    newAuthentication.put("type", "username");
    newAuthentication.put("password", "newSecret456"); // New credential = intentional change
    newHttpclient.put("authentication", newAuthentication);
    newAttributes.put("httpclient", newHttpclient);

    repositoryXO.setAttributes(newAttributes);
    when(configuration.getAttributes()).thenReturn(storedAttributes);

    // Should NOT throw - user provided new credentials
    underTest.update(repositoryXO);

    // Verify new password was kept
    assertThat(newAuthentication.get("password"), is("newSecret456"));
  }

  @Test
  void testRemoteUrlHostChangeBlocksWithPlaceholder() throws Exception {
    // Stored proxy config with remoteUrl and credentials
    Map<String, Map<String, Object>> storedAttributes = new HashMap<>();
    Map<String, Object> storedProxy = new HashMap<>();
    storedProxy.put("remoteUrl", "https://repo.example.com/v1/");
    storedAttributes.put("proxy", storedProxy);
    Map<String, Object> storedHttpclient = new HashMap<>();
    Map<String, Object> storedAuthentication = new HashMap<>();
    storedAuthentication.put("type", "username");
    storedAuthentication.put("password", "secret123");
    storedHttpclient.put("authentication", storedAuthentication);
    storedAttributes.put("httpclient", storedHttpclient);

    when(configuration.getAttributes()).thenReturn(storedAttributes);

    // New attributes: different host + placeholder password
    Map<String, Map<String, Object>> newAttributes = new HashMap<>();
    Map<String, Object> newProxy = new HashMap<>();
    newProxy.put("remoteUrl", "https://attacker.example.com/v1/"); // Different host
    newAttributes.put("proxy", newProxy);
    Map<String, Object> newHttpclient = new HashMap<>();
    Map<String, Object> newAuthentication = new HashMap<>();
    newAuthentication.put("type", "username");
    newAuthentication.put("password", "#~NXRM~PLACEHOLDER~PASSWORD~#"); // Placeholder = credential exfiltration
    newHttpclient.put("authentication", newAuthentication);
    newAttributes.put("httpclient", newHttpclient);

    repositoryXO.setAttributes(newAttributes);
    when(configuration.getAttributes()).thenReturn(storedAttributes);

    // Should throw BadRequestException
    BadRequestException exception = assertThrows(BadRequestException.class,
        () -> underTest.update(repositoryXO));
    assertThat(exception.getMessage(), is(REMOTE_URL_CHANGE_ERROR_MESSAGE));
  }

  @Test
  void testRemoteUrlHostChangeBlocksWithBearerTokenPlaceholder() {
    // Stored proxy config with bearer token
    Map<String, Map<String, Object>> storedAttributes = new HashMap<>();
    Map<String, Object> storedProxy = new HashMap<>();
    storedProxy.put("remoteUrl", "https://repo.example.com/v1/");
    storedAttributes.put("proxy", storedProxy);
    Map<String, Object> storedHttpclient = new HashMap<>();
    Map<String, Object> storedAuth = new HashMap<>();
    storedAuth.put("type", "bearerToken");
    storedAuth.put("bearerTokenId", "secret-token-id");
    storedHttpclient.put("authentication", storedAuth);
    storedAttributes.put("httpclient", storedHttpclient);

    // New attributes with host changed AND bearer token placeholder
    Map<String, Map<String, Object>> newAttributes = new HashMap<>();
    Map<String, Object> newProxy = new HashMap<>();
    newProxy.put("remoteUrl", "https://attacker.example.com/v1/"); // Different host
    newAttributes.put("proxy", newProxy);
    Map<String, Object> newHttpclient = new HashMap<>();
    Map<String, Object> newAuthentication = new HashMap<>();
    newAuthentication.put("type", "bearerToken");
    newAuthentication.put("bearerTokenId", "#~NXRM~PLACEHOLDER~PASSWORD~#"); // Placeholder
    newHttpclient.put("authentication", newAuthentication);
    newAttributes.put("httpclient", newHttpclient);

    repositoryXO.setAttributes(newAttributes);
    when(configuration.getAttributes()).thenReturn(storedAttributes);

    // Should throw BadRequestException
    BadRequestException exception = assertThrows(BadRequestException.class, () -> underTest.update(repositoryXO));
    assertThat(exception.getMessage(), is(REMOTE_URL_CHANGE_ERROR_MESSAGE));
  }

  @Test
  void testRemoteUrlPathChangeAllowedWithNewCredentials() throws Exception {
    // Stored proxy config with remoteUrl
    Map<String, Map<String, Object>> storedAttributes = new HashMap<>();
    Map<String, Object> storedProxy = new HashMap<>();
    storedProxy.put("remoteUrl", "https://repo.example.com/v1/");
    storedAttributes.put("proxy", storedProxy);
    Map<String, Object> storedHttpclient = new HashMap<>();
    Map<String, Object> storedAuthentication = new HashMap<>();
    storedAuthentication.put("type", "username");
    storedAuthentication.put("password", "oldSecret123");
    storedHttpclient.put("authentication", storedAuthentication);
    storedAttributes.put("httpclient", storedHttpclient);

    when(configuration.getAttributes()).thenReturn(storedAttributes);
    when(configuration.copy()).thenReturn(configuration);

    // New attributes with path changed AND new credentials (legitimate change)
    Map<String, Map<String, Object>> newAttributes = new HashMap<>();
    Map<String, Object> newProxy = new HashMap<>();
    newProxy.put("remoteUrl", "https://repo.example.com/v2/"); // Different path
    newAttributes.put("proxy", newProxy);
    Map<String, Object> newHttpclient = new HashMap<>();
    Map<String, Object> newAuthentication = new HashMap<>();
    newAuthentication.put("type", "username");
    newAuthentication.put("password", "newSecret456"); // New credential = intentional change
    newHttpclient.put("authentication", newAuthentication);
    newAttributes.put("httpclient", newHttpclient);

    repositoryXO.setAttributes(newAttributes);
    when(configuration.getAttributes()).thenReturn(storedAttributes);

    // Should NOT throw - user provided new credentials
    underTest.update(repositoryXO);

    // Verify new password was kept
    assertThat(newAuthentication.get("password"), is("newSecret456"));
  }

  @Test
  void testRemoteUrlPathChangeBlocksWithPlaceholder() throws Exception {
    repositoryXO = repositoryXO("test", "format", "recipe");

    // Stored proxy config with remoteUrl
    Map<String, Map<String, Object>> storedAttributes = new HashMap<>();
    Map<String, Object> storedProxy = new HashMap<>();
    storedProxy.put("remoteUrl", "https://repo.example.com/v1/");
    storedAttributes.put("proxy", storedProxy);
    Map<String, Object> storedHttpclient = new HashMap<>();
    Map<String, Object> storedAuthentication = new HashMap<>();
    storedAuthentication.put("type", "username");
    storedAuthentication.put("password", "secret123");
    storedHttpclient.put("authentication", storedAuthentication);
    storedAttributes.put("httpclient", storedHttpclient);

    // New attributes with only path changed - ANY URL change now triggers validation
    Map<String, Map<String, Object>> newAttributes = new HashMap<>();
    Map<String, Object> newProxy = new HashMap<>();
    newProxy.put("remoteUrl", "https://repo.example.com/v2/"); // Different path = URL changed
    newAttributes.put("proxy", newProxy);
    Map<String, Object> newHttpclient = new HashMap<>();
    Map<String, Object> newAuthentication = new HashMap<>();
    newAuthentication.put("type", "username");
    newAuthentication.put("password", "#~NXRM~PLACEHOLDER~PASSWORD~#"); // Placeholder = credential exfiltration
    newHttpclient.put("authentication", newAuthentication);
    newAttributes.put("httpclient", newHttpclient);

    repositoryXO.setAttributes(newAttributes);
    when(repositoryXO.getAttributes()).thenReturn(newAttributes);
    when(configuration.getAttributes()).thenReturn(storedAttributes);

    // Should throw BadRequestException - ANY URL change with placeholder is blocked
    BadRequestException exception = assertThrows(BadRequestException.class,
        () -> underTest.update(repositoryXO));
    assertThat(exception.getMessage(), is(REMOTE_URL_CHANGE_ERROR_MESSAGE));
  }

  @Test
  void testRemoteUrlPortChangeBlocksWithPlaceholder() throws Exception {
    repositoryXO = repositoryXO("test", "format", "recipe");

    // Stored proxy config with default port
    Map<String, Map<String, Object>> storedAttributes = new HashMap<>();
    Map<String, Object> storedProxy = new HashMap<>();
    storedProxy.put("remoteUrl", "https://repo.example.com/v1/");
    storedAttributes.put("proxy", storedProxy);
    Map<String, Object> storedHttpclient = new HashMap<>();
    Map<String, Object> storedAuthentication = new HashMap<>();
    storedAuthentication.put("type", "username");
    storedAuthentication.put("password", "secret123");
    storedHttpclient.put("authentication", storedAuthentication);
    storedAttributes.put("httpclient", storedHttpclient);

    // New attributes with different port and placeholder
    Map<String, Map<String, Object>> newAttributes = new HashMap<>();
    Map<String, Object> newProxy = new HashMap<>();
    newProxy.put("remoteUrl", "https://repo.example.com:9443/v1/"); // Different port = URL changed
    newAttributes.put("proxy", newProxy);
    Map<String, Object> newHttpclient = new HashMap<>();
    Map<String, Object> newAuthentication = new HashMap<>();
    newAuthentication.put("type", "username");
    newAuthentication.put("password", "#~NXRM~PLACEHOLDER~PASSWORD~#");
    newHttpclient.put("authentication", newAuthentication);
    newAttributes.put("httpclient", newHttpclient);

    repositoryXO.setAttributes(newAttributes);
    when(configuration.getAttributes()).thenReturn(storedAttributes);

    // Should throw BadRequestException - URL changed with placeholder
    BadRequestException exception = assertThrows(BadRequestException.class,
        () -> underTest.update(repositoryXO));
    assertThat(exception.getMessage(), is(REMOTE_URL_CHANGE_ERROR_MESSAGE));
  }

  @Test
  void testRemoteUrlSchemeChangeBlocksWithPlaceholder() throws Exception {
    repositoryXO = repositoryXO("test", "format", "recipe");

    // Stored proxy config with https
    Map<String, Map<String, Object>> storedAttributes = new HashMap<>();
    Map<String, Object> storedProxy = new HashMap<>();
    storedProxy.put("remoteUrl", "https://repo.example.com/v1/");
    storedAttributes.put("proxy", storedProxy);
    Map<String, Object> storedHttpclient = new HashMap<>();
    Map<String, Object> storedAuthentication = new HashMap<>();
    storedAuthentication.put("type", "username");
    storedAuthentication.put("password", "secret123");
    storedHttpclient.put("authentication", storedAuthentication);
    storedAttributes.put("httpclient", storedHttpclient);

    // New attributes with scheme changed and placeholder
    Map<String, Map<String, Object>> newAttributes = new HashMap<>();
    Map<String, Object> newProxy = new HashMap<>();
    newProxy.put("remoteUrl", "http://repo.example.com/v1/"); // http instead of https = URL changed
    newAttributes.put("proxy", newProxy);
    Map<String, Object> newHttpclient = new HashMap<>();
    Map<String, Object> newAuthentication = new HashMap<>();
    newAuthentication.put("type", "username");
    newAuthentication.put("password", "#~NXRM~PLACEHOLDER~PASSWORD~#");
    newHttpclient.put("authentication", newAuthentication);
    newAttributes.put("httpclient", newHttpclient);

    repositoryXO.setAttributes(newAttributes);
    when(configuration.getAttributes()).thenReturn(storedAttributes);

    // Should throw BadRequestException - URL changed with placeholder
    BadRequestException exception = assertThrows(BadRequestException.class,
        () -> underTest.update(repositoryXO));
    assertThat(exception.getMessage(), is(REMOTE_URL_CHANGE_ERROR_MESSAGE));
  }

  @Test
  void testRoutingRuleCleared() throws Exception {
    repositoryXO = repositoryXO("test", "format", "recipe");
    repositoryXO.setAttributes(repositoryXO.getAttributes());

    underTest.update(repositoryXO);

    verify(configuration).setOnline(true);
    verify(configuration).setRoutingRuleId(null);
    verify(configuration).setAttributes(repositoryXO.getAttributes());
  }

  // NEXUS-52779: ExtDirect create must derive format from the recipe registry, not the caller-supplied DTO field

  @Test
  void testRoutingRuleSet() throws Exception {
    repositoryXO = repositoryXO("test", "format", "recipe");
    repositoryXO.setRoutingRuleId("test");
    repositoryXO.setAttributes(repositoryXO.getAttributes());

    underTest.update(repositoryXO);

    verify(configuration).setOnline(true);
    verify(configuration).setRoutingRuleId(any(EntityId.class));
    verify(configuration).setAttributes(repositoryXO.getAttributes());
  }

  @Test
  void testUpdateFailedRepository_nullRecipe() throws Exception {
    String repoName = "failed-repo";

    // Setup: Configuration with a recipe name that doesn't exist in recipes map
    Configuration config = mock();
    when(config.getRepositoryName()).thenReturn(repoName);
    when(config.getRecipeName()).thenReturn("nonexistent-recipe");

    when(configurationStore.list()).thenReturn(List.of(config));
    when(failedRepositoryTracker.hasFailed(repoName)).thenReturn(true);

    // Note: recipes HashMap naturally returns null for "nonexistent-recipe" since mockRecipes() only adds "testRecipe"

    // Create a minimal RepositoryXO
    RepositoryXO xo = repositoryXO(repoName, "missing", "missing-proxy");

    // Execute: Should throw BadRequestException for null recipe
    BadRequestException ex = assertThrows(BadRequestException.class, () -> underTest.update(xo));
    assertThat(ex.getMessage(), is("Recipe not found for repository: failed-repo"));
  }

  @Test
  void testUpdateFailedRepository_stillFailing() throws Exception {
    // Setup: Repository failed and remains failed after update
    String repoName = "still-failing-repo";
    repositoryXO = repositoryXO("still-failing-repo", "maven2", "group");
    repositoryXO.setOnline(false);

    // Mock configuration
    Configuration storedConfiguration = mock();
    when(storedConfiguration.getRepositoryName()).thenReturn(repoName);
    when(storedConfiguration.getRecipeName()).thenReturn("maven2-proxy");
    when(storedConfiguration.copy()).thenReturn(storedConfiguration);
    when(storedConfiguration.isOnline()).thenReturn(false);
    when(storedConfiguration.getAttributes()).thenReturn(new HashMap<>());

    when(failedRepositoryTracker.hasFailed(repoName)).thenReturn(true);
    // Mock configuration store
    when(configurationStore.list()).thenReturn(List.of(storedConfiguration));

    // Setup recipe for QualifierUtil
    Recipe mockRecipe = mock(Recipe.class, "maven2-proxy");
    Format mockFormat = mock();
    Type mockType = mock();
    when(mockFormat.getValue()).thenReturn("maven2");
    when(mockType.getValue()).thenReturn("proxy");
    when(mockRecipe.getFormat()).thenReturn(mockFormat);
    when(mockRecipe.getType()).thenReturn(mockType);

    // Create recipe list and mock QualifierUtil to return recipe map
    Map<String, Recipe> recipeMap = new HashMap<>();
    recipeMap.put("maven2-proxy", mockRecipe);
    List<Recipe> recipeList = List.of(mockRecipe);
    // Use existing mockedStatic from @BeforeEach setup
    mockedStatic.when(() -> QualifierUtil.buildQualifierBeanMap(recipeList)).thenReturn(recipeMap);

    // Mock failed repository tracker
    when(failedRepositoryTracker.hasFailed(repoName)).thenReturn(true);

    // Mock BaseRepositoryManager with failed retry (returns empty)
    BaseRepositoryManager<?> baseRepositoryManager = mock(BaseRepositoryManager.class);
    when(baseRepositoryManager.retryFailedRepository(repoName)).thenReturn(Optional.empty());

    // Use real RepositoryUiService instance for this test
    RepositoryUiService testService = new RepositoryUiService(repositoryCacheInvalidationService,
        baseRepositoryManager, repositoryMetricsService, configurationStore, securityHelper,
        recipeList, taskScheduler, typeLookup, formats, repositoryPermissionChecker,
        failedRepositoryTracker, httpAuthenticationSecretEncoder);

    // Execute: Update the failed repository
    testService.update(repositoryXO);

    // Verify: Configuration was updated
    verify(storedConfiguration).setOnline(false);
    verify(storedConfiguration).setAttributes(repositoryXO.getAttributes());
    verify(configurationStore).update(storedConfiguration);

    // Verify: Retry was attempted but failed
    verify(baseRepositoryManager).retryFailedRepository(repoName);
    verify(failedRepositoryTracker).hasFailed(repoName);
  }

  @Test
  void testUpdateFailedRepository_successfulRecovery() throws Exception {
    // Setup: Repository failed to load initially
    String repoName = "failed-repo";
    Type type = mock();
    repositoryXO = repositoryXO(repoName, "maven2", "recipe");

    // Mock configuration
    Configuration storedConfiguration = mock();
    when(storedConfiguration.getRepositoryName()).thenReturn(repoName);
    when(storedConfiguration.getRecipeName()).thenReturn("maven2-hosted");
    when(storedConfiguration.getAttributes()).thenReturn(new HashMap<>());
    when(storedConfiguration.copy()).thenReturn(storedConfiguration);
    when(storedConfiguration.isOnline()).thenReturn(true);

    // Mock configuration store
    when(failedRepositoryTracker.hasFailed(repoName)).thenReturn(true);
    when(configurationStore.list()).thenReturn(List.of(storedConfiguration));

    // Setup recipe for QualifierUtil
    Recipe mockRecipe = mock(Recipe.class, "maven2-hosted");
    when(mockRecipe.getFormat()).thenReturn(format);
    when(format.getValue()).thenReturn("maven2");

    // Create recipe list and mock QualifierUtil to return recipe map
    Map<String, Recipe> recipeMap = new HashMap<>();
    recipeMap.put("maven2-hosted", mockRecipe);
    List<Recipe> recipeList = List.of(mockRecipe);
    // Use existing mockedStatic from @BeforeEach setup
    mockedStatic.when(() -> QualifierUtil.buildQualifierBeanMap(recipeList)).thenReturn(recipeMap);

    // Mock failed repository tracker
    when(failedRepositoryTracker.hasFailed(repoName)).thenReturn(true);

    // Mock BaseRepositoryManager with successful retry
    BaseRepositoryManager<?> baseRepositoryManager = mock(BaseRepositoryManager.class);
    Repository recoveredRepo = mock(Repository.class);
    when(recoveredRepo.getName()).thenReturn(repoName);
    when(recoveredRepo.getType()).thenReturn(type);
    when(recoveredRepo.getFormat()).thenReturn(format);
    when(recoveredRepo.getConfiguration()).thenReturn(storedConfiguration);
    when(baseRepositoryManager.retryFailedRepository(repoName)).thenReturn(Optional.of(recoveredRepo));

    // Use real RepositoryUiService instance for this test
    RepositoryUiService testService = new RepositoryUiService(repositoryCacheInvalidationService,
        baseRepositoryManager, repositoryMetricsService, configurationStore, securityHelper,
        recipeList, taskScheduler, typeLookup, formats, repositoryPermissionChecker,
        failedRepositoryTracker, httpAuthenticationSecretEncoder);

    // Execute: Update the failed repository
    testService.update(repositoryXO);

    // Verify: Configuration was updated
    verify(storedConfiguration).setOnline(true);
    verify(storedConfiguration).setAttributes(repositoryXO.getAttributes());
    verify(configurationStore).update(storedConfiguration);
    verify(baseRepositoryManager).retryFailedRepository(repoName);
    verify(failedRepositoryTracker).hasFailed(repoName);

    // Verify: Retry was attempted
    verify(baseRepositoryManager).retryFailedRepository(repoName);
  }

  private static Map<String, Map<String, Object>> attributes() {
    Map<String, Object> storageAttr = new HashMap<>();
    storageAttr.put("blobStoreName", "default");

    return Map.of("storage", storageAttr);
  }

  private static List<RepositoryReferenceXO> getTestRepositories() {
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
    Recipe recipe = mock();
    Type type = mock();
    Format format = mock();

    c.setRepositoryName(repoName);
    c.setRecipeName(recipeName);
    c.setOnline(isOnline);
    c.setAttributes(
        Map.of("storage", Map.of("blobStoreName", blobStoreName), "maven", Map.of("versionPolicy", versionPolicy)));

    doReturn(type).when(recipe).getType();
    doReturn(typeValue).when(type).getValue();
    doReturn(format).when(recipe).getFormat();
    doReturn(formatValue).when(format).getValue();

    underTest.addRecipe(recipeName, recipe);
    recipes.put(recipeName, recipe);

    return c;
  }

  private List<Configuration> givenRepoConfigurations() {
    return List.of(
        givenRepoConfiguration("repo1", "raw", "rawRecipe", "blobStore1", "strict", "testType", true),
        givenRepoConfiguration("repo2", "maven", "mavenRecipe", "blobStore2", "none", "otherType", false));
  }

  private static Configuration invalidConfig() {
    return mock();
  }

  private void mockRecipes() {
    lenient().when(recipe.getType()).thenReturn(new HostedType());
    lenient().when(recipe.getFormat()).thenReturn(new Format("maven2")
    {
    });
    recipes = new HashMap<>();
    recipes.put("testRecipe", recipe);

    // cargo recipes used by cargo auth override tests
    Recipe cargoProxyRecipe = mock();
    lenient().when(cargoProxyRecipe.getType()).thenReturn(new ProxyType());
    lenient().when(cargoProxyRecipe.getFormat()).thenReturn(new Format("cargo")
    {
    });
    recipes.put("cargo-proxy", cargoProxyRecipe);

    Recipe cargoGroupRecipe = mock();
    lenient().when(cargoGroupRecipe.getType()).thenReturn(new GroupType());
    lenient().when(cargoGroupRecipe.getFormat()).thenReturn(new Format("cargo")
    {
    });
    recipes.put("cargo-group", cargoGroupRecipe);

    // maven2-hosted recipe used by non-cargo override test
    Recipe maven2HostedRecipe = mock();
    lenient().when(maven2HostedRecipe.getType()).thenReturn(new HostedType());
    lenient().when(maven2HostedRecipe.getFormat()).thenReturn(new Format("maven2")
    {
    });
    recipes.put("maven2-hosted", maven2HostedRecipe);
  }

  private void mockRepository() {
    lenient().when(repository.getName()).thenReturn("repository");
    lenient().when(repository.getType()).thenReturn(new HostedType());
    lenient().when(repository.getFormat()).thenReturn(format);
  }

  private static StoreLoadParameters params(String property, String value) {
    StoreLoadParameters params = new StoreLoadParameters();
    List<StoreLoadParameters.Filter> filters = new ArrayList<>();
    StoreLoadParameters.Filter formatFilter = new StoreLoadParameters.Filter();
    formatFilter.setProperty("format");
    formatFilter.setValue(value);
    filters.add(formatFilter);
    params.setFilter(filters);
    return params;
  }

  private static Configuration validConfig() {
    Configuration validConfig = mock();
    when(validConfig.getRepositoryName()).thenReturn("valid-repo");
    when(validConfig.getRecipeName()).thenReturn("testRecipe"); // exists in recipes map

    return validConfig;
  }

  private static StoreLoadParameters createParameters() {
    StoreLoadParameters params = new StoreLoadParameters();
    params.setFilter(List.of());
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

  private static RepositoryXO repositoryXO() {
    return repositoryXO("my-repo", "raw", "testRecipe");
  }

  private static RepositoryXO repositoryXO(String name, String format, String recipe) {
    RepositoryXO repositoryXO = new RepositoryXO();
    repositoryXO.setName(name);
    repositoryXO.setFormat(format);
    repositoryXO.setRecipe(recipe);
    repositoryXO.setOnline(true);
    repositoryXO.setAttributes(Map.of("dummy", Map.of()));
    return repositoryXO;
  }

  // ---- NEXUS-54946: toFacetList allowlist (CWE-470 / classloader-escape) ----

  /**
   * A URI-shaped facet string (jar:file: scheme) must never reach {@code typeLookup.type()}.
   */
  @Test
  void toFacetList_rejectsJarFileScheme() {
    StoreLoadParameters params = new StoreLoadParameters();
    params.setFilter(List.of(
        filterEntry("facets", "jar:file:/proc/self/fd/3!/evil")));
    when(configurationStore.list()).thenReturn(List.of());
    when(repositoryPermissionChecker.userCanBrowseRepositories(any(Configuration[].class)))
        .thenReturn(List.of());

    underTest.readReferences(params);

    verify(typeLookup, never()).type("jar:file:/proc/self/fd/3!/evil");
  }

  /**
   * A file: URI facet string must never reach {@code typeLookup.type()}.
   */
  @Test
  void toFacetList_rejectsFileUri() {
    StoreLoadParameters params = new StoreLoadParameters();
    params.setFilter(List.of(
        filterEntry("facets", "file:/etc/passwd")));
    when(configurationStore.list()).thenReturn(List.of());
    when(repositoryPermissionChecker.userCanBrowseRepositories(any(Configuration[].class)))
        .thenReturn(List.of());

    underTest.readReferences(params);

    verify(typeLookup, never()).type("file:/etc/passwd");
  }

  /**
   * Any string containing /proc/ must never reach {@code typeLookup.type()}.
   */
  @Test
  void toFacetList_rejectsProcPath() {
    StoreLoadParameters params = new StoreLoadParameters();
    params.setFilter(List.of(
        filterEntry("facets", "/proc/self/mem")));
    when(configurationStore.list()).thenReturn(List.of());
    when(repositoryPermissionChecker.userCanBrowseRepositories(any(Configuration[].class)))
        .thenReturn(List.of());

    underTest.readReferences(params);

    verify(typeLookup, never()).type("/proc/self/mem");
  }

  /**
   * Any string containing a colon or slash must never reach {@code typeLookup.type()}.
   */
  @Test
  void toFacetList_rejectsColonOrSlash() {
    StoreLoadParameters params = new StoreLoadParameters();
    params.setFilter(List.of(
        filterEntry("facets", "evil:colon/slash")));
    when(configurationStore.list()).thenReturn(List.of());
    when(repositoryPermissionChecker.userCanBrowseRepositories(any(Configuration[].class)))
        .thenReturn(List.of());

    underTest.readReferences(params);

    verify(typeLookup, never()).type("evil:colon/slash");
  }

  /**
   * A valid dotted Java binary class name must still pass through to {@code typeLookup.type()}.
   */
  @Test
  void toFacetList_allowsValidJavaBinaryClassName() {
    String validClassName = "org.sonatype.nexus.repository.Facet";
    StoreLoadParameters params = new StoreLoadParameters();
    params.setFilter(List.of(
        filterEntry("facets", validClassName)));
    when(configurationStore.list()).thenReturn(List.of());
    when(repositoryPermissionChecker.userCanBrowseRepositories(any(Configuration[].class)))
        .thenReturn(List.of());
    when(typeLookup.type(validClassName)).thenReturn(null);

    underTest.readReferences(params);

    verify(typeLookup).type(validClassName);
  }

  /** Helper: build a {@link StoreLoadParameters.Filter} entry for {@code readReferences} tests. */
  private static StoreLoadParameters.Filter filterEntry(final String property, final String value) {
    StoreLoadParameters.Filter filter = new StoreLoadParameters.Filter();
    filter.setProperty(property);
    filter.setValue(value);
    return filter;
  }
}

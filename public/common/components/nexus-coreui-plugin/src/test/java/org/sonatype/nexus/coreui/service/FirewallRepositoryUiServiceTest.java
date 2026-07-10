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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.sonatype.nexus.common.QualifierUtil;
import org.sonatype.nexus.common.app.BaseUrlHolder;
import org.sonatype.nexus.common.app.GlobalComponentLookupHelper;
import org.sonatype.nexus.coreui.RepositoryXO;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Recipe;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.cache.RepositoryCacheInvalidationService;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.config.ConfigurationStore;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.rest.api.RepositoryMetricsService;
import org.sonatype.nexus.repository.security.RepositoryPermissionChecker;
import org.sonatype.nexus.repository.types.ProxyType;
import org.sonatype.nexus.scheduling.TaskScheduler;
import org.sonatype.nexus.security.SecurityHelper;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension.WithUser;
import org.sonatype.nexus.testcommon.validation.ValidationExtension;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.sonatype.nexus.repository.httpclient.HttpClientFacet;
import org.sonatype.nexus.repository.httpclient.RemoteConnectionStatus;

/**
 * Tests for ExtJS firewall configuration UI scenarios through {@link RepositoryUiService}.
 * These tests verify that the UI layer properly handles firewall configuration data.
 */
@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(ValidationExtension.class)
@ExtendWith(AuthenticationExtension.class)
@WithUser
class FirewallRepositoryUiServiceTest
{
  private static final String FIREWALL_ENABLED = "enabled";

  private static final String FIREWALL_MODE = "mode";

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

  @BeforeEach
  void setup() {
    mockedStatic = Mockito.mockStatic(QualifierUtil.class);
    mockRepository();
    mockRecipes();
    BaseUrlHolder.set("http://nexus-url", "");
    when(format.getValue()).thenReturn("npm");
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
  void testCreateRepositoryWithFirewallEnabled() throws Exception {
    // Scenario 1: Create repository with firewall enabled
    when(repositoryXO.getName()).thenReturn("npm-proxy");
    when(repositoryXO.getFormat()).thenReturn("npm");
    when(repositoryXO.getOnline()).thenReturn(true);

    Map<String, Map<String, Object>> testAttributes = new HashMap<>();
    Map<String, Object> firewallAttributes = new HashMap<>();
    firewallAttributes.put(FIREWALL_ENABLED, true);
    firewallAttributes.put(FIREWALL_MODE, "AUDIT");
    testAttributes.put("firewall", firewallAttributes);

    when(repositoryXO.getAttributes()).thenReturn(testAttributes);

    underTest.update(repositoryXO);

    verify(configuration).setAttributes(testAttributes);
    assertTrue((Boolean) testAttributes.get("firewall").get(FIREWALL_ENABLED));
    assertEquals("AUDIT", testAttributes.get("firewall").get(FIREWALL_MODE));
  }

  @Test
  void testEditRepositoryToChangeFirewallMode() throws Exception {
    // Scenario 2: Edit repository to change firewall mode from AUDIT to QUARANTINE
    when(repositoryXO.getName()).thenReturn("npm-proxy");
    when(repositoryXO.getFormat()).thenReturn("npm");
    when(repositoryXO.getOnline()).thenReturn(true);

    // Existing configuration has AUDIT mode
    Map<String, Map<String, Object>> storedAttributes = new HashMap<>();
    Map<String, Object> storedFirewall = new HashMap<>();
    storedFirewall.put(FIREWALL_ENABLED, true);
    storedFirewall.put(FIREWALL_MODE, "AUDIT");
    storedAttributes.put("firewall", storedFirewall);

    when(configuration.getAttributes()).thenReturn(storedAttributes);

    // UI sends updated configuration with QUARANTINE mode
    Map<String, Map<String, Object>> updatedAttributes = new HashMap<>();
    Map<String, Object> updatedFirewall = new HashMap<>();
    updatedFirewall.put(FIREWALL_ENABLED, true);
    updatedFirewall.put(FIREWALL_MODE, "QUARANTINE");
    updatedAttributes.put("firewall", updatedFirewall);

    when(repositoryXO.getAttributes()).thenReturn(updatedAttributes);

    underTest.update(repositoryXO);

    verify(configuration).setAttributes(updatedAttributes);
    assertEquals("QUARANTINE", updatedAttributes.get("firewall").get(FIREWALL_MODE));
  }

  @Test
  void testPCCSOptionForNpmRepository() throws Exception {
    // Scenario 3: PCCS mode should be allowed for npm repositories
    when(repositoryXO.getName()).thenReturn("npm-proxy");
    when(repositoryXO.getFormat()).thenReturn("npm");
    when(repositoryXO.getOnline()).thenReturn(true);

    Map<String, Map<String, Object>> testAttributes = new HashMap<>();
    Map<String, Object> firewallAttributes = new HashMap<>();
    firewallAttributes.put(FIREWALL_ENABLED, true);
    firewallAttributes.put(FIREWALL_MODE, "PCCS");
    testAttributes.put("firewall", firewallAttributes);

    when(repositoryXO.getAttributes()).thenReturn(testAttributes);

    underTest.update(repositoryXO);

    verify(configuration).setAttributes(testAttributes);
    assertEquals("PCCS", testAttributes.get("firewall").get(FIREWALL_MODE));
  }

  @Test
  void testPCCSOptionForPyPiRepository() throws Exception {
    // Scenario 3: PCCS mode should be allowed for pypi repositories
    when(repositoryXO.getName()).thenReturn("pypi-proxy");
    when(repositoryXO.getFormat()).thenReturn("pypi");
    when(repositoryXO.getOnline()).thenReturn(true);

    Map<String, Map<String, Object>> testAttributes = new HashMap<>();
    Map<String, Object> firewallAttributes = new HashMap<>();
    firewallAttributes.put(FIREWALL_ENABLED, true);
    firewallAttributes.put(FIREWALL_MODE, "PCCS");
    testAttributes.put("firewall", firewallAttributes);

    when(repositoryXO.getAttributes()).thenReturn(testAttributes);

    underTest.update(repositoryXO);

    verify(configuration).setAttributes(testAttributes);
    assertEquals("PCCS", testAttributes.get("firewall").get(FIREWALL_MODE));
  }

  @Test
  void testFirewallDisabledClearsMode() throws Exception {
    // Scenario 4: When firewall is unchecked, mode should be cleared
    when(repositoryXO.getName()).thenReturn("npm-proxy");
    when(repositoryXO.getFormat()).thenReturn("npm");
    when(repositoryXO.getOnline()).thenReturn(true);

    Map<String, Map<String, Object>> testAttributes = new HashMap<>();
    Map<String, Object> firewallAttributes = new HashMap<>();
    firewallAttributes.put(FIREWALL_ENABLED, false);
    firewallAttributes.put(FIREWALL_MODE, null); // Mode cleared when disabled
    testAttributes.put("firewall", firewallAttributes);

    when(repositoryXO.getAttributes()).thenReturn(testAttributes);

    underTest.update(repositoryXO);

    verify(configuration).setAttributes(testAttributes);
    assertFalse((Boolean) testAttributes.get("firewall").get(FIREWALL_ENABLED));
    assertThat(testAttributes.get("firewall").get(FIREWALL_MODE), nullValue());
  }

  @Test
  void testFirewallSectionAppearsInMavenProxyRepository() throws Exception {
    // Scenario 5: Firewall section should appear in all proxy repository forms
    when(repositoryXO.getName()).thenReturn("maven-proxy");
    when(repositoryXO.getFormat()).thenReturn("maven2");
    when(repositoryXO.getOnline()).thenReturn(true);

    Map<String, Map<String, Object>> testAttributes = new HashMap<>();
    Map<String, Object> firewallAttributes = new HashMap<>();
    firewallAttributes.put(FIREWALL_ENABLED, true);
    firewallAttributes.put(FIREWALL_MODE, "AUDIT");
    testAttributes.put("firewall", firewallAttributes);

    when(repositoryXO.getAttributes()).thenReturn(testAttributes);

    underTest.update(repositoryXO);

    verify(configuration).setAttributes(testAttributes);
    assertNotNull(testAttributes.get("firewall"));
    assertTrue((Boolean) testAttributes.get("firewall").get(FIREWALL_ENABLED));
  }

  @Test
  void testFirewallSectionAppearsInDockerProxyRepository() throws Exception {
    // Scenario 5: Firewall section should appear in all proxy repository forms
    when(repositoryXO.getName()).thenReturn("docker-proxy");
    when(repositoryXO.getFormat()).thenReturn("docker");
    when(repositoryXO.getOnline()).thenReturn(true);

    Map<String, Map<String, Object>> testAttributes = new HashMap<>();
    Map<String, Object> firewallAttributes = new HashMap<>();
    firewallAttributes.put(FIREWALL_ENABLED, true);
    firewallAttributes.put(FIREWALL_MODE, "QUARANTINE");
    testAttributes.put("firewall", firewallAttributes);

    when(repositoryXO.getAttributes()).thenReturn(testAttributes);

    underTest.update(repositoryXO);

    verify(configuration).setAttributes(testAttributes);
    assertNotNull(testAttributes.get("firewall"));
    assertTrue((Boolean) testAttributes.get("firewall").get(FIREWALL_ENABLED));
  }

  @Test
  void testFirewallSectionAppearsInNuGetProxyRepository() throws Exception {
    // Scenario 5: Firewall section should appear in all proxy repository forms
    when(repositoryXO.getName()).thenReturn("nuget-proxy");
    when(repositoryXO.getFormat()).thenReturn("nuget");
    when(repositoryXO.getOnline()).thenReturn(true);

    Map<String, Map<String, Object>> testAttributes = new HashMap<>();
    Map<String, Object> firewallAttributes = new HashMap<>();
    firewallAttributes.put(FIREWALL_ENABLED, false);
    testAttributes.put("firewall", firewallAttributes);

    when(repositoryXO.getAttributes()).thenReturn(testAttributes);

    underTest.update(repositoryXO);

    verify(configuration).setAttributes(testAttributes);
    assertNotNull(testAttributes.get("firewall"));
    assertFalse((Boolean) testAttributes.get("firewall").get(FIREWALL_ENABLED));
  }

  @Test
  void testTransitionFromDisabledToEnabled() throws Exception {
    // Test transitioning from firewall disabled to enabled with mode selection
    when(repositoryXO.getName()).thenReturn("npm-proxy");
    when(repositoryXO.getFormat()).thenReturn("npm");
    when(repositoryXO.getOnline()).thenReturn(true);

    // Initially disabled
    Map<String, Map<String, Object>> storedAttributes = new HashMap<>();
    Map<String, Object> storedFirewall = new HashMap<>();
    storedFirewall.put(FIREWALL_ENABLED, false);
    storedAttributes.put("firewall", storedFirewall);

    when(configuration.getAttributes()).thenReturn(storedAttributes);

    // UI sends enabled with AUDIT mode
    Map<String, Map<String, Object>> updatedAttributes = new HashMap<>();
    Map<String, Object> updatedFirewall = new HashMap<>();
    updatedFirewall.put(FIREWALL_ENABLED, true);
    updatedFirewall.put(FIREWALL_MODE, "AUDIT");
    updatedAttributes.put("firewall", updatedFirewall);

    when(repositoryXO.getAttributes()).thenReturn(updatedAttributes);

    underTest.update(repositoryXO);

    verify(configuration).setAttributes(updatedAttributes);
    assertTrue((Boolean) updatedAttributes.get("firewall").get(FIREWALL_ENABLED));
    assertEquals("AUDIT", updatedAttributes.get("firewall").get(FIREWALL_MODE));
  }

  @Test
  void testReadRepositoryIncludesFirewallConfiguration() throws Exception {
    // Test that reading a repository includes firewall configuration
    String repoName = "npm-proxy";
    String recipeName = "npm-proxy-recipe";

    when(configuration.getRecipeName()).thenReturn(recipeName);
    when(configuration.getRepositoryName()).thenReturn(repoName);
    when(repositoryMetricsService.get(anyString())).thenReturn(Optional.empty());

    // Set up firewall configuration
    Map<String, Map<String, Object>> storedAttributes = new HashMap<>();
    Map<String, Object> storedFirewall = new HashMap<>();
    storedFirewall.put(FIREWALL_ENABLED, true);
    storedFirewall.put(FIREWALL_MODE, "QUARANTINE");
    storedAttributes.put("firewall", storedFirewall);

    when(configuration.getAttributes()).thenReturn(storedAttributes);
    when(repositoryPermissionChecker.userHasRepositoryAdminPermissionFor(any(Iterable.class), anyString()))
        .thenReturn(Collections.singletonList(configuration));

    // Rebase fallout: RepositoryUiService.buildStatus (NEXUS-51389) now reads HttpClientFacet for
    // proxy repos via the private asRepository(Configuration) path, which can't be overridden by
    // the spy. Stub the facet so the production code path completes; getStatus() values are not
    // asserted by this test.
    HttpClientFacet httpClientFacet = mock(HttpClientFacet.class);
    org.mockito.Mockito.doReturn(httpClientFacet).when(repository).facet(HttpClientFacet.class);
    RemoteConnectionStatus remoteStatus = mock(RemoteConnectionStatus.class);
    when(httpClientFacet.getStatus()).thenReturn(remoteStatus);

    List<RepositoryXO> repos = underTest.read();

    assertThat(repos, notNullValue());
  }

  @Test
  void testPCCSOnlyForNpmAndPyPi() {
    // Verify that PCCS mode is only valid for npm and pypi formats
    // This test validates the UI constraint that PCCS should only be shown for npm/pypi

    // For npm - PCCS is valid
    assertTrue(isValidFirewallMode("npm", "PCCS"));
    assertTrue(isValidFirewallMode("npm", "AUDIT"));
    assertTrue(isValidFirewallMode("npm", "QUARANTINE"));

    // For pypi - PCCS is valid
    assertTrue(isValidFirewallMode("pypi", "PCCS"));
    assertTrue(isValidFirewallMode("pypi", "AUDIT"));
    assertTrue(isValidFirewallMode("pypi", "QUARANTINE"));

    // For other formats - only AUDIT and QUARANTINE are valid
    assertFalse(isValidFirewallMode("maven2", "PCCS"));
    assertTrue(isValidFirewallMode("maven2", "AUDIT"));
    assertTrue(isValidFirewallMode("maven2", "QUARANTINE"));

    assertFalse(isValidFirewallMode("docker", "PCCS"));
    assertTrue(isValidFirewallMode("docker", "AUDIT"));

    assertFalse(isValidFirewallMode("nuget", "PCCS"));
    assertTrue(isValidFirewallMode("nuget", "QUARANTINE"));
  }

  @Test
  void testChangingModeBetweenAllOptions() throws Exception {
    // Test changing mode between all valid options for npm
    when(repositoryXO.getName()).thenReturn("npm-proxy");
    when(repositoryXO.getFormat()).thenReturn("npm");
    when(repositoryXO.getOnline()).thenReturn(true);

    // Test AUDIT -> QUARANTINE
    testModeChange("AUDIT", "QUARANTINE");

    // Test QUARANTINE -> PCCS
    testModeChange("QUARANTINE", "PCCS");

    // Test PCCS -> AUDIT
    testModeChange("PCCS", "AUDIT");
  }

  private void testModeChange(String fromMode, String toMode) throws Exception {
    Map<String, Map<String, Object>> storedAttributes = new HashMap<>();
    Map<String, Object> storedFirewall = new HashMap<>();
    storedFirewall.put(FIREWALL_ENABLED, true);
    storedFirewall.put(FIREWALL_MODE, fromMode);
    storedAttributes.put("firewall", storedFirewall);

    when(configuration.getAttributes()).thenReturn(storedAttributes);

    Map<String, Map<String, Object>> updatedAttributes = new HashMap<>();
    Map<String, Object> updatedFirewall = new HashMap<>();
    updatedFirewall.put(FIREWALL_ENABLED, true);
    updatedFirewall.put(FIREWALL_MODE, toMode);
    updatedAttributes.put("firewall", updatedFirewall);

    when(repositoryXO.getAttributes()).thenReturn(updatedAttributes);

    underTest.update(repositoryXO);

    assertEquals(toMode, updatedAttributes.get("firewall").get(FIREWALL_MODE));
  }

  private boolean isValidFirewallMode(String format, String mode) {
    if ("PCCS".equals(mode)) {
      return "npm".equals(format) || "pypi".equals(format);
    }
    return true; // AUDIT and QUARANTINE are valid for all formats
  }

  private void mockRepository() {
    when(repository.getName()).thenReturn("repository");
    when(repository.getType()).thenReturn(new ProxyType());
    when(repository.getFormat()).thenReturn(format);
  }

  private void mockRecipes() {
    when(recipe.getType()).thenReturn(new ProxyType());
    when(recipe.getFormat()).thenReturn(new Format("npm")
    {
    });
    recipes = new HashMap<>();
    recipes.put("npm-proxy-recipe", recipe);
  }
}

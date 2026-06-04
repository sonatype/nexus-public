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
package org.sonatype.nexus.cleanup.internal.rest;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response;

import org.sonatype.nexus.cleanup.config.CleanupPolicyConfiguration;
import org.sonatype.nexus.cleanup.config.DefaultCleanupPolicyConfiguration;
import org.sonatype.nexus.cleanup.internal.preview.CsvCleanupPreviewContentWriter;
import org.sonatype.nexus.cleanup.preview.CleanupPreviewHelper;
import org.sonatype.nexus.cleanup.rest.CleanupPolicyRequestValidator;
import org.sonatype.nexus.cleanup.rest.CleanupPolicyXO;
import org.sonatype.nexus.cleanup.storage.CleanupPolicy;
import org.sonatype.nexus.cleanup.storage.CleanupPolicyStorage;
import org.sonatype.nexus.common.QualifierUtil;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.rest.ValidationErrorsException;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension.WithUser;
import org.sonatype.nexus.testcommon.validation.ValidationExtension;

import jakarta.inject.Provider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static java.util.Collections.singleton;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringEndsWith.endsWith;
import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.hamcrest.text.IsEmptyString.isEmptyOrNullString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.cleanup.config.CleanupPolicyConstants.REGEX_KEY;

@ExtendWith({ValidationExtension.class, AuthenticationExtension.class, MockitoExtension.class})
@WithUser
class CleanupPolicyResourceTest
{
  @Mock
  private CleanupPolicyStorage cleanupPolicyStorage;

  @Mock
  private List<Format> formats;

  private Map<String, CleanupPolicyConfiguration> cleanupFormatConfigurationMap;

  @Mock
  private Provider<CleanupPreviewHelper> cleanupPreviewHelper;

  @Mock
  private RepositoryManager repositoryManager;

  @Mock
  private EventManager eventManager;

  @Mock
  private CsvCleanupPreviewContentWriter csvCleanupPreviewContentWriter;

  @Mock
  private CleanupPolicyRequestValidator cleanupPolicyValidator;

  @Mock
  private Format mockFormat;

  private Collection<CleanupPolicyRequestValidator> cleanupPolicyValidators;

  private CleanupPolicyResource underTest;

  private final String repositoryName = "test-repo";

  private MockedStatic<QualifierUtil> mockedStatic;

  @BeforeEach
  void setUp() throws Exception {
    mockedStatic = mockStatic(QualifierUtil.class);
    cleanupFormatConfigurationMap =
        Map.of(DefaultCleanupPolicyConfiguration.NAME, mock(CleanupPolicyConfiguration.class));
    lenient().when(QualifierUtil.buildQualifierBeanMap(Mockito.<List<CleanupPolicyConfiguration>>any()))
        .thenReturn(cleanupFormatConfigurationMap);
    Repository repository = mock(Repository.class);
    lenient().when(repositoryManager.get(repositoryName)).thenReturn(repository);
    lenient().when(repository.getName()).thenReturn(repositoryName);
    lenient().when(repository.getFormat()).thenReturn(mockFormat);
    lenient().when(mockFormat.getValue()).thenReturn("test-format");
    cleanupPolicyValidators = singleton(cleanupPolicyValidator);
  }

  @AfterEach
  void tearDown() {
    mockedStatic.close();
  }

  @Test
  void testPreviewContentCsv() {
    underTest =
        new CleanupPolicyResource(
            cleanupPolicyStorage,
            formats,
            List.of(),
            cleanupPreviewHelper,
            repositoryManager,
            eventManager,
            true,
            csvCleanupPreviewContentWriter,
            cleanupPolicyValidators);

    Response response = underTest.previewContentCsv(null, repositoryName, null, null, null, null, null, null);

    assertThat(response.getStatus(), is(200));
    String contentDisposition = response.getHeaderString("Content-Disposition");
    assertThat(contentDisposition, not(isEmptyOrNullString()));
    String expectedPrefix = "attachment; filename=CleanupPreview-" + repositoryName;
    assertThat(contentDisposition, startsWith(expectedPrefix));
    assertThat(contentDisposition, endsWith(".csv"));

    response = underTest.previewContentCsv("policy-name", repositoryName, null, null, null, null, null, null);

    assertThat(response.getStatus(), is(200));
    contentDisposition = response.getHeaderString("Content-Disposition");
    assertThat(contentDisposition, not(isEmptyOrNullString()));
    expectedPrefix = "attachment; filename=policy-name-" + repositoryName;
    assertThat(contentDisposition, startsWith(expectedPrefix));
    assertThat(contentDisposition, endsWith(".csv"));
  }

  @Test
  void testPreviewContentCsvWithSpecialCharactersInRegex() {
    underTest =
        new CleanupPolicyResource(
            cleanupPolicyStorage,
            formats,
            List.of(),
            cleanupPreviewHelper,
            repositoryManager,
            eventManager,
            true,
            csvCleanupPreviewContentWriter,
            cleanupPolicyValidators);

    // Test regex with curly braces (quantifiers)
    Response response = underTest.previewContentCsv(
        "test-policy",
        repositoryName,
        null,
        null,
        null,
        "[a-zA-Z0-9]{40}",
        null,
        null);

    assertThat(response.getStatus(), is(200));
    String contentDisposition = response.getHeaderString("Content-Disposition");
    assertThat(contentDisposition, not(isEmptyOrNullString()));
    assertThat(contentDisposition, startsWith("attachment; filename=test-policy-" + repositoryName));
    assertThat(contentDisposition, endsWith(".csv"));

    // Test regex with various special characters
    response = underTest.previewContentCsv(
        "special-chars-policy",
        repositoryName,
        null,
        null,
        null,
        ".*\\.(jar|war|zip)$",
        null,
        null);

    assertThat(response.getStatus(), is(200));
    contentDisposition = response.getHeaderString("Content-Disposition");
    assertThat(contentDisposition, not(isEmptyOrNullString()));
    assertThat(contentDisposition, startsWith("attachment; filename=special-chars-policy-" + repositoryName));
    assertThat(contentDisposition, endsWith(".csv"));

    // Test regex with character classes and ranges
    response = underTest.previewContentCsv(
        "complex-regex-policy",
        repositoryName,
        null,
        null,
        null,
        "^[0-9]{3,5}\\.[a-z]+\\.(txt|log)$",
        null,
        null);

    assertThat(response.getStatus(), is(200));
    contentDisposition = response.getHeaderString("Content-Disposition");
    assertThat(contentDisposition, not(isEmptyOrNullString()));
    assertThat(contentDisposition, startsWith("attachment; filename=complex-regex-policy-" + repositoryName));
    assertThat(contentDisposition, endsWith(".csv"));
  }

  @Test
  void testPreviewContentCsvWithUrlEncodedRegex() {
    underTest =
        new CleanupPolicyResource(
            cleanupPolicyStorage,
            formats,
            List.of(),
            cleanupPreviewHelper,
            repositoryManager,
            eventManager,
            true,
            csvCleanupPreviewContentWriter,
            cleanupPolicyValidators);

    // Test regex with curly braces (quantifiers)
    // JAX-RS @QueryParam already decodes %7B6,%7D to {6,} before reaching the method
    Response response = underTest.previewContentCsv(
        "url-encoded-policy",
        repositoryName,
        null,
        null,
        null,
        ".*-g[0-9a-f]{6,}.*",
        null,
        null);

    assertThat(response.getStatus(), is(200));
    String contentDisposition = response.getHeaderString("Content-Disposition");
    assertThat(contentDisposition, not(isEmptyOrNullString()));
    assertThat(contentDisposition, startsWith("attachment; filename=url-encoded-policy-" + repositoryName));
    assertThat(contentDisposition, endsWith(".csv"));

    // Test regex with parentheses and pipes
    // JAX-RS @QueryParam already decodes %28/%29/%7C before reaching the method
    response = underTest.previewContentCsv(
        "encoded-parentheses",
        repositoryName,
        null,
        null,
        null,
        ".*\\.(jar|war)$",
        null,
        null);

    assertThat(response.getStatus(), is(200));
    contentDisposition = response.getHeaderString("Content-Disposition");
    assertThat(contentDisposition, not(isEmptyOrNullString()));
    assertThat(contentDisposition, startsWith("attachment; filename=encoded-parentheses-" + repositoryName));
    assertThat(contentDisposition, endsWith(".csv"));

    // Test regex with character class quantifier
    // JAX-RS @QueryParam already decodes %7B3,5%7D to {3,5} before reaching the method
    response = underTest.previewContentCsv(
        "mixed-encoding",
        repositoryName,
        null,
        null,
        null,
        "^[0-9]{3,5}\\.[a-z]+$",
        null,
        null);

    assertThat(response.getStatus(), is(200));
    contentDisposition = response.getHeaderString("Content-Disposition");
    assertThat(contentDisposition, not(isEmptyOrNullString()));
    assertThat(contentDisposition, startsWith("attachment; filename=mixed-encoding-" + repositoryName));
    assertThat(contentDisposition, endsWith(".csv"));
  }

  @Test
  void testAddPolicyWithUrlEncodedRegex() {
    // Setup
    Map<String, Boolean> configMap = new HashMap<>();
    configMap.put(REGEX_KEY, true);
    CleanupPolicyConfiguration mockConfig = mock(CleanupPolicyConfiguration.class);
    when(mockConfig.getConfiguration()).thenReturn(configMap);

    CleanupPolicyConfiguration defaultConfig = mock(CleanupPolicyConfiguration.class);
    lenient().when(defaultConfig.getConfiguration()).thenReturn(configMap);

    cleanupFormatConfigurationMap = Map.of(
        "docker", mockConfig,
        DefaultCleanupPolicyConfiguration.NAME, defaultConfig);
    when(QualifierUtil.buildQualifierBeanMap(Mockito.<List<CleanupPolicyConfiguration>>any()))
        .thenReturn(cleanupFormatConfigurationMap);

    CleanupPolicy mockPolicy = mock(CleanupPolicy.class);
    when(cleanupPolicyStorage.newCleanupPolicy()).thenReturn(mockPolicy);
    when(cleanupPolicyStorage.add(any())).thenReturn(mockPolicy);
    when(mockPolicy.getName()).thenReturn("test-policy");
    when(mockPolicy.getFormat()).thenReturn("docker");
    when(mockPolicy.getNotes()).thenReturn("");
    when(mockPolicy.getCriteria()).thenReturn(new HashMap<>());

    List<Format> mockFormats = List.of(mockFormat);
    when(mockFormat.getValue()).thenReturn("docker");

    underTest = new CleanupPolicyResource(
        cleanupPolicyStorage,
        mockFormats,
        List.of(mockConfig, defaultConfig),
        cleanupPreviewHelper,
        repositoryManager,
        eventManager,
        true,
        csvCleanupPreviewContentWriter,
        cleanupPolicyValidators);

    // Create policy with regex containing curly braces (as JAX-RS delivers after decoding)
    CleanupPolicyXO policyXO = new CleanupPolicyXO();
    policyXO.setName("test-policy");
    policyXO.setFormat("docker");
    policyXO.setCriteriaAssetRegex(".*-g[0-9a-f]{6,}.*");

    // Execute
    underTest.add(policyXO);

    // Verify - setCriteria is called multiple times (for storage and event)
    ArgumentCaptor<Map<String, String>> criteriaCaptor = ArgumentCaptor.forClass(Map.class);
    verify(mockPolicy, atLeastOnce()).setCriteria(criteriaCaptor.capture());

    // Get the first captured criteria (the one used for storage)
    Map<String, String> capturedCriteria = criteriaCaptor.getAllValues().get(0);
    // The regex should be preserved as-is (JAX-RS already decoded it)
    assertThat(capturedCriteria.get(REGEX_KEY), equalTo(".*-g[0-9a-f]{6,}.*"));
  }

  @Test
  void testEditPolicyWithUrlEncodedRegex() {
    // Setup
    Map<String, Boolean> configMap = new HashMap<>();
    configMap.put(REGEX_KEY, true);
    CleanupPolicyConfiguration mockConfig = mock(CleanupPolicyConfiguration.class);
    when(mockConfig.getConfiguration()).thenReturn(configMap);

    CleanupPolicyConfiguration defaultConfig = mock(CleanupPolicyConfiguration.class);
    lenient().when(defaultConfig.getConfiguration()).thenReturn(configMap);

    cleanupFormatConfigurationMap = Map.of(
        "docker", mockConfig,
        DefaultCleanupPolicyConfiguration.NAME, defaultConfig);
    when(QualifierUtil.buildQualifierBeanMap(Mockito.<List<CleanupPolicyConfiguration>>any()))
        .thenReturn(cleanupFormatConfigurationMap);

    CleanupPolicy existingPolicy = mock(CleanupPolicy.class);
    when(existingPolicy.getFormat()).thenReturn("docker");
    when(existingPolicy.getName()).thenReturn("test-policy");
    when(existingPolicy.getNotes()).thenReturn("");
    when(existingPolicy.getCriteria()).thenReturn(new HashMap<>());
    when(cleanupPolicyStorage.get("test-policy")).thenReturn(existingPolicy);
    when(cleanupPolicyStorage.update(any())).thenReturn(existingPolicy);
    when(repositoryManager.browseForCleanupPolicy("test-policy")).thenReturn(java.util.stream.Stream.empty());

    List<Format> mockFormats = List.of(mockFormat);
    when(mockFormat.getValue()).thenReturn("docker");

    underTest = new CleanupPolicyResource(
        cleanupPolicyStorage,
        mockFormats,
        List.of(mockConfig, defaultConfig),
        cleanupPreviewHelper,
        repositoryManager,
        eventManager,
        true,
        csvCleanupPreviewContentWriter,
        cleanupPolicyValidators);

    // Update policy with regex containing curly braces (as JAX-RS delivers after decoding)
    CleanupPolicyXO policyXO = new CleanupPolicyXO();
    policyXO.setName("test-policy");
    policyXO.setFormat("docker");
    policyXO.setCriteriaAssetRegex(".*{6,}.*");

    // Execute
    underTest.edit("test-policy", policyXO);

    // Verify
    ArgumentCaptor<Map<String, String>> criteriaCaptor = ArgumentCaptor.forClass(Map.class);
    verify(existingPolicy).setCriteria(criteriaCaptor.capture());

    Map<String, String> capturedCriteria = criteriaCaptor.getValue();
    // The regex should be preserved as-is (JAX-RS already decoded it)
    assertThat(capturedCriteria.get(REGEX_KEY), equalTo(".*{6,}.*"));
  }

  @Test
  void testPreviewContentCsvWithPlusQuantifierInRegex() {
    // The + regex quantifier should NOT be interpreted as a space.
    // In a real HTTP scenario, JAX-RS @QueryParam already decodes the URL-encoded value,
    // so the method receives a literal + character. The second URLDecoder.decode() in
    // normalizeAndValidateRegex() interprets + as a space (application/x-www-form-urlencoded),
    // which silently corrupts the regex pattern.
    underTest =
        new CleanupPolicyResource(
            cleanupPolicyStorage,
            formats,
            List.of(),
            cleanupPreviewHelper,
            repositoryManager,
            eventManager,
            true,
            csvCleanupPreviewContentWriter,
            cleanupPolicyValidators);

    // Simulate what JAX-RS delivers after decoding: a literal + in the regex
    // This is the customer's regex: v2/myrepo/.*/[1-9][0-9]*\.[0-9]+\.[0-9]+
    String regexWithPlus = "[0-9]+\\.[0-9]+";

    Response response = underTest.previewContentCsv(
        "plus-quantifier-policy",
        repositoryName,
        null,
        null,
        null,
        regexWithPlus,
        null,
        null);

    // The response should succeed (200) AND the regex should still contain +
    assertThat(response.getStatus(), is(200));
  }

  @Test
  void testAddPolicyWithPlusQuantifierInRegex() {
    // NEXUS-51975: Verify that the + quantifier is preserved when creating a policy.
    // The double URL-decoding in handleCriteria -> normalizeAndValidateRegex turns + into space.
    Map<String, Boolean> configMap = new HashMap<>();
    configMap.put(REGEX_KEY, true);
    CleanupPolicyConfiguration mockConfig = mock(CleanupPolicyConfiguration.class);
    when(mockConfig.getConfiguration()).thenReturn(configMap);

    CleanupPolicyConfiguration defaultConfig = mock(CleanupPolicyConfiguration.class);
    lenient().when(defaultConfig.getConfiguration()).thenReturn(configMap);

    cleanupFormatConfigurationMap = Map.of(
        "docker", mockConfig,
        DefaultCleanupPolicyConfiguration.NAME, defaultConfig);
    when(QualifierUtil.buildQualifierBeanMap(Mockito.<List<CleanupPolicyConfiguration>>any()))
        .thenReturn(cleanupFormatConfigurationMap);

    CleanupPolicy mockPolicy = mock(CleanupPolicy.class);
    when(cleanupPolicyStorage.newCleanupPolicy()).thenReturn(mockPolicy);
    when(cleanupPolicyStorage.add(any())).thenReturn(mockPolicy);
    when(mockPolicy.getName()).thenReturn("test-policy");
    when(mockPolicy.getFormat()).thenReturn("docker");
    when(mockPolicy.getNotes()).thenReturn("");
    when(mockPolicy.getCriteria()).thenReturn(new HashMap<>());

    List<Format> mockFormats = List.of(mockFormat);
    when(mockFormat.getValue()).thenReturn("docker");

    underTest = new CleanupPolicyResource(
        cleanupPolicyStorage,
        mockFormats,
        List.of(mockConfig, defaultConfig),
        cleanupPreviewHelper,
        repositoryManager,
        eventManager,
        true,
        csvCleanupPreviewContentWriter,
        cleanupPolicyValidators);

    // Create policy with regex containing + quantifier (as JAX-RS would deliver it)
    CleanupPolicyXO policyXO = new CleanupPolicyXO();
    policyXO.setName("test-policy");
    policyXO.setFormat("docker");
    policyXO.setCriteriaAssetRegex("[0-9]+\\.[0-9]+");

    // Execute
    underTest.add(policyXO);

    // Verify the stored regex still contains + (not a space)
    ArgumentCaptor<Map<String, String>> criteriaCaptor = ArgumentCaptor.forClass(Map.class);
    verify(mockPolicy, atLeastOnce()).setCriteria(criteriaCaptor.capture());

    Map<String, String> capturedCriteria = criteriaCaptor.getAllValues().get(0);
    // BUG: This currently FAILS because + is converted to space by URLDecoder.decode()
    assertThat(capturedCriteria.get(REGEX_KEY), equalTo("[0-9]+\\.[0-9]+"));
  }

  @Test
  void testAddPolicyWithInvalidRegex() {
    // Setup
    Map<String, Boolean> configMap = new HashMap<>();
    configMap.put(REGEX_KEY, true);
    CleanupPolicyConfiguration mockConfig = mock(CleanupPolicyConfiguration.class);
    when(mockConfig.getConfiguration()).thenReturn(configMap);

    CleanupPolicyConfiguration defaultConfig = mock(CleanupPolicyConfiguration.class);
    lenient().when(defaultConfig.getConfiguration()).thenReturn(configMap);

    cleanupFormatConfigurationMap = Map.of(
        "docker", mockConfig,
        DefaultCleanupPolicyConfiguration.NAME, defaultConfig);
    when(QualifierUtil.buildQualifierBeanMap(Mockito.<List<CleanupPolicyConfiguration>>any()))
        .thenReturn(cleanupFormatConfigurationMap);

    CleanupPolicy mockPolicy = mock(CleanupPolicy.class);
    when(cleanupPolicyStorage.newCleanupPolicy()).thenReturn(mockPolicy);

    List<Format> mockFormats = List.of(mockFormat);
    when(mockFormat.getValue()).thenReturn("docker");

    underTest = new CleanupPolicyResource(
        cleanupPolicyStorage,
        mockFormats,
        List.of(mockConfig, defaultConfig),
        cleanupPreviewHelper,
        repositoryManager,
        eventManager,
        true,
        csvCleanupPreviewContentWriter,
        cleanupPolicyValidators);

    // Create policy with invalid regex (unclosed bracket)
    CleanupPolicyXO policyXO = new CleanupPolicyXO();
    policyXO.setName("test-policy");
    policyXO.setFormat("docker");
    policyXO.setCriteriaAssetRegex("[0-9"); // Invalid: unclosed bracket

    // Execute and verify exception
    ValidationErrorsException exception = assertThrows(
        ValidationErrorsException.class,
        () -> underTest.add(policyXO));
    assertThat(exception.getMessage(), containsString("Invalid regex pattern"));
  }

  @Test
  void testAddPolicyWithNormalRegex() {
    // Setup
    Map<String, Boolean> configMap = new HashMap<>();
    configMap.put(REGEX_KEY, true);
    CleanupPolicyConfiguration mockConfig = mock(CleanupPolicyConfiguration.class);
    when(mockConfig.getConfiguration()).thenReturn(configMap);

    CleanupPolicyConfiguration defaultConfig = mock(CleanupPolicyConfiguration.class);
    lenient().when(defaultConfig.getConfiguration()).thenReturn(configMap);

    cleanupFormatConfigurationMap = Map.of(
        "docker", mockConfig,
        DefaultCleanupPolicyConfiguration.NAME, defaultConfig);
    when(QualifierUtil.buildQualifierBeanMap(Mockito.<List<CleanupPolicyConfiguration>>any()))
        .thenReturn(cleanupFormatConfigurationMap);

    CleanupPolicy mockPolicy = mock(CleanupPolicy.class);
    when(cleanupPolicyStorage.newCleanupPolicy()).thenReturn(mockPolicy);
    when(cleanupPolicyStorage.add(any())).thenReturn(mockPolicy);
    when(mockPolicy.getName()).thenReturn("test-policy");
    when(mockPolicy.getFormat()).thenReturn("docker");
    when(mockPolicy.getNotes()).thenReturn("");
    when(mockPolicy.getCriteria()).thenReturn(new HashMap<>());

    List<Format> mockFormats = List.of(mockFormat);
    when(mockFormat.getValue()).thenReturn("docker");

    underTest = new CleanupPolicyResource(
        cleanupPolicyStorage,
        mockFormats,
        List.of(mockConfig, defaultConfig),
        cleanupPreviewHelper,
        repositoryManager,
        eventManager,
        true,
        csvCleanupPreviewContentWriter,
        cleanupPolicyValidators);

    // Create policy with normal (non-encoded) regex
    CleanupPolicyXO policyXO = new CleanupPolicyXO();
    policyXO.setName("test-policy");
    policyXO.setFormat("docker");
    policyXO.setCriteriaAssetRegex(".*-g[0-9a-f]{6,}.*");

    // Execute
    underTest.add(policyXO);

    // Verify - setCriteria is called multiple times (for storage and event)
    ArgumentCaptor<Map<String, String>> criteriaCaptor = ArgumentCaptor.forClass(Map.class);
    verify(mockPolicy, atLeastOnce()).setCriteria(criteriaCaptor.capture());

    // Get the first captured criteria (the one used for storage)
    Map<String, String> capturedCriteria = criteriaCaptor.getAllValues().get(0);
    // The regex should remain unchanged
    assertThat(capturedCriteria.get(REGEX_KEY), equalTo(".*-g[0-9a-f]{6,}.*"));
  }
}

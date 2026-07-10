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
import java.util.Set;
import java.util.stream.Stream;

import java.io.ByteArrayOutputStream;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;

import org.sonatype.nexus.extdirect.model.PagedResponse;
import org.sonatype.nexus.repository.rest.api.ComponentXO;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.cleanup.config.CleanupPolicyConstants.LAST_BLOB_UPDATED_KEY;
import static org.sonatype.nexus.cleanup.config.CleanupPolicyConstants.LAST_DOWNLOADED_KEY;
import static org.sonatype.nexus.cleanup.config.CleanupPolicyConstants.REGEX_KEY;
import static org.sonatype.nexus.cleanup.config.CleanupPolicyConstants.RETAIN_KEY;
import static org.sonatype.nexus.cleanup.config.CleanupPolicyConstants.RETAIN_SORT_BY_KEY;

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

  @Mock
  private CleanupPolicyRepositoryAssociator repositoryAssociator;

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
            cleanupPolicyValidators,
            repositoryAssociator,
            true);

    Response response = underTest.previewContentCsv(null, repositoryName, null, null, null, null, null, null);

    assertThat(response.getStatus(), is(200));
    String contentDisposition = response.getHeaderString("Content-Disposition");
    assertThat(contentDisposition, not(isEmptyOrNullString()));
    String expectedPrefix = "attachment; filename=\"CleanupPreview-" + repositoryName;
    assertThat(contentDisposition, startsWith(expectedPrefix));
    assertThat(contentDisposition, endsWith(".csv\""));

    response = underTest.previewContentCsv("policy-name", repositoryName, null, null, null, null, null, null);

    assertThat(response.getStatus(), is(200));
    contentDisposition = response.getHeaderString("Content-Disposition");
    assertThat(contentDisposition, not(isEmptyOrNullString()));
    expectedPrefix = "attachment; filename=\"policy-name-" + repositoryName;
    assertThat(contentDisposition, startsWith(expectedPrefix));
    assertThat(contentDisposition, endsWith(".csv\""));
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
            cleanupPolicyValidators,
            repositoryAssociator,
            true);

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
    assertThat(contentDisposition, startsWith("attachment; filename=\"test-policy-" + repositoryName));
    assertThat(contentDisposition, endsWith(".csv\""));

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
    assertThat(contentDisposition, startsWith("attachment; filename=\"special-chars-policy-" + repositoryName));
    assertThat(contentDisposition, endsWith(".csv\""));

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
    assertThat(contentDisposition, startsWith("attachment; filename=\"complex-regex-policy-" + repositoryName));
    assertThat(contentDisposition, endsWith(".csv\""));
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
            cleanupPolicyValidators,
            repositoryAssociator,
            true);

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
    assertThat(contentDisposition, startsWith("attachment; filename=\"url-encoded-policy-" + repositoryName));
    assertThat(contentDisposition, endsWith(".csv\""));

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
    assertThat(contentDisposition, startsWith("attachment; filename=\"encoded-parentheses-" + repositoryName));
    assertThat(contentDisposition, endsWith(".csv\""));

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
    assertThat(contentDisposition, startsWith("attachment; filename=\"mixed-encoding-" + repositoryName));
    assertThat(contentDisposition, endsWith(".csv\""));
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
        cleanupPolicyValidators,
        repositoryAssociator,
        true);

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
        cleanupPolicyValidators,
        repositoryAssociator,
        true);

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
            cleanupPolicyValidators,
            repositoryAssociator,
            true);

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
        cleanupPolicyValidators,
        repositoryAssociator,
        true);

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
        cleanupPolicyValidators,
        repositoryAssociator,
        true);

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
        cleanupPolicyValidators,
        repositoryAssociator,
        true);

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

  @Test
  void testPreviewContentCsvSanitizesMaliciousFilename() {
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
            cleanupPolicyValidators,
            repositoryAssociator,
            true);

    // Test with semicolon injection attempt (reflected file download)
    Response response = underTest.previewContentCsv(
        "setup.bat;ignore",
        repositoryName,
        null,
        null,
        null,
        null,
        null,
        null);

    assertThat(response.getStatus(), is(200));
    String contentDisposition = response.getHeaderString("Content-Disposition");
    assertThat(contentDisposition, not(isEmptyOrNullString()));
    // Semicolon should be removed, resulting in "setup.batignore"
    assertThat(contentDisposition, startsWith("attachment; filename=\"setup.batignore-" + repositoryName));
    assertThat(contentDisposition, endsWith(".csv\""));
    // Ensure there's no unquoted semicolon that could break the parameter
    assertThat(contentDisposition, not(containsString(";ignore")));
    assertThat(contentDisposition, not(containsString(".bat;")));

    // Test with quote injection attempt
    response = underTest.previewContentCsv(
        "test\"file",
        repositoryName,
        null,
        null,
        null,
        null,
        null,
        null);

    assertThat(response.getStatus(), is(200));
    contentDisposition = response.getHeaderString("Content-Disposition");
    assertThat(contentDisposition, not(isEmptyOrNullString()));
    // Quote should be removed
    assertThat(contentDisposition, startsWith("attachment; filename=\"testfile-" + repositoryName));
    assertThat(contentDisposition, endsWith(".csv\""));

    // Test with backslash injection attempt
    response = underTest.previewContentCsv(
        "test\\file",
        repositoryName,
        null,
        null,
        null,
        null,
        null,
        null);

    assertThat(response.getStatus(), is(200));
    contentDisposition = response.getHeaderString("Content-Disposition");
    assertThat(contentDisposition, not(isEmptyOrNullString()));
    // Backslash should be removed
    assertThat(contentDisposition, startsWith("attachment; filename=\"testfile-" + repositoryName));
    assertThat(contentDisposition, endsWith(".csv\""));
  }

  // ---------------------------------------------------------------------------
  // Embedded `repositories` field on add()/edit() — Option A refactor coverage.
  // ---------------------------------------------------------------------------

  /**
   * Build a resource instance configured for the supported format "npm" with the
   * CLEANUP_RETAIN_ALL_FORMATS feature flag enabled. Used by the embedded
   * `repositories`-field tests below.
   */
  private CleanupPolicyResource newResourceForNpm(final boolean retainAllFormatsEnabled) {
    Map<String, Boolean> configMap = new HashMap<>();
    configMap.put(REGEX_KEY, true);
    CleanupPolicyConfiguration npmConfig = mock(CleanupPolicyConfiguration.class);
    lenient().when(npmConfig.getConfiguration()).thenReturn(configMap);
    CleanupPolicyConfiguration defaultConfig = mock(CleanupPolicyConfiguration.class);
    lenient().when(defaultConfig.getConfiguration()).thenReturn(configMap);
    cleanupFormatConfigurationMap = Map.of(
        "npm", npmConfig,
        DefaultCleanupPolicyConfiguration.NAME, defaultConfig);
    when(QualifierUtil.buildQualifierBeanMap(Mockito.<List<CleanupPolicyConfiguration>>any()))
        .thenReturn(cleanupFormatConfigurationMap);
    lenient().when(mockFormat.getValue()).thenReturn("npm");
    List<Format> mockFormats = List.of(mockFormat);
    return new CleanupPolicyResource(
        cleanupPolicyStorage,
        mockFormats,
        List.of(npmConfig, defaultConfig),
        cleanupPreviewHelper,
        repositoryManager,
        eventManager,
        true,
        csvCleanupPreviewContentWriter,
        cleanupPolicyValidators,
        repositoryAssociator,
        retainAllFormatsEnabled);
  }

  private CleanupPolicy stubStoredPolicy(final String name, final String format) {
    CleanupPolicy stored = mock(CleanupPolicy.class);
    lenient().when(stored.getName()).thenReturn(name);
    lenient().when(stored.getFormat()).thenReturn(format);
    lenient().when(stored.getNotes()).thenReturn("");
    lenient().when(stored.getCriteria()).thenReturn(new HashMap<>());
    return stored;
  }

  @Test
  void testAddWithEmbeddedRepositoriesInvokesAssociator() {
    underTest = newResourceForNpm(true);

    CleanupPolicy stored = stubStoredPolicy("p1", "npm");
    when(cleanupPolicyStorage.newCleanupPolicy()).thenReturn(stored);
    when(cleanupPolicyStorage.add(any())).thenReturn(stored);

    CleanupPolicyXO xo = new CleanupPolicyXO();
    xo.setName("p1");
    xo.setFormat("npm");
    xo.setRepositories(List.of("repo-a", "repo-b"));

    underTest.add(xo);

    verify(repositoryAssociator).updateRepositoriesForPolicy(
        eq("p1"), eq("npm"), eq(Set.of("repo-a", "repo-b")));
  }

  @Test
  void testAddWithNullRepositoriesDoesNotInvokeAssociator() {
    underTest = newResourceForNpm(true);

    CleanupPolicy stored = stubStoredPolicy("p1", "npm");
    when(cleanupPolicyStorage.newCleanupPolicy()).thenReturn(stored);
    when(cleanupPolicyStorage.add(any())).thenReturn(stored);

    CleanupPolicyXO xo = new CleanupPolicyXO();
    xo.setName("p1");
    xo.setFormat("npm");
    // repositories deliberately omitted -> preserve existing attachments

    underTest.add(xo);

    verify(repositoryAssociator, org.mockito.Mockito.never())
        .updateRepositoriesForPolicy(any(), any(), any());
  }

  @Test
  void testAddWithEmptyRepositoriesIsNoOp() {
    underTest = newResourceForNpm(true);

    CleanupPolicy stored = stubStoredPolicy("p1", "npm");
    when(cleanupPolicyStorage.newCleanupPolicy()).thenReturn(stored);
    when(cleanupPolicyStorage.add(any())).thenReturn(stored);

    CleanupPolicyXO xo = new CleanupPolicyXO();
    xo.setName("p1");
    xo.setFormat("npm");
    xo.setRepositories(List.of());

    underTest.add(xo);

    // Empty list is treated as "no attachment change" and must not call the
    // associator. Detachment of existing attachments is an explicit operation
    // through the dedicated /{name}/repositories endpoint.
    verify(repositoryAssociator, org.mockito.Mockito.never())
        .updateRepositoriesForPolicy(any(), any(), any());
  }

  @Test
  void testAddRejectsRepositoriesWhenFeatureFlagDisabled() {
    underTest = newResourceForNpm(false);

    CleanupPolicyXO xo = new CleanupPolicyXO();
    xo.setName("p1");
    xo.setFormat("npm");
    xo.setRepositories(List.of("repo-a"));

    assertThrows(ValidationErrorsException.class, () -> underTest.add(xo));
    verify(cleanupPolicyStorage, org.mockito.Mockito.never()).add(any());
    verify(repositoryAssociator, org.mockito.Mockito.never())
        .updateRepositoriesForPolicy(any(), any(), any());
  }

  @Test
  void testAddRejectsRepositoriesForUnsupportedFormat() {
    // maven2 is not in REPOSITORIES_FIELD_SUPPORTED_FORMATS.
    Map<String, Boolean> configMap = new HashMap<>();
    configMap.put(REGEX_KEY, true);
    CleanupPolicyConfiguration mavenConfig = mock(CleanupPolicyConfiguration.class);
    lenient().when(mavenConfig.getConfiguration()).thenReturn(configMap);
    CleanupPolicyConfiguration defaultConfig = mock(CleanupPolicyConfiguration.class);
    lenient().when(defaultConfig.getConfiguration()).thenReturn(configMap);
    cleanupFormatConfigurationMap = Map.of(
        "maven2", mavenConfig,
        DefaultCleanupPolicyConfiguration.NAME, defaultConfig);
    when(QualifierUtil.buildQualifierBeanMap(Mockito.<List<CleanupPolicyConfiguration>>any()))
        .thenReturn(cleanupFormatConfigurationMap);
    lenient().when(mockFormat.getValue()).thenReturn("maven2");
    underTest = new CleanupPolicyResource(
        cleanupPolicyStorage,
        List.of(mockFormat),
        List.of(mavenConfig, defaultConfig),
        cleanupPreviewHelper,
        repositoryManager,
        eventManager,
        true,
        csvCleanupPreviewContentWriter,
        cleanupPolicyValidators,
        repositoryAssociator,
        true);

    CleanupPolicyXO xo = new CleanupPolicyXO();
    xo.setName("p1");
    xo.setFormat("maven2");
    xo.setRepositories(List.of("repo-a"));

    assertThrows(ValidationErrorsException.class, () -> underTest.add(xo));
    verify(cleanupPolicyStorage, org.mockito.Mockito.never()).add(any());
    verify(repositoryAssociator, org.mockito.Mockito.never())
        .updateRepositoriesForPolicy(any(), any(), any());
  }

  @Test
  void testAddCompensatesByDeletingPolicyWhenAttachmentFails() {
    underTest = newResourceForNpm(true);

    CleanupPolicy stored = stubStoredPolicy("p1", "npm");
    when(cleanupPolicyStorage.newCleanupPolicy()).thenReturn(stored);
    when(cleanupPolicyStorage.add(any())).thenReturn(stored);
    RuntimeException attachFailure = new RuntimeException("attach boom");
    org.mockito.Mockito.doThrow(attachFailure)
        .when(repositoryAssociator)
        .updateRepositoriesForPolicy(eq("p1"), eq("npm"), any());

    CleanupPolicyXO xo = new CleanupPolicyXO();
    xo.setName("p1");
    xo.setFormat("npm");
    xo.setRepositories(List.of("repo-a"));

    RuntimeException thrown = assertThrows(RuntimeException.class, () -> underTest.add(xo));
    assertThat(thrown.getMessage(), containsString("attach boom"));
    // Compensating delete must have been invoked on the orphan policy.
    verify(cleanupPolicyStorage).remove(stored);
  }

  @Test
  void testEditWithEmbeddedRepositoriesInvokesAssociator() {
    underTest = newResourceForNpm(true);

    CleanupPolicy existing = stubStoredPolicy("p1", "npm");
    when(cleanupPolicyStorage.get("p1")).thenReturn(existing);
    when(cleanupPolicyStorage.update(any())).thenReturn(existing);
    lenient().when(repositoryManager.browseForCleanupPolicy("p1")).thenReturn(Stream.empty());

    CleanupPolicyXO xo = new CleanupPolicyXO();
    xo.setName("p1");
    xo.setFormat("npm");
    xo.setRepositories(List.of("repo-a"));

    underTest.edit("p1", xo);

    verify(repositoryAssociator).updateRepositoriesForPolicy(
        eq("p1"), eq("npm"), eq(Set.of("repo-a")));
  }

  @Test
  void testEditWithEmptyRepositoriesIsNoOp() {
    underTest = newResourceForNpm(true);

    CleanupPolicy existing = stubStoredPolicy("p1", "npm");
    when(cleanupPolicyStorage.get("p1")).thenReturn(existing);
    when(cleanupPolicyStorage.update(any())).thenReturn(existing);
    lenient().when(repositoryManager.browseForCleanupPolicy("p1")).thenReturn(Stream.empty());

    CleanupPolicyXO xo = new CleanupPolicyXO();
    xo.setName("p1");
    xo.setFormat("npm");
    xo.setRepositories(List.of());

    underTest.edit("p1", xo);

    // Empty list is treated as "no attachment change" and must not call the
    // associator. Detachment of existing attachments is an explicit operation
    // through the dedicated /{name}/repositories endpoint.
    verify(repositoryAssociator, org.mockito.Mockito.never())
        .updateRepositoriesForPolicy(any(), any(), any());
  }

  @Test
  void testEditRejectsRepositoriesWhenFeatureFlagDisabled() {
    underTest = newResourceForNpm(false);

    CleanupPolicy existing = stubStoredPolicy("p1", "npm");
    when(cleanupPolicyStorage.get("p1")).thenReturn(existing);
    lenient().when(repositoryManager.browseForCleanupPolicy("p1")).thenReturn(Stream.empty());

    CleanupPolicyXO xo = new CleanupPolicyXO();
    xo.setName("p1");
    xo.setFormat("npm");
    xo.setRepositories(List.of("repo-a"));

    assertThrows(ValidationErrorsException.class, () -> underTest.edit("p1", xo));
    verify(cleanupPolicyStorage, org.mockito.Mockito.never()).update(any());
    verify(repositoryAssociator, org.mockito.Mockito.never())
        .updateRepositoriesForPolicy(any(), any(), any());
  }

  @Test
  void testGetByNamePopulatesRepositoriesFromAssociator() {
    underTest = newResourceForNpm(true);

    CleanupPolicy existing = stubStoredPolicy("p1", "npm");
    when(cleanupPolicyStorage.get("p1")).thenReturn(existing);
    lenient().when(repositoryManager.browseForCleanupPolicy("p1")).thenReturn(Stream.empty());
    when(repositoryAssociator.getRepositoriesForPolicy("p1", "npm"))
        .thenReturn(Set.of("repo-a", "repo-b"));

    CleanupPolicyXO xo = underTest.getByName("p1");

    assertThat(xo.getRepositories(), org.hamcrest.Matchers.containsInAnyOrder("repo-a", "repo-b"));
  }

  // ---------------------------------------------------------------------------
  // Additional coverage: list / criteria-formats / preview / delete /
  // not-found branches / repositories endpoints.
  // ---------------------------------------------------------------------------

  private CleanupPolicyResource newResource(
      final List<Format> fmts,
      final boolean previewEnabled,
      final boolean retainAll)
  {
    return new CleanupPolicyResource(
        cleanupPolicyStorage,
        fmts,
        List.of(),
        cleanupPreviewHelper,
        repositoryManager,
        eventManager,
        previewEnabled,
        csvCleanupPreviewContentWriter,
        cleanupPolicyValidators,
        repositoryAssociator,
        retainAll);
  }

  private CleanupPolicy policyMock(final String name, final String format) {
    CleanupPolicy p = mock(CleanupPolicy.class);
    lenient().when(p.getName()).thenReturn(name);
    lenient().when(p.getFormat()).thenReturn(format);
    lenient().when(p.getNotes()).thenReturn("");
    lenient().when(p.getCriteria()).thenReturn(new HashMap<>());
    return p;
  }

  @Test
  void testGetReturnsAllPoliciesSorted() {
    underTest = newResource(formats, true, true);
    CleanupPolicy pb = policyMock("b", "npm");
    CleanupPolicy pa = policyMock("a", "npm");
    when(cleanupPolicyStorage.getAll()).thenReturn(List.of(pb, pa));
    lenient().when(repositoryManager.browseForCleanupPolicy(any())).thenAnswer(inv -> Stream.empty());
    lenient().when(repositoryAssociator.getRepositoriesForPolicy(any(), any())).thenReturn(Set.of());

    List<CleanupPolicyXO> result = underTest.get(null);

    assertThat(result.size(), is(2));
    assertThat(result.get(0).getName(), is("a"));
  }

  @Test
  void testGetByFormatUsesGetAllByFormat() {
    underTest = newResource(formats, true, true);
    CleanupPolicy pnpm = policyMock("p", "npm");
    when(cleanupPolicyStorage.getAllByFormat("npm")).thenReturn(List.of(pnpm));
    lenient().when(repositoryManager.browseForCleanupPolicy(any())).thenAnswer(inv -> Stream.empty());
    lenient().when(repositoryAssociator.getRepositoriesForPolicy(any(), any())).thenReturn(Set.of());

    assertThat(underTest.get("npm").size(), is(1));
  }

  @Test
  void testGetByNameNotFound() {
    underTest = newResource(formats, true, true);
    when(cleanupPolicyStorage.get("missing")).thenReturn(null);

    assertThrows(NotFoundException.class, () -> underTest.getByName("missing"));
  }

  @Test
  void testDeleteRemovesPolicyAndPostsEvent() {
    underTest = newResource(formats, true, true);
    CleanupPolicy policy = policyMock("p", "npm");
    when(cleanupPolicyStorage.get("p")).thenReturn(policy);

    underTest.delete("p");

    verify(cleanupPolicyStorage).remove(policy);
    verify(eventManager).post(any());
  }

  @Test
  void testDeleteNotFound() {
    underTest = newResource(formats, true, true);
    when(cleanupPolicyStorage.get("missing")).thenReturn(null);

    assertThrows(NotFoundException.class, () -> underTest.delete("missing"));
  }

  @Test
  void testEditNotFound() {
    underTest = newResource(formats, true, true);
    when(cleanupPolicyStorage.get("missing")).thenReturn(null);

    CleanupPolicyXO xo = new CleanupPolicyXO();
    xo.setName("missing");
    xo.setFormat("npm");

    assertThrows(NotFoundException.class, () -> underTest.edit("missing", xo));
  }

  @Test
  void testEditRejectsInvalidFormat() {
    // formats is an empty @Mock list -> only ALL_FORMATS is valid.
    underTest = newResource(formats, true, true);
    CleanupPolicy p = policyMock("p", "npm");
    when(cleanupPolicyStorage.get("p")).thenReturn(p);

    CleanupPolicyXO xo = new CleanupPolicyXO();
    xo.setName("p");
    xo.setFormat("bogus");

    assertThrows(ValidationErrorsException.class, () -> underTest.edit("p", xo));
  }

  @Test
  void testEditRejectsFormatChangeWhileInUse() {
    Format docker = mock(Format.class);
    lenient().when(docker.getValue()).thenReturn("docker");
    Format npm = mock(Format.class);
    lenient().when(npm.getValue()).thenReturn("npm");
    underTest = newResource(List.of(docker, npm), true, true);

    CleanupPolicy existing = policyMock("p", "docker");
    when(cleanupPolicyStorage.get("p")).thenReturn(existing);
    when(repositoryManager.browseForCleanupPolicy("p")).thenReturn(Stream.of(mock(Repository.class)));

    CleanupPolicyXO xo = new CleanupPolicyXO();
    xo.setName("p");
    xo.setFormat("npm"); // different but valid format while in use -> reject

    assertThrows(ValidationErrorsException.class, () -> underTest.edit("p", xo));
  }

  @Test
  void testAddRejectsInvalidFormat() {
    underTest = newResource(formats, true, true); // empty formats -> only "*" valid

    CleanupPolicyXO xo = new CleanupPolicyXO();
    xo.setName("p");
    xo.setFormat("npm");

    assertThrows(ValidationErrorsException.class, () -> underTest.add(xo));
  }

  @Test
  void testPreviewContentReturnsResults() {
    underTest = newResource(formats, true, true);
    PreviewRequestXO req = mock(PreviewRequestXO.class);
    lenient().when(req.getRepository()).thenReturn(repositoryName);

    CleanupPreviewHelper helper = mock(CleanupPreviewHelper.class);
    when(cleanupPreviewHelper.get()).thenReturn(helper);
    PagedResponse<ComponentXO> paged = mock(PagedResponse.class);
    lenient().when(paged.getTotal()).thenReturn(3L);
    lenient().when(paged.getData()).thenReturn(List.of());
    when(helper.getSearchResults(any(), any(), any())).thenReturn(paged);

    assertThat(underTest.previewContent(req) != null, is(true));
  }

  @Test
  void testPreviewContentRepositoryNotFound() {
    underTest = newResource(formats, true, true);
    PreviewRequestXO req = mock(PreviewRequestXO.class);
    when(req.getRepository()).thenReturn("missing");

    assertThrows(NotFoundException.class, () -> underTest.previewContent(req));
  }

  @Test
  void testPreviewContentInvalidFilterBecomesValidationError() {
    underTest = newResource(formats, true, true);
    PreviewRequestXO req = mock(PreviewRequestXO.class);
    lenient().when(req.getRepository()).thenReturn(repositoryName);

    CleanupPreviewHelper helper = mock(CleanupPreviewHelper.class);
    when(cleanupPreviewHelper.get()).thenReturn(helper);
    when(helper.getSearchResults(any(), any(), any())).thenThrow(new IllegalArgumentException("bad filter"));

    assertThrows(ValidationErrorsException.class, () -> underTest.previewContent(req));
  }

  @Test
  void testPreviewContentCsvReturns404WhenPreviewDisabled() {
    underTest = newResource(formats, false, true);

    Response response = underTest.previewContentCsv(null, repositoryName, null, null, null, null, null, null);

    assertThat(response.getStatus(), is(Response.Status.NOT_FOUND.getStatusCode()));
  }

  @Test
  void testPreviewContentCsvRepositoryNotFound() {
    underTest = newResource(formats, true, true);

    assertThrows(NotFoundException.class,
        () -> underTest.previewContentCsv(null, "missing-repo", null, null, null, null, null, null));
  }

  @Test
  void testGetRepositoriesForPolicyNotFound() {
    underTest = newResource(formats, true, true);
    when(cleanupPolicyStorage.get("missing")).thenReturn(null);

    assertThrows(NotFoundException.class, () -> underTest.getRepositoriesForPolicy("missing"));
  }

  @Test
  void testGetRepositoriesForPolicyFiltersByFormat() {
    underTest = newResource(formats, true, true);
    CleanupPolicy p = policyMock("p", "npm");
    when(cleanupPolicyStorage.get("p")).thenReturn(p);

    Repository matching = mock(Repository.class);
    Format npmFmt = mock(Format.class);
    lenient().when(npmFmt.getValue()).thenReturn("npm");
    org.sonatype.nexus.repository.Type hostedType = mock(org.sonatype.nexus.repository.Type.class);
    lenient().when(matching.getName()).thenReturn("repo-npm");
    lenient().when(matching.getFormat()).thenReturn(npmFmt);
    lenient().when(matching.getType()).thenReturn(hostedType);
    lenient().when(hostedType.getValue()).thenReturn("hosted");

    Repository other = mock(Repository.class);
    Format mavenFmt = mock(Format.class);
    lenient().when(mavenFmt.getValue()).thenReturn("maven2");
    lenient().when(other.getFormat()).thenReturn(mavenFmt);

    when(repositoryManager.browse()).thenReturn(List.of(matching, other));
    lenient().when(repositoryAssociator.repositoryHasPolicy(matching, "p")).thenReturn(true);
    lenient().when(repositoryAssociator.repositoryHasPolicy(other, "p")).thenReturn(true);

    List<CleanupPolicyRepositoryXO> result = underTest.getRepositoriesForPolicy("p");

    assertThat(result.size(), is(1));
    assertThat(result.get(0).getName(), is("repo-npm"));
  }

  @Test
  void testUpdateRepositoriesForPolicyNotFound() {
    underTest = newResource(formats, true, true);
    when(cleanupPolicyStorage.get("missing")).thenReturn(null);
    CleanupPolicyRepositoriesRequestXO req = mock(CleanupPolicyRepositoriesRequestXO.class);

    assertThrows(NotFoundException.class, () -> underTest.updateRepositoriesForPolicy("missing", req));
  }

  @Test
  void testUpdateRepositoriesForPolicyAppliesRequested() {
    underTest = newResource(formats, true, true);
    CleanupPolicy p = policyMock("p", "npm");
    when(cleanupPolicyStorage.get("p")).thenReturn(p);
    CleanupPolicyRepositoriesRequestXO req = mock(CleanupPolicyRepositoriesRequestXO.class);
    when(req.getRepositories()).thenReturn(List.of("r1", "r2"));

    underTest.updateRepositoriesForPolicy("p", req);

    verify(repositoryAssociator).updateRepositoriesForPolicy("p", "npm", Set.of("r1", "r2"));
  }

  @Test
  void testUpdateRepositoriesForPolicyNullRepositoriesUsesEmptySet() {
    underTest = newResource(formats, true, true);
    CleanupPolicy p = policyMock("p", "npm");
    when(cleanupPolicyStorage.get("p")).thenReturn(p);
    CleanupPolicyRepositoriesRequestXO req = mock(CleanupPolicyRepositoriesRequestXO.class);
    when(req.getRepositories()).thenReturn(null);

    underTest.updateRepositoriesForPolicy("p", req);

    verify(repositoryAssociator).updateRepositoriesForPolicy("p", "npm", Set.of());
  }

  @Test
  void testPreviewContentCsvStreamsOutputAndPostsDryRunEvent() throws Exception {
    underTest = newResource(formats, true, true);

    CleanupPreviewHelper helper = mock(CleanupPreviewHelper.class);
    lenient().when(cleanupPreviewHelper.get()).thenReturn(helper);
    lenient().when(helper.getSearchResultsStream(any(), any(), any())).thenAnswer(inv -> Stream.empty());

    Response response =
        underTest.previewContentCsv("policy", repositoryName, 1, 2, null, null, null, null);
    assertThat(response.getStatus(), is(200));

    // Consume the streaming body so the lambda (search + CSV write + dry-run event) executes.
    StreamingOutput body = (StreamingOutput) response.getEntity();
    body.write(new ByteArrayOutputStream());

    verify(csvCleanupPreviewContentWriter).write(any(), any(), any());
    verify(eventManager).post(any());
  }

  @Test
  void testGetCriteriaForFormats() {
    Map<String, Boolean> cfg = new HashMap<>();
    cfg.put(LAST_BLOB_UPDATED_KEY, true);
    cfg.put(LAST_DOWNLOADED_KEY, true);
    CleanupPolicyConfiguration defaultCfg = mock(CleanupPolicyConfiguration.class);
    lenient().when(defaultCfg.getConfiguration()).thenReturn(cfg);
    when(QualifierUtil.buildQualifierBeanMap(Mockito.<List<CleanupPolicyConfiguration>>any()))
        .thenReturn(Map.of(DefaultCleanupPolicyConfiguration.NAME, defaultCfg));
    when(mockFormat.getValue()).thenReturn("docker");

    underTest = new CleanupPolicyResource(
        cleanupPolicyStorage, List.of(mockFormat), List.of(defaultCfg), cleanupPreviewHelper,
        repositoryManager, eventManager, true, csvCleanupPreviewContentWriter,
        cleanupPolicyValidators, repositoryAssociator, true);

    List<CleanupPolicyFormatXO> result = underTest.getCriteriaForFormats();

    // docker + the prepended "All Formats" entry.
    assertThat(result.size(), is(2));
  }

  @Test
  void testAddPersistsRetainSortByAndReleaseTypeCriteria() {
    Map<String, Boolean> cfg = new HashMap<>();
    cfg.put(LAST_BLOB_UPDATED_KEY, true);
    cfg.put(LAST_DOWNLOADED_KEY, true);
    cfg.put(REGEX_KEY, true);
    cfg.put(RETAIN_KEY, true);
    cfg.put(RETAIN_SORT_BY_KEY, true);
    cfg.put("isPrerelease", true);
    CleanupPolicyConfiguration dockerCfg = mock(CleanupPolicyConfiguration.class);
    lenient().when(dockerCfg.getConfiguration()).thenReturn(cfg);
    CleanupPolicyConfiguration defaultCfg = mock(CleanupPolicyConfiguration.class);
    lenient().when(defaultCfg.getConfiguration()).thenReturn(cfg);
    when(QualifierUtil.buildQualifierBeanMap(Mockito.<List<CleanupPolicyConfiguration>>any()))
        .thenReturn(Map.of("docker", dockerCfg, DefaultCleanupPolicyConfiguration.NAME, defaultCfg));
    when(mockFormat.getValue()).thenReturn("docker");

    CleanupPolicy stored = policyMock("p", "docker");
    when(cleanupPolicyStorage.newCleanupPolicy()).thenReturn(stored);
    when(cleanupPolicyStorage.add(any())).thenReturn(stored);

    underTest = new CleanupPolicyResource(
        cleanupPolicyStorage, List.of(mockFormat), List.of(dockerCfg, defaultCfg), cleanupPreviewHelper,
        repositoryManager, eventManager, true, csvCleanupPreviewContentWriter,
        cleanupPolicyValidators, repositoryAssociator, true);

    CleanupPolicyXO xo = new CleanupPolicyXO();
    xo.setName("p");
    xo.setFormat("docker");
    xo.setCriteriaLastBlobUpdated(1L);
    xo.setCriteriaLastDownloaded(2L);
    xo.setRetain(3);
    xo.setSortBy("version");

    ArgumentCaptor<Map<String, String>> captor = ArgumentCaptor.forClass(Map.class);
    underTest.add(xo);

    verify(stored, atLeastOnce()).setCriteria(captor.capture());
    Map<String, String> criteria = captor.getAllValues().get(0);
    assertThat(criteria.get(RETAIN_KEY), is("3"));
    assertThat(criteria.get(RETAIN_SORT_BY_KEY), is("version"));
  }
}

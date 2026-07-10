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
package org.sonatype.nexus.cleanup.internal.preview;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.rest.api.AssetXO;
import org.sonatype.nexus.repository.rest.api.ComponentXO;
import org.sonatype.nexus.repository.rest.api.DefaultComponentXO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.lenient;

/**
 * Tests for CSV formula injection prevention in {@link CsvCleanupPreviewContentWriter}.
 */
@ExtendWith(MockitoExtension.class)
class CsvCleanupPreviewContentWriterTest
{
  private CsvCleanupPreviewContentWriter underTest;

  @Mock
  private Repository repository;

  @BeforeEach
  void setUp() {
    underTest = new CsvCleanupPreviewContentWriter();
    lenient().when(repository.getName()).thenReturn("test-repo");
  }

  @Test
  void testFormulaInjectionPreventionWithEquals() throws IOException {
    // Test = prefix (most common formula injection)
    ComponentXO component = new DefaultComponentXO();
    component.setGroup("=cmd|' /C calc'!A0"); // Formula injection attempt
    component.setName("normal-name");
    component.setVersion("1.0.0");

    AssetXO asset = new AssetXO();
    asset.setPath("/test/path");
    asset.setBlobStoreName("default");
    asset.setFileSize(1024L);
    asset.setLastDownloaded(new Date());
    asset.setBlobCreated(new Date());
    component.setAssets(List.of(asset));

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    underTest.write(repository, Stream.of(component), outputStream);

    String csvContent = outputStream.toString(StandardCharsets.UTF_8);

    // Should be escaped with leading single quote
    assertThat("Field with = prefix should be escaped",
        csvContent, containsString("'=cmd|' /C calc'!A0"));
  }

  @Test
  void testFormulaInjectionPreventionWithPlus() throws IOException {
    // Test + prefix
    ComponentXO component = new DefaultComponentXO();
    component.setGroup("normal-group");
    component.setName("+HYPERLINK(\"http://evil.com\")");
    component.setVersion("1.0.0");

    AssetXO asset = new AssetXO();
    asset.setPath("/test/path");
    asset.setBlobStoreName("default");
    asset.setFileSize(1024L);
    asset.setLastDownloaded(new Date());
    asset.setBlobCreated(new Date());
    component.setAssets(List.of(asset));

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    underTest.write(repository, Stream.of(component), outputStream);

    String csvContent = outputStream.toString(StandardCharsets.UTF_8);

    assertThat("Field with + prefix should be escaped",
        csvContent, containsString("'+HYPERLINK"));
  }

  @Test
  void testFormulaInjectionPreventionWithMinus() throws IOException {
    // Test - prefix
    ComponentXO component = new DefaultComponentXO();
    component.setGroup("normal-group");
    component.setName("normal-name");
    component.setVersion("-@SUM(1+1)");

    AssetXO asset = new AssetXO();
    asset.setPath("/test/path");
    asset.setBlobStoreName("default");
    asset.setFileSize(1024L);
    asset.setLastDownloaded(new Date());
    asset.setBlobCreated(new Date());
    component.setAssets(List.of(asset));

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    underTest.write(repository, Stream.of(component), outputStream);

    String csvContent = outputStream.toString(StandardCharsets.UTF_8);

    assertThat("Field with - prefix should be escaped",
        csvContent, containsString("'-@SUM(1+1)"));
  }

  @Test
  void testFormulaInjectionPreventionWithAtSign() throws IOException {
    // Test @ prefix
    ComponentXO component = new DefaultComponentXO();
    component.setGroup("normal-group");
    component.setName("@SUM(A1:A10)");
    component.setVersion("1.0.0");

    AssetXO asset = new AssetXO();
    asset.setPath("/test/path");
    asset.setBlobStoreName("default");
    asset.setFileSize(1024L);
    asset.setLastDownloaded(new Date());
    asset.setBlobCreated(new Date());
    component.setAssets(List.of(asset));

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    underTest.write(repository, Stream.of(component), outputStream);

    String csvContent = outputStream.toString(StandardCharsets.UTF_8);

    assertThat("Field with @ prefix should be escaped",
        csvContent, containsString("'@SUM(A1:A10)"));
  }

  @Test
  void testTabPrefixInjection() throws IOException {
    // Test TAB prefix which can also trigger formulas in some spreadsheets
    ComponentXO component = new DefaultComponentXO();
    component.setGroup("\t=cmd|' /C calc'!A0");
    component.setName("normal-name");
    component.setVersion("1.0.0");

    AssetXO asset = new AssetXO();
    asset.setPath("/test/path");
    asset.setBlobStoreName("default");
    asset.setFileSize(1024L);
    asset.setLastDownloaded(new Date());
    asset.setBlobCreated(new Date());
    component.setAssets(List.of(asset));

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    underTest.write(repository, Stream.of(component), outputStream);

    String csvContent = outputStream.toString(StandardCharsets.UTF_8);

    // TAB-prefixed content should be escaped with single quote
    assertThat("TAB-prefixed field should be escaped",
        csvContent, containsString("'\t=cmd|' /C calc'!A0"));
  }

  @Test
  void testNormalDataNotAffected() throws IOException {
    // Create a component with normal data
    ComponentXO component = new DefaultComponentXO();
    component.setGroup("com.example");
    component.setName("test-artifact");
    component.setVersion("1.0.0");

    AssetXO asset = new AssetXO();
    asset.setPath("/com/example/test-artifact/1.0.0/test-artifact-1.0.0.jar");
    asset.setBlobStoreName("default");
    asset.setFileSize(1024L);
    asset.setLastDownloaded(new Date());
    asset.setBlobCreated(new Date());
    component.setAssets(List.of(asset));

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    underTest.write(repository, Stream.of(component), outputStream);

    String csvContent = outputStream.toString(StandardCharsets.UTF_8);

    // Normal data should not have leading quote added
    assertThat("CSV should contain normal namespace", csvContent.contains("com.example"));
    assertThat("CSV should contain normal name", csvContent.contains("test-artifact"));
    assertThat("CSV should contain normal version", csvContent.contains("1.0.0"));
    assertThat("Normal data should not start with quote", not(csvContent.contains("'com.example")));
  }

  @Test
  void testEmptyFields() throws IOException {
    // Test handling of null/empty fields
    ComponentXO component = new DefaultComponentXO();
    component.setGroup(null);
    component.setName("");
    component.setVersion("1.0.0");

    AssetXO asset = new AssetXO();
    asset.setPath("/test/path");
    asset.setBlobStoreName("default");
    asset.setFileSize(1024L);
    asset.setLastDownloaded(new Date());
    asset.setBlobCreated(new Date());
    component.setAssets(List.of(asset));

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    underTest.write(repository, Stream.of(component), outputStream);

    String csvContent = outputStream.toString(StandardCharsets.UTF_8);

    // Should not throw and should contain header
    assertThat("CSV should have header line",
        csvContent.contains("Namespace,Name,Version,Path"));
  }

  @Test
  void testEscapeFormulaInjectionUnit() {
    // Unit tests for the escapeFormulaInjection method
    assertThat("Should escape = prefix",
        CsvCleanupPreviewContentWriter.escapeFormulaInjection("=cmd"), containsString("'=cmd"));
    assertThat("Should escape + prefix",
        CsvCleanupPreviewContentWriter.escapeFormulaInjection("+cmd"), containsString("'+cmd"));
    assertThat("Should escape - prefix",
        CsvCleanupPreviewContentWriter.escapeFormulaInjection("-cmd"), containsString("'-cmd"));
    assertThat("Should escape @ prefix",
        CsvCleanupPreviewContentWriter.escapeFormulaInjection("@cmd"), containsString("'@cmd"));
    assertThat("Should escape TAB prefix",
        CsvCleanupPreviewContentWriter.escapeFormulaInjection("\tcmd"), containsString("'\tcmd"));
    assertThat("Should not escape normal values",
        CsvCleanupPreviewContentWriter.escapeFormulaInjection("normal"), containsString("normal"));
    assertThat("Should handle null",
        CsvCleanupPreviewContentWriter.escapeFormulaInjection(null) == null);
    assertThat("Should handle empty string",
        CsvCleanupPreviewContentWriter.escapeFormulaInjection("").isEmpty());
    assertThat("Should not escape numbers",
        CsvCleanupPreviewContentWriter.escapeFormulaInjection("1.0.0"), containsString("1.0.0"));
    assertThat("Should not escape version-like strings",
        CsvCleanupPreviewContentWriter.escapeFormulaInjection("v1.0.0"), containsString("v1.0.0"));
  }
}

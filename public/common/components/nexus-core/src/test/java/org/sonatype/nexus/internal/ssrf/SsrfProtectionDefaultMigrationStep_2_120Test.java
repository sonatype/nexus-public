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
package org.sonatype.nexus.internal.ssrf;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.sonatype.nexus.kv.GlobalKeyValueStore;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.validation.ssrf.SsrfProtectionConfiguration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SsrfProtectionDefaultMigrationStep_2_120Test
{
  @Mock
  private GlobalKeyValueStore globalKeyValueStore;

  @Mock
  private Format mavenFormat;

  @Mock
  private Format npmFormat;

  @Mock
  private Connection connection;

  @Captor
  private ArgumentCaptor<SsrfProtectionConfigData> configCaptor;

  @BeforeEach
  void setUp() {
    lenient().when(mavenFormat.getValue()).thenReturn("maven2");
    lenient().when(npmFormat.getValue()).thenReturn("npm");
  }

  @Test
  void testMigrate_newInstall_enablesSsrfProtection() throws Exception {
    noExistingConfig();
    mockEmptyTable("maven2_component");
    mockEmptyTable("maven2_asset");
    mockEmptyTable("npm_component");
    mockEmptyTable("npm_asset");

    SsrfProtectionDefaultMigrationStep_2_120 underTest = createStep(List.of(mavenFormat, npmFormat));
    underTest.migrate(connection);

    SsrfProtectionConfiguration config = captureWrittenConfig();
    assertThat(config.enabled()).isTrue();
    assertThat(config.allowedIPs()).isEmpty();
    assertThat(config.allowedDomains()).isEmpty();
  }

  @Test
  void testMigrate_existingInstall_withComponents_skips() throws Exception {
    noExistingConfig();
    mockNonEmptyTable("maven2_component");

    SsrfProtectionDefaultMigrationStep_2_120 underTest = createStep(List.of(mavenFormat));
    underTest.migrate(connection);

    verifyNoConfigWritten();
  }

  @Test
  void testMigrate_existingInstall_withAssetsOnly_skips() throws Exception {
    noExistingConfig();
    mockEmptyTable("maven2_component");
    mockNonEmptyTable("maven2_asset");

    SsrfProtectionDefaultMigrationStep_2_120 underTest = createStep(List.of(mavenFormat));
    underTest.migrate(connection);

    verifyNoConfigWritten();
  }

  @Test
  void testMigrate_existingInstall_withKvEntry_skips() throws Exception {
    SsrfProtectionConfigData existingConfig = SsrfProtectionConfigData.from(
        new SsrfProtectionConfiguration(false, Set.of("10.0.0.1"), Set.of()));
    when(globalKeyValueStore.get(eq("ssrf.protection.config"), eq(SsrfProtectionConfigData.class)))
        .thenReturn(Optional.of(existingConfig));

    SsrfProtectionDefaultMigrationStep_2_120 underTest = createStep(List.of(mavenFormat));
    underTest.migrate(connection);

    verifyNoConfigWritten();
    verify(connection, never()).prepareStatement(anyString());
  }

  @Test
  void testMigrate_tablesDoNotExist_treatedAsEmpty() throws Exception {
    noExistingConfig();
    mockMissingTable("maven2_component");
    mockMissingTable("maven2_asset");

    SsrfProtectionDefaultMigrationStep_2_120 underTest = createStep(List.of(mavenFormat));
    underTest.migrate(connection);

    assertThat(captureWrittenConfig().enabled()).isTrue();
  }

  @Test
  void testMigrate_mixedFormats_someHaveComponents_skips() throws Exception {
    noExistingConfig();
    mockEmptyTable("maven2_component");
    mockEmptyTable("maven2_asset");
    mockNonEmptyTable("npm_component");

    SsrfProtectionDefaultMigrationStep_2_120 underTest = createStep(List.of(mavenFormat, npmFormat));
    underTest.migrate(connection);

    verifyNoConfigWritten();
  }

  @Test
  void testMigrate_mixedFormats_someHaveAssetsOnly_skips() throws Exception {
    noExistingConfig();
    mockEmptyTable("maven2_component");
    mockEmptyTable("maven2_asset");
    mockEmptyTable("npm_component");
    mockNonEmptyTable("npm_asset");

    SsrfProtectionDefaultMigrationStep_2_120 underTest = createStep(List.of(mavenFormat, npmFormat));
    underTest.migrate(connection);

    verifyNoConfigWritten();
  }

  @Test
  void testMigrate_noFormats_treatedAsNewInstall() throws Exception {
    noExistingConfig();

    SsrfProtectionDefaultMigrationStep_2_120 underTest = createStep(List.of());
    underTest.migrate(connection);

    assertThat(captureWrittenConfig().enabled()).isTrue();
  }

  @Test
  void testMigrate_idempotent_secondRunFindsKvEntry() throws Exception {
    SsrfProtectionConfigData existingConfig = SsrfProtectionConfigData.from(
        new SsrfProtectionConfiguration(true, Set.of(), Set.of()));
    when(globalKeyValueStore.get(eq("ssrf.protection.config"), eq(SsrfProtectionConfigData.class)))
        .thenReturn(Optional.of(existingConfig));

    SsrfProtectionDefaultMigrationStep_2_120 underTest = createStep(List.of(mavenFormat));
    underTest.migrate(connection);

    verifyNoConfigWritten();
    verify(connection, never()).prepareStatement(anyString());
  }

  private void noExistingConfig() {
    when(globalKeyValueStore.get(eq("ssrf.protection.config"), eq(SsrfProtectionConfigData.class)))
        .thenReturn(Optional.empty());
  }

  private void mockEmptyTable(final String tableName) throws SQLException {
    PreparedStatement stmt = mock(PreparedStatement.class);
    ResultSet rs = mock(ResultSet.class);
    when(connection.prepareStatement("SELECT 1 FROM " + tableName + " LIMIT 1")).thenReturn(stmt);
    when(stmt.executeQuery()).thenReturn(rs);
    when(rs.next()).thenReturn(false);
  }

  private void mockNonEmptyTable(final String tableName) throws SQLException {
    PreparedStatement stmt = mock(PreparedStatement.class);
    ResultSet rs = mock(ResultSet.class);
    when(connection.prepareStatement("SELECT 1 FROM " + tableName + " LIMIT 1")).thenReturn(stmt);
    when(stmt.executeQuery()).thenReturn(rs);
    when(rs.next()).thenReturn(true);
  }

  private void mockMissingTable(final String tableName) throws SQLException {
    PreparedStatement stmt = mock(PreparedStatement.class);
    when(connection.prepareStatement("SELECT 1 FROM " + tableName + " LIMIT 1")).thenReturn(stmt);
    when(stmt.executeQuery()).thenThrow(new SQLException("Table not found"));
  }

  private SsrfProtectionConfiguration captureWrittenConfig() {
    verify(globalKeyValueStore).setString(eq("ssrf.protection.config"), configCaptor.capture());
    return configCaptor.getValue().toConfiguration();
  }

  private void verifyNoConfigWritten() {
    verify(globalKeyValueStore, never()).setString(anyString(), (Object) any());
  }

  private SsrfProtectionDefaultMigrationStep_2_120 createStep(final List<Format> formats) {
    return new SsrfProtectionDefaultMigrationStep_2_120(globalKeyValueStore, formats);
  }
}

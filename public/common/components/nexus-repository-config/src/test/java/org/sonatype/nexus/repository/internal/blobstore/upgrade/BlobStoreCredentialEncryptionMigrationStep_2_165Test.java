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
package org.sonatype.nexus.repository.internal.blobstore.upgrade;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.Optional;

import org.sonatype.nexus.blobstore.BlobStoreDescriptor;
import org.sonatype.nexus.crypto.secrets.Secret;
import org.sonatype.nexus.crypto.secrets.SecretsService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers the second run of the credential-encryption sweep. The behaviour of the sweep itself is
 * exercised in {@link BlobStoreCredentialEncryptionMigrationStep_2_160Test}; what matters here is that
 * this step really does re-run it, and that re-running is harmless for credentials 2.160 already handled.
 *
 * <p>
 * Azure is the concrete motivation (NEXUS-54313): its descriptor only began declaring {@code accountKey}
 * sensitive in that change, so instances that had already recorded 2.160 never encrypted it.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class BlobStoreCredentialEncryptionMigrationStep_2_165Test
{
  /**
   * The value of {@code AzureConfigurationConstants.TYPE}. Duplicated as a literal because this module
   * cannot depend on the Pro-only Azure component; if the two ever diverge the sweep stops matching Azure
   * configurations, so the coupling is deliberate and worth asserting against.
   */
  private static final String AZURE_TYPE = "Azure Cloud Storage";

  private static final String AZURE_CONFIG_KEY = "azure cloud storage";

  private static final String ACCOUNT_KEY = "accountKey";

  @Mock
  private SecretsService secretsService;

  private BlobStoreDescriptor azureDescriptor;

  @Mock
  private Connection connection;

  @Mock
  private DatabaseMetaData metadata;

  @Mock
  private PreparedStatement selectStmt;

  @Mock
  private PreparedStatement updateStmt;

  @Mock
  private ResultSet resultSet;

  private BlobStoreCredentialEncryptionMigrationStep_2_165 underTest;

  @BeforeEach
  public void setUp() throws Exception {
    // Name the mock with the Azure type so QualifierUtil keys it the way the descriptor's @Qualifier does
    azureDescriptor = mock(BlobStoreDescriptor.class, AZURE_TYPE);
    when(azureDescriptor.getSensitiveConfigurationFields()).thenReturn(List.of(ACCOUNT_KEY));

    when(connection.getMetaData()).thenReturn(metadata);
    when(metadata.getDatabaseProductName()).thenReturn("H2");

    when(connection.prepareStatement(any())).thenReturn(selectStmt, updateStmt);
    when(selectStmt.executeQuery()).thenReturn(resultSet);

    underTest = new BlobStoreCredentialEncryptionMigrationStep_2_165(
        secretsService,
        List.of(azureDescriptor));
  }

  @Test
  public void testVersion() {
    assertThat(underTest.version(), is(Optional.of("2.165")));
  }

  /**
   * The whole point of the step: an Azure account key left in plaintext by 2.160 gets encrypted now.
   * This also exercises the multi-word type key, which the sweep lower-cases to find the attributes
   * sub-map ("Azure Cloud Storage" -> "azure cloud storage").
   */
  @Test
  public void azurePlaintextAccountKeyGetsEncrypted() throws Exception {
    stubSingleRow("{\"" + AZURE_CONFIG_KEY + "\":{\"" + ACCOUNT_KEY + "\":\"myAccountKey\"}}");

    // SecretsService hands legacy plaintext back verbatim, which is how the sweep recognises it
    Secret plaintextSecret = mock(Secret.class);
    when(plaintextSecret.decrypt()).thenReturn("myAccountKey".toCharArray());
    when(secretsService.from("myAccountKey")).thenReturn(plaintextSecret);

    Secret encryptedSecret = mock(Secret.class);
    when(encryptedSecret.getId()).thenReturn("_42");
    when(secretsService.encryptMaven(eq("blobstore-config"), aryEq("myAccountKey".toCharArray()), isNull()))
        .thenReturn(encryptedSecret);

    underTest.migrate(connection);

    verify(secretsService).encryptMaven(eq("blobstore-config"), aryEq("myAccountKey".toCharArray()), isNull());
    verify(updateStmt).executeUpdate();
  }

  /**
   * Idempotency: a credential 2.160 already converted is stored as a {@code _<id>} reference and must be
   * left strictly alone — no re-encryption, and no UPDATE issued at all.
   */
  @Test
  public void alreadyEncryptedCredentialIsSkipped() throws Exception {
    stubSingleRow("{\"" + AZURE_CONFIG_KEY + "\":{\"" + ACCOUNT_KEY + "\":\"_42\"}}");

    underTest.migrate(connection);

    verify(secretsService, never()).encryptMaven(any(), any(), any());
    // Only the SELECT was prepared; the UPDATE path was never entered
    verify(connection, times(1)).prepareStatement(any());
  }

  /**
   * Idempotency across repeated execution: the sweep encrypts on the first pass and, reading back the
   * value it just wrote, does nothing on the second. Guards against a re-run double-wrapping a secret
   * reference in another secret.
   */
  @Test
  public void runningTwiceEncryptsOnlyOnce() throws Exception {
    // Three prepareStatement calls across the two runs, in order: run 1's SELECT, run 1's UPDATE, then
    // run 2's SELECT. selectStmt is deliberately repeated as the third value rather than being a typo —
    // Mockito hands back each thenReturn value once in sequence, so the third call needs its own entry.
    // Run 2 issues no UPDATE, which is exactly what this test asserts.
    when(connection.prepareStatement(any())).thenReturn(selectStmt, updateStmt, selectStmt);

    // Both runs read through the same resultSet mock (both SELECTs return selectStmt.executeQuery()),
    // so this one chain is consumed across both: true/false ends run 1's single row, true/false run 2's.
    when(resultSet.next()).thenReturn(true, false, true, false);
    when(resultSet.getString("id")).thenReturn("azure-id");
    when(resultSet.getString("name")).thenReturn("test-azure");
    when(resultSet.getString("type")).thenReturn(AZURE_TYPE);
    when(resultSet.getString("attributes")).thenReturn(
        "{\"" + AZURE_CONFIG_KEY + "\":{\"" + ACCOUNT_KEY + "\":\"myAccountKey\"}}",
        "{\"" + AZURE_CONFIG_KEY + "\":{\"" + ACCOUNT_KEY + "\":\"_42\"}}");

    Secret plaintextSecret = mock(Secret.class);
    when(plaintextSecret.decrypt()).thenReturn("myAccountKey".toCharArray());
    when(secretsService.from("myAccountKey")).thenReturn(plaintextSecret);

    Secret encryptedSecret = mock(Secret.class);
    when(encryptedSecret.getId()).thenReturn("_42");
    when(secretsService.encryptMaven(any(), any(), any())).thenReturn(encryptedSecret);

    underTest.migrate(connection);
    underTest.migrate(connection);

    verify(secretsService, times(1)).encryptMaven(any(), any(), any());
    verify(updateStmt, times(1)).executeUpdate();
  }

  /**
   * Managed-identity and environment-variable auth persist no account key at all.
   */
  @Test
  public void configurationWithoutAnAccountKeyIsUntouched() throws Exception {
    stubSingleRow("{\"" + AZURE_CONFIG_KEY + "\":{\"authenticationMethod\":\"MANAGED_IDENTITY\"}}");

    underTest.migrate(connection);

    verify(secretsService, never()).encryptMaven(any(), any(), any());
    verify(connection, times(1)).prepareStatement(any());
  }

  @Test
  public void testNoDescriptorsSkipsMigration() throws Exception {
    BlobStoreCredentialEncryptionMigrationStep_2_165 emptyDescriptors =
        new BlobStoreCredentialEncryptionMigrationStep_2_165(secretsService, List.of());

    emptyDescriptors.migrate(connection);

    verify(connection, never()).prepareStatement(any());
    verify(secretsService, never()).encryptMaven(any(), any(), any());
  }

  private void stubSingleRow(final String attributesJson) throws Exception {
    when(resultSet.next()).thenReturn(true, false);
    when(resultSet.getString("id")).thenReturn("azure-id");
    when(resultSet.getString("name")).thenReturn("test-azure");
    when(resultSet.getString("type")).thenReturn(AZURE_TYPE);
    when(resultSet.getString("attributes")).thenReturn(attributesJson);
  }
}

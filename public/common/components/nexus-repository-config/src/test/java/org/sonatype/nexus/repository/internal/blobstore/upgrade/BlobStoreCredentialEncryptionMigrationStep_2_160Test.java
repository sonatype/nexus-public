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
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.sonatype.nexus.blobstore.BlobStoreDescriptor;
import org.sonatype.nexus.crypto.secrets.Secret;
import org.sonatype.nexus.crypto.secrets.SecretsService;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BlobStoreCredentialEncryptionMigrationStep_2_160Test
{
  @Rule
  public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Mock
  private SecretsService secretsService;

  private BlobStoreDescriptor s3Descriptor;

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

  private BlobStoreCredentialEncryptionMigrationStep_2_160 underTest;

  @Before
  public void setUp() throws Exception {
    // Name the mock "s3" so QualifierUtil resolves it correctly
    s3Descriptor = mock(BlobStoreDescriptor.class, "s3");
    // S3 blob store has secretKey as sensitive
    when(s3Descriptor.getSensitiveConfigurationFields()).thenReturn(List.of("secretKey"));

    // Setup connection metadata
    when(connection.getMetaData()).thenReturn(metadata);
    when(metadata.getDatabaseProductName()).thenReturn("H2");

    // Setup prepared statements
    when(connection.prepareStatement(any())).thenReturn(selectStmt, updateStmt);
    when(selectStmt.executeQuery()).thenReturn(resultSet);

    underTest = new BlobStoreCredentialEncryptionMigrationStep_2_160(
        secretsService,
        List.of(s3Descriptor));
  }

  @Test
  public void testVersion() {
    assertThat(underTest.version(), is(Optional.of("2.160")));
  }

  @Test
  public void testNoConfigurations() throws Exception {
    // ResultSet has no rows
    when(resultSet.next()).thenReturn(false);

    underTest.migrate(connection);

    verify(secretsService, never()).encryptMaven(any(), any(), any());
  }

  @Test
  public void testPlaintextCredentialGetsEncrypted() throws Exception {
    // Setup one row in ResultSet with plaintext credential
    when(resultSet.next()).thenReturn(true, false);
    when(resultSet.getString("id")).thenReturn("s3-id");
    when(resultSet.getString("name")).thenReturn("test-s3");
    when(resultSet.getString("type")).thenReturn("s3");
    when(resultSet.getString("attributes"))
        .thenReturn("{\"s3\":{\"secretKey\":\"mySecretKey\"}}");

    // Mock decryption to return same value (indicates plaintext)
    Secret plaintextSecret = mock(Secret.class);
    when(plaintextSecret.decrypt()).thenReturn("mySecretKey".toCharArray());
    when(secretsService.from("mySecretKey")).thenReturn(plaintextSecret);

    // Mock encryption
    Secret encryptedSecret = mock(Secret.class);
    when(encryptedSecret.getId()).thenReturn("_42");
    when(secretsService.encryptMaven(eq("blobstore-config"), aryEq("mySecretKey".toCharArray()), isNull()))
        .thenReturn(encryptedSecret);

    underTest.migrate(connection);

    // Verify the secret was created
    verify(secretsService).encryptMaven(eq("blobstore-config"), aryEq("mySecretKey".toCharArray()), isNull());

    // Verify the update was executed
    verify(updateStmt).executeUpdate();
  }

  @Test
  public void testAlreadyEncryptedCredentialIsSkipped() throws Exception {
    // Setup one row with already-encrypted secret ID reference
    when(resultSet.next()).thenReturn(true, false);
    when(resultSet.getString("id")).thenReturn("s3-id");
    when(resultSet.getString("name")).thenReturn("test-s3");
    when(resultSet.getString("type")).thenReturn("s3");
    when(resultSet.getString("attributes"))
        .thenReturn("{\"s3\":{\"secretKey\":\"_123\"}}");

    underTest.migrate(connection);

    // Verify no encryption happened
    verify(secretsService, never()).encryptMaven(any(), any(), any());
  }

  @Test
  public void testLegacyPBECredentialIsSkipped() throws Exception {
    // Setup one row with legacy PBE-encrypted value
    when(resultSet.next()).thenReturn(true, false);
    when(resultSet.getString("id")).thenReturn("s3-id");
    when(resultSet.getString("name")).thenReturn("test-s3");
    when(resultSet.getString("type")).thenReturn("s3");
    when(resultSet.getString("attributes"))
        .thenReturn("{\"s3\":{\"secretKey\":\"{enc}legacyPBEValue\"}}");

    // Mock decryption to return different value (indicates legacy PBE)
    Secret pbeSecret = mock(Secret.class);
    when(pbeSecret.decrypt()).thenReturn("decryptedValue".toCharArray());
    when(secretsService.from("{enc}legacyPBEValue")).thenReturn(pbeSecret);

    underTest.migrate(connection);

    // Verify no encryption happened for legacy PBE
    verify(secretsService, never()).encryptMaven(any(), any(), any());
  }

  @Test
  public void testNoSensitiveFields() throws Exception {
    // Descriptor with no sensitive fields
    when(s3Descriptor.getSensitiveConfigurationFields()).thenReturn(Collections.emptyList());

    when(resultSet.next()).thenReturn(true, false);
    when(resultSet.getString("id")).thenReturn("s3-id");
    when(resultSet.getString("name")).thenReturn("test-s3");
    when(resultSet.getString("type")).thenReturn("s3");
    when(resultSet.getString("attributes"))
        .thenReturn("{\"s3\":{\"someField\":\"someValue\"}}");

    underTest.migrate(connection);

    verify(secretsService, never()).encryptMaven(any(), any(), any());
  }

  @Test
  public void testUnknownBlobStoreTypeIsSkipped() throws Exception {
    when(resultSet.next()).thenReturn(true, false);
    when(resultSet.getString("id")).thenReturn("unknown-id");
    when(resultSet.getString("name")).thenReturn("test-unknown");
    when(resultSet.getString("type")).thenReturn("unknown-type");
    when(resultSet.getString("attributes"))
        .thenReturn("{\"unknown-type\":{\"someKey\":\"someValue\"}}");

    underTest.migrate(connection);

    verify(secretsService, never()).encryptMaven(any(), any(), any());
  }

  @Test
  public void testNoDescriptorsSkipsMigration() throws Exception {
    // Create migration step with no descriptors
    BlobStoreCredentialEncryptionMigrationStep_2_160 emptyDescriptors =
        new BlobStoreCredentialEncryptionMigrationStep_2_160(
            secretsService,
            Collections.emptyList());

    emptyDescriptors.migrate(connection);

    // Should not attempt to query database
    verify(connection, never()).prepareStatement(any());
    verify(secretsService, never()).encryptMaven(any(), any(), any());
  }
}

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
package org.sonatype.nexus.internal.security.apikey.task;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

import org.sonatype.nexus.crypto.internal.PbeCipherFactory;
import org.sonatype.nexus.crypto.internal.PbeCipherFactory.PbeCipher;
import org.sonatype.nexus.crypto.secrets.EncryptedSecret;
import org.sonatype.nexus.crypto.secrets.internal.EncryptionKeyList.FixedEncryption;
import org.sonatype.nexus.crypto.secrets.internal.EncryptionKeyList.SecretEncryptionKey;
import org.sonatype.nexus.crypto.secrets.internal.EncryptionKeySource;
import org.sonatype.nexus.datastore.api.DataSessionSupplier;
import org.sonatype.nexus.internal.security.secrets.tasks.ReEncryptTaskDescriptor;
import org.sonatype.nexus.scheduling.TaskConfiguration;

import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.Silent.class)
public class ReEncryptPrincipalsTaskTest
{
  private static final String SELECT = """
      SELECT principals, domain, username, access_key
      FROM api_key_v2
      ORDER BY username, principals, domain
      LIMIT ? OFFSET ?
      """;

  private static final String UPDATE = """
      UPDATE api_key_v2
      SET principals = ?
      WHERE domain = ? AND username = ? AND access_key = ?
      """;

  @Mock
  private DataSessionSupplier sessionSupplier;

  @Mock
  private EncryptionKeySource encryptionKeySource;

  @Mock
  private PbeCipherFactory pbeCipherFactory;

  private ReEncryptPrincipalsTask underTest;

  private PreparedStatement mockPreparedStatementSelect;

  private PreparedStatement mockPreparedStatementUpdate;

  private PbeCipher mockPbeCipherToEncrypt;

  @Before
  public void setup() throws Exception {
    underTest =
        spy(new ReEncryptPrincipalsTask(sessionSupplier, encryptionKeySource, pbeCipherFactory, "password", "salt",
            "iv", "PBKDF2WithHmacSHA256", null));
    doReturn(setupTaskConfig()).when(underTest).taskConfiguration();

    Connection mockConnection = mock(Connection.class);
    when(sessionSupplier.openConnection("nexus")).thenReturn(mockConnection);

    mockPreparedStatementSelect = mock(PreparedStatement.class);
    when(mockConnection.prepareStatement(SELECT)).thenReturn(mockPreparedStatementSelect);
    ResultSet mockResultSet = getMockResultSet();
    when(mockPreparedStatementSelect.executeQuery()).thenReturn(mockResultSet);

    mockPreparedStatementUpdate = mock(PreparedStatement.class);
    when(mockConnection.prepareStatement(UPDATE)).thenReturn(mockPreparedStatementUpdate);

    mockPbeCipherToEncrypt = mock(PbeCipher.class);
    when(pbeCipherFactory.create(any(), any(), any(), any())).thenReturn(mockPbeCipherToEncrypt);
    when(mockPbeCipherToEncrypt.encrypt(any()))
        .thenReturn(EncryptedSecret.parse("$test2$kv1=v1,kv2=v2$test-salt2$dGVzdA=="));
  }

  @Test
  public void testExecute() throws Exception {
    PbeCipher mockPbeCipherToDecrypt = mock(PbeCipher.class);
    when(pbeCipherFactory.create(any(), any())).thenReturn(mockPbeCipherToDecrypt);
    when(mockPbeCipherToDecrypt.decrypt()).thenReturn("decrypted".getBytes());

    Object result = underTest.execute();

    assertThat(result).isEqualTo(3);
    verify(sessionSupplier, times(1)).openConnection(anyString());
    verify(pbeCipherFactory, times(3)).create(any(), any());
    verify(mockPreparedStatementSelect, times(2)).executeQuery();
    verify(mockPbeCipherToDecrypt, times(3)).decrypt();
    verify(mockPbeCipherToEncrypt, times(3)).encrypt(any());
    verify(mockPreparedStatementUpdate, times(3)).executeUpdate();
  }

  @Test
  public void testExecuteWithFixedEncryptionConfigured() throws Exception {
    // Simulate fixed encryption configuration
    SecretEncryptionKey keyToBeUsed = new SecretEncryptionKey("test-key-id", "somePassword");
    when(encryptionKeySource.getFixedEncryption()).thenReturn(
        Optional.of(new FixedEncryption("test-key-id", "test-salt", "test-iv")));
    when(encryptionKeySource.getKey("test-key-id")).thenReturn(Optional.of(keyToBeUsed));

    PbeCipher mockPbeCipherToDecrypt = mock(PbeCipher.class);
    when(pbeCipherFactory.create(any(), any())).thenReturn(mockPbeCipherToDecrypt);
    when(mockPbeCipherToDecrypt.decrypt()).thenReturn("decrypted".getBytes());

    Object result = underTest.execute();

    assertThat(result).isEqualTo(3);
    verify(sessionSupplier, times(1)).openConnection(anyString());
    verify(pbeCipherFactory, times(1)).create(keyToBeUsed, "test-salt", "test-iv", 10000);
    verify(pbeCipherFactory, times(3)).create(any(SecretEncryptionKey.class), anyString());

    verify(mockPreparedStatementSelect, times(2)).executeQuery();
    verify(mockPbeCipherToDecrypt, times(3)).decrypt();
    verify(mockPbeCipherToEncrypt, times(3)).encrypt(any());
    verify(mockPreparedStatementUpdate, times(3)).executeUpdate();
  }

  @Test
  public void testExecuteWithPreviousAndFixedEncryptionConfigured() throws Exception {
    // Simulate fixed encryption configuration
    SecretEncryptionKey keyToBeUsed = new SecretEncryptionKey("test-key-id", "somePassword");
    when(encryptionKeySource.getPreviousFixedEncryption()).thenReturn(
        Optional.of(new FixedEncryption("test-key-id", "old-salt", "old-iv")));
    when(encryptionKeySource.getFixedEncryption()).thenReturn(
        Optional.of(new FixedEncryption("test-key-id", "test-salt", "test-iv")));
    when(encryptionKeySource.getKey("test-key-id")).thenReturn(Optional.of(keyToBeUsed));

    PbeCipher mockPbeCipherToDecrypt = mock(PbeCipher.class);
    when(pbeCipherFactory.create(any(), any())).thenReturn(mockPbeCipherToDecrypt);
    when(mockPbeCipherToDecrypt.decrypt()).thenReturn("decrypted".getBytes());

    Object result = underTest.execute();

    assertThat(result).isEqualTo(3);
    verify(sessionSupplier, times(1)).openConnection(anyString());
    verify(pbeCipherFactory, times(1)).create(keyToBeUsed, "test-salt", "test-iv", 10000);
    verify(pbeCipherFactory, times(3)).create(eq(keyToBeUsed), anyString());

    verify(mockPreparedStatementSelect, times(2)).executeQuery();
    verify(mockPbeCipherToDecrypt, times(3)).decrypt();
    verify(mockPbeCipherToEncrypt, times(3)).encrypt(any());
    verify(mockPreparedStatementUpdate, times(3)).executeUpdate();
  }

  @Test
  public void testExecute_WhenSecondPrincipalsFails() throws Exception {
    PbeCipher mockPbeCipherToDecrypt = mock(PbeCipher.class);
    when(pbeCipherFactory.create(any(), any())).thenReturn(mockPbeCipherToDecrypt);

    // Simulate decryption failure for the second one
    when(mockPbeCipherToDecrypt.decrypt())
        .thenReturn("decrypted1".getBytes())
        .thenThrow(new RuntimeException("Decryption failed"))
        .thenReturn("decrypted3".getBytes());

    Object result = underTest.execute();

    assertThat(result).isEqualTo(2);
    verify(sessionSupplier, times(1)).openConnection(anyString());
    verify(pbeCipherFactory, times(1)).create(any(), any(), any(), any());
    verify(pbeCipherFactory, times(3)).create(any(), any());
    verify(mockPreparedStatementSelect, times(2)).executeQuery();
    verify(mockPbeCipherToDecrypt, times(3)).decrypt();
    verify(mockPbeCipherToEncrypt, times(2)).encrypt(any());
    verify(mockPreparedStatementUpdate, times(2)).executeUpdate();
  }

  @Test
  public void testExecuteWithCustomIterationsFromTaskConfiguration() throws Exception {
    underTest = spy(new ReEncryptPrincipalsTask(sessionSupplier, encryptionKeySource, pbeCipherFactory,
        "password", "salt", "iv", "PBKDF2WithHmacSHA256", 15000));

    TaskConfiguration customTaskConfig = new TaskConfiguration();
    customTaskConfig.setId(UUID.randomUUID().toString());
    customTaskConfig.setTypeId(ReEncryptTaskDescriptor.TYPE_ID);
    customTaskConfig.setString("algorithmForDecryption", "PBKDF2WithHmacSHA1");
    customTaskConfig.setInteger("iterationsForDecryption", 5000);
    doReturn(customTaskConfig).when(underTest).taskConfiguration();

    Connection mockConnection = mock(Connection.class);
    when(sessionSupplier.openConnection("nexus")).thenReturn(mockConnection);
    when(mockConnection.prepareStatement(SELECT)).thenReturn(mockPreparedStatementSelect);
    when(mockConnection.prepareStatement(UPDATE)).thenReturn(mockPreparedStatementUpdate);

    PbeCipher mockPbeCipherToDecrypt = mock(PbeCipher.class);
    when(pbeCipherFactory.create(any(), any())).thenReturn(mockPbeCipherToDecrypt);
    when(mockPbeCipherToDecrypt.decrypt()).thenReturn("decrypted".getBytes());

    Object result = underTest.execute();

    assertThat(result).isEqualTo(3);
    verify(sessionSupplier, times(1)).openConnection(anyString());
    verify(pbeCipherFactory, times(1)).create(any(), eq("salt"), eq("iv"), eq(15000));
    verify(mockPreparedStatementSelect, times(2)).executeQuery();
    verify(mockPbeCipherToDecrypt, times(3)).decrypt();
    verify(mockPbeCipherToEncrypt, times(3)).encrypt(any());
    verify(mockPreparedStatementUpdate, times(3)).executeUpdate();
  }

  @Test
  public void testExecuteWithIterationsFromNexusProperties() throws Exception {
    underTest = spy(new ReEncryptPrincipalsTask(sessionSupplier, encryptionKeySource, pbeCipherFactory,
        "password", "salt", "iv", "PBKDF2WithHmacSHA256", 210000));

    TaskConfiguration customTaskConfig = new TaskConfiguration();
    customTaskConfig.setId(UUID.randomUUID().toString());
    customTaskConfig.setTypeId(ReEncryptTaskDescriptor.TYPE_ID);
    customTaskConfig.setString("algorithmForDecryption", "PBKDF2WithHmacSHA1");
    doReturn(customTaskConfig).when(underTest).taskConfiguration();

    Connection mockConnection = mock(Connection.class);
    when(sessionSupplier.openConnection("nexus")).thenReturn(mockConnection);
    when(mockConnection.prepareStatement(SELECT)).thenReturn(mockPreparedStatementSelect);
    when(mockConnection.prepareStatement(UPDATE)).thenReturn(mockPreparedStatementUpdate);

    PbeCipher mockPbeCipherToDecrypt = mock(PbeCipher.class);
    when(pbeCipherFactory.create(any(), any())).thenReturn(mockPbeCipherToDecrypt);
    when(mockPbeCipherToDecrypt.decrypt()).thenReturn("decrypted".getBytes());

    Object result = underTest.execute();

    assertThat(result).isEqualTo(3);
    verify(pbeCipherFactory, times(1)).create(any(), eq("salt"), eq("iv"), eq(210000));
  }

  @Test
  public void testExecuteWithoutConfiguredIterations_UsesDefaults() throws Exception {
    underTest = spy(new ReEncryptPrincipalsTask(sessionSupplier, encryptionKeySource, pbeCipherFactory,
        "password", "salt", "iv", "PBKDF2WithHmacSHA256", null));

    TaskConfiguration customTaskConfig = new TaskConfiguration();
    customTaskConfig.setId(UUID.randomUUID().toString());
    customTaskConfig.setTypeId(ReEncryptTaskDescriptor.TYPE_ID);
    customTaskConfig.setString("algorithmForDecryption", "PBKDF2WithHmacSHA1");
    doReturn(customTaskConfig).when(underTest).taskConfiguration();

    Connection mockConnection = mock(Connection.class);
    when(sessionSupplier.openConnection("nexus")).thenReturn(mockConnection);
    when(mockConnection.prepareStatement(SELECT)).thenReturn(mockPreparedStatementSelect);
    when(mockConnection.prepareStatement(UPDATE)).thenReturn(mockPreparedStatementUpdate);

    PbeCipher mockPbeCipherToDecrypt = mock(PbeCipher.class);
    when(pbeCipherFactory.create(any(), any())).thenReturn(mockPbeCipherToDecrypt);
    when(mockPbeCipherToDecrypt.decrypt()).thenReturn("decrypted".getBytes());

    Object result = underTest.execute();

    assertThat(result).isEqualTo(3);
    verify(pbeCipherFactory, times(1)).create(any(), eq("salt"), eq("iv"), eq(10000));
  }

  private static @NotNull ResultSet getMockResultSet() throws SQLException {
    ResultSet mockResultSet = mock(ResultSet.class);
    // only one page of results
    when(mockResultSet.isBeforeFirst()).thenReturn(true, false);
    // 3 rows in the result set
    when(mockResultSet.next()).thenReturn(true, true, true, false);
    when(mockResultSet.getBytes("principals")).thenReturn("new-principals1".getBytes(), "new-principals2".getBytes(),
        "new-principals3".getBytes());
    when(mockResultSet.getString("domain")).thenReturn("domain1", "domain2", "domain3");
    when(mockResultSet.getString("username")).thenReturn("user1", "user2", "user3");
    when(mockResultSet.getString("access_key")).thenReturn("access-key-1", "access-key-2", "access-key-3");
    return mockResultSet;
  }

  @Test
  public void testExecuteWithUnsupportedAlgorithmThrowsException() throws Exception {
    underTest = spy(new ReEncryptPrincipalsTask(sessionSupplier, encryptionKeySource, pbeCipherFactory,
        "password", "salt", "iv", "PBKDF2WithHmacSHA256", 15000));

    TaskConfiguration customTaskConfig = new TaskConfiguration();
    customTaskConfig.setId(UUID.randomUUID().toString());
    customTaskConfig.setTypeId(ReEncryptTaskDescriptor.TYPE_ID);
    // Set an unsupported algorithm
    customTaskConfig.setString("algorithmForDecryption", "UnsupportedAlgorithm");
    doReturn(customTaskConfig).when(underTest).taskConfiguration();

    Connection mockConnection = mock(Connection.class);
    when(sessionSupplier.openConnection("nexus")).thenReturn(mockConnection);
    when(mockConnection.prepareStatement(SELECT)).thenReturn(mockPreparedStatementSelect);
    when(mockConnection.prepareStatement(UPDATE)).thenReturn(mockPreparedStatementUpdate);

    // Execute should throw IllegalArgumentException
    try {
      underTest.execute();
      fail("Expected IllegalArgumentException to be thrown");
    }
    catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).contains("Unsupported algorithm: 'UnsupportedAlgorithm'");
      assertThat(e.getMessage()).contains("Supported algorithms are:");
      assertThat(e.getMessage()).contains("PBKDF2WithHmacSHA1");
      assertThat(e.getMessage()).contains("PBKDF2WithHmacSHA256");
    }
  }

  private static TaskConfiguration setupTaskConfig() {
    TaskConfiguration taskConfiguration = new TaskConfiguration();
    taskConfiguration.setId(UUID.randomUUID().toString());
    taskConfiguration.setTypeId(ReEncryptTaskDescriptor.TYPE_ID);
    taskConfiguration.setString("algorithmForDecryption", "PBKDF2WithHmacSHA1");
    return taskConfiguration;
  }
}

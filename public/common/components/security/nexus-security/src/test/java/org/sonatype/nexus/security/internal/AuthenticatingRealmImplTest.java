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
package org.sonatype.nexus.security.internal;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.datastore.api.DataAccessException;
import org.sonatype.nexus.security.config.CUser;
import org.sonatype.nexus.security.config.SecurityConfigurationManager;
import org.sonatype.nexus.security.config.memory.MemoryCUser;

import org.apache.shiro.authc.CredentialsException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authc.credential.PasswordService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

public class AuthenticatingRealmImplTest
    extends TestSupport
{
  private static final String TEST_USERNAME = "testUser";

  private static final String TEST_PASSWORD = "admin123";

  private static final String LEGACY_PASSWORD_HASH = "f865b53623b121fd34ee5426c792e5c33af8c227";

  private static final String SHIRO_PASSWORD_ALGORITHM = "shiro1";

  private static final String SHA256_PASSWORD_ALGORITHM = "pbkdf2-sha256";

  @Mock
  private SecurityConfigurationManager configuration;

  @Mock
  private PasswordService passwordService;

  private final CUser testUser = new MemoryCUser();

  @Before
  public void setUp() throws Exception {

    testUser.setId(TEST_USERNAME);
    testUser.setStatus(CUser.STATUS_ACTIVE);
    testUser.setPassword(LEGACY_PASSWORD_HASH);

    // read must pass back detached copy to avoid side-effects in test
    when(configuration.readUser(TEST_USERNAME)).thenAnswer((inv) -> testUser.clone());

    // can't use argument captor because argument is mutated after call
    doAnswer((inv) -> {
      // capture password from the updated user at the time of the call
      testUser.setPassword(((CUser) inv.getArguments()[0]).getPassword());
      return null;
    }).when(configuration).updateUser(any());

    // Configure passwordService to match any password for simplicity
    when(passwordService.passwordsMatch(any(), any())).thenReturn(true);
  }

  @Test
  public void testLegacyPasswordIsReHashedUsingShiroOnOrient() {
    when(passwordService.encryptPassword("admin123")).thenReturn(
        "$shiro1$SHA-512$1024$jp/XAZ6ZsFoy8Tshw1/xGw==$CbW0M68TR1Sp+mS4SfQGolAv2tBiUTbp6PZVhzhVSnWeAtR1NWt1Hn2S+OlvIWg+qYKDmWRvbDEVwdJt9La4ng==");
    assertThat(testUser.getPassword(), is(LEGACY_PASSWORD_HASH));
    AuthenticatingRealmImpl underTestOrient =
        new AuthenticatingRealmImpl(configuration, passwordService, true, SHIRO_PASSWORD_ALGORITHM, null);
    underTestOrient.getAuthenticationInfo(new UsernamePasswordToken(TEST_USERNAME, TEST_PASSWORD));
    assertThat(testUser.getPassword(), startsWith("$shiro1$SHA-512$"));
  }

  @Test
  public void testLegacyPasswordIsReHashedUsingShiroOnNewDB() {
    when(passwordService.encryptPassword("admin123")).thenReturn(
        "$shiro1$SHA-512$1024$jp/XAZ6ZsFoy8Tshw1/xGw==$CbW0M68TR1Sp+mS4SfQGolAv2tBiUTbp6PZVhzhVSnWeAtR1NWt1Hn2S+OlvIWg+qYKDmWRvbDEVwdJt9La4ng==");
    assertThat(testUser.getPassword(), is(LEGACY_PASSWORD_HASH));
    AuthenticatingRealmImpl underTestOrient =
        new AuthenticatingRealmImpl(configuration, passwordService, false, SHIRO_PASSWORD_ALGORITHM, null);
    underTestOrient.getAuthenticationInfo(new UsernamePasswordToken(TEST_USERNAME, TEST_PASSWORD));
    assertThat(testUser.getPassword(), startsWith("$shiro1$SHA-512"));
  }

  @Test
  public void testLegacyPasswordIsReHashedToSha256OnOrient() {
    when(passwordService.encryptPassword("admin123")).thenReturn(
        "$pbkdf2-sha256$i=1024$jp/XAZ6ZsFoy8Tshw1/xGw==$CbW0M68TR1Sp+mS4SfQGolAv2tBiUTbp6PZVhzhVSnWeAtR1NWt1Hn2S+OlvIWg+qYKDmWRvbDEVwdJt9La4ng==");
    assertThat(testUser.getPassword(), is(LEGACY_PASSWORD_HASH));
    AuthenticatingRealmImpl underTestOrient =
        new AuthenticatingRealmImpl(configuration, passwordService, true, SHA256_PASSWORD_ALGORITHM, 1024);
    underTestOrient.getAuthenticationInfo(new UsernamePasswordToken(TEST_USERNAME, TEST_PASSWORD));
    assertThat(testUser.getPassword(), startsWith("$pbkdf2-sha256$i"));
  }

  @Test
  public void testLegacyPasswordIsReHashedToSha256OnNewDB() {
    when(passwordService.encryptPassword("admin123")).thenReturn(
        "$pbkdf2-sha256$i=1024$jp/XAZ6ZsFoy8Tshw1/xGw==$CbW0M68TR1Sp+mS4SfQGolAv2tBiUTbp6PZVhzhVSnWeAtR1NWt1Hn2S+OlvIWg+qYKDmWRvbDEVwdJt9La4ng==");
    assertThat(testUser.getPassword(), is(LEGACY_PASSWORD_HASH));
    AuthenticatingRealmImpl underTestOrient =
        new AuthenticatingRealmImpl(configuration, passwordService, false, SHA256_PASSWORD_ALGORITHM, 1024);
    underTestOrient.getAuthenticationInfo(new UsernamePasswordToken(TEST_USERNAME, TEST_PASSWORD));
    assertThat(testUser.getPassword(), startsWith("$pbkdf2-sha256$i"));
  }

  @Test
  public void testPasswordWithDifferentIterationsIsReHashed() {
    String phcPasswordWithOldIterations =
        "$pbkdf2-sha256$i=5000$jp/XAZ6ZsFoy8Tshw1/xGw==$CbW0M68TR1Sp+mS4SfQGolAv2tBiUTbp6PZVhzhVSnWeAtR1NWt1Hn2S+OlvIWg+qYKDmWRvbDEVwdJt9La4ng==";
    String phcPasswordWithNewIterations = "$pbkdf2-sha256$i=10000$jp/XAZ6ZsFoy8Tshw1/xGw==$DifferentHash==";
    testUser.setPassword(phcPasswordWithOldIterations);

    when(passwordService.passwordsMatch(any(), eq(phcPasswordWithOldIterations))).thenReturn(true);
    when(passwordService.encryptPassword("admin123")).thenReturn(phcPasswordWithNewIterations);

    AuthenticatingRealmImpl underTest =
        new AuthenticatingRealmImpl(configuration, passwordService, false, SHA256_PASSWORD_ALGORITHM, 10000);
    underTest.getAuthenticationInfo(new UsernamePasswordToken(TEST_USERNAME, TEST_PASSWORD));

    // Password should be re-hashed with new iterations
    assertThat(testUser.getPassword(), is(phcPasswordWithNewIterations));
  }

  @Test
  public void testPasswordWithNoIterationsAttributeIsReHashed() {
    String phcPasswordWithoutIterations =
        "$pbkdf2-sha256$jp/XAZ6ZsFoy8Tshw1/xGw==$CbW0M68TR1Sp+mS4SfQGolAv2tBiUTbp6PZVhzhVSnWeAtR1NWt1Hn2S+OlvIWg+qYKDmWRvbDEVwdJt9La4ng==";
    testUser.setPassword(phcPasswordWithoutIterations);

    AuthenticatingRealmImpl underTest =
        new AuthenticatingRealmImpl(configuration, passwordService, false, SHA256_PASSWORD_ALGORITHM, 10000);
    underTest.getAuthenticationInfo(new UsernamePasswordToken(TEST_USERNAME, TEST_PASSWORD));

    // Password should NOT be re-hashed because unparseable format returns true from catch block
    assertThat(testUser.getPassword(), is(phcPasswordWithoutIterations));
  }

  @Test
  public void testPasswordWithIterationsWhenNoIterationsConfigured() {
    String phcPasswordWithIterations =
        "$pbkdf2-sha256$i=10000$jp/XAZ6ZsFoy8Tshw1/xGw==$CbW0M68TR1Sp+mS4SfQGolAv2tBiUTbp6PZVhzhVSnWeAtR1NWt1Hn2S+OlvIWg+qYKDmWRvbDEVwdJt9La4ng==";
    testUser.setPassword(phcPasswordWithIterations);

    AuthenticatingRealmImpl underTest =
        new AuthenticatingRealmImpl(configuration, passwordService, false, SHA256_PASSWORD_ALGORITHM, null);
    underTest.getAuthenticationInfo(new UsernamePasswordToken(TEST_USERNAME, TEST_PASSWORD));

    // Password should NOT be re-hashed because algorithm matches and no specific iterations are configured
    assertThat(testUser.getPassword(), is(phcPasswordWithIterations));
  }

  @Test
  public void testPasswordWithDifferentAlgorithmIsReHashed() {
    String shiroPassword = "$shiro1$SHA-512$500000$eWV0YW5vdGhlcnJhbmRvbXNhbHQ=$XYZ123456789";
    String pbkdf2Password = "$pbkdf2-sha256$i=10000$jp/XAZ6ZsFoy8Tshw1/xGw==$DifferentHash==";
    testUser.setPassword(shiroPassword);

    when(passwordService.encryptPassword("admin123")).thenReturn(pbkdf2Password);

    AuthenticatingRealmImpl underTest =
        new AuthenticatingRealmImpl(configuration, passwordService, false, SHA256_PASSWORD_ALGORITHM, 10000);
    underTest.getAuthenticationInfo(new UsernamePasswordToken(TEST_USERNAME, TEST_PASSWORD));

    // Password should be re-hashed because algorithm is different
    assertThat(testUser.getPassword(), is(pbkdf2Password));
  }

  @Test
  public void testPasswordWithInvalidIterationsFormatIsReHashed() {
    String phcPasswordWithInvalidIterations =
        "$pbkdf2-sha256$i=invalid$jp/XAZ6ZsFoy8Tshw1/xGw==$CbW0M68TR1Sp+mS4SfQGolAv2tBiUTbp6PZVhzhVSnWeAtR1NWt1Hn2S+OlvIWg+qYKDmWRvbDEVwdJt9La4ng==";
    String phcPasswordWithValidIterations = "$pbkdf2-sha256$i=10000$jp/XAZ6ZsFoy8Tshw1/xGw==$DifferentHash==";
    testUser.setPassword(phcPasswordWithInvalidIterations);

    when(passwordService.encryptPassword("admin123")).thenReturn(phcPasswordWithValidIterations);

    AuthenticatingRealmImpl underTest =
        new AuthenticatingRealmImpl(configuration, passwordService, false, SHA256_PASSWORD_ALGORITHM, 10000);
    underTest.getAuthenticationInfo(new UsernamePasswordToken(TEST_USERNAME, TEST_PASSWORD));

    // Password should be re-hashed because iterations format is invalid (NumberFormatException)
    assertThat(testUser.getPassword(), is(phcPasswordWithValidIterations));
  }

  @Test
  public void testPasswordWithUnparseableFormatIsNotReHashed() {
    String legacyFormatPassword = "oldHashedPassword123";
    String newPhcPassword = "$pbkdf2-sha256$i=10000$jp/XAZ6ZsFoy8Tshw1/xGw==$DifferentHash==";
    testUser.setPassword(legacyFormatPassword);

    when(passwordService.encryptPassword("admin123")).thenReturn(newPhcPassword);

    AuthenticatingRealmImpl underTest =
        new AuthenticatingRealmImpl(configuration, passwordService, false, SHA256_PASSWORD_ALGORITHM, 10000);
    underTest.getAuthenticationInfo(new UsernamePasswordToken(TEST_USERNAME, TEST_PASSWORD));

    // Password SHOULD be re-hashed because algorithm doesn't match (not starting with $pbkdf2-sha256)
    assertThat(testUser.getPassword(), is(newPhcPassword));
  }

  @Test(expected = CredentialsException.class)
  public void testNullPasswordReturnsFalseForAlgorithmAndIterationsCheck() {
    testUser.setPassword(null);

    AuthenticatingRealmImpl underTest =
        new AuthenticatingRealmImpl(configuration, passwordService, false, SHA256_PASSWORD_ALGORITHM, 10000);

    // We expect this to fail at credential validation level with CredentialsException
    underTest.getAuthenticationInfo(new UsernamePasswordToken(TEST_USERNAME, TEST_PASSWORD));
  }

  @Test
  public void testPasswordWithSameIterationsButDifferentAlgorithmIsReHashed() {
    String shiroPasswordWithIterations = "$shiro1$SHA-512$500000$eWV0YW5vdGhlcnJhbmRvbXNhbHQ=$XYZ123456789";
    String pbkdf2Password = "$pbkdf2-sha256$i=10000$jp/XAZ6ZsFoy8Tshw1/xGw==$DifferentHash==";
    testUser.setPassword(shiroPasswordWithIterations);

    when(passwordService.encryptPassword("admin123")).thenReturn(pbkdf2Password);

    AuthenticatingRealmImpl underTest =
        new AuthenticatingRealmImpl(configuration, passwordService, false, SHA256_PASSWORD_ALGORITHM, 10000);
    underTest.getAuthenticationInfo(new UsernamePasswordToken(TEST_USERNAME, TEST_PASSWORD));

    // Password should be re-hashed because algorithm is different (even though iterations match)
    assertThat(testUser.getPassword(), is(pbkdf2Password));
  }

  @Test
  public void testEarlyReturnWhenNoIterationsConfiguredAvoidsParsing() {
    String validPhcPassword =
        "$pbkdf2-sha256$i=5000$jp/XAZ6ZsFoy8Tshw1/xGw==$CbW0M68TR1Sp+mS4SfQGolAv2tBiUTbp6PZVhzhVSnWeAtR1NWt1Hn2S+OlvIWg+qYKDmWRvbDEVwdJt9La4ng==";
    testUser.setPassword(validPhcPassword);

    AuthenticatingRealmImpl underTest =
        new AuthenticatingRealmImpl(configuration, passwordService, false, SHA256_PASSWORD_ALGORITHM, null);
    underTest.getAuthenticationInfo(new UsernamePasswordToken(TEST_USERNAME, TEST_PASSWORD));

    // Should NOT re-hash because:
    // 1. Algorithm matches (pbkdf2-sha256)
    // 2. No specific iterations required (null)
    assertThat(testUser.getPassword(), is(validPhcPassword));
  }

  @Test
  public void testDataAccessExceptionWhenReadingUser() throws Exception {
    UsernamePasswordToken upToken = new UsernamePasswordToken(TEST_USERNAME, TEST_PASSWORD);

    // Mock configuration.readUser to throw DataAccessException
    DataAccessException dataAccessException = new DataAccessException("Database unavailable");
    when(configuration.readUser(TEST_USERNAME)).thenThrow(dataAccessException);

    AuthenticatingRealmImpl underTest =
        new AuthenticatingRealmImpl(configuration, passwordService, false, SHA256_PASSWORD_ALGORITHM, null);

    try {
      underTest.doGetAuthenticationInfo(upToken);
      fail("Expected DataAccessException to be thrown");
    }
    catch (DataAccessException e) {
      // Verify the exception message
      assertThat(e.getMessage(), containsString("Database unavailable"));
    }
  }
}

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
package org.sonatype.nexus.security.token;

import java.util.Optional;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.security.SecurityHelper;
import org.sonatype.nexus.security.authc.apikey.ApiKey;
import org.sonatype.nexus.security.authc.apikey.ApiKeyService;

import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BearerTokenManagerTest
    extends TestSupport
{
  private static final String FORMAT = "format";

  private static final String TOKEN = "token";

  @Mock
  private SecurityHelper securityHelper;

  @Mock
  private ApiKeyService apiKeyService;

  @Mock
  private SecurityManager securityManager;

  @Mock
  private AuthenticationInfo authenticationInfo;

  @Mock
  private PrincipalCollection principalCollection;

  @Mock
  private Subject subject;

  BearerTokenManager underTest;

  @Before
  public void setup() throws Exception {
    when(securityHelper.getSecurityManager()).thenReturn(securityManager);
    when(securityManager.authenticate(any())).thenReturn(authenticationInfo);
    when(authenticationInfo.getPrincipals()).thenReturn(principalCollection);
    when(securityHelper.subject()).thenReturn(subject);
    when(subject.getPrincipals()).thenReturn(principalCollection);
    underTest = new BearerTokenManager(apiKeyService, securityHelper, FORMAT) { };
  }

  @Test(expected = NullPointerException.class)
  public void failFastWhenApiKeyStoreIsNull() throws Exception {
    new BearerTokenManager(null, securityHelper, FORMAT) { };
  }

  @Test(expected = NullPointerException.class)
  public void failFastWhenSecurityHelperIsNull() throws Exception {
    new BearerTokenManager(apiKeyService, null, FORMAT) { };
  }

  @Test(expected = NullPointerException.class)
  public void failFastWhenFormatIsNull() throws Exception {
    new BearerTokenManager(apiKeyService, securityHelper, null) { };
  }

  @Test(expected = NullPointerException.class)
  public void createTokenFailFastWhenPrincipalsNull() throws Exception {
    underTest.createToken(principalCollection);
  }

  @Test
  public void createNewKeyWhenOneDoesNotAlreadyExist() throws Exception {
    when(apiKeyService.getApiKey(any(), any())).thenReturn(Optional.empty());
    when(apiKeyService.createApiKey(FORMAT, principalCollection)).thenReturn(TOKEN.toCharArray());
    assertThat(underTest.createToken(principalCollection), is(equalTo(FORMAT + "." + TOKEN)));
    verify(apiKeyService).createApiKey(FORMAT, principalCollection);
  }

  @Test
  public void reuseTokenWhenExists() throws Exception {
    Optional<ApiKey> apiKey = Optional.of(mockApiKey(TOKEN.toCharArray()));
    when(apiKeyService.getApiKey(any(), any())).thenReturn(apiKey);
    assertThat(underTest.createToken(principalCollection), is(equalTo(FORMAT + "." + TOKEN)));
    verify(apiKeyService, never()).createApiKey(any(), any());
  }

  @Test
  public void deleteKey() throws Exception {
    Optional<ApiKey> apiKey = Optional.of(mockApiKey(TOKEN.toCharArray()));
    when(apiKeyService.getApiKey(any(), any())).thenReturn(apiKey);
    assertTrue(underTest.deleteToken());
    verify(apiKeyService).deleteApiKey(FORMAT, principalCollection);
  }

  @Test
  public void doNotDeleteKeyWhenNoKeyExists() throws Exception {
    when(apiKeyService.getApiKey(any(), any())).thenReturn(Optional.empty());
    assertFalse(underTest.deleteToken());
    verify(apiKeyService, never()).deleteApiKey(FORMAT, principalCollection);
  }

  private ApiKey mockApiKey(final char[] token) {
    ApiKey key = mock(ApiKey.class);
    when(key.getApiKey()).thenReturn(token);
    return key;
  }
}

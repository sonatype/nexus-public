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
package org.sonatype.nexus.internal.email;

import org.apache.commons.mail.EmailConstants;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.crypto.secrets.Secret;
import org.sonatype.nexus.crypto.secrets.SecretsService;
import org.sonatype.nexus.email.EmailConfiguration;
import org.sonatype.nexus.security.UserIdHelper;
import org.sonatype.nexus.ssl.TrustStore;

import org.apache.commons.mail.Email;
import org.apache.commons.mail.SimpleEmail;
import org.mockito.MockedStatic;

import javax.inject.Provider;
import javax.net.ssl.SSLContext;
import java.util.function.Function;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.*;
import static org.sonatype.nexus.internal.email.EmailManagerImpl.EMAIL_CONFIGURATION_SOURCE;

public class EmailManagerImplTest
    extends TestSupport
{
  private static final String PASSWORD_PLACEHOLDER = "#~NXRM~PLACEHOLDER~PASSWORD~#";

  @Mock
  private EventManager eventManager;

  @Mock
  private EmailConfigurationStore emailConfigurationStore;

  @Mock
  private TrustStore trustStore;

  @Mock
  private Function<EmailConfiguration, EmailConfiguration> defaults;

  @Mock
  private Provider capabilityRegistryProvider;

  @Mock
  private SecretsService secretsService;

  @InjectMocks
  private EmailManagerImpl emailManager;

  private MockedStatic<UserIdHelper> userIdHelperMock;

  @Before
  public void setup() {
    userIdHelperMock = mockStatic(UserIdHelper.class);
    userIdHelperMock.when(UserIdHelper::get).thenReturn("userId");
  }

  @After
  public void tearDown() {
    userIdHelperMock.close();
  }

  @Test
  public void testConfiguresEmailsCorrectly() throws Exception {
    when(trustStore.getSSLContext()).thenReturn(SSLContext.getDefault());

    EmailConfiguration emailConfig = mock(EmailConfiguration.class);
    when(emailConfig.getHost()).thenReturn("example.com");
    when(emailConfig.getPort()).thenReturn(25);
    when(emailConfig.getFromAddress()).thenReturn("sender@example.com");
    when(emailConfig.getUsername()).thenReturn("user");
    when(emailConfig.isStartTlsEnabled()).thenReturn(true);
    when(emailConfig.isStartTlsRequired()).thenReturn(false);
    when(emailConfig.isSslOnConnectEnabled()).thenReturn(false);
    when(emailConfig.isSslCheckServerIdentityEnabled()).thenReturn(false);
    when(emailConfig.isNexusTrustStoreEnabled()).thenReturn(true);

    SimpleEmail mail = spy(SimpleEmail.class);

    Email email = emailManager.apply(emailConfig, mail, "pass");

    assertThat(email.isStartTLSEnabled(), is(true));
    assertThat(email.isStartTLSRequired(), is(false));
    assertThat(email.isSSLOnConnect(), is(false));
    assertThat(email.isSSLCheckServerIdentity(), is(false));
    assertThat(email.getMailSession().getProperties().containsKey("mail.smtp.ssl.socketFactory"), is(true));
    assertThat(email.getMailSession().getProperties().containsKey(EmailConstants.MAIL_SMTP_SSL_ENABLE), is(true));
    assertThat(email.getMailSession().getProperties().containsKey("mail.smtp.socketFactory.class"), is(false));
    verify(mail).setAuthentication("user", "pass");
  }

  @Test
  public void testSetConfiguration() {
    EmailConfiguration oldEmailConfig = mock(EmailConfiguration.class);
    when(emailConfigurationStore.load()).thenReturn(oldEmailConfig);
    when(oldEmailConfig.copy()).thenReturn(oldEmailConfig);
    Secret oldPass = mock(Secret.class);
    when(oldEmailConfig.getPassword()).thenReturn(oldPass);
    EmailConfiguration emailConfig = mock(EmailConfiguration.class);
    Secret secret = mock(Secret.class);
    when(emailConfig.copy()).thenReturn(emailConfig);
    when(secretsService.encrypt(anyString(), any(), anyString())).thenReturn(secret);

    emailManager.setConfiguration(emailConfig, "newPassword");

    verify(emailConfigurationStore).save(emailConfig);
    verify(secretsService).encrypt(EMAIL_CONFIGURATION_SOURCE, "newPassword".toCharArray(), "userId");
    verify(secretsService).remove(oldPass);
    verify(eventManager).post(any());
  }

  @Test
  public void testSetConfigurationWithEmptyPassword() {
    EmailConfiguration oldEmailConfig = mock(EmailConfiguration.class);
    when(emailConfigurationStore.load()).thenReturn(oldEmailConfig);
    when(oldEmailConfig.copy()).thenReturn(oldEmailConfig);
    Secret oldPass = mock(Secret.class);
    when(oldEmailConfig.getPassword()).thenReturn(oldPass);
    EmailConfiguration emailConfig = mock(EmailConfiguration.class);
    Secret secret = mock(Secret.class);
    when(emailConfig.copy()).thenReturn(emailConfig);
    when(secretsService.encrypt(anyString(), any(), anyString())).thenReturn(secret);

    emailManager.setConfiguration(emailConfig, "");

    verify(emailConfigurationStore).save(emailConfig);
    verify(secretsService, never()).encrypt(anyString(), any(), anyString());
    verify(secretsService).remove(oldPass);
    verify(eventManager).post(any());
  }

  @Test
  public void testSetConfigurationWithException() {
    EmailConfiguration oldEmailConfig = mock(EmailConfiguration.class);
    when(emailConfigurationStore.load()).thenReturn(oldEmailConfig);
    when(oldEmailConfig.copy()).thenReturn(oldEmailConfig);
    Secret oldPass = mock(Secret.class);
    EmailConfiguration emailConfig = mock(EmailConfiguration.class);
    Secret secret = mock(Secret.class);
    when(emailConfig.copy()).thenReturn(emailConfig);
    when(secretsService.encrypt(anyString(), any(), anyString())).thenReturn(secret);
    when(emailConfig.getPassword()).thenReturn(secret);
    doThrow(new RuntimeException()).when(emailConfigurationStore).save(emailConfig);

    assertThrows(RuntimeException.class, () -> emailManager.setConfiguration(emailConfig, "newPassword"));

    verify(secretsService).encrypt(EMAIL_CONFIGURATION_SOURCE, "newPassword".toCharArray(), "userId");
    verify(secretsService).remove(secret);
    verify(secretsService, never()).remove(oldPass);
  }

  @Test
  public void testSetConfigurationWithPlaceHolder() {
    EmailConfiguration oldEmailConfig = mock(EmailConfiguration.class);
    when(emailConfigurationStore.load()).thenReturn(oldEmailConfig);
    when(oldEmailConfig.copy()).thenReturn(oldEmailConfig);
    Secret oldPass = mock(Secret.class);
    when(oldEmailConfig.getPassword()).thenReturn(oldPass);
    EmailConfiguration emailConfig = mock(EmailConfiguration.class);
    when(emailConfig.copy()).thenReturn(emailConfig);
    when(emailConfig.getPassword()).thenReturn(oldPass);

    emailManager.setConfiguration(emailConfig, PASSWORD_PLACEHOLDER);

    verify(emailConfigurationStore).save(emailConfig);
    verify(secretsService, never()).encrypt(EMAIL_CONFIGURATION_SOURCE, PASSWORD_PLACEHOLDER.toCharArray(), "userId");
    verify(secretsService, never()).remove(oldPass);
    verify(eventManager).post(any());
  }

  @Test
  public void testSendWithEnabledConfiguration() throws Exception {
    EmailConfiguration emailConfig = mock(EmailConfiguration.class);
    when(emailConfig.isEnabled()).thenReturn(true);
    when(emailConfigurationStore.load()).thenReturn(emailConfig);
    when(trustStore.getSSLContext()).thenReturn(SSLContext.getDefault());

    Email email = mock(Email.class);

    emailManager.send(email);

    verify(email).send();
  }

  @Test
  public void testSendWithDisabledConfiguration() throws Exception {
    EmailConfiguration emailConfig = mock(EmailConfiguration.class);
    when(emailConfig.isEnabled()).thenReturn(false);
    when(emailConfigurationStore.load()).thenReturn(emailConfig);

    Email email = mock(Email.class);

    emailManager.send(email);

    verify(email, never()).send();
  }
}

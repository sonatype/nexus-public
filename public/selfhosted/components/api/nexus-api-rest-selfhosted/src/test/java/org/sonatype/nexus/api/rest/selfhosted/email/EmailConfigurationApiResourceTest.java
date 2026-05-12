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
package org.sonatype.nexus.api.rest.selfhosted.email;

import javax.ws.rs.core.Response;

import org.sonatype.nexus.api.rest.selfhosted.email.model.ApiEmailConfiguration;
import org.sonatype.nexus.api.rest.selfhosted.email.model.ApiEmailValidation;
import org.sonatype.nexus.common.text.Strings2;
import org.sonatype.nexus.email.EmailConfiguration;
import org.sonatype.nexus.email.EmailManager;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension.WithUser;
import org.sonatype.nexus.testcommon.validation.ValidationExtension;

import org.apache.commons.mail.EmailException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ExtendWith(ValidationExtension.class)
@ExtendWith(AuthenticationExtension.class)
@WithUser
class EmailConfigurationApiResourceTest
{
  @Mock
  private EmailManager emailManager;

  @Mock
  private EmailConfiguration emailConfiguration;

  private EmailConfigurationApiResource underTest;

  @BeforeEach
  void setup() {
    underTest = new EmailConfigurationApiResource(emailManager);
  }

  @Test
  void getUnconfiguredEmailConfigurationHandlesNullDefaultConfiguration() {
    ApiEmailConfiguration response = underTest.getEmailConfiguration();

    assertThat(response.getFromAddress(), is(nullValue()));
    assertThat(response.getHost(), is(nullValue()));
    assertThat(response.getPassword(), is(nullValue()));
    assertThat(response.getPort(), is(nullValue()));
    assertThat(response.getSubjectPrefix(), is(nullValue()));
    assertThat(response.getUsername(), is(nullValue()));
    assertThat(response.isEnabled(), is(false));
    assertThat(response.isNexusTrustStoreEnabled(), is(false));
    assertThat(response.isSslOnConnectEnabled(), is(false));
    assertThat(response.isSslServerIdentityCheckEnabled(), is(false));
    assertThat(response.isStartTlsEnabled(), is(false));
    assertThat(response.isStartTlsRequired(), is(false));
  }

  @Test
  void getEmailConfigurationObfuscatesThePassword() {
    when(emailManager.getConfiguration()).thenReturn(emailConfiguration);

    ApiEmailConfiguration response = underTest.getEmailConfiguration();

    assertThat(response.getPassword(), is(nullValue()));
  }

  @Test
  void setEmailConfigurationSetsTheNewConfiguration() {
    EmailConfiguration newConfiguration = mock(EmailConfiguration.class);
    String newPassword = "testPassword";
    ApiEmailConfiguration request = new ApiEmailConfiguration();
    request.setEnabled(true);
    request.setPassword(newPassword);

    when(emailManager.newConfiguration()).thenReturn(newConfiguration);

    underTest.setEmailConfiguration(request);

    verify(emailManager).setConfiguration(newConfiguration, newPassword);
    verify(newConfiguration).setEnabled(true);
  }

  @Test
  void setEmailConfigurationKeepsTheOriginalPassword() {
    EmailConfiguration newConfiguration = mock(EmailConfiguration.class);
    when(emailManager.newConfiguration()).thenReturn(newConfiguration);

    ApiEmailConfiguration request = new ApiEmailConfiguration();
    request.setEnabled(true);
    request.setPassword(Strings2.EMPTY);

    underTest.setEmailConfiguration(request);

    verify(newConfiguration).setEnabled(true);
    verify(emailManager).setConfiguration(newConfiguration, Strings2.EMPTY);
  }

  @Test
  void testEmailConfiguration_Success() throws EmailException {
    when(emailManager.getConfiguration()).thenReturn(emailConfiguration);
    String destinationAddress = "test@example.com";

    Response response = underTest.testEmailConfiguration(destinationAddress);

    assertThat(response, is(notNullValue()));
    assertThat(response.getStatus(), is(200));
    assertThat(response.hasEntity(), is(true));

    ApiEmailValidation validation = (ApiEmailValidation) response.getEntity();
    assertThat(validation.isSuccess(), is(true));
    assertThat(validation.getReason(), is(""));

    verify(emailManager).sendVerification(emailConfiguration, destinationAddress);
  }

  @Test
  void testEmailConfiguration_NullConfiguration() {
    when(emailManager.getConfiguration()).thenReturn(null);
    String destinationAddress = "test@example.com";

    Response response = underTest.testEmailConfiguration(destinationAddress);

    assertThat(response, is(notNullValue()));
    assertThat(response.getStatus(), is(400));
    assertThat(response.hasEntity(), is(true));

    ApiEmailValidation validation = (ApiEmailValidation) response.getEntity();
    assertThat(validation.isSuccess(), is(false));
    assertThat(validation.getReason(), is("Email Settings are not yet configured"));
  }

  @Test
  void testEmailConfiguration_EmailExceptionWithRootCause() throws EmailException {
    when(emailManager.getConfiguration()).thenReturn(emailConfiguration);
    String destinationAddress = "test@example.com";

    IllegalStateException rootCause = new IllegalStateException("Connection refused");
    EmailException emailException = new EmailException("Failed to send email", rootCause);
    doThrow(emailException).when(emailManager).sendVerification(emailConfiguration, destinationAddress);

    Response response = underTest.testEmailConfiguration(destinationAddress);

    assertThat(response, is(notNullValue()));
    assertThat(response.getStatus(), is(400));
    assertThat(response.hasEntity(), is(true));

    ApiEmailValidation validation = (ApiEmailValidation) response.getEntity();
    assertThat(validation.isSuccess(), is(false));
    assertThat(validation.getReason(), is("Connection refused"));
  }

  @Test
  void testEmailConfiguration_EmailExceptionWithoutRootCause() throws EmailException {
    when(emailManager.getConfiguration()).thenReturn(emailConfiguration);
    String destinationAddress = "test@example.com";

    EmailException emailException = new EmailException("Invalid email address");
    doThrow(emailException).when(emailManager).sendVerification(emailConfiguration, destinationAddress);

    Response response = underTest.testEmailConfiguration(destinationAddress);

    assertThat(response, is(notNullValue()));
    assertThat(response.getStatus(), is(400));
    assertThat(response.hasEntity(), is(true));

    ApiEmailValidation validation = (ApiEmailValidation) response.getEntity();
    assertThat(validation.isSuccess(), is(false));
    assertThat(validation.getReason(), is("Invalid email address"));
  }
}

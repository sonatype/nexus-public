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
package org.sonatype.nexus.coreui;

import jakarta.validation.Validator;

import org.sonatype.nexus.bootstrap.validation.ValidationConfiguration;
import org.sonatype.nexus.crypto.secrets.Secret;
import org.sonatype.nexus.email.EmailConfiguration;
import org.sonatype.nexus.email.EmailManager;
import org.sonatype.nexus.rapture.PasswordPlaceholder;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension.WithUser;
import org.sonatype.nexus.testcommon.validation.ValidationExtension;
import org.sonatype.nexus.testcommon.validation.ValidationExtension.ValidationExecutor;

import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorFactoryImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ExtendWith(ValidationExtension.class)
@ExtendWith(AuthenticationExtension.class)
@WithUser
class EmailComponentTest
{
  private static final String ADDRESS = "you@somewhere.com";

  @ValidationExecutor
  private final Validator validator =
      new ValidationConfiguration().validatorFactory(new ConstraintValidatorFactoryImpl()).getValidator();

  @Mock
  private EmailManager emailManager;

  @Mock
  private EmailConfiguration emailConfiguration;

  @InjectMocks
  private EmailComponent underTest;

  @BeforeEach
  void setUp() {
    lenient().when(emailManager.getConfiguration()).thenReturn(emailConfiguration);
    lenient().when(emailConfiguration.isEnabled()).thenReturn(false);
    lenient().when(emailConfiguration.getHost()).thenReturn("localhost");
    lenient().when(emailConfiguration.getPort()).thenReturn(25);
    lenient().when(emailConfiguration.getFromAddress()).thenReturn("nexus@example.org");
    lenient().when(emailConfiguration.getUsername()).thenReturn("foo");
    lenient().when(emailConfiguration.isStartTlsEnabled()).thenReturn(true);
    lenient().when(emailConfiguration.isStartTlsRequired()).thenReturn(true);
    lenient().when(emailConfiguration.isSslOnConnectEnabled()).thenReturn(true);
    lenient().when(emailConfiguration.getSubjectPrefix()).thenReturn("prefix");
  }

  @Test
  void readReturnsCurrentConfigurationWithPasswordPlaceHolder() {
    when(emailConfiguration.getPassword()).thenReturn(mock(Secret.class));

    EmailConfigurationXO actualConfig = underTest.read();

    assertThat(actualConfig, is(notNullValue()));
    assertThat(actualConfig.isEnabled(), is(false));
    assertThat(actualConfig.getHost(), is("localhost"));
    assertThat(actualConfig.getPort(), is(25));
    assertThat(actualConfig.getUsername(), is("foo"));
    assertThat(actualConfig.getFromAddress(), is("nexus@example.org"));
    assertThat(actualConfig.getPassword(), is(PasswordPlaceholder.get()));
    assertThat(actualConfig.isStartTlsEnabled(), is(true));
    assertThat(actualConfig.isStartTlsRequired(), is(true));
    assertThat(actualConfig.isSslOnConnectEnabled(), is(true));
    assertThat(actualConfig.getSubjectPrefix(), is("prefix"));
    assertThat(actualConfig.isNexusTrustStoreEnabled(), is(false));
    assertThat(actualConfig.isSslCheckServerIdentityEnabled(), is(false));
  }

  @Test
  void readReturnsCurrentConfigurationWithEmptyPasswordPlaceHolder() {
    when(emailConfiguration.getPassword()).thenReturn(null);

    EmailConfigurationXO actualConfig = underTest.read();

    assertThat(actualConfig, is(notNullValue()));
    assertThat(actualConfig.isEnabled(), is(false));
    assertThat(actualConfig.getHost(), is("localhost"));
    assertThat(actualConfig.getPort(), is(25));
    assertThat(actualConfig.getUsername(), is("foo"));
    assertThat(actualConfig.getFromAddress(), is("nexus@example.org"));
    assertThat(actualConfig.getPassword(), is(nullValue()));
    assertThat(actualConfig.isStartTlsEnabled(), is(true));
    assertThat(actualConfig.isStartTlsRequired(), is(true));
    assertThat(actualConfig.isSslOnConnectEnabled(), is(true));
    assertThat(actualConfig.getSubjectPrefix(), is("prefix"));
    assertThat(actualConfig.isNexusTrustStoreEnabled(), is(false));
    assertThat(actualConfig.isSslCheckServerIdentityEnabled(), is(false));
  }

  @Test
  void updateSavesConfiguration() {
    when(emailManager.newConfiguration()).thenReturn(emailConfiguration);

    underTest.update(getConfigurationXO("baz"));

    verify(emailManager).setConfiguration(emailConfiguration, "baz");
    verify(emailManager).getConfiguration();
  }

  @Test
  void sendVerification() throws Exception {
    EmailConfigurationXO formCredentials = getConfigurationXO("baz");
    EmailConfiguration emailConfig = mock(EmailConfiguration.class);
    when(emailManager.newConfiguration()).thenReturn(emailConfig);

    underTest.sendVerification(formCredentials, ADDRESS);

    verify(emailManager).sendVerification(emailConfig, "baz", ADDRESS);
  }

  private static EmailConfigurationXO getConfigurationXO(final String password) {
    return new EmailConfigurationXO(
        false, "localhost", 25,
        "foo", password, "nexus@example.org",
        null, false, false,
        false, false, false);
  }
}

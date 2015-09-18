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
package com.sonatype.nexus.ssl.plugin.internal;

import java.util.Properties;

import javax.net.ssl.SSLContext;

import com.sonatype.nexus.ssl.plugin.TrustStore;

import org.sonatype.goodies.testsupport.TestSupport;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link SMTPSessionParametersCustomizer} UTs.
 *
 * @since ssl 1.0
 */
public class SMTPSessionParametersCustomizerTest
    extends TestSupport
{

  /**
   * Verify that "mail.smtp.socketFactory" is added to params when truststore is enabled for SMTP and SMTP is using
   * SSL/TLS ("mail.smtp.socketFactory.class" is present in params).
   */
  @Test
  public void socketFactoryAddedToParams()
      throws Exception
  {
    final TrustStore trustStore = mock(TrustStore.class);
    when(trustStore.getSSLContext()).thenReturn(SSLContext.getDefault());

    final SMTPSessionParametersCustomizer underTest = new SMTPSessionParametersCustomizer(trustStore);
    final Properties properties = new Properties();
    properties.setProperty("mail.smtp.socketFactory.class", "Foo.class");
    properties.setProperty("mail.smtp.ssl.useTrustStore", Boolean.TRUE.toString());
    underTest.customize(properties);

    assertThat(properties.keySet(), hasItem((Object) "mail.smtp.ssl.enable"));
    assertThat(properties.keySet(), hasItem((Object) "mail.smtp.ssl.socketFactory"));
    assertThat(properties.keySet(), not(hasItem((Object) "mail.smtp.socketFactory.class")));
  }

  /**
   * Verify that "mail.smtp.socketFactory" is not added to params when truststore is not enabled for SMTP.
   */
  @Test
  public void socketFactoryNotAddedToParamsWhenTrustStoreNotEnabled()
      throws Exception
  {
    final TrustStore trustStore = mock(TrustStore.class);
    when(trustStore.getSSLContext()).thenReturn(SSLContext.getDefault());

    final SMTPSessionParametersCustomizer underTest = new SMTPSessionParametersCustomizer(trustStore);
    final Properties properties = new Properties();
    properties.setProperty("mail.smtp.socketFactory.class", "Foo.class");
    properties.setProperty("mail.smtp.ssl.useTrustStore", Boolean.FALSE.toString());
    underTest.customize(properties);

    assertThat(properties.keySet(), not(hasItem((Object) "mail.smtp.ssl.enable")));
    assertThat(properties.keySet(), not(hasItem((Object) "mail.smtp.ssl.socketFactory")));
    assertThat(properties.keySet(), hasItem((Object) "mail.smtp.socketFactory.class"));
  }

  /**
   * Verify that "mail.smtp.socketFactory" is not added to params when truststore is enabled for SMTP and SMTP is
   * not using SSL/TLS ("mail.smtp.socketFactory.class" is not present in params).
   */
  @Test
  public void socketFactoryNotAddedToParamsWhenSSLParamsNotPresent()
      throws Exception
  {
    final TrustStore trustStore = mock(TrustStore.class);
    when(trustStore.getSSLContext()).thenReturn(SSLContext.getDefault());

    final SMTPSessionParametersCustomizer underTest = new SMTPSessionParametersCustomizer(trustStore);
    final Properties properties = new Properties();
    underTest.customize(properties);

    assertThat(properties.keySet(), not(hasItem((Object) "mail.smtp.ssl.enable")));
    assertThat(properties.keySet(), not(hasItem((Object) "mail.smtp.ssl.socketFactory")));
  }

}

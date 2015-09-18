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

import javax.inject.Inject;

import com.sonatype.nexus.ssl.plugin.TrustStore;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.property.SystemPropertiesHelper;

import static com.google.common.base.Preconditions.checkNotNull;

// FIXME: Need to sort out changes to sisu-mailer or replacement

/**
 * An {@link SMTPSessionParametersCustomizer} that will configure SMTP parameters to use Nexus SSL TrustStore in case
 * that is enabled.
 *
 * @since ssl 1.0
 */
//@Named
//@Singleton
public class SMTPSessionParametersCustomizer
    extends ComponentSupport
    //implements SmtpSessionParametersCustomizer
{

  private final TrustStore trustStore;

  private final boolean checkServerIdentity = SystemPropertiesHelper.getBoolean(
      "org.sonatype.nexus.ssl.smtp.checkServerIdentity", true
  );

  @Inject
  public SMTPSessionParametersCustomizer(final TrustStore trustStore) {
    this.trustStore = checkNotNull(trustStore);
  }

  //@Override
  public Properties customize(final Properties params) {
    final String host = params.getProperty("mail.smtp.host");
    final String port = params.getProperty("mail.smtp.port");
    if (Boolean.valueOf(params.getProperty("mail.smtp.ssl.useTrustStore"))
        && params.remove("mail.smtp.socketFactory.class") != null) {
      log.debug("SMTP is using a Nexus SSL truststore for accessing {}:{}", host, port);

      params.put("mail.smtp.ssl.enable", Boolean.TRUE);
      params.put("mail.smtp.ssl.checkserveridentity", checkServerIdentity);
      params.put("mail.smtp.ssl.socketFactory", trustStore.getSSLContext().getSocketFactory());

      log.debug("SMTP parameters: {}", params);
      return params;
    }
    log.debug("SMTP is using a JVM truststore for accessing {}:{}", host, port);
    return params;
  }

}

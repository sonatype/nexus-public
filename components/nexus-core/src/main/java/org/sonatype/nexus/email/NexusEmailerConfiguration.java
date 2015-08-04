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
package org.sonatype.nexus.email;

import java.util.List;
import java.util.Properties;

import javax.inject.Inject;
import javax.inject.Named;
import javax.mail.Authenticator;
import javax.mail.Session;

import org.sonatype.micromailer.EmailerConfiguration;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Nexus specific {@link EmailerConfiguration}. Uses available {@link SmtpSessionParametersCustomizer}s to customize
 * session parameters before creating the SMTP session.
 *
 * @since 2.4
 */
@Named
public class NexusEmailerConfiguration
    extends EmailerConfiguration
{

  private final List<SmtpSessionParametersCustomizer> customizers;

  @Inject
  public NexusEmailerConfiguration(final List<SmtpSessionParametersCustomizer> customizers) {
    this.customizers = checkNotNull(customizers);
  }

  @Override
  protected Session createSession(final Properties properties, final Authenticator authenticator) {
    Properties params = properties;
    for (final SmtpSessionParametersCustomizer customizer : customizers) {
      params = customizer.customize(params);
    }
    return super.createSession(params, authenticator);
  }

}

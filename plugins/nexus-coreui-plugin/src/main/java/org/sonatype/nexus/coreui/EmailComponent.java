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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Email;

import org.sonatype.nexus.email.EmailConfiguration;
import org.sonatype.nexus.email.EmailManager;
import org.sonatype.nexus.extdirect.DirectComponent;
import org.sonatype.nexus.extdirect.DirectComponentSupport;
import org.sonatype.nexus.rapture.PasswordPlaceholder;
import org.sonatype.nexus.validation.Validate;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.softwarementors.extjs.djn.config.annotations.DirectAction;
import com.softwarementors.extjs.djn.config.annotations.DirectMethod;
import org.apache.commons.mail.EmailException;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Email {@link DirectComponent}.
 *
 * @since 3.0
 */
@Named
@Singleton
@DirectAction(action = "coreui_Email")
public class EmailComponent
    extends DirectComponentSupport
{
  private final EmailManager emailManager;

  @Inject
  public EmailComponent(final EmailManager emailManager) {
    this.emailManager = checkNotNull(emailManager);
  }

  /**
   * Returns current configuration.
   */
  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresPermissions("nexus:settings:read")
  public EmailConfigurationXO read() {
    return convert(emailManager.getConfiguration());
  }

  EmailConfigurationXO convert(final EmailConfiguration value) {
    return new EmailConfigurationXO(
        value.isEnabled(),
        value.getHost(),
        value.getPort(),
        value.getUsername(),
        value.getPassword() != null ? PasswordPlaceholder.get() : null,
        value.getFromAddress(),
        value.getSubjectPrefix(),
        value.isStartTlsEnabled(),
        value.isStartTlsRequired(),
        value.isSslOnConnectEnabled(),
        value.isSslCheckServerIdentityEnabled(),
        value.isNexusTrustStoreEnabled()
    );
  }

  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresAuthentication
  @RequiresPermissions("nexus:settings:update")
  @Validate
  public EmailConfigurationXO update(@NotNull @Valid final EmailConfigurationXO configuration) {
    emailManager.setConfiguration(convert(configuration), configuration.getPassword());
    return read();
  }

  EmailConfiguration convert(final EmailConfigurationXO value) {
    EmailConfiguration emailConfiguration = emailManager.newConfiguration();
    emailConfiguration.setEnabled(value.isEnabled());
    emailConfiguration.setHost(value.getHost());
    emailConfiguration.setPort(value.getPort());
    emailConfiguration.setUsername(value.getUsername());
    emailConfiguration.setFromAddress(value.getFromAddress());
    emailConfiguration.setSubjectPrefix(value.getSubjectPrefix());
    emailConfiguration.setStartTlsEnabled(value.isStartTlsEnabled());
    emailConfiguration.setStartTlsRequired(value.isStartTlsRequired());
    emailConfiguration.setSslOnConnectEnabled(value.isSslOnConnectEnabled());
    emailConfiguration.setSslCheckServerIdentityEnabled(value.isSslCheckServerIdentityEnabled());
    emailConfiguration.setNexusTrustStoreEnabled(value.isNexusTrustStoreEnabled());

    return emailConfiguration;
  }

  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresAuthentication
  @RequiresPermissions("nexus:settings:update")
  @Validate
  public void sendVerification(
      @NotNull @Valid final EmailConfigurationXO configuration,
      @NotNull @Email final String address)
      throws EmailException
  {
    emailManager.sendVerification(convert(configuration), configuration.getPassword(), address);
  }
}

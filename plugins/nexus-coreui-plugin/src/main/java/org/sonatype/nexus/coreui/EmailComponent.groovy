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
package org.sonatype.nexus.coreui

import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import javax.validation.Valid
import javax.validation.constraints.NotNull

import org.sonatype.nexus.email.EmailConfiguration
import org.sonatype.nexus.email.EmailManager
import org.sonatype.nexus.extdirect.DirectComponent
import org.sonatype.nexus.extdirect.DirectComponentSupport
import org.sonatype.nexus.validation.Validate

import com.softwarementors.extjs.djn.config.annotations.DirectAction
import com.softwarementors.extjs.djn.config.annotations.DirectMethod
import groovy.transform.PackageScope
import org.apache.shiro.authz.annotation.RequiresAuthentication
import org.apache.shiro.authz.annotation.RequiresPermissions
import org.hibernate.validator.constraints.Email

/**
 * Email {@link DirectComponent}.
 *
 * @since 3.0
 */
@Named
@Singleton
@DirectAction(action = 'coreui_Email')
class EmailComponent
    extends DirectComponentSupport
{
  @Inject
  EmailManager emailManager

  /**
   * Returns current configuration.
   */
  @DirectMethod
  @RequiresPermissions('nexus:settings:read')
  EmailConfigurationXO read() {
    return convert(emailManager.configuration)
  }

  @PackageScope
  EmailConfigurationXO convert(final EmailConfiguration value) {
    return new EmailConfigurationXO(
        enabled: value.enabled,
        host: value.host,
        port: value.port,
        username: value.username,
        password: value.password,
        fromAddress: value.fromAddress,
        subjectPrefix: value.subjectPrefix
    )
  }

  /**
   * Update configuration, returns updated configuration.
   */
  @DirectMethod
  @RequiresAuthentication
  @RequiresPermissions('nexus:settings:update')
  @Validate
  EmailConfigurationXO update(@NotNull @Valid final EmailConfigurationXO configuration)
  {
    emailManager.configuration = convert(configuration)
    return read()
  }

  @PackageScope
  EmailConfiguration convert(final EmailConfigurationXO value) {
    return new EmailConfiguration(
        enabled: value.enabled,
        host: value.host,
        port: value.port,
        username: value.username,
        password: value.password,
        fromAddress: value.fromAddress,
        subjectPrefix: value.subjectPrefix
    )
  }

  /**
   * Send verification email.
   */
  @DirectMethod
  @RequiresAuthentication
  @RequiresPermissions('nexus:settings:update')
  @Validate
  void sendVerification(
      @NotNull @Valid final EmailConfigurationXO configuration,
      @NotNull @Email final String address)
  {
    emailManager.sendVerification(convert(configuration), address)
  }
}

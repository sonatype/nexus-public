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

import org.sonatype.nexus.extdirect.DirectComponent
import org.sonatype.nexus.extdirect.DirectComponentSupport
import org.sonatype.nexus.security.anonymous.AnonymousConfiguration
import org.sonatype.nexus.security.anonymous.AnonymousManager

import com.softwarementors.extjs.djn.config.annotations.DirectAction
import com.softwarementors.extjs.djn.config.annotations.DirectMethod
import org.apache.shiro.authz.annotation.RequiresAuthentication
import org.apache.shiro.authz.annotation.RequiresPermissions

/**
 * Anonymous Security Settings {@link DirectComponent}.
 *
 * @since 3.0
 */
@Named
@Singleton
@DirectAction(action = 'coreui_AnonymousSettings')
class AnonymousSettingsComponent
    extends DirectComponentSupport
{
  @Inject
  AnonymousManager anonymousManager

  /**
   * Retrieves anonymous security settings.
   * @return anonymous security settings
   */
  @DirectMethod
  @RequiresPermissions('nexus:settings:read')
  AnonymousSettingsXO read() {
    def config = anonymousManager.configuration
    return new AnonymousSettingsXO(
        enabled: config.enabled,
        userId: config.userId,
        realmName: config.realmName
    )
  }

  /**
   * Updates anonymous security settings.
   * @return updated anonymous security settings
   */
  @DirectMethod
  @RequiresAuthentication
  @RequiresPermissions('nexus:settings:update')
  AnonymousSettingsXO update(final @NotNull @Valid AnonymousSettingsXO anonymousXO) {
    anonymousManager.configuration = new AnonymousConfiguration(
        enabled: anonymousXO.enabled,
        userId: anonymousXO.userId,
        realmName: anonymousXO.realmName
    )
    return read()
  }
}

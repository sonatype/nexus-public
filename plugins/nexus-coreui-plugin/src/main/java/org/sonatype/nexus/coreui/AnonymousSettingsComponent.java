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

import org.sonatype.nexus.extdirect.DirectComponent;
import org.sonatype.nexus.extdirect.DirectComponentSupport;
import org.sonatype.nexus.security.anonymous.AnonymousConfiguration;
import org.sonatype.nexus.security.anonymous.AnonymousManager;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.softwarementors.extjs.djn.config.annotations.DirectAction;
import com.softwarementors.extjs.djn.config.annotations.DirectMethod;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Anonymous Security Settings {@link DirectComponent}.
 */
@Named
@Singleton
@DirectAction(action = "coreui_AnonymousSettings")
public class AnonymousSettingsComponent
    extends DirectComponentSupport
{
  private final AnonymousManager anonymousManager;

  @Inject
  public AnonymousSettingsComponent(final AnonymousManager anonymousManager) {
    this.anonymousManager = checkNotNull(anonymousManager);
  }

  /**
   * Retrieves anonymous security settings.
   *
   * @return anonymous security settings
   */
  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresPermissions("nexus:settings:read")
  public AnonymousSettingsXO read() {
    AnonymousConfiguration config = anonymousManager.getConfiguration();
    AnonymousSettingsXO xo = new AnonymousSettingsXO();
    xo.setEnabled(config.isEnabled());
    xo.setUserId(config.getUserId());
    xo.setRealmName(config.getRealmName());
    return xo;
  }

  /**
   * Updates anonymous security settings.
   *
   * @return updated anonymous security settings
   */
  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresAuthentication
  @RequiresPermissions("nexus:settings:update")
  public AnonymousSettingsXO update(@NotNull @Valid final AnonymousSettingsXO anonymousXO) {
    AnonymousConfiguration configuration = anonymousManager.newConfiguration();
    configuration.setEnabled(anonymousXO.getEnabled());
    configuration.setRealmName(anonymousXO.getRealmName());
    configuration.setUserId(anonymousXO.getUserId());
    anonymousManager.setConfiguration(configuration);
    return read();
  }
}

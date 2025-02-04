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

import org.sonatype.nexus.common.app.FreezeService;
import org.sonatype.nexus.extdirect.DirectComponentSupport;
import org.sonatype.nexus.validation.Validate;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.softwarementors.extjs.djn.config.annotations.DirectAction;
import com.softwarementors.extjs.djn.config.annotations.DirectMethod;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Component for freezing and releasing the application.
 *
 * @since 3.2
 */
@Named
@Singleton
@DirectAction(action = "coreui_Freeze")
class FreezeComponent
    extends DirectComponentSupport
{
  private final FreezeService freezeService;

  @Inject
  public FreezeComponent(final FreezeService freezeService) {
    this.freezeService = checkNotNull(freezeService);
  }

  @DirectMethod
  @Timed
  @ExceptionMetered
  public FreezeStatusXO read() {
    return buildStatus();
  }

  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresAuthentication
  @RequiresPermissions("nexus:*")
  @Validate
  public FreezeStatusXO update(final @NotNull @Valid FreezeStatusXO freezeStatusXO) {
    if (freezeStatusXO.isFrozen()) {
      freezeService.requestFreeze("UI request");
    }
    else {
      freezeService.cancelFreeze();
    }
    return buildStatus();
  }

  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresAuthentication
  @RequiresPermissions("nexus:*")
  @Validate
  public FreezeStatusXO forceRelease() {
    freezeService.cancelAllFreezeRequests();
    return buildStatus();
  }

  private FreezeStatusXO buildStatus() {
    FreezeStatusXO freezeStatus = new FreezeStatusXO();
    freezeStatus.setFrozen(freezeService.isFrozen());
    return freezeStatus;
  }
}

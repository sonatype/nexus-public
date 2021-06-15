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
package org.sonatype.nexus.script.plugin.internal;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.script.ScriptManager;

import com.codahale.metrics.health.HealthCheck;

/**
 * Gives an unhealthy response if scripting is enabled in the instance.
 * 
 * @since 3.31
 */
@Named("Scripting")
@Singleton
public class ScriptPluginHealthCheck extends HealthCheck
{
  public static final String SCRIPTING_DISABLED_MESSAGE = "Scripting is disabled.";

  public static final String SCRIPTING_ENABLED_ERROR =
      "Enabling and running scripts is not recommended as this bypasses security checks and can cause your " +
          "Nexus instance to be vulnerable to existing and future attacks. " +
          "We recommend using alternate ways to automate the configuration of this instance where possible.";

  private final ScriptManager scriptManager;

  @Inject
  public ScriptPluginHealthCheck(final ScriptManager scriptManager) {
    this.scriptManager = scriptManager;
  }

  @Override
  protected Result check() {
    return scriptManager.isEnabled() ? Result.unhealthy(SCRIPTING_ENABLED_ERROR) : Result.healthy(SCRIPTING_DISABLED_MESSAGE);
  }
}

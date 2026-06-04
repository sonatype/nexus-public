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
package org.sonatype.nexus.internal.script.groovy;

import org.springframework.beans.factory.annotation.Autowired;
import javax.script.ScriptEngineManager;

import org.sonatype.nexus.bootstrap.entrypoint.configuration.ApplicationDirectories;
import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.SERVICES;
import org.springframework.stereotype.Component;

/**
 * Groovy script engine loader. Is used to run groovy scripts.
 */
@Component
@ManagedLifecycle(phase = SERVICES)
public class GroovyScriptEngineLoader
    extends StateGuardLifecycleSupport
{
  private final ApplicationDirectories applicationDirectories;

  private final ScriptEngineManager scriptEngineManager;

  @Autowired
  public GroovyScriptEngineLoader(
      final ApplicationDirectories applicationDirectories,
      final ScriptEngineManager scriptEngineManager)
  {
    this.applicationDirectories = checkNotNull(applicationDirectories);
    this.scriptEngineManager = checkNotNull(scriptEngineManager);
  }

  @Override
  protected void doStart() throws Exception {
    GroovyScriptEngineFactory groovyEngineFactory =
        new GroovyScriptEngineFactory(applicationDirectories);
    log.debug("Registering engine-factory: {}", groovyEngineFactory);
    groovyEngineFactory.getNames().forEach(name -> scriptEngineManager.registerEngineName(name, groovyEngineFactory));
  }
}

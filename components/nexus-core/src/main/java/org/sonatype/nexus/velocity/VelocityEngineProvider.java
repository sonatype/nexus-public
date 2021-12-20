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
package org.sonatype.nexus.velocity;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.sisu.goodies.common.ComponentSupport;

import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;

/**
 * Nexus preconfigured ans shared Velocity provider.
 *
 * @since 2.8
 */
@Named
@Singleton
public class VelocityEngineProvider
    extends ComponentSupport
    implements Provider<VelocityEngine>
{
  private final VelocityEngine sharedVelocityEngine;

  @Inject
  public VelocityEngineProvider() {
    this.sharedVelocityEngine = createEngine();
  }

  @Override
  public VelocityEngine get() {
    return sharedVelocityEngine;
  }

  private VelocityEngine createEngine() {
    log.info("Creating Nexus VelocityEngine");

    VelocityEngine velocityEngine = new VelocityEngine();

    // setting various defaults
    // ========================
    // to avoid "unable to find resource 'VM_global_library.vm' in any resource loader."
    velocityEngine.setProperty("velocimacro.library.path", "");
    // to use classpath loader
    velocityEngine.setProperty(RuntimeConstants.RESOURCE_LOADERS, "class");
    velocityEngine.setProperty("resource.loader.class.class",
        "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
    // to make us strict with template references (early problem detection)
    velocityEngine.setProperty(RuntimeConstants.RUNTIME_REFERENCES_STRICT, "true");

    velocityEngine.setProperty(RuntimeConstants.RESOURCE_LOADERS, "class");
    velocityEngine.setProperty("resource.loader.class.class",
        "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
    // to set caching ON
    velocityEngine.setProperty("resource.loader.class.cache", "true");
    // to never check for template modification (they are JARred)
    velocityEngine.setProperty(RuntimeConstants.RESOURCE_LOADER_CHECK_INTERVAL, "0");
    // to set strict mode OFF
    velocityEngine.setProperty(RuntimeConstants.RUNTIME_REFERENCES_STRICT, "false");
    // to force templates having inline local scope for VM definitions
    velocityEngine.setProperty("velocimacro.inline.local_scope", "true");

    // fire up the engine
    // ==================
    try {
      velocityEngine.init();
    }
    catch (Exception e) {
      throw new IllegalStateException("Cannot initialize VelocityEngine", e);
    }
    return velocityEngine;
  }
}

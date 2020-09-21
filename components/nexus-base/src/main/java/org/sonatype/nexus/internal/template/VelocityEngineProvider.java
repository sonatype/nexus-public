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
package org.sonatype.nexus.internal.template;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;

import com.google.common.base.Joiner;
import com.google.common.base.Throwables;
import org.apache.velocity.app.VelocityEngine;

import static org.apache.velocity.runtime.RuntimeConstants.RESOURCE_LOADERS;
import static org.apache.velocity.runtime.RuntimeConstants.RESOURCE_LOADER_CACHE;
import static org.apache.velocity.runtime.RuntimeConstants.RESOURCE_LOADER_CHECK_INTERVAL;
import static org.apache.velocity.runtime.RuntimeConstants.RESOURCE_LOADER_CLASS;
import static org.apache.velocity.runtime.RuntimeConstants.RUNTIME_REFERENCES_STRICT;
import static org.apache.velocity.runtime.RuntimeConstants.VM_LIBRARY;
import static org.apache.velocity.runtime.RuntimeConstants.VM_PERM_INLINE_LOCAL;

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
  private final VelocityEngine engine;

  @Inject
  public VelocityEngineProvider() {
    this.engine = create();
  }

  @Override
  public VelocityEngine get() {
    return engine;
  }

  private VelocityEngine create() {
    VelocityEngine engine = new VelocityEngine();
    Joiner j = Joiner.on('.');

    // to avoid "unable to find resource 'VM_global_library.vm' in any resource loader."
    engine.setProperty(VM_LIBRARY, "");

    // to use classpath loader
    engine.setProperty(RESOURCE_LOADERS, RESOURCE_LOADER_CLASS);
    engine.setProperty(j.join(RESOURCE_LOADERS, RESOURCE_LOADER_CLASS, RESOURCE_LOADER_CLASS),
        "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");

    // to set caching ON
    engine.setProperty(j.join(RESOURCE_LOADERS, RESOURCE_LOADER_CLASS, RESOURCE_LOADER_CACHE), "true");

    // to never check for template modification (they are JARred)
    engine.setProperty(j.join(RESOURCE_LOADERS, RESOURCE_LOADER_CLASS, RESOURCE_LOADER_CHECK_INTERVAL), "0");

    // to set strict mode OFF
    engine.setProperty(RUNTIME_REFERENCES_STRICT, "false");

    // to force templates having inline local scope for VM definitions
    engine.setProperty(VM_PERM_INLINE_LOCAL, "true");

    log.debug("Initializing: {}", engine);
    try {
      engine.init();
    }
    catch (Exception e) {
      Throwables.throwIfUnchecked(e);
      throw new RuntimeException(e);
    }

    return engine;
  }
}

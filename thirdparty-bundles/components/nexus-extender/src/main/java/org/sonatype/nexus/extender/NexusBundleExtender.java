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
package org.sonatype.nexus.extender;

import java.util.Dictionary;
import java.util.Hashtable;

import javax.servlet.ServletContextListener;

import org.eclipse.sisu.inject.MutableBeanLocator;
import org.eclipse.sisu.launch.SisuExtender;
import org.eclipse.sisu.launch.SisuTracker;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * {@link Bundle} extender that manages bundles with Nexus components.
 *
 * @since 3.0
 */
public class NexusBundleExtender
    extends SisuExtender
{
  private BundleContext context;

  private NexusContextListener listener;

  private boolean activated;

  @Override
  public void start(final BundleContext ctx) {
    context = ctx;

    listener = new NexusContextListener(this);

    final Dictionary<String, Object> listenerProperties = new Hashtable<>();
    listenerProperties.put("name", "nexus");

    // register our listener; the bootstrap code will call us back with the servlet context
    ctx.registerService(ServletContextListener.class, listener, listenerProperties);
  }

  @Override
  public void stop(final BundleContext ctx) {
    if (listener != null) {
      listener.contextDestroyed(null /* unused */);
      listener = null;
    }
  }

  public BundleContext getBundleContext() {
    return context;
  }

  public void doStart() {
    // start tracking
    super.start(context);
    activated = true;
  }

  public void doStop() {
    listener = null;
    if (activated) {
      // stop tracking
      activated = false;
      super.stop(context);
    }
  }

  @Override
  protected SisuTracker createTracker(final BundleContext ctx) {
    return new NexusBundleTracker(ctx, createLocator(ctx));
  }

  @Override
  protected MutableBeanLocator createLocator(final BundleContext ctx) {
    return listener.getInjector().getInstance(MutableBeanLocator.class);
  }
}
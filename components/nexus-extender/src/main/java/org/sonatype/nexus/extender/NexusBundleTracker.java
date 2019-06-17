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

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.sisu.inject.BindingPublisher;
import org.eclipse.sisu.inject.MutableBeanLocator;
import org.eclipse.sisu.launch.BundlePlan;
import org.eclipse.sisu.launch.SisuTracker;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link Bundle} tracker that tracks and binds bundles with Nexus components.
 *
 * @since 3.0
 */
public class NexusBundleTracker
    extends SisuTracker
{
  private static final Logger log = LoggerFactory.getLogger(NexusBundleTracker.class);

  private final Set<String> visited = ConcurrentHashMap.newKeySet();

  private final Bundle systemBundle;

  private boolean closing = false;

  public NexusBundleTracker(final BundleContext context, final MutableBeanLocator locator) {
    super(context, Bundle.STARTING | Bundle.ACTIVE, locator);
    systemBundle = context.getBundle(0);
  }

  @Override
  protected List<BundlePlan> discoverPlans() {
    return Collections.<BundlePlan> singletonList(new NexusBundlePlan(locator));
  }

  @Override
  public BindingPublisher prepare(final Bundle bundle) {
    if (visited.add(bundle.getSymbolicName()) && hasComponents(bundle)) {
      if ("org.ops4j.pax.url.mvn".equals(bundle.getSymbolicName())) {
        return null; // false-positive, this doesn't need preparing
      }
      prepareDependencies(bundle);
      try {
        BindingPublisher publisher;
        log.info("ACTIVATING {}", bundle);
        publisher = super.prepare(bundle);
        log.info("ACTIVATED {}", bundle);
        return publisher;
      }
      catch (final Exception e) {
        log.warn("BROKEN {}", bundle);
        throw e;
      }
    }
    // make sure we have everything we need to start
    else if (bundle.getBundleContext() == context) {
      prepareDependencies(bundle);
    }
    return null;
  }

  @Override
  public void close() {
    try {
      closing = true;
      purgeBundles();
      super.close();
    }
    finally {
      closing = false;
      visited.clear();
    }
  }

  @Override
  protected boolean evictBundle(final Bundle bundle) {
    // force eviction while extender is closing; otherwise only allow it while the framework is stable
    return closing || (super.evictBundle(bundle) && (systemBundle.getState() & Bundle.STOPPING) == 0);
  }

  private void prepareDependencies(final Bundle bundle) {
    final BundleWiring wiring = bundle.adapt(BundleWiring.class);
    final List<BundleWire> wires = wiring.getRequiredWires(BundleRevision.PACKAGE_NAMESPACE);
    if (wires != null) {
      for (final BundleWire wire : wires) {
        try {
          final Bundle dependency = wire.getProviderWiring().getBundle();
          if (!visited.contains(dependency.getSymbolicName()) && hasComponents(dependency)) {
            if (!live(dependency)) {
              dependency.start();
            }
            if (live(dependency)) {
              // pseudo-event to trigger bundle activation
              addingBundle(dependency, null /* unused */);
            }
          }
        }
        catch (final Exception e) {
          log.warn("MISSING {}", wire, e);
        }
      }
    }
  }

  private static boolean hasComponents(final Bundle bundle) {
    return bundle.getResource("META-INF/sisu/javax.inject.Named") != null;
  }

  private static boolean live(final Bundle bundle) {
    return (bundle.getState() & (Bundle.STARTING | Bundle.ACTIVE)) != 0;
  }
}

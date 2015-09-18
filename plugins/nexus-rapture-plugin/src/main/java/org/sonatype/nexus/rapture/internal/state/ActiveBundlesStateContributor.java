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
package org.sonatype.nexus.rapture.internal.state;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.rapture.StateContributor;

import com.google.common.collect.ImmutableMap;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.util.tracker.BundleTracker;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Contributes {@code activeBundles} state.
 *
 * @since 3.0
 */
@Named
@Singleton
public class ActiveBundlesStateContributor
  extends ComponentSupport
  implements StateContributor
{
  public static final String STATE_ID = "activeBundles";

  private final Set<String> activeBundles = new ConcurrentSkipListSet<>();

  @Inject
  public ActiveBundlesStateContributor(final BundleContext bundleContext) {
    checkNotNull(bundleContext);

    // install tracker to invalidate cache
    new BundleTracker<String>(bundleContext, Bundle.ACTIVE, null)
    {
      @Override
      public String addingBundle(final Bundle bundle, final BundleEvent event) {
        String name = bundle.getSymbolicName();
        String location = bundle.getLocation();

        // ignore wrap bundles, and stop tracking them
        if (location.startsWith("wrap:mvn")) {
          return null;
        }

        activeBundles.add(name);
        return name;
      }

      @Override
      public void removedBundle(final Bundle bundle, final BundleEvent event, final String name) {
        activeBundles.remove(name);
      }
    }.open();
  }

  @Override
  public Map<String, Object> getState() {
    return ImmutableMap.of(STATE_ID, (Object)activeBundles);
  }

}

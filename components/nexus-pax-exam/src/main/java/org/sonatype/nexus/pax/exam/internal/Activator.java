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
package org.sonatype.nexus.pax.exam.internal;

import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;

import org.ops4j.pax.exam.ProbeInvokerFactory;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * Registers our delayed {@link ProbeInvokerFactory} which waits for Nexus to start before invoking the testsuite.
 * 
 * @since 3.0
 */
public class Activator
    implements BundleActivator
{
  public void start(final BundleContext context) throws Exception {
    final Dictionary<String, ?> properties = new Hashtable<>(Collections.singletonMap("driver", "nexus"));
    context.registerService(ProbeInvokerFactory.class, new DelayedProbeInvokerFactory(context), properties);
  }

  public void stop(final BundleContext context) throws Exception {
    // nothing to do...
  }
}

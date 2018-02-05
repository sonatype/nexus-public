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

import org.eclipse.equinox.region.RegionDigraph;
import org.ops4j.pax.exam.ProbeInvoker;
import org.ops4j.pax.exam.ProbeInvokerFactory;
import org.ops4j.pax.exam.TestContainerException;
import org.ops4j.pax.exam.util.Injector;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.util.tracker.ServiceTracker;

import static org.sonatype.nexus.pax.exam.NexusPaxExamSupport.NEXUS_PAX_EXAM_INVOKER_DEFAULT;
import static org.sonatype.nexus.pax.exam.NexusPaxExamSupport.NEXUS_PAX_EXAM_INVOKER_KEY;
import static org.sonatype.nexus.pax.exam.NexusPaxExamSupport.NEXUS_PAX_EXAM_TIMEOUT_DEFAULT;
import static org.sonatype.nexus.pax.exam.NexusPaxExamSupport.NEXUS_PAX_EXAM_TIMEOUT_KEY;

/**
 * Delayed {@link ProbeInvokerFactory} that waits for Nexus to start before invoking the testsuite.
 * 
 * @since 3.0
 */
public class DelayedProbeInvokerFactory
    implements ProbeInvokerFactory
{
  private final long examTimeout;

  private final String examInvoker;

  private final ServiceTracker<?, ProbeInvokerFactory> probeFactoryTracker;

  private final ServiceTracker<?, Injector> nexusInjectorTracker;

  private final ServiceTracker<?, RegionDigraph> regionDigraphTracker;

  public DelayedProbeInvokerFactory(final BundleContext context) {
    examTimeout = getExamTimeout(context);
    examInvoker = getExamInvoker(context);

    probeFactoryTracker = new ServiceTracker<>(context, probeInvokerFilter(), null);
    nexusInjectorTracker = new ServiceTracker<>(context, nexusInjectorFilter(), null);
    regionDigraphTracker = new ServiceTracker<>(context, RegionDigraph.class, null);

    probeFactoryTracker.open();
    nexusInjectorTracker.open();
    regionDigraphTracker.open();
  }

  public ProbeInvoker createProbeInvoker(final Object context, final String expr) {
    try {
      // wait for Nexus to start and register its Pax-Exam injector
      if (nexusInjectorTracker.waitForService(examTimeout) != null) {

        // include the generated pax-exam test-probe bundle in the root region so it can see our service
        regionDigraphTracker.getService().getRegion("root").addBundle(((BundleContext) context).getBundle());

        // use the real Pax-Exam invoker factory to supply the testsuite invoker
        return probeFactoryTracker.getService().createProbeInvoker(context, expr);
      }
      throw new TestContainerException("Nexus failed to start after " + examTimeout + "ms");
    }
    catch (final InterruptedException|BundleException e) {
      throw new TestContainerException("Nexus failed to start after " + examTimeout + "ms", e);
    }
  }

  /**
   * @return Timeout to apply when waiting for Nexus to start
   */
  private static int getExamTimeout(final BundleContext context) {
    final String value = context.getProperty(NEXUS_PAX_EXAM_TIMEOUT_KEY);
    if (value == null || value.trim().length() == 0) {
      return NEXUS_PAX_EXAM_TIMEOUT_DEFAULT;
    }
    try {
      return Integer.parseInt(value);
    }
    catch (final NumberFormatException e) {
      return NEXUS_PAX_EXAM_TIMEOUT_DEFAULT;
    }
  }

  /**
   * @return Name of the real Pax-Exam {@link ProbeInvokerFactory}
   */
  private static String getExamInvoker(final BundleContext context) {
    final String value = context.getProperty(NEXUS_PAX_EXAM_INVOKER_KEY);
    if (value == null || value.trim().length() == 0) {
      return NEXUS_PAX_EXAM_INVOKER_DEFAULT;
    }
    return value;
  }

  /**
   * @return LDAP filter that matches the real Pax-Exam {@link ProbeInvokerFactory}
   */
  private Filter probeInvokerFilter() {
    return filter("(&(objectClass=%s)(driver=%s))", ProbeInvokerFactory.class.getName(), examInvoker);
  }

  /**
   * @return LDAP filter that matches the nexus-specific Pax-Exam {@link Injector}
   */
  private Filter nexusInjectorFilter() {
    return filter("(&(objectClass=%s)(name=nexus))", Injector.class.getName());
  }

  /**
   * @return LDAP filter created by formatting the given arguments
   */
  private static Filter filter(final String format, final Object... args) {
    try {
      return FrameworkUtil.createFilter(String.format(format, args));
    }
    catch (final InvalidSyntaxException e) {
      throw new IllegalArgumentException(e);
    }
  }
}

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

import org.ops4j.pax.exam.ProbeInvoker;
import org.ops4j.pax.exam.ProbeInvokerFactory;
import org.ops4j.pax.exam.TestContainerException;
import org.ops4j.pax.exam.util.Injector;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.util.tracker.ServiceTracker;

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

  private final ServiceTracker<?, ProbeInvokerFactory> probeFactoryTracker;

  private final ServiceTracker<?, Injector> examInjectorTracker;

  public DelayedProbeInvokerFactory(final BundleContext context) {
    examTimeout = getExamTimeout(context);

    probeFactoryTracker = new ServiceTracker<>(context, junitInvokerFilter(), null);
    examInjectorTracker = new ServiceTracker<>(context, nexusInjectorFilter(), null);

    probeFactoryTracker.open();
    examInjectorTracker.open();
  }

  public ProbeInvoker createProbeInvoker(final Object context, final String expr) {
    try {
      // wait for Nexus to start and register its Pax-Exam injector
      if (examInjectorTracker.waitForService(examTimeout) != null) {

        // use the original Pax-Exam JUnit factory to supply the testsuite invoker
        return probeFactoryTracker.getService().createProbeInvoker(context, expr);
      }
      throw new TestContainerException("Nexus failed to start after " + examTimeout + "ms");
    }
    catch (final InterruptedException e) {
      throw new TestContainerException("Nexus failed to start after " + examTimeout + "ms", e);
    }
  }

  /**
   * @return Timeout to apply when waiting for Nexus to start
   */
  private static int getExamTimeout(final BundleContext context) {
    try {
      return Integer.parseInt(context.getProperty(NEXUS_PAX_EXAM_TIMEOUT_KEY));
    }
    catch (final Exception e) {
      return NEXUS_PAX_EXAM_TIMEOUT_DEFAULT;
    }
  }

  /**
   * @return Filter that matches the original JUnit Pax-Exam {@link ProbeInvokerFactory}
   */
  private static Filter junitInvokerFilter() {
    try {
      return FrameworkUtil.createFilter("(&(objectClass=" + ProbeInvokerFactory.class.getName() + ")(driver=junit))");
    }
    catch (final InvalidSyntaxException e) {
      throw new IllegalArgumentException(e);
    }
  }

  /**
   * @return Filter that matches the nexus-specific Pax-Exam {@link Injector}
   */
  private static Filter nexusInjectorFilter() {
    try {
      return FrameworkUtil.createFilter("(&(objectClass=" + Injector.class.getName() + ")(name=nexus))");
    }
    catch (final InvalidSyntaxException e) {
      throw new IllegalArgumentException(e);
    }
  }
}

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
package org.sonatype.nexus.pax.exam;

import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.manipulation.Filter;
import org.junit.runner.manipulation.Filterable;
import org.junit.runner.manipulation.NoTestsRemainException;
import org.junit.runner.manipulation.Sortable;
import org.junit.runner.manipulation.Sorter;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;

import static org.sonatype.nexus.pax.exam.NexusPaxExamTestIndexRule.captureLogsOnFailure;

/**
 * Safe {@link Runner} that records a failure if an exception escapes from the runner specified by {@link SafeRunWith}.
 *
 * This can happen with Pax-Exam runners if the probe installation fails or the container dies before it's shutdown.
 *
 * This runner also sends an early notification that the suite of tests in the test class is starting, to workaround
 * a limitation in Maven surefire/failsafe where it only captures output after the first notification. Since Pax-Exam
 * only fires off test notifications once the container is up, much of the early output was left in "null-output.txt"
 * which was then overwritten by subsequent test classes.
 *
 * @since 3.14
 */
public class SafeRunner
    extends Runner
    implements Filterable, Sortable
{
  private final Description suiteDescription;

  private final Runner delegate;

  public SafeRunner(final Class<?> testClass) {
    this.suiteDescription = Description.createSuiteDescription(testClass);

    SafeRunWith safeRunWith = testClass.getAnnotation(SafeRunWith.class);
    if (safeRunWith == null) {
      throw new IllegalArgumentException(testClass + " should also be annotated with @SafeRunWith");
    }

    Class<? extends Runner> runnerClass = safeRunWith.value();
    try {
      this.delegate = runnerClass.getConstructor(Class.class).newInstance(testClass);
    }
    catch (Throwable e) { // NOSONAR
      throw new IllegalArgumentException("Problem constructing " + runnerClass + " runner for " + testClass, e);
    }
  }

  @Override
  public Description getDescription() {
    return delegate.getDescription();
  }

  @Override
  public void run(final RunNotifier notifier) {
    notifier.fireTestStarted(suiteDescription);
    try {
      delegate.run(notifier);
    }
    catch (Throwable e) { // NOSONAR
      Failure failure = new Failure(suiteDescription, e);
      notifier.fireTestFailure(failure);
      captureLogsOnFailure(failure);
    }
    finally {
      notifier.fireTestFinished(suiteDescription);
    }
  }

  @Override
  public void filter(final Filter filter) throws NoTestsRemainException {
    if (delegate instanceof Filterable) {
      ((Filterable) delegate).filter(filter);
    }
  }

  @Override
  public void sort(final Sorter sorter) {
    if (delegate instanceof Sortable) {
      ((Sortable) delegate).sort(sorter);
    }
  }
}

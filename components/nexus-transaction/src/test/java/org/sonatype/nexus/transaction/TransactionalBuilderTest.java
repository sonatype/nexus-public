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
package org.sonatype.nexus.transaction;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;

import org.sonatype.goodies.testsupport.TestSupport;

import com.google.common.base.Throwables;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContainingInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;

/**
 * Test {@link TransactionalBuilder} behaviour.
 */
public class TransactionalBuilderTest
    extends TestSupport
{
  private interface SampleAnnotations
  {
    @Transactional
    void defaultValues();

    @Transactional(commitOn = IOException.class)
    void customCommitOn();

    @Transactional(retryOn = { InvocationTargetException.class, IllegalStateException.class })
    void customRetryOn();

    @Transactional(swallow = { RuntimeException.class, MalformedURLException.class })
    void customSwallow();

    @Transactional(commitOn = IllegalStateException.class, retryOn = RuntimeException.class, swallow = IOException.class)
    void customValues();
  }

  @Test
  public void testBuilderChecksArguments() {

    try {
      new TransactionalBuilder(null).commitOn(IOException.class, null, RuntimeException.class);
      fail("Expected NullPointerException");
    }
    catch (NullPointerException e) {
      // expected
    }

    try {
      new TransactionalBuilder(null).commitOn((Class<? extends Exception>[]) null);
      fail("Expected NullPointerException");
    }
    catch (NullPointerException e) {
      // expected
    }

    try {
      new TransactionalBuilder(null).retryOn(IOException.class, null, RuntimeException.class);
      fail("Expected NullPointerException");
    }
    catch (NullPointerException e) {
      // expected
    }

    try {
      new TransactionalBuilder(null).retryOn((Class<? extends Exception>[]) null);
      fail("Expected NullPointerException");
    }
    catch (NullPointerException e) {
      // expected
    }

    try {
      new TransactionalBuilder(null).swallow(IOException.class, null, RuntimeException.class);
      fail("Expected NullPointerException");
    }
    catch (NullPointerException e) {
      // expected
    }

    try {
      new TransactionalBuilder(null).swallow((Class<? extends Exception>[]) null);
      fail("Expected NullPointerException");
    }
    catch (NullPointerException e) {
      // expected
    }
  }

  @Test
  public void testBuilderAnnotionBehaviour() {

    assertBehaviour(
        TransactionalBuilder.DEFAULT_SPEC,
        sample("defaultValues"));

    assertBehaviour(
        new TransactionalBuilder(null).build(),
        sample("defaultValues"));

    assertBehaviour(
        new TransactionalBuilder(null).commitOn(IOException.class).build(),
        sample("customCommitOn"));

    assertBehaviour(
        new TransactionalBuilder(null).retryOn(InvocationTargetException.class, IllegalStateException.class).build(),
        sample("customRetryOn"));

    assertBehaviour(
        new TransactionalBuilder(null).swallow(RuntimeException.class, MalformedURLException.class).build(),
        sample("customSwallow"));

    assertBehaviour(
        new TransactionalBuilder(null).commitOn(IllegalStateException.class).retryOn(RuntimeException.class).swallow(IOException.class).build(),
        sample("customValues"));
  }

  private static void assertBehaviour(final Annotation lhs, final Annotation rhs) {
    assertThat(lhs.equals(null), is(rhs.equals(null)));
    assertThat(lhs.equals(TransactionalBuilder.DEFAULT_SPEC), is(rhs.equals(TransactionalBuilder.DEFAULT_SPEC)));
    assertThat(lhs.equals(rhs), is(rhs.equals(lhs)));
    assertThat(lhs.hashCode(), is(rhs.hashCode()));
    // cope with random order of properties in the JDK's default annotation toString() implementation
    assertThat(lhs.toString().split("[(), ]"), arrayContainingInAnyOrder(rhs.toString().split("[(), ]")));
    assertThat(lhs.annotationType(), is(equalTo(rhs.annotationType())));
  }

  private static Transactional sample(final String name) {
    try {
      return SampleAnnotations.class.getMethod(name).getAnnotation(Transactional.class);
    }
    catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }
}

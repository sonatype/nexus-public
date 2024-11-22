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
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;

import javax.inject.Named;

import org.sonatype.goodies.testsupport.TestSupport;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;

/**
 * Test {@link Operations} builder methods.
 */
public class OperationsBuilderTest
    extends TestSupport
{
  private interface SampleAnnotations
  {
    @Transactional
    void defaultValues();

    @Transactional(commitOn = IOException.class)
    void customCommitOn();

    @Transactional(retryOn = {InvocationTargetException.class, IllegalStateException.class})
    void customRetryOn();

    @Transactional(swallow = {RuntimeException.class, MalformedURLException.class})
    void customSwallow();

    @Transactional(commitOn = IllegalStateException.class, retryOn = RuntimeException.class,
        swallow = IOException.class)
    void customValues();

    @Retention(RetentionPolicy.RUNTIME)
    @Transactional(commitOn = IllegalStateException.class, retryOn = RuntimeException.class,
        swallow = IOException.class)
    @interface Stereotype
    {
      // meta-annotated with @Transactional
    }
  }

  @Test
  public void testBuilderChecksArguments() {

    try {
      new Operations().commitOn(IOException.class, null, RuntimeException.class);
      fail("Expected NullPointerException");
    }
    catch (NullPointerException e) {
      // expected
    }

    try {
      new Operations().commitOn((Class<? extends Exception>[]) null);
      fail("Expected NullPointerException");
    }
    catch (NullPointerException e) {
      // expected
    }

    try {
      new Operations().retryOn(IOException.class, null, RuntimeException.class);
      fail("Expected NullPointerException");
    }
    catch (NullPointerException e) {
      // expected
    }

    try {
      new Operations().retryOn((Class<? extends Exception>[]) null);
      fail("Expected NullPointerException");
    }
    catch (NullPointerException e) {
      // expected
    }

    try {
      new Operations().swallow(IOException.class, null, RuntimeException.class);
      fail("Expected NullPointerException");
    }
    catch (NullPointerException e) {
      // expected
    }

    try {
      new Operations().swallow((Class<? extends Exception>[]) null);
      fail("Expected NullPointerException");
    }
    catch (NullPointerException e) {
      // expected
    }

    try {
      new Operations().stereotype(null);
      fail("Expected NullPointerException");
    }
    catch (NullPointerException e) {
      // expected
    }

    try {
      new Operations().stereotype(Named.class);
      fail("Expected IllegalArgumentException");
    }
    catch (IllegalArgumentException e) {
      // expected
    }
  }

  @Test
  public void testBuilderAnnotionBehaviour() {

    assertBehaviour(
        Operations.DEFAULT_SPEC,
        sample("defaultValues"));

    assertBehaviour(
        new Operations().spec,
        sample("defaultValues"));

    assertBehaviour(
        new Operations().commitOn(IOException.class).spec,
        sample("customCommitOn"));

    assertBehaviour(
        new Operations().retryOn(InvocationTargetException.class, IllegalStateException.class).spec,
        sample("customRetryOn"));

    assertBehaviour(
        new Operations().swallow(RuntimeException.class, MalformedURLException.class).spec,
        sample("customSwallow"));

    assertBehaviour(
        new Operations().commitOn(IllegalStateException.class)
            .retryOn(RuntimeException.class)
            .swallow(IOException.class).spec,
        sample("customValues"));

    assertBehaviour(
        new Operations().stereotype(SampleAnnotations.Stereotype.class).spec,
        sample("customValues"));
  }

  private static void assertBehaviour(final Transactional lhs, final Transactional rhs) {
    assertThat(lhs.equals(null), is(rhs.equals(null)));
    assertThat(lhs.equals(Operations.DEFAULT_SPEC), is(rhs.equals(Operations.DEFAULT_SPEC)));
    assertThat(lhs.equals(rhs), is(rhs.equals(lhs)));
    assertThat(lhs.hashCode(), is(rhs.hashCode()));

    assertThat(lhs.commitOn(), is(rhs.commitOn()));
    assertThat(lhs.isolation(), is(rhs.isolation()));
    assertThat(lhs.reason(), is(rhs.reason()));
    assertThat(lhs.retryOn(), is(rhs.retryOn()));
    assertThat(lhs.swallow(), is(rhs.swallow()));

    assertThat(lhs.annotationType(), is(equalTo(rhs.annotationType())));
  }

  private static Transactional sample(final String name) {
    try {
      return SampleAnnotations.class.getMethod(name).getAnnotation(Transactional.class);
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}

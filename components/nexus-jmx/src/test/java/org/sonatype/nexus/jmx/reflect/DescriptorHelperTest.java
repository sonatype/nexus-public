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
package org.sonatype.nexus.jmx.reflect;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;
import javax.management.Descriptor;

import org.sonatype.goodies.testsupport.TestSupport;

import org.hamcrest.CustomTypeSafeMatcher;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;

public class DescriptorHelperTest
    extends TestSupport
{

  @TestAuthor("jason")
  public class TestBean
  {

    @TestComments("foo bar baz")
    public void foo() {
      // empty
    }

    @TestInvalidAnnotationValue(@TestComments("foo"))
    public void invalid1() {
      // empty
    }
  }

  @Test
  public void findsAnnotations() {
    TestBean bean = new TestBean();
    List<Annotation> annotations = DescriptorHelper.findAllAnnotations(bean.getClass().getAnnotations());

    // custom annotation should be found
    assertThat(annotations, hasItem(new AnnotationMatcher(TestAuthor.class.getName())));
  }

  @Test
  public void buildDescriptorFromType() {
    TestBean bean = new TestBean();
    Descriptor descriptor = DescriptorHelper.build(bean.getClass());

    // descriptor should have author
    assertThat(descriptor.getFields().length, equalTo(1));
    assertThat(descriptor.getFieldValue("author"), equalTo("jason"));
  }

  @Test
  public void buildDescriptorFromMethod() throws NoSuchMethodException {
    Method method = TestBean.class.getMethod("foo");
    Descriptor descriptor = DescriptorHelper.build(method);

    // descriptor should have comments
    assertThat(descriptor.getFields().length, equalTo(1));
    assertThat(descriptor.getFieldValue("comments"), equalTo("foo bar baz"));
  }

  @Test
  public void buildDescriptorFailsDueToInvalid() throws NoSuchMethodException {
    Method method = TestBean.class.getMethod("invalid1");
    assertThrows(DescriptorHelper.InvalidDescriptorKeyException.class, () -> DescriptorHelper.build(method));
  }

  private static class AnnotationMatcher
      extends CustomTypeSafeMatcher<Annotation>
  {
    private final String annotationName;

    public AnnotationMatcher(final String annotationName) {
      super("Matches: " + annotationName);
      this.annotationName = annotationName;
    }

    @Override
    protected boolean matchesSafely(final Annotation annotation) {
      return annotation.annotationType().getName().equals(annotationName);
    }
  }
}

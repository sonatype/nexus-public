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
package org.sonatype.nexus.supportzip;

import java.io.InputStream;

import org.sonatype.nexus.supportzip.SupportBundle.ContentSource.Priority;
import org.sonatype.nexus.supportzip.SupportBundle.ContentSource.Type;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;

/**
 * UT for {@link ContentSourceSupport}.
 */
public class ContentSourceSupportTest
{
  @Test
  public void testFullConstructorRetainsTypePathAndPriority() {
    ContentSourceSupport source = new TestContentSource(Type.CONFIG, "some/path", Priority.HIGH);

    assertThat(source.getType(), is(Type.CONFIG));
    assertThat(source.getPath(), is("some/path"));
    assertThat(source.getPriority(), is(Priority.HIGH));
  }

  @Test
  public void testShortConstructorDefaultsToDefaultPriority() {
    ContentSourceSupport source = new TestContentSource(Type.LOG, "another/path");

    assertThat(source.getType(), is(Type.LOG));
    assertThat(source.getPath(), is("another/path"));
    assertThat(source.getPriority(), is(Priority.DEFAULT));
  }

  @Test
  public void testGetType() {
    assertThat(new TestContentSource(Type.SECURITY, "p").getType(), is(Type.SECURITY));
  }

  @Test
  public void testGetPath() {
    assertThat(new TestContentSource(Type.CONFIG, "foo/bar").getPath(), is("foo/bar"));
  }

  @Test
  public void testGetPriority() {
    assertThat(new TestContentSource(Type.CONFIG, "p", Priority.OPTIONAL).getPriority(), is(Priority.OPTIONAL));
  }

  @Test
  public void testSetPriorityUpdatesPriority() {
    ContentSourceSupport source = new TestContentSource(Type.CONFIG, "p");
    assertThat(source.getPriority(), is(Priority.DEFAULT));

    source.setPriority(Priority.REQUIRED);

    assertThat(source.getPriority(), is(Priority.REQUIRED));
  }

  @Test
  public void testPathBackslashesAreNormalizedToForwardSlashes() {
    ContentSourceSupport source = new TestContentSource(Type.CONFIG, "a\\b\\c");

    assertThat(source.getPath(), is("a/b/c"));
  }

  @Test
  public void testPathWithoutBackslashesIsUnchanged() {
    ContentSourceSupport source = new TestContentSource(Type.CONFIG, "a/b/c");

    assertThat(source.getPath(), is("a/b/c"));
  }

  @Test
  public void testCompareToOrdersByPriorityOrder() {
    ContentSourceSupport required = new TestContentSource(Type.CONFIG, "p", Priority.REQUIRED);
    ContentSourceSupport defaultPriority = new TestContentSource(Type.CONFIG, "p", Priority.DEFAULT);
    ContentSourceSupport optional = new TestContentSource(Type.CONFIG, "p", Priority.OPTIONAL);

    // REQUIRED(0) < DEFAULT(50) < OPTIONAL(999)
    assertThat(required.compareTo(defaultPriority), is(lessThan(0)));
    assertThat(defaultPriority.compareTo(optional), is(lessThan(0)));
    assertThat(optional.compareTo(required), is(greaterThan(0)));
  }

  @Test
  public void testCompareToReturnsZeroForEqualPriority() {
    ContentSourceSupport first = new TestContentSource(Type.CONFIG, "p", Priority.DEFAULT);
    ContentSourceSupport second = new TestContentSource(Type.LOG, "other", Priority.DEFAULT);

    assertThat(first.compareTo(second), is(0));
  }

  @Test
  public void testToStringContainsTypePathAndPriority() {
    ContentSourceSupport source = new TestContentSource(Type.CONFIG, "some/path", Priority.HIGH);

    String result = source.toString();

    assertThat(result, containsString("type=" + Type.CONFIG));
    assertThat(result, containsString("path='some/path'"));
    assertThat(result, containsString("priority=" + Priority.HIGH));
    assertThat(result, containsString(TestContentSource.class.getSimpleName()));
    assertThat(result, is("TestContentSource{type=CONFIG, path='some/path', priority=HIGH}"));
  }

  @Test(expected = NullPointerException.class)
  public void testConstructorRejectsNullType() {
    new TestContentSource(null, "some/path", Priority.DEFAULT);
  }

  @Test(expected = NullPointerException.class)
  public void testConstructorRejectsNullPath() {
    new TestContentSource(Type.CONFIG, null, Priority.DEFAULT);
  }

  @Test(expected = NullPointerException.class)
  public void testConstructorRejectsNullPriority() {
    new TestContentSource(Type.CONFIG, "some/path", null);
  }

  @Test(expected = NullPointerException.class)
  public void testSetPriorityRejectsNull() {
    new TestContentSource(Type.CONFIG, "some/path").setPriority(null);
  }

  @Test(expected = NullPointerException.class)
  public void testShortConstructorRejectsNullType() {
    new TestContentSource(null, "some/path");
  }

  @Test(expected = NullPointerException.class)
  public void testShortConstructorRejectsNullPath() {
    new TestContentSource(Type.CONFIG, null);
  }

  @Test
  public void testEmptyPathIsNormalizedToEmpty() {
    ContentSourceSupport source = new TestContentSource(Type.CONFIG, "");

    assertThat(source.getPath(), is(""));
  }

  @Test
  public void testPathWithLeadingTrailingAndRepeatedBackslashesNormalized() {
    ContentSourceSupport source = new TestContentSource(Type.CONFIG, "\\a\\\\b\\");

    // normalization is a plain replace('\\', '/'): each backslash is individually replaced with '/',
    // so doubled backslashes become doubled slashes (slashes are NOT collapsed)
    assertThat(source.getPath(), is("/a//b/"));
  }

  @Test
  public void testToStringReflectsNormalizedPath() {
    ContentSourceSupport source = new TestContentSource(Type.CONFIG, "a\\b", Priority.DEFAULT);

    assertThat(source.toString(), containsString("path='a/b'"));
  }

  @Test
  public void testCompareToIsReflexive() {
    ContentSourceSupport source = new TestContentSource(Type.CONFIG, "p", Priority.DEFAULT);

    assertThat(source.compareTo(source), is(0));
  }

  @Test
  public void testCompareToOrdersAcrossAllPriorities() {
    ContentSourceSupport required = new TestContentSource(Type.CONFIG, "p", Priority.REQUIRED);
    ContentSourceSupport high = new TestContentSource(Type.CONFIG, "p", Priority.HIGH);
    ContentSourceSupport defaultPriority = new TestContentSource(Type.CONFIG, "p", Priority.DEFAULT);
    ContentSourceSupport low = new TestContentSource(Type.CONFIG, "p", Priority.LOW);
    ContentSourceSupport optional = new TestContentSource(Type.CONFIG, "p", Priority.OPTIONAL);

    // REQUIRED(0) < HIGH(10) < DEFAULT(50) < LOW(100) < OPTIONAL(999)
    assertThat(required.compareTo(high), is(lessThan(0)));
    assertThat(high.compareTo(defaultPriority), is(lessThan(0)));
    assertThat(defaultPriority.compareTo(low), is(lessThan(0)));
    assertThat(low.compareTo(optional), is(lessThan(0)));
    assertThat(optional.compareTo(required), is(greaterThan(0)));
  }

  /**
   * Minimal concrete {@link ContentSourceSupport} used to exercise the abstract base class.
   */
  private static class TestContentSource
      extends ContentSourceSupport
  {
    TestContentSource(final Type type, final String path, final Priority priority) {
      super(type, path, priority);
    }

    TestContentSource(final Type type, final String path) {
      super(type, path);
    }

    @Override
    public long getSize() {
      return 0;
    }

    @Override
    public InputStream getContent() {
      return null;
    }

    @Override
    public void prepare() {
      // no-op
    }

    @Override
    public void cleanup() {
      // no-op
    }
  }
}

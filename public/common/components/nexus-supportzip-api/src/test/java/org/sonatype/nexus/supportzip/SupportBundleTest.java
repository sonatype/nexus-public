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

import java.util.List;

import org.junit.Test;
import org.sonatype.nexus.supportzip.SupportBundle.ContentSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link SupportBundle}
 */
public class SupportBundleTest
{
  private final SupportBundle underTest = new SupportBundle();

  @Test
  public void testGetSourcesInitiallyEmpty() {
    List<ContentSource> sources = underTest.getSources();
    assertThat(sources, is(empty()));
  }

  @Test
  public void testGetSourcesReturnsSameBackingList() {
    assertThat(underTest.getSources(), sameInstance(underTest.getSources()));
  }

  @Test
  public void testAddAppendsSource() {
    ContentSource source = mock(ContentSource.class);

    underTest.add(source);

    assertThat(underTest.getSources(), hasSize(1));
    assertThat(underTest.getSources(), contains(source));
  }

  @Test
  public void testAddIncrementsSize() {
    ContentSource first = mock(ContentSource.class);
    ContentSource second = mock(ContentSource.class);

    underTest.add(first);
    assertThat(underTest.getSources(), hasSize(1));

    underTest.add(second);
    assertThat(underTest.getSources(), hasSize(2));
    assertThat(underTest.getSources(), contains(first, second));
  }

  @Test
  public void testAddAllowsDuplicateSources() {
    ContentSource source = mock(ContentSource.class);

    underTest.add(source);
    underTest.add(source);

    assertThat(underTest.getSources(), hasSize(2));
    assertThat(underTest.getSources(), contains(source, source));
  }

  @Test
  public void testAddNullThrowsNullPointerException() {
    assertThrows(NullPointerException.class, () -> underTest.add(null));

    assertThat(underTest.getSources(), is(empty()));
  }

  @Test
  public void testLeftShiftDelegatesToAdd() {
    ContentSource source = mock(ContentSource.class);

    underTest.leftShift(source);

    assertThat(underTest.getSources(), hasSize(1));
    assertThat(underTest.getSources(), contains(source));
  }

  @Test
  public void testLeftShiftNullThrowsNullPointerException() {
    assertThrows(NullPointerException.class, () -> underTest.leftShift(null));

    assertThat(underTest.getSources(), is(empty()));
  }

  @Test
  public void testLeftShiftPreservesInsertionOrder() {
    ContentSource first = mock(ContentSource.class);
    ContentSource second = mock(ContentSource.class);

    underTest.leftShift(first);
    underTest.leftShift(second);

    assertThat(underTest.getSources(), contains(first, second));
  }

  @Test
  public void testAddAndLeftShiftPreserveCombinedInsertionOrder() {
    ContentSource first = mock(ContentSource.class);
    ContentSource second = mock(ContentSource.class);
    ContentSource third = mock(ContentSource.class);

    underTest.add(first);
    underTest.leftShift(second);
    underTest.add(third);

    assertThat(underTest.getSources(), hasSize(3));
    assertThat(underTest.getSources(), contains(first, second, third));
  }

  @Test
  public void testGetSourcesReflectsAddedSource() {
    ContentSource source = mock(ContentSource.class);
    underTest.getSources().add(source);

    assertSame(source, underTest.getSources().get(0));
  }

  @Test
  public void testGetSourcesIsLiveViewOfBackingList() {
    List<ContentSource> sources = underTest.getSources();
    ContentSource source = mock(ContentSource.class);

    underTest.add(source);

    assertThat(sources, contains(source));
    assertSame(source, sources.get(0));
  }
}

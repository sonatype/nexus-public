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
package org.sonatype.nexus.common.app;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

public class VersionComparatorTest
{
  @Test
  public void testStringVersionComparator() {
    List<String> sorted = Stream.of("1.1", "1.2", "1.0")
        .sorted(VersionComparator.INSTANCE)
        .collect(Collectors.toList());

    assertThat(sorted.get(0), equalTo("1.0"));
    assertThat(sorted.get(1), equalTo("1.1"));
    assertThat(sorted.get(2), equalTo("1.2"));
  }

  @Test
  public void testStringVersionComparator_Snapshot() {
    List<String> sorted = Stream
        .of("1.1-20170919.212404-2", "1.1-20170919.212405-3", "1.1-20170919.212403-1")
        .sorted(VersionComparator.INSTANCE)
        .collect(Collectors.toList());
    assertThat(sorted.get(0), equalTo("1.1-20170919.212403-1"));
    assertThat(sorted.get(1), equalTo("1.1-20170919.212404-2"));
    assertThat(sorted.get(2), equalTo("1.1-20170919.212405-3"));
  }

  @Test
  public void testStringVersionComparator_isTransitive() {
    assertThat(VersionComparator.INSTANCE.compare("1.0", "1.1"), lessThan(0));
    assertThat(VersionComparator.INSTANCE.compare("1.1", "1.2"), lessThan(0));
    assertThat(VersionComparator.INSTANCE.compare("1.0", "1.2"), lessThan(0));

    assertThat(VersionComparator.INSTANCE.compare("1.1", "1.0"), greaterThan(0));
    assertThat(VersionComparator.INSTANCE.compare("1.2", "1.1"), greaterThan(0));
    assertThat(VersionComparator.INSTANCE.compare("1.2", "1.0"), greaterThan(0));
  }

  @Test
  public void testStringVersionComparator_mixedVersionAndNonVersionStringAreSortedTogether() {
    List<String> versions = Arrays.asList("2.0.0", "", "1.0-foo", "1.0-beta", "1.2", "1.1-SNAPSHOT", "foo", "2foo");
    versions.sort(VersionComparator.INSTANCE);
    assertThat(versions, is(Arrays.asList("", "2foo", "foo", "1.0-beta", "1.0-foo", "1.1-SNAPSHOT", "1.2", "2.0.0")));
  }

  @Test
  public void testStringVersionComparator_isNotThrowingExceptionOnMixedParsableAndNotParsableInput() {
    List<String> list = Arrays
        .asList("Z9BtVtVO", "11.0", "93eAzyqO", "saB5kQ64", "9.0", "2.0", "Mevi29bx", "10.0", "nPqYe0qc", "14.0",
            "AsQD7LvI", "7.0", "3.0", "13.0", "W1UmeoHQ", "8.0", "0eyq3xGh", "ADdMasr4", "KsepxKG4", "15.0", "LjEGUAU0",
            "Txu1bI2F", "16.0", "5.0", "1.0", "QmcAWDLQ", "6.0", "4.0", "apUQ6KHw", "ayFi1K6t", "CqJzJm5Z", "ckhC6xIH",
            "12.0");

    try {
      list.sort(VersionComparator.INSTANCE);
    }
    catch (IllegalArgumentException e) {
      fail("An exception was thrown when sorting a list: " + e.getMessage());
    }
  }

}

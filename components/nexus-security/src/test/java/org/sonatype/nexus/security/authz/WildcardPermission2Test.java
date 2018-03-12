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
package org.sonatype.nexus.security.authz;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.sonatype.goodies.testsupport.TestSupport;

import com.google.common.collect.ImmutableSet;
import org.junit.Test;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class WildcardPermission2Test
    extends TestSupport
{
  private static final List<String> SUB_PARTS = asList("subPart1", "subPart2");

  private static final List<String> MULTIPLE_ACTIONS = asList("actionNumber1", "actionNumber2");

  private static final List<String> SINGLE_ACTION = asList("singleAction");

  private static final boolean CASE_SENSITIVE = true;

  @Test
  public void testSetPartsNotCaseSensitive() {
    List<Set<String>> expectedParts = new ArrayList<>();
    expectedParts.add(ImmutableSet.of("subpart1"));
    expectedParts.add(ImmutableSet.of("subpart2"));
    expectedParts.add(ImmutableSet.of("actionnumber1", "actionnumber2"));

    verifyParts(SUB_PARTS, MULTIPLE_ACTIONS, !CASE_SENSITIVE, expectedParts);
  }

  @Test
  public void testSetPartsCaseSensitive() {
    List<Set<String>> expectedParts = new ArrayList<>();
    expectedParts.add(ImmutableSet.of("subPart1"));
    expectedParts.add(ImmutableSet.of("subPart2"));
    expectedParts.add(ImmutableSet.of("actionNumber1", "actionNumber2"));

    verifyParts(SUB_PARTS, MULTIPLE_ACTIONS, CASE_SENSITIVE, expectedParts);
  }

  @Test
  public void setPartsSingleAction() {
    List<Set<String>> expectedParts = new ArrayList<>();
    expectedParts.add(ImmutableSet.of("subpart1"));
    expectedParts.add(ImmutableSet.of("subpart2"));
    expectedParts.add(ImmutableSet.of("singleaction"));

    verifyParts(SUB_PARTS, SINGLE_ACTION, !CASE_SENSITIVE, expectedParts);
  }

  private static void verifyParts(final List<String> subParts,
                                  final List<String> actions,
                                  final boolean caseSensitive,
                                  final List<Set<String>> expectedParts)
  {
    WildcardPermission2 underTest = new WildcardPermission2();
    underTest.setParts(subParts, actions, caseSensitive);
    assertThat(underTest.getParts(), is(expectedParts));
  }
}

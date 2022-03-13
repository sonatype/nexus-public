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
package org.sonatype.nexus.common.collect;

import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

import org.sonatype.goodies.testsupport.TestSupport;

import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.isNotNull;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class DetachingSetTest
    extends TestSupport
{
  @Mock
  private Set<String> backing;

  @Mock
  private BooleanSupplier allowDetach;

  @Mock
  private Function<String, String> detach;

  private DetachingSet<String> underTest;

  @Before
  public void setUp() {
    underTest = new DetachingSet<>(backing, allowDetach, detach);
  }

  @Test
  public void nonEscapingQueriesNeverDetach() {

    underTest.contains(null);
    underTest.containsAll(null);
    underTest.equals(null);
    underTest.hashCode();
    underTest.isEmpty();
    underTest.size();
    underTest.toString();

    InOrder inOrder = inOrder(backing);

    inOrder.verify(backing).contains(null);
    inOrder.verify(backing).containsAll(null);
    // Mockito ignores equals
    // Mockito ignores hashCode
    inOrder.verify(backing).isEmpty();
    inOrder.verify(backing).size();
    // Mockito ignores toString

    verifyNoMoreInteractions(backing);

    verifyNoInteractions(allowDetach, detach);
  }

  @Test
  public void escapingQueriesTriggerDetach() {
    when(allowDetach.getAsBoolean()).thenReturn(true);

    underTest.iterator();
    underTest.toArray();
    underTest.toArray(new String[0]);

    InOrder inOrder = inOrder(backing, allowDetach, detach);

    inOrder.verify(allowDetach).getAsBoolean();
    inOrder.verify(backing).size();
    inOrder.verify(backing).forEach(isNotNull());
    // the rest happens on the detached set

    verifyNoMoreInteractions(backing, allowDetach, detach);
  }

  @Test
  public void mutationsTriggerDetach() {
    when(allowDetach.getAsBoolean()).thenReturn(true);

    underTest.add("");
    underTest.clear();
    underTest.remove("");

    InOrder inOrder = inOrder(backing, allowDetach, detach);

    inOrder.verify(allowDetach).getAsBoolean();
    inOrder.verify(backing).size();
    inOrder.verify(backing).forEach(isNotNull());
    // the rest happens on the detached set

    verifyNoMoreInteractions(backing, allowDetach, detach);
  }

  @Test
  public void detachingCanBeDisallowed() {
    when(allowDetach.getAsBoolean()).thenReturn(false);

    underTest.add("");
    underTest.clear();
    underTest.remove("");
    underTest.iterator();
    underTest.toArray();
    underTest.toArray(new String[0]);

    InOrder inOrder = inOrder(backing, allowDetach, detach);

    inOrder.verify(allowDetach).getAsBoolean();
    inOrder.verify(backing).add("");
    inOrder.verify(allowDetach).getAsBoolean();
    inOrder.verify(backing).clear();
    inOrder.verify(allowDetach).getAsBoolean();
    inOrder.verify(backing).remove("");
    inOrder.verify(allowDetach).getAsBoolean();
    inOrder.verify(backing).iterator();
    inOrder.verify(allowDetach).getAsBoolean();
    inOrder.verify(backing).toArray();
    inOrder.verify(allowDetach).getAsBoolean();
    inOrder.verify(backing).toArray(new String[0]);

    verifyNoMoreInteractions(backing, allowDetach, detach);
  }

  @Test
  public void simpleDetach() {
    Set<String> original = ImmutableSet.of("HELLO", "THERE");

    underTest = new DetachingSet<>(original, allowDetach, detach);

    when(allowDetach.getAsBoolean()).thenReturn(true);
    when(detach.apply(isNotNull())).thenAnswer(returnsFirstArg());

    assertThat(underTest.remove("THERE"), is(true));
    assertThat(underTest.add("WORLD"), is(true));
    assertThat(underTest, containsInAnyOrder("HELLO", "WORLD"));

    // original set contents should be unchanged
    assertThat(original, containsInAnyOrder("HELLO", "THERE"));

    InOrder inOrder = inOrder(allowDetach, detach);

    inOrder.verify(allowDetach).getAsBoolean();
    inOrder.verify(detach).apply("HELLO");
    inOrder.verify(detach).apply("THERE");

    verifyNoMoreInteractions(allowDetach, detach);
  }
}

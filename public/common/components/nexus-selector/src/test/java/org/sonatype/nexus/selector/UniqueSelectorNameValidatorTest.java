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
package org.sonatype.nexus.selector;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.Silent.class)
public class UniqueSelectorNameValidatorTest
{
  @Mock
  private SelectorManager selectorManager;

  private UniqueSelectorNameValidator underTest;

  @Before
  public void setUp() {
    underTest = new UniqueSelectorNameValidator(selectorManager);
  }

  /*
   * Null value is considered valid (other constraints such as @NotEmpty handle the null/empty case)
   */
  @Test
  public void isValid_nullValue() {
    assertThat(underTest.isValid(null, null), is(true));
  }

  @Test
  public void isValid_selectorNameAlreadyExists() {
    SelectorConfiguration existing = mock(SelectorConfiguration.class);
    when(existing.getName()).thenReturn("existing-selector");
    when(selectorManager.browse()).thenReturn(Collections.singletonList(existing));

    assertThat(underTest.isValid("existing-selector", null), is(false));
  }

  @Test
  public void isValid_selectorNameDoesNotExist() {
    SelectorConfiguration existing = mock(SelectorConfiguration.class);
    when(existing.getName()).thenReturn("existing-selector");
    when(selectorManager.browse()).thenReturn(Collections.singletonList(existing));

    assertThat(underTest.isValid("new-selector", null), is(true));
  }

  @Test
  public void isValid_noSelectorsConfigured() {
    when(selectorManager.browse()).thenReturn(Collections.emptyList());

    assertThat(underTest.isValid("any-selector", null), is(true));
  }

  @Test
  public void isValid_matchesOneOfMultipleSelectors() {
    SelectorConfiguration selector1 = mock(SelectorConfiguration.class);
    when(selector1.getName()).thenReturn("selector-one");
    SelectorConfiguration selector2 = mock(SelectorConfiguration.class);
    when(selector2.getName()).thenReturn("selector-two");
    when(selectorManager.browse()).thenReturn(Arrays.asList(selector1, selector2));

    assertThat(underTest.isValid("selector-two", null), is(false));
  }
}

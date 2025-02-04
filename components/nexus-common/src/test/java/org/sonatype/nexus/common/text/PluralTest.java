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
package org.sonatype.nexus.common.text;

import org.sonatype.goodies.testsupport.TestSupport;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Tests for {@link Plural}
 */
public class PluralTest
    extends TestSupport
{
  @Test
  public void testSimplePlural() {
    assertThat(Plural.of(-1, "dog"), is("-1 dogs"));
    assertThat(Plural.of(0, "dog"), is("0 dogs"));
    assertThat(Plural.of(1, "dog"), is("1 dog"));
    assertThat(Plural.of(2, "dog"), is("2 dogs"));
  }

  @Test
  public void testComplexPlural() {
    assertThat(Plural.of(-1, "candy", "candies"), is("-1 candies"));
    assertThat(Plural.of(0, "candy", "candies"), is("0 candies"));
    assertThat(Plural.of(1, "candy", "candies"), is("1 candy"));
    assertThat(Plural.of(2, "candy", "candies"), is("2 candies"));
  }
}

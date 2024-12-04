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
package org.sonatype.nexus.repository.manager.internal;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.Facet;
import org.sonatype.nexus.repository.FacetSupport;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThrows;

public class FacetLookupTest
    extends TestSupport
{
  private FacetLookup underTest;

  @Before
  public void setUp() {
    underTest = new FacetLookup();
  }

  @Facet.Exposed
  public interface ExampleFacet
      extends Facet
  {
    // empty
  }

  public static class ExampleFacetSupport
      extends FacetSupport
      implements ExampleFacet
  {
    // empty
  }

  @Facet.Exposed
  public static class MyExampleFacet
      extends ExampleFacetSupport
  {
    // empty
  }

  public static class FacetNoExposure
      extends FacetSupport
  {
    // empty
  }

  @Test
  public void addAndGetFacet() {
    MyExampleFacet facet1 = new MyExampleFacet();
    underTest.add(facet1);

    MyExampleFacet facet2 = underTest.get(MyExampleFacet.class);
    assertThat(facet1, is(facet2));
  }

  @Test
  public void addAndGetFacetExposure() {
    MyExampleFacet facet1 = new MyExampleFacet();
    underTest.add(facet1);

    // lookup by exposed interface should return same instance
    ExampleFacet facet2 = underTest.get(ExampleFacet.class);
    assertThat(facet2, is(facet1));

    // lookup by exposed concrete should return same instance
    MyExampleFacet facet3 = underTest.get(MyExampleFacet.class);
    assertThat(facet3, is(facet1));

    // look up of non-exposed facet type returns null
    ExampleFacetSupport facet4 = underTest.get(ExampleFacetSupport.class);
    assertThat(facet4, is(nullValue()));
  }

  @Test
  public void addFacetWithNothingExposed() {
    FacetNoExposure facet1 = new FacetNoExposure();
    assertThrows(Exception.class, () -> underTest.add(facet1));
  }

  @Test
  public void addDuplicateFacetExposureDisallowed() {
    underTest.add(new MyExampleFacet());
    assertThrows(Exception.class, () -> underTest.add(new MyExampleFacet()));
  }
}

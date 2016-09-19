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
package org.sonatype.nexus.repository.manager.internal

import org.sonatype.goodies.testsupport.TestSupport
import org.sonatype.nexus.repository.Facet
import org.sonatype.nexus.repository.FacetSupport

import org.junit.Before
import org.junit.Test

import static org.junit.Assert.fail

/**
 * Tests for {@link org.sonatype.nexus.repository.manager.FacetLookup}.
 */
class FacetLookupTest
  extends TestSupport
{
  private FacetLookup underTest

  @Before
  void setUp() {
    underTest = new FacetLookup()
  }

  @Facet.Exposed
  static interface ExampleFacet
    extends Facet
  {
    // empty
  }

  static class ExampleFacetSupport
      extends FacetSupport
      implements ExampleFacet
  {
    // empty
  }

  @Facet.Exposed
  static class MyExampleFacet
    extends ExampleFacetSupport
  {
    // empty
  }

  static class FacetNoExposure
    extends FacetSupport
  {
    // empty
  }

  @Test
  void 'add and get facet'() {
    def facet1 = new MyExampleFacet()
    underTest.add(facet1)

    def facet2 = underTest.get(MyExampleFacet.class)
    assert facet1 == facet2
  }

  @Test
  void 'add and get facet exposure'() {
    def facet1 = new MyExampleFacet()
    underTest.add(facet1)

    // lookup by exposed intf should return same instance
    def facet2 = underTest.get(ExampleFacet.class)
    assert facet2 == facet1

    // lookup by exposed concrete should return same instance
    def facet3 = underTest.get(MyExampleFacet.class)
    assert facet3 == facet1

    // look up of non-exposed facet type returns null
    def facet4 = underTest.get(ExampleFacetSupport.class)
    assert facet4 == null
  }

  @Test
  void 'add facet with nothing exposed'() {
    def facet1 = new FacetNoExposure()
    try {
      underTest.add(facet1)
      fail();
    }
    catch (Exception e) {
      // expected
    }
  }

  @Test
  void 'add duplicate facet exposure disallowed'() {
    underTest.add(new MyExampleFacet())

    try {
      underTest.add(new MyExampleFacet())
      fail()
    }
    catch (Exception e) {
      // expected
    }
  }
}

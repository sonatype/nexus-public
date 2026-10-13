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
package org.sonatype.nexus.api.common.shared.security.model;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

/**
 * Tests {@link ReferenceXO}.
 */
class ReferenceXOTest
{
  @Test
  void constructorPopulatesIdAndName() {
    ReferenceXO xo = new ReferenceXO("the-id", "the-name");
    assertThat(xo.getId(), is("the-id"));
    assertThat(xo.getName(), is("the-name"));
  }

  @Test
  void settersUpdateFields() {
    ReferenceXO xo = new ReferenceXO();
    xo.setId("id2");
    xo.setName("name2");
    assertThat(xo.getId(), is("id2"));
    assertThat(xo.getName(), is("name2"));
  }

  @Test
  void equalityConsidersBothIdAndName() {
    ReferenceXO base = new ReferenceXO("id", "name");

    assertThat(new ReferenceXO("id", "name"), is(base));
    assertThat(new ReferenceXO("id", "name").hashCode(), is(base.hashCode()));

    assertThat(new ReferenceXO("id", "other-name"), is(not(base)));
    assertThat(new ReferenceXO("other-id", "name"), is(not(base)));
  }

  @Test
  void toStringIncludesIdAndName() {
    String s = new ReferenceXO("the-id", "the-name").toString();
    assertThat(s.contains("the-id"), is(true));
    assertThat(s.contains("the-name"), is(true));
  }
}

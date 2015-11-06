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
package org.sonatype.nexus.repository.storage;

import java.util.Map;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.storage.Query.Builder;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

public class QueryTest
    extends TestSupport
{
  private Builder builder;

  @Before
  public void setup() {
    builder = Query.builder();
  }

  @Test
  public void testHasWhere() {
    assertThat(builder.hasWhere(), is(equalTo(false)));

    builder.where("   "); // this will get trimmed
    assertThat(builder.hasWhere(), is(equalTo(false)));

    builder.where("placebo");
    assertThat(builder.hasWhere(), is(equalTo(true)));
  }

  @Test
  public void testAnonymousParameter() {
    builder.where("x = ").param("placebo");

    final Query query = builder.build();

    assertThat(query.getWhere(), is(equalTo("x = :p0")));
    final Map<String, Object> parameters = query.getParameters();

    assertThat(parameters.size(), is(equalTo(1)));
    assertThat((String) parameters.get("p0"), is(equalTo("placebo")));
  }

  @Test(expected = IllegalStateException.class)
  public void testEqMissingWhere() {
    builder.eq("any");
  }

  @Test
  public void testEq() {
    Query query = builder.where("x").eq("placebo").build();
    assertThat(query.getWhere(), is("x = :p0"));

    final Map<String, Object> parameters = query.getParameters();
    assertThat(parameters.size(), is(equalTo(1)));
    assertThat((String) parameters.get("p0"), is(equalTo("placebo")));
  }

  @Test(expected = IllegalStateException.class)
  public void testAndMissingWhere() {
    builder.and("any");
  }

  @Test
  public void testAnd() {
    assertThat(builder.where("x").and("y").build().getWhere(), is("x AND y"));
  }
}


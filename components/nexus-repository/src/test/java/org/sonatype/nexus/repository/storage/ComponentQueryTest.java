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

import org.sonatype.nexus.repository.storage.ComponentQuery;
import org.sonatype.nexus.repository.storage.ComponentQuery.Builder;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

public class ComponentQueryTest
{
  @Test
  public void testHasWhere() {
    final Builder b = new Builder();

    assertThat(b.hasWhere(), is(equalTo(false)));

    b.where("   "); // this will get trimmed
    assertThat(b.hasWhere(), is(equalTo(false)));

    b.where("placebo");
    assertThat(b.hasWhere(), is(equalTo(true)));
  }

  @Test
  public void testAnonymousParameter() {
    final Builder b = new Builder();

    b.where("x = ").param("placebo");

    final ComponentQuery query = b.build();

    assertThat(query.getWhere(), is(equalTo("x = :p0")));
    final Map<String, Object> parameters = query.getParameters();

    assertThat(parameters.size(), is(equalTo(1)));
    assertThat((String) parameters.get("p0"), is(equalTo("placebo")));
  }
}
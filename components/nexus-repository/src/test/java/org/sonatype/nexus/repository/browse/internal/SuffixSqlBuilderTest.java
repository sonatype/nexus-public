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
package org.sonatype.nexus.repository.browse.internal;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.browse.QueryOptions;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.sonatype.nexus.repository.browse.internal.SuffixSqlBuilder.buildSuffix;

public class SuffixSqlBuilderTest
    extends TestSupport
{
  QueryOptions queryOptions;

  @Before
  public void setup() throws Exception {
    queryOptions = new QueryOptions("filter", "name", "asc", 99, 10);
  }

  @Test(expected = NullPointerException.class)
  public void failOnNullQueryOptions() throws Exception {
    buildSuffix(null);
  }

  @Test
  public void suffix() throws Exception {
    assertThat(buildSuffix(queryOptions), is(equalTo(" SKIP 99 LIMIT 10")));
  }

  @Test
  public void testId() {
    assertThat(buildSuffix(new QueryOptions("filter", "id", "asc", 99, 10))
        , is(equalTo(" ORDER BY @rid asc SKIP 99 LIMIT 10")));
  }
}

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
package org.sonatype.nexus.repository.rest.internal.resources;

import org.sonatype.goodies.testsupport.TestSupport;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.sonatype.nexus.common.io.Hex.encode;

public class TokenEncoderTest
    extends TestSupport
{
  private static final int PAGE_SIZE = 50;

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private TokenEncoder underTest;

  @Before
  public void setUp() {
    underTest = new TokenEncoder();
  }

  @Test
  public void testContinuationToken() {
    //Start with no token provided, it should return 'from' of 0
    int from = underTest.decode(null, boolQuery());
    assertThat(from, is(0));

    //Now get a continuation token from that initial value of 0
    String continuationToken = underTest.encode(from, PAGE_SIZE, boolQuery());
    assertThat(continuationToken, notNullValue());

    //'from' derived from the new continuation should now be 10
    int from2 = underTest.decode(continuationToken, boolQuery());
    assertThat(from2, is(50));

    //new continuation token should be different
    String continuationToken2 = underTest.encode(from2, PAGE_SIZE, boolQuery());
    assertThat(continuationToken2, notNullValue());
    assertThat(continuationToken2, not(continuationToken));

    //one more check for good measure
    int from3 = underTest.decode(continuationToken2, boolQuery());
    assertThat(from3, is(100));
  }

  @Test
  public void testBadContinuationToken() {
    String badToken = encode("bad".getBytes());

    thrown.expect(hasProperty("response", hasProperty("status", is(406))));

    underTest.decode(badToken, boolQuery());
  }

  @Test
  public void testChangingQuery() {
    String token = underTest.encode(0, PAGE_SIZE, boolQuery());

    thrown.expect(hasProperty("response", hasProperty("status", is(406))));

    underTest.decode(token, matchAllQuery());
  }

}

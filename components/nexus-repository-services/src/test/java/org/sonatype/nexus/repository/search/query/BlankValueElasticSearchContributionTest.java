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
package org.sonatype.nexus.repository.search.query;

import org.sonatype.goodies.testsupport.TestSupport;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;

public class BlankValueElasticSearchContributionTest
    extends TestSupport
{
  private BlankValueElasticSearchContribution underTest;

  @Before
  public void setup() {
    underTest = new BlankValueElasticSearchContribution();
  }

  @Test
  public void testContribute() {
    BoolQueryBuilder query = QueryBuilders.boolQuery();

    underTest.contribute(query::must, "group", "");

    assertThat(query.toString(), Matchers.containsString("\"should\" : [ {"));
    assertThat(query.toString(), Matchers.containsString("\"must_not\" : {"));
    assertThat(query.toString(), Matchers.containsString("\"wildcard\" : {"));
    assertThat(query.toString(), Matchers.containsString("\"group\" : \"*\""));
    assertThat(query.toString(), Matchers.containsString("\"exists\" : {"));
    assertThat(query.toString(), Matchers.containsString("\"field\" : \"group\""));
  }
}

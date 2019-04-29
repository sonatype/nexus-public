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
package org.sonatype.nexus.repository.search;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.hamcrest.Matchers;
import org.junit.Test;

import static org.junit.Assert.assertThat;

public class DefaultSearchContributionTest
{
  private DefaultSearchContribution defaultSearchContribution = new DefaultSearchContribution();

  @Test
  public void defaultSearchContributionEscapesStartingSlash() {
    BoolQueryBuilder query = QueryBuilders.boolQuery();
    String field = "name";
    String value = "/foo";

    defaultSearchContribution.contribute(query, field, value);

    assertThat(query.toString(), Matchers.containsString("\"query\" : \"\\\\/foo\""));
  }

  @Test
  public void defaultSearchContributionEscapesContainedSlashes() {
    BoolQueryBuilder query = QueryBuilders.boolQuery();
    String field = "name";
    String value = "a/b/";

    defaultSearchContribution.contribute(query, field, value);

    assertThat(query.toString(), Matchers.containsString("\"query\" : \"a\\\\/b\\\\/\""));
  }

}

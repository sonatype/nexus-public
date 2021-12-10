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

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ElasticSearchContributionSupportTest
{
  private ElasticSearchContributionSupport
      elasticSearchContributionSupport = new ElasticSearchContributionSupport();

  @Test
  public void escapeLeavesRegularCharactersAsIs() {
    String regularCharacters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890.";
    assertThat(elasticSearchContributionSupport.escape(regularCharacters), is(regularCharacters));
  }

  @Test
  public void escapeLeavesSupportedSpecialCharactersUnescaped() {
    String supportedSpecialCharacters = "?*\"\"";
    assertThat(elasticSearchContributionSupport.escape(supportedSpecialCharacters), is(supportedSpecialCharacters));
  }

  @Test
  public void escapeEscapesAllUnsupportedSpecialCharacters() {
    assertThat(
        elasticSearchContributionSupport.escape(":[]-+!(){}^~/\\"),
        is("\\:\\[\\]\\-\\+\\!\\(\\)\\{\\}\\^\\~\\/\\\\")
    );
  }

  @Test
  public void escapeEscapesOddNumberOfDoubleQuotes() {
    assertThat(elasticSearchContributionSupport.escape("\""), is("\\\""));
    assertThat(elasticSearchContributionSupport.escape("\"a\"b\""), is("\\\"a\\\"b\\\""));
  }

  @Test
  public void escapeIgnoresEvenNumberOfDoubleQuotes() {
    assertThat(elasticSearchContributionSupport.escape("\"ab\""), is("\"ab\""));
    assertThat(elasticSearchContributionSupport.escape("\"ab\" \"ab\""), is("\"ab\" \"ab\""));
    assertThat(elasticSearchContributionSupport.escape("\"\"\"\""), is("\"\"\"\""));
  }

  @Test
  public void escapeSupportsCommonSearches() {
    assertThat(elasticSearchContributionSupport.escape("library/alpine-dev"), is("library\\/alpine\\-dev"));

    String mavenGroup = "org.sonatype.nexus";
    assertThat(elasticSearchContributionSupport.escape(mavenGroup), is(mavenGroup));

    assertThat(
        elasticSearchContributionSupport.escape("org.apache.maven maven-plugin-registry"),
        is("org.apache.maven maven\\-plugin\\-registry")
    );
  }
}

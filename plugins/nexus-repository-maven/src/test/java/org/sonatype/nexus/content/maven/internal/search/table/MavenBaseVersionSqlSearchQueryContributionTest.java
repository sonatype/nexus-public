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
package org.sonatype.nexus.content.maven.internal.search.table;

import java.util.Arrays;
import java.util.Optional;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.maven.internal.search.MavenSearchMappings;
import org.sonatype.nexus.repository.rest.sql.SearchField;
import org.sonatype.nexus.repository.search.query.SearchFilter;
import org.sonatype.nexus.repository.search.sql.SearchMappingService;
import org.sonatype.nexus.repository.search.sql.query.syntax.ExactTerm;
import org.sonatype.nexus.repository.search.sql.query.syntax.Expression;
import org.sonatype.nexus.repository.search.sql.query.syntax.Operand;
import org.sonatype.nexus.repository.search.sql.query.syntax.SqlPredicate;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertTrue;
import static org.sonatype.nexus.content.maven.internal.search.table.MavenBaseVersionSqlSearchQueryContribution.BASE_VERSION;

public class MavenBaseVersionSqlSearchQueryContributionTest
    extends TestSupport
{
  private MavenBaseVersionSqlSearchQueryContribution underTest;

  @Before
  public void setup() {
    underTest = new MavenBaseVersionSqlSearchQueryContribution();
    underTest.init(new SearchMappingService(Arrays.asList(new MavenSearchMappings())));
  }

  @Test
  public void shouldContributeCustomisedBaseVersion() {
    String baseVersion = "1.0-snapshot";

    Optional<Expression> result = underTest.createPredicate(new SearchFilter(BASE_VERSION, baseVersion));

    assertTrue(result.isPresent());

    assertThat(result.get(), is(new SqlPredicate(Operand.EQ, SearchField.FORMAT_FIELD_1, new ExactTerm("/" + baseVersion))));
  }
}

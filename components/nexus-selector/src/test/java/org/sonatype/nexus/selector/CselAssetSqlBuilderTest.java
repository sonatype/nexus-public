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
package org.sonatype.nexus.selector;

import org.sonatype.goodies.testsupport.TestSupport;

import org.apache.commons.jexl3.JexlException;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class CselAssetSqlBuilderTest
    extends TestSupport
{
  private static final String FORMAT = "maven2";

  private CselAssetSqlBuilder builder = new CselAssetSqlBuilder();

  @Test
  public void buildWhereClauseHandlesEqualsConversion() throws Exception {
    CselAssetSql whereClause = builder.buildWhereClause("format == \"maven2\"", FORMAT, "a", "");

    assertThat(whereClause.getSql(), is("format = :a0"));
    assertThat(whereClause.getSqlParameters().get("a0"), is("maven2"));
  }

  @Test
  public void buildWhereClauseHandlesRegexMatching() throws Exception {
    CselAssetSql whereClause = builder.buildWhereClause("path =~ \".*\"", FORMAT, "a", "");

    assertThat(whereClause.getSql(), is("name matches :a0"));
    assertThat(whereClause.getSqlParameters().get("a0"), is(".*"));
  }

  @Test
  public void buildWhereClauseHandlesStartsWithMatching() throws Exception {
    CselAssetSql whereClause = builder.buildWhereClause("path =^ \"assetname\"", FORMAT, "a", "");

    assertThat(whereClause.getSql(), is("name like :a0"));
    assertThat(whereClause.getSqlParameters().get("a0"), is("assetname%"));
  }

  @Test(expected = JexlException.class)
  public void buildWhereClauseHandlesStartsWithMatchingWithInvalidValue() throws Exception {
    builder.buildWhereClause("path =^ 7", FORMAT, "a", "");
  }

  @Test
  public void buildWhereClauseHandlesAndConditionalLogic() throws Exception {
    CselAssetSql whereClause = builder.buildWhereClause("format == \"a\" && path == 'b'", FORMAT, "a", "");

    assertThat(whereClause.getSql(), is("format = :a0 and name = :a1"));
    assertThat(whereClause.getSqlParameters().get("a0"), is("a"));
    assertThat(whereClause.getSqlParameters().get("a1"), is("b"));
  }

  @Test
  public void buildWhereClauseHandlesOrConditionalLogic() throws Exception {
    CselAssetSql whereClause = builder.buildWhereClause("format == \"a\" || path == 'b'", FORMAT, "a", "");

    assertThat(whereClause.getSql(), is("format = :a0 or name = :a1"));
    assertThat(whereClause.getSqlParameters().get("a0"), is("a"));
    assertThat(whereClause.getSqlParameters().get("a1"), is("b"));
  }

  @Test
  public void buildWhereClauseHandlesParenthesizedExpressions() throws Exception {
    CselAssetSql whereClause = builder.buildWhereClause("( format == \"maven2\" )", FORMAT, "a", "");

    assertThat(whereClause.getSql(), is("(format = :a0)"));
    assertThat(whereClause.getSqlParameters().get("a0"), is("maven2"));
  }

  @Test
  public void buildWhereClauseHandlesFormatSpecificAttributes() throws Exception {
    CselAssetSql whereClause = builder.buildWhereClause("coordinate.groupId == \"com.sonatype\"", FORMAT, "a", "");

    assertThat(whereClause.getSql(), is("attributes." + FORMAT + ".groupId = :a0"));
    assertThat(whereClause.getSqlParameters().get("a0"), is("com.sonatype"));
  }

  @Test
  public void buildWhereClauseHandlesFieldPrefixes() throws Exception {
    CselAssetSql whereClause = builder
        .buildWhereClause("format == \"maven2\" && path =~ \".*\" && coordinate.groupId == \"com.sonatype\"", FORMAT,
            "a", "$asset.");

    assertThat(whereClause.getSql(),
        is("$asset.format = :a0 and $asset.name matches :a1 and $asset.attributes." + FORMAT + ".groupId = :a2"));
    assertThat(whereClause.getSqlParameters().get("a0"), is("maven2"));
    assertThat(whereClause.getSqlParameters().get("a1"), is(".*"));
    assertThat(whereClause.getSqlParameters().get("a2"), is("com.sonatype"));
  }

  @Test
  public void buildWhereClauseHandlesPathLeadingSlash() throws Exception {
    CselAssetSql whereClause = builder.buildWhereClause("format == \"a\" || path == '/bar/foo'", FORMAT, "a", "");

    assertThat(whereClause.getSql(), is("format = :a0 or name = :a1"));
    assertThat(whereClause.getSqlParameters().get("a0"), is("a"));
    assertThat(whereClause.getSqlParameters().get("a1"), is("bar/foo"));

    whereClause = builder.buildWhereClause("format == \"a\" || '/bar/foo' == path", FORMAT, "a", "");

    assertThat(whereClause.getSql(), is("format = :a0 or name = :a1"));
    assertThat(whereClause.getSqlParameters().get("a0"), is("a"));
    assertThat(whereClause.getSqlParameters().get("a1"), is("bar/foo"));
  }
}

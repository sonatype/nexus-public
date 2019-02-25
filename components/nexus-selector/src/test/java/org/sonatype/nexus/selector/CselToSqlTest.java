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
import static org.sonatype.nexus.selector.CselToSql.transformCselToSql;

public class CselToSqlTest
    extends TestSupport
{
  private static final String FORMAT = "maven2";

  private JexlEngine engine = new JexlEngine();

  @Test
  public void buildWhereClauseHandlesEqualsConversion() throws Exception {
    SelectorSqlBuilder builder = toSql("format == \"maven2\"", FORMAT, "a", "");

    assertThat(builder.getQueryString(), is("format = :a0"));
    assertThat(builder.getQueryParameters().get("a0"), is("maven2"));
  }

  @Test
  public void buildWhereClauseHandlesNotEqualsConversion() throws Exception {
    SelectorSqlBuilder builder = toSql("format != \"maven2\"", FORMAT, "a", "");

    assertThat(builder.getQueryString(), is("(format is null or format <> :a0)"));
    assertThat(builder.getQueryParameters().get("a0"), is("maven2"));
  }

  @Test
  public void buildWhereClauseHandlesNotEqualsConversion_differentOrder() throws Exception {
    SelectorSqlBuilder builder = toSql("\"maven2\" != format", FORMAT, "a", "");

    assertThat(builder.getQueryString(), is("(format is null or format <> :a0)"));
    assertThat(builder.getQueryParameters().get("a0"), is("maven2"));
  }

  @Test
  public void buildWhereClauseHandlesNotEqualsConversion_properSpacing() throws Exception {
    SelectorSqlBuilder builder = toSql("path != \"something\" && format != \"maven2\" && path == \"test\"", FORMAT, "a", "");

    assertThat(builder.getQueryString(), is("(name is null or name <> :a0) and (format is null or format <> :a1) and name = :a2"));
    assertThat(builder.getQueryParameters().get("a0"), is("something"));
    assertThat(builder.getQueryParameters().get("a1"), is("maven2"));
    assertThat(builder.getQueryParameters().get("a2"), is("test"));
  }

  @Test
  public void buildWhereClauseHandlesRegexMatching() throws Exception {
    SelectorSqlBuilder builder = toSql("path =~ \".*\"", FORMAT, "a", "");

    assertThat(builder.getQueryString(), is("name matches :a0"));
    assertThat(builder.getQueryParameters().get("a0"), is(".*"));
  }

  @Test
  public void buildWhereClauseHandlesStartsWithMatching() throws Exception {
    SelectorSqlBuilder builder = toSql("path =^ \"assetname\"", FORMAT, "a", "");

    assertThat(builder.getQueryString(), is("name like :a0"));
    assertThat(builder.getQueryParameters().get("a0"), is("assetname%"));
  }

  @Test(expected = JexlException.class)
  public void buildWhereClauseHandlesStartsWithMatchingWithInvalidValue() throws Exception {
    toSql("path =^ 7", FORMAT, "a", "");
  }

  @Test
  public void buildWhereClauseHandlesAndConditionalLogic() throws Exception {
    SelectorSqlBuilder builder = toSql("format == \"a\" && path == 'b'", FORMAT, "a", "");

    assertThat(builder.getQueryString(), is("format = :a0 and name = :a1"));
    assertThat(builder.getQueryParameters().get("a0"), is("a"));
    assertThat(builder.getQueryParameters().get("a1"), is("b"));
  }

  @Test
  public void buildWhereClauseHandlesOrConditionalLogic() throws Exception {
    SelectorSqlBuilder builder = toSql("format == \"a\" || path == 'b'", FORMAT, "a", "");

    assertThat(builder.getQueryString(), is("format = :a0 or name = :a1"));
    assertThat(builder.getQueryParameters().get("a0"), is("a"));
    assertThat(builder.getQueryParameters().get("a1"), is("b"));
  }

  @Test
  public void buildWhereClauseHandlesParenthesizedExpressions() throws Exception {
    SelectorSqlBuilder builder = toSql("( format == \"maven2\" )", FORMAT, "a", "");

    assertThat(builder.getQueryString(), is("(format = :a0)"));
    assertThat(builder.getQueryParameters().get("a0"), is("maven2"));
  }

  @Test
  public void buildWhereClauseHandlesFormatSpecificAttributes() throws Exception {
    SelectorSqlBuilder builder = toSql("coordinate.groupId == \"com.sonatype\"", FORMAT, "a", "");

    assertThat(builder.getQueryString(), is("attributes." + FORMAT + ".groupId = :a0"));
    assertThat(builder.getQueryParameters().get("a0"), is("com.sonatype"));
  }

  @Test
  public void buildWhereClauseHandlesFieldPrefixes() throws Exception {
    SelectorSqlBuilder builder = toSql(
        "format == \"maven2\" && path =~ \".*\" && coordinate.groupId == \"com.sonatype\"", FORMAT, "a", "$asset.");

    assertThat(builder.getQueryString(),
        is("$asset.format = :a0 and $asset.name matches :a1 and $asset.attributes." + FORMAT + ".groupId = :a2"));
    assertThat(builder.getQueryParameters().get("a0"), is("maven2"));
    assertThat(builder.getQueryParameters().get("a1"), is(".*"));
    assertThat(builder.getQueryParameters().get("a2"), is("com.sonatype"));
  }

  @Test
  public void buildWhereClauseHandlesPathLeadingSlash() throws Exception {
    SelectorSqlBuilder builder = toSql("format == \"a\" || path == '/bar/foo'", FORMAT, "a", "");

    assertThat(builder.getQueryString(), is("format = :a0 or name = :a1"));
    assertThat(builder.getQueryParameters().get("a0"), is("a"));
    assertThat(builder.getQueryParameters().get("a1"), is("bar/foo"));

    builder = toSql("format == \"a\" || '/bar/foo' == path", FORMAT, "a", "");

    assertThat(builder.getQueryString(), is("format = :a0 or :a1 = name"));
    assertThat(builder.getQueryParameters().get("a0"), is("a"));
    assertThat(builder.getQueryParameters().get("a1"), is("bar/foo"));
  }

  private SelectorSqlBuilder toSql(final String expression,
                                   final String format,
                                   final String parameterPrefix,
                                   final String assetPrefix)
  {
    SelectorSqlBuilder builder = new SelectorSqlBuilder();
    builder.propertyAlias("format", assetPrefix + "format");
    builder.propertyAlias("path", assetPrefix + "name");
    builder.propertyPrefix(assetPrefix + "attributes." + format + ".");
    builder.parameterPrefix(parameterPrefix);
    transformCselToSql(engine.buildExpression(expression).getSyntaxTree(), builder);
    return builder;
  }
}

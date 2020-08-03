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
package org.sonatype.nexus.selector.internal;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.selector.JexlEngine;
import org.sonatype.nexus.selector.SelectorSqlBuilder;

import org.apache.commons.jexl3.parser.ASTJexlScript;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class DatastoreCselToSqlTest
    extends TestSupport
{
  private JexlEngine jexlEngine = new JexlEngine();

  private SelectorSqlBuilder builder;

  private DatastoreCselToSql underTest;

  @Before
  public void setup() {
    underTest = new DatastoreCselToSql();
  }

  @Before
  public void createSqlBuilder() {
    builder = new SelectorSqlBuilder();

    builder.propertyAlias("a", "a_alias");
    builder.propertyAlias("b", "b_alias");

    builder.propertyPrefix("prop.");
    builder.parameterPrefix(":");
    builder.parameterNamePrefix("param_");
  }

  @Test
  public void andTest() {
    final ASTJexlScript script = jexlEngine.parseExpression("a==\"woof\" && b==\"meow\"");

    script.childrenAccept(underTest, builder);

    assertThat(builder.getQueryString(), is("a_alias = :param_0 and b_alias = :param_1"));
    assertThat(builder.getQueryParameters().size(), is(2));
    assertThat(builder.getQueryParameters().get("param_0"), is("woof"));
    assertThat(builder.getQueryParameters().get("param_1"), is("meow"));
  }

  @Test
  public void orTest() {
    final ASTJexlScript script = jexlEngine.parseExpression("a==\"woof\" || b==\"meow\"");

    script.childrenAccept(underTest, builder);

    assertThat(builder.getQueryString(), is("a_alias = :param_0 or b_alias = :param_1"));
    assertThat(builder.getQueryParameters().size(), is(2));
    assertThat(builder.getQueryParameters().get("param_0"), is("woof"));
    assertThat(builder.getQueryParameters().get("param_1"), is("meow"));
  }

  @Test
  public void likeTest() {
    final ASTJexlScript script = jexlEngine.parseExpression("a =^ \"woof\"");

    script.childrenAccept(underTest, builder);

    assertThat(builder.getQueryString(), is("a_alias like :param_0"));
    assertThat(builder.getQueryParameters().size(), is(1));
    assertThat(builder.getQueryParameters().get("param_0"), is("woof%"));
  }

  @Test
  public void notEqualTest() {
    final ASTJexlScript script = jexlEngine.parseExpression("a != \"woof\"");

    script.childrenAccept(underTest, builder);

    assertThat(builder.getQueryString(), is("(a_alias is null or a_alias <> :param_0)"));
    assertThat(builder.getQueryParameters().size(), is(1));
    assertThat(builder.getQueryParameters().get("param_0"), is("woof"));
  }

  @Test
  public void parensTest() {
    final ASTJexlScript script = jexlEngine.parseExpression("a==\"woof\" && (b==\"meow\" || b==\"purr\")");

    script.childrenAccept(underTest, builder);

    assertThat(builder.getQueryString(), is("a_alias = :param_0 and (b_alias = :param_1 or b_alias = :param_2)"));
    assertThat(builder.getQueryParameters().size(), is(3));
    assertThat(builder.getQueryParameters().get("param_0"), is("woof"));
    assertThat(builder.getQueryParameters().get("param_1"), is("meow"));
    assertThat(builder.getQueryParameters().get("param_2"), is("purr"));
  }

  @Test
  public void refTest() {
    final ASTJexlScript script = jexlEngine.parseExpression("a == dog.name");

    script.childrenAccept(underTest, builder);

    assertThat(builder.getQueryString(), is("a_alias = prop.name"));
    assertThat(builder.getQueryParameters().size(), is(0));
  }

  @Test
  public void regexpTest() {
    final ASTJexlScript script = jexlEngine.parseExpression("a =~ \"woof\"");

    script.childrenAccept(underTest, builder);

    assertThat(builder.getQueryString(), is("a_alias ~ :param_0"));
    assertThat(builder.getQueryParameters().size(), is(1));
    assertThat(builder.getQueryParameters().get("param_0"), is("^(woof)$"));
  }
}

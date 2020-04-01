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

import org.apache.commons.jexl3.parser.ASTJexlScript;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class PostgresSqlTransformerTest
    extends DatastoreSqlTransformerTest
{
  private PostgresSqlTransformer transformer;

  public DatastoreSqlTransformer getTransformer() {
    return transformer;
  }

  @Before
  public void setupTransformer() {
    transformer = new PostgresSqlTransformer();
  }

  @Test
  public void regexpTest() {
    final ASTJexlScript script = jexlEngine.parseExpression("a =~ \"woof\"");

    script.childrenAccept(getTransformer(), builder);

    assertThat(builder.getQueryString(), is("a_alias ~ :param_0"));
    assertThat(builder.getQueryParameters().size(), is(1));
    assertThat(builder.getQueryParameters().get("param_0"), is("woof"));
  }
}

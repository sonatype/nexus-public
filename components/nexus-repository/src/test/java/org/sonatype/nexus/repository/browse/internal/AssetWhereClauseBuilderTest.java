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

import org.junit.Test;

import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.sonatype.nexus.repository.browse.internal.AssetWhereClauseBuilder.whereClause;

public class AssetWhereClauseBuilderTest
    extends TestSupport
{
  @Test
  public void content() throws Exception {
    assertThat(whereClause("content", false), is(equalTo("content")));
  }

  @Test
  public void contentAndFilter() throws Exception {
    assertThat(whereClause("content", true), is(equalTo("content AND name LIKE :nameFilter")));
  }

  @Test
  public void contentAndLastId() {
    assertThat(whereClause("content", true, true), is(equalTo("content AND name LIKE :nameFilter AND @rid > :rid")));
  }

  @Test
  public void noContent() {
    assertThat(whereClause(null, false), is(nullValue()));
  }

  @Test
  public void noContentAndFilter() {
    assertThat(whereClause("", true), is(equalTo("name LIKE :nameFilter")));
  }

  @Test
  public void noContentFilterAndLastId() {
    assertThat(whereClause(null, true, true), is(equalTo("name LIKE :nameFilter AND @rid > :rid")));
  }
}

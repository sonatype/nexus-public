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
package org.sonatype.nexus.repository.golang;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.cache.CacheControllerHolder;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

public class AssetKindTest
    extends TestSupport
{
  private AssetKind GO_PACKAGE;

  private AssetKind GO_MODULE;

  private AssetKind GO_INFO;

  @Before
  public void setUp() throws Exception {
    GO_PACKAGE = AssetKind.PACKAGE;
    GO_MODULE = AssetKind.MODULE;
    GO_INFO = AssetKind.INFO;
  }

  @Test
  public void getCacheType() {
    assertThat(GO_PACKAGE.getCacheType(), is(equalTo(CacheControllerHolder.CONTENT)));
    assertThat(GO_MODULE.getCacheType(), is(equalTo(CacheControllerHolder.METADATA)));
    assertThat(GO_INFO.getCacheType(), is(equalTo(CacheControllerHolder.METADATA)));
  }
}

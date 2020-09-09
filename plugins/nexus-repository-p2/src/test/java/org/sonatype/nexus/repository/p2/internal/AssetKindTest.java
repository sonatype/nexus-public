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
package org.sonatype.nexus.repository.p2.internal;

import org.sonatype.goodies.testsupport.TestSupport;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.sonatype.nexus.repository.cache.CacheControllerHolder.CONTENT;
import static org.sonatype.nexus.repository.cache.CacheControllerHolder.METADATA;
import static org.sonatype.nexus.repository.p2.internal.AssetKind.ARTIFACTS_METADATA;
import static org.sonatype.nexus.repository.p2.internal.AssetKind.BUNDLE;
import static org.sonatype.nexus.repository.p2.internal.AssetKind.BINARY_BUNDLE;
import static org.sonatype.nexus.repository.p2.internal.AssetKind.COMPOSITE_ARTIFACTS;
import static org.sonatype.nexus.repository.p2.internal.AssetKind.COMPOSITE_CONTENT;
import static org.sonatype.nexus.repository.p2.internal.AssetKind.P2_INDEX;

public class AssetKindTest
    extends TestSupport
{
  @Test
  public void cacheTypes() throws Exception {
    assertThat(P2_INDEX.getCacheType(), is(equalTo(METADATA)));
    assertThat(AssetKind.CONTENT_METADATA.getCacheType(), is(equalTo(METADATA)));
    assertThat(ARTIFACTS_METADATA.getCacheType(), is(equalTo(METADATA)));
    assertThat(COMPOSITE_ARTIFACTS.getCacheType(), is(equalTo(METADATA)));
    assertThat(COMPOSITE_CONTENT.getCacheType(), is(equalTo(METADATA)));
    assertThat(BUNDLE.getCacheType(), is(equalTo(CONTENT)));
    assertThat(BINARY_BUNDLE.getCacheType(), is(equalTo(CONTENT)));
  }
}

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
package org.sonatype.nexus.repository.npm.internal.orient;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.storage.TempBlob;

import com.google.common.collect.ImmutableMap;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;

public class NpmPublishRequestTest
    extends TestSupport
{
  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Mock
  NestedAttributesMap packageRoot;

  @Mock
  TempBlob tempBlobA;

  @Mock
  TempBlob tempBlobB;

  @Test
  public void manageAndDeleteTempBlobsCorrectly() {
    try (NpmPublishRequest request = newNpmPublishRequest()) {
      assertThat(request.getPackageRoot(), is(packageRoot));
      assertThat(request.requireBlob("a"), is(tempBlobA));
      assertThat(request.requireBlob("b"), is(tempBlobB));
    }
    verify(tempBlobA).close();
    verify(tempBlobB).close();
  }

  @Test
  public void throwExceptionOnMissingTempBlob() {
    exception.expectMessage("blob-z");
    exception.expect(IllegalStateException.class);
    try (NpmPublishRequest request = newNpmPublishRequest()) {
      request.requireBlob("blob-z");
    }
  }

  private NpmPublishRequest newNpmPublishRequest() {
    return new NpmPublishRequest(packageRoot, ImmutableMap.of("a", tempBlobA, "b", tempBlobB));
  }
}

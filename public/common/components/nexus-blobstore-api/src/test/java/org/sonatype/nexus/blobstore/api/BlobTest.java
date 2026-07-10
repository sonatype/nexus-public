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
package org.sonatype.nexus.blobstore.api;

import java.net.URL;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;

/**
 * Tests for the default methods of {@link Blob}.
 */
public class BlobTest
{
  private Blob underTest;

  @Before
  public void setUp() {
    underTest = mock(Blob.class, CALLS_REAL_METHODS);
  }

  @Test
  public void getRedirectUrlReturnsEmptyByDefault() {
    Optional<URL> result = underTest.getRedirectUrl("download", "name.txt", "text/plain");

    assertThat(result.isPresent(), is(false));
  }

  @Test
  public void getRedirectUrlReturnsEmptyWithNullContentType() {
    Optional<URL> result = underTest.getRedirectUrl("download", "name.txt", null);

    assertThat(result.isPresent(), is(false));
  }

  @Test
  public void getRedirectUrlReturnsEmptyRegardlessOfArguments() {
    // the default body ignores every argument, so even null action/name must not throw and still yields empty
    Optional<URL> result = underTest.getRedirectUrl(null, null, null);

    assertThat(result.isPresent(), is(false));
  }
}

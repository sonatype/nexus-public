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
package org.sonatype.nexus.blobstore;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.blobstore.api.BlobId;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.sonatype.nexus.blobstore.DirectPathLocationStrategy.DIRECT_PATH_PREFIX;
import static org.sonatype.nexus.blobstore.DirectPathLocationStrategy.DIRECT_PATH_ROOT;

/**
 * Tests for {@link DirectPathLocationStrategy}.
 */
public class DirectPathLocationStrategyTest
    extends TestSupport
{
  private static final String CORRECT_PATH = "/healthCheckSummary/maven-central/current/summary.html";

  private static final String PATH_WITH_TRAVERSAL = "/healthCheckSummary/maven-central/1/../details/details.html";

  private static final String PATH_WITH_PREFIX_INSIDE_TRAVERSAL =
      "/healthCheckSummary/maven-central/1/.path$./details/details.html";

  private static final String EXPECTED_PATH = DIRECT_PATH_ROOT + "/" + CORRECT_PATH;

  private LocationStrategy underTest;

  @Before
  public void setUp() {
    underTest = new DirectPathLocationStrategy();
  }

  @Test
  public void testLocation() {
    String location = underTest.location(new BlobId(DIRECT_PATH_PREFIX + CORRECT_PATH));
    assertEquals(EXPECTED_PATH, location);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testLocationWithTraversal() {
    underTest.location(new BlobId(DIRECT_PATH_PREFIX + PATH_WITH_TRAVERSAL));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testLocationWithPrefixInsideTraversal() {
    underTest.location(new BlobId(DIRECT_PATH_PREFIX + PATH_WITH_PREFIX_INSIDE_TRAVERSAL));
  }

  @Test(expected = NullPointerException.class)
  public void testLocationWithNullableBlobId() {
    underTest.location(null);
  }
}

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
package org.sonatype.nexus.repository.cache.internal

import org.sonatype.goodies.testsupport.TestSupport
import org.sonatype.nexus.repository.cache.NegativeCacheKey

import org.junit.Test

import static org.mockito.Mockito.mock

/**
 * Tests for {@link PathNegativeCacheKey}.
 */
class PathNegativeCacheKeyTest
extends TestSupport
{

  /**
   * Given:
   * - a parent path cache key
   * - a child key that has a path that starts with parent key path
   * Then:
   * - #isParentOf returns false
   */
  @Test
  void 'child key should start with same path'() {
    def underTest = new PathNegativeCacheKey('/foo/')
    assert underTest.isParentOf(new PathNegativeCacheKey('/foo/bar.jar'))
    assert underTest.isParentOf(new PathNegativeCacheKey('/foo/and/more/bar.jar'))
  }

  /**
   * Given:
   * - a parent path cache key
   * - a child key that has a path that starts with parent key path
   * Then:
   * - #isParentOf returns false
   */
  @Test
  void 'path must end with slash to be a parent'() {
    def underTest = new PathNegativeCacheKey('/foo')
    assert !underTest.isParentOf(new PathNegativeCacheKey('/foo/bar.jar'))
    assert !underTest.isParentOf(new PathNegativeCacheKey('/foo/and/more/bar.jar'))
  }

  /**
   * Given:
   * - a parent path cache key
   * - child key is not an {@link PathNegativeCacheKey}
   * Then:
   * - #isParentOf returns false
   */
  @Test
  void 'child key must be of same class'() {
    def underTest = new PathNegativeCacheKey('/foo/bar.jar')
    assert !underTest.isParentOf(mock(NegativeCacheKey))
  }

}

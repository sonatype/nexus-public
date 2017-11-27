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
package org.sonatype.nexus.repository.browse;

import java.util.List;

import org.sonatype.nexus.repository.storage.Component;

import com.google.common.collect.Lists;
import org.junit.Test;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

public class BrowseResultTest
{
  private final List<Component> components = Lists.newArrayList(
      mock(Component.class),
      mock(Component.class),
      mock(Component.class)
  );

  @Test
  public void testEstimateCount_NoStartOrLimit() {
    // no start or limit, expected is just the count of results
    runTest(null, null, 3);
  }

  @Test
  public void testEstimateCount_LessResultsThanPageSize() {
    // start at 0 with a limit of 2, expected is the count of results (i.e. no further page expected)
    runTest(0, 2, 3);
  }

  @Test
  public void testEstimateCount_SameResultsAsPageSize() {
    // start at 0 with a limit of 3 (same as results count), expect another page
    runTest(0, 3, 6);
  }

  @Test
  public void testEstimateCount_WithSkipExpectThirdPage() {
    // start at 3 (2nd page of skip/limit style paging) with a limit of 3 (same as results count), expect a 3rd page of results
    runTest(3, 3, 9);
  }

  private void runTest(final Integer start, final Integer limit, final long expected) {
    QueryOptions queryOptions = new QueryOptions(null, null, null, start, limit, null);
    BrowseResult<Component> result = new BrowseResult<>(queryOptions, components);
    assertThat(result.getTotal(), equalTo(expected));
  }
}

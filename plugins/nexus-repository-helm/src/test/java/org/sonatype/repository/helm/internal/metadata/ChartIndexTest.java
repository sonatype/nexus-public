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
package org.sonatype.repository.helm.internal.metadata;

import org.sonatype.goodies.testsupport.TestSupport;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class ChartIndexTest
    extends TestSupport
{
  private ChartIndex underTest;

  @Before
  public void setUp() throws Exception {
    underTest = new ChartIndex();
  }

  @Test
  public void addChartEntry() {
    ChartEntry chartEntry = new ChartEntry();
    underTest.addEntry(chartEntry);

    assertThat(underTest.getEntries().size(), is(1));
  }

  @Test
  public void addMultipleChartEntries() throws Exception {
    ChartEntry chartEntry = new ChartEntry();
    ChartEntry chartEntrySameName = new ChartEntry();
    ChartEntry chartEntryDifferentName = new ChartEntry();

    chartEntry.setName("test");
    chartEntrySameName.setName("test");
    chartEntryDifferentName.setName("different name");

    underTest.addEntry(chartEntry);
    underTest.addEntry(chartEntrySameName);

    assertThat(underTest.getEntries().size(), is(1));

    underTest.addEntry(chartEntryDifferentName);

    assertThat(underTest.getEntries().size(), is(2));
  }
}

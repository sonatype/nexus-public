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
package org.sonatype.nexus.repository.apt.datastore.internal.search.sql;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.search.sql.SearchRecord;

import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AptSearchCustomFieldContributorTest
    extends TestSupport
{
  @Mock
  private Asset asset;

  private final AptSearchCustomFieldContributor underTest = new AptSearchCustomFieldContributor();

  @Test
  public void shouldAddPathWithoutLeadingSlash() {
    SearchRecord data = mock(SearchRecord.class);
    String path = "/org/foo/1.0/foo-1.0.txt";
    when(asset.path()).thenReturn(path);

    underTest.populateSearchCustomFields(data, asset);

    verify(data).addKeyword(path.substring(1));
  }
}

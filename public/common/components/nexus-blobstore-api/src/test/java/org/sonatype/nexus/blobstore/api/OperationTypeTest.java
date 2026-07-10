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

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;

/**
 * Tests for {@link OperationType}.
 */
public class OperationTypeTest
{
  @Test
  public void testValues() {
    assertThat(OperationType.values(), arrayContaining(OperationType.UPLOAD, OperationType.DOWNLOAD));
  }

  @Test
  public void testValueOf() {
    assertThat(OperationType.valueOf("UPLOAD"), is(sameInstance(OperationType.UPLOAD)));
    assertThat(OperationType.valueOf("DOWNLOAD"), is(sameInstance(OperationType.DOWNLOAD)));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testValueOfUnknownThrows() {
    OperationType.valueOf("UNKNOWN");
  }

  @Test(expected = NullPointerException.class)
  public void testValueOfNullThrows() {
    OperationType.valueOf(null);
  }

  @Test
  public void testNameAndOrdinal() {
    assertThat(OperationType.UPLOAD.name(), is("UPLOAD"));
    assertThat(OperationType.UPLOAD.ordinal(), is(0));
    assertThat(OperationType.DOWNLOAD.name(), is("DOWNLOAD"));
    assertThat(OperationType.DOWNLOAD.ordinal(), is(1));
  }
}

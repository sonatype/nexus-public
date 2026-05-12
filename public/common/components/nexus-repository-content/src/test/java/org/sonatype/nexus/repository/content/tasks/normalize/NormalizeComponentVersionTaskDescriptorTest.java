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
package org.sonatype.nexus.repository.content.tasks.normalize;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

public class NormalizeComponentVersionTaskDescriptorTest
{
  @Test
  public void testTypeId() {
    assertThat(NormalizeComponentVersionTaskDescriptor.TYPE_ID, is(equalTo("component.normalize.version")));
  }

  @Test
  public void testDescriptorProperties() {
    NormalizeComponentVersionTaskDescriptor underTest =
        new NormalizeComponentVersionTaskDescriptor(false, false);

    assertThat(underTest.getId(), is(equalTo(NormalizeComponentVersionTaskDescriptor.TYPE_ID)));
    assertThat(underTest.getName(), is(equalTo("Repair - Normalize component versions for retain-n")));
    assertThat(underTest.getType(), is(equalTo(NormalizeComponentVersionTask.class)));
    assertThat(underTest.isVisible(), is(false));
    assertThat(underTest.isExposed(), is(false));
  }
}

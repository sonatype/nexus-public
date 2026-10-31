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

public class ReNormalizeComponentVersionTaskDescriptorTest
{
  @Test
  public void testTypeId() {
    assertThat(ReNormalizeComponentVersionTaskDescriptor.TYPE_ID, is(equalTo("component.renormalize.version")));
  }

  @Test
  public void testDescriptorProperties() {
    ReNormalizeComponentVersionTaskDescriptor underTest = new ReNormalizeComponentVersionTaskDescriptor();

    assertThat(underTest.getId(), is(equalTo(ReNormalizeComponentVersionTaskDescriptor.TYPE_ID)));
    assertThat(underTest.getType(), is(equalTo(ReNormalizeComponentVersionTask.class)));
    // internal repair task: never user-visible or exposed, unlike NormalizeComponentVersionTaskDescriptor which
    // supports opt-in visibility via nexus.component.normalize.{visible,expose}
    assertThat(underTest.isVisible(), is(false));
    assertThat(underTest.isExposed(), is(false));
  }
}

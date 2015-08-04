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
package org.sonatype.scheduling;

import org.sonatype.sisu.litmus.testsupport.TestSupport;

import org.junit.Test;

/**
 * Tests for {@link LoggingProgressListener}.
 */
public class LoggingProgressListenerTest
    extends TestSupport
{
  @Test
  public void testSimple() {
    LoggingProgressListener pl = new LoggingProgressListener("foo");

    pl.beginTask("Task1", 10);

    pl.working(3);

    pl.working("Hm, this is hard!", 3);

    pl.beginTask("Task2", 10);

    pl.working(3);

    pl.beginTask("Task3", 10);

    pl.working(3);
    pl.working("Hm, this is hard!", 5);

    pl.endTask("Okay!");
    pl.endTask("Okay!");
    pl.endTask("Okay!");
  }

  @Test
  public void testSimpleUnknown() {
    LoggingProgressListener pl = new LoggingProgressListener("foo");

    pl.beginTask("Task1");

    pl.working(3);

    pl.working("Hm, this is hard!", 3);

    pl.beginTask("Task2", 10);

    pl.working(3);

    pl.beginTask("Task3", 10);

    pl.working(3);
    pl.working("Hm, this is hard!", 5);

    pl.endTask("Okay!");
    pl.endTask("Okay!");
    pl.endTask("Okay!");
  }

}

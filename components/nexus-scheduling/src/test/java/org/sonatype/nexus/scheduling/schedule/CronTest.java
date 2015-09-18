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
package org.sonatype.nexus.scheduling.schedule;

import java.util.Date;

import org.sonatype.goodies.testsupport.TestSupport;

import org.junit.Test;

public class CronTest
    extends TestSupport
{
  @Test
  public void goodOnes() {
    new Cron(new Date(), "0 0 0 ? * SAT");
    new Cron(new Date(), "0 0 0 ? * FRI,SAT");
    new Cron(new Date(), "1 2 4 1,2,3,10,11,12 * ?");
    new Cron(new Date(), "0,5,10 0-6 0 L * ?");
    new Cron(new Date(), "0,5,10 0-6 0 L * 2014");
    new Cron(new Date(), "0,5,10 0-6 0 L * 2014-2017");
  }

  @Test(expected = IllegalArgumentException.class)
  public void badOneMonthInsteadOfDay() {
    new Cron(new Date(), "0 0 0 ? * JAN");
  }

  @Test(expected = IllegalArgumentException.class)
  public void badOneDotInDays() {
    new Cron(new Date(), "0 0 0 ? * FRI.,SAT");
  }

  @Test(expected = IllegalArgumentException.class)
  public void badOneQuestionmarkAmongDays() {
    new Cron(new Date(), "1 2 4 ?,1,2,3,10,11,12 * ?");
  }

  @Test(expected = IllegalArgumentException.class)
  public void badOneLAtBadPosition() {
    new Cron(new Date(), "0 0 L L * ?");
  }

}

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
package org.sonatype.nexus.pax.exam;

import java.io.File;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.testsupport.junit.TestIndexRule;

import org.junit.runner.Description;
import org.junit.runner.notification.Failure;

import static org.sonatype.nexus.pax.exam.NexusPaxExamSupport.captureLogs;
import static org.sonatype.nexus.pax.exam.TestCounters.lastInstallDirectory;

/**
 * A readily configured and injectable {@link TestIndexRule} for Pax-Exam based integration tests.
 * 
 * @since 3.2
 */
@Named
@Singleton
public class NexusPaxExamTestIndexRule
    extends TestIndexRule
{
  public NexusPaxExamTestIndexRule() {
    super(TestBaseDir.resolve("target/it-reports"), TestBaseDir.resolve("target/it-data"));
  }

  /**
   * Last chance capture of log files when there's a failure outside of normal notification.
   *
   * @since 3.14
   */
  static void captureLogsOnFailure(final Failure failure) {
    try {
      new NexusPaxExamTestIndexRule().doCaptureLogsOnFailure(failure);
    }
    catch (Throwable e) {  // NOSONAR
      e.printStackTrace(); // NOSONAR
    }
  }

  private void doCaptureLogsOnFailure(final Failure failure) {
    Description description = failure.getDescription();
    File lastInstallDir = lastInstallDirectory();

    // point index to the last known install of NXRM
    setDirectory(lastInstallDir);

    // need to go through all these steps to record and persist the failure and logs
    starting(description);
    failed(failure.getException(), description);
    File logDir = new File(lastInstallDir, "nexus3/log");
    captureLogs(this, logDir, description.getClassName());
    finished(description);
  }
}

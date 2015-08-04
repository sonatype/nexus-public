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
package org.sonatype.nexus.testsuite.misc.nexus166;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.integrationtests.TestContainer;

import org.junit.BeforeClass;
import org.junit.Test;


/**
 * A sample test and a good starting point: <a href='https://docs.sonatype.com/display/NX/Nexus+Test-Harness'>Nexus
 * Test-Harness</a>
 */
//@RunWith( ConsoleLoggingRunner.class )
public class Nexus166SampleIT
    extends AbstractNexusIntegrationTest
{
  @BeforeClass
  public static void setSecureTest() {
    TestContainer.getInstance().getTestContext().setSecureTest(true);
  }

  @Test
  public void sampleTest() throws IOException {
    log.debug("This is just an example test");
    log.debug("I will show you how to do a few simple things...");

    File exampleFile = this.getTestFile("example.txt");

    BufferedReader reader = new BufferedReader(new FileReader(exampleFile));

    // we only have one line to read.
    String exampleText = reader.readLine();
    reader.close();

    // you get the point...
    log.debug("exampleText: " + exampleText);
  }

}

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
package com.sonatype.nexus.docker.testsupport;

import java.util.List;

import com.sonatype.nexus.docker.testsupport.framework.DockerContainerClient;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.junit.After;
import org.junit.Before;
import org.slf4j.LoggerFactory;

import static ch.qos.logback.classic.Level.INFO;
import static ch.qos.logback.classic.Level.TRACE;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertThat;

public abstract class AbstractCommandLineTest
{
  @Before
  public void onInitializeCommandLineTest() {
    setDockerContainerClientLogLevel(TRACE);
  }

  @After
  public void onTearDownCommandLineTest() {
    setDockerContainerClientLogLevel(INFO);
  }

  protected void assertToHaveMoreLinesThan(List<String> outputLines, int size) {
    assertThat(outputLines.size(), greaterThan(size));
  }

  protected void assertLastValue(List<String> outputLines, String lineValue) {
    assertThat(outputLines.get(outputLines.size() - 1), equalTo(lineValue));
  }

  protected void setDockerContainerClientLogLevel(Level level) {
    ((Logger) LoggerFactory.getLogger(DockerContainerClient.class)).setLevel(level);
  }
}

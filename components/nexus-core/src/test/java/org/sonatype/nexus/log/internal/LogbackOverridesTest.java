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
package org.sonatype.nexus.log.internal;

import java.io.File;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import org.sonatype.nexus.log.LoggerLevel;
import org.sonatype.sisu.litmus.testsupport.TestSupport;
import org.sonatype.sisu.litmus.testsupport.hamcrest.DiffMatchers;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

/**
 * {@link LogbackOverrides} UTs.
 *
 * @since 2.7
 */

public class LogbackOverridesTest
    extends TestSupport
{

  /**
   * Verify that loggers are written in an expected logback format.
   */
  @Test
  public void writeLogbackXml() throws Exception {
    File logbackXml = util.createTempFile();

    Map<String, LoggerLevel> loggers = Maps.newLinkedHashMap();
    loggers.put("foo", LoggerLevel.ERROR);
    loggers.put("bar", LoggerLevel.INFO);

    LogbackOverrides.write(logbackXml, loggers);

    String expected = IOUtils.toString(getClass().getResourceAsStream("logback-expected.xml"));
    String actual = FileUtils.readFileToString(logbackXml);
    assertThat(actual, DiffMatchers.equalTo(expected));
  }

  /**
   * Verify that loggers are read from logback format.
   */
  @Test
  public void readLogbackXml() throws Exception {
    File logbackXml = util.createTempFile();
    FileUtils.write(logbackXml, IOUtils.toString(getClass().getResourceAsStream("logback-expected.xml")));
    Collection<String> actual = convertToStringList(LogbackOverrides.read(logbackXml));
    assertThat(actual, containsInAnyOrder("foo|ERROR", "bar|INFO"));
  }

  private Collection<String> convertToStringList(final Map<String, LoggerLevel> loggers) {
    Collection<String> converted = Sets.newHashSet();
    for (Entry<String, LoggerLevel> entry : loggers.entrySet()) {
      converted.add(entry.getKey() + "|" + entry.getValue().name());
    }
    return converted;
  }

}

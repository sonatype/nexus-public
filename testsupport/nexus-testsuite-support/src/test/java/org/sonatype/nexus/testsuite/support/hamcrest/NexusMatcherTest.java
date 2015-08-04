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
package org.sonatype.nexus.testsuite.support.hamcrest;

import java.io.File;

import org.sonatype.sisu.litmus.testsupport.TestSupport;
import org.sonatype.sisu.litmus.testsupport.junit.TestInfoRule;

import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;

/**
 * {@link NexusMatchers} UTs.
 *
 * @since 2.2
 */
public class NexusMatcherTest
    extends TestSupport
{

  @Rule
  public ExpectedException thrown = ExpectedException.none().handleAssertionErrors();

  @Rule
  public TestInfoRule testInfo = new TestInfoRule();

  /**
   * Verify that if log file to be matched does not exist an {@link AssertionError} is thrown with a proper message.
   */
  @Test
  public void inexistentLogFile() {
    thrown.expect(AssertionError.class);
    thrown.expectMessage(Matchers.<String>allOf(
        containsString("java.io.FileNotFoundException"),
        containsString("nexus.log")
    ));
    assertThat(
        new File("nexus.log"),
        NexusMatchers.doesNotHaveCommonExceptions()
    );
  }

  /**
   * Verify that a log file that does not contain NPE,CNF,CCE matches.
   */
  @Test
  public void doesNotHaveCommonExceptions() {
    assertThat(
        resolveLogFile(),
        NexusMatchers.doesNotHaveCommonExceptions()
    );
  }

  /**
   * Verify that a log file that does contain one of NPE,CNF,CCE fails.
   */
  @Test
  public void hasCommonExceptions() {
    thrown.expect(AssertionError.class);
    thrown.expectMessage(Matchers.<String>allOf(
        containsString("Log file does not contain any of the common unwanted exceptions"),
        endsWith("contained on line <1>: \"java.lang.ClassNotFoundException: some.package.Foo\"")
    ));
    assertThat(
        resolveLogFile(),
        NexusMatchers.doesNotHaveCommonExceptions()
    );
  }

  /**
   * Verify that a log file that has all plugins activated successfully matches.
   */
  @Test
  public void doesNotHaveFailingPlugins()
      throws Exception
  {
    assertThat(
        resolveLogFile(),
        NexusMatchers.doesNotHaveFailingPlugins()
    );
  }

  /**
   * Verify that a log file that has failing plugins matches.
   */
  @Test
  public void hasFailingPlugins()
      throws Exception
  {
    assertThat(
        resolveLogFile(),
        NexusMatchers.hasFailingPlugins()
    );
  }

  /**
   * Verify that a log file that has  successfully activated "org.sonatype.nexus.plugins:nexus-indexer-lucene-plugin"
   * plugin matches, even if some other plugin failed to activate.
   */
  @Test
  public void hasPluginActivatedSuccessfully()
      throws Exception
  {
    assertThat(
        resolveLogFile(),
        NexusMatchers.hasPluginActivatedSuccessfully("org.sonatype.nexus.plugins:nexus-indexer-lucene-plugin")
    );
  }

  /**
   * Verify that a log file that has a failing "com.sonatype.nexus.plugin:nexus-outreach-plugin" plugin matches.
   */
  @Test
  public void hasFailingPlugin()
      throws Exception
  {
    assertThat(
        resolveLogFile(),
        NexusMatchers.hasFailingPlugin("com.sonatype.nexus.plugin:nexus-outreach-plugin")
    );
  }

  private File resolveLogFile() {
    return util.resolveFile(String.format(
        "src/test/uncopied-resources/%s/%s.log", testInfo.getTestClass().getSimpleName(), testInfo.getMethodName()
    ));
  }

}

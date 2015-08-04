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
package org.sonatype.nexus.ruby;

import org.sonatype.sisu.litmus.testsupport.TestSupport;

import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.sonatype.nexus.ruby.TestUtils.lastLine;
import static org.sonatype.nexus.ruby.TestUtils.numberOfLines;

public class BundleRunnerTest
  extends TestSupport
{
  @Rule
  public TestJRubyContainerRule testJRubyContainerRule = new TestJRubyContainerRule();

  @Test
  public void testInstall() throws Exception {
    final BundleRunner runner = new BundleRunner(testJRubyContainerRule.getScriptingContainer());
    //System.err.println( runner.install() );
    assertThat(numberOfLines(runner.install()), is(10));
    assertThat(lastLine(runner.install()),
        startsWith("Use `bundle show [gemname]` to see where a bundled gem is installed."));
  }

  @Test
  public void testShowAll() throws Exception {
    final BundleRunner runner = new BundleRunner(testJRubyContainerRule.getScriptingContainer());
    assertThat(numberOfLines(runner.show()), is(5));
  }

  @Test
  public void testShow() throws Exception {
    final BundleRunner runner = new BundleRunner(testJRubyContainerRule.getScriptingContainer());
    assertThat(numberOfLines(runner.show("zip")), is(1));
    assertThat(lastLine(runner.show("zip")), endsWith("zip-2.0.2"));
  }

  @Test
  public void testConfig() throws Exception {
    final BundleRunner runner = new BundleRunner(testJRubyContainerRule.getScriptingContainer());
    assertThat(runner.config(), containsString("mirror.http://rubygems.org"));
  }
}

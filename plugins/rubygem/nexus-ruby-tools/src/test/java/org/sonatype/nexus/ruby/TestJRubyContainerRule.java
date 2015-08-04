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

import org.jruby.embed.ScriptingContainer;
import org.junit.rules.ExternalResource;

/**
 * Simple Junit rule to expose managed container. If a test needs access to container in more specific places,
 * ie. ctor and so, take a look at {@link RubyScriptingTestSupport}.
 */
public class TestJRubyContainerRule
    extends ExternalResource
{
  private TestJRubyContainer testScriptingContainer = new TestJRubyContainer();

  @Override
  protected void before() throws Throwable {
    super.before();
    testScriptingContainer.start();
  }

  @Override
  protected void after() {
    super.after();
    testScriptingContainer.stop();
  }

  public ScriptingContainer getScriptingContainer() {
    return testScriptingContainer.getScriptingContainer();
  }

  public RubygemsGateway getRubygemsGateway() { return testScriptingContainer.getRubygemsGateway(); }
}
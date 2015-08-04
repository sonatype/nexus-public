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

import org.jruby.embed.PathType;
import org.jruby.embed.ScriptingContainer;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * wrapper around the <code>bundle</code> command using the a jruby ScriptingContainer
 * to execute it.
 *
 * @author christian
 */
public class BundleRunner
    extends ScriptWrapper
{
  /**
   * @param ruby ScriptingContainer to use
   */
  public BundleRunner(ScriptingContainer ruby) {
    super(ruby, newScript(ruby));
  }

  /**
   * create a new ruby object of the bundler command
   */
  private static Object newScript(final ScriptingContainer scriptingContainer) {
    IRubyObject runnerClass = scriptingContainer.parse(PathType.CLASSPATH, "nexus/bundle_runner.rb").run();
    return scriptingContainer.callMethod(runnerClass, "new", IRubyObject.class);
  }

  /**
   * execute <code>bundle install</code>
   *
   * @return STDOUT from the command execution as String
   */
  public String install() {
    return callMethod("exec", "install", String.class);
  }

  /**
   * execute <code>bundle show</code>
   *
   * @return STDOUT from the command execution as String
   */
  public String show() {
    return callMethod("exec", "show", String.class);
  }

  /**
   * execute <code>bundle config</code>
   *
   * @return STDOUT from the command execution as String
   */
  public String config() {
    return callMethod("exec", "config", String.class);
  }

  /**
   * execute <code>bundle show {gem-name}</code>
   *
   * @param gemName to be passed to the show command
   * @return STDOUT from the command execution as String
   */
  public String show(String gemName) {
    return callMethod("exec", new String[]{"show", gemName}, String.class);
  }
}

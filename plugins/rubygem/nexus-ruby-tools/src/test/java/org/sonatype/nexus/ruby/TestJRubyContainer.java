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

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.jruby.embed.ScriptingContainer;

/**
 * Encapsulates all JRuby related containers and helpers into one class and manages their lifecycle. Hence,
 * this helper is reusable as in test rules, but also in other constructs.
 */
class TestJRubyContainer
{
  private ScriptingContainer scriptingContainer;

  private RubygemsGateway rubygemsGateway;

  public void start() {
    scriptingContainer = createScriptingContainer();
    rubygemsGateway = new DefaultRubygemsGateway(scriptingContainer);
  }

  public void stop() {
    if (rubygemsGateway != null) {
      rubygemsGateway.terminate();
    }
    rubygemsGateway = null;
    scriptingContainer = null;
  }

  public ScriptingContainer getScriptingContainer() {
    return scriptingContainer;
  }

  public RubygemsGateway getRubygemsGateway() {
    return rubygemsGateway;
  }

  private ScriptingContainer createScriptingContainer() {
    final String rubygems = new File("target/test-classes/rubygems").getAbsolutePath();
    final String gemfile = new File("target/test-classes/it/Gemfile").getAbsolutePath();
    final Map<String, String> env = new HashMap<String, String>();
    env.put("GEM_HOME", rubygems);
    env.put("GEM_PATH", rubygems);
    env.put("BUNDLE_GEMFILE", gemfile);
    env.put("PATH", ""); // bundler needs a PATH set
    env.put("DEBUG", "true");

    final ScriptingContainer scriptingContainer = new ScriptingContainer();
    scriptingContainer.setEnvironment(env);
    return scriptingContainer;
  }
}
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jruby.embed.PathType;
import org.jruby.embed.ScriptingContainer;
import org.jruby.runtime.builtin.IRubyObject;

public class GemRunner
    extends ScriptWrapper
{
  private final String baseUrl;

  public GemRunner(ScriptingContainer ruby, String baseUrl) {
    super(ruby, newScript(ruby));
    this.baseUrl = baseUrl;
  }

  private static Object newScript(final ScriptingContainer scriptingContainer) {
    IRubyObject runnerClass = scriptingContainer.parse(PathType.CLASSPATH, "nexus/gem_runner.rb").run();
    return scriptingContainer.callMethod(runnerClass, "new", IRubyObject.class);
  }

  public String install(String repoId, String... gems) {
    List<String> args = new ArrayList<String>();
    args.add("install");
    args.add("-r");
    addNoDocu(args);
    setSource(args, repoId);
    args.addAll(Arrays.asList(gems));
    return callMethod("exec", args.toArray(), String.class);
  }

  private void setSource(List<String> args, String repoId) {
    args.add("--clear-sources");
    args.add("--source");
    args.add(baseUrl + repoId + "/");
    args.add("--update-sources");
  }

  public String install(File... gems) {
    List<String> args = new ArrayList<String>();
    args.add("install");
    args.add("-l");
    addNoDocu(args);
    for (File gem : gems) {
      args.add(gem.getAbsolutePath());
    }

    return callMethod("exec", args.toArray(), String.class);
  }

  private void addNoDocu(List<String> args) {
    args.add("--no-rdoc");
    args.add("--no-ri");
  }

  public String push(String repoId, File gem) {
    List<String> args = new ArrayList<String>();
    args.add("push");
    args.add("--key");
    args.add("test");
    args.add("--host");
    args.add(baseUrl + repoId);
    args.add(gem.getAbsolutePath());

    return callMethod("exec", args.toArray(), String.class);
  }

  public String clearCache() {
    List<String> args = new ArrayList<String>();
    args.add("sources");
    args.add("--clear-all");

    return callMethod("exec", args.toArray(), String.class);
  }

  public String list() {
    return list(null);
  }

  public String list(String repoId) {
    List<String> args = new ArrayList<String>();
    args.add("list");
    if (repoId == null) {
      args.add("-l");
    }
    else {
      args.add("-r");
      setSource(args, repoId);
    }

    return callMethod("exec", args.toArray(), String.class);
  }

  public String nexus(File config, File gem) {
    List<String> args = new ArrayList<String>();
    args.add("nexus");
    args.add("--nexus-config");
    args.add(config.getAbsolutePath());
    args.add(gem.getAbsolutePath());

    // make sure the custom gem command is loaded when the gem is installed
    callMethod("load_plugins");

    return callMethod("exec", args.toArray(), String.class);
  }
}

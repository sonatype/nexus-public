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
package org.sonatype.nexus.ruby.cuba.api;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.sonatype.nexus.ruby.RubygemsFile;
import org.sonatype.nexus.ruby.cuba.Cuba;
import org.sonatype.nexus.ruby.cuba.State;

/**
 * cuba for /api/v1/dependencies
 *
 * @author christian
 */
public class ApiV1DependenciesCuba
    implements Cuba
{
  public static final String RUBY = ".ruby";

  private static final Pattern FILE = Pattern.compile("^([^/]+)[.]" + RUBY.substring(1) + "$");

  /**
   * no sub-directories
   *
   * if there is query string with "gems" parameter then <code>BundlerApiFile</code> or
   * <code>DependencyFile</code> gets created.
   *
   * otherwise all {name}.ruby will be created as <code>DependencyFile</code>
   *
   * the directory itself does not produce the directory listing - only the empty <code>Directory</code>
   * object.
   */
  @Override
  public RubygemsFile on(State state) {
    if (state.name.isEmpty()) {
      if (state.context.query.startsWith("gems=")) {
        if (state.context.query.contains(",") || state.context.query.contains("%2C")) {
          return state.context.factory.bundlerApiFile(state.context.query.substring(5));
        }
        else if (state.context.query.length() > 5) {
          return state.context.factory.dependencyFile(state.context.query.substring(5));
        }
      }
      if (state.context.original.endsWith("/")) {
        return state.context.factory.directory(state.context.original);
      }
      else {
        return state.context.factory.noContent(state.context.original);
      }
    }
    Matcher m;
    if (state.name.length() == 1) {
      if (state.path.length() < 2) {
        return state.context.factory.directory(state.context.original);
      }
      m = FILE.matcher(state.path.substring(1));
    }
    else {
      m = FILE.matcher(state.name);
    }
    if (m.matches()) {
      return state.context.factory.dependencyFile(m.group(1));
    }
    return state.context.factory.notFound(state.context.original);
  }
}

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
package org.sonatype.nexus.ruby.cuba.quick;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.sonatype.nexus.ruby.RubygemsFile;
import org.sonatype.nexus.ruby.cuba.Cuba;
import org.sonatype.nexus.ruby.cuba.State;

/**
 * cuba for /quick/Marshal.4.8
 *
 * @author christian
 */
public class QuickMarshalCuba
    implements Cuba
{
  public static final String GEMSPEC_RZ = ".gemspec.rz";

  private static Pattern FILE = Pattern.compile("^([^/]/)?([^/]+)" + GEMSPEC_RZ + "$");

  /**
   * no sub-directories
   *
   * create <code>GemspecFile</code>s for {name}-{version}.gemspec.rz or {first-char-of-name}/{name}-{version}.gemspec.rz
   *
   * the directory itself does not produce the directory listing - only the empty <code>Directory</code>
   * object.
   */
  @Override
  public RubygemsFile on(State state) {
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
      return state.context.factory.gemspecFile(m.group(2));
    }
    if (state.name.isEmpty()) {
      return state.context.factory.directory(state.context.original);
    }
    return state.context.factory.notFound(state.context.original);
  }
}
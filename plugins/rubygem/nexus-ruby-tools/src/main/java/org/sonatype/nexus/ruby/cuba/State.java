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
package org.sonatype.nexus.ruby.cuba;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.sonatype.nexus.ruby.RubygemsFile;

/**
 * this is the <code>State</code> with the current directory <code>name</code>
 * and the not parsed path.
 *
 * it can be visited by a <code>Cuba</code> object to eval itself via the <code>nested</code>
 * method.
 *
 * @author christian
 */

public class State
{
  static Pattern PATH_PART = Pattern.compile("^/([^/]*).*");

  public final String path;

  public final String name;

  public final Context context;

  public State(Context ctx, String path, String name) {
    this.context = ctx;
    this.path = path;
    this.name = name;
  }

  /**
   * it passes on the next directory of the remaining path (can be empty)
   * or there is no next directory then a <code>RubygemsFile</code> marked
   * as <code>notFound</code> is created.
   */
  public RubygemsFile nested(Cuba cuba) {
    if (path.isEmpty()) {
      // that is an directory, let the cuba object create the
      // right RubygemsFile for it
      return cuba.on(new State(context, "", ""));
    }
    Matcher m = PATH_PART.matcher(path);

    if (m.matches()) {
      String name = m.group(1);
      return cuba.on(new State(context,
          this.path.substring(1 + name.length()),
          name));
    }
    return context.factory.notFound(context.original);
  }

  public String toString() {
    StringBuilder b = new StringBuilder(getClass().getSimpleName());
    b.append("<").append(path).append(",").append(name).append("> )");
    return b.toString();
  }
}
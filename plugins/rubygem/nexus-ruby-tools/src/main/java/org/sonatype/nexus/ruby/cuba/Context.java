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

import org.sonatype.nexus.ruby.RubygemsFileFactory;

/**
 * the <code>Context</code> carries the original path and the query string
 * from the (HTTP) request as well the <code>RubygemsFileFactory</code> which
 * is used by the <code>Cuba</code> objects to create <code>RubygemsFile</code>s.
 *
 * it is basically the static part of the <code>State</code> object and is immutable.
 *
 * @author christian
 */

public class Context
{
  public final String original;

  public final String query;

  public final RubygemsFileFactory factory;

  public Context(RubygemsFileFactory factory, String original, String query) {
    this.original = original;
    this.query = query;
    this.factory = factory;
  }

  public String toString() {
    StringBuilder b = new StringBuilder(getClass().getSimpleName());
    b.append("<").append(original);
    if (!query.isEmpty()) {
      b.append("?").append(query);
    }
    b.append(">");
    return b.toString();
  }
}
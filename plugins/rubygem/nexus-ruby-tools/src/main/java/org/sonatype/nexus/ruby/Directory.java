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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * represents a directory with entries/items
 *
 * has no payload.
 *
 * @author christian
 */
public class Directory
    extends RubygemsFile
{
  /**
   * directory items
   */
  final List<String> items;

  public Directory(RubygemsFileFactory factory, String path, String name, String... items) {
    super(factory, FileType.DIRECTORY, path, path, name);
    set(null);// no payload
    this.items = new ArrayList<>(Arrays.asList(items));
  }

  /**
   * @return String[] the directory entries
   */
  public String[] getItems() {
    return items.toArray(new String[items.size()]);
  }

  protected void addToString(StringBuilder builder) {
    super.addToString(builder);
    builder.append(", items=").append(items);
  }
}
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

import java.io.InputStream;

import org.sonatype.nexus.ruby.FileType;
import org.sonatype.nexus.ruby.RubygemsFile;
import org.sonatype.nexus.ruby.RubygemsFileFactory;
import org.sonatype.nexus.ruby.layout.Layout;

public class RubygemsFileSystem
{
  private final Cuba cuba;

  private final RubygemsFileFactory factory;

  private final Layout getLayout;

  private final Layout postLayout;

  private final Layout deleteLayout;

  protected RubygemsFileSystem(RubygemsFileFactory factory,
                               Layout getLayout,
                               Layout postLayout,
                               Layout deleteLayout,
                               Cuba cuba)
  {
    this.cuba = cuba;
    this.factory = factory;
    this.getLayout = getLayout;
    this.postLayout = postLayout;
    this.deleteLayout = deleteLayout;
  }

  public RubygemsFile file(String path) {
    return visit(factory, path, null);
  }

  public RubygemsFile file(String path, String query) {
    return visit(factory, path, query);
  }

  public RubygemsFile get(String path) {
    return visit(getLayout, path, null);
  }

  public RubygemsFile get(String path, String query) {
    return visit(getLayout, path, query);
  }

  private RubygemsFile visit(RubygemsFileFactory factory, String originalPath, String query) {
    //normalize PATH-Separator from Windows platform to valid URL-Path
    //    https://github.com/sonatype/nexus-ruby-support/issues/38
    originalPath = originalPath.replace('\\', '/');
    if (!originalPath.startsWith("/")) {
      originalPath = "/" + originalPath;
    }
    String path = originalPath;
    if (query == null) {
      if (originalPath.contains("?")) {
        int index = originalPath.indexOf("?");
        if (index > -1) {
          query = originalPath.substring(index + 1);
          path = originalPath.substring(0, index);
        }
      }
      else {
        query = "";
      }
    }

    return new State(new Context(factory, originalPath, query), path, null).nested(cuba);
  }

  public RubygemsFile post(InputStream is, String path) {
    if (postLayout != null) {
      RubygemsFile file = visit(postLayout, path, "");
      if (!file.forbidden() && file.type() != FileType.NOT_FOUND) {
        post(is, file);
      }
      return file;
    }
    RubygemsFile file = visit(factory, path, "");
    file.markAsForbidden();
    return file;
  }

  public void post(InputStream is, RubygemsFile file) {
    postLayout.addGem(is, file);
  }

  public RubygemsFile delete(String path) {
    return visit(deleteLayout, path, "");
  }

  public String toString() {
    StringBuilder b = new StringBuilder(getClass().getSimpleName());
    b.append("<").append(cuba.getClass().getSimpleName()).append(">");
    return b.toString();
  }
}
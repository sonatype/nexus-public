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
package org.sonatype.nexus.yum.internal.rest;

import java.io.File;

import org.sonatype.nexus.yum.YumRepository;

import org.restlet.data.MediaType;
import org.restlet.resource.StringRepresentation;

/**
 * @since yum 3.0
 */
public class IndexRepresentation
    extends StringRepresentation
{

  public IndexRepresentation(UrlPathInterpretation interpretation, YumRepository yumRepository) {
    super(generateRepoIndex(yumRepository, interpretation));
    setMediaType(MediaType.TEXT_HTML);
  }

  private static CharSequence generateRepoIndex(YumRepository yumRepository,
                                                UrlPathInterpretation interpretation)
  {
    StringBuilder builder = new StringBuilder();
    builder.append("<html><head><title>File list</title></head><body><ul>");

    File directory = yumRepository.resolvePath(interpretation.getPath());

    appendFiles(builder, directory.listFiles());

    builder.append("</ul></html>");
    return builder.toString();
  }

  private static void appendFiles(StringBuilder builder, File[] files) {
    for (File file : files) {
      String name = file.getName();
      if (file.isDirectory()) {
        name += "/";
      }
      builder.append(String.format("<li><a href=\"%s\">%s</a></li>", name, name));
    }
  }
}

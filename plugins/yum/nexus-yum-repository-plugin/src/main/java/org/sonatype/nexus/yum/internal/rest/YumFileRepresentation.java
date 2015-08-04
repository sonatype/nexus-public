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

import org.sonatype.nexus.yum.YumRepository;

import org.restlet.data.MediaType;
import org.restlet.resource.FileRepresentation;

/**
 * @since yum 3.0
 */
public class YumFileRepresentation
    extends FileRepresentation
{

  public YumFileRepresentation(UrlPathInterpretation interpretation, YumRepository yumRepository) {
    super(yumRepository.resolvePath(interpretation.getPath()), getMediaType(interpretation.getPath()));
  }

  private static MediaType getMediaType(String path) {
    if (path.endsWith("xml")) {
      return MediaType.APPLICATION_XML;
    }
    else if (path.endsWith("gz")) {
      return MediaType.APPLICATION_GNU_ZIP;
    }
    else {
      return MediaType.APPLICATION_ALL;
    }
  }
}

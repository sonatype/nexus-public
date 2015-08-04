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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.sonatype.nexus.yum.Yum;

import org.restlet.data.Request;
import org.restlet.resource.ResourceException;

import static org.apache.commons.lang.StringUtils.join;
import static org.restlet.data.Status.CLIENT_ERROR_BAD_REQUEST;
import static org.restlet.data.Status.SERVER_ERROR_INTERNAL;

/**
 * @since yum 3.0
 */
public class UrlPathParser
{

  private static final int FIRST_PARAM = 0;

  private static final int SECOND_PARAM = 1;

  private final String segmentPrefix;

  private final int segmentsAfterPrefix;

  public UrlPathParser(String yumRepoPrefixName, int segmentsAfterPrefix) {
    this.segmentPrefix = yumRepoPrefixName;
    this.segmentsAfterPrefix = segmentsAfterPrefix;
  }

  public UrlPathInterpretation parse(Request request)
      throws ResourceException
  {
    final List<String> segments = request.getResourceRef().getSegments();
    final int yumIndex = segments.indexOf(segmentPrefix);
    if (yumIndex < 0) {
      throw new ResourceException(SERVER_ERROR_INTERNAL, "Prefix '" + segmentPrefix + "' not found.");
    }

    final URL repoUrl;
    try {
      repoUrl =
          new URL(request.getResourceRef().getHostIdentifier() + "/"
              + join(segments.subList(0, yumIndex + segmentsAfterPrefix + 1), "/"));
    }
    catch (MalformedURLException e) {
      throw new ResourceException(SERVER_ERROR_INTERNAL, e);
    }

    final List<String> lastSegments = segments.subList(yumIndex + segmentsAfterPrefix + 1, segments.size());

    if (lastSegments.contains("..")) {
      throw new ResourceException(CLIENT_ERROR_BAD_REQUEST, "Requests with '..' are not allowed");
    }

    if (lastSegments.isEmpty()) {
      return new UrlPathInterpretation(repoUrl, null, true, true, pathToIndex(segments));
    }

    if (lastSegments.get(FIRST_PARAM).length() == 0) {
      return new UrlPathInterpretation(repoUrl, null, true);
    }

    if (Yum.PATH_OF_REPODATA.equals(lastSegments.get(FIRST_PARAM))) {
      if (lastSegments.size() == 1) {
        return new UrlPathInterpretation(
            repoUrl, Yum.PATH_OF_REPODATA, true, true, pathToIndex(segments)
        );
      }

      if (lastSegments.get(SECOND_PARAM).length() == 0) {
        return new UrlPathInterpretation(repoUrl, Yum.PATH_OF_REPODATA, true);
      }

      if (lastSegments.size() == 2) {
        return new UrlPathInterpretation(repoUrl, lastSegments.get(FIRST_PARAM) + "/"
            + lastSegments.get(SECOND_PARAM), false);
      }
    }

    return new UrlPathInterpretation(repoUrl, join(lastSegments, "/"), false, false, null);
  }

  private String pathToIndex(List<String> segments) {
    return "/" + join(segments, "/") + "/";
  }

}

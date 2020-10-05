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
package org.sonatype.nexus.repository.rest.api;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import org.sonatype.nexus.rest.WebApplicationMessageException;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;

/**
 * Extracts the format and type from the {@link UriInfo} path for use in resource handlers
 *
 * @since 3.28
 */
public class FormatAndType
{
  private static final Pattern GET_REPO_PATH = Pattern.compile(".+/(.+)/(hosted|group|proxy)/(?!/).+");

  private String format;

  private String type;

  @Context
  public void setUriInfo(final UriInfo uriInfo) {
    Matcher matcher = GET_REPO_PATH.matcher(uriInfo.getPath());
    if (matcher.matches()) {
      this.format = "maven".equals(matcher.group(1)) ? "maven2" : matcher.group(1);
      this.type = matcher.group(2);
    } else {
      throw new WebApplicationMessageException(NOT_FOUND, "\"Repository not found\"", APPLICATION_JSON);
    }
  }

  public String type() {
    return type;
  }

  public String format() {
    return format;
  }
}

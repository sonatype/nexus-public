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

package org.sonatype.nexus.servlet;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * @since 3.11
 */
@Named
@Singleton
public class XFrameOptions
    implements Serializable
{
  private static final long serialVersionUID = 5092514762222572451L;

  private static final String COPYRIGHT_PATH = "/COPYRIGHT.html";

  private static final String HEALTHCHECK_PATH = "/static/healthcheck-tos.html";

  private static final String OC_LICENSE_PATH = "/OC-LICENSE.html";

  private static final String PRO_LICENSE_PATH = "/PRO-LICENSE.html";

  private static final String CE_LICENSE_PATH = "/CE-LICENSE.html";

  private static final String SWAGGER_UI = "/swagger-ui/";

  public static final String DENY = "DENY";

  public static final String SAME_ORIGIN = "SAMEORIGIN";

  private final boolean defaultDeny;

  private final Set<String> frameablePaths;

  @Inject
  public XFrameOptions(@Named("${nexus.http.denyframe.enabled:-true}") final boolean defaultDeny) {
    this.defaultDeny = defaultDeny;
    frameablePaths = new HashSet<>();
    frameablePaths.add(COPYRIGHT_PATH);
    frameablePaths.add(HEALTHCHECK_PATH);
    frameablePaths.add(OC_LICENSE_PATH);
    frameablePaths.add(PRO_LICENSE_PATH);
    frameablePaths.add(CE_LICENSE_PATH);
    frameablePaths.add(SWAGGER_UI);
  }

  public String getValueForPath(final String path) {
    if (!defaultDeny || frameablePaths.contains(path)) {
      return SAME_ORIGIN;
    }
    return DENY;
  }
}

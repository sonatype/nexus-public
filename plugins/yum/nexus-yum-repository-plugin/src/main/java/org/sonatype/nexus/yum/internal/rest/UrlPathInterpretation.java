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

import java.net.URL;

/**
 * @since yum 3.0
 */
public class UrlPathInterpretation
{

  private final URL repoUrl;

  private final String path;

  private final boolean index;

  private final boolean redirect;

  private final String redirectUri;

  public UrlPathInterpretation(URL repoUrl, String path, boolean index) {
    this(repoUrl, path, index, false, null);
  }

  public UrlPathInterpretation(URL repoUrl, String path, boolean index, boolean redirect, String redirectUri) {
    this.repoUrl = repoUrl;
    this.path = path;
    this.index = index;
    this.redirect = redirect;
    this.redirectUri = redirectUri;
  }

  public String getPath() {
    return path;
  }

  public boolean isIndex() {
    return index;
  }

  public URL getRepositoryUrl() {
    return repoUrl;
  }

  public boolean isRedirect() {
    return redirect;
  }

  public String getRedirectUri() {
    return redirectUri;
  }

}

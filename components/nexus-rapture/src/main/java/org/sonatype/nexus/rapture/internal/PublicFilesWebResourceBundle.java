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
package org.sonatype.nexus.rapture.internal;

import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.mime.MimeSupport;
import org.sonatype.nexus.webresources.UrlWebResource;
import org.sonatype.nexus.webresources.WebResource;
import org.sonatype.nexus.webresources.WebResourceBundle;

import static com.google.common.base.Preconditions.checkNotNull;

@Named
@Singleton
public class PublicFilesWebResourceBundle
    extends ComponentSupport
    implements WebResourceBundle
{
  private static final List<String> FILES =
      Arrays.asList("apple-touch-icon.png", "browserconfig.xml", "favicon.ico", "favicon-16x16.png",
          "favicon-32x32.png", "mstile-144x144.png", "mstile-150x150.png", "mstile-310x310.png", "mstile-70x70.png",
          "OSS-LICENSE.html", "PRO-LICENSE.html", "robots.txt", "safari-pinned-tab.svg");

  private final MimeSupport mimeSupport;

  @Inject
  public PublicFilesWebResourceBundle(final MimeSupport mimeSupport) {
    this.mimeSupport = checkNotNull(mimeSupport);
  }

  @Override
  public List<WebResource> getResources() {
    return FILES.stream()
      .map(this::toWebResource)
      .filter(Objects::nonNull)
      .collect(Collectors.toList());
  }

  private WebResource toWebResource(final String filename) {
    URL url = PublicFilesWebResourceBundle.class.getResource("/static/public/" + filename);
    if (url == null) {
      return null;
    }
    return new UrlWebResource(url, '/' + filename, getContentType(filename));
  }

  private String getContentType(final String filename) {
    return mimeSupport.guessMimeTypeFromPath(filename);
  }
}

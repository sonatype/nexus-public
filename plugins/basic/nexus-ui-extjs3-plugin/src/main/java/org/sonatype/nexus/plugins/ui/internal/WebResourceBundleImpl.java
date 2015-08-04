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
package org.sonatype.nexus.plugins.ui.internal;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.plugin.support.UrlWebResource;
import org.sonatype.nexus.web.WebResource;
import org.sonatype.nexus.web.WebResourceBundle;

// FIXME: Sort out why, if at all, component is needed.  Apparently only used to "rename" some resources.

@Named
public class WebResourceBundleImpl
    implements WebResourceBundle
{
  private final BuildNumberService buildNumberService;

  @Inject
  public WebResourceBundleImpl(final BuildNumberService buildNumberService) {
    this.buildNumberService = buildNumberService;
  }

  @Override
  public List<WebResource> getResources() {
    String prefix = buildNumberService.getBuildNumber();

    List<WebResource> result = new ArrayList<WebResource>();

    result.add(new UrlWebResource(getClass().getResource("/static/js/nexus-ui-extjs3-plugin-all.js"),
        "/js/" + prefix + "/sonatype-all.js", "text/javascript"));

    result.add(new UrlWebResource(getClass().getResource("/static/js/nx-all.js"),
        "/js/" + prefix + "/nx-all.js", "text/javascript"));

    result.add(new UrlWebResource(getClass().getResource("/static/js/sonatype-lib.js"),
        "/js/" + prefix + "/sonatype-lib.js", "text/javascript"));

    result.add(new UrlWebResource(getClass().getResource("/static/css/nexus-ui-extjs3-plugin-all.css"),
        "/style/" + prefix + "/sonatype-all.css", "text/css"));

    result.add(new UrlWebResource(getClass().getResource("/static/css/nexus-ui-extjs3-plugin-all.css"),
        "/style/sonatype-all.css", "text/css"));

    return result;
  }
}

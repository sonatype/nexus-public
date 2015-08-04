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
package org.sonatype.nexus.rest;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.configuration.application.GlobalRestApiSettings;
import org.sonatype.plexus.rest.DefaultReferenceFactory;

import org.apache.commons.lang.StringUtils;
import org.restlet.data.Reference;
import org.restlet.data.Request;

import static com.google.common.base.Preconditions.checkState;

@Named
@Singleton
public class NexusReferenceFactory
    extends DefaultReferenceFactory
{
  private final GlobalRestApiSettings globalRestApiSettings;

  @Inject
  public NexusReferenceFactory(final GlobalRestApiSettings globalRestApiSettings) {
    this.globalRestApiSettings = globalRestApiSettings;
  }

  @Override
  public Reference getContextRoot(Request request) {
    Reference result = null;

    if (globalRestApiSettings.isEnabled() && globalRestApiSettings.isForceBaseUrl()
        && StringUtils.isNotEmpty(globalRestApiSettings.getBaseUrl())) {
      result = new Reference(globalRestApiSettings.getBaseUrl());
    }
    else {
      // TODO: NEXUS-6045 hack, Restlet app root is now "/service/local", so going up 2 levels!
      result = request.getRootRef().getParentRef().getParentRef();
    }

    // fix for when restlet is at webapp root
    if (StringUtils.isEmpty(result.getPath())) {
      result.setPath("/");
    }

    return result;
  }

  /**
   * Returns the resource-path for the given request, does not include "service/local" prefix.
   * Should never start with "/".
   */
  private String getResourcePath(final Request request) {
    // do not use getContentRoot() here, we do not want force base-url messing up resource path extraction
    String rootUri = request.getRootRef().getTargetRef().toString();
    if (!rootUri.endsWith("/")) {
      rootUri += "/";
    }

    String resourceUri = request.getResourceRef().getTargetRef().toString();
    String path = resourceUri.substring(rootUri.length());
    if (path.startsWith("/")) {
      path = path.substring(1, path.length());
    }

    // in a runtime instance the root-ref will include service/local since restlet is no longer mounted at root
    // so this should be stripped off as part of substring above
    checkState(!path.startsWith("service/local"));

    return path;
  }

  /**
   * Override to cope with changing base-url when forced and service/local location wrt to root ref.
   */
  @Override
  public Reference createThisReference(final Request request) {
    // normalized root-ref which respects force base-url
    Reference rootRef = getContextRoot(request);

    // normalized reference to resource relative to root-ref
    Reference thisRef = new Reference(rootRef, "service/local/" + getResourcePath(request));

    return updateBaseRefPath(thisRef);
  }
}

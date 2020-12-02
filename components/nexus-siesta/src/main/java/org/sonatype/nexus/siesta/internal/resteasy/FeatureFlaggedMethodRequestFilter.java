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
package org.sonatype.nexus.siesta.internal.resteasy;

import java.lang.reflect.Method;
import java.io.IOException;

import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.rest.FeatureFlaggedMethod;

import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static org.sonatype.nexus.common.property.SystemPropertiesHelper.getBoolean;

@FeatureFlaggedMethod
public class FeatureFlaggedMethodRequestFilter
    extends ComponentSupport
    implements ContainerRequestFilter
{
  @Context
  private ResourceInfo resourceInfo;

  @Override
  public void filter(final ContainerRequestContext requestContext) throws IOException {
    Method method = resourceInfo.getResourceMethod();
    FeatureFlaggedMethod annotation = method.getAnnotation(FeatureFlaggedMethod.class);
    String featureFlagName = annotation.name();
    if (method != null) {
      boolean enabled = getBoolean(featureFlagName, annotation.enabledByDefault());
      if (!enabled) {
        log.debug("Filtering method {} flagged by feature flag {}", method.getName(), featureFlagName);
        requestContext.abortWith(Response.status(FORBIDDEN).entity("feature is disabled").build());
      }
    }
  }
}

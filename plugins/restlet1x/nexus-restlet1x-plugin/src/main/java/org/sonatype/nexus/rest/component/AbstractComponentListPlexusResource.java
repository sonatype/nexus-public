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
package org.sonatype.nexus.rest.component;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.rest.AbstractNexusPlexusResource;
import org.sonatype.nexus.rest.model.PlexusComponentListResource;
import org.sonatype.nexus.rest.model.PlexusComponentListResourceResponse;

import com.google.inject.Key;
import org.apache.commons.lang.StringUtils;
import org.eclipse.sisu.BeanEntry;
import org.eclipse.sisu.inject.BeanLocator;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;

public abstract class AbstractComponentListPlexusResource
    extends AbstractNexusPlexusResource
{
  public static final String ROLE_ID = "role";

  private BeanLocator beanLocator;

  private ClassLoader uberClassLoader;

  @Inject
  public void setContainer(BeanLocator beanLocator, @Named("nexus-uber") ClassLoader uberClassLoader) {
    this.beanLocator = beanLocator;
    this.uberClassLoader = uberClassLoader;
  }

  @Override
  public Object getPayloadInstance() {
    return null;
  }

  protected String getRole(Request request) {
    return request.getAttributes().get(ROLE_ID).toString();
  }

  @Override
  public Object get(Context context, Request request, Response response, Variant variant) throws ResourceException {
    PlexusComponentListResourceResponse result = new PlexusComponentListResourceResponse();

    // get role from request
    String role = getRole(request);

    try {
      Key<?> roleKey = Key.get(uberClassLoader.loadClass(role), Named.class);
      Iterable<? extends BeanEntry<Named, ?>> components = beanLocator.locate(roleKey);

      if (!components.iterator().hasNext()) {
        throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND);
      }

      for (BeanEntry<Named, ?> entry : components) {
        PlexusComponentListResource resource = new PlexusComponentListResource();

        String hint = entry.getKey().value();
        String description = entry.getDescription();

        resource.setRoleHint(hint);
        resource.setDescription(StringUtils.isNotEmpty(description) ? description : hint);

        // add it to the collection
        result.addData(resource);
      }
    }
    catch (ClassNotFoundException | LinkageError e) {
      if (this.getLogger().isDebugEnabled()) {
        getLogger().debug("Unable to look up plexus component with role '" + role + "'.", e);
      }
      throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND);
    }

    return result;
  }
}

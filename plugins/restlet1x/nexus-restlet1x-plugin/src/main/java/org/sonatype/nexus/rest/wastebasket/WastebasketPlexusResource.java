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
package org.sonatype.nexus.rest.wastebasket;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.sonatype.nexus.proxy.wastebasket.Wastebasket;
import org.sonatype.nexus.rest.AbstractNexusPlexusResource;
import org.sonatype.nexus.rest.model.WastebasketResource;
import org.sonatype.nexus.rest.model.WastebasketResourceResponse;
import org.sonatype.nexus.scheduling.NexusScheduler;
import org.sonatype.nexus.tasks.EmptyTrashTask;
import org.sonatype.plexus.rest.resource.PathProtectionDescriptor;

import org.codehaus.enunciate.contract.jaxrs.ResourceMethodSignature;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;

/**
 * The Wastebasket resource handler. It returns the status of the wastebasket, and purges it.
 *
 * @author cstamas
 * @author tstevens
 */
@Named
@Singleton
@Path(WastebasketPlexusResource.RESOURCE_URI)
@Produces({"application/xml", "application/json"})
public class WastebasketPlexusResource
    extends AbstractNexusPlexusResource
{
  public static final String RESOURCE_URI = "/wastebasket";

  private final Wastebasket wastebasket;

  private final NexusScheduler nexusScheduler;

  @Inject
  public WastebasketPlexusResource(final Wastebasket wastebasket, final NexusScheduler nexusScheduler) {
    this.wastebasket = wastebasket;
    this.nexusScheduler = nexusScheduler;
  }

  @Override
  public Object getPayloadInstance() {
    return null;
  }

  @Override
  public String getResourceUri() {
    return RESOURCE_URI;
  }

  @Override
  public PathProtectionDescriptor getResourceProtection() {
    return new PathProtectionDescriptor("/wastebasket**", "authcBasic,perms[nexus:wastebasket]");
  }

  /**
   * Get details about the contents of the wastebasket.
   */
  @Override
  @GET
  @ResourceMethodSignature(output = WastebasketResourceResponse.class)
  public Object get(Context context, Request request, Response response, Variant variant)
      throws ResourceException
  {
    WastebasketResourceResponse result = new WastebasketResourceResponse();

    WastebasketResource resource = new WastebasketResource();

    resource.setItemCount(-1);

    Long totalSize = wastebasket.getTotalSize();

    if (totalSize != null) {
      resource.setSize(totalSize);
    }
    else {
      resource.setSize(-1);
    }

    result.setData(resource);

    return result;
  }

  /**
   * Empty the wastebasket.
   */
  @Override
  @DELETE
  @ResourceMethodSignature()
  public void delete(Context context, Request request, Response response)
      throws ResourceException
  {
    EmptyTrashTask task = nexusScheduler.createTaskInstance(EmptyTrashTask.class);

    nexusScheduler.submit("Internal", task);

    response.setStatus(Status.SUCCESS_NO_CONTENT);
  }

  @Override
  public boolean isModifiable() {
    return true;
  }

}

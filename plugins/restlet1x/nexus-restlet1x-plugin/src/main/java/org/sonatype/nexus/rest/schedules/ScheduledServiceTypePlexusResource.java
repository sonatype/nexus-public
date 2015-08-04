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
package org.sonatype.nexus.rest.schedules;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.sonatype.nexus.rest.model.FormFieldResource;
import org.sonatype.nexus.rest.model.ScheduledServiceTypeResource;
import org.sonatype.nexus.rest.model.ScheduledServiceTypeResourceResponse;
import org.sonatype.nexus.tasks.descriptors.ScheduledTaskDescriptor;
import org.sonatype.plexus.rest.resource.PathProtectionDescriptor;

import org.codehaus.enunciate.contract.jaxrs.ResourceMethodSignature;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;

/**
 * @author tstevens
 */
@Named
@Singleton
@Path(ScheduledServiceTypePlexusResource.RESOURCE_URI)
@Produces({"application/xml", "application/json"})
public class ScheduledServiceTypePlexusResource
    extends AbstractScheduledServicePlexusResource
{
  public static final String RESOURCE_URI = "/schedule_types";

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
    return new PathProtectionDescriptor(getResourceUri(), "authcBasic,perms[nexus:tasktypes]");
  }

  /**
   * Get the list of scheduled service types available in nexus. And all of the configuration parameters available
   * for
   * each type.
   */
  @Override
  @GET
  @ResourceMethodSignature(output = ScheduledServiceTypeResourceResponse.class)
  public Object get(Context context, Request request, Response response, Variant variant)
      throws ResourceException
  {
    ScheduledServiceTypeResourceResponse result = new ScheduledServiceTypeResourceResponse();

    List<ScheduledTaskDescriptor> taskDescriptors = getNexusConfiguration().listScheduledTaskDescriptors();

    for (ScheduledTaskDescriptor taskDescriptor : taskDescriptors) {
      if (taskDescriptor.isExposed()) {
        ScheduledServiceTypeResource type = new ScheduledServiceTypeResource();
        type.setId(taskDescriptor.getId());
        type.setName(taskDescriptor.getName());

        type.setFormFields((List<FormFieldResource>) formFieldToDTO(taskDescriptor.formFields(),
            FormFieldResource.class));

        result.addData(type);
      }
    }

    sortTaskType(result.getData());

    return result;
  }

  private void sortTaskType(List<ScheduledServiceTypeResource> types) {
    Collections.sort(types, new Comparator<ScheduledServiceTypeResource>()
    {
      public int compare(ScheduledServiceTypeResource t1, ScheduledServiceTypeResource t2) {
        return (t1.getName()).compareTo(t2.getName());
      }
    });
  }

}

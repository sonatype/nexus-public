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
package org.sonatype.nexus.rest.status;

import java.io.PrintWriter;
import java.io.StringWriter;

import javax.enterprise.inject.Typed;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.sonatype.nexus.ApplicationStatusSource;
import org.sonatype.nexus.SystemStatus;
import org.sonatype.nexus.rest.model.NexusAuthenticationClientPermissions;
import org.sonatype.nexus.rest.model.StatusResource;
import org.sonatype.nexus.rest.model.StatusResourceResponse;
import org.sonatype.nexus.web.BaseUrlHolder;
import org.sonatype.plexus.rest.resource.ManagedPlexusResource;
import org.sonatype.plexus.rest.resource.PathProtectionDescriptor;
import org.sonatype.security.rest.authentication.AbstractUIPermissionCalculatingPlexusResource;
import org.sonatype.security.rest.model.AuthenticationClientPermissions;

import com.yammer.metrics.annotation.Timed;
import org.codehaus.enunciate.contract.jaxrs.ResourceMethodSignature;
import org.restlet.Context;
import org.restlet.data.Form;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;

@Named("StatusPlexusResource")
@Singleton
@Typed(ManagedPlexusResource.class)
@Path(StatusPlexusResource.RESOURCE_URI)
@Produces({"application/xml", "application/json"})
public class StatusPlexusResource
    extends AbstractUIPermissionCalculatingPlexusResource
    implements ManagedPlexusResource
{
  public static final String RESOURCE_URI = "/status";

  private final ApplicationStatusSource applicationStatusSource;

  @Inject
  public StatusPlexusResource(final ApplicationStatusSource applicationStatusSource) {
    this.applicationStatusSource = applicationStatusSource;
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
    return new PathProtectionDescriptor(getResourceUri(), "authcBasic,perms[nexus:status]");
  }

  /**
   * Returns the status of the Nexus server.
   *
   * @param perms If query parameter with this name present (without or with any value, does not matter, it is only
   *              checked for presence), this resource will emit the user permissions too.
   */
  @Timed
  @Override
  @GET
  @ResourceMethodSignature(queryParams = {@QueryParam("perms")}, output = StatusResourceResponse.class)
  public Object get(Context context, Request request, Response response, Variant variant)
      throws ResourceException
  {
    final SystemStatus status = applicationStatusSource.getSystemStatus();

    final StatusResource resource = new StatusResource();

    resource.setAppName(status.getAppName());

    resource.setFormattedAppName(status.getFormattedAppName());

    resource.setVersion(status.getVersion());

    resource.setApiVersion(status.getApiVersion());

    resource.setEditionLong(status.getEditionLong());

    resource.setEditionShort(status.getEditionShort());

    resource.setAttributionsURL(status.getAttributionsURL());

    resource.setPurchaseURL(status.getPurchaseURL());

    resource.setUserLicenseURL(status.getUserLicenseURL());

    resource.setState(status.getState().toString());

    resource.setInitializedAt(status.getInitializedAt());

    resource.setStartedAt(status.getStartedAt());

    resource.setLastConfigChange(status.getLastConfigChange());

    resource.setFirstStart(status.isFirstStart());

    resource.setInstanceUpgraded(status.isInstanceUpgraded());

    resource.setConfigurationUpgraded(status.isConfigurationUpgraded());

    resource.setErrorCause(spit(status.getErrorCause()));

    // if ( status.getConfigurationValidationResponse() != null )
    // {
    // resource.setConfigurationValidationResponse( new StatusConfigurationValidationResponse() );
    //
    // resource.getConfigurationValidationResponse().setValid(
    // status.getConfigurationValidationResponse().isValid() );
    //
    // resource.getConfigurationValidationResponse().setModified(
    // status.getConfigurationValidationResponse().isModified() );
    //
    // for ( ValidationMessage msg : status.getConfigurationValidationResponse().getValidationErrors() )
    // {
    // resource.getConfigurationValidationResponse().addValidationError( msg.toString() );
    // }
    // for ( ValidationMessage msg : status.getConfigurationValidationResponse().getValidationWarnings() )
    // {
    // resource.getConfigurationValidationResponse().addValidationWarning( msg.toString() );
    // }
    // }

    final Form form = request.getResourceRef().getQueryAsForm();
    if (form.getFirst("perms") != null) {
      resource.setClientPermissions(getClientPermissions(request));
    }

    resource.setBaseUrl(BaseUrlHolder.get());

    resource.setLicenseInstalled(status.isLicenseInstalled());

    resource.setLicenseExpired(status.isLicenseExpired());

    resource.setTrialLicense(status.isTrialLicense());

    StatusResourceResponse result = new StatusResourceResponse();

    result.setData(resource);

    return result;
  }

  private NexusAuthenticationClientPermissions getClientPermissions(Request request) throws ResourceException {
    AuthenticationClientPermissions originalClientPermissions = getClientPermissionsForCurrentUser(request);

    // TODO: this is a modello work around,
    // the SystemStatus could not include a field of type AuthenticationClientPermissions
    // because it is in a different model, but I can extend that class... and include it.

    NexusAuthenticationClientPermissions clientPermissions = new NexusAuthenticationClientPermissions();
    clientPermissions.setLoggedIn(originalClientPermissions.isLoggedIn());
    clientPermissions.setLoggedInUsername(originalClientPermissions.getLoggedInUsername());
    clientPermissions.setLoggedInUserSource(originalClientPermissions.getLoggedInUserSource());
    clientPermissions.setLoggedInUserSource(originalClientPermissions.getLoggedInUserSource());
    clientPermissions.setPermissions(originalClientPermissions.getPermissions());

    return clientPermissions;
  }

  private String spit(Throwable t) {
    if (t == null) {
      return null;
    }
    else {
      StringWriter sw = new StringWriter();

      t.printStackTrace(new PrintWriter(sw));

      return sw.toString();
    }

  }
}

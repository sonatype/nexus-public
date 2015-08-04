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
package org.sonatype.nexus.security.ldap.realms.test.api;

import java.net.MalformedURLException;

import javax.enterprise.inject.Typed;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.sonatype.nexus.security.ldap.realms.api.AbstractLdapRealmPlexusResource;
import org.sonatype.nexus.security.ldap.realms.test.api.dto.LdapAuthenticationTestRequest;
import org.sonatype.plexus.rest.resource.PathProtectionDescriptor;
import org.sonatype.plexus.rest.resource.PlexusResource;
import org.sonatype.security.ldap.dao.LdapConnectionTester;
import org.sonatype.security.ldap.realms.DefaultLdapContextFactory;
import org.sonatype.security.ldap.realms.persist.ConfigurationValidator;
import org.sonatype.security.ldap.realms.persist.ValidationResponse;
import org.sonatype.security.ldap.realms.persist.model.CConnectionInfo;

import org.codehaus.enunciate.contract.jaxrs.ResourceMethodSignature;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;

/**
 * Resource for connection info validation and testing.
 */
@Path("/ldap/test_auth")
@Produces({"application/xml", "application/json"})
@Consumes({"application/xml", "application/json"})
@Singleton
@Named("LdapTestAuthenticationPlexusResource")
@Typed(PlexusResource.class)
public class LdapTestAuthenticationPlexusResource
    extends AbstractLdapRealmPlexusResource
{

  @Inject
  private LdapConnectionTester ldapConnectionTester;

  @Inject
  private ConfigurationValidator configurationValidator;

  public LdapTestAuthenticationPlexusResource() {
    this.setModifiable(true);
  }

  @Override
  public Object getPayloadInstance() {
    return new LdapAuthenticationTestRequest();
  }

  @Override
  public PathProtectionDescriptor getResourceProtection() {
    return new PathProtectionDescriptor(getResourceUri(), "authcBasic,perms[nexus:ldaptestauth]");
  }

  @Override
  public String getResourceUri() {
    return "/ldap/test_auth";
  }

  /**
   * Validates connection info and performs a connection test with it. The response's HTTP Status code in case of
   * success is 204 Success No Content. In case of failure, 400 Bad request.
   */
  @Override
  @PUT
  @ResourceMethodSignature(input = LdapAuthenticationTestRequest.class)
  public Object put(Context context, Request request, Response response, Object payload)
      throws ResourceException
  {
    LdapAuthenticationTestRequest authRequest = (LdapAuthenticationTestRequest) payload;

    CConnectionInfo connectionInfo = this.restToLdapModel(authRequest.getData());

    ValidationResponse validationResponse =
        this.configurationValidator.validateConnectionInfo(null, connectionInfo);
    // sets the status and throws an exception if the validation was junk.
    // if the validation was ok, then nothing really happens
    this.handleValidationResponse(validationResponse);

    try {
      DefaultLdapContextFactory ldapContextFactory = this.buildDefaultLdapContextFactory(connectionInfo);
      ldapConnectionTester.testConnection(ldapContextFactory);
    }
    catch (MalformedURLException e) {
      // should NEVER hit this
      this.getLogger().warn("Validation of URL was successful, but failed after validation.", e);
      throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, e);
    }
    catch (Exception e) {
      this.getLogger().debug("Failed to connect to Ldap Server.", e);
      throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Failed to connect to Ldap Server: "
          + e.getMessage(), e);
    }

    response.setStatus(Status.SUCCESS_NO_CONTENT);
    return null;
  }

}

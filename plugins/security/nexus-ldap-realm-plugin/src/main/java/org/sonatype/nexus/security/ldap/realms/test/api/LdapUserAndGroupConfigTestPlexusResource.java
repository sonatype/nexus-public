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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.enterprise.inject.Typed;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.naming.NamingException;
import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.sonatype.nexus.security.ldap.realms.api.AbstractLdapRealmPlexusResource;
import org.sonatype.nexus.security.ldap.realms.api.dto.LdapUserAndGroupConfigurationDTO;
import org.sonatype.nexus.security.ldap.realms.api.dto.LdapUserListResponse;
import org.sonatype.nexus.security.ldap.realms.api.dto.LdapUserResponseDTO;
import org.sonatype.nexus.security.ldap.realms.test.api.dto.LdapUserAndGroupConfigTestRequest;
import org.sonatype.nexus.security.ldap.realms.test.api.dto.LdapUserAndGroupConfigTestRequestDTO;
import org.sonatype.plexus.rest.resource.PathProtectionDescriptor;
import org.sonatype.plexus.rest.resource.PlexusResource;
import org.sonatype.security.ldap.dao.LdapAuthConfiguration;
import org.sonatype.security.ldap.dao.LdapConnectionTester;
import org.sonatype.security.ldap.dao.LdapDAOException;
import org.sonatype.security.ldap.dao.LdapUser;
import org.sonatype.security.ldap.realms.LdapManager;
import org.sonatype.security.ldap.realms.persist.ConfigurationValidator;
import org.sonatype.security.ldap.realms.persist.UsersGroupAuthTestLdapConfiguration;
import org.sonatype.security.ldap.realms.persist.ValidationResponse;
import org.sonatype.security.ldap.realms.persist.model.CConnectionInfo;

import org.codehaus.enunciate.contract.jaxrs.ResourceMethodSignature;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;

/**
 * Resource for user and group mapping testing.
 */
@Path("/ldap/test_user_conf")
@Produces({"application/xml", "application/json"})
@Consumes({"application/xml", "application/json"})
@Singleton
@Named("LdapUserAndGroupConfigTestPlexusResource")
@Typed(PlexusResource.class)
public class LdapUserAndGroupConfigTestPlexusResource
    extends AbstractLdapRealmPlexusResource
{
  @Inject
  private LdapConnectionTester ldapConnectionTester;

  @Inject
  private ConfigurationValidator configurationValidator;
  
  @Inject
  @Named(UsersGroupAuthTestLdapConfiguration.NAME)
  private Provider<UsersGroupAuthTestLdapConfiguration> usersGroupAuthTestLdapConfigurationProvider;
  
  @Inject
  @Named(TestLdapManager.NAME)
  private Provider<TestLdapManager> testLdapManagerProvider;


  public LdapUserAndGroupConfigTestPlexusResource() {
    this.setModifiable(true);
    this.setReadable(false);
  }

  @Override
  public Object getPayloadInstance() {
    return new LdapUserAndGroupConfigTestRequest();
  }

  @Override
  public PathProtectionDescriptor getResourceProtection() {
    return new PathProtectionDescriptor(getResourceUri(), "authcBasic,perms[nexus:ldaptestuserconf]");
  }

  @Override
  public String getResourceUri() {
    return "/ldap/test_user_conf";
  }

  /**
   * Validates and performs test connection using the supplied user and group mapping. Returns a list of resulting
   * mapping.
   */
  @Override
  @PUT
  @ResourceMethodSignature(input = LdapUserAndGroupConfigTestRequest.class, output = LdapUserListResponse.class)
  public Object put(Context context, Request request, Response response, Object payload)
      throws ResourceException
  {

    LdapUserAndGroupConfigTestRequest configResponse = (LdapUserAndGroupConfigTestRequest) payload;
    LdapUserAndGroupConfigTestRequestDTO dto = configResponse.getData();

    ValidationResponse validationResponse =
        this.configurationValidator.validateConnectionInfo(null, this.getConnectionInfo(dto));
    // sets the status and throws an exception if the validation was junk.
    // if the validation was ok, then nothing really happens
    this.handleValidationResponse(validationResponse);

    // do validation before we try to connect
    validationResponse =
        this.configurationValidator.validateUserAndGroupAuthConfiguration(null, this.restToLdapModel(dto));
    this.handleValidationResponse(validationResponse);

    LdapUserListResponse result = new LdapUserListResponse();
    try {
      // FIXME: move this to the UI.
      int limit = (dto.getUserLimitCount() != 0) ? dto.getUserLimitCount() : 20;

      result.setLdapUserRoleMappings(this.getPopulatedDTOs(this.convertToAuthConfig(dto),
          this.getConnectionInfo(dto), limit));
    }
    catch (Exception e) {
      throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "LDAP Realm is not configured correctly: "
          + e.getMessage(), e);
    }
    return result;
  }

  private CConnectionInfo getConnectionInfo(LdapUserAndGroupConfigTestRequestDTO dto) {
    CConnectionInfo connInfo = new CConnectionInfo();
    connInfo.setAuthScheme(dto.getAuthScheme());
    connInfo.setHost(dto.getHost());
    connInfo.setPort(dto.getPort());
    connInfo.setProtocol(dto.getProtocol());
    connInfo.setSearchBase(dto.getSearchBase());
    connInfo.setSystemUsername(dto.getSystemUsername());
    connInfo.setSystemPassword(dto.getSystemPassword());
    connInfo.setRealm(dto.getRealm());

    return connInfo;
  }

  private LdapAuthConfiguration convertToAuthConfig(LdapUserAndGroupConfigurationDTO dto) {
    LdapAuthConfiguration authConfig = new LdapAuthConfiguration();

    authConfig.setGroupMemberFormat(dto.getGroupMemberFormat());
    authConfig.setGroupObjectClass(dto.getGroupObjectClass());
    authConfig.setGroupBaseDn(dto.getGroupBaseDn());
    authConfig.setGroupIdAttribute(dto.getGroupIdAttribute());
    authConfig.setGroupMemberAttribute(dto.getGroupMemberAttribute());
    authConfig.setUserObjectClass(dto.getUserObjectClass());
    authConfig.setUserBaseDn(dto.getUserBaseDn());
    authConfig.setUserIdAttribute(dto.getUserIdAttribute());
    authConfig.setPasswordAttribute(dto.getUserPasswordAttribute());
    authConfig.setUserRealNameAttribute(dto.getUserRealNameAttribute());
    authConfig.setEmailAddressAttribute(dto.getEmailAddressAttribute());
    authConfig.setLdapGroupsAsRoles(dto.isLdapGroupsAsRoles());
    authConfig.setUserSubtree(dto.isUserSubtree());
    authConfig.setGroupSubtree(dto.isGroupSubtree());
    authConfig.setUserMemberOfAttribute(dto.getUserMemberOfAttribute());
    authConfig.setLdapFilter(dto.getLdapFilter());

    return authConfig;
  }

  protected LdapManager getLdapManager(LdapAuthConfiguration ldapAuthConfiguration, CConnectionInfo connectionInfo)
      throws ResourceException
  {
    // the component we need to replace is nested 2 layers deep, and we need to do this per request
    // which is why I am monkeying around with the container. Its not exactly 'clean'....
    // get the ldapConfig
    final UsersGroupAuthTestLdapConfiguration ldapConfiguration = usersGroupAuthTestLdapConfigurationProvider.get();
    ldapConfiguration.setLdapAuthConfiguration(ldapAuthConfiguration);
    ldapConfiguration.setConnectionInfo(connectionInfo);

    final TestLdapManager ldapManager = testLdapManagerProvider.get();
    ldapManager.setLdapConfiguration(ldapConfiguration);

    return ldapManager;
  }

  private List<LdapUserResponseDTO> getPopulatedDTOs(LdapAuthConfiguration ldapAuthConfiguration,
                                                     CConnectionInfo cConnectionInfo, int limit)
      throws LdapDAOException, MalformedURLException, NamingException
  {

    List<LdapUserResponseDTO> result = new ArrayList<LdapUserResponseDTO>();
    Collection<LdapUser> sUsers =
        ldapConnectionTester.testUserAndGroupMapping(this.buildDefaultLdapContextFactory(cConnectionInfo),
            ldapAuthConfiguration, limit);

    for (LdapUser ldapUser : sUsers) {
      result.add(getPopulatedUser(ldapUser));
    }
    return result;
  }

  private LdapUserResponseDTO getPopulatedUser(LdapUser ldapUser) {
    LdapUserResponseDTO dto;

    dto = new LdapUserResponseDTO();

    // now set the rest of the props
    dto.setUserId(ldapUser.getUsername());
    dto.setEmail(ldapUser.getEmail());
    dto.setName(ldapUser.getRealName());

    // add the roles
    for (String role : (Set<String>) ldapUser.getMembership()) {
      dto.addRole(role);
    }
    return dto;
  }
}

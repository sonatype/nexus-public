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
package org.sonatype.nexus.security.ldap.realms.api;

import org.sonatype.nexus.NexusLdapTestSupport;
import org.sonatype.nexus.security.ldap.realms.api.dto.LdapUserAndGroupConfigurationDTO;
import org.sonatype.nexus.security.ldap.realms.api.dto.LdapUserAndGroupConfigurationResponse;
import org.sonatype.plexus.rest.resource.PlexusResource;
import org.sonatype.plexus.rest.resource.PlexusResourceException;
import org.sonatype.plexus.rest.resource.error.ErrorResponse;

import org.junit.Assert;
import org.junit.Test;


public class LdapUserGroupConfValidationIT
    extends NexusLdapTestSupport
{

  private PlexusResource getResource()
      throws Exception
  {
    return this.lookup(
        PlexusResource.class,
        "LdapUserAndGroupsConfigurationPlexusResource");
  }

  private LdapUserAndGroupConfigurationDTO getPopulatedDTO() {
    LdapUserAndGroupConfigurationDTO userGroupConf = new LdapUserAndGroupConfigurationDTO();
    userGroupConf.setGroupMemberFormat("uid=${username},ou=people,o=sonatype");
    userGroupConf.setGroupObjectClass("groupOfUniqueNames");
    userGroupConf.setGroupBaseDn("ou=groups");
    userGroupConf.setGroupIdAttribute("cn");
    userGroupConf.setGroupMemberAttribute("uniqueMember");
    userGroupConf.setUserObjectClass("inetOrgPerson");
    userGroupConf.setUserBaseDn("ou=people");
    userGroupConf.setUserIdAttribute("uid");
    userGroupConf.setUserPasswordAttribute("userPassword");
    userGroupConf.setUserRealNameAttribute("cn");
    userGroupConf.setEmailAddressAttribute("mail");
    return userGroupConf;
  }

  @Test
  public void testNoUserBaseDn()
      throws Exception
  {
    PlexusResource resource = getResource();

    LdapUserAndGroupConfigurationResponse response = new LdapUserAndGroupConfigurationResponse();
    LdapUserAndGroupConfigurationDTO userGroupConf = this.getPopulatedDTO();
    response.setData(userGroupConf);

    userGroupConf.setUserBaseDn(null);

    LdapUserAndGroupConfigurationResponse result = (LdapUserAndGroupConfigurationResponse) resource
        .put(null, null, null, response);
    // make sure its null,
    Assert.assertNull(result.getData().getUserBaseDn());
  }

  @Test
  public void testNoGroupBaseDn()
      throws Exception
  {
    PlexusResource resource = getResource();

    LdapUserAndGroupConfigurationResponse response = new LdapUserAndGroupConfigurationResponse();
    LdapUserAndGroupConfigurationDTO userGroupConf = this.getPopulatedDTO();
    response.setData(userGroupConf);

    userGroupConf.setGroupBaseDn(null);

    LdapUserAndGroupConfigurationResponse result = (LdapUserAndGroupConfigurationResponse) resource
        .put(null, null, null, response);
    // make sure its null,
    Assert.assertNull(result.getData().getGroupBaseDn());

  }

  @Test
  public void testNoUserIdAttrib()
      throws Exception
  {
    PlexusResource resource = getResource();

    LdapUserAndGroupConfigurationResponse response = new LdapUserAndGroupConfigurationResponse();
    LdapUserAndGroupConfigurationDTO userGroupConf = this.getPopulatedDTO();
    response.setData(userGroupConf);

    userGroupConf.setUserIdAttribute(null);

    try {
      resource.put(null, null, null, response);
      Assert.fail("Expected PlexusResourceException");
    }
    catch (PlexusResourceException e) {
      ErrorResponse result = (ErrorResponse) e.getResultObject();
      Assert.assertEquals(1, result.getErrors().size());
      Assert.assertTrue(
          "Expected error to have the work 'user', was: " + this.getErrorString(result, 0),
          (this.getErrorString(result, 0).toString().toLowerCase().contains("user")));
    }

  }

  @Test
  public void testMultipleErrors()
      throws Exception
  {
    PlexusResource resource = getResource();

    LdapUserAndGroupConfigurationResponse response = new LdapUserAndGroupConfigurationResponse();
    LdapUserAndGroupConfigurationDTO userGroupConf = this.getPopulatedDTO();
    response.setData(userGroupConf);

    userGroupConf.setUserIdAttribute(null);
    userGroupConf.setEmailAddressAttribute(null);
    try {
      resource.put(null, null, null, response);
      Assert.fail("Expected PlexusResourceException");
    }
    catch (PlexusResourceException e) {
      ErrorResponse result = (ErrorResponse) e.getResultObject();
      Assert.assertEquals(2, result.getErrors().size());
    }
  }

}

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
package org.sonatype.nexus.testsuite.ldap.nxcm355;

import org.sonatype.nexus.security.ldap.realms.api.dto.LdapUserAndGroupConfigurationDTO;
import org.sonatype.nexus.testsuite.ldap.AbstractLdapIntegrationIT;
import org.sonatype.nexus.testsuite.ldap.LdapUserGroupMessageUtil;

import com.thoughtworks.xstream.XStream;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.restlet.data.MediaType;

public class Nxcm355UserSubTreeNotSavedIT
    extends AbstractLdapIntegrationIT
{
  private XStream xstream;

  private MediaType mediaType;

  public Nxcm355UserSubTreeNotSavedIT() {
    super();
  }

  @Before
  public void init() {
    this.xstream = this.getJsonXStream();
    this.mediaType = MediaType.APPLICATION_JSON;
  }

  @Test
  public void saveUserAndGroupConfigWithUserSubtree()
      throws Exception
  {
    LdapUserGroupMessageUtil userGroupUtil = new LdapUserGroupMessageUtil(this.xstream, this.mediaType);

    // configure LDAP user/group config
    LdapUserAndGroupConfigurationDTO userGroupDto = new LdapUserAndGroupConfigurationDTO();

    userGroupDto.setGroupMemberFormat("uid=${username},ou=people,o=sonatype");
    userGroupDto.setGroupObjectClass("groupOfUniqueNames");
    userGroupDto.setGroupBaseDn("ou=groups");
    userGroupDto.setGroupIdAttribute("cn");
    userGroupDto.setGroupMemberAttribute("uniqueMember");
    userGroupDto.setUserObjectClass("inetOrgPerson");
    userGroupDto.setUserBaseDn("ou=people");
    userGroupDto.setUserIdAttribute("uid");
    userGroupDto.setUserPasswordAttribute("userpassword");
    userGroupDto.setUserRealNameAttribute("sn");
    userGroupDto.setEmailAddressAttribute("mail");
    userGroupDto.setLdapGroupsAsRoles(false);
    // the problem was that subtree was getting set to groupSubtree
    userGroupDto.setUserSubtree(true);
    userGroupDto.setGroupSubtree(false);
    userGroupDto.setUserMemberOfAttribute("");

    LdapUserAndGroupConfigurationDTO result = userGroupUtil.updateUserGroupConfig(userGroupDto);
    Assert.assertEquals(result, userGroupDto);

  }
}

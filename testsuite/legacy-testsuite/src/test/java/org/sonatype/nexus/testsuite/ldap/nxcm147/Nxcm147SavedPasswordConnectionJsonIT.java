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
package org.sonatype.nexus.testsuite.ldap.nxcm147;

import org.sonatype.nexus.security.ldap.realms.api.LdapRealmPlexusResourceConst;
import org.sonatype.nexus.security.ldap.realms.api.dto.LdapConnectionInfoDTO;
import org.sonatype.nexus.security.ldap.realms.test.api.dto.LdapUserAndGroupConfigTestRequestDTO;
import org.sonatype.nexus.testsuite.ldap.AbstractLdapIntegrationIT;
import org.sonatype.nexus.testsuite.ldap.LdapConnMessageUtil;
import org.sonatype.nexus.testsuite.ldap.LdapUserGroupMessageUtil;
import org.sonatype.nexus.testsuite.ldap.LdapUsersMessageUtil;

import com.thoughtworks.xstream.XStream;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.restlet.data.MediaType;
import org.restlet.data.Response;
import org.restlet.data.Status;

public class Nxcm147SavedPasswordConnectionJsonIT
    extends AbstractLdapIntegrationIT
{
  private XStream xstream;

  private MediaType mediaType;

  public Nxcm147SavedPasswordConnectionJsonIT() {
    super();
  }

  @Before
  public void init() {
    this.xstream = this.getJsonXStream();
    this.mediaType = MediaType.APPLICATION_JSON;
  }

  @Test
  public void connectionTestWithFakePassword()
      throws Exception
  {

    LdapConnMessageUtil connUtil = new LdapConnMessageUtil(this.xstream, this.mediaType);
    LdapUserGroupMessageUtil userGroupUtil = new LdapUserGroupMessageUtil(this.xstream, this.mediaType);
    LdapUsersMessageUtil userUtil = new LdapUsersMessageUtil(this, this.xstream, this.mediaType);

    // get
    LdapConnectionInfoDTO dto = new LdapConnectionInfoDTO();
    dto.setAuthScheme("simple");
    dto.setHost("localhost");
    dto.setPort(this.getLdapPort());
    dto.setProtocol("ldap");
    dto.setSearchBase("o=sonatype");
    dto.setSystemUsername("uid=admin,ou=system");
    dto.setSystemPassword("secret");
    dto = connUtil.updateConnectionInfo(dto);
    Assert.assertEquals(dto.getSystemPassword(), LdapRealmPlexusResourceConst.FAKE_PASSWORD);

    // test
    Response testResponse = connUtil.sendTestMessage(dto);
    Assert.assertEquals(Status.SUCCESS_NO_CONTENT.getCode(), testResponse.getStatus().getCode());

    // configure LDAP user/group config
    LdapUserAndGroupConfigTestRequestDTO userGroupTestDto = new LdapUserAndGroupConfigTestRequestDTO();
    userGroupTestDto.setAuthScheme(dto.getAuthScheme());
    userGroupTestDto.setHost(dto.getHost());
    userGroupTestDto.setPort(dto.getPort());
    userGroupTestDto.setProtocol(dto.getProtocol());
    userGroupTestDto.setSearchBase(dto.getSearchBase());
    userGroupTestDto.setSystemUsername(dto.getSystemUsername());
    userGroupTestDto.setSystemPassword(dto.getSystemPassword());

    userGroupTestDto.setUserMemberOfAttribute("");

    userGroupTestDto.setGroupMemberFormat("uid=${username},ou=people,o=sonatype");
    userGroupTestDto.setGroupObjectClass("groupOfUniqueNames");
    userGroupTestDto.setGroupBaseDn("ou=groups");
    userGroupTestDto.setGroupIdAttribute("cn");
    userGroupTestDto.setGroupMemberAttribute("uniqueMember");
    userGroupTestDto.setUserObjectClass("inetOrgPerson");
    userGroupTestDto.setUserBaseDn("ou=people");
    userGroupTestDto.setUserIdAttribute("uid");
    userGroupTestDto.setUserPasswordAttribute("userpassword");
    userGroupTestDto.setUserRealNameAttribute("sn");
    userGroupTestDto.setEmailAddressAttribute("mail");
    userGroupTestDto.setLdapGroupsAsRoles(false);
    userGroupTestDto.setUserSubtree(false);
    userGroupTestDto.setGroupSubtree(false);
    userGroupTestDto.setUserMemberOfAttribute("");

    testResponse = userGroupUtil.sendTestMessage(userGroupTestDto);
    Assert.assertEquals(Status.SUCCESS_OK.getCode(), testResponse.getStatus().getCode());

  }
}

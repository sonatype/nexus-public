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

import org.sonatype.nexus.rest.model.XStreamConfigurator;
import org.sonatype.nexus.security.ldap.realms.api.dto.LdapConnectionInfoDTO;
import org.sonatype.nexus.security.ldap.realms.api.dto.LdapConnectionInfoResponse;
import org.sonatype.nexus.security.ldap.realms.api.dto.LdapUserAndGroupConfigurationDTO;
import org.sonatype.nexus.security.ldap.realms.api.dto.LdapUserAndGroupConfigurationResponse;
import org.sonatype.nexus.security.ldap.realms.api.dto.LdapUserListResponse;
import org.sonatype.nexus.security.ldap.realms.api.dto.LdapUserResponseDTO;
import org.sonatype.nexus.security.ldap.realms.test.api.dto.LdapAuthenticationTestRequest;
import org.sonatype.nexus.security.ldap.realms.test.api.dto.LdapUserAndGroupConfigTestRequest;
import org.sonatype.nexus.security.ldap.realms.test.api.dto.LdapUserAndGroupConfigTestRequestDTO;
import org.sonatype.plexus.rest.xstream.json.JsonOrgHierarchicalStreamDriver;
import org.sonatype.plexus.rest.xstream.xml.LookAheadXppDriver;

import com.thoughtworks.xstream.XStream;
import org.codehaus.plexus.util.StringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class MarshalUnmarchalTest
{
  private XStream xstreamXML;

  private XStream xstreamJSON;

  @Before
  public void setUp()
      throws Exception
  {
    xstreamXML = new XStream(new LookAheadXppDriver());
    XStreamConfigurator.configureXStream(xstreamXML);
    LdapXStreamConfigurator.configureXStream(xstreamXML);

    xstreamJSON = new XStream(new JsonOrgHierarchicalStreamDriver());
    XStreamConfigurator.configureXStream(xstreamJSON);
    LdapXStreamConfigurator.configureXStream(xstreamJSON);
  }

  @Test
  public void testLdapConnectionInfoResponse()
      throws Exception
  {
    LdapConnectionInfoResponse resource = new LdapConnectionInfoResponse();
    LdapConnectionInfoDTO dto = new LdapConnectionInfoDTO();

    resource.setData(dto);

    dto.setAuthScheme("authScheme");
    dto.setHost("host");
    dto.setPort(123);
    dto.setProtocol("protocol");
    dto.setRealm("realm");
    dto.setSearchBase("searchBase");
    dto.setSystemPassword("systemPassword");
    dto.setSystemUsername("systemUsername");

    validateXmlHasNoPackageNames(resource);
  }

  @Test
  public void testLdapUserAndGroupConfigurationResponse()
      throws Exception
  {
    LdapUserAndGroupConfigurationResponse resource = new LdapUserAndGroupConfigurationResponse();
    LdapUserAndGroupConfigurationDTO dto = new LdapUserAndGroupConfigurationDTO();

    resource.setData(dto);

    dto.setUserMemberOfAttribute("userMemberOfAttribute");
    dto.setEmailAddressAttribute("emailAddressAttribute");
    dto.setGroupBaseDn("groupBaseDn");
    dto.setGroupIdAttribute("groupIdAttribute");
    dto.setGroupMemberFormat("groupMemberFormat");
    dto.setGroupMemberAttribute("groupMemberAttribute");
    dto.setGroupMemberFormat("groupMemberFormat");
    dto.setGroupObjectClass("groupObjectClass");
    dto.setUserBaseDn("userBaseDn");
    dto.setUserIdAttribute("userIdAttribute");
    dto.setUserObjectClass("userObjectClass");
    dto.setUserPasswordAttribute("userPasswordAttribute");
    dto.setUserRealNameAttribute("userRealNameAttribute");
    dto.setUserSubtree(true);

    validateXmlHasNoPackageNames(resource);
  }

  @Test
  public void testLdapUserListResponse()
      throws Exception
  {
    LdapUserListResponse resource = new LdapUserListResponse();
    LdapUserResponseDTO dto1 = new LdapUserResponseDTO();
    resource.addLdapUserRoleMapping(dto1);

    dto1.setEmail("email1");
    dto1.setName("name1");
    dto1.setUserId("userId1");
    dto1.addRole("role1");
    dto1.addRole("role2");

    LdapUserResponseDTO dto2 = new LdapUserResponseDTO();
    resource.addLdapUserRoleMapping(dto2);

    dto2.setEmail("email2");
    dto2.setName("name2");
    dto2.setUserId("userId2");
    dto2.addRole("role3");
    dto2.addRole("role4");

    validateXmlHasNoPackageNames(resource);
  }

  @Test
  public void testLdapUserAndGroupConfigTestRequest()
      throws Exception
  {
    LdapUserAndGroupConfigTestRequest resource = new LdapUserAndGroupConfigTestRequest();
    LdapUserAndGroupConfigTestRequestDTO dto = new LdapUserAndGroupConfigTestRequestDTO();

    resource.setData(dto);

    dto.setAuthScheme("authScheme");
    dto.setHost("host");
    dto.setPort(123);
    dto.setProtocol("protocol");
    dto.setRealm("realm");
    dto.setSearchBase("searchBase");
    dto.setSystemPassword("systemPassword");
    dto.setSystemUsername("systemUsername");
    dto.setUserMemberOfAttribute("userMemberOfAttribute");
    dto.setEmailAddressAttribute("emailAddressAttribute");
    dto.setGroupBaseDn("groupBaseDn");
    dto.setGroupIdAttribute("groupIdAttribute");
    dto.setGroupMemberFormat("groupMemberFormat");
    dto.setGroupMemberAttribute("groupMemberAttribute");
    dto.setGroupMemberFormat("groupMemberFormat");
    dto.setGroupObjectClass("groupObjectClass");
    dto.setUserBaseDn("userBaseDn");
    dto.setUserIdAttribute("userIdAttribute");
    dto.setUserObjectClass("userObjectClass");
    dto.setUserPasswordAttribute("userPasswordAttribute");
    dto.setUserRealNameAttribute("userRealNameAttribute");
    dto.setUserSubtree(true);

    validateXmlHasNoPackageNames(resource);
  }

  @Test
  public void testLdapUserAndGroupConfigTestRequestWithEscape()
      throws Exception
  {
    LdapUserAndGroupConfigTestRequest resource = new LdapUserAndGroupConfigTestRequest();
    LdapUserAndGroupConfigTestRequestDTO dto = new LdapUserAndGroupConfigTestRequestDTO();

    resource.setData(dto);

    dto.setAuthScheme("authScheme&");
    dto.setHost("host&");
    dto.setPort(123);
    dto.setProtocol("protocol&");
    dto.setRealm("realm&");
    dto.setSearchBase("searchBase&");
    dto.setSystemPassword("systemPassword&");
    dto.setSystemUsername("systemUsername&");
    dto.setUserMemberOfAttribute("userMemberOfAttribute&");
    dto.setEmailAddressAttribute("emailAddressAttribute&");
    dto.setGroupBaseDn("groupBaseDn&");
    dto.setGroupIdAttribute("groupIdAttribute&");
    dto.setGroupMemberFormat("groupMemberFormat&");
    dto.setGroupMemberAttribute("groupMemberAttribute&");
    dto.setGroupMemberFormat("groupMemberFormat&");
    dto.setGroupObjectClass("groupObjectClass&");
    dto.setUserBaseDn("userBaseDn&");
    dto.setUserIdAttribute("userIdAttribute&");
    dto.setUserObjectClass("userObjectClass&");
    dto.setUserPasswordAttribute("userPasswordAttribute&");
    dto.setUserRealNameAttribute("userRealNameAttribute&");
    dto.setLdapFilter("ldapFilter&");
    dto.setUserSubtree(true);

    validateXmlHasNoPackageNames(resource);

    String xml = this.xstreamXML.toXML(resource);
    LdapUserAndGroupConfigTestRequestDTO result =
        ((LdapUserAndGroupConfigTestRequest) this.xstreamXML.fromXML(xml)).getData();

    Assert.assertEquals(result.getSearchBase(), "searchBase&");
    Assert.assertEquals(result.getSystemPassword(), "systemPassword&");
    Assert.assertEquals(result.getSystemUsername(), "systemUsername&");
    Assert.assertEquals(result.getGroupMemberFormat(), "groupMemberFormat&");
    Assert.assertEquals(result.getGroupBaseDn(), "groupBaseDn&");
    Assert.assertEquals(result.getUserBaseDn(), "userBaseDn&");
    Assert.assertEquals(result.getLdapFilter(), "ldapFilter&");

    Assert.assertEquals(result.getAuthScheme(), "authScheme&amp;");
    Assert.assertEquals(result.getHost(), "host&amp;");
    Assert.assertEquals(result.getPort(), 123);
    Assert.assertEquals(result.getProtocol(), "protocol&amp;");
    Assert.assertEquals(result.getRealm(), "realm&amp;");
    Assert.assertEquals(result.getUserMemberOfAttribute(), "userMemberOfAttribute&amp;");
    Assert.assertEquals(result.getEmailAddressAttribute(), "emailAddressAttribute&amp;");
    Assert.assertEquals(result.getGroupIdAttribute(), "groupIdAttribute&amp;");
    Assert.assertEquals(result.getGroupMemberAttribute(), "groupMemberAttribute&amp;");
    Assert.assertEquals(result.getGroupObjectClass(), "groupObjectClass&amp;");
    Assert.assertEquals(result.getUserIdAttribute(), "userIdAttribute&amp;");
    Assert.assertEquals(result.getUserObjectClass(), "userObjectClass&amp;");
    Assert.assertEquals(result.getUserPasswordAttribute(), "userPasswordAttribute&amp;");
    Assert.assertEquals(result.getUserRealNameAttribute(), "userRealNameAttribute&amp;");
    Assert.assertEquals(result.isUserSubtree(), true);

  }

  @Test
  public void testNullHTMLEscapedField() {
    LdapUserAndGroupConfigTestRequest resource = new LdapUserAndGroupConfigTestRequest();
    LdapUserAndGroupConfigTestRequestDTO dto = new LdapUserAndGroupConfigTestRequestDTO();

    resource.setData(dto);
    dto.setLdapFilter(null); // already null, but more clear for the test

    String xml = this.xstreamXML.toXML(resource);
    LdapUserAndGroupConfigTestRequestDTO result =
        ((LdapUserAndGroupConfigTestRequest) this.xstreamXML.fromXML(xml)).getData();

    Assert.assertNull(result.getLdapFilter());

    // simple json string with an explicit null value (generated from the Nexus UI)
    String payload = "{\"data\":{\"ldapFilter\":null}}";

    StringBuilder sb =
        new StringBuilder("{ \"").append(LdapUserAndGroupConfigTestRequest.class.getName()).append("\" : ").append(
            payload).append(" }");
    // validate this parses without error
    xstreamJSON.fromXML(sb.toString());

  }

  @Test
  public void testLdapAuthenticationTestRequest()
      throws Exception
  {
    LdapAuthenticationTestRequest resource = new LdapAuthenticationTestRequest();
    LdapConnectionInfoDTO dto = new LdapConnectionInfoDTO();

    resource.setData(dto);

    dto.setAuthScheme("authScheme");
    dto.setHost("host");
    dto.setPort(123);
    dto.setProtocol("protocol");
    dto.setRealm("realm");
    dto.setSearchBase("searchBase");
    dto.setSystemPassword("systemPassword");
    dto.setSystemUsername("systemUsername");

    validateXmlHasNoPackageNames(resource);
  }

  private void validateXmlHasNoPackageNames(Object obj) {
    String xml = this.xstreamXML.toXML(obj);

    // quick way of looking for the class="org attribute
    // i don't want to parse a dom to figure this out

    int totalCount = StringUtils.countMatches(xml, "org.sonatype");
    totalCount += StringUtils.countMatches(xml, "com.sonatype");

    // check the counts
    Assert.assertFalse("Found package name in XML:\n" + xml, totalCount > 0);

    // // print out each type of method, so i can rafb it
    System.out.println("\n\nClass: " + obj.getClass() + "\n");
    System.out.println(xml + "\n");
    //
    // Assert.assertFalse( "Found <string> XML: " + obj.getClass() + "\n" + xml, xml.contains( "<string>" ) );

  }

}

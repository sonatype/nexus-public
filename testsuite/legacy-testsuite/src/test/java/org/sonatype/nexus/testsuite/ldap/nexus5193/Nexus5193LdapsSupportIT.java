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
package org.sonatype.nexus.testsuite.ldap.nexus5193;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import org.sonatype.ldaptestsuite.LdapServer;
import org.sonatype.nexus.integrationtests.TestContainer;
import org.sonatype.nexus.security.ldap.realms.api.dto.LdapConnectionInfoDTO;
import org.sonatype.nexus.testsuite.ldap.AbstractLdapIntegrationIT;
import org.sonatype.nexus.testsuite.ldap.LdapConnMessageUtil;
import org.sonatype.sisu.goodies.testsupport.net.TrustingX509TrustManager;

import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.junit.BeforeClass;
import org.junit.Test;
import org.restlet.data.MediaType;

import static org.sonatype.nexus.test.utils.NexusRequestMatchers.isSuccessful;

public class Nexus5193LdapsSupportIT
    extends AbstractLdapIntegrationIT
{

  @Override
  protected LdapServer lookupLdapServer()
      throws ComponentLookupException
  {
    return lookup(LdapServer.class, getClass().getName());
  }

  /**
   * This only works because Nexus runs in the same VM as this test.
   *
   * For a launcher-based test, you would probably need to create a keystore with
   * the apache-ds cert, and set '-Djavax.net.ssl.trustStore=$jksPath'.
   */
  @BeforeClass
  public static void trustAllCerts()
      throws KeyManagementException, NoSuchAlgorithmException
  {
    SSLContext context = SSLContext.getInstance("SSL");
    TrustManager[] tm = new TrustManager[]{new TrustingX509TrustManager()};
    context.init(null, tm, null);
    SSLContext.setDefault(context);
  }

  @Test
  public void testAuth()
      throws Exception
  {
    TestContainer.getInstance().getTestContext().useAdminForRequests();
    LdapConnMessageUtil connUtil = new LdapConnMessageUtil(getJsonXStream(), MediaType.APPLICATION_JSON);
    final LdapConnectionInfoDTO connectionInfo = getConnectionInfo();

    connUtil.testAuth(connectionInfo, isSuccessful());
  }

  private LdapConnectionInfoDTO getConnectionInfo() {
    LdapConnectionInfoDTO connInfo = new LdapConnectionInfoDTO();

    connInfo.setSearchBase("o=sonatype");
    connInfo.setAuthScheme("none");
    connInfo.setProtocol("ldaps");
    connInfo.setHost("localhost");
    connInfo.setPort(this.getLdapPort());
    connInfo.setRealm(null);
    connInfo.setSystemPassword(null);
    connInfo.setSystemUsername(null);
    //        connInfo.setAuthScheme( "simple" );
    //        connInfo.setSystemPassword( "secret" );
    //        connInfo.setSystemUsername( "uid=admin,ou=system" );
    return connInfo;
  }
}

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
package org.sonatype.nexus.testsuite.ldap;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.security.ldap.realms.persist.model.Configuration;
import org.sonatype.security.ldap.realms.persist.model.io.xpp3.LdapConfigurationXpp3Reader;

import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

public class LdapConfigurationUtil
{
  public static Configuration getConfiguration() throws IOException, XmlPullParserException {
    File ldapConfig = new File(AbstractNexusIntegrationTest.WORK_CONF_DIR, "/ldap.xml");
    return getConfiguration(ldapConfig);
  }

  public static Configuration getConfiguration(File configurationFile) throws IOException, XmlPullParserException {

    Reader fr = null;
    FileInputStream is = null;
    Configuration configuration = null;

    try {
      is = new FileInputStream(configurationFile);

      LdapConfigurationXpp3Reader reader = new LdapConfigurationXpp3Reader();

      fr = new InputStreamReader(is);

      configuration = reader.read(fr);
    }
    finally {
      if (fr != null) {
        try {
          fr.close();
        }
        catch (IOException e) {
          // just closing if open
        }
      }

      if (is != null) {
        try {
          is.close();
        }
        catch (IOException e) {
          // just closing if open
        }
      }
    }

    return configuration;
  }

}

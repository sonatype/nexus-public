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
package org.sonatype.nexus.configuration;

import java.io.File;

import org.sonatype.nexus.NexusAppTestSupport;
import org.sonatype.nexus.configuration.model.CRemoteAuthentication;
import org.sonatype.nexus.configuration.model.CRemoteHttpProxySettings;
import org.sonatype.nexus.configuration.model.CRemoteProxySettings;
import org.sonatype.nexus.configuration.model.CRepository;
import org.sonatype.nexus.configuration.model.Configuration;
import org.sonatype.nexus.configuration.source.ApplicationConfigurationSource;

import com.thoughtworks.xstream.XStream;
import org.apache.commons.io.FileUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.junit.Assert;
import org.junit.Test;

public class ClearPasswordTest
    extends NexusAppTestSupport
{
  private ApplicationConfigurationSource getConfigSource()
      throws Exception
  {
    // get the config
    return this.lookup(ApplicationConfigurationSource.class, "file");
  }

  @Test
  public void testDefaultConfig()
      throws Exception
  {
    // start with the default nexus config
    this.copyDefaultConfigToPlace();

    this.doTestLogic();
  }

  private void doTestLogic()
      throws Exception
  {
    ApplicationConfigurationSource source = this.getConfigSource();

    Configuration config = source.loadConfiguration();

    // make sure the smtp-password is what we expect
    Assert.assertEquals("Incorrect SMTP password found in nexus.xml", "smtp-password", config
        .getSmtpConfiguration().getPassword());

    // set the clear passwords
    String password = "clear-text";

    // smtp
    config.getSmtpConfiguration().setPassword(password);

    // global proxy
    config.setRemoteProxySettings(new CRemoteProxySettings());

    final CRemoteHttpProxySettings httpProxySettings = new CRemoteHttpProxySettings();
    httpProxySettings.setProxyHostname("localhost");
    httpProxySettings.setProxyPort(1234);
    httpProxySettings.setAuthentication(new CRemoteAuthentication());
    httpProxySettings.getAuthentication().setPassword(password);

    final CRemoteHttpProxySettings httpsProxySettings = new CRemoteHttpProxySettings();
    httpsProxySettings.setProxyHostname("localhost");
    httpsProxySettings.setProxyPort(1234);
    httpsProxySettings.setAuthentication(new CRemoteAuthentication());
    httpsProxySettings.getAuthentication().setPassword(password);

    config.getRemoteProxySettings().setHttpProxySettings(httpProxySettings);
    config.getRemoteProxySettings().setHttpsProxySettings(httpsProxySettings);

    //        config.getSecurity().setAnonymousPassword( password );
    //
    //        // anon username
    //        config.getSecurity().setAnonymousPassword( password );

    // repo auth pass
    CRepository central = this.getCentralRepo(config);
    central.getRemoteStorage().setAuthentication(new CRemoteAuthentication());
    central.getRemoteStorage().getAuthentication().setPassword(password);

    // now we need to make the file valid....
    config.getRemoteProxySettings().getHttpProxySettings().setProxyPort(1234);

    // save it
    source.storeConfiguration();

    XStream xs = new XStream();
    xs.processAnnotations(new Class[] { Xpp3Dom.class });
    Assert.assertTrue("Configuration is corroupt, passwords are encrypted (in memory). ",
        xs.toXML(config).contains(password));

    // now get the file and look for the "clear-text"
    String configString = FileUtils.readFileToString(new File(this.getNexusConfiguration()));

    Assert.assertFalse("Clear text password found in nexus.xml:\n" + configString, configString
        .contains(password));

    // make sure we do not have the default smtp password either
    Assert.assertFalse("Old SMTP password found in nexus.xml", configString.contains("smtp-password"));

    // now load it again and make sure the password is clear text
    Configuration newConfig = source.loadConfiguration();
    Assert.assertEquals(password, newConfig.getSmtpConfiguration().getPassword());
    Assert.assertEquals(
        password, newConfig.getRemoteProxySettings().getHttpProxySettings().getAuthentication().getPassword()
    );
    Assert.assertEquals(
        password, newConfig.getRemoteProxySettings().getHttpsProxySettings().getAuthentication().getPassword()
    );
    //        Assert.assertEquals( password, newConfig.getSecurity().getAnonymousPassword() );

    central = this.getCentralRepo(newConfig);
    Assert.assertEquals(password, central.getRemoteStorage().getAuthentication().getPassword());
  }

  private CRepository getCentralRepo(Configuration config) {
    for (CRepository repo : config.getRepositories()) {
      if (repo.getId().equals("central")) {
        return repo;
      }
    }
    return null;
  }

}

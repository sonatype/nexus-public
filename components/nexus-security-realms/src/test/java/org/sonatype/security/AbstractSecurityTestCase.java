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
package org.sonatype.security;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;

import javax.inject.Inject;

import org.sonatype.security.configuration.model.SecurityConfiguration;
import org.sonatype.security.configuration.source.SecurityConfigurationSource;
import org.sonatype.security.guice.SecurityModule;
import org.sonatype.security.model.Configuration;
import org.sonatype.security.model.io.xpp3.SecurityConfigurationXpp3Reader;

import com.google.inject.Binder;
import net.sf.ehcache.CacheManager;
import org.apache.commons.io.FileUtils;
import org.apache.shiro.realm.Realm;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.sisu.launch.InjectedTestCase;
import org.eclipse.sisu.space.BeanScanning;

public abstract class AbstractSecurityTestCase
    extends InjectedTestCase
{

  public static final String PLEXUS_SECURITY_XML_FILE = "security-xml-file";

  protected File PLEXUS_HOME = new File("./target/plexus_home");

  protected File CONFIG_DIR = new File(PLEXUS_HOME, "conf");

  @Inject
  private Map<String, Realm> realmMap;

  @Override
  public void configure(Properties properties) {
    properties.put("application-conf", CONFIG_DIR.getAbsolutePath());
    properties.put("security-xml-file", CONFIG_DIR.getAbsolutePath() + "/security.xml");
    super.configure(properties);
  }

  @Override
  public void configure(final Binder binder) {
    // install the module, to not rely on deprecated Plexus components anymore
    // (as they are REMOVED)
    binder.install(new SecurityModule());
  }

  @Override
  public BeanScanning scanning() {
    return BeanScanning.INDEX;
  }

  @Override
  protected void setUp()
      throws Exception
  {
    FileUtils.deleteDirectory(PLEXUS_HOME);

    super.setUp();

    CONFIG_DIR.mkdirs();

    SecurityConfigurationSource source = this.lookup(SecurityConfigurationSource.class, "file");
    SecurityConfiguration config = source.loadConfiguration();

    config.setRealms(new ArrayList<String>(realmMap.keySet()));
    source.storeConfiguration();

    lookup(SecuritySystem.class).start();
  }

  @Override
  protected void tearDown()
      throws Exception
  {
    try {
      lookup(SecuritySystem.class).stop();
      lookup(CacheManager.class).shutdown();
    }
    finally {
      super.tearDown();
    }
  }

  protected Configuration getConfigurationFromStream(InputStream is)
      throws Exception
  {
    SecurityConfigurationXpp3Reader reader = new SecurityConfigurationXpp3Reader();

    Reader fr = new InputStreamReader(is);

    return reader.read(fr);
  }

  protected Configuration getSecurityConfiguration()
      throws IOException, XmlPullParserException
  {
    // now lets check the XML file for the user and the role mapping
    SecurityConfigurationXpp3Reader secReader = new SecurityConfigurationXpp3Reader();
    try (FileReader fileReader = new FileReader(new File(CONFIG_DIR, "security.xml"))) {
      return secReader.read(fileReader);
    }
  }
}

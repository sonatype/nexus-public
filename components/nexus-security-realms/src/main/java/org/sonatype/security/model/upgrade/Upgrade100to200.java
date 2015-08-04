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
package org.sonatype.security.model.upgrade;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

import javax.enterprise.inject.Typed;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.configuration.upgrade.ConfigurationIsCorruptedException;
import org.sonatype.configuration.upgrade.UpgradeMessage;
import org.sonatype.security.legacy.model.v1_0_0.CApplicationPrivilege;
import org.sonatype.security.legacy.model.v1_0_0.CRepoTargetPrivilege;
import org.sonatype.security.legacy.model.v1_0_0.CRole;
import org.sonatype.security.legacy.model.v1_0_0.CUser;
import org.sonatype.security.legacy.model.v1_0_0.Configuration;
import org.sonatype.security.legacy.model.v1_0_0.io.xpp3.SecurityLegacyConfigurationXpp3Reader;

import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

@Singleton
@Typed(SecurityUpgrader.class)
@Named("1.0.0")
public class Upgrade100to200
    implements SecurityUpgrader
{
  public Object loadConfiguration(File file)
      throws IOException, ConfigurationIsCorruptedException
  {
    FileReader fr = null;

    try {
      // reading without interpolation to preserve user settings as variables
      fr = new FileReader(file);

      SecurityLegacyConfigurationXpp3Reader reader = new SecurityLegacyConfigurationXpp3Reader();

      return reader.read(fr);
    }
    catch (XmlPullParserException e) {
      throw new ConfigurationIsCorruptedException(file.getAbsolutePath(), e);
    }
    finally {
      if (fr != null) {
        fr.close();
      }
    }
  }

  public void upgrade(UpgradeMessage message)
      throws ConfigurationIsCorruptedException
  {
    Configuration oldc = (Configuration) message.getConfiguration();

    org.sonatype.security.model.v2_0_0.Configuration newc = new org.sonatype.security.model.v2_0_0.Configuration();

    newc.setVersion(org.sonatype.security.model.v2_0_0.Configuration.MODEL_VERSION);

    for (CUser oldu : (List<CUser>) oldc.getUsers()) {
      org.sonatype.security.model.v2_0_0.CUser newu = new org.sonatype.security.model.v2_0_0.CUser();

      newu.setEmail(oldu.getEmail());
      newu.setId(oldu.getUserId());
      newu.setName(oldu.getName());
      newu.setPassword(oldu.getPassword());
      newu.setStatus(oldu.getStatus());
      newu.setRoles(oldu.getRoles());

      newc.addUser(newu);
    }

    for (CRole oldr : (List<CRole>) oldc.getRoles()) {
      org.sonatype.security.model.v2_0_0.CRole newr = new org.sonatype.security.model.v2_0_0.CRole();

      newr.setDescription(oldr.getDescription());
      newr.setId(oldr.getId());
      newr.setName(oldr.getName());
      newr.setPrivileges(oldr.getPrivileges());
      newr.setRoles(oldr.getRoles());
      newr.setSessionTimeout(oldr.getSessionTimeout());

      newc.addRole(newr);
    }

    for (CRepoTargetPrivilege oldp : (List<CRepoTargetPrivilege>) oldc.getRepositoryTargetPrivileges()) {
      org.sonatype.security.model.v2_0_0.CPrivilege newp = new org.sonatype.security.model.v2_0_0.CPrivilege();

      newp.setDescription(oldp.getDescription());
      newp.setId(oldp.getId());
      newp.setName(oldp.getName());
      newp.setType("target");

      org.sonatype.security.model.v2_0_0.CProperty prop = new org.sonatype.security.model.v2_0_0.CProperty();
      prop.setKey("method");
      prop.setValue(oldp.getMethod());
      newp.addProperty(prop);

      if (!StringUtils.isEmpty(oldp.getRepositoryId())) {
        prop = new org.sonatype.security.model.v2_0_0.CProperty();
        prop.setKey("repositoryGroupId");
        prop.setValue(oldp.getGroupId());
        newp.addProperty(prop);
      }

      if (!StringUtils.isEmpty(oldp.getRepositoryId())) {
        prop = new org.sonatype.security.model.v2_0_0.CProperty();
        prop.setKey("repositoryId");
        prop.setValue(oldp.getRepositoryId());
        newp.addProperty(prop);
      }

      prop = new org.sonatype.security.model.v2_0_0.CProperty();
      prop.setKey("repositoryTargetId");
      prop.setValue(oldp.getRepositoryTargetId());
      newp.addProperty(prop);

      newc.addPrivilege(newp);
    }

    for (CApplicationPrivilege oldp : (List<CApplicationPrivilege>) oldc.getApplicationPrivileges()) {
      org.sonatype.security.model.v2_0_0.CPrivilege newp = new org.sonatype.security.model.v2_0_0.CPrivilege();

      newp.setDescription(oldp.getDescription());
      newp.setId(oldp.getId());
      newp.setName(oldp.getName());
      newp.setType("method");

      org.sonatype.security.model.v2_0_0.CProperty prop = new org.sonatype.security.model.v2_0_0.CProperty();
      prop.setKey("method");
      prop.setValue(oldp.getMethod());
      newp.addProperty(prop);

      prop = new org.sonatype.security.model.v2_0_0.CProperty();
      prop.setKey("permission");
      prop.setValue(oldp.getPermission());
      newp.addProperty(prop);

      newc.addPrivilege(newp);
    }

    message.setModelVersion(org.sonatype.security.model.v2_0_0.Configuration.MODEL_VERSION);
    message.setConfiguration(newc);
  }
}

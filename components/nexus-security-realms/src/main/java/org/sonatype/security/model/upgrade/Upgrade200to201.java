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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.enterprise.inject.Typed;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.configuration.upgrade.ConfigurationIsCorruptedException;
import org.sonatype.configuration.upgrade.UpgradeMessage;
import org.sonatype.security.model.v2_0_0.CPrivilege;
import org.sonatype.security.model.v2_0_0.CProperty;
import org.sonatype.security.model.v2_0_0.CRole;
import org.sonatype.security.model.v2_0_0.CUser;
import org.sonatype.security.model.v2_0_0.Configuration;
import org.sonatype.security.model.v2_0_0.io.xpp3.SecurityConfigurationXpp3Reader;

import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

@Singleton
@Typed(SecurityUpgrader.class)
@Named("2.0.0")
public class Upgrade200to201
    implements SecurityUpgrader
{
  public Object loadConfiguration(File file)
      throws IOException, ConfigurationIsCorruptedException
  {
    FileReader fr = null;

    try {
      // reading without interpolation to preserve user settings as variables
      fr = new FileReader(file);

      SecurityConfigurationXpp3Reader reader = new SecurityConfigurationXpp3Reader();

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

  @SuppressWarnings("unchecked")
  public void upgrade(UpgradeMessage message)
      throws ConfigurationIsCorruptedException
  {
    Configuration oldc = (Configuration) message.getConfiguration();

    org.sonatype.security.model.v2_0_1.Configuration newc = new org.sonatype.security.model.v2_0_1.Configuration();

    newc.setVersion(org.sonatype.security.model.v2_0_1.Configuration.MODEL_VERSION);

    for (CUser oldu : (List<CUser>) oldc.getUsers()) {
      org.sonatype.security.model.v2_0_1.CUser newu = new org.sonatype.security.model.v2_0_1.CUser();

      newu.setEmail(oldu.getEmail());
      newu.setId(oldu.getId());
      newu.setName(oldu.getName());
      newu.setPassword(oldu.getPassword());
      newu.setStatus(oldu.getStatus());
      newu.setRoles(oldu.getRoles());

      newc.addUser(newu);
    }

    List<RoleMap> roleMapList = new ArrayList<RoleMap>();

    for (CRole oldr : (List<CRole>) oldc.getRoles()) {
      // Simplest case, not an internal role, just copy
      if (!getRolesToRemove().contains(oldr.getId())) {
        org.sonatype.security.model.v2_0_1.CRole newr = new org.sonatype.security.model.v2_0_1.CRole();

        newr.setDescription(oldr.getDescription());
        newr.setId(oldr.getId());
        newr.setName(oldr.getName());
        newr.setPrivileges(oldr.getPrivileges());
        newr.setRoles(oldr.getRoles());
        newr.setSessionTimeout(oldr.getSessionTimeout());

        newc.addRole(newr);
      }
      // If we have internally, and the user has changed the role previously (as read only is new in this version)
      else if (shouldArchiveRole(oldr)) {
        org.sonatype.security.model.v2_0_1.CRole newr = new org.sonatype.security.model.v2_0_1.CRole();

        newr.setDescription(oldr.getDescription());
        newr.setId(oldr.getId() + "-customized");
        newr.setName(oldr.getName() + " (Customized)");
        newr.setPrivileges(oldr.getPrivileges());
        newr.setRoles(oldr.getRoles());
        newr.setSessionTimeout(oldr.getSessionTimeout());

        newc.addRole(newr);

        roleMapList.add(new RoleMap(oldr.getId(), newr.getId()));
      }
      // else the role will be removed, if it is now internal, and the user hasn't changed it
    }

    for (CPrivilege oldp : (List<CPrivilege>) oldc.getPrivileges()) {
      if (!getPrivsToRemove().contains(oldp.getId())) {
        org.sonatype.security.model.v2_0_1.CPrivilege newp =
            new org.sonatype.security.model.v2_0_1.CPrivilege();

        newp.setDescription(oldp.getDescription());
        newp.setId(oldp.getId());
        newp.setName(oldp.getName());
        newp.setType(oldp.getType());

        for (CProperty oldprop : (List<CProperty>) oldp.getProperties()) {
          org.sonatype.security.model.v2_0_1.CProperty newprop =
              new org.sonatype.security.model.v2_0_1.CProperty();
          newprop.setKey(oldprop.getKey());
          newprop.setValue(oldprop.getValue());
          newp.addProperty(newprop);
        }
        newc.addPrivilege(newp);
      }
    }

    // Fix to use the archived roles
    for (RoleMap roleMap : roleMapList) {
      applyArchivedRoles(roleMap, newc);
    }

    // Fix the new anon and deployment roles if assigned to users
    applyNewRepoRoles(newc);

    message.setModelVersion(org.sonatype.security.model.v2_0_1.Configuration.MODEL_VERSION);
    message.setConfiguration(newc);
  }

  @SuppressWarnings("unchecked")
  private void applyNewRepoRoles(org.sonatype.security.model.v2_0_1.Configuration config) {
    for (org.sonatype.security.model.v2_0_1.CUser user : (List<org.sonatype.security.model.v2_0_1.CUser>) config
        .getUsers()) {
      if (user.getRoles().contains("anonymous")) {
        user.getRoles().add("repo-all-read");
      }

      if (user.getRoles().contains("deployment")) {
        user.getRoles().add("repo-all-full");
      }
    }

    for (org.sonatype.security.model.v2_0_1.CRole role : (List<org.sonatype.security.model.v2_0_1.CRole>) config
        .getRoles()) {
      if (role.getRoles().contains("anonymous")) {
        role.getRoles().add("repo-all-read");
      }

      if (role.getRoles().contains("deployment")) {
        role.getRoles().add("repo-all-full");
      }
    }
  }

  @SuppressWarnings("unchecked")
  private void applyArchivedRoles(RoleMap roleMap, org.sonatype.security.model.v2_0_1.Configuration config) {
    for (org.sonatype.security.model.v2_0_1.CUser user : (List<org.sonatype.security.model.v2_0_1.CUser>) config
        .getUsers()) {
      if (user.getRoles().contains(roleMap.oldId)) {
        int index = user.getRoles().indexOf(roleMap.oldId);
        user.getRoles().remove(roleMap.oldId);
        user.getRoles().add(index, roleMap.newId);
      }
    }

    for (org.sonatype.security.model.v2_0_1.CRole role : (List<org.sonatype.security.model.v2_0_1.CRole>) config
        .getRoles()) {
      if (role.getRoles().contains(roleMap.oldId)) {
        int index = role.getRoles().indexOf(roleMap.oldId);
        role.getRoles().remove(roleMap.oldId);
        role.getRoles().add(index, roleMap.newId);
      }
    }
  }

  private boolean shouldArchiveRole(CRole oldRole) {
    boolean result = true;

    if ("admin".equals(oldRole.getId()) && oldRole.getPrivileges().size() == 1
        && oldRole.getPrivileges().contains("1000") && oldRole.getRoles().size() == 0) {
      result = false;
    }
    else if ("anonymous".equals(oldRole.getId()) && oldRole.getPrivileges().size() == 14
        && oldRole.getPrivileges().contains("1") && oldRole.getPrivileges().contains("6")
        && oldRole.getPrivileges().contains("14") && oldRole.getPrivileges().contains("17")
        && oldRole.getPrivileges().contains("19") && oldRole.getPrivileges().contains("44")
        && oldRole.getPrivileges().contains("54") && oldRole.getPrivileges().contains("55")
        && oldRole.getPrivileges().contains("56") && oldRole.getPrivileges().contains("57")
        && oldRole.getPrivileges().contains("58") && oldRole.getPrivileges().contains("64")
        && oldRole.getPrivileges().contains("T1") && oldRole.getPrivileges().contains("T2")
        && oldRole.getRoles().size() == 0) {
      result = false;
    }
    else if ("deployment".equals(oldRole.getId()) && oldRole.getPrivileges().size() == 9
        && oldRole.getPrivileges().contains("64") && oldRole.getPrivileges().contains("T1")
        && oldRole.getPrivileges().contains("T2") && oldRole.getPrivileges().contains("T3")
        && oldRole.getPrivileges().contains("T4") && oldRole.getPrivileges().contains("T5")
        && oldRole.getPrivileges().contains("T6") && oldRole.getPrivileges().contains("T7")
        && oldRole.getPrivileges().contains("T8") && oldRole.getRoles().size() == 1
        && oldRole.getRoles().contains("anonymous")) {
      result = false;
    }
    else if ("developer".equals(oldRole.getId()) && oldRole.getPrivileges().size() == 1
        && oldRole.getPrivileges().contains("2") && oldRole.getRoles().size() == 2
        && oldRole.getRoles().contains("deployment") && oldRole.getRoles().contains("anonymous")) {
      result = false;
    }
    else if ("repo-all-read".equals(oldRole.getId()) && oldRole.getPrivileges().size() == 2
        && oldRole.getPrivileges().contains("T1") && oldRole.getPrivileges().contains("T2")
        && oldRole.getRoles().size() == 0) {
      result = false;
    }
    else if ("repo-all-full".equals(oldRole.getId()) && oldRole.getPrivileges().size() == 8
        && oldRole.getPrivileges().contains("T1") && oldRole.getPrivileges().contains("T2")
        && oldRole.getPrivileges().contains("T3") && oldRole.getPrivileges().contains("T4")
        && oldRole.getPrivileges().contains("T5") && oldRole.getPrivileges().contains("T6")
        && oldRole.getPrivileges().contains("T7") && oldRole.getPrivileges().contains("T8")
        && oldRole.getRoles().size() == 0) {
      result = false;
    }
    else if ("ui-search".equals(oldRole.getId()) && oldRole.getPrivileges().size() == 2
        && oldRole.getPrivileges().contains("17") && oldRole.getPrivileges().contains("19")
        && oldRole.getRoles().size() == 0) {
      result = false;
    }
    else if ("ui-repo-browser".equals(oldRole.getId()) && oldRole.getPrivileges().size() == 3
        && oldRole.getPrivileges().contains("6") && oldRole.getPrivileges().contains("14")
        && oldRole.getPrivileges().contains("55") && oldRole.getRoles().size() == 0) {
      result = false;
    }
    else if ("ui-system-feeds".equals(oldRole.getId()) && oldRole.getPrivileges().size() == 1
        && oldRole.getPrivileges().contains("44") && oldRole.getRoles().size() == 0) {
      result = false;
    }
    else if ("ui-logs-config-files".equals(oldRole.getId()) && oldRole.getPrivileges().size() == 2
        && oldRole.getPrivileges().contains("42") && oldRole.getPrivileges().contains("43")
        && oldRole.getRoles().size() == 0) {
      result = false;
    }
    else if ("ui-server-admin".equals(oldRole.getId()) && oldRole.getPrivileges().size() == 2
        && oldRole.getPrivileges().contains("3") && oldRole.getPrivileges().contains("4")
        && oldRole.getRoles().size() == 0) {
      result = false;
    }
    else if ("ui-repository-admin".equals(oldRole.getId()) && oldRole.getPrivileges().size() == 5
        && oldRole.getPrivileges().contains("5") && oldRole.getPrivileges().contains("6")
        && oldRole.getPrivileges().contains("7") && oldRole.getPrivileges().contains("8")
        && oldRole.getPrivileges().contains("10") && oldRole.getRoles().size() == 0) {
      result = false;
    }
    else if ("ui-group-admin".equals(oldRole.getId()) && oldRole.getPrivileges().size() == 5
        && oldRole.getPrivileges().contains("6") && oldRole.getPrivileges().contains("13")
        && oldRole.getPrivileges().contains("14") && oldRole.getPrivileges().contains("15")
        && oldRole.getPrivileges().contains("16") && oldRole.getRoles().size() == 0) {
      result = false;
    }
    else if ("ui-routing-admin".equals(oldRole.getId()) && oldRole.getPrivileges().size() == 6
        && oldRole.getPrivileges().contains("6") && oldRole.getPrivileges().contains("14")
        && oldRole.getPrivileges().contains("22") && oldRole.getPrivileges().contains("23")
        && oldRole.getPrivileges().contains("24") && oldRole.getPrivileges().contains("25")
        && oldRole.getRoles().size() == 0) {
      result = false;
    }
    else if ("ui-scheduled-tasks-admin".equals(oldRole.getId()) && oldRole.getPrivileges().size() == 7
        && oldRole.getPrivileges().contains("6") && oldRole.getPrivileges().contains("14")
        && oldRole.getPrivileges().contains("26") && oldRole.getPrivileges().contains("27")
        && oldRole.getPrivileges().contains("28") && oldRole.getPrivileges().contains("29")
        && oldRole.getPrivileges().contains("69") && oldRole.getRoles().size() == 0) {
      result = false;
    }
    else if ("ui-repository-targets-admin".equals(oldRole.getId()) && oldRole.getPrivileges().size() == 5
        && oldRole.getPrivileges().contains("45") && oldRole.getPrivileges().contains("46")
        && oldRole.getPrivileges().contains("47") && oldRole.getPrivileges().contains("48")
        && oldRole.getPrivileges().contains("56") && oldRole.getRoles().size() == 0) {
      result = false;
    }
    else if ("ui-users-admin".equals(oldRole.getId()) && oldRole.getPrivileges().size() == 5
        && oldRole.getPrivileges().contains("35") && oldRole.getPrivileges().contains("38")
        && oldRole.getPrivileges().contains("39") && oldRole.getPrivileges().contains("40")
        && oldRole.getPrivileges().contains("41") && oldRole.getRoles().size() == 0) {
      result = false;
    }
    else if ("ui-roles-admin".equals(oldRole.getId()) && oldRole.getPrivileges().size() == 5
        && oldRole.getPrivileges().contains("31") && oldRole.getPrivileges().contains("34")
        && oldRole.getPrivileges().contains("35") && oldRole.getPrivileges().contains("36")
        && oldRole.getPrivileges().contains("37") && oldRole.getRoles().size() == 0) {
      result = false;
    }
    else if ("ui-privileges-admin".equals(oldRole.getId()) && oldRole.getPrivileges().size() == 7
        && oldRole.getPrivileges().contains("6") && oldRole.getPrivileges().contains("14")
        && oldRole.getPrivileges().contains("30") && oldRole.getPrivileges().contains("31")
        && oldRole.getPrivileges().contains("32") && oldRole.getPrivileges().contains("33")
        && oldRole.getPrivileges().contains("46") && oldRole.getRoles().size() == 0) {
      result = false;
    }
    else if ("ui-basic".equals(oldRole.getId()) && oldRole.getPrivileges().size() == 3
        && oldRole.getPrivileges().contains("1") && oldRole.getPrivileges().contains("2")
        && oldRole.getPrivileges().contains("64") && oldRole.getRoles().size() == 0) {
      result = false;
    }

    return result;
  }

  private Set<String> getRolesToRemove() {
    Set<String> set = new HashSet<String>();

    set.add("admin");
    set.add("deployment");
    set.add("anonymous");
    set.add("developer");
    set.add("ui-search");
    set.add("ui-repo-browser");
    set.add("ui-system-feeds");
    set.add("ui-logs-config-files");
    set.add("ui-server-admin");
    set.add("ui-repository-admin");
    set.add("ui-group-admin");
    set.add("ui-routing-admin");
    set.add("ui-scheduled-tasks-admin");
    set.add("ui-repository-targets-admin");
    set.add("ui-users-admin");
    set.add("ui-roles-admin");
    set.add("ui-privileges-admin");
    set.add("ui-basic");

    return set;
  }

  private Set<String> getPrivsToRemove() {
    Set<String> set = new HashSet<String>();

    set.add("T1");
    set.add("T2");
    set.add("T3");
    set.add("T4");
    set.add("T5");
    set.add("T6");
    set.add("T7");
    set.add("T8");
    set.add("1000");
    set.add("1");
    set.add("2");
    set.add("3");
    set.add("4");
    set.add("5");
    set.add("6");
    set.add("7");
    set.add("8");
    set.add("9");
    set.add("10");
    set.add("11");
    set.add("12");
    set.add("13");
    set.add("14");
    set.add("15");
    set.add("16");
    set.add("17");
    set.add("18");
    set.add("19");
    set.add("20");
    set.add("21");
    set.add("22");
    set.add("23");
    set.add("24");
    set.add("25");
    set.add("26");
    set.add("27");
    set.add("28");
    set.add("29");
    set.add("30");
    set.add("31");
    set.add("32");
    set.add("33");
    set.add("34");
    set.add("35");
    set.add("36");
    set.add("37");
    set.add("38");
    set.add("39");
    set.add("40");
    set.add("41");
    set.add("42");
    set.add("43");
    set.add("44");
    set.add("45");
    set.add("46");
    set.add("47");
    set.add("48");
    set.add("49");
    set.add("50");
    set.add("51");
    set.add("54");
    set.add("55");
    set.add("56");
    set.add("57");
    set.add("58");
    set.add("59");
    set.add("64");
    set.add("65");
    set.add("66");
    set.add("67");
    set.add("68");
    set.add("69");
    set.add("70");
    set.add("71");
    set.add("72");

    return set;
  }

  private static class RoleMap
  {
    private String oldId;

    private String newId;

    protected RoleMap(String oldId, String newId) {
      this.oldId = oldId;
      this.newId = newId;
    }

    public String getNewId() {
      return newId;
    }

    public String getOldId() {
      return oldId;
    }
  }
}

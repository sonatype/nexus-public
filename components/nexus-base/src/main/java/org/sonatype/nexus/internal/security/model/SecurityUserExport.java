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
package org.sonatype.nexus.internal.security.model;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.security.config.CRole;
import org.sonatype.nexus.security.config.CUser;
import org.sonatype.nexus.security.config.CUserRoleMapping;
import org.sonatype.nexus.security.config.SecurityConfiguration;
import org.sonatype.nexus.supportzip.ExportSecurityData;
import org.sonatype.nexus.supportzip.ImportData;
import org.sonatype.nexus.supportzip.datastore.JsonExporter;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import static java.util.function.Function.identity;
import static org.sonatype.nexus.security.user.UserManager.DEFAULT_SOURCE;

/**
 * Write/Read {@link CRole} data to/from a JSON file.
 *
 * @since 3.29
 */
@Named("securityUserExport")
@Singleton
public class SecurityUserExport
    extends JsonExporter
    implements ExportSecurityData, ImportData
{
  private final SecurityConfiguration configuration;

  @Inject
  public SecurityUserExport(final SecurityConfiguration configuration) {
    this.configuration = configuration;
  }

  @Override
  public void export(final File file) throws IOException {
    log.debug("Export CUser and CUserRoleMapping data to {}", file);
    Map<String, CUser> userIdToCUser = configuration.getUsers()
        .stream()
        .collect(Collectors.toMap(CUser::getId, identity()));
    List<CUserRoleMapping> userRoleMappings = configuration.getUserRoleMappings();
    List<SecurityUserData> securityUsers = new ArrayList<>(userIdToCUser.size());
    for (Entry<String, CUser> userEntry : userIdToCUser.entrySet()) {
      List<CUserRoleMapping> roleMappings = userRoleMappings.stream()
          .filter(user -> user.getUserId().equals(userEntry.getKey()))
          .collect(Collectors.toList());
      SecurityUserData securityUserData = new SecurityUserData();
      securityUserData.setUser(userEntry.getValue());
      securityUserData.setUserRoleMappings(roleMappings);
      securityUsers.add(securityUserData);
    }

    exportToJson(securityUsers, file);
  }

  @Override
  public void restore(final File file) throws IOException {
    log.debug("Restoring CUser and CUserRoleMapping data from {}", file);
    List<SecurityUserData> securityUsers = importFromJson(file, SecurityUserData.class);
    for (SecurityUserData securityUser : securityUsers) {
      configuration.addUser(securityUser.user, securityUser.getRoles());
    }
  }

  public static class SecurityUserData
  {
    @JsonProperty
    @JsonDeserialize(as = CUserData.class)
    private CUser user;

    @JsonProperty
    @JsonDeserialize(contentAs = CUserRoleMappingData.class)
    private List<CUserRoleMapping> userRoleMappings;

    public CUser getUser() {
      return user;
    }

    public void setUser(final CUser user) {
      this.user = user;
    }

    public List<CUserRoleMapping> getUserRoleMappings() {
      return userRoleMappings;
    }

    public void setUserRoleMappings(final List<CUserRoleMapping> userRoleMappings) {
      this.userRoleMappings = userRoleMappings;
    }

    @JsonIgnore
    public Set<String> getRoles() {
      return userRoleMappings.stream()
          .filter(role -> DEFAULT_SOURCE.equals(role.getSource()))
          .findFirst()
          .map(CUserRoleMapping::getRoles)
          .orElse(Collections.emptySet());
    }
  }
}

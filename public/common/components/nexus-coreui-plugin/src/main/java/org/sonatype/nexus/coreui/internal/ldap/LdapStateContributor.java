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
package org.sonatype.nexus.coreui.internal.ldap;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.sonatype.nexus.rapture.StateContributor;
import org.sonatype.nexus.security.realm.RealmManager;

import com.google.common.collect.ImmutableMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class LdapStateContributor
    implements StateContributor
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  private static final String LDAP_REALM_NAME = "LdapRealm";

  public boolean featureFlag;

  public int mappedRoleQueryCharacterLimit;

  private final RealmManager realmManager;

  @Autowired
  public LdapStateContributor(
      @Value("${nexus.react.ldap:false}") final Boolean featureFlag,
      @Value("${nexus.ldap.mapped.role.query.character.limit:3}") final int mappedRoleQueryCharacterLimit,
      final RealmManager realmManager)
  {
    this.featureFlag = featureFlag;
    this.mappedRoleQueryCharacterLimit = mappedRoleQueryCharacterLimit;
    this.realmManager = realmManager;
  }

  @Override
  public Map<String, Object> getState() {
    return ImmutableMap.of(
        "nexus.react.ldap", featureFlag,
        "nexus.ldap.mapped.role.query.character.limit", mappedRoleQueryCharacterLimit,
        "ldapRealmEnabled", realmManager.isRealmEnabled(LDAP_REALM_NAME));
  }
}

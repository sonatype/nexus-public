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
package org.sonatype.nexus.internal.security;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.security.config.MemorySecurityConfiguration;
import org.sonatype.nexus.security.config.SecurityContributor;
import org.sonatype.nexus.security.config.SecurityContributorSupport;

import static org.apache.commons.lang.StringUtils.capitalize;
import static org.sonatype.nexus.security.Roles.ADMIN_ROLE_ID;
import static org.sonatype.nexus.security.Roles.ANONYMOUS_ROLE_ID;

/**
 * Default Nexus security configuration.
 *
 * @since 3.0
 */
@Named
@Singleton
public class NexusSecurityContributor
    extends SecurityContributorSupport
    implements SecurityContributor
{
  private static final String SETTINGS_DOMAIN = "settings";

  private static final String BUNDLES_DOMAIN = "bundles";

  private static final String SEARCH_DOMAIN = "search";

  private static final String APIKEY_DOMAIN = "apikey";

  public static final String NX_ALL_PRIV_ID = "nx-all";

  public static final String NX_ALL_PRIV_DESCRIPTION = "All permissions";

  public static final String NX_ALL_PATTERN = "nexus:*";

  public static final String NX_SETTINGS_ALL_PRIV_ID = "nx-settings-all";

  public static final String NX_SETTINGS_READ_PRIV_ID = "nx-settings-read";

  public static final String NX_SETTINGS_UPDATE_PRIV_ID = "nx-settings-update";

  public static final String NX_BUNDLES_ALL_PRIV_ID = "nx-bundles-all";

  public static final String NX_BUNDLES_READ_PRIV_ID = "nx-bundles-read";

  public static final String NX_SEARCH_READ_PRIV_ID = "nx-search-read";

  public static final String NX_APIKEY_ALL_PRIV_ID = "nx-apikey-all";

  public static final String NX_APIKEY_ALL_DESCRIPTION = ALL_DESCRIPTION_BASE + "APIKey";

  public static final String NX_PRIVILEGE_DOMAIN = "privileges";

  public static final String NX_PRIVILEGE_PRIV_ID_PREFIX = "nx-privileges";

  public static final String NX_ROLE_DOMAIN = "roles";

  public static final String NX_ROLE_PRIV_ID_PREFIX = "nx-roles";

  public static final String NX_USER_DOMAIN = "users";

  public static final String NX_USER_PRIV_ID_PREFIX = "nx-users";

  public static final String NX_USERCHANGEPW_DOMAIN = "userschangepw";

  public static final String NX_USERCHANGEPW_PRIV_ID = "nx-userschangepw";

  public static final String NX_USERCHANGEPW_PRIV_DESCRIPTION = "Change password permission";

  public static final String NX_UPLOAD_DOMAIN = "component";

  public static final String NX_UPLOAD_PRIV_ID = "nx-component-upload";

  public static final String NX_UPLOAD_PRIV_DESCRIPTION = "Upload component permission";

  public static final String NX_ADMIN_ROLE_DESCRIPTION = "Administrator Role";

  public static final String NX_ANON_ROLE_DESCRIPTION = "Anonymous Role";

  public static final String NX_HEALTHCHECK_READ_PRIV_ID = "nx-healthcheck-read";

  public static final String NX_REPO_VIEW_ALL_BROWSE_PRIV_ID = "nx-repository-view-*-*-browse";

  public static final String NX_REPO_VIEW_ALL_READ_PRIV_ID = "nx-repository-view-*-*-read";

  @Override
  public MemorySecurityConfiguration getContribution() {
    MemorySecurityConfiguration configuration = new MemorySecurityConfiguration();

    configuration.addPrivilege(createWildcardPrivilege(NX_ALL_PRIV_ID, NX_ALL_PRIV_DESCRIPTION, NX_ALL_PATTERN));

    configuration.addPrivilege(
        createApplicationPrivilege(NX_SETTINGS_ALL_PRIV_ID, ALL_DESCRIPTION_BASE + capitalize(SETTINGS_DOMAIN),
            SETTINGS_DOMAIN, ACTION_ALL));
    configuration.addPrivilege(
        createApplicationPrivilege(NX_SETTINGS_READ_PRIV_ID, READ_DESCRIPTION_BASE + capitalize(SETTINGS_DOMAIN),
            SETTINGS_DOMAIN, ACTION_READ));
    configuration.addPrivilege(
        createApplicationPrivilege(NX_SETTINGS_UPDATE_PRIV_ID, UPDATE_DESCRIPTION_BASE + capitalize(SETTINGS_DOMAIN),
            SETTINGS_DOMAIN, ACTION_UPDATE));

    configuration.addPrivilege(
        createApplicationPrivilege(NX_BUNDLES_ALL_PRIV_ID, ALL_DESCRIPTION_BASE + capitalize(BUNDLES_DOMAIN),
            BUNDLES_DOMAIN, ACTION_ALL));
    configuration.addPrivilege(
        createApplicationPrivilege(NX_BUNDLES_READ_PRIV_ID, READ_DESCRIPTION_BASE + capitalize(BUNDLES_DOMAIN),
            BUNDLES_DOMAIN, ACTION_READ));

    configuration.addPrivilege(
        createApplicationPrivilege(NX_SEARCH_READ_PRIV_ID, READ_DESCRIPTION_BASE + capitalize(SEARCH_DOMAIN),
            SEARCH_DOMAIN, ACTION_READ));

    configuration.addPrivilege(
        createApplicationPrivilege(NX_APIKEY_ALL_PRIV_ID, NX_APIKEY_ALL_DESCRIPTION, APIKEY_DOMAIN, ACTION_ALL));

    createCrudAndAllApplicationPrivileges(NX_PRIVILEGE_PRIV_ID_PREFIX, NX_PRIVILEGE_DOMAIN)
        .forEach(configuration::addPrivilege);
    createCrudAndAllApplicationPrivileges(NX_ROLE_PRIV_ID_PREFIX, NX_ROLE_DOMAIN).forEach(configuration::addPrivilege);
    createCrudAndAllApplicationPrivileges(NX_USER_PRIV_ID_PREFIX, NX_USER_DOMAIN).forEach(configuration::addPrivilege);

    // FIXME: Sort out what the use-case is for this distinct permission, consider nexus:users:change-password?
    configuration.addPrivilege(
        createApplicationPrivilege(NX_USERCHANGEPW_PRIV_ID, NX_USERCHANGEPW_PRIV_DESCRIPTION, NX_USERCHANGEPW_DOMAIN,
            ACTION_CREATE));

    configuration.addPrivilege(
        createApplicationPrivilege(NX_UPLOAD_PRIV_ID, NX_UPLOAD_PRIV_DESCRIPTION, NX_UPLOAD_DOMAIN,
            ACTION_CREATE_ONLY));

    configuration.addRole(createRole(ADMIN_ROLE_ID, ADMIN_ROLE_ID, NX_ADMIN_ROLE_DESCRIPTION, NX_ALL_PRIV_ID));
    configuration.addRole(
        createRole(ANONYMOUS_ROLE_ID, ANONYMOUS_ROLE_ID, NX_ANON_ROLE_DESCRIPTION, NX_SEARCH_READ_PRIV_ID,
            NX_HEALTHCHECK_READ_PRIV_ID, NX_REPO_VIEW_ALL_BROWSE_PRIV_ID, NX_REPO_VIEW_ALL_READ_PRIV_ID));

    return configuration;
  }
}

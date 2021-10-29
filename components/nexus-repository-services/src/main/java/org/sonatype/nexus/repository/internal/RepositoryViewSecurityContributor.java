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
package org.sonatype.nexus.repository.internal;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.security.config.MemorySecurityConfiguration;
import org.sonatype.nexus.security.config.SecurityConfiguration;
import org.sonatype.nexus.security.config.SecurityContributor;
import org.sonatype.nexus.security.config.SecurityContributorSupport;
import org.sonatype.nexus.security.config.memory.MemoryCPrivilege;
import org.sonatype.nexus.security.config.memory.MemoryCPrivilege.MemoryCPrivilegeBuilder;

import static org.apache.commons.lang.StringUtils.capitalize;
import static org.sonatype.nexus.repository.security.RepositoryViewPrivilegeDescriptor.P_ACTIONS;
import static org.sonatype.nexus.repository.security.RepositoryViewPrivilegeDescriptor.P_FORMAT;
import static org.sonatype.nexus.repository.security.RepositoryViewPrivilegeDescriptor.P_REPOSITORY;
import static org.sonatype.nexus.repository.security.RepositoryViewPrivilegeDescriptor.TYPE;
import static org.sonatype.nexus.security.privilege.PrivilegeDescriptorSupport.ALL;

/**
 * Repository view security configuration.
 *
 * @since 3.0
 */
@Named
@Singleton
public class RepositoryViewSecurityContributor
    extends SecurityContributorSupport
    implements SecurityContributor
{
  public static final String REPOSITORY_VIEW_ALL_PREFIX = "nx-repository-view-*-*-";

  public static final String REPOSITORY_VIEW_ALL_DESCRIPTION_SUFFIX = " permissions for all repository views";

  public static final String ACTION_BROWSE = "browse";

  public static final String ACTION_READ = "read";

  public static final String ACTION_EDIT = "edit";

  public static final String ACTION_ADD = "add";

  public static final String ACTION_DELETE = "delete";

  @Override
  public SecurityConfiguration getContribution() {
    MemorySecurityConfiguration config = new MemorySecurityConfiguration();

    config.addPrivilege(createRepositoryViewPrivilege(ALL));
    config.addPrivilege(createRepositoryViewPrivilege(ACTION_BROWSE));
    config.addPrivilege(createRepositoryViewPrivilege(ACTION_READ));
    config.addPrivilege(createRepositoryViewPrivilege(ACTION_EDIT));
    config.addPrivilege(createRepositoryViewPrivilege(ACTION_ADD));
    config.addPrivilege(createRepositoryViewPrivilege(ACTION_DELETE));

    return config;
  }

  private MemoryCPrivilege createRepositoryViewPrivilege(
      final String action)
  {
    final String id = REPOSITORY_VIEW_ALL_PREFIX + action;
    final String description =
        (ALL.equals(action) ? "All" : capitalize(action)) + REPOSITORY_VIEW_ALL_DESCRIPTION_SUFFIX;

    return new MemoryCPrivilegeBuilder(id)
        .type(TYPE)
        .readOnly(true)
        .name(id)
        .description(description)
        .property(P_FORMAT, ALL)
        .property(P_REPOSITORY, ALL)
        .property(P_ACTIONS, action).build();
  }
}

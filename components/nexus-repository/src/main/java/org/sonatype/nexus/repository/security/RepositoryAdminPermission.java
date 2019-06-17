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
package org.sonatype.nexus.repository.security;

import java.util.Arrays;
import java.util.List;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.security.authz.WildcardPermission2;

import com.google.common.collect.ImmutableList;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Repository administration permission.
 *
 * @since 3.0
 */
public class RepositoryAdminPermission
    extends WildcardPermission2
{
  public static final String SYSTEM = "nexus";

  public static final String DOMAIN = "repository-admin";

  private final String format;

  private final String name;

  private final List<String> actions;

  public RepositoryAdminPermission(final String format, final String name, final List<String> actions) {
    this.format = checkNotNull(format);
    this.name = checkNotNull(name);
    this.actions = checkNotNull(actions);

    setParts(ImmutableList.of(SYSTEM, DOMAIN, format, name), actions);
  }

  /**
   * @since 3.17
   */
  public RepositoryAdminPermission(final Repository repository, final String... actions) {
    this(repository.getFormat().getValue(), repository.getName(), Arrays.asList(actions));
  }

  public String getFormat() {
    return format;
  }

  public String getName() {
    return name;
  }

  public List<String> getActions() {
    return actions;
  }
}

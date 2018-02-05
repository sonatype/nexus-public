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
package org.sonatype.nexus.security.authz;

import javax.inject.Named;
import javax.inject.Singleton;

import com.google.common.base.Joiner;
import org.apache.shiro.web.filter.authz.PermissionsAuthorizationFilter;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Nexus {@link PermissionsAuthorizationFilter}.
 */
@Named
@Singleton
public class PermissionsFilter
  extends PermissionsAuthorizationFilter
{
  public static final String NAME = "nx-perms";

  /**
   * Helper to build filter configuration.
   */
  public static String config(final String... permissions) {
    checkNotNull(permissions);
    checkArgument(permissions.length != 0);
    return String.format("%s[%s]", NAME, Joiner.on(",").join(permissions));
  }
}

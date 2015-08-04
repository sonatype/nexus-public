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
package org.sonatype.security.authorization;

import org.apache.shiro.authz.Permission;

/**
 * A permission factory that creates Permission instances. It may apply other stuff, like caching instances for
 * example,
 * based on permission string representation. This is just a concept to be able to hide caching of it. Which
 * implementation you use, depends on your app very much, but usually you'd want the one producing the most widely used
 * permission in Shiro: the {@link WildcardPermissionFactory}.
 *
 * @author cstamas
 * @since sonatype-security 2.8
 */
public interface PermissionFactory
{
  Permission create(final String permission);
}

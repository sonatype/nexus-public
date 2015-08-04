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

import java.util.concurrent.ConcurrentMap;

import javax.enterprise.inject.Typed;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.google.common.collect.MapMaker;
import org.apache.shiro.authz.Permission;

/**
 * Permission factory that delegates to another factory, and caches returned instances keyed by permission string
 * representation into weak map. The trick is, that permissions themself may be considered "static". The application
 * defines them upfront, and they are constant during runtime. The mapping of users to permissions (using different
 * concepts like "roles" or "groups") are volatile, but this factory has nothing to do with mapping, and even then, the
 * mapped permissions are still constants.
 *
 * @author cstamas
 * @since sonatype-security 2.8
 */
@Named("caching")
@Singleton
@Typed(PermissionFactory.class)
public class InstanceCachingPermissionFactory
    implements PermissionFactory
{
  private final ConcurrentMap<String, Permission> instances;

  private final PermissionFactory delegate;

  @Inject
  public InstanceCachingPermissionFactory(@Named("wildcard") final PermissionFactory delegate) {
    this.instances = new MapMaker().weakValues().makeMap();
    this.delegate = delegate;
  }

  @Override
  public Permission create(final String permission) {
    return getOrCreate(permission);
  }

  // ==

  protected Permission getOrCreate(final String permission) {
    Permission result = instances.get(permission);
    if (result == null) {
      Permission newPermission = delegateCreate(permission);
      result = instances.putIfAbsent(permission, newPermission);
      if (result == null) {
        // put succeeded, use new value
        result = newPermission;
      }
    }
    return result;
  }

  protected Permission delegateCreate(final String permission) {
    return delegate.create(permission);
  }
}

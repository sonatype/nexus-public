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
package org.sonatype.nexus.security.config

import javax.inject.Named
import javax.inject.Singleton

// FIXME: Perhaps this would be better in nexus-core internal.security?

/**
 * Security model configuration defaults.
 *
 * @since 3.0
 */
@Named('static')
@Singleton
class StaticSecurityConfigurationSource
    extends PreconfiguredSecurityConfigurationSource
    implements SecurityConfigurationSource
{
  StaticSecurityConfigurationSource() {
    super(new MemorySecurityConfiguration(
        users: [
            new CUser(
                id: 'admin',
                // password='admin123'
                password: '$shiro1$SHA-512$1024$NE+wqQq/TmjZMvfI7ENh/g==$V4yPw8T64UQ6GfJfxYq2hLsVrBY8D1v+bktfOxGdt4b/9BthpWPNUy/CBk6V9iA0nHpzYzJFWO8v/tZFtES8CA==',
                firstName: 'Administrator',
                lastName: 'User',
                status: 'active',
                email: 'admin@example.org'
            ),
            new CUser(
                id: 'anonymous',
                // TODO: password=???
                password: '$shiro1$SHA-512$1024$CPJm1XWdYNg5eCAYp4L4HA==$HIGwnJhC07ZpgeVblZcFRD1F6KH+xPG8t7mIcEMbfycC+n5Ljudyoj9dzdinrLmChTrmKMCw2/z29F7HeLbTbQ==',
                firstName: 'Anonymous',
                lastName: 'User',
                status: 'active',
                email: 'anonymous@example.org'
            )
        ],
        userRoleMappings: [
            new CUserRoleMapping(
                userId: 'admin',
                source: 'default',
                roles: [
                    'admin'
                ]
            ),
            new CUserRoleMapping(
                userId: 'anonymous',
                source: 'default',
                roles: [
                    'anonymous'
                ]
            )
        ],
    ))
  }
}


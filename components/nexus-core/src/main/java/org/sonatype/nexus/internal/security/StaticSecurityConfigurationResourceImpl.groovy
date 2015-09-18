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
package org.sonatype.nexus.internal.security

import javax.inject.Named
import javax.inject.Singleton

import org.sonatype.nexus.security.config.CPrivilege
import org.sonatype.nexus.security.config.CRole
import org.sonatype.nexus.security.config.MemorySecurityConfiguration
import org.sonatype.nexus.security.config.StaticSecurityConfigurationResource

/**
 * Default nexus static security resource.
 *
 * @since 3.0
 */
@Named
@Singleton
class StaticSecurityConfigurationResourceImpl
    implements StaticSecurityConfigurationResource
{
  @Override
  MemorySecurityConfiguration getConfiguration() {
    return new MemorySecurityConfiguration(
        privileges: [
            /**
             * Grants permission for anything in the 'nexus:' namespace.
             */
            new CPrivilege(
                id: 'all',
                type: 'wildcard',
                description: 'All permissions',
                properties: [
                    pattern: 'nexus:*'
                ]
            ),

            //
            // nexus:settings
            //

            new CPrivilege(
                id: 'settings-all',
                description: 'All permissions for Settings',
                type: 'application',
                properties: [
                    domain : 'settings',
                    actions: '*'
                ]
            ),
            new CPrivilege(
                id: 'settings-read',
                description: 'Read permission for Settings',
                type: 'application',
                properties: [
                    domain : 'settings',
                    actions: 'read'
                ]
            ),
            new CPrivilege(
                id: 'settings-update',
                description: 'Update permission for Settings',
                type: 'application',
                properties: [
                    domain : 'settings',
                    actions: 'update,read'
                ]
            ),

            //
            // nexus:bundles
            //

            new CPrivilege(
                id: 'bundles-all',
                description: 'All permissions for Bundles',
                type: 'application',
                properties: [
                    domain : 'bundles',
                    actions: '*'
                ]
            ),
            new CPrivilege(
                id: 'bundles-read',
                description: 'Read permission for Bundles',
                type: 'application',
                properties: [
                    domain : 'bundles',
                    actions: 'read'
                ]
            ),

            //
            // nexus:search
            //

            new CPrivilege(
                id: 'search-read',
                description: 'Read permission for Search',
                type: 'application',
                properties: [
                    domain : 'search',
                    actions: 'read'
                ]
            ),

            //
            // nexus:apikey
            //

            new CPrivilege(
                id: 'apikey-all',
                description: 'All permissions for APIKey',
                type: 'application',
                properties: [
                    domain : 'apikey',
                    actions: '*'
                ]
            ),

            //
            // nexus:privileges
            //

            new CPrivilege(
                id: 'privileges-all',
                description: 'All permissions for Privileges',
                type: 'application',
                properties: [
                    domain : 'privileges',
                    actions: '*'
                ]
            ),
            new CPrivilege(
                id: 'privileges-create',
                description: 'Create permission for Privileges',
                type: 'application',
                properties: [
                    domain : 'privileges',
                    actions: 'create,read'
                ]
            ),
            new CPrivilege(
                id: 'privileges-read',
                description: 'Read permission for Privileges',
                type: 'application',
                properties: [
                    domain : 'privileges',
                    actions: 'read'
                ]
            ),
            new CPrivilege(
                id: 'privileges-update',
                description: 'Update permission for Privileges',
                type: 'application',
                properties: [
                    domain : 'privileges',
                    actions: 'update,read'
                ]
            ),
            new CPrivilege(
                id: 'privileges-delete',
                description: 'Delete permission for Privileges',
                type: 'application',
                properties: [
                    domain : 'privileges',
                    actions: 'delete,read'
                ]
            ),

            //
            // nexus:roles
            //

            new CPrivilege(
                id: 'roles-all',
                description: 'All permissions for Roles',
                type: 'application',
                properties: [
                    domain : 'roles',
                    actions: '*'
                ]
            ),
            new CPrivilege(
                id: 'roles-create',
                description: 'Create permission for Roles',
                type: 'application',
                properties: [
                    domain : 'roles',
                    actions: 'create,read'
                ]
            ),
            new CPrivilege(
                id: 'roles-read',
                description: 'Read permission for Roles',
                type: 'application',
                properties: [
                    domain : 'roles',
                    actions: 'read'
                ]
            ),
            new CPrivilege(
                id: 'roles-update',
                description: 'Update permission for Roles',
                type: 'application',
                properties: [
                    domain : 'roles',
                    actions: 'update,read'
                ]
            ),
            new CPrivilege(
                id: 'roles-delete',
                description: 'Delete permission for Roles',
                type: 'application',
                properties: [
                    domain : 'roles',
                    actions: 'delete,read'
                ]
            ),

            //
            // nexus:users
            //

            new CPrivilege(
                id: 'users-all',
                description: 'All permissions for Users',
                type: 'application',
                properties: [
                    domain : 'users',
                    actions: '*'
                ]
            ),
            new CPrivilege(
                id: 'users-create',
                description: 'Create permission for Users',
                type: 'application',
                properties: [
                    domain : 'users',
                    actions: 'create,read'
                ]
            ),
            new CPrivilege(
                id: 'users-read',
                description: 'Read permission for Users',
                type: 'application',
                properties: [
                    domain : 'users',
                    actions: 'read'
                ]
            ),
            new CPrivilege(
                id: 'users-update',
                description: 'Update permission for Users',
                type: 'application',
                properties: [
                    domain : 'users',
                    actions: 'update,read'
                ]
            ),
            new CPrivilege(
                id: 'users-delete',
                description: 'Delete permission for Users',
                type: 'application',
                properties: [
                    domain : 'users',
                    actions: 'delete,read'
                ]
            ),

            // FIXME: Sort out what the use-case is for this distinct permission, consider nexus:users:change-password?
            new CPrivilege(
                id: 'userschangepw',
                description: 'Change password permission',
                type: 'application',
                properties: [
                    domain : 'userschangepw',
                    actions: 'create,read'
                ]
            )
        ],

        roles: [
            /**
             * Admin role grants all permissions (ie. super-user)
             */
            new CRole(
                id: 'admin',
                description: 'Administrator Role',
                privileges: [
                    'all'
                ]
            ),

            /**
             * Anonymous role grants permissions to non-authenticated users.
             */
            new CRole(
                id: 'anonymous',
                description: 'Anonymous Role',
                privileges: [
                    'search-read',
                    'repository-view-*-*-browse',
                    'repository-view-*-*-read'
                ]
            )
        ]
    )
  }
}


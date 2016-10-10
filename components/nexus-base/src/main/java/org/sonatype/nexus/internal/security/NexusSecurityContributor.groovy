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

import org.sonatype.nexus.security.Roles
import org.sonatype.nexus.security.config.CPrivilege
import org.sonatype.nexus.security.config.CRole
import org.sonatype.nexus.security.config.MemorySecurityConfiguration
import org.sonatype.nexus.security.config.SecurityContributor

/**
 * Default Nexus security configuration.
 *
 * @since 3.0
 */
@Named
@Singleton
class NexusSecurityContributor
    implements SecurityContributor
{
  @Override
  MemorySecurityConfiguration getContribution() {
    return new MemorySecurityConfiguration(
        privileges: [
            /**
             * Grants permission for anything in the 'nexus:' namespace.
             */
            new CPrivilege(
                id: 'nx-all',
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
                id: 'nx-settings-all',
                description: 'All permissions for Settings',
                type: 'application',
                properties: [
                    domain : 'settings',
                    actions: '*'
                ]
            ),
            new CPrivilege(
                id: 'nx-settings-read',
                description: 'Read permission for Settings',
                type: 'application',
                properties: [
                    domain : 'settings',
                    actions: 'read'
                ]
            ),
            new CPrivilege(
                id: 'nx-settings-update',
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
                id: 'nx-bundles-all',
                description: 'All permissions for Bundles',
                type: 'application',
                properties: [
                    domain : 'bundles',
                    actions: '*'
                ]
            ),
            new CPrivilege(
                id: 'nx-bundles-read',
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
                id: 'nx-search-read',
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
                id: 'nx-apikey-all',
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
                id: 'nx-privileges-all',
                description: 'All permissions for Privileges',
                type: 'application',
                properties: [
                    domain : 'privileges',
                    actions: '*'
                ]
            ),
            new CPrivilege(
                id: 'nx-privileges-create',
                description: 'Create permission for Privileges',
                type: 'application',
                properties: [
                    domain : 'privileges',
                    actions: 'create,read'
                ]
            ),
            new CPrivilege(
                id: 'nx-privileges-read',
                description: 'Read permission for Privileges',
                type: 'application',
                properties: [
                    domain : 'privileges',
                    actions: 'read'
                ]
            ),
            new CPrivilege(
                id: 'nx-privileges-update',
                description: 'Update permission for Privileges',
                type: 'application',
                properties: [
                    domain : 'privileges',
                    actions: 'update,read'
                ]
            ),
            new CPrivilege(
                id: 'nx-privileges-delete',
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
                id: 'nx-roles-all',
                description: 'All permissions for Roles',
                type: 'application',
                properties: [
                    domain : 'roles',
                    actions: '*'
                ]
            ),
            new CPrivilege(
                id: 'nx-roles-create',
                description: 'Create permission for Roles',
                type: 'application',
                properties: [
                    domain : 'roles',
                    actions: 'create,read'
                ]
            ),
            new CPrivilege(
                id: 'nx-roles-read',
                description: 'Read permission for Roles',
                type: 'application',
                properties: [
                    domain : 'roles',
                    actions: 'read'
                ]
            ),
            new CPrivilege(
                id: 'nx-roles-update',
                description: 'Update permission for Roles',
                type: 'application',
                properties: [
                    domain : 'roles',
                    actions: 'update,read'
                ]
            ),
            new CPrivilege(
                id: 'nx-roles-delete',
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
                id: 'nx-users-all',
                description: 'All permissions for Users',
                type: 'application',
                properties: [
                    domain : 'users',
                    actions: '*'
                ]
            ),
            new CPrivilege(
                id: 'nx-users-create',
                description: 'Create permission for Users',
                type: 'application',
                properties: [
                    domain : 'users',
                    actions: 'create,read'
                ]
            ),
            new CPrivilege(
                id: 'nx-users-read',
                description: 'Read permission for Users',
                type: 'application',
                properties: [
                    domain : 'users',
                    actions: 'read'
                ]
            ),
            new CPrivilege(
                id: 'nx-users-update',
                description: 'Update permission for Users',
                type: 'application',
                properties: [
                    domain : 'users',
                    actions: 'update,read'
                ]
            ),
            new CPrivilege(
                id: 'nx-users-delete',
                description: 'Delete permission for Users',
                type: 'application',
                properties: [
                    domain : 'users',
                    actions: 'delete,read'
                ]
            ),

            // FIXME: Sort out what the use-case is for this distinct permission, consider nexus:users:change-password?
            new CPrivilege(
                id: 'nx-userschangepw',
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
                id: Roles.ADMIN_ROLE_ID,
                description: 'Administrator Role',
                privileges: [
                    'nx-all'
                ]
            ),

            /**
             * Anonymous role grants permissions to non-authenticated users.
             */
            new CRole(
                id: Roles.ANONYMOUS_ROLE_ID,
                description: 'Anonymous Role',
                privileges: [
                    'nx-search-read',
                    'nx-repository-view-*-*-browse',
                    'nx-repository-view-*-*-read'
                ]
            )
        ]
    )
  }
}


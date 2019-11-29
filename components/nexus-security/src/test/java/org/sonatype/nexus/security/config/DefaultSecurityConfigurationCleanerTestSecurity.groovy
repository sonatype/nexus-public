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

import org.sonatype.nexus.security.config.memory.MemoryCPrivilege
import org.sonatype.nexus.security.config.memory.MemoryCRole
import org.sonatype.nexus.security.config.memory.MemoryCUser
import org.sonatype.nexus.security.config.memory.MemoryCUserRoleMapping

/**
 * @since 3.0
 */
class DefaultSecurityConfigurationCleanerTestSecurity
{

  static MemorySecurityConfiguration securityModel() {
    return new MemorySecurityConfiguration(
        users: [
            new MemoryCUser(
                id: 'user1',
                password: 'user1',
                firstName: 'first1',
                lastName: 'last1',
                status: 'active',
                email: 'email'
            ),
            new MemoryCUser(
                id: 'user2',
                password: 'user2',
                firstName: 'first2',
                lastName: 'last2',
                status: 'active',
                email: 'email'
            ),
            new MemoryCUser(
                id: 'user3',
                password: 'user3',
                firstName: 'first3',
                lastName: 'last3',
                status: 'active',
                email: 'email'
            ),
            new MemoryCUser(
                id: 'user4',
                password: 'user4',
                firstName: 'first4',
                lastName: 'last4',
                status: 'active',
                email: 'email'
            ),
            new MemoryCUser(
                id: 'user5',
                password: 'user5',
                firstName: 'first5',
                lastName: 'last5',
                status: 'active',
                email: 'email'
            )
        ],
        userRoleMappings: [
            new MemoryCUserRoleMapping(
                userId: 'user1',
                source: 'default',
                roles: ['role1']
            ),
            new MemoryCUserRoleMapping(
                userId: 'user2',
                source: 'default',
                roles: ['role1', 'role2']
            ),
            new MemoryCUserRoleMapping(
                userId: 'user3',
                source: 'default',
                roles: ['role1', 'role2', 'role3']
            ),
            new MemoryCUserRoleMapping(
                userId: 'user4',
                source: 'default',
                roles: ['role1', 'role2', 'role3', 'role4']
            ),
            new MemoryCUserRoleMapping(
                userId: 'user5',
                source: 'default',
                roles: ['role1', 'role2', 'role3', 'role4', 'role5']
            )
        ],
        privileges: [
            new MemoryCPrivilege(
                id: 'priv1',
                type: 'method',
                name: 'priv1',
                description: '',
                properties: [
                    'method': 'read',
                    'permission': '/some/path/'
                ]
            ),
            new MemoryCPrivilege(
                id: 'priv2',
                type: 'method',
                name: 'priv2',
                description: '',
                properties: [
                    'method': 'read',
                    'permission': '/some/path/'
                ]
            ),
            new MemoryCPrivilege(
                id: 'priv3',
                type: 'method',
                name: 'priv3',
                description: '',
                properties: [
                    'method': 'read',
                    'permission': '/some/path/'
                ]
            ),
            new MemoryCPrivilege(
                id: 'priv4',
                type: 'method',
                name: 'priv4',
                description: '',
                properties: [
                    'method': 'read',
                    'permission': '/some/path/'
                ]
            ),
            new MemoryCPrivilege(
                id: 'priv5',
                type: 'method',
                name: 'priv5',
                description: '',
                properties: [
                    'method': 'read',
                    'permission': '/some/path/'
                ]
            )
        ],
        roles: [
            new MemoryCRole(
                id: 'role1',
                name: 'role1',
                description: 'role1',
                privileges: ['priv1'],
                roles: ['role2', 'role3', 'role4', 'role5']
            ),
            new MemoryCRole(
                id: 'role2',
                name: 'role2',
                description: 'role2',
                privileges: ['priv1', 'priv2'],
                roles: ['role3', 'role4', 'role5']
            ),
            new MemoryCRole(
                id: 'role3',
                name: 'role3',
                description: 'role3',
                privileges: ['priv1', 'priv2', 'priv3'],
                roles: ['role4', 'role5']
            ),
            new MemoryCRole(
                id: 'role4',
                name: 'role4',
                description: 'role4',
                privileges: ['priv1', 'priv2', 'priv3', 'priv4'],
                roles: ['role5']
            ),
            new MemoryCRole(
                id: 'role5',
                name: 'role5',
                description: 'role5',
                privileges: ['priv1', 'priv2', 'priv3', 'priv4', 'priv5']
            )
        ]
    )
  }

}


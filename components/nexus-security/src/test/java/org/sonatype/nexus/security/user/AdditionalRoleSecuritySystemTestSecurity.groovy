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
package org.sonatype.nexus.security.user

import org.sonatype.nexus.security.config.CPrivilege
import org.sonatype.nexus.security.config.CRole
import org.sonatype.nexus.security.config.CUser
import org.sonatype.nexus.security.config.CUserRoleMapping
import org.sonatype.nexus.security.config.MemorySecurityConfiguration

/**
 * @since 3.0
 */
class AdditionalRoleSecuritySystemTestSecurity
{

  static MemorySecurityConfiguration securityModel() {
    return new MemorySecurityConfiguration(
        users: [
            new CUser(
                id: 'admin',
                password: 'f865b53623b121fd34ee5426c792e5c33af8c227',
                firstName: 'Administrator',
                status: 'active',
                email: 'admin@example.org'
            ),
            new CUser(
                id: 'test-user',
                password: 'b2a0e378437817cebdf753d7dff3dd75483af9e0',
                firstName: 'Test User',
                status: 'active',
                email: 'test-user@example.org'
            ),
            new CUser(
                id: 'anonymous',
                password: '0a92fab3230134cca6eadd9898325b9b2ae67998',
                firstName: 'Anonynmous User',
                status: 'active',
                email: 'anonymous@example.org'
            )
        ],
        userRoleMappings: [
            new CUserRoleMapping(
                userId: 'other-user',
                source: 'default',
                roles: ['Role2', 'Role3']
            ),
            new CUserRoleMapping(
                userId: 'admin',
                source: 'default',
                roles: ['Role1']
            ),
            new CUserRoleMapping(
                userId: 'test-user',
                source: 'default',
                roles: ['Role1', 'Role2']
            ),
            new CUserRoleMapping(
                userId: 'anonymous',
                source: 'default',
                roles: ['Role2']
            ),
            new CUserRoleMapping(
                userId: 'jcoder',
                source: 'MockUserManagerA',
                roles: ['Role1']
            ),
            new CUserRoleMapping(
                userId: 'dknudsen',
                source: 'MockUserManagerA',
                roles: ['Role2']
            )
        ],
        privileges: [
            new CPrivilege(
                id: '1',
                type: 'method',
                name: '1',
                description: '',
                properties: [
                    'method': 'read',
                    'permission': '/some/path/'
                ]
            ),
            new CPrivilege(
                id: '2',
                type: 'method',
                name: '2',
                description: '',
                properties: [
                    'method': 'read',
                    'permission': '/some/path/'
                ]
            ),
            new CPrivilege(
                id: '3',
                type: 'method',
                name: '3',
                description: '',
                properties: [
                    'method': 'read',
                    'permission': '/some/path/'
                ]
            ),
            new CPrivilege(
                id: '4',
                type: 'method',
                name: '4',
                description: '',
                properties: [
                    'method': 'read',
                    'permission': '/some/path/'
                ]
            )
        ],
        roles: [
            new CRole(
                id: 'Role1',
                name: 'RoleOne',
                description: 'Role One',
                privileges: ['1', '2']
            ),
            new CRole(
                id: 'Role2',
                name: 'RoleTwo',
                description: 'Role Two',
                privileges: ['3', '4']
            ),
            new CRole(
                id: 'Role3',
                name: 'RoleThree',
                description: 'Role Three',
                privileges: ['1', '4'],
                roles: ['Role1']
            )
        ]
    )
  }

}


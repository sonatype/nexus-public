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

import javax.inject.Singleton

/**
 * @since 3.0
 */
@Singleton
class StaticSecurityConfigurationResource1
implements StaticSecurityConfigurationResource
{
  @Override
  MemorySecurityConfiguration getConfiguration() {
    return new MemorySecurityConfiguration(
        privileges: [
            new CPrivilege(
                id: 'priv1',
                type: 'method',
                name: 'priv1',
                description: '',
                properties: [
                    'method': 'read',
                    'permission': 'priv1-ONE'
                ])
            ,
            new CPrivilege(
                id: 'priv2',
                type: 'method',
                name: 'priv2',
                description: '',
                properties: [
                    'method': 'read',
                    'permission': 'priv2-TWO'
                ])
            ,
            new CPrivilege(
                id: 'priv3',
                type: 'method',
                name: 'priv3',
                description: '',
                properties: [
                    'method': 'read',
                    'permission': 'priv3-THREE'
                ])
            ,
            new CPrivilege(
                id: 'priv4',
                type: 'method',
                name: 'priv4',
                description: '',
                properties: [
                    'method': 'read',
                    'permission': 'priv4-FOUR'
                ])
            ,
            new CPrivilege(
                id: 'priv5',
                type: 'method',
                name: 'priv5',
                description: '',
                properties: [
                    'method': 'read',
                    'permission': 'priv5-FIVE'
                ])
        ],
        roles: [
            new CRole(
                id: 'anon',
                name: 'Test Anon Role',
                description: 'Test Anon Role Description',
                privileges: ['priv1'],
                roles: ['role2']
            )
            ,
            new CRole(
                id: 'other',
                name: '',
                description: '',
                privileges: ['priv2'],
                roles: ['role2']
            )
            ,
            new CRole(
                id: 'role1',
                name: 'role1',
                description: 'role1',
                privileges: ['priv1'],
                roles: ['role2']
            )
            ,
            new CRole(
                id: 'role2',
                name: 'role2',
                description: 'role2',
                privileges: ['priv1', 'priv2'],
                roles: ['role3', 'role4', 'role5']
            )
            ,
            new CRole(
                id: 'role3',
                name: 'role3',
                description: 'role3',
                privileges: ['priv1', 'priv2', 'priv3'],
                roles: ['role4', 'role5']
            )
            ,
            new CRole(
                id: 'role4',
                name: 'role4',
                description: 'role4',
                privileges: ['priv1', 'priv2', 'priv3', 'priv4']
            )
            ,
            new CRole(
                id: 'role5',
                name: 'role5',
                description: 'role5',
                privileges: ['priv3', 'priv4', 'priv5']
            )
        ]
    )
  }
}


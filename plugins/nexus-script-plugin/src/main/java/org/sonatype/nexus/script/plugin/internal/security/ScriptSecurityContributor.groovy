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
package org.sonatype.nexus.script.plugin.internal.security

import javax.inject.Named
import javax.inject.Singleton

import org.sonatype.nexus.security.config.CPrivilege
import org.sonatype.nexus.security.config.MemorySecurityConfiguration
import org.sonatype.nexus.security.config.SecurityContributor

import groovy.transform.CompileStatic

/**
 * Script security configuration.
 *
 * @since 3.0
 */
@Named
@Singleton
@CompileStatic
class ScriptSecurityContributor
    implements SecurityContributor
{
  @Override
  MemorySecurityConfiguration getContribution() {
    def configuration = new MemorySecurityConfiguration()
    configuration.setPrivileges([
        new CPrivilege(
            id: 'nx-script-*-*',
            description: 'All permissions for Scripts',
            type: 'script',
            properties: [
                name : '*',
                actions: '*'
            ]
        ),
        new CPrivilege(
            id: 'nx-script-*-browse',
            description: 'Browse permission for Scripts',
            type: 'script',
            properties: [
                name : '*',
                actions: 'browse,read'
            ]
        ),
        new CPrivilege(
            id: 'nx-script-*-add',
            description: 'Add permission for Scripts',
            type: 'script',
            properties: [
                name : '*',
                actions: 'add,read'
            ]
        ),
        new CPrivilege(
            id: 'nx-script-*-edit',
            description: 'Edit permission for Scripts',
            type: 'script',
            properties: [
                name : '*',
                actions: 'edit,read'
            ]
        ),
        new CPrivilege(
            id: 'nx-script-*-read',
            description: 'Read permission for Scripts',
            type: 'script',
            properties: [
                name : '*',
                actions: 'read'
            ]
        ),
        new CPrivilege(
            id: 'nx-script-*-delete',
            description: 'Delete permission for Scripts',
            type: 'script',
            properties: [
                name : '*',
                actions: 'delete,read'
            ]
        ),
        new CPrivilege(
            id: 'nx-script-*-run',
            description: 'Run permission for Scripts',
            type: 'script',
            properties: [
                name : '*',
                actions: 'run'
            ]
        )
    ])
    return configuration
  }
}

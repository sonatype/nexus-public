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
package org.sonatype.nexus.internal.blobstore

import javax.inject.Named
import javax.inject.Singleton

import org.sonatype.nexus.security.config.CPrivilege
import org.sonatype.nexus.security.config.MemorySecurityConfiguration
import org.sonatype.nexus.security.config.StaticSecurityConfigurationResource

/**
 * Blobstore static security configuration.
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
            new CPrivilege(
                id: 'blobstores-all',
                description: 'All permissions for Blobstores',
                type: 'application',
                properties: [
                    domain : 'blobstores',
                    actions: '*'
                ]
            ),
            new CPrivilege(
                id: 'blobstores-create',
                description: 'Create permission for Blobstores',
                type: 'application',
                properties: [
                    domain : 'blobstores',
                    actions: 'create,read'
                ]
            ),
            new CPrivilege(
                id: 'blobstores-read',
                description: 'Read permission for Blobstores',
                type: 'application',
                properties: [
                    domain : 'blobstores',
                    actions: 'read'
                ]
            ),
            new CPrivilege(
                id: 'blobstores-delete',
                description: 'Delete permission for Blobstores',
                type: 'application',
                properties: [
                    domain : 'blobstores',
                    actions: 'delete,read'
                ]
            )
        ]
    )
  }
}


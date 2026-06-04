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

import React from 'react';
import { Box, Flex, Text } from '@radix-ui/themes';
import { SettingsFormSection } from '../../../../shared/form';
import { Role, formatRoleSourceDisplay } from './types';

interface RoleOverviewProps {
  role: Role;
}

/**
 * RoleOverview - Displays basic metadata for a role in the profile view.
 */
export function RoleOverview({ role }: RoleOverviewProps) {
  return (
    <Box p="4" className="role-overview">
      <SettingsFormSection title="Role Information" defaultOpen>
        <Box className="role-detail__info">
          <Flex className="role-detail__row" mb="3">
            <Text size="2" weight="medium" style={{ width: '120px', color: 'var(--gray-11)' }}>ID</Text>
            <Text size="2">{role.id}</Text>
          </Flex>
          <Flex className="role-detail__row" mb="3">
            <Text size="2" weight="medium" style={{ width: '120px', color: 'var(--gray-11)' }}>Name</Text>
            <Text size="2">{role.name}</Text>
          </Flex>
          <Flex className="role-detail__row" mb="3">
            <Text size="2" weight="medium" style={{ width: '120px', color: 'var(--gray-11)' }}>Description</Text>
            <Text size="2">{role.description || '—'}</Text>
          </Flex>
          <Flex className="role-detail__row" mb="3">
            <Text size="2" weight="medium" style={{ width: '120px', color: 'var(--gray-11)' }}>Source</Text>
            <Text size="2">{formatRoleSourceDisplay(role.source)}</Text>
          </Flex>
        </Box>
      </SettingsFormSection>

      <SettingsFormSection title="Direct Assignments">
        <Flex direction="column" gap="4">
          <Box>
            <Text size="2" weight="medium" mb="2" display="block">Direct Privileges ({role.privileges?.length || 0})</Text>
            <Flex direction="column" gap="1">
              {role.privileges && role.privileges.length > 0 ? (
                role.privileges.map((priv) => (
                  <Text key={priv} size="2" p="2" style={{ background: 'var(--gray-3)', borderRadius: 'var(--radius-2)', fontFamily: 'var(--font-mono)' }}>
                    {priv}
                  </Text>
                ))
              ) : (
                <Text size="2" color="gray" style={{ fontStyle: 'italic' }}>No direct privileges assigned.</Text>
              )}
            </Flex>
          </Box>

          <Box>
            <Text size="2" weight="medium" mb="2" display="block">Contained Roles ({role.roles?.length || 0})</Text>
            <Flex direction="column" gap="1">
              {role.roles && role.roles.length > 0 ? (
                role.roles.map((r) => (
                  <Text key={r} size="2" p="2" style={{ background: 'var(--gray-3)', borderRadius: 'var(--radius-2)', fontFamily: 'var(--font-mono)' }}>
                    {r}
                  </Text>
                ))
              ) : (
                <Text size="2" color="gray" style={{ fontStyle: 'italic' }}>No contained roles.</Text>
              )}
            </Flex>
          </Box>
        </Flex>
      </SettingsFormSection>
    </Box>
  );
}

export default RoleOverview;

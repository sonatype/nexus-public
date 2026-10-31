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
import { Flex, Text } from '@radix-ui/themes';
import { Loader2 } from 'lucide-react';

import { getSourceLabel } from '../users/types';
import { ROLE_DETAIL } from './RoleDetailStrings';
import type { useRoleEffectivePrivileges } from './useRoleEffectivePrivileges';
import type { useAssignedUsers } from './useAssignedUsers';

// Read-only chip list used by both edit and view surfaces to render a set of
// privilege / role ids as monospace chips, with an italic empty label.

export interface IdChipListProps {
  ids: string[];
  emptyLabel: string;
}

export function IdChipList({ ids, emptyLabel }: IdChipListProps) {
  if (!ids || ids.length === 0) {
    return (
      <Text size="2" color="gray" style={{ fontStyle: 'italic' }}>
        {emptyLabel}
      </Text>
    );
  }
  return (
    <Flex direction="column" gap="2">
      {ids.map((id) => (
        <Text
          key={id}
          size="2"
          p="2"
          style={{
            background: 'var(--gray-3)',
            borderRadius: 'var(--radius-2)',
            fontFamily: 'var(--font-mono)',
          }}
        >
          {id}
        </Text>
      ))}
    </Flex>
  );
}

export interface EffectivePermissionsSectionProps {
  privileges: ReturnType<typeof useRoleEffectivePrivileges>['privileges'];
  roleMap: ReturnType<typeof useRoleEffectivePrivileges>['roleMap'];
  loading: boolean;
  error: string | null;
}

export function EffectivePermissionsSection({
  privileges,
  roleMap,
  loading,
  error,
}: EffectivePermissionsSectionProps) {
  if (loading) {
    return (
      <Flex align="center" gap="2" py="3">
        <Loader2 size={16} className="role-detail__spinner" />
        <Text size="2">{ROLE_DETAIL.SECTION_EFFECTIVE.LOADING}</Text>
      </Flex>
    );
  }
  if (error) {
    return (
      <Text size="2" color="red">
        {error}
      </Text>
    );
  }
  if (privileges.length === 0) {
    return (
      <Text size="2" color="gray" style={{ fontStyle: 'italic' }}>
        {ROLE_DETAIL.SECTION_EFFECTIVE.EMPTY}
      </Text>
    );
  }
  return (
    <Flex direction="column" gap="2" data-testid="role-detail-effective-privileges">
      <Text size="2" color="gray">
        {ROLE_DETAIL.SECTION_EFFECTIVE.COUNT(privileges.length)}
      </Text>
      {privileges.map((p) => {
        const grantedBy = p.grantedByRoleIds
          .map((rid) => roleMap.get(rid)?.name ?? rid)
          .join(', ');
        return (
          <Flex key={p.id} direction="column" gap="1" className="role-detail__effective-item">
            <Text size="2" style={{ fontFamily: 'var(--font-mono)' }}>
              {p.name ?? p.id}
            </Text>
            <Text size="1" color="gray">
              {ROLE_DETAIL.SECTION_EFFECTIVE.GRANTED_BY(grantedBy)}
            </Text>
          </Flex>
        );
      })}
    </Flex>
  );
}

export interface AssignedUsersSectionProps {
  users: ReturnType<typeof useAssignedUsers>['users'];
  loading: boolean;
  error: string | null;
}

export function AssignedUsersSection({ users, loading, error }: AssignedUsersSectionProps) {
  if (loading) {
    return (
      <Flex align="center" gap="2" py="3">
        <Loader2 size={16} className="role-detail__spinner" />
        <Text size="2">{ROLE_DETAIL.SECTION_ASSIGNED_USERS.LOADING}</Text>
      </Flex>
    );
  }
  if (error) {
    return (
      <Text size="2" color="red">
        {error}
      </Text>
    );
  }
  if (users.length === 0) {
    return (
      <Text size="2" color="gray" style={{ fontStyle: 'italic' }}>
        {ROLE_DETAIL.SECTION_ASSIGNED_USERS.EMPTY}
      </Text>
    );
  }
  return (
    <Flex direction="column" gap="2" data-testid="role-detail-assigned-users">
      <Text size="2" color="gray">
        {ROLE_DETAIL.SECTION_ASSIGNED_USERS.COUNT(users.length)}
      </Text>
      {users.map((u) => (
        <Flex
          key={`${u.source}:${u.userId}`}
          direction="column"
          gap="1"
          className="role-detail__assigned-item"
        >
          <Text size="2" weight="medium">
            {u.userId}
          </Text>
          <Text size="1" color="gray">
            {u.firstName} {u.lastName} ({getSourceLabel(u.source)})
          </Text>
        </Flex>
      ))}
    </Flex>
  );
}

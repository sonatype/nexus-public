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
import { Loader2, AlertTriangle } from 'lucide-react';

import {
  SettingsFormSection,
  SettingsButton,
  SettingsAlert,
} from '../../../shared/form';
import { RoleForm } from './RoleForm';
import { useRolesApi } from './useRolesApi';
import {
  Role,
  RoleFormData,
  RoleDetailProps,
  NEXUS_SOURCE,
  isReadOnlyRole,
  formatRoleSourceDisplay,
} from './types';

import './RoleDetail.scss';

/**
 * RoleDetail - Detailed view and edit form for a single role
 */
export function RoleDetail({
  role,
  loading,
  canEdit,
  canDelete,
  onDelete,
  onCancel,
  onComplete,
  error,
}: RoleDetailProps) {
  const { loading: apiLoading, error: apiError, setError } = useRolesApi();

  const isReadOnly = role ? isReadOnlyRole(role) : false;
  const showDeleteButton = canDelete && role && !isReadOnly;

  // Loading state
  if (loading) {
    return (
      <Flex align="center" justify="center" className="role-detail__loading">
        <Loader2 size={24} className="role-detail__spinner" />
        <Text size="2">Loading role details...</Text>
      </Flex>
    );
  }

  // No role found
  if (!role) {
    return (
      <Box className="role-detail__not-found">
        <AlertTriangle size={24} />
        <Text size="2">Role not found</Text>
      </Box>
    );
  }

  // Read-only view for users without edit permission or for read-only roles
  if (!canEdit || isReadOnly) {
    return (
      <Box className="role-detail">
        {/* Error Messages */}
        {apiError && (
          <SettingsAlert type="error" onClose={() => setError(null)}>
            {apiError}
          </SettingsAlert>
        )}

        {isReadOnly && (
          <Box className="role-detail__readonly-banner">
            <SettingsAlert type="info">
              This role is read-only and cannot be modified.
            </SettingsAlert>
          </Box>
        )}

        <SettingsFormSection title="Role Information" defaultOpen>
          <Box className="role-detail__info">
            <Flex className="role-detail__row">
              <Text size="2" weight="medium" className="role-detail__label">ID</Text>
              <Text size="2">{role.id}</Text>
            </Flex>
            <Flex className="role-detail__row">
              <Text size="2" weight="medium" className="role-detail__label">Name</Text>
              <Text size="2">{role.name}</Text>
            </Flex>
            <Flex className="role-detail__row">
              <Text size="2" weight="medium" className="role-detail__label">Description</Text>
              <Text size="2">{role.description || '-'}</Text>
            </Flex>
            <Flex className="role-detail__row">
              <Text size="2" weight="medium" className="role-detail__label">Source</Text>
              <Text size="2">{formatRoleSourceDisplay(role.source)}</Text>
            </Flex>
          </Box>
        </SettingsFormSection>

        <SettingsFormSection title="Privileges">
          <Box className="role-detail__list">
            {role.privileges && role.privileges.length > 0 ? (
              role.privileges.map((priv) => (
                <Text key={priv} size="2" className="role-detail__list-item">
                  {priv}
                </Text>
              ))
            ) : (
              <Text size="2" className="role-detail__empty">
                No privileges assigned
              </Text>
            )}
          </Box>
        </SettingsFormSection>

        <SettingsFormSection title="Contained Roles">
          <Box className="role-detail__list">
            {role.roles && role.roles.length > 0 ? (
              role.roles.map((r) => (
                <Text key={r} size="2" className="role-detail__list-item">
                  {r}
                </Text>
              ))
            ) : (
              <Text size="2" className="role-detail__empty">
                No contained roles
              </Text>
            )}
          </Box>
        </SettingsFormSection>

      </Box>
    );
  }

  // Edit view
  return (
    <Box className="role-detail">
      {/* Error Messages */}
      {apiError && (
        <SettingsAlert type="error" onClose={() => setError(null)}>
          {apiError}
        </SettingsAlert>
      )}

      {/* Role Form */}
      <RoleForm
        role={role}
        isCreate={false}
        onCancel={onCancel}
        onDelete={showDeleteButton ? onDelete : undefined}
        onComplete={onComplete}
        loading={loading || apiLoading}
        error={error}
      />
    </Box>
  );
}

export default RoleDetail;



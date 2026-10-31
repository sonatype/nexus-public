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

import { EditableRoleDetail } from './EditableRoleDetail';
import { ReadOnlyRoleDetail } from './ReadOnlyRoleDetail';
import { ROLE_DETAIL } from './RoleDetailStrings';
import type { RoleDetailProps } from './types';

import './RoleDetail.scss';

/**
 * Router-level entry for the Role detail page. Handles the loading / not-found
 * placeholders and dispatches to Editable or ReadOnly by canEdit.
 */
export function RoleDetail({
  role,
  loading,
  canEdit,
  canDelete,
  onDelete,
  onComplete,
  error,
}: RoleDetailProps) {
  if (loading && !role) {
    return (
      <Flex align="center" justify="center" className="role-detail__loading">
        <Loader2 size={24} className="role-detail__spinner" />
        <Text size="2">{ROLE_DETAIL.LOADING}</Text>
      </Flex>
    );
  }

  if (!role) {
    return (
      <Box className="role-detail__not-found">
        <AlertTriangle size={24} />
        <Text size="2">{ROLE_DETAIL.NOT_FOUND}</Text>
      </Box>
    );
  }

  return canEdit ? (
    <EditableRoleDetail
      role={role}
      canDelete={canDelete}
      onDelete={onDelete}
      onComplete={onComplete}
      error={error}
    />
  ) : (
    <ReadOnlyRoleDetail
      role={role}
      canDelete={canDelete}
      onDelete={onDelete}
      error={error}
    />
  );
}

export default RoleDetail;

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

import React, { useState, useEffect, } from 'react';
import { Box, Flex, Text, Tabs, } from '@radix-ui/themes';
import { 
  Shield, 
  Key, 
  Users, 
  Info,
} from 'lucide-react';

import {
  LoadingState,
  ErrorState,
  MetadataGrid,
} from '../../../../shared';
import { SettingsFormSection } from '../../../../shared/form';
import { useRoleTree } from './useRoleTree';
import { RoleExplorerTree } from './RoleExplorerTree';
import { CalculatedPermissions } from './CalculatedPermissions';
import { RoleAssignedUsers } from './RoleAssignedUsers';
import { useRolesApi } from './useRolesApi';
import { Role } from './types';

import './RoleProfilePage.scss';

interface RoleProfilePageProps {
  roleName: string;
  onBack: () => void;
  /** When true, cross-entity links (users) render as plain text — used in full-screen modal */
  embedMode?: boolean;
}

/**
 * RoleProfilePage - Dedicated read-only operational dashboard for a Role.
 * Follows the "Profile Pattern" established for Repositories.
 */
export function RoleProfilePage({ roleName, onBack, embedMode = false }: RoleProfilePageProps) {
  const [role, setRole] = useState<Role | null>(null);
  const [roleLoading, setRoleLoading] = useState(true);
  const [roleError, setRoleError] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState('overview');
  const [treeExpandedNodes, setTreeExpandedNodes] = useState<Set<string>>(() => new Set([roleName]));
  const [treeSearchTerm, setTreeSearchTerm] = useState('');

  useEffect(() => {
    setTreeExpandedNodes(new Set([roleName]));
  }, [roleName]);

  const { findRole } = useRolesApi();
  const { tree, effectivePrivileges, loading: treeLoading, toggleExpand, expandAll, collapseAll } = useRoleTree(roleName, {
    searchTerm: treeSearchTerm,
    initialExpandedNodes: treeExpandedNodes,
    onExpandedNodesChange: setTreeExpandedNodes,
  });

  useEffect(() => {
    const loadRole = async () => {
      setRoleLoading(true);
      try {
        const data = await findRole(roleName);
        setRole(data);
      } catch (err: any) {
        setRoleError(err.message || 'Failed to load role');
      } finally {
        setRoleLoading(false);
      }
    };

    loadRole();
  }, [roleName, findRole]);

  if (roleLoading) return <LoadingState message="Loading role profile..." />;
  if (roleError) return <ErrorState message={roleError} onRetry={() => window.location.reload()} />;
  if (!role) return <ErrorState message="Role not found" />;

  return (
    <Box className="role-profile-page">
      <Tabs.Root value={activeTab} onValueChange={setActiveTab}>
        <Tabs.List className="role-profile-page__tabs-list">
          <Tabs.Trigger value="overview">
            <Flex align="center" gap="1">
              <Info size={14} />
              <Text size="2">Overview</Text>
            </Flex>
          </Tabs.Trigger>
          <Tabs.Trigger value="explorer">
            <Flex align="center" gap="1">
              <Shield size={14} />
              <Text size="2">Security Tree</Text>
            </Flex>
          </Tabs.Trigger>
          <Tabs.Trigger value="effective">
            <Flex align="center" gap="1">
              <Key size={14} />
              <Text size="2">Effective Permissions</Text>
            </Flex>
          </Tabs.Trigger>
          <Tabs.Trigger value="users">
            <Flex align="center" gap="1">
              <Users size={14} />
              <Text size="2">Assigned Users</Text>
            </Flex>
          </Tabs.Trigger>
        </Tabs.List>

        <Box pt="4" className="role-profile-page__content">
          <Tabs.Content value="overview">
            <SettingsFormSection title="Role Metadata" defaultOpen>
              <MetadataGrid
                items={[
                  {
                    label: 'ID',
                    value: (
                      <Text size="2" style={{ fontFamily: 'var(--font-mono)' }}>
                        {role.id}
                      </Text>
                    ),
                  },
                  { label: 'Name', value: role.name },
                  { label: 'Description', value: role.description },
                ]}
              />
            </SettingsFormSection>
          </Tabs.Content>

          <Tabs.Content value="explorer">
            <Box className="role-profile-page__pane role-profile-page__pane--tree">
              <RoleExplorerTree
                tree={tree}
                loading={treeLoading}
                onToggleExpand={toggleExpand}
                onExpandAll={expandAll}
                onCollapseAll={collapseAll}
                onSearchChange={setTreeSearchTerm}
              />
            </Box>
          </Tabs.Content>

          <Tabs.Content value="effective">
            <Box className="role-profile-page__pane">
              <CalculatedPermissions
                privileges={effectivePrivileges}
                loading={treeLoading}
                linksDisabled={embedMode}
              />
            </Box>
          </Tabs.Content>

          <Tabs.Content value="users">
            <Box className="role-profile-page__pane">
              <RoleAssignedUsers roleId={roleName} />
            </Box>
          </Tabs.Content>
        </Box>
      </Tabs.Root>
    </Box>
  );
}

export default RoleProfilePage;

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

import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Badge, Box, Button, Checkbox, Flex, Heading, Spinner, Table, Text, TextField } from '@radix-ui/themes';

import { getSourceLabel } from '../../../security/users/types';
import { useToast } from '../../../../../shared';
import { WizardForm } from '../../../../../shared/form/WizardForm';

import { NEW_ROLE_ID_PATTERN } from './grantWizardMachine';
import { useGrantWizard, userDirectoryKey } from './useGrantWizard';
import type { MergedApiEndpoint } from '../utils/mergeSwaggerPermissions';

export interface GrantWizardProps {
  row: MergedApiEndpoint;
  active: boolean;
}

const WIZARD_STEPS = [
  { id: 'permissions', label: 'Permissions' },
  { id: 'role', label: 'Choose Role' },
  { id: 'users', label: 'Select Users' },
  { id: 'confirm', label: 'Confirm' },
];

function stateToStep(state: { matches: (s: string) => boolean }): number {
  if (state.matches('step1')) return 0;
  if (state.matches('step2')) return 1;
  if (state.matches('step3')) return 2;
  if (state.matches('step4') || state.matches('applying')) return 3;
  return 0;
}

export function GrantWizard({ row, active }: GrantWizardProps) {
  const {
    state,
    send,
    session,
    loadingDir,
    dirError,
    recommendedRoles,
    userCountByRoleId,
    noMappedPermissions,
    suggestedRoleId,
    suggestedRoleName,
  } = useGrantWizard(row, active);

  const toast = useToast();
  const prevDoneRef = useRef(false);

  useEffect(() => {
    const isDone = state.matches('done');
    if (isDone && !prevDoneRef.current) {
      const ctx = state.context;
      const results = ctx.applyResults;
      const successCount = results.filter((r) => r.ok).length;
      const failCount = results.length - successCount;
      const successNames = results.filter((r) => r.ok).map((r) => r.userKey.split('\0')[0]);
      const roleName = ctx.mode === 'create'
        ? (ctx.newRoleName.trim() || ctx.newRoleId.trim())
        : (session?.roles.find((r) => r.id === ctx.existingRoleId)?.name || ctx.existingRoleId || 'role');
      const userList = successNames.length <= 3
        ? successNames.join(', ')
        : `${successNames.slice(0, 3).join(', ')} and ${successNames.length - 3} more`;
      if (failCount === 0) {
        toast.success(`Role "${roleName}" granted to ${userList}`);
      } else if (successCount > 0) {
        toast.success(`Role "${roleName}" granted to ${userList} (${failCount} failed)`);
      } else {
        toast.error(`Failed to grant role "${roleName}" — see details below`);
      }
    }
    prevDoneRef.current = isDone;
  }, [state, toast, session]);

  const [userFilter, setUserFilter] = useState('');

  const permissionReqs = row.permission?.permissions ?? [];
  const logical = permissionReqs[0]?.logical === 'OR' ? 'OR' : 'AND';

  const filteredUsers = useMemo(() => {
    if (!session) {
      return [];
    }
    const q = userFilter.trim().toLowerCase();
    if (!q) {
      return session.users;
    }
    return session.users.filter((u) => {
      const blob = `${u.userId} ${u.firstName} ${u.lastName} ${u.emailAddress}`.toLowerCase();
      return blob.includes(q);
    });
  }, [session, userFilter]);

  const wizardStep = stateToStep(state);
  const ctx = state.context;

  const handleStepChange = useCallback(
    (newStep: number) => {
      if (newStep > wizardStep) {
        send({ type: 'NEXT' });
      } else if (newStep < wizardStep) {
        send({ type: 'BACK' });
      }
    },
    [wizardStep, send]
  );

  const handleComplete = useCallback(() => {
    send({ type: 'APPLY' });
  }, [send]);

  const handleCancel = useCallback(() => {
    send({ type: 'RESET' });
  }, [send]);

  const canAdvance = useMemo(() => {
    if (wizardStep === 0) return true;
    if (wizardStep === 1) {
      if (ctx.mode === 'existing') return Boolean(ctx.existingRoleId);
      const id = ctx.newRoleId.trim();
      return NEW_ROLE_ID_PATTERN.test(id) && ctx.newRoleName.trim().length > 0;
    }
    if (wizardStep === 2) return ctx.selectedUserKeys.length > 0;
    return true;
  }, [wizardStep, ctx]);

  if (!active) {
    return null;
  }

  if (noMappedPermissions || permissionReqs.length === 0) {
    return (
      <Box data-testid="api-grant-wizard" className="api-grant-wizard">
        <Text size="2" color="gray">
          Grant Access needs mapped permission strings for this operation. None are registered for this endpoint.
        </Text>
      </Box>
    );
  }

  if (loadingDir && !session) {
    return (
      <Flex align="center" gap="2" py="4" data-testid="api-grant-wizard">
        <Spinner />
        <Text size="2">Loading roles and users…</Text>
      </Flex>
    );
  }

  if (dirError) {
    return (
      <Text size="2" color="red" data-testid="api-grant-wizard">
        {dirError}
      </Text>
    );
  }

  if (!session) {
    return null;
  }

  const anonymous = row.permission && !row.permission.authenticated;

  const summaryRoleLabel =
    ctx.mode === 'create'
      ? `${ctx.newRoleName.trim() || ctx.newRoleId.trim() || '(new role)'}`
      : session.roles.find((r) => r.id === ctx.existingRoleId)?.name || ctx.existingRoleId || '';

  if (state.matches('done')) {
    return (
      <Box className="api-grant-wizard" data-testid="api-grant-wizard">
        <Heading as="h3" size="3" weight="medium" mb="2">
          Result
        </Heading>
        <Table.Root variant="surface" mb="3">
          <Table.Header>
            <Table.Row>
              <Table.ColumnHeaderCell>User</Table.ColumnHeaderCell>
              <Table.ColumnHeaderCell>Status</Table.ColumnHeaderCell>
            </Table.Row>
          </Table.Header>
          <Table.Body>
            {ctx.applyResults.map((r) => {
              const [uid] = r.userKey.split('\0');
              return (
                <Table.Row key={r.userKey}>
                  <Table.Cell>{uid}</Table.Cell>
                  <Table.Cell>
                    {r.ok ? (
                      <Text size="2" color="green">
                        Assigned
                      </Text>
                    ) : (
                      <Text size="2" color="red">
                        {r.message || 'Failed'}
                      </Text>
                    )}
                  </Table.Cell>
                </Table.Row>
              );
            })}
          </Table.Body>
        </Table.Root>
        <Flex gap="2" wrap="wrap">
          <Button variant="soft" onClick={() => send({ type: 'DONE_ANOTHER' })}>
            Grant to more users
          </Button>
          <Button onClick={() => send({ type: 'RESET' })}>Start over</Button>
        </Flex>
      </Box>
    );
  }

  return (
    <Box className="api-grant-wizard" data-testid="api-grant-wizard">
      {anonymous && (
        <Box mb="3" p="2" className="api-who-has-access-tab__banner">
          <Text size="2">
            This endpoint may allow anonymous access. Granting roles still limits access when authentication is required
            elsewhere.
          </Text>
        </Box>
      )}

      <WizardForm
        steps={WIZARD_STEPS}
        currentStep={wizardStep}
        onStepChange={handleStepChange}
        onComplete={handleComplete}
        onCancel={handleCancel}
        completeLabel="Apply"
        canAdvance={canAdvance}
        loading={state.matches('applying')}
        error={ctx.applyError || undefined}
        dirty={false}
        noDirtyTracking
        testId="api-grant-wizard"
        className="api-grant-wizard__form"
      >
        {wizardStep === 0 && (
          <Box>
            <Text size="2" color="gray" mb="2">
              This operation requires {logical === 'OR' ? 'ANY' : 'ALL'} of:
            </Text>
            <Box mb="3">
              {permissionReqs.map((p) => (
                <Text key={p.permission} size="2" as="div">
                  <Text weight="bold" as="span">
                    {p.permission}
                  </Text>
                </Text>
              ))}
            </Box>
          </Box>
        )}

        {wizardStep === 1 && (
          <Box>
            <Text size="2" color="gray" mb="3">
              Pick an existing role that already satisfies the permission{permissionReqs.length === 1 ? '' : 's'}, or
              create a dedicated role with only the needed privileges.
            </Text>

            {recommendedRoles.length > 0 && (
              <Box mb="3">
                <Text size="2" weight="medium" mb="2">
                  Recommended
                </Text>
                <Flex direction="column" gap="2">
                  {recommendedRoles.map((r, idx) => (
                    <Flex key={r.id} align="center" gap="2" wrap="wrap">
                      <Button
                        type="button"
                        variant={ctx.existingRoleId === r.id && ctx.mode === 'existing' ? 'solid' : 'outline'}
                        onClick={() => send({ type: 'SELECT_EXISTING', roleId: r.id })}
                      >
                        {r.name}
                      </Button>
                      {idx === 0 && (
                        <Badge color="green" size="1">
                          Most specific
                        </Badge>
                      )}
                      <Text size="1" color="gray">
                        {userCountByRoleId.get(r.id) ?? 0} users · {r.id}
                      </Text>
                    </Flex>
                  ))}
                </Flex>
              </Box>
            )}

            <Box mb="3">
              <Button
                type="button"
                variant={ctx.mode === 'create' ? 'solid' : 'outline'}
                onClick={() => {
                  send({ type: 'SELECT_CREATE' });
                  if (!ctx.newRoleId.trim()) {
                    send({ type: 'SET_NEW_ROLE_FIELD', field: 'newRoleId', value: suggestedRoleId });
                  }
                  if (!ctx.newRoleName.trim()) {
                    send({ type: 'SET_NEW_ROLE_FIELD', field: 'newRoleName', value: suggestedRoleName });
                  }
                }}
              >
                Create new role
              </Button>
            </Box>

            {ctx.mode === 'create' && (
              <Box mb="3" style={{ maxWidth: 480 }}>
                <Text size="2" mb="2" as="div">
                  Role ID
                </Text>
                <TextField.Root
                  mb="2"
                  value={ctx.newRoleId}
                  onChange={(e) =>
                    send({ type: 'SET_NEW_ROLE_FIELD', field: 'newRoleId', value: e.target.value })
                  }
                  aria-label="New role id"
                />
                {!NEW_ROLE_ID_PATTERN.test(ctx.newRoleId.trim()) && ctx.newRoleId.trim() !== '' && (
                  <Text size="1" color="red" mb="2">
                    Use letters, digits, periods, hyphens, and underscores only.
                  </Text>
                )}
                <Text size="2" mb="2" as="div">
                  Display name
                </Text>
                <TextField.Root
                  mb="2"
                  value={ctx.newRoleName}
                  onChange={(e) =>
                    send({ type: 'SET_NEW_ROLE_FIELD', field: 'newRoleName', value: e.target.value })
                  }
                  aria-label="New role name"
                />
                <Text size="2" mb="2" as="div">
                  Description (optional)
                </Text>
                <TextField.Root
                  value={ctx.newRoleDescription}
                  onChange={(e) =>
                    send({ type: 'SET_NEW_ROLE_FIELD', field: 'newRoleDescription', value: e.target.value })
                  }
                  aria-label="New role description"
                />
              </Box>
            )}
          </Box>
        )}

        {wizardStep === 2 && (
          <Box>
            <Text size="2" color="gray" mb="2">
              Choose one or more users to receive the role.
            </Text>
            <Box mb="2" style={{ maxWidth: 360 }}>
              <TextField.Root
                placeholder="Filter users…"
                value={userFilter}
                onChange={(e) => setUserFilter(e.target.value)}
                aria-label="Filter users for grant"
              />
            </Box>
            <Table.Root variant="surface" mb="3">
              <Table.Header>
                <Table.Row>
                  <Table.ColumnHeaderCell style={{ width: 48 }}> </Table.ColumnHeaderCell>
                  <Table.ColumnHeaderCell>User</Table.ColumnHeaderCell>
                  <Table.ColumnHeaderCell>Source</Table.ColumnHeaderCell>
                </Table.Row>
              </Table.Header>
              <Table.Body>
                {filteredUsers.map((u) => {
                  const key = userDirectoryKey(u);
                  const checked = ctx.selectedUserKeys.includes(key);
                  return (
                    <Table.Row key={key}>
                      <Table.Cell>
                        <Checkbox
                          checked={checked}
                          onCheckedChange={() => send({ type: 'TOGGLE_USER', userKey: key })}
                          aria-label={`Select user ${u.userId}`}
                        />
                      </Table.Cell>
                      <Table.Cell>
                        <Text size="2">{u.userId}</Text>
                        <Text size="1" color="gray" as="div">
                          {u.firstName} {u.lastName}
                        </Text>
                      </Table.Cell>
                      <Table.Cell>
                        <Text size="2">{getSourceLabel(u.source)}</Text>
                      </Table.Cell>
                    </Table.Row>
                  );
                })}
              </Table.Body>
            </Table.Root>
          </Box>
        )}

        {wizardStep === 3 && (
          <Box>
            <Text size="2" mb="3">
              {ctx.mode === 'create' ? (
                <>
                  Create role <Text weight="bold">{ctx.newRoleId.trim()}</Text> ({summaryRoleLabel}) with the mapped
                  privileges, then assign it to {ctx.selectedUserKeys.length} user(s).
                </>
              ) : (
                <>
                  Assign role <Text weight="bold">{summaryRoleLabel}</Text> to {ctx.selectedUserKeys.length} user(s).
                </>
              )}
            </Text>
          </Box>
        )}
      </WizardForm>
    </Box>
  );
}

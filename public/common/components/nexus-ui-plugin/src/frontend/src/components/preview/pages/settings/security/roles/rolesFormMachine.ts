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

import { assign } from 'xstate';
import { ENDPOINTS, restClient } from '../../../../../../interface/api';
import { createFormMachine, type FormContext, type ValidationErrors } from '../../../../../../interface/form';

import {
  Role,
  RoleFormData,
  RoleReference,
  PrivilegeReference,
  RoleSource,
  NEXUS_SOURCE,
  DEFAULT_SOURCE,
} from './types';

/**
 * REST API role shape (from REST API response)
 */
interface RestRole {
  id: string;
  source: string;
  name: string;
  description: string;
  readOnly: boolean;
  privileges: string[];
  roles: string[];
}

/**
 * Convert REST role to UI Role model
 */
function restToRole(rest: RestRole): Role {
  return {
    id: rest.id,
    version: '1',
    source: rest.source === DEFAULT_SOURCE ? 'Default' : rest.source,
    name: rest.name,
    description: rest.description || '',
    readOnly: rest.readOnly ?? false,
    privileges: rest.privileges || [],
    roles: rest.roles || [],
  };
}

/**
 * Find a role by ID and transform to UI model
 */
async function findRole(id: string): Promise<Role | null> {
  try {
    const data = await restClient.get<RestRole>(`${ENDPOINTS.ROLES}/${encodeURIComponent(id)}`);
    return restToRole(data);
  } catch (err) {
    console.error('Failed to load role:', err);
    return null;
  }
}

/**
 * Validate role form data.
 * Returns an object with field names as keys and error messages as values.
 */
function validateRole(
  data: RoleFormData,
  existingRoles: RoleReference[] = [],
  currentRoleId?: string
): ValidationErrors {
  const errors: ValidationErrors = {};

  if (!data.id?.trim()) {
    errors.id = 'Role ID is required';
  } else if (!/^[a-zA-Z0-9_-]+$/.test(data.id)) {
    errors.id = 'Role ID can only contain letters, numbers, underscores, and hyphens';
  }

  if (!data.name?.trim()) {
    errors.name = 'Role name is required';
  } else {
    const duplicate = existingRoles.find(
      (r) => r.name.toLowerCase() === data.name.trim().toLowerCase() && r.id !== currentRoleId
    );
    if (duplicate) {
      errors.name = `A role named "${duplicate.name}" already exists (ID: ${duplicate.id})`;
    }
  }

  if (
    (!data.privileges || data.privileges.length === 0) &&
    (!data.roles || data.roles.length === 0)
  ) {
    errors.privileges = 'Select at least one privilege or contained role';
  }

  return errors;
}

/**
 * Create a roles form machine with XState.
 * Uses createFormMachine with no editingConfig since roles don't have type variants.
 *
 * This factory creates a machine configured for create or edit mode.
 * The machine loads both the role being edited (if roleId provided) and
 * reference data (available privileges and roles).
 */
export function createRolesFormMachine(
  roleId: string | undefined,
  preloadedRole?: Role
) {
  return createFormMachine({
    id: `roles-form-${roleId ?? 'new'}`,
    context: {
      data: {
        id: '',
        name: '',
        description: '',
        privileges: [] as string[],
        roles: [] as string[],
      } as RoleFormData,
      // Initialize reference data as empty - populated by load service
      role: preloadedRole ?? (null as Role | null),
      allPrivileges: [] as PrivilegeReference[],
      allRoles: [] as RoleReference[],
      allSources: [] as RoleSource[],
    },
    actions: {
      validate: assign((ctx: FormContext<RoleFormData>) => {
        const { allRoles, role } = ctx as any;
        return {
          validationErrors: validateRole(ctx.data, allRoles || [], role?.id ?? roleId),
        };
      }),
    },
    services: {
      load: async () => {
        // Load role and reference data in parallel
        const [role, privileges, roles, sources] = await Promise.all([
          // If role is preloaded, use it; otherwise fetch if roleId is provided
          preloadedRole
            ? Promise.resolve(preloadedRole)
            : roleId
            ? findRole(roleId)
            : Promise.resolve(null),
          restClient
            .get(ENDPOINTS.PRIVILEGES)
            .then((data: unknown) => {
              const arr = data as Array<{ name: string; description?: string }>;
              return arr.map((p) => ({ id: p.name, name: p.name, description: p.description }));
            })
            .catch((err: unknown) => {
              console.warn('Could not load privileges for role form:', err);
              return [] as PrivilegeReference[];
            }),
          restClient
            .get(`${ENDPOINTS.ROLES}?source=${DEFAULT_SOURCE}`)
            .then((data: unknown) => {
              const arr = data as RestRole[];
              return arr.map((r) => ({ id: r.id, name: r.name }));
            })
            .catch((err: unknown) => {
              console.warn('Could not load roles for role form:', err);
              return [] as RoleReference[];
            }),
          restClient
            .get(ENDPOINTS.ROLE_SOURCES)
            .then((data: unknown) => {
              const arr = data as Array<{ id: string; name: string }>;
              return arr;
            })
            .catch((err: unknown) => {
              console.warn('Could not load role sources:', err);
              return [] as RoleSource[];
            }),
        ]);

        const allPrivileges = Array.isArray(privileges) ? privileges : [];
        // Filter out current role from available roles to prevent circular reference
        // and deduplicate by ID (same role ID can appear from multiple sources)
        const rolesById = new Map<string, RoleReference>();
        if (Array.isArray(roles)) {
          for (const r of roles) {
            if (r.id !== (role?.id ?? roleId) && !rolesById.has(r.id)) {
              rolesById.set(r.id, { id: r.id, name: r.name });
            }
          }
        }
        const allRoles = Array.from(rolesById.values());
        const allSources = Array.isArray(sources) ? sources : [];

        // Build initial form data from loaded role or use defaults
        const initialData: RoleFormData = role
          ? {
              id: role.id,
              name: role.name,
              description: role.description,
              privileges: role.privileges,
              roles: role.roles,
              source: role.source,
              version: role.version,
            }
          : {
              id: '',
              name: '',
              description: '',
              privileges: [],
              roles: [],
            };

        return {
          data: initialData,
          role,
          allPrivileges,
          allRoles,
          allSources,
        };
      },
      // save service is provided via useForm options
    },
  });
}

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
  User,
  UserFormData,
  DEFAULT_SOURCE,
  isExternalUser,
} from './types';

/**
 * Guard: check if event value represents a local source
 */
const isSourceLocalGuard = (
  _context: unknown,
  event: { type: string; value?: string }
) => !event.value || event.value === DEFAULT_SOURCE;

/**
 * Guard: check if event value represents an external source
 */
const isSourceExternalGuard = (
  _context: unknown,
  event: { type: string; value?: string }
) => Boolean(event.value) && event.value !== DEFAULT_SOURCE;

/**
 * REST API user response shape
 */
interface RestUser {
  userId: string;
  firstName: string;
  lastName: string;
  emailAddress: string;
  source: string;
  status: 'active' | 'disabled';
  roles: string[];
  externalRoles?: string[];
  readOnly?: boolean;
}

/**
 * Convert REST user to UI User model
 */
function restToUser(rest: RestUser): User {
  return {
    userId: rest.userId,
    realm: rest.source,
    source: rest.source,
    firstName: rest.firstName,
    lastName: rest.lastName,
    emailAddress: rest.emailAddress,
    email: rest.emailAddress,
    status: rest.status,
    roles: rest.roles || [],
    externalRoles: rest.externalRoles,
    readOnly: rest.readOnly,
  };
}

/**
 * Find a user by ID and source from the REST API.
 * The users API requires searching with query params since there is
 * no direct /users/{id} GET endpoint.
 */
async function findUser(userId: string, source: string): Promise<User | null> {
  try {
    const url = `${ENDPOINTS.USERS}?userId=${encodeURIComponent(userId)}&source=${encodeURIComponent(source)}`;
    const restUsers = await restClient.get<RestUser[]>(url);
    const found = restUsers.find(
      (u) => u.userId === userId && u.source === source
    );
    return found ? restToUser(found) : null;
  } catch (err) {
    console.error('Failed to load user:', err);
    throw err;
  }
}

/**
 * Validate email format
 */
function validateEmail(email: string): string | undefined {
  if (!email) return 'Email is required';
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  if (!emailRegex.test(email)) return 'Invalid email format';
  return undefined;
}

/**
 * Validate password and confirmation fields.
 * Used for both create (required) and edit (required only when non-empty) modes.
 */
const MIN_PASSWORD_LENGTH = 8;

function validatePassword(
  password: string | undefined,
  passwordConfirm: string | undefined,
  required: boolean
): ValidationErrors {
  const errors: ValidationErrors = {};

  if (required || password || passwordConfirm) {
    if (!password) {
      errors.password = 'Password is required';
    } else if (password.length < MIN_PASSWORD_LENGTH) {
      errors.password = `Password must be at least ${MIN_PASSWORD_LENGTH} characters`;
    }
    if (!passwordConfirm) {
      errors.passwordConfirm = 'Please confirm your password';
    } else if (password && password !== passwordConfirm) {
      errors.passwordConfirm = 'Passwords do not match';
    }
  }

  return errors;
}

/**
 * Validate user form data.
 * Validation rules vary based on whether the user is local or external
 * and whether we're in create or edit mode.
 */
function validateUser(
  data: UserFormData,
  isCreate: boolean,
  isExternal: boolean
): ValidationErrors {
  const errors: ValidationErrors = {};

  if (!data.userId?.trim()) {
    errors.userId = 'User ID is required';
  }

  if (!isExternal) {
    if (!data.firstName?.trim()) {
      errors.firstName = 'First name is required';
    }
    if (!data.lastName?.trim()) {
      errors.lastName = 'Last name is required';
    }
    if (!data.emailAddress?.trim()) {
      errors.emailAddress = 'Email is required';
    } else {
      const emailError = validateEmail(data.emailAddress);
      if (emailError && emailError !== 'Email is required') {
        errors.emailAddress = emailError;
      }
    }
  }

  if (!isExternal) {
    const passwordErrors = validatePassword(
      data.password,
      data.passwordConfirm,
      isCreate
    );
    Object.assign(errors, passwordErrors);
  }

  if (data.roles.length === 0) {
    errors.roles = 'At least one role must be assigned';
  }

  return errors;
}

/**
 * User source reference for dropdowns
 */
interface UserSourceRef {
  id: string;
  name: string;
}

/**
 * Role reference for transfer lists and Inspector
 */
interface RoleRef {
  id: string;
  name: string;
  source?: string;
}

/**
 * Create a users form machine with XState.
 * Uses createFormMachine WITH editingConfig for source variants:
 * - 'local': Full form with password fields, name, email
 * - 'external': Reduced form with only userId, status, roles
 *
 * The SOURCE_CHANGE event transitions between local and external sub-states,
 * clearing local-only fields when switching to external.
 */
export function createUsersFormMachine(
  userId: string | undefined,
  userSource: string = DEFAULT_SOURCE,
  preloadedUser?: User
) {
  const isCreate = !userId && !preloadedUser;

  return createFormMachine({
    id: `users-form-${userId ?? 'new'}`,
    context: {
      data: {
        userId: '',
        firstName: '',
        lastName: '',
        emailAddress: '',
        password: '',
        passwordConfirm: '',
        status: true,
        roles: [] as string[],
        source: DEFAULT_SOURCE,
      } as UserFormData,
      // Initialize reference data as empty - populated by load service
      user: preloadedUser ?? (null as User | null),
      allRoles: [] as RoleRef[],
      userSources: [] as UserSourceRef[],
    },
    actions: {
      validate: assign((ctx: FormContext<UserFormData>) => ({
        validationErrors: validateUser(
          ctx.data,
          isCreate,
          isExternalUser(ctx.data.source || DEFAULT_SOURCE)
        ),
      })),
      // Custom action: update source and clear local-only fields when switching to external
      changeSource: assign((context: any, event: any) => {
        const newSource = event.value;
        const external = isExternalUser(newSource);
        return {
          data: {
            ...context.data,
            source: newSource,
            // Clear local-only fields when switching to external source
            ...(external
              ? {
                  firstName: '',
                  lastName: '',
                  emailAddress: '',
                  password: '',
                  passwordConfirm: '',
                }
              : {}),
          },
          touched: { ...context.touched, source: true },
        };
      }),
    },
    guards: {
      // Override auto-generated guards for the determining state routing.
      // The auto-generated guards check data.source === 'local' / 'external',
      // but actual source values are 'default' (local) or 'LDAP', 'Crowd', etc.
      isType_local: ((context: FormContext<UserFormData>) => {
        const source = context.data.source;
        return !source || source === DEFAULT_SOURCE;
      }) as any,
      isType_external: ((context: FormContext<UserFormData>) => {
        const source = context.data.source;
        return Boolean(source) && source !== DEFAULT_SOURCE;
      }) as any,
      // Event-based guards for SOURCE_CHANGE transitions
      isSourceLocal: isSourceLocalGuard as any,
      isSourceExternal: isSourceExternalGuard as any,
    },
    services: {
      load: async () => {
        // Load user and reference data in parallel
        const [user, roles, sources] = await Promise.all([
          // If user is preloaded, use it; otherwise fetch if userId is provided
          preloadedUser
            ? Promise.resolve(preloadedUser)
            : userId
            ? findUser(userId, userSource).catch((err: unknown) => {
                console.error('Failed to load user:', err);
                throw err;
              })
            : Promise.resolve(null),
          restClient
            .get(ENDPOINTS.ROLES)
            .then((data: unknown) => {
              const arr = data as Array<{ id: string; name: string; source?: string }>;
              return arr.map((r) => ({ id: r.id, name: r.name, source: r.source }));
            })
            .catch((err: unknown) => {
              console.warn('Could not load roles for user form:', err);
              return [] as RoleRef[];
            }),
          restClient
            .get(ENDPOINTS.USER_SOURCES)
            .then((data: unknown) => {
              const arr = data as string[] | UserSourceRef[];
              // API may return array of strings or array of objects
              if (arr.length > 0 && typeof arr[0] === 'string') {
                return (arr as string[]).map((id) => ({ id, name: id }));
              }
              return arr as UserSourceRef[];
            })
            .catch((err: unknown) => {
              console.warn('Could not load user sources:', err);
              return [] as UserSourceRef[];
            }),
        ]);

        const allRoles = Array.isArray(roles) ? roles : [];
        // Add "Local" option for default source and merge with fetched sources
        // Filter out any "default" source from API to prevent duplicates
        const localSource: UserSourceRef = { id: DEFAULT_SOURCE, name: 'Local' };
        const filteredSources = Array.isArray(sources)
          ? sources.filter((s) => s.id !== DEFAULT_SOURCE)
          : [];
        const userSources = [localSource, ...filteredSources];

        // Determine the effective source for the loaded user
        const effectiveSource = user?.source ?? userSource;

        // Build initial form data from loaded user or use defaults
        const initialData: UserFormData = user
          ? {
              userId: user.userId,
              firstName: user.firstName,
              lastName: user.lastName,
              emailAddress: user.emailAddress || user.email || '',
              password: '',
              passwordConfirm: '',
              status: user.status === 'active',
              roles: user.roles || [],
              source: effectiveSource,
            }
          : {
              userId: '',
              firstName: '',
              lastName: '',
              emailAddress: '',
              password: '',
              passwordConfirm: '',
              status: true,
              roles: [],
              source: DEFAULT_SOURCE,
            };

        return {
          data: initialData,
          user,
          allRoles,
          userSources,
        };
      },
      // save service is provided via useForm options
    },
    // Custom event for source changes (transitions between local/external sub-states)
    on: {
      SOURCE_CHANGE: [
        {
          target: '.local',
          cond: 'isSourceLocal',
          actions: ['changeSource', 'validate', 'computePristine'],
        },
        {
          target: '.external',
          cond: 'isSourceExternal',
          actions: ['changeSource', 'validate', 'computePristine'],
        },
      ],
    },
    // Source variant sub-states within the editing state.
    // Local users have full form fields including password.
    // External users have a reduced form (userId, status, roles only).
    editingConfig: {
      defaultState: 'local',
      typeField: 'source',
      states: {
        local: {
          meta: {
            typeLabel: 'Local',
            fields: [
              'userId',
              'firstName',
              'lastName',
              'emailAddress',
              'password',
              'passwordConfirm',
              'status',
              'roles',
            ],
            requiredFields: [
              'userId',
              'firstName',
              'lastName',
              'emailAddress',
            ],
            fieldConfig: {
              userId: {
                label: 'ID',
                type: 'text',
                helpText: 'This will be used as the username',
              },
              firstName: { label: 'First Name', type: 'text' },
              lastName: { label: 'Last Name', type: 'text' },
              emailAddress: {
                label: 'Email',
                type: 'email',
                helpText: 'Used for notifications',
              },
              password: { label: 'Password', type: 'password' },
              passwordConfirm: { label: 'Confirm Password', type: 'password' },
              status: {
                label: 'Active',
                type: 'checkbox',
                description: 'User account is enabled',
              },
              roles: { label: 'Roles', type: 'transferList' },
            },
          },
        },
        external: {
          meta: {
            typeLabel: 'External',
            fields: ['userId', 'status', 'roles'],
            requiredFields: ['userId'],
            fieldConfig: {
              userId: {
                label: 'ID',
                type: 'text',
                helpText: 'External user identifier',
                disabled: true,
              },
              status: {
                label: 'Active',
                type: 'checkbox',
                description: 'User account is enabled',
              },
              roles: { label: 'Roles', type: 'transferList' },
            },
          },
        },
      },
    },
  });
}

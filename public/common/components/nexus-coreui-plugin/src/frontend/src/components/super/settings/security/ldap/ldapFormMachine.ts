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
import { createFormMachine } from '@sonatype/nexus-ui-plugin';
import type { FormContext, ValidationErrors } from '@sonatype/nexus-ui-plugin';

import {
  LdapServer,
  LdapFormData,
  LdapSchemaTemplate,
  DEFAULT_LDAP_SERVER,
} from './types';

/**
 * Validate LDAP connection fields.
 * Checks name, host, port, search base DN, and auth credentials.
 */
function validateConnection(data: LdapFormData): ValidationErrors {
  const errors: ValidationErrors = {};

  if (!data.name?.trim()) {
    errors.name = 'Name is required';
  }

  if (!data.protocol || !['ldap', 'ldaps'].includes(data.protocol)) {
    errors.protocol = 'Protocol is required';
  }

  if (!data.host?.trim()) {
    errors.host = 'Hostname is required';
  }

  if (!data.port || data.port < 1 || data.port > 65535) {
    errors.port = 'Port must be between 1 and 65535';
  }

  if (!data.searchBase?.trim()) {
    errors.searchBase = 'Search base DN is required';
  }

  // Auth credentials required when scheme is not anonymous
  if (data.authScheme !== 'none') {
    if (!data.authUsername?.trim()) {
      errors.authUsername = 'Username is required for authenticated connection';
    }
    if (!data.authPassword) {
      errors.authPassword = 'Password is required for authenticated connection';
    }
  }

  return errors;
}

/**
 * Validate LDAP user and group mapping fields.
 * Checks user object class, ID/name/email attributes, and group fields
 * when LDAP groups as roles is enabled.
 */
function validateUserGroup(data: LdapFormData): ValidationErrors {
  const errors: ValidationErrors = {};

  if (!data.userObjectClass?.trim()) {
    errors.userObjectClass = 'User object class is required';
  }

  if (!data.userIdAttribute?.trim()) {
    errors.userIdAttribute = 'User ID attribute is required';
  }

  if (!data.userRealNameAttribute?.trim()) {
    errors.userRealNameAttribute = 'Real name attribute is required';
  }

  if (!data.userEmailAddressAttribute?.trim()) {
    errors.userEmailAddressAttribute = 'Email attribute is required';
  }

  if (data.ldapGroupsAsRoles) {
    if (data.groupType === 'static') {
      if (!data.groupObjectClass?.trim()) {
        errors.groupObjectClass = 'Group object class is required';
      }
      if (!data.groupIdAttribute?.trim()) {
        errors.groupIdAttribute = 'Group ID attribute is required';
      }
      if (!data.groupMemberAttribute?.trim()) {
        errors.groupMemberAttribute = 'Group member attribute is required';
      }
      if (!data.groupMemberFormat?.trim()) {
        errors.groupMemberFormat = 'Group member format is required';
      }
    } else if (data.groupType === 'dynamic') {
      if (!data.userMemberOfAttribute?.trim()) {
        errors.userMemberOfAttribute = 'User member of attribute is required';
      }
    }
  }

  return errors;
}

/**
 * Validate all LDAP form fields (connection + user/group mapping).
 */
export function validateLdap(data: LdapFormData): ValidationErrors {
  return {
    ...validateConnection(data),
    ...validateUserGroup(data),
  };
}

/**
 * Create an LDAP form machine with XState.
 *
 * The LDAP form is a flat form (no type variants / editingConfig) that covers:
 * - Connection settings (name, protocol, host, port, search base, auth)
 * - User mapping (object class, ID/name/email attributes, filters)
 * - Group mapping (conditional on ldapGroupsAsRoles flag)
 *
 * The load service fetches the LDAP server data (if editing) and
 * schema templates for pre-filling common configurations.
 */
export function createLdapFormMachine(
  serverId: string | undefined,
  preloadedServer?: LdapServer | null
) {
  // Initialize form data from preloaded server if available, otherwise use defaults
  // This ensures the form displays correct values immediately before load service completes
  const initialFormData: LdapFormData = preloadedServer
    ? {
        id: preloadedServer.id,
        name: preloadedServer.name,
        protocol: preloadedServer.protocol,
        useTrustStore: preloadedServer.useTrustStore,
        host: preloadedServer.host,
        port: preloadedServer.port,
        searchBase: preloadedServer.searchBase,
        authScheme: preloadedServer.authScheme,
        authRealm: preloadedServer.authRealm,
        authUsername: preloadedServer.authUsername,
        authPassword: preloadedServer.authPassword,
        connectionTimeout: preloadedServer.connectionTimeout,
        connectionRetryDelay: preloadedServer.connectionRetryDelay,
        maxIncidentsCount: preloadedServer.maxIncidentsCount,
        userBaseDn: preloadedServer.userBaseDn,
        userSubtree: preloadedServer.userSubtree,
        userObjectClass: preloadedServer.userObjectClass,
        userLdapFilter: preloadedServer.userLdapFilter,
        userIdAttribute: preloadedServer.userIdAttribute,
        userRealNameAttribute: preloadedServer.userRealNameAttribute,
        userEmailAddressAttribute: preloadedServer.userEmailAddressAttribute,
        userPasswordAttribute: preloadedServer.userPasswordAttribute,
        ldapGroupsAsRoles: preloadedServer.ldapGroupsAsRoles,
        groupType: preloadedServer.groupType,
        groupBaseDn: preloadedServer.groupBaseDn,
        groupSubtree: preloadedServer.groupSubtree,
        groupObjectClass: preloadedServer.groupObjectClass,
        groupIdAttribute: preloadedServer.groupIdAttribute,
        groupMemberAttribute: preloadedServer.groupMemberAttribute,
        groupMemberFormat: preloadedServer.groupMemberFormat,
        userMemberOfAttribute: preloadedServer.userMemberOfAttribute,
      }
    : { ...DEFAULT_LDAP_SERVER };

  return createFormMachine({
    id: `ldap-form-${serverId ?? 'new'}`,
    context: {
      data: initialFormData,
      // Reference data populated by load service
      server: preloadedServer ?? (null as LdapServer | null),
      templates: [] as LdapSchemaTemplate[],
    },
    actions: {
      validate: assign((ctx: FormContext<LdapFormData>) => ({
        validationErrors: validateLdap(ctx.data),
      })),
      // Apply a schema template to user/group mapping fields
      applyTemplate: assign((context: any, event: any) => {
        const template = event.template as LdapSchemaTemplate;
        if (!template) return {};
        return {
          data: {
            ...context.data,
            userBaseDn: template.userBaseDn ?? context.data.userBaseDn,
            userSubtree: template.userSubtree ?? context.data.userSubtree,
            userObjectClass: template.userObjectClass ?? context.data.userObjectClass,
            userLdapFilter: template.userLdapFilter ?? context.data.userLdapFilter,
            userIdAttribute: template.userIdAttribute ?? context.data.userIdAttribute,
            userRealNameAttribute: template.userRealNameAttribute ?? context.data.userRealNameAttribute,
            userEmailAddressAttribute: template.userEmailAddressAttribute ?? context.data.userEmailAddressAttribute,
            userPasswordAttribute: template.userPasswordAttribute ?? context.data.userPasswordAttribute,
            ldapGroupsAsRoles: template.ldapGroupsAsRoles ?? context.data.ldapGroupsAsRoles,
            groupType: template.groupType ?? context.data.groupType,
            groupBaseDn: template.groupBaseDn ?? context.data.groupBaseDn,
            groupSubtree: template.groupSubtree ?? context.data.groupSubtree,
            groupObjectClass: template.groupObjectClass ?? context.data.groupObjectClass,
            groupIdAttribute: template.groupIdAttribute ?? context.data.groupIdAttribute,
            groupMemberAttribute: template.groupMemberAttribute ?? context.data.groupMemberAttribute,
            groupMemberFormat: template.groupMemberFormat ?? context.data.groupMemberFormat,
            userMemberOfAttribute: template.userMemberOfAttribute ?? context.data.userMemberOfAttribute,
          },
        };
      }),
      // Handle protocol change with automatic port update
      changeProtocol: assign((context: any, event: any) => {
        const protocol = event.value as string;
        let port = context.data.port;
        if (protocol === 'ldaps' && port === 389) {
          port = 636;
        } else if (protocol === 'ldap' && port === 636) {
          port = 389;
        }
        return {
          data: { ...context.data, protocol, port },
          touched: { ...context.touched, protocol: true },
        };
      }),
    },
    services: {
      load: async () => {
        // Build initial form data from server or defaults
        const initialData: LdapFormData = preloadedServer
          ? {
              id: preloadedServer.id,
              name: preloadedServer.name,
              protocol: preloadedServer.protocol,
              useTrustStore: preloadedServer.useTrustStore,
              host: preloadedServer.host,
              port: preloadedServer.port,
              searchBase: preloadedServer.searchBase,
              authScheme: preloadedServer.authScheme,
              authRealm: preloadedServer.authRealm,
              authUsername: preloadedServer.authUsername,
              authPassword: preloadedServer.authPassword,
              connectionTimeout: preloadedServer.connectionTimeout,
              connectionRetryDelay: preloadedServer.connectionRetryDelay,
              maxIncidentsCount: preloadedServer.maxIncidentsCount,
              userBaseDn: preloadedServer.userBaseDn,
              userSubtree: preloadedServer.userSubtree,
              userObjectClass: preloadedServer.userObjectClass,
              userLdapFilter: preloadedServer.userLdapFilter,
              userIdAttribute: preloadedServer.userIdAttribute,
              userRealNameAttribute: preloadedServer.userRealNameAttribute,
              userEmailAddressAttribute: preloadedServer.userEmailAddressAttribute,
              userPasswordAttribute: preloadedServer.userPasswordAttribute,
              ldapGroupsAsRoles: preloadedServer.ldapGroupsAsRoles,
              groupType: preloadedServer.groupType,
              groupBaseDn: preloadedServer.groupBaseDn,
              groupSubtree: preloadedServer.groupSubtree,
              groupObjectClass: preloadedServer.groupObjectClass,
              groupIdAttribute: preloadedServer.groupIdAttribute,
              groupMemberAttribute: preloadedServer.groupMemberAttribute,
              groupMemberFormat: preloadedServer.groupMemberFormat,
              userMemberOfAttribute: preloadedServer.userMemberOfAttribute,
            }
          : { ...DEFAULT_LDAP_SERVER };

        return {
          data: initialData,
          server: preloadedServer ?? null,
          templates: [] as LdapSchemaTemplate[],
        };
      },
      // save service is provided via useForm options
    },
    // Custom event: apply a schema template to user/group mapping fields
    on: {
      APPLY_TEMPLATE: {
        actions: ['applyTemplate', 'validate', 'computePristine'],
      },
      PROTOCOL_CHANGE: {
        actions: ['changeProtocol', 'validate', 'computePristine'],
      },
    },
  });
}

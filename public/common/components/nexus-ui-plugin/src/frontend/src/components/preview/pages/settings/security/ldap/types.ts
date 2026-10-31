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

/**
 * Protocol for LDAP connection
 */
export type LdapProtocol = 'ldap' | 'ldaps';

/**
 * Authentication scheme for LDAP
 */
export type LdapAuthScheme = 'none' | 'simple' | 'DIGEST-MD5' | 'CRAM-MD5';

/**
 * Group type for LDAP group mapping
 */
export type LdapGroupType = 'static' | 'dynamic';

/**
 * LDAP Server connection settings
 */
export interface LdapServerConnection {
  id?: string;
  order?: number;
  name: string;
  url?: string;
  protocol: LdapProtocol;
  useTrustStore?: boolean;
  host: string;
  port: number;
  searchBase: string;
  authScheme: string;
  authRealm?: string;
  authUsername?: string;
  authPassword?: string;
  connectionTimeout?: number;
  connectionRetryDelay?: number;
  maxIncidentsCount?: number;
}

/**
 * LDAP Server full configuration (extends connection with user/group mapping)
 */
export interface LdapServer extends LdapServerConnection {
  // User mapping
  userBaseDn?: string;
  userSubtree?: boolean;
  userObjectClass: string;
  userLdapFilter?: string;
  userIdAttribute: string;
  userRealNameAttribute: string;
  userEmailAddressAttribute: string;
  userPasswordAttribute?: string;

  // Group mapping
  ldapGroupsAsRoles: boolean;
  groupType?: LdapGroupType;
  groupBaseDn?: string;
  groupSubtree?: boolean;
  groupObjectClass?: string;
  groupIdAttribute?: string;
  groupMemberAttribute?: string;
  groupMemberFormat?: string;
  userMemberOfAttribute?: string;
}

/**
 * LDAP Schema template for pre-filling forms
 */
export interface LdapSchemaTemplate {
  name: string;
  userBaseDn?: string;
  userSubtree?: boolean;
  userObjectClass?: string;
  userLdapFilter?: string;
  userIdAttribute?: string;
  userRealNameAttribute?: string;
  userEmailAddressAttribute?: string;
  userPasswordAttribute?: string;
  ldapGroupsAsRoles?: boolean;
  groupType?: LdapGroupType;
  groupBaseDn?: string;
  groupSubtree?: boolean;
  groupObjectClass?: string;
  groupIdAttribute?: string;
  groupMemberAttribute?: string;
  groupMemberFormat?: string;
  userMemberOfAttribute?: string;
}

/**
 * LDAP user returned from verify user mapping
 */
export interface LdapUser {
  username: string;
  realName?: string;
  email?: string;
  membership?: string[];
}

/**
 * Form data for LDAP server
 */
export interface LdapFormData extends Omit<LdapServer, 'id' | 'order' | 'url'> {
  id?: string;
}

/**
 * Validation errors for LDAP form.
 *
 * Not consumed internally by this module - ldapFormMachine.ts's
 * validateConnection/validateUserGroup/validateLdap return the generic
 * ValidationErrors type from the shared form interface instead (see
 * NEXUS-53623 F7). This interface is kept and re-exported (index.ts) purely
 * as a typed error-shape for external/plugin consumers that want static
 * typing on LDAP form error keys; keep it in sync with the keys actually
 * produced by ldapFormMachine.ts's validators when adding new fields.
 */
export interface LdapFormErrors {
  name?: string;
  host?: string;
  port?: string;
  searchBase?: string;
  authScheme?: string;
  authUsername?: string;
  authPassword?: string;
  connectionTimeout?: string;
  connectionRetryDelay?: string;
  maxIncidentsCount?: string;
  userObjectClass?: string;
  userIdAttribute?: string;
  userRealNameAttribute?: string;
  userEmailAddressAttribute?: string;
  groupType?: string;
  groupObjectClass?: string;
  groupIdAttribute?: string;
  groupMemberAttribute?: string;
  groupMemberFormat?: string;
  userMemberOfAttribute?: string;
}

/**
 * Props for LdapPage component
 */
export interface LdapPageProps {
  className?: string;
}

/**
 * Props for LdapList component
 */
export interface LdapListProps {
  servers: LdapServer[];
  onSelect: (server: LdapServer) => void;
  onCreate: () => void;
  onReorder: (serverNames: string[]) => Promise<void>;
  onDelete: (server: LdapServer) => void;
  onClearCache: () => void;
  loading?: boolean;
}

/**
 * View mode for LDAP page
 */
export type LdapViewMode = 'list' | 'create' | 'edit';

/**
 * Step in the LDAP wizard form
 */
export type LdapFormStep = 'connection' | 'userGroup';

/**
 * Authentication scheme options
 */
export const AUTH_SCHEMES: Array<{ value: string; label: string }> = [
  { value: 'none', label: 'Anonymous Authentication' },
  { value: 'simple', label: 'Simple Authentication' },
  { value: 'DIGEST-MD5', label: 'DIGEST-MD5' },
  { value: 'CRAM-MD5', label: 'CRAM-MD5' },
];

/**
 * Default values for new LDAP server
 */
export const DEFAULT_LDAP_SERVER: LdapFormData = {
  name: '',
  protocol: 'ldap',
  useTrustStore: false,
  host: '',
  port: 389,
  searchBase: '',
  authScheme: 'simple',
  authUsername: '',
  authPassword: '',
  connectionTimeout: 30,
  connectionRetryDelay: 300,
  maxIncidentsCount: 3,
  userBaseDn: '',
  userSubtree: false,
  userObjectClass: 'inetOrgPerson',
  userLdapFilter: '',
  userIdAttribute: 'uid',
  userRealNameAttribute: 'cn',
  userEmailAddressAttribute: 'mail',
  userPasswordAttribute: '',
  ldapGroupsAsRoles: true,
  groupType: 'dynamic',
  groupBaseDn: '',
  groupSubtree: false,
  groupObjectClass: 'groupOfUniqueNames',
  groupIdAttribute: 'cn',
  groupMemberAttribute: 'uniqueMember',
  groupMemberFormat: 'uid=${username},ou=people,dc=example,dc=com',
  userMemberOfAttribute: 'memberOf',
};

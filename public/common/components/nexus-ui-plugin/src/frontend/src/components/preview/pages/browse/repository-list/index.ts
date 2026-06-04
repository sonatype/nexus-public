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
 * Repository List Module
 *
 * Browse functionality for viewing repositories in a table with sorting,
 * filtering, and IQ Server integration.
 */

// Main component
export { RepositoryList, default } from './RepositoryList';
export { RepositoryListTable } from './RepositoryListTable';
export { BrowseSidebar } from './BrowseSidebar';

// Sub-components
export { RepositoryStatusBadge } from './RepositoryStatusBadge';
export { HealthCheckCell } from '../../../shared/security/HealthCheckCell';
export { FirewallCell as IqPolicyViolationsCell } from '../../../shared/security/FirewallCell';

// Hooks
export {
  useRepositoryList,
  isIqServerEnabled,
  canUpdateHealthCheck,
  canReadFirewallStatus,
} from './useRepositoryList';

// Server-side filtering hook (enterprise-scalable)
export {
  useRepositoryListServer,
  arrayToFilterString,
  filterStringToArray,
} from './useRepositoryListServer';

// Types
export type {
  Repository,
  RepositoryStatus,
  RepositoryType,
  RepositoryListProps,
  RepositoryListState,
  RepositoryStatusBadgeProps,
  HealthCheckStatus,
  FirewallStatus,
  SortableField,
  SortDirection,
  SortConfig,
  RepositoryListStrings,
} from './repository-list.types';


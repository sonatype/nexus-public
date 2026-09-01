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
 * GA Detail Module - Agent 2 Workspace
 * 
 * Exports components for GA detail views with 5 tabs:
 * - Overview: description, license, project URL
 * - Versions: version list with status badges
 * - Repositories: repos containing this GA
 * - Files: assets for selected version
 * - Security: vulnerabilities for selected version
 */

// Main page component
export { GADetailPage } from './GADetailPage';

// Tab components
export { GAOverviewTab } from './GAOverviewTab';
export { GAVersionsTab } from './GAVersionsTab';
export { GARepositoriesTab } from './GARepositoriesTab';
export { GAFilesTab } from './GAFilesTab';
export { GASecurityTab } from './GASecurityTab';

// Shared components
export { Breadcrumbs } from './Breadcrumbs';
export type { BreadcrumbItem } from './Breadcrumbs';

// Hooks
export { useGADetail } from './useGADetail';


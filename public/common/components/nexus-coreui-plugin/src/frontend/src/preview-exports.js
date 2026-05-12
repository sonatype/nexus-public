/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Open Source Version is distributed with Sencha Ext JS pursuant to a FLOSS Exception agreed upon
 * between Sonatype, Inc. and Sencha Inc. Sencha Ext JS is licensed under GPL v3 and cannot be redistributed as part of a
 * closed source work.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
/**
 * Preview UI Exports
 *
 * Sprint 14: This file exports Preview UI route definitions and feature flags
 * so that nexus-cloudui-plugin can import them and add Preview UI support.
 *
 * Usage in nexus-cloudui-plugin:
 *   import { previewAdminRoutes, previewBrowseRoutes, previewUserRoutes }
 *     from '@sonatype/nexus-coreui-plugin/preview-exports';
 */

// Route definitions
export { previewAdminRoutes } from './routerConfig/routes/previewAdminRoutes';
export { previewBrowseRoutes } from './routerConfig/routes/previewBrowseRoutes';
export { previewUserRoutes } from './routerConfig/routes/previewUserRoutes';

// Feature flags
export { PREVIEW_FEATURE_FLAGS, isDevelopmentMode, isFeatureEnabled } from './config/previewFeatureFlags';

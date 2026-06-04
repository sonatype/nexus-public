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
 * Browse Actions Module
 *
 * Provides action components for the Browse functionality:
 * - DeleteDialog: Confirmation dialog for delete operations
 * - CopyUrlButton: Copy URL to clipboard with toast feedback
 * - useActions: Hook for delete API operations
 */

// Components
export { DeleteDialog } from './DeleteDialog';
export { CopyUrlButton } from './CopyUrlButton';

// Hooks
export { useActions } from './useActions';

// Types
export type {
  DeleteItemType,
  DeleteItemInfo,
  DeleteDialogProps,
  CopyUrlButtonProps,
  DeleteResult,
  UseActionsOptions,
  UseActionsReturn,
  ActionStrings,
} from './actions.types';

// Constants
export { DELETE_ENDPOINTS, ACTION_STRINGS } from './actions.types';


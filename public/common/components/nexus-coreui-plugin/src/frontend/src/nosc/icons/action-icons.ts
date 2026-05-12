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

import {
  Trash2,
  Plus,
  ArrowLeft,
  Pencil,
  Check,
  X,
  Search,
  RefreshCw,
  Download,
  ExternalLink,
  Copy,
  Settings2,
} from 'lucide-react';

/**
 * Semantic action icon mappings.
 *
 * Use these instead of importing Lucide icons directly to ensure
 * consistent icon usage across the application.
 *
 * @example
 * import { ActionIcons } from '@nosc/icons/action-icons';
 * <ActionIcons.Delete size={16} />
 */
export const ActionIcons = {
  Delete: Trash2,
  Add: Plus,
  Back: ArrowLeft,
  Edit: Pencil,
  Save: Check,
  Cancel: X,
  Search,
  Refresh: RefreshCw,
  Download,
  ExternalLink,
  Copy,
  Settings: Settings2,
} as const;

export type ActionIconName = keyof typeof ActionIcons;

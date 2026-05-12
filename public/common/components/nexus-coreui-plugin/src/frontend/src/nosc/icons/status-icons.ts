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
  Loader2,
  AlertCircle,
  AlertTriangle,
  CheckCircle2,
  Info,
} from 'lucide-react';

/**
 * Semantic status icon mappings for feedback states.
 *
 * @example
 * import { StatusIcons } from '@nosc/icons/status-icons';
 * <StatusIcons.Loading size={16} className="animate-spin" />
 */
export const StatusIcons = {
  Loading: Loader2,
  Error: AlertCircle,
  Warning: AlertTriangle,
  Success: CheckCircle2,
  Info,
} as const;

export type StatusIconName = keyof typeof StatusIcons;

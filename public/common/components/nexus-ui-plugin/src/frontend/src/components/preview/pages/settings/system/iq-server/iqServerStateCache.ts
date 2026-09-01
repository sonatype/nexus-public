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

import type { IqServerConfiguration } from './types';

/**
 * Shared module-level caches for IQ Server connect/disconnect navigation.
 *
 * After a PUT, the backend processes IQ state asynchronously, so an immediate
 * GET may return stale data.  Two mechanisms cover the two flows:
 *
 *   freshIqConfigCache  — set after a successful Save (connect).  The newly
 *     mounted IqServerConnectedPage reads it on first load to know justSaved=true,
 *     skips verifyConnection (which returns 400 briefly after save), and forces
 *     enabled:true regardless of GET response.
 *
 *   pendingDisconnect   — set before navigating away after Disconnect.
 *     IqServerOverviewPage checks it on mount and skips its own GET entirely,
 *     showing the disconnected card without racing the server.
 */

export let freshIqConfigCache: IqServerConfiguration | null = null;
export function setFreshIqConfigCache(config: IqServerConfiguration): void { freshIqConfigCache = config; }
export function clearFreshIqConfigCache(): void { freshIqConfigCache = null; }

export let pendingDisconnect = false;
export function setPendingDisconnect(): void { pendingDisconnect = true; }
export function clearPendingDisconnect(): void { pendingDisconnect = false; }

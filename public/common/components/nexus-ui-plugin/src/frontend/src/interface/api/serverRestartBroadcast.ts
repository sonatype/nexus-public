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
 * Dispatched when the ExtJS State controller detects a server restart while the
 * Preview UI is active, so the React shell can show ServerRestartModal instead of
 * the legacy NX.Dialogs.showInfo dialog. The classic UI keeps the ExtJS dialog.
 *
 * The ExtJS side (NX/controller/State.js) dispatches this same string literal
 * directly; keep the two in sync.
 */
export const NEXUS_SERVER_RESTARTED_EVENT = 'nexus:server-restarted';

/*
 * State.js dispatches this event directly against the string literal above, so no
 * React-side dispatcher is needed; the machine only listens for the event name.
 */

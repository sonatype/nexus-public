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
 * True when the app is loaded from a typical local dev host (localhost, loopback).
 * Used to auto-enable Sonatype internal Test Hub routes/nav on self-hosted (8081) and cloud (7777) without
 * differing behavior by port — localStorage is per origin, so `localhost:8081` and `localhost:7777` are separate.
 */
export function isLocalDevHostname(): boolean {
  if (typeof window === 'undefined') {
    return false;
  }
  const h = window.location.hostname;
  return h === 'localhost' || h === '127.0.0.1' || h === '[::1]';
}

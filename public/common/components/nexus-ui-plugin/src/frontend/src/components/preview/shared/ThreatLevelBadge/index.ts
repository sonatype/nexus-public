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

// Types are re-exported with `export type`, not bundled into the value export.
// This build transpiles TypeScript with Babel, which erases types without
// whole-program knowledge, so a type re-exported as a value has nothing to bind
// to at runtime -- rspack reports it as an ESModulesLinkingWarning on every
// build. `export type` tells Babel to drop it outright.
export {
  ThreatLevelBadge,
  getThreatLevelBucket,
  THREAT_LEVEL_BUCKET_LABELS,
} from './ThreatLevelBadge';

export type { ThreatLevelBadgeProps, ThreatLevelBucket } from './ThreatLevelBadge';

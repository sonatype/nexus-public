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

import { ExtJS } from '../../../../interface/ExtJS';

/**
 * True when the instance is running a licensed edition. The single chokepoint for React
 * "is licensed" checks — mirrors `ExtJS.isLicensedEdition()`, which remains the bridge for
 * non-React (classic ExtJS) callers.
 */
export function useIsLicensedEdition(): boolean {
  return Boolean(ExtJS.useState(() => ExtJS.isLicensedEdition()));
}

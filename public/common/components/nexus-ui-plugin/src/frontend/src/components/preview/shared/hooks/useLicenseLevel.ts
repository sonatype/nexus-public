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

export enum LicenseLevel {
  PRO = "nexus-professional-edition",
  PRO_PLUS = "nexus-professional-plus-edition",
  ENTERPRISE = "nexus-enterprise-edition"
}

/**
 * True when the instance has the specified license, or higher. The single chokepoint for React
 * "is licensed" checks — depends on `ExtJS.state().getEdition()`, which remains the bridge for
 * non-React (classic ExtJS) callers.
 */
export function useLicenseLevel(level : LicenseLevel): boolean {
  let fn;
  if (LicenseLevel.PRO === level) {
    fn = (edition: string):boolean => edition === LicenseLevel.PRO || edition === LicenseLevel.PRO_PLUS ||
       edition === LicenseLevel.ENTERPRISE;
  }
  else if (LicenseLevel.PRO_PLUS === level) {
    fn = (edition: string):boolean => edition === LicenseLevel.PRO_PLUS || edition === LicenseLevel.ENTERPRISE;
  }
  else if (LicenseLevel.ENTERPRISE === level) {
    fn = (edition: string):boolean => edition === LicenseLevel.ENTERPRISE;
  }
  else {
    throw Error("Unknown edition");
  }
  return Boolean(ExtJS.useState?.(() => fn(ExtJS.state().getValue("editionId")?.toLowerCase())) ?? false);
}

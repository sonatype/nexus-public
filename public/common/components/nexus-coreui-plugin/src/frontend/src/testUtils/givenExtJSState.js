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

import { ExtJS } from '@sonatype/nexus-ui-plugin';

export default   function givenExtJSState(
    values = {},
    edition = "COMMUNITY",
    majorMinorVersion = '1.x.x',
    fullVersion = '1.x.x-snapshot'
) {
  const getValueMock = jest.fn().mockImplementation((key) => {
    return values[key];
  });

  const stateMock = {
    getEdition: jest.fn().mockReturnValue(edition),
    getVersionMajorMinor:jest.fn().mockReturnValue(majorMinorVersion),
    getVersion: jest.fn().mockReturnValue(fullVersion),
    getValue: getValueMock,
    getUser: jest.fn()
  };

  // these resolve to the same thing the real world, but have to be mocked individually
  // because of the way they are accessed
  jest.spyOn(ExtJS, 'state').mockReturnValue(stateMock);
  global.NX.State = stateMock;
  
  // Also mock useStatus for components that use that hook
  jest.spyOn(ExtJS, 'useStatus').mockReturnValue({
    edition: edition,
    version: fullVersion,
  });
}

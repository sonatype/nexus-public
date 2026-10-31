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

import {isSonatypeDevMode} from './devModeUtils';

describe('isSonatypeDevMode', () => {
  afterEach(() => {
    localStorage.clear();
  });

  it('is false when neither the build flag nor the localStorage flag is set', () => {
    expect(isSonatypeDevMode()).toBe(false);
  });

  it('is true when the localStorage flag is set to "true"', () => {
    localStorage.setItem('SONATYPE_INTERNAL', 'true');
    expect(isSonatypeDevMode()).toBe(true);
  });

  it('is false when the localStorage flag is set to a non-"true" value', () => {
    localStorage.setItem('SONATYPE_INTERNAL', 'false');
    expect(isSonatypeDevMode()).toBe(false);
  });

  it('is false when localStorage access throws', () => {
    const getItemSpy = jest.spyOn(Storage.prototype, 'getItem').mockImplementation(() => {
      throw new Error('localStorage is unavailable');
    });

    expect(isSonatypeDevMode()).toBe(false);

    getItemSpy.mockRestore();
  });
});

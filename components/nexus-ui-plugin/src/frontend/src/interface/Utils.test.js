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
import Utils from './Utils';
import UIStrings from '../constants/UIStrings';

describe('Utils', () => {
  describe('isInRange', () => {
    it ('rejects non-numeric values', () => {
      expect(Utils.isInRange({value: '1a'})).toBe(UIStrings.ERROR.NAN);
      expect(Utils.isInRange({value: '1e'})).toBe(UIStrings.ERROR.NAN);
      expect(Utils.isInRange({value: '1e1'})).toBe(null);
    });

    it('accepts numeric values with no range', () => {
      expect(Utils.isInRange({value: '1'})).toBe(null);
      expect(Utils.isInRange({value: 1})).toBe(null);
    });

    it('rejects numeric values below the min', () => {
      expect(Utils.isInRange({value: 0, min: 0})).toBe(null);
      expect(Utils.isInRange({value: -1, min: 0})).toBe(UIStrings.ERROR.MIN(0));
    });

    it('rejects numeric values above the max', () => {
      expect(Utils.isInRange({value: 1, max: 1})).toBe(null);
      expect(Utils.isInRange({value: 2, max: 1})).toBe(UIStrings.ERROR.MAX(1));
    });
  });
});

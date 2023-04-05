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
import { toURIParams} from './urlUtil';

describe('urlUtil', function () {
  describe('toURIParams', function () {
    it('encodes only defined parameters', function () {
      const params = {
        foo: null,
        'f o o': '?x=шеллы',
        baz: undefined,
        bar: '?x=test',
        qwerty: 'as df'
      };
      expect(toURIParams(params))
          .toEqual('f%20o%20o=%3Fx%3D%D1%88%D0%B5%D0%BB%D0%BB%D1%8B&bar=%3Fx%3Dtest&qwerty=as%20df');
    });

    it('handles empty object', function () {
      expect(toURIParams({})).toEqual('');
    });
  });
});

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
import {
  getEditionDisplayNameByShortName,
  translateNexusStatusEdition,
} from './systemInformationEdition';

const EDITION_FIELD = 'edition';
const UNKNOWN_EDITION = 'CORE';
const VERSION_FIELD = 'version';
const VERSION_VALUE = '3.0.0';

describe('systemInformationEdition', () => {
  describe('getEditionDisplayNameByShortName', () => {
    it('shouldTranslateProPlusShortName', () => {
      expect(getEditionDisplayNameByShortName('PROPLUS')).toBe('Pro+');
    });

    it('shouldTranslateEnterpriseShortName', () => {
      expect(getEditionDisplayNameByShortName('ENTERPRISE')).toBe('Enterprise');
    });

    it('shouldTranslateProShortName', () => {
      expect(getEditionDisplayNameByShortName('PRO')).toBe('Pro');
    });

    it('shouldTranslateCommunityShortName', () => {
      expect(getEditionDisplayNameByShortName('COMMUNITY')).toBe('Community');
    });

    it('shouldReturnRawValueForUnmappedShortName', () => {
      expect(getEditionDisplayNameByShortName(UNKNOWN_EDITION)).toBe(UNKNOWN_EDITION);
    });
  });

  describe('translateNexusStatusEdition', () => {
    it('shouldTranslateEditionFieldToDisplayName', () => {
      const result = translateNexusStatusEdition({[EDITION_FIELD]: 'PROPLUS'});

      expect(result[EDITION_FIELD]).toBe('Pro+');
    });

    it('shouldLeaveOtherFieldsUnchanged', () => {
      const result = translateNexusStatusEdition({
        [EDITION_FIELD]: 'ENTERPRISE',
        [VERSION_FIELD]: VERSION_VALUE,
      });

      expect(result[VERSION_FIELD]).toBe(VERSION_VALUE);
      expect(result[EDITION_FIELD]).toBe('Enterprise');
    });

    it('shouldReturnSectionUnchangedWhenNoEditionField', () => {
      const section = {[VERSION_FIELD]: VERSION_VALUE};

      expect(translateNexusStatusEdition(section)).toBe(section);
    });

    it('shouldReturnInputUnchangedWhenNullOrUndefined', () => {
      expect(translateNexusStatusEdition(undefined)).toBeUndefined();
      expect(translateNexusStatusEdition(null)).toBeNull();
    });

    it('shouldNotMutateInputSection', () => {
      const section = {[EDITION_FIELD]: 'PRO'};

      translateNexusStatusEdition(section);

      expect(section[EDITION_FIELD]).toBe('PRO');
    });
  });
});

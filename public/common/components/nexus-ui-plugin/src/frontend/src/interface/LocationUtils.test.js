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

import {scrollToUsageCenter} from './LocationUtils';

describe('scrollToUsageCenter', () => {
  let getElementByIdSpy;

  beforeEach(() => {
    jest.useRealTimers();
    getElementByIdSpy = jest.spyOn(document, 'getElementById');
  });

  afterEach(() => {
    window.location.hash = '';
    getElementByIdSpy.mockRestore();
    jest.useRealTimers();
  });

  describe('Preview UI', () => {
    it('routes to usage-metrics tab from welcome overview', () => {
      window.location.hash = '#preview/browse/welcome';
      scrollToUsageCenter();
      expect(decodeURIComponent(window.location.hash)).toBe('#preview/browse/welcome?tab=usage-metrics');
    });

    it('routes to usage-metrics tab from another preview page', () => {
      window.location.hash = '#preview/browse/browse';
      scrollToUsageCenter();
      expect(decodeURIComponent(window.location.hash)).toBe('#preview/browse/welcome?tab=usage-metrics');
    });

    it('does not scroll in Preview UI (Usage Center is a tab, not an in-page element)', () => {
      window.location.hash = '#preview/browse/welcome';
      scrollToUsageCenter();
      expect(getElementByIdSpy).not.toHaveBeenCalled();
    });
  });

  describe('Classic UI', () => {
    beforeEach(() => {
      jest.useFakeTimers();
    });

    afterEach(() => {
      jest.useRealTimers();
    });

    it('navigates to browse/welcome and scrolls to usage center element', () => {
      window.location.hash = '#admin/system/licensing';
      const mockElement = {scrollIntoView: jest.fn()};
      getElementByIdSpy.mockReturnValue(mockElement);

      scrollToUsageCenter();

      expect(window.location.hash).toBe('#browse/welcome');
      expect(window.location.hash).not.toContain('tab=usage-metrics');

      jest.advanceTimersByTime(200);

      expect(mockElement.scrollIntoView).toHaveBeenCalledWith({behavior: 'smooth'});
    });

    it('scrolls immediately when already on browse/welcome', () => {
      window.location.hash = '#browse/welcome';
      const mockElement = {scrollIntoView: jest.fn()};
      getElementByIdSpy.mockReturnValue(mockElement);

      scrollToUsageCenter();

      expect(window.location.hash).toBe('#browse/welcome');
      expect(mockElement.scrollIntoView).toHaveBeenCalledWith({behavior: 'smooth'});
      expect(window.location.hash).not.toContain('tab=usage-metrics');
    });
  });
});

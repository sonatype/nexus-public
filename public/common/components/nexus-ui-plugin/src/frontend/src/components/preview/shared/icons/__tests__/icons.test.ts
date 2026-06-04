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

import { ActionIcons, StatusIcons, NavIcons } from '../index';

describe('Icon Registries', () => {
  describe('ActionIcons', () => {
    it('maps all semantic action names', () => {
      expect(ActionIcons.Delete).toBeDefined();
      expect(ActionIcons.Add).toBeDefined();
      expect(ActionIcons.Edit).toBeDefined();
      expect(ActionIcons.Save).toBeDefined();
      expect(ActionIcons.Cancel).toBeDefined();
      expect(ActionIcons.Search).toBeDefined();
      expect(ActionIcons.Back).toBeDefined();
      expect(ActionIcons.Refresh).toBeDefined();
      expect(ActionIcons.Download).toBeDefined();
      expect(ActionIcons.ExternalLink).toBeDefined();
      expect(ActionIcons.Copy).toBeDefined();
      expect(ActionIcons.Settings).toBeDefined();
    });

    it('maps to Lucide components (functions)', () => {
      Object.values(ActionIcons).forEach(icon => {
        expect(typeof icon).toBe('object');
      });
    });
  });

  describe('StatusIcons', () => {
    it('maps all status names', () => {
      expect(StatusIcons.Loading).toBeDefined();
      expect(StatusIcons.Error).toBeDefined();
      expect(StatusIcons.Warning).toBeDefined();
      expect(StatusIcons.Success).toBeDefined();
      expect(StatusIcons.Info).toBeDefined();
    });
  });

  describe('NavIcons', () => {
    it('maps all navigation names', () => {
      expect(NavIcons.Forward).toBeDefined();
      expect(NavIcons.Back).toBeDefined();
      expect(NavIcons.Expand).toBeDefined();
      expect(NavIcons.Collapse).toBeDefined();
      expect(NavIcons.Return).toBeDefined();
    });
  });
});

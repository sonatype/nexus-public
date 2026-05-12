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

jest.mock('@sonatype/nexus-ui-plugin', () => ({
  ExtJS: { checkPermission: jest.fn(), state: jest.fn() },
}));

describe('@nosc barrel exports', () => {
  it('exports all Settings form components', () => {
    const nosc = require('../index');
    expect(nosc.SettingsForm).toBeDefined();
    expect(nosc.SettingsTextInput).toBeDefined();
    expect(nosc.SettingsCheckbox).toBeDefined();
    expect(nosc.SettingsSelect).toBeDefined();
    expect(nosc.SettingsButton).toBeDefined();
    expect(nosc.SettingsAlert).toBeDefined();
    expect(nosc.ConfirmDialog).toBeDefined();
  });

  it('exports page components', () => {
    const nosc = require('../index');
    expect(nosc.EntityTable).toBeDefined();
    expect(nosc.FilterSidebar).toBeDefined();
    expect(nosc.EmptyState).toBeDefined();
    expect(nosc.StatusBadge).toBeDefined();
    expect(nosc.PageHeader).toBeDefined();
  });

  it('exports hooks', () => {
    const nosc = require('../index');
    expect(nosc.useToast).toBeDefined();
    expect(nosc.useUnsavedChangesWarning).toBeDefined();
    expect(nosc.clearDirtyState).toBeDefined();
  });

  it('exports REST utilities', () => {
    const nosc = require('../index');
    expect(nosc.restClient).toBeDefined();
    expect(nosc.ENDPOINTS).toBeDefined();
    expect(nosc.parseApiError).toBeDefined();
  });

  it('exports icon registries', () => {
    const nosc = require('../index');
    expect(nosc.ActionIcons).toBeDefined();
    expect(nosc.StatusIcons).toBeDefined();
    expect(nosc.NavIcons).toBeDefined();
  });

  it('exports design tokens', () => {
    const nosc = require('../index');
    expect(nosc.colors).toBeDefined();
    expect(nosc.spacing).toBeDefined();
    expect(nosc.radii).toBeDefined();
    expect(nosc.fontSizes).toBeDefined();
  });
});

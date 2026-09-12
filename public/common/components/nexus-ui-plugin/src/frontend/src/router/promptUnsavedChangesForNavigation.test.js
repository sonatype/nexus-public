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

import { promptUnsavedChangesForNavigation } from './createRouter';
import * as dialog from './unsavedChangesDialog';

describe('promptUnsavedChangesForNavigation', () => {
  afterEach(() => {
    delete window.showPreviewUnsavedDialog;
    jest.restoreAllMocks();
  });

  it('uses the Radix preview dialog and not the classic modal on preview routes when registered', async () => {
    const classicSpy = jest
      .spyOn(dialog, 'showUnsavedChangesModal')
      .mockResolvedValue(true);
    window.showPreviewUnsavedDialog = jest.fn().mockResolvedValue(true);

    const result = await promptUnsavedChangesForNavigation(true);

    expect(window.showPreviewUnsavedDialog).toHaveBeenCalledTimes(1);
    expect(classicSpy).not.toHaveBeenCalled();
    expect(result).toBe(true);
  });

  it('falls back to the classic modal on preview routes when the Radix dialog is unavailable', async () => {
    const classicSpy = jest
      .spyOn(dialog, 'showUnsavedChangesModal')
      .mockResolvedValue(false);
    delete window.showPreviewUnsavedDialog;

    const result = await promptUnsavedChangesForNavigation(true);

    expect(classicSpy).toHaveBeenCalledTimes(1);
    expect(result).toBe(false);
  });

  it('uses the classic modal directly on non-preview routes', async () => {
    const classicSpy = jest
      .spyOn(dialog, 'showUnsavedChangesModal')
      .mockResolvedValue(true);
    window.showPreviewUnsavedDialog = jest.fn().mockResolvedValue(true);

    const result = await promptUnsavedChangesForNavigation(false);

    expect(classicSpy).toHaveBeenCalledTimes(1);
    expect(window.showPreviewUnsavedDialog).not.toHaveBeenCalled();
    expect(result).toBe(true);
  });
});

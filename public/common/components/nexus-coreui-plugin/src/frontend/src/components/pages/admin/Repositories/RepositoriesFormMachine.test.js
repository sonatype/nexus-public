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
import Axios from 'axios';
import {ExtJS} from '@sonatype/nexus-ui-plugin';

import RepositoriesFormMachine, {evaluationSettingsUrl} from './RepositoriesFormMachine';

jest.mock('axios');

describe('RepositoriesFormMachine URL helpers', () => {
  describe('evaluationSettingsUrl', () => {
    it('does not start with / so Axios baseURL is respected', () => {
      expect(evaluationSettingsUrl('my-repo').startsWith('/')).toBe(false);
    });

    it('produces the correct path for a simple repository name', () => {
      expect(evaluationSettingsUrl('my-repo')).toBe(
        'service/rest/v1/repositories/my-repo/evaluation-settings'
      );
    });

    it('percent-encodes repository names with special characters', () => {
      expect(evaluationSettingsUrl('my/repo')).toBe(
        'service/rest/v1/repositories/my%2Frepo/evaluation-settings'
      );
    });
  });
});

describe('RepositoriesFormMachine saveData evaluation settings', () => {
  const {saveData} = RepositoriesFormMachine.options.services;

  const hostedRepo = (format) => ({
    format,
    type: 'hosted',
    name: 'repo-1',
    attributes: {evaluation: {mode: 'INHERIT'}}
  });

  beforeEach(() => {
    jest.spyOn(ExtJS, 'state').mockReturnValue({getValue: () => true});
    Axios.post.mockResolvedValue({data: {name: 'repo-1'}});
    Axios.put.mockResolvedValue({data: {}});
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  it('saves evaluation settings for a supported format', async () => {
    await saveData({data: hostedRepo('maven2'), pristineData: {name: ''}});

    expect(Axios.put).toHaveBeenCalledWith(evaluationSettingsUrl('repo-1'), expect.objectContaining({mode: 'INHERIT'}));
  });

  it('does not enrol a new repository in an unsupported format', async () => {
    await saveData({data: hostedRepo('raw'), pristineData: {name: ''}});

    expect(Axios.put).not.toHaveBeenCalledWith(evaluationSettingsUrl('repo-1'), expect.anything());
  });
});

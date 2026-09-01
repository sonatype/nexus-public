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

import { interpret } from 'xstate';
import { waitFor } from 'xstate/lib/waitFor';
import { createUploadFormMachine, createInitialFormData, isFormDataDirty } from '../hooks/uploadFormMachine';
import type { UploadComponentField, UploadFieldDefinition } from '../upload.types';
import { restClient } from '../../../../../interface/api';

// Mock restClient used by uploadFormMachine
jest.mock('../../../../../interface/api', () => ({
  restClient: {
    post: jest.fn(),
  },
}));

// Mock APIConstants for UPLOAD endpoint
jest.mock('../../../../../constants/APIConstants', () => ({
  APIConstants: {
    REST: {
      INTERNAL: {
        UPLOAD: 'service/rest/internal/ui/upload/',
      },
    },
  },
}));

const mockPost = restClient.post as jest.Mock;

const componentFields: UploadComponentField[] = [
  { name: 'groupId', type: 'STRING', displayName: 'Group ID', group: 'Component coordinates', optional: false },
  { name: 'artifactId', type: 'STRING', displayName: 'Artifact ID', group: 'Component coordinates', optional: false },
  { name: 'version', type: 'STRING', displayName: 'Version', group: 'Component coordinates', optional: false },
  { name: 'generate-pom', type: 'BOOLEAN', displayName: 'Generate POM', group: 'Options', optional: true },
];
const assetFields: UploadFieldDefinition[] = [
  { name: 'extension', type: 'STRING', displayName: 'Extension', optional: false },
  { name: 'classifier', type: 'STRING', displayName: 'Classifier', optional: true },
];
const init = { repositoryName: 'maven-releases', componentFields, assetFields, multipleUpload: true, regexMap: null, disabledFields: new Set<string>() };
const start = (overrides = {}) => interpret(createUploadFormMachine({ ...init, ...overrides })).start();

async function validAndSubmit(overrides = {}) {
  const s = start(overrides);
  s.send({ type: 'SET_ASSET_FILE', index: 0, file: new File(['x'], 'test.jar') });
  s.send({ type: 'SET_ASSET_FIELD', index: 0, field: 'extension', value: 'jar' });
  s.send({ type: 'SET_COMPONENT_FIELD', field: 'groupId', value: 'com.example' });
  s.send({ type: 'SET_COMPONENT_FIELD', field: 'artifactId', value: 'test' });
  s.send({ type: 'SET_COMPONENT_FIELD', field: 'version', value: '1.0.0' });
  s.send({ type: 'SUBMIT' });
  return s;
}

describe('uploadFormMachine editing actions', () => {
  it('initializes with one empty asset and empty component fields', () => {
    const s = start();
    const ctx = s.getSnapshot().context;
    expect(ctx.formData.assets).toHaveLength(1);
    expect(ctx.formData.assets[0].file).toBeNull();
    expect(ctx.formData.assets[0].extension).toBe('');
    expect(ctx.formData.componentFields.groupId).toBe('');
    expect(ctx.formData.componentFields['generate-pom']).toBe(false);
    s.stop();
  });

  it('sets asset field, component field, adds and removes assets (not the last)', () => {
    const s = start();
    s.send({ type: 'SET_ASSET_FIELD', index: 0, field: 'extension', value: 'jar' });
    s.send({ type: 'SET_COMPONENT_FIELD', field: 'groupId', value: 'com.example' });
    s.send({ type: 'ADD_ASSET' });
    s.send({ type: 'ADD_ASSET' });
    expect(s.getSnapshot().context.formData.assets).toHaveLength(3);
    s.send({ type: 'REMOVE_ASSET', index: 1 });
    expect(s.getSnapshot().context.formData.assets).toHaveLength(2);
    const s2 = start();
    s2.send({ type: 'REMOVE_ASSET', index: 0 });
    expect(s2.getSnapshot().context.formData.assets).toHaveLength(1);
    s.stop(); s2.stop();
  });

  it('does not add assets when multipleUpload is false', () => {
    const s = start({ multipleUpload: false });
    s.send({ type: 'ADD_ASSET' });
    expect(s.getSnapshot().context.formData.assets).toHaveLength(1);
    s.stop();
  });

  it('auto-fills extension and filename from file and applies regexMap', () => {
    const s = start({
      assetFields: [
        { name: 'extension', type: 'STRING', optional: false },
        { name: 'artifactId', type: 'STRING', optional: true },
        { name: 'version', type: 'STRING', optional: true },
      ],
      regexMap: { regex: '^([a-zA-Z]+)-([0-9.]+)\\.([a-z]+)$', fieldList: ['artifactId', 'version', 'extension'] },
    });
    s.send({ type: 'SET_ASSET_FILE', index: 0, file: new File(['x'], 'myapp-1.0.0.jar') });
    const a = s.getSnapshot().context.formData.assets[0];
    expect(a.artifactId).toBe('myapp');
    expect(a.version).toBe('1.0.0');
    expect(a.extension).toBe('jar');
    s.stop();
  });

  it('VALIDATE flags required file/fields and rejects bad groupId; RESET clears', () => {
    const s = start();
    s.send({ type: 'VALIDATE' });
    let ctx = s.getSnapshot().context;
    expect(ctx.validationErrors.assets?.[0]?.file).toBe('File is required');
    expect(ctx.validationErrors.componentFields?.groupId).toBe('This field is required');
    s.send({ type: 'SET_COMPONENT_FIELD', field: 'groupId', value: 'bad value!' });
    s.send({ type: 'VALIDATE' });
    expect(s.getSnapshot().context.validationErrors.componentFields?.groupId)
      .toContain('Group ID must contain only letters');
    s.send({ type: 'RESET' });
    ctx = s.getSnapshot().context;
    expect(ctx.formData.componentFields.groupId).toBe('');
    expect(ctx.validationErrors).toEqual({});
    s.stop();
  });

  it('blur validates a single field without a prior VALIDATE and marks touched', () => {
    const s = start();
    s.send({ type: 'SET_COMPONENT_FIELD', field: 'groupId', value: 'bad value!' });
    expect(s.getSnapshot().context.validationErrors.componentFields?.groupId).toBeFalsy();
    s.send({ type: 'BLUR_COMPONENT_FIELD', field: 'groupId' });
    const ctx = s.getSnapshot().context;
    expect(ctx.validationErrors.componentFields?.groupId).toContain('Group ID must contain only letters');
    expect(ctx.touchedFields.has('groupId')).toBe(true);
    s.stop();
  });

  it('flags duplicate assets as not unique', () => {
    const s = start();
    s.send({ type: 'SET_ASSET_FILE', index: 0, file: new File(['a'], 'a.jar') });
    s.send({ type: 'SET_ASSET_FIELD', index: 0, field: 'extension', value: 'jar' });
    s.send({ type: 'SET_COMPONENT_FIELD', field: 'groupId', value: 'com.example' });
    s.send({ type: 'SET_COMPONENT_FIELD', field: 'artifactId', value: 'test' });
    s.send({ type: 'SET_COMPONENT_FIELD', field: 'version', value: '1.0.0' });
    s.send({ type: 'ADD_ASSET' });
    s.send({ type: 'SET_ASSET_FILE', index: 1, file: new File(['b'], 'b.jar') });
    s.send({ type: 'SET_ASSET_FIELD', index: 1, field: 'extension', value: 'jar' });
    s.send({ type: 'VALIDATE' });
    const ctx = s.getSnapshot().context;
    expect(ctx.validationErrors.assets?.[0]?.extension).toBe('Asset fields must be unique');
    expect(ctx.validationErrors.assets?.[1]?.extension).toBe('Asset fields must be unique');
    s.stop();
  });

  it('SYNC_CONFIG updates disabledFields without resetting form data', () => {
    const s = start();
    s.send({ type: 'SET_COMPONENT_FIELD', field: 'groupId', value: 'com.example' });
    s.send({ type: 'SYNC_CONFIG', config: { disabledFields: new Set(['groupId']) } });
    expect(s.getSnapshot().context.formData.componentFields.groupId).toBe('com.example');
    s.send({ type: 'VALIDATE' });
    expect(s.getSnapshot().context.validationErrors.componentFields?.groupId).toBeFalsy();
    s.stop();
  });
});

describe('uploadFormMachine submit flow', () => {
  beforeEach(() => jest.clearAllMocks());

  it('SUBMIT with validation errors returns to editing without calling the API', () => {
    const s = start();
    s.send({ type: 'SUBMIT' });
    expect(s.getSnapshot().matches('editing')).toBe(true);
    expect(mockPost).not.toHaveBeenCalled();
    s.stop();
  });

  it('valid SUBMIT posts and stores componentName', async () => {
    mockPost.mockResolvedValue({ success: true, data: 'com.example:test:1.0.0' });
    const s = await validAndSubmit();
    await waitFor(s, (st) => st.matches('editing') && st.context.submitResult !== null);
    const ctx = s.getSnapshot().context;
    expect(ctx.submitResult?.componentName).toBe('com.example:test:1.0.0');
    expect(mockPost).toHaveBeenCalledWith('service/rest/internal/ui/upload/maven-releases', expect.any(FormData));

    // Verify FormData structure matches useUploadForm.ts:484-512
    const formDataArg = mockPost.mock.calls[0][1] as FormData;
    expect(formDataArg.get('asset0')).toBeInstanceOf(File);
    expect(formDataArg.get('asset0.extension')).toBe('jar');
    expect(formDataArg.get('groupId')).toBe('com.example');
    expect(formDataArg.get('artifactId')).toBe('test');
    expect(formDataArg.get('version')).toBe('1.0.0');
    s.stop();
  });

  it('API success:false surfaces the envelope message', async () => {
    mockPost.mockResolvedValue({ success: false, 0: { message: 'Invalid component' } });
    const s = await validAndSubmit();
    await waitFor(s, (st) => st.matches('editing') && st.context.submitError !== null);
    expect(s.getSnapshot().context.submitError).toContain('Invalid component');
    s.stop();
  });

  it('HTTP 400 envelope is unwrapped (NEXUS-53344)', async () => {
    mockPost.mockRejectedValue(Object.assign(new Error('Request failed with status code 400'), {
      response: { status: 400, data: [{ success: false, message: 'Repository is read only: tf-hosted' }] },
    }));
    const s = await validAndSubmit();
    await waitFor(s, (st) => st.matches('editing') && st.context.submitError !== null);
    const err = s.getSnapshot().context.submitError!;
    expect(err).toContain('Repository is read only: tf-hosted');
    expect(err).not.toContain('Request failed with status code 400');
    s.stop();
  });
});

describe('isFormDataDirty', () => {
  const initial = createInitialFormData(componentFields, assetFields);

  it('is not dirty when nothing changed', () => {
    const current = createInitialFormData(componentFields, assetFields);
    expect(isFormDataDirty(current, initial)).toBe(false);
  });

  it('is dirty when only a file is selected', () => {
    const current = createInitialFormData(componentFields, assetFields);
    current.assets[0] = { ...current.assets[0], file: new File(['x'], 'test.jar') };
    expect(isFormDataDirty(current, initial)).toBe(true);
  });

  it('is not dirty when the same file (by identity) is present on both sides', () => {
    const file = new File(['x'], 'test.jar');
    const withFile = createInitialFormData(componentFields, assetFields);
    withFile.assets[0] = { ...withFile.assets[0], file };
    const otherWithFile = createInitialFormData(componentFields, assetFields);
    otherWithFile.assets[0] = { ...otherWithFile.assets[0], file };
    expect(isFormDataDirty(otherWithFile, withFile)).toBe(false);
  });

  it('is dirty when a component field changes', () => {
    const current = createInitialFormData(componentFields, assetFields);
    current.componentFields.groupId = 'com.example';
    expect(isFormDataDirty(current, initial)).toBe(true);
  });

  it('is dirty when asset count differs', () => {
    const current = createInitialFormData(componentFields, assetFields);
    current.assets.push({ ...current.assets[0] });
    expect(isFormDataDirty(current, initial)).toBe(true);
  });
});

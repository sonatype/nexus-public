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

import { createTagDetailMachine, createInitialContext } from '../tagDetailMachine';
import type { TagDetail } from '../../tags.types';

// Mock the API and REST client
jest.mock('../../tags.api', () => ({
  fetchTagDetail: jest.fn(),
}));

jest.mock('../../../../../../interface/api', () => ({
  restClient: {
    get: jest.fn(),
    delete: jest.fn(),
  },
  urlBuilder: {
    tags: {
      delete: (name: string) => `/service/rest/v1/tags/${encodeURIComponent(name)}`,
    },
  },
  parseApiError: jest.fn((err: Error) => ({ message: err?.message ?? 'Unknown error' })),
}));

import { fetchTagDetail } from '../../tags.api';
import { restClient } from '../../../../../../interface/api';

const mockFetchTagDetail = fetchTagDetail as jest.MockedFunction<typeof fetchTagDetail>;
const mockRestClient = restClient as jest.Mocked<typeof restClient>;

// Flush pending microtasks/macrotasks so invoked services settle.
const flush = () => new Promise((resolve) => setTimeout(resolve, 0));

describe('tagDetailMachine', () => {
  const tagName = 'test-tag';

  const mockTagDetail: TagDetail = {
    name: tagName,
    firstCreated: '2024-01-01T00:00:00.000Z',
    lastUpdated: '2024-01-15T00:00:00.000Z',
    attributes: {},
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('should create initial context correctly', () => {
    const context = createInitialContext(tagName);

    expect(context).toEqual({
      tagName,
      tagDetail: null,
      tagLoading: true,
      tagError: null,
      components: [],
      componentsLoading: false,
      componentsError: null,
      continuationToken: null,
      totalComponentCount: null,
    });
  });

  it('should start in loading state', () => {
    const machine = createTagDetailMachine(tagName);

    expect(machine.initialState.matches('loading')).toBe(true);
    expect(machine.initialState.context.tagLoading).toBe(true);
  });

  it('should transition from loading to loaded on successful fetch', async () => {
    mockFetchTagDetail.mockResolvedValue(mockTagDetail);
    mockRestClient.get.mockResolvedValue({ items: [] });

    const service = interpret(createTagDetailMachine(tagName)).start();
    await waitFor(service, (state) => state.matches('loaded'));

    expect(service.state.context.tagDetail).toEqual(mockTagDetail);
    expect(service.state.context.tagLoading).toBe(false);

    service.stop();
  });

  it('should transition from loading to loadError on failed fetch', async () => {
    mockFetchTagDetail.mockRejectedValue(new Error('Tag not found'));

    const service = interpret(createTagDetailMachine(tagName)).start();
    await waitFor(service, (state) => state.matches('loadError'));

    expect(service.state.context.tagError).toBe('Tag not found');
    expect(service.state.context.tagLoading).toBe(false);

    service.stop();
  });

  it('should handle RETRY event from loadError state', async () => {
    mockFetchTagDetail
      .mockRejectedValueOnce(new Error('Network error'))
      .mockResolvedValueOnce(mockTagDetail);
    mockRestClient.get.mockResolvedValue({ items: [] });

    const service = interpret(createTagDetailMachine(tagName)).start();
    await waitFor(service, (state) => state.matches('loadError'));

    service.send({ type: 'RETRY' });
    await waitFor(service, (state) => state.matches('loaded'));

    expect(service.state.matches('loaded')).toBe(true);

    service.stop();
  });

  it('should handle LOAD event from loaded state to refetch', async () => {
    mockFetchTagDetail.mockResolvedValue(mockTagDetail);
    mockRestClient.get.mockResolvedValue({ items: [] });

    const service = interpret(createTagDetailMachine(tagName)).start();
    await waitFor(service, (state) => state.matches('loaded'));

    // Trigger reload; the machine re-enters `loading` then settles back in `loaded`.
    service.send({ type: 'LOAD' });
    await waitFor(service, (state) => state.matches('loading'));
    await waitFor(service, (state) => state.matches('loaded'));

    expect(service.state.matches('loaded')).toBe(true);

    service.stop();
  });

  it('should handle DELETE_TAG event from loaded state', async () => {
    mockFetchTagDetail.mockResolvedValue(mockTagDetail);
    mockRestClient.get.mockResolvedValue({ items: [] });
    mockRestClient.delete.mockResolvedValue(undefined);

    const onDeleted = jest.fn();
    const service = interpret(
      createTagDetailMachine(tagName).withConfig({ actions: { onDeleted } })
    ).start();
    await waitFor(service, (state) => state.matches('loaded'));

    service.send({ type: 'DELETE_TAG' });
    await flush();

    expect(mockRestClient.delete).toHaveBeenCalled();
    expect(onDeleted).toHaveBeenCalled();

    service.stop();
  });

  it('should handle CLEAR_ERROR event', async () => {
    mockFetchTagDetail.mockRejectedValue(new Error('Tag not found'));

    const service = interpret(createTagDetailMachine(tagName)).start();
    await waitFor(service, (state) => state.context.tagError === 'Tag not found');

    service.send({ type: 'CLEAR_ERROR' });
    await waitFor(service, (state) => state.context.tagError === null);

    expect(service.state.context.tagError).toBeNull();

    service.stop();
  });
});

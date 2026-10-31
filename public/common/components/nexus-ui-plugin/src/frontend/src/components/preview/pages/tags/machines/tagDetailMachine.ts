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

import { assign, createMachine } from 'xstate';

import type { TagDetail, TaggedComponent } from '../tags.types';
import { fetchTagDetail } from '../tags.api';
import { restClient, parseApiError, urlBuilder } from '../../../../../interface/api';
import ExtJS from '../../../../../interface/ExtJS';

// =============================================================================
// Types
// =============================================================================

export interface TagDetailMachineContext {
  /** Tag name being viewed */
  tagName: string;
  /** Tag detail data */
  tagDetail: TagDetail | null;
  /** Whether data is currently loading */
  tagLoading: boolean;
  /** Error message if fetch failed */
  tagError: string | null;
  /** List of tagged components */
  components: TaggedComponent[];
  /** Whether components are loading */
  componentsLoading: boolean;
  /** Error for components fetch */
  componentsError: string | null;
  /** Continuation token for pagination */
  continuationToken: string | null;
  /** Total component count from tags API */
  totalComponentCount: number | null;
}

export type TagDetailMachineEvent =
  | { type: 'LOAD' }
  | { type: 'RETRY' }
  | { type: 'LOAD_COMPONENTS'; append?: boolean }
  | { type: 'DELETE_TAG' }
  | { type: 'CLEAR_ERROR' }
  | { type: 'SET_TAG_NAME'; tagName: string };

interface LoadResult {
  tagDetail: TagDetail;
  components: TaggedComponent[];
  continuationToken: string | null;
  totalComponentCount: number | null;
  /** Set when the best-effort component fetch failed, so the view can distinguish
   *  "search API failed" from "tag legitimately has no components". */
  componentsError: string | null;
}

interface LoadComponentsResult {
  components: TaggedComponent[];
  continuationToken: string | null;
}

// =============================================================================
// Initial Context Factory
// =============================================================================

export function createInitialContext(tagName: string): TagDetailMachineContext {
  return {
    tagName,
    tagDetail: null,
    tagLoading: true,
    tagError: null,
    components: [],
    componentsLoading: false,
    componentsError: null,
    continuationToken: null,
    totalComponentCount: null,
  };
}

// =============================================================================
// Services
// =============================================================================

async function loadTagDetailService(context: TagDetailMachineContext): Promise<LoadResult> {
  if (!context.tagName || context.tagName.trim() === '') {
    throw new Error('Tag name is required');
  }

  await ExtJS.waitForPermissions();

  // The tag detail is required (a failure surfaces the page error state), but the
  // component list is best-effort: if the search API fails we degrade to an empty
  // list rather than failing the whole page. We still capture the error message so
  // the view can distinguish a failed fetch from a tag with no components.
  // fetchTotalComponentCount already swallows its own errors and returns null.
  const [tagDetail, componentsData, totalCount] = await Promise.all([
    fetchTagDetail(context.tagName),
    fetchComponents(context.tagName)
      .then((data) => ({ ...data, error: null as string | null }))
      .catch((err) => ({
        components: [] as TaggedComponent[],
        continuationToken: null,
        error: parseApiError(err).message,
      })),
    fetchTotalComponentCount(context.tagName),
  ]);

  return {
    tagDetail,
    components: componentsData.components,
    continuationToken: componentsData.continuationToken,
    totalComponentCount: totalCount,
    componentsError: componentsData.error,
  };
}

async function fetchComponents(
  tagName: string,
  continuationToken?: string
): Promise<{ components: TaggedComponent[]; continuationToken: string | null }> {
  const params = new URLSearchParams();
  params.set('tag', tagName);

  if (continuationToken) {
    params.set('continuationToken', continuationToken);
  }

  const data = await restClient.get<{
    items?: TaggedComponent[];
    continuationToken?: string;
  }>(`/service/rest/v1/search?${params.toString()}`);

  return {
    components: (data.items || []) as TaggedComponent[],
    continuationToken: data.continuationToken || null,
  };
}

async function fetchTotalComponentCount(tagName: string): Promise<number | null> {
  try {
    // nameFilter is a substring match on the backend. pageSize=100 (server max) maximises the
    // chance the exact match is included, but with >100 similarly-named tags the exact match
    // can be pushed off the first page and this returns null (the view then shows "unknown").
    // TODO(NEXUS-53658): drop this list-endpoint derivation once GET /v1/tags/{name} returns
    // an exact componentCount.
    const params = new URLSearchParams({
      nameFilter: tagName,
      pageSize: '100',
      page: '0',
      sortField: 'name',
      sortDirection: 'asc',
    });
    const data = await restClient.get<{
      items: Array<{ name: string; componentCount: number }>;
    }>(`/service/rest/internal/ui/tags/filtered?${params.toString()}`);
    const match = data.items?.find((t) => t.name === tagName);
    return match?.componentCount ?? null;
  } catch (err) {
    // Best-effort: a broken filtered-tags endpoint must not fail the page, but log
    // so a prod outage is diagnosable rather than silently surfacing as an unknown
    // count. Returning null lets the view show "count unknown" (not a wrong number).
    console.error('Failed to fetch total component count for tag:', tagName, err);
    return null;
  }
}

async function loadMoreComponentsService(
  context: TagDetailMachineContext
): Promise<LoadComponentsResult> {
  if (!context.continuationToken) {
    return { components: [], continuationToken: null };
  }

  const result = await fetchComponents(context.tagName, context.continuationToken);
  return result;
}

async function deleteTagService(context: TagDetailMachineContext): Promise<void> {
  // Use the api layer's URL builder (single source of truth for the endpoint)
  // rather than reconstructing the path from APIConstants here.
  await restClient.delete(urlBuilder.tags.delete(context.tagName));
}

// =============================================================================
// Actions
// =============================================================================

const setTagLoading = assign<TagDetailMachineContext>({
  tagLoading: true,
  tagError: null,
  // Entering `loading` (initial load, SET_TAG_NAME, RETRY, or reload) invalidates
  // any components already in context. Clear them here — the single choke point for
  // every path into loading — so the machine never holds a stale component list
  // (e.g. the previous tag's rows, or a prior successful load being retried). The
  // full-page tagLoading spinner masks this region today, but resetting keeps the
  // model honest and robust if that guard ever changes. componentsLoading is left
  // false on purpose: setTagSuccess does not reset it, so forcing it true here would
  // strand the component-section spinner for a tag that legitimately has 0 components.
  components: [],
  continuationToken: null,
  totalComponentCount: null,
  componentsError: null,
});

const setTagSuccess = assign<TagDetailMachineContext, { data: LoadResult }>({
  tagLoading: false,
  tagError: null,
  tagDetail: (_context, event) => event.data.tagDetail,
  components: (_context, event) => event.data.components,
  continuationToken: (_context, event) => event.data.continuationToken,
  totalComponentCount: (_context, event) => event.data.totalComponentCount,
  componentsError: (_context, event) => event.data.componentsError ?? null,
});

const setTagError = assign<TagDetailMachineContext, { data: Error }>({
  tagLoading: false,
  tagError: (_context, event) =>
    event.data instanceof Error ? event.data.message : 'Failed to load tag details',
});

const setComponentsLoading = assign<TagDetailMachineContext>({
  componentsLoading: true,
  componentsError: null,
});

const appendComponents = assign<TagDetailMachineContext, { data: LoadComponentsResult }>({
  componentsLoading: false,
  components: (context, event) => [...context.components, ...event.data.components],
  continuationToken: (_context, event) => event.data.continuationToken,
});

const setComponentsError = assign<TagDetailMachineContext, { data: Error }>({
  componentsLoading: false,
  componentsError: (_context, event) =>
    event.data instanceof Error ? event.data.message : 'Failed to load components',
});

const clearError = assign<TagDetailMachineContext>({
  tagError: null,
  componentsError: null,
});

const setTagName = assign<TagDetailMachineContext, { tagName: string }>({
  tagName: (_context, event) => event.tagName,
  // Component-side context is reset by setTagLoading on entry to `loading` (which
  // SET_TAG_NAME transitions into), so it does not need to be cleared here.
});

// =============================================================================
// Machine
// =============================================================================

export function createTagDetailMachine(tagName: string) {
  return createMachine<TagDetailMachineContext, TagDetailMachineEvent>(
    {
      id: 'tagDetail',
      initial: 'loading',
      context: createInitialContext(tagName),
      states: {
        loading: {
          entry: 'setTagLoading',
          invoke: {
            id: 'loadTagDetail',
            src: 'loadTagDetail',
            onDone: {
              target: 'loaded',
              actions: 'setTagSuccess',
            },
            onError: {
              target: 'loadError',
              actions: 'setTagError',
            },
          },
        },
        loaded: {
          on: {
            LOAD: 'loading',
            RETRY: 'loading',
            LOAD_COMPONENTS: '.loadingMore',
            DELETE_TAG: 'deleting',
          },
          initial: 'idle',
          states: {
            idle: {},
            loadingMore: {
              entry: 'setComponentsLoading',
              invoke: {
                id: 'loadMoreComponents',
                src: 'loadMoreComponents',
                onDone: {
                  target: 'idle',
                  actions: 'appendComponents',
                },
                onError: {
                  target: 'idle',
                  actions: 'setComponentsError',
                },
              },
            },
          },
        },
        loadError: {
          on: {
            RETRY: 'loading',
            LOAD: 'loading',
          },
        },
        deleting: {
          invoke: {
            id: 'deleteTag',
            src: 'deleteTag',
            onDone: {
              // Return to 'loaded' as a safety net; onDeleted normally navigates
              // away (unmounting the page) before this is observed.
              target: 'loaded',
              actions: 'onDeleted',
            },
            onError: {
              target: 'loaded',
              actions: 'onDeleteError',
            },
          },
        },
      },
      on: {
        CLEAR_ERROR: {
          actions: 'clearError',
        },
        // Reload for a different tag without remounting (e.g. navigating
        // between tag detail routes, which reuses the same component).
        SET_TAG_NAME: {
          target: 'loading',
          actions: 'setTagName',
        },
      },
    },
    {
      services: {
        loadTagDetail: (context) => loadTagDetailService(context),
        loadMoreComponents: (context) => loadMoreComponentsService(context),
        deleteTag: (context) => deleteTagService(context),
      },
      actions: {
        setTagLoading,
        setTagSuccess,
        setTagError,
        setComponentsLoading,
        appendComponents,
        setComponentsError,
        clearError,
        setTagName,
        onDeleted: () => {
          // Default no-op; overridden via withConfig() in useTagDetailExtended
        },
        onDeleteError: () => {
          // Default no-op; overridden via withConfig() in useTagDetailExtended
          // so the page can surface a delete failure (e.g. a toast).
        },
      },
    }
  );
}

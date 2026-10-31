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

import { useCallback, useEffect, useMemo, useRef } from 'react';
import { useMachine } from '@xstate/react';

import { parseApiError } from '../../../../../interface/api';
import type { TagDetail, TaggedComponent } from '../tags.types';
import { createTagDetailMachine } from '../machines/tagDetailMachine';

export type { TaggedComponent };

/**
 * State for the useTagDetail hook.
 */
export interface TagDetailState {
  /** Tag detail data */
  tagDetail: TagDetail | null;
  /** Whether data is currently loading */
  loading: boolean;
  /** Error message if fetch failed */
  error: string | null;
}

/**
 * Actions returned by the useTagDetail hook.
 */
export interface TagDetailActions {
  /** Retry fetching data after an error */
  retry: () => void;
}

/**
 * Return type for the useTagDetail hook.
 */
export interface UseTagDetailResult {
  state: TagDetailState;
  actions: TagDetailActions;
}

/**
 * Props for useTagDetail hook.
 */
export interface UseTagDetailProps {
  /** Name of the tag to display */
  tagName: string;
  /** Callback when tag is deleted */
  onDeleted?: () => void;
  /** Callback when tag deletion fails, with the error message */
  onDeleteError?: (message: string) => void;
}

/**
 * Extended return type with component data (used by TagDetailPage).
 */
export interface UseTagDetailReturn {
  /** Tag detail data */
  tagDetail: TagDetail | null;
  /** Whether tag detail is loading */
  tagLoading: boolean;
  /** Error message for tag detail */
  tagError: string | null;
  /** List of tagged components */
  components: TaggedComponent[];
  /** Whether components are loading */
  componentsLoading: boolean;
  /** Error for components fetch */
  componentsError: string | null;
  /** Whether there's a next page of components */
  hasMoreComponents: boolean;
  /** Total component count */
  totalComponentCount: number | null;
  /** Load more components */
  loadMoreComponents: () => void;
  /** Retry loading tag detail */
  retry: () => void;
  /** Delete the tag */
  deleteTag: () => void;
  /** Whether a delete is currently in flight */
  deleting: boolean;
}

/**
 * Hook to fetch and manage tag detail data.
 *
 * This hook follows the three-layer architecture:
 * - Layer 1: tagDetailMachine (XState state machine)
 * - Layer 2: useTagDetail (this integration hook)
 * - Layer 3: TagDetailPage component
 *
 * The hook uses an XState machine to handle:
 * - Loading tag details with proper state transitions
 * - Error handling with retry capability
 * - Component list loading (for extended version)
 * - Tag deletion
 *
 * @param tagName - Name of the tag to fetch
 * @returns State and actions for tag detail
 *
 * @example
 * ```tsx
 * const { state, actions } = useTagDetail('release-1.0');
 *
 * if (state.loading) return <Spinner />;
 * if (state.error) return <Error onRetry={actions.retry} />;
 *
 * return <div>{state.tagDetail?.name}</div>;
 * ```
 */
export function useTagDetail(tagName: string): UseTagDetailResult {
  // useMachine binds to the machine instance from the first render, so the
  // machine is created once with the initial tag name and later tag changes
  // are pushed in via SET_TAG_NAME (see effect below).
  const initialTagName = useRef(tagName).current;
  const machine = useMemo(() => createTagDetailMachine(initialTagName), [initialTagName]);

  // Use the XState machine
  const [state, send] = useMachine(machine);

  // Reload when the tag name prop changes (route navigation reuses the hook).
  const previousTagName = useRef(tagName);
  useEffect(() => {
    if (previousTagName.current !== tagName) {
      previousTagName.current = tagName;
      send({ type: 'SET_TAG_NAME', tagName });
    }
  }, [tagName, send]);

  // Extract context values for backward-compatible API
  const { tagDetail, tagLoading, tagError } = state.context;

  /**
   * Retry fetching data after an error.
   */
  const retry = useCallback(() => {
    send({ type: 'RETRY' });
  }, [send]);

  return {
    state: {
      tagDetail,
      loading: tagLoading,
      error: tagError,
    },
    actions: {
      retry,
    },
  };
}

/**
 * Extended hook for TagDetailPage with component management.
 *
 * This version includes component list loading and tag deletion.
 *
 * @param props - Hook configuration
 * @returns Extended tag detail state and actions
 */
export function useTagDetailExtended({
  tagName,
  onDeleted,
  onDeleteError,
}: UseTagDetailProps): UseTagDetailReturn {
  // Keep the latest callbacks in refs so the machine's actions always invoke the
  // current closures. useMachine binds to the machine from the first render, so
  // without refs the onDeleted/onDeleteError captured here would go stale after a
  // SET_TAG_NAME reload (e.g. reporting the previous tag name in a toast).
  const onDeletedRef = useRef(onDeleted);
  onDeletedRef.current = onDeleted;
  const onDeleteErrorRef = useRef(onDeleteError);
  onDeleteErrorRef.current = onDeleteError;

  // Machine is created once with the initial tag name; later tag changes are
  // pushed in via SET_TAG_NAME (useMachine binds to the first machine instance).
  const initialTagName = useRef(tagName).current;
  const machine = useMemo(
    () =>
      createTagDetailMachine(initialTagName).withConfig({
        actions: {
          onDeleted: () => {
            onDeletedRef.current?.();
          },
          onDeleteError: (_context, event) => {
            // Run the thrown error through parseApiError so the user sees the
            // server-side message (e.g. "Access denied") instead of the generic
            // axios "Request failed with status code 403".
            const message =
              'data' in event && event.data !== undefined
                ? parseApiError(event.data).message
                : 'Failed to delete tag';
            onDeleteErrorRef.current?.(message);
          },
        },
      }),
    [initialTagName]
  );

  // Use the XState machine
  const [state, send] = useMachine(machine);

  // Reload when the tag name prop changes (route navigation reuses the hook).
  const previousTagName = useRef(tagName);
  useEffect(() => {
    if (previousTagName.current !== tagName) {
      previousTagName.current = tagName;
      send({ type: 'SET_TAG_NAME', tagName });
    }
  }, [tagName, send]);

  // Extract context values
  const {
    tagDetail,
    tagLoading,
    tagError,
    components,
    componentsLoading,
    componentsError,
    continuationToken,
    totalComponentCount,
  } = state.context;

  /**
   * Check if there are more components to load.
   */
  const hasMoreComponents = continuationToken !== null;

  /**
   * Whether a delete is currently in flight.
   */
  const deleting = state.matches('deleting');

  /**
   * Whether the machine is in a state that accepts a component load.
   */
  const isLoaded = state.matches('loaded');

  /**
   * Load more components (next page).
   */
  const loadMoreComponents = useCallback(() => {
    // Mirror the machine's acceptance: LOAD_COMPONENTS is only handled in `loaded`
    // (it is ignored in loading, loadError, and deleting). Guarding on isLoaded here
    // keeps the hook from dispatching an event the machine would silently drop — e.g.
    // while a delete is in flight, when continuationToken may still be non-null.
    if (isLoaded && hasMoreComponents && !componentsLoading) {
      send({ type: 'LOAD_COMPONENTS', append: true });
    }
  }, [isLoaded, hasMoreComponents, componentsLoading, send]);

  /**
   * Retry loading tag detail after error.
   */
  const retry = useCallback(() => {
    send({ type: 'RETRY' });
  }, [send]);

  /**
   * Delete the tag.
   */
  const deleteTag = useCallback(() => {
    send({ type: 'DELETE_TAG' });
  }, [send]);

  return {
    tagDetail,
    tagLoading,
    tagError,
    components,
    componentsLoading,
    componentsError,
    hasMoreComponents,
    totalComponentCount,
    loadMoreComponents,
    retry,
    deleteTag,
    deleting,
  };
}

export default useTagDetail;

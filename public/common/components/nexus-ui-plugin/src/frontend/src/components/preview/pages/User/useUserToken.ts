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
import Axios from 'axios';
import { APIConstants } from '../../../../constants/APIConstants';
import { ExtJS } from '../../../../interface/ExtJS';
import { useToast } from '../../shared';
import { createRevealTokenMachine } from './shared/revealTokenMachine';

const USER_TOKEN_BASE = '/service/rest/internal/current-user/user-token';
const ATTRIBUTES_URL = `/${APIConstants.REST.USER_TOKEN_TIMESTAMP}`;

// Backend message for the "feature disabled" state; matched on the body so unrelated 400s
// fall through to error. Must stay in sync with CurrentUserUserTokenApiResource.validateUserTokenEnabled().
export const FEATURE_DISABLED_MESSAGE = 'User-token feature is not enabled';

export type UserTokenPageState =
  | 'loading'
  | 'disabled'
  | 'no-token'
  | 'has-token'
  | 'expired-token'
  | 'error';

export interface UserTokenAttributes {
  expirationTimeTimestamp?: string;
}

export type UserTokenStatusKind = 'present' | 'absent' | 'expired';

export interface UserTokenStatus {
  enabled: boolean;
  kind: UserTokenStatusKind;
  attributes: UserTokenAttributes;
}

export interface RevealedUserToken {
  nameCode: string;
  passCode: string;
}

type AttributesResult =
  | { kind: 'present'; attributes: UserTokenAttributes }
  | { kind: 'absent' }
  | { kind: 'expired' }
  | { kind: 'disabled' };

function isFeatureDisabledBody(data: unknown): boolean {
  // JSON shape (ValidationErrorXO): exact match, JSON gives the raw string.
  // Plain-text shape: substring match, because the body may arrive wrapped
  // (e.g. `ValidationErrorXO{...message='...'}`) depending on the writer.
  if (typeof data === 'string') {
    return data.includes(FEATURE_DISABLED_MESSAGE);
  }
  if (data && typeof data === 'object' && 'message' in data) {
    const message = (data as { message: unknown }).message;
    return message === FEATURE_DISABLED_MESSAGE;
  }
  return false;
}

async function fetchAttributes(): Promise<AttributesResult> {
  try {
    const res = await Axios.get<UserTokenAttributes>(ATTRIBUTES_URL);
    return { kind: 'present', attributes: res.data ?? {} };
  } catch (err: unknown) {
    if (Axios.isAxiosError(err)) {
      if (err.response?.status === 404) return { kind: 'absent' };
      if (err.response?.status === 410) return { kind: 'expired' };
      if (err.response?.status === 400 && isFeatureDisabledBody(err.response.data)) {
        return { kind: 'disabled' };
      }
    }
    throw err;
  }
}

async function checkTokenStatus(): Promise<UserTokenStatus> {
  const result = await fetchAttributes();
  if (result.kind === 'disabled') {
    return { enabled: false, kind: 'absent', attributes: {} };
  }
  if (result.kind === 'present') {
    return { enabled: true, kind: 'present', attributes: result.attributes };
  }
  if (result.kind === 'expired') {
    return { enabled: true, kind: 'expired', attributes: {} };
  }
  return { enabled: true, kind: 'absent', attributes: {} };
}

function tokenUrl(authToken: string): string {
  return `${USER_TOKEN_BASE}?authToken=${btoa(authToken)}`;
}

async function fetchToken(): Promise<RevealedUserToken> {
  const authToken = await ExtJS.requestAuthenticationToken(
    'Please authenticate to access your user token.'
  );
  const res = await Axios.get<RevealedUserToken>(tokenUrl(authToken));
  return res.data;
}

async function generateToken(): Promise<RevealedUserToken> {
  const authToken = await ExtJS.requestAuthenticationToken(
    'Please authenticate to generate a new user token.'
  );
  const res = await Axios.post<RevealedUserToken>(tokenUrl(authToken));
  return res.data;
}

async function deleteToken(): Promise<void> {
  const authToken = await ExtJS.requestAuthenticationToken(
    'Please authenticate to reset your user token.'
  );
  await Axios.delete(tokenUrl(authToken));
}

export interface UseUserTokenReturn {
  pageState: UserTokenPageState;
  expirationTimeTimestamp: string | undefined;
  revealedToken: RevealedUserToken | null;
  actionLoading: boolean;
  handleAccess: () => void;
  handleGenerate: () => void;
  handleReset: () => void;
  handleCloseReveal: () => void;
  handleRetry: () => void;
}

/**
 * Hook that drives the User Token page via the shared revealTokenMachine.
 *
 * Loads status, exposes REVEAL/GENERATE/DELETE actions, tracks which action is in
 * flight for the correct toast wording, and derives the six page states the UI needs
 * (loading, disabled, no-token, has-token, expired-token, error) from context.
 */
export function useUserToken(): UseUserTokenReturn {
  const toast = useToast();
  const toastRef = useRef(toast);
  toastRef.current = toast;

  const machine = useMemo(
    () =>
      createRevealTokenMachine<UserTokenStatus, RevealedUserToken>({
        id: 'user-token',
        tokenType: 'user',
        services: {
          checkTokenStatus,
          fetchToken,
          generateToken,
          deleteToken,
        },
      }),
    []
  );

  const [state, send] = useMachine(machine);
  const pendingActionRef = useRef<'reveal' | 'generate' | 'delete' | null>(null);
  const wasLoadingErrorRef = useRef(false);
  const wasDeletingRef = useRef(false);
  const prevActionErrorRef = useRef<string | null>(null);

  useEffect(() => {
    const isLoadError = state.matches('loadError');
    if (isLoadError && !wasLoadingErrorRef.current) {
      toastRef.current.error('Failed to load user token status.');
    }
    wasLoadingErrorRef.current = isLoadError;
  }, [state]);

  useEffect(() => {
    const isDeleting = state.matches('deleting');
    if (wasDeletingRef.current && !isDeleting && !state.context.actionError) {
      toastRef.current.success('User token reset successfully.');
      pendingActionRef.current = null;
    }
    wasDeletingRef.current = isDeleting;
  }, [state]);

  useEffect(() => {
    const err = state.context.actionError;
    if (err && err !== prevActionErrorRef.current) {
      const action = pendingActionRef.current;
      if (action === 'reveal') {
        toastRef.current.error('Failed to retrieve user token. Please check your credentials.');
      } else if (action === 'generate') {
        toastRef.current.error('Failed to generate user token. Please check your credentials.');
      } else if (action === 'delete') {
        toastRef.current.error('Failed to reset user token. Please check your credentials.');
      }
      pendingActionRef.current = null;
    }
    prevActionErrorRef.current = err;
  }, [state.context.actionError]);

  const pageState: UserTokenPageState = ((): UserTokenPageState => {
    if (state.matches('loading')) {
      return 'loading';
    }
    if (state.matches('loadError')) {
      return 'error';
    }
    const status = state.context.status;
    if (!status) {
      return 'loading';
    }
    if (!status.enabled) {
      return 'disabled';
    }
    if (status.kind === 'expired') {
      return 'expired-token';
    }
    if (status.kind === 'absent') {
      return 'no-token';
    }
    return 'has-token';
  })();

  const actionLoading =
    state.matches('revealing') ||
    state.matches('generating') ||
    state.matches('deleting') ||
    state.matches('reloadingAfterGenerate');

  const isRevealed = state.matches('revealed');
  const revealedToken = isRevealed ? state.context.tokenValue : null;

  const handleAccess = useCallback(() => {
    pendingActionRef.current = 'reveal';
    send({ type: 'REVEAL' });
  }, [send]);

  const handleGenerate = useCallback(() => {
    pendingActionRef.current = 'generate';
    send({ type: 'GENERATE' });
  }, [send]);

  const handleReset = useCallback(() => {
    pendingActionRef.current = 'delete';
    send({ type: 'DELETE' });
  }, [send]);

  const handleCloseReveal = useCallback(() => {
    send({ type: 'HIDE' });
  }, [send]);

  const handleRetry = useCallback(() => {
    send({ type: 'RETRY' });
  }, [send]);

  return {
    pageState,
    expirationTimeTimestamp: state.context.status?.attributes.expirationTimeTimestamp,
    revealedToken,
    actionLoading,
    handleAccess,
    handleGenerate,
    handleReset,
    handleCloseReveal,
    handleRetry,
  };
}

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

import React, { useCallback, useEffect, useMemo, useRef } from 'react';
import { useInterpret, useSelector } from '@xstate/react';
import Axios from 'axios';
import type { CreateTokenForm, RoleOption, ServiceAccountToken } from './types';
import { SERVICE_ACCOUNT_TOKENS_STRINGS } from './strings';
import {
  createServiceAccountTokensMachine,
  type CreatedTokenResult,
  type ServiceAccountTokensContext,
  type ServiceAccountTokensLoadResult,
} from './serviceAccountTokensMachine';

const SERVICE_ACCOUNT_TOKENS_URL = '/service/rest/v1/security/service-account-tokens';
const ROLES_URL = '/service/rest/v1/security/roles/assignable';
const tokenUrl = (id: string) => `${SERVICE_ACCOUNT_TOKENS_URL}/${encodeURIComponent(id)}`;

const { MESSAGES } = SERVICE_ACCOUNT_TOKENS_STRINGS;

function axiosResponseOf(err: unknown): { status?: number; data?: unknown } | undefined {
  if (err && typeof err === 'object' && 'response' in err) {
    const response = (err as { response?: unknown }).response;
    if (response && typeof response === 'object') {
      return response as { status?: number; data?: unknown };
    }
  }
  return undefined;
}

function mapCreateError(err: unknown): string {
  const response = axiosResponseOf(err);
  const status = response?.status;
  if (status === 403) return MESSAGES.CREATE_ERROR_FORBIDDEN;
  if (status === 400) {
    const body = response?.data;
    const detail =
      typeof body === 'string'
        ? body
        : typeof (body as { message?: unknown })?.message === 'string'
          ? ((body as { message?: string }).message ?? '')
          : '';
    return detail || MESSAGES.CREATE_ERROR_INVALID;
  }
  const message = (response?.data as { message?: unknown })?.message;
  return typeof message === 'string' && message ? message : MESSAGES.CREATE_ERROR_GENERIC;
}

function mapRevokeError(err: unknown): string {
  const response = axiosResponseOf(err);
  const status = response?.status;
  if (status === 404) return MESSAGES.REVOKE_ERROR_NOT_FOUND;
  if (status === 403) return MESSAGES.REVOKE_ERROR_FORBIDDEN;
  const message = (response?.data as { message?: unknown })?.message;
  return typeof message === 'string' && message ? message : MESSAGES.REVOKE_ERROR_GENERIC;
}

function mapRolesError(err: unknown): React.ReactNode {
  if (axiosResponseOf(err)?.status === 403)
    return SERVICE_ACCOUNT_TOKENS_STRINGS.CREATE_MODAL.ROLES_LOAD_ERROR_FORBIDDEN;
  return SERVICE_ACCOUNT_TOKENS_STRINGS.CREATE_MODAL.ROLES_LOAD_ERROR_GENERIC;
}

async function fetchTokensRaw(): Promise<ServiceAccountToken[]> {
  const response = await Axios.get(SERVICE_ACCOUNT_TOKENS_URL);
  return Array.isArray(response?.data) ? response.data : [];
}

async function fetchRolesRaw(): Promise<RoleOption[]> {
  const response = await Axios.get(ROLES_URL);
  return Array.isArray(response?.data) ? response.data : [];
}

async function createTokenRaw(form: CreateTokenForm): Promise<CreatedTokenResult> {
  const payload: Record<string, unknown> = {
    name: form.name,
    roleId: form.roleId,
  };
  if (form.description) payload.description = form.description;
  if (form.expirationDays != null) payload.expirationDays = form.expirationDays;

  try {
    const response = await Axios.post(SERVICE_ACCOUNT_TOKENS_URL, payload);
    const data = response?.data ?? {};
    const rawToken: string = data.token ?? '';
    const token: ServiceAccountToken = {
      id: data.id,
      name: data.name,
      description: data.description ?? '',
      roleId: data.roleId,
      createdBy: data.createdBy ?? '',
      createdAt: data.createdAt,
      expiresAt: data.expiresAt ?? null,
      lastUsedAt: data.lastUsedAt ?? null,
    };
    return { token, rawToken };
  } catch (err: unknown) {
    throw new Error(mapCreateError(err));
  }
}

async function revokeTokenRaw(tokenId: string): Promise<void> {
  try {
    await Axios.delete(tokenUrl(tokenId));
  } catch (err: unknown) {
    throw new Error(mapRevokeError(err));
  }
}

interface UseServiceAccountTokensOptions {
  canCreate?: boolean;
}

interface PendingCommand<TResult> {
  commandId: number;
  resolve: (value: TResult) => void;
  reject: (reason: unknown) => void;
}

export interface UseServiceAccountTokensReturn {
  tokens: ServiceAccountToken[];
  roles: RoleOption[];
  rolesError: React.ReactNode | null;
  loading: boolean;
  error: string | null;
  setError: (err: string | null) => void;
  /**
   * Enqueue a list refresh. The returned promise resolves immediately after the
   * REFRESH event is sent; the machine drives the actual reload asynchronously,
   * so observe `loading` to know when the refresh finishes.
   */
  loadAll: () => Promise<void>;
  createToken: (form: CreateTokenForm) => Promise<CreatedTokenResult>;
  revokeToken: (tokenId: string) => Promise<void>;
}

/**
 * XState-backed replacement for useServiceAccountTokensApi.
 *
 * The machine owns list load, create, revoke, and their error recovery paths. This hook
 * bridges each command-style call to the machine's promise-based invoke: it stamps every
 * SUBMIT_CREATE / REVOKE with a monotonic commandId, then subscribes to the machine and
 * resolves/rejects the caller's promise when the transition for THAT command completes.
 * The commandId gating is what lets concurrent callers not cross-resolve each other.
 */
export function useServiceAccountTokens(
  options: UseServiceAccountTokensOptions = {}
): UseServiceAccountTokensReturn {
  const { canCreate = false } = options;

  const machine = useMemo(
    () =>
      createServiceAccountTokensMachine({
        loadAll: async (): Promise<ServiceAccountTokensLoadResult> => {
          const tokensPromise = fetchTokensRaw();
          const rolesPromise = canCreate
            ? fetchRolesRaw().then(
                (data) => ({ data, error: null as React.ReactNode | null }),
                (err) => ({ data: [] as RoleOption[], error: mapRolesError(err) })
              )
            : Promise.resolve({ data: [] as RoleOption[], error: null as React.ReactNode | null });

          const [tokens, rolesResult] = await Promise.all([tokensPromise, rolesPromise]);
          return { tokens, roles: rolesResult.data, rolesError: rolesResult.error };
        },
        createToken: (form) => createTokenRaw(form),
        revokeToken: (tokenId) => revokeTokenRaw(tokenId),
      }),
    [canCreate]
  );

  const service = useInterpret(machine);

  const tokens = useSelector(service, (s) => s.context.tokens);
  const roles = useSelector(service, (s) => s.context.roles);
  const rolesError = useSelector(service, (s) => s.context.rolesError);
  const loadError = useSelector(service, (s) => s.context.loadError);
  const createError = useSelector(service, (s) => s.context.createError);
  const revokeError = useSelector(service, (s) => s.context.revokeError);
  const loading = useSelector(
    service,
    (s) => s.matches('loading') || s.matches('submittingCreate') || s.matches('revoking')
  );

  // Combined error surface for the page's SettingsAlert. Priority is load > create > revoke.
  const error = loadError ?? createError ?? revokeError;

  const setError = useCallback(
    (_err: string | null) => {
      service.send({ type: 'CLEAR_ERROR' });
    },
    [service]
  );

  const loadAll = useCallback(async (): Promise<void> => {
    service.send({ type: 'REFRESH' });
  }, [service]);

  const pendingCreateRef = useRef<PendingCommand<CreatedTokenResult> | null>(null);
  const pendingRevokeRef = useRef<PendingCommand<void> | null>(null);

  useEffect(() => {
    const subscription = service.subscribe((state) => {
      const pendingCreate = pendingCreateRef.current;
      if (pendingCreate && pendingCreate.commandId === state.context.commandId) {
        if (state.context.lastCreated && !state.matches('submittingCreate')) {
          pendingCreateRef.current = null;
          pendingCreate.resolve(state.context.lastCreated);
        } else if (state.context.createError && !state.matches('submittingCreate')) {
          pendingCreateRef.current = null;
          pendingCreate.reject(new Error(state.context.createError));
        }
      }

      const pendingRevoke = pendingRevokeRef.current;
      if (pendingRevoke && pendingRevoke.commandId === state.context.commandId) {
        if (state.context.revokeError && !state.matches('revoking')) {
          pendingRevokeRef.current = null;
          pendingRevoke.reject(new Error(state.context.revokeError));
        } else if (
          !state.matches('revoking') &&
          state.context.pendingRevokeId === null &&
          !state.context.revokeError
        ) {
          pendingRevokeRef.current = null;
          pendingRevoke.resolve();
        }
      }
    });
    return () => subscription.unsubscribe();
  }, [service]);

  const createToken = useCallback(
    (form: CreateTokenForm): Promise<CreatedTokenResult> => {
      return new Promise((resolve, reject) => {
        const snapshot = service.getSnapshot() as {
          context: ServiceAccountTokensContext;
          matches: (state: string) => boolean;
        };
        // Guard: SUBMIT_CREATE is only handled in the idle state. Sending it while
        // another command is in flight would be silently dropped by the machine but
        // would still overwrite pendingCreateRef, orphaning the first caller's promise.
        if (!snapshot.matches('idle')) {
          reject(new Error(MESSAGES.OPERATION_IN_PROGRESS));
          return;
        }
        const commandId = snapshot.context.commandId + 1;
        pendingCreateRef.current = { commandId, resolve, reject };
        service.send({ type: 'SUBMIT_CREATE', form, commandId });
      });
    },
    [service]
  );

  const revokeToken = useCallback(
    (tokenId: string): Promise<void> => {
      return new Promise((resolve, reject) => {
        const snapshot = service.getSnapshot() as {
          context: ServiceAccountTokensContext;
          matches: (state: string) => boolean;
        };
        // Guard: REVOKE is only handled in the idle state; see createToken above.
        if (!snapshot.matches('idle')) {
          reject(new Error(MESSAGES.OPERATION_IN_PROGRESS));
          return;
        }
        const commandId = snapshot.context.commandId + 1;
        pendingRevokeRef.current = { commandId, resolve, reject };
        service.send({ type: 'REVOKE', tokenId, commandId });
      });
    },
    [service]
  );

  return {
    tokens,
    roles,
    rolesError,
    loading,
    error,
    setError,
    loadAll,
    createToken,
    revokeToken,
  };
}

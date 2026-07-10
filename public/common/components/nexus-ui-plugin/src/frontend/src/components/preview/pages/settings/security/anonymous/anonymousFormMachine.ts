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

import { assign } from 'xstate';
import { APIConstants } from '../../../../../../constants/APIConstants';
import { createFormMachine, type FormContext, type ValidationErrors } from '../../../../../../interface/form';
import { restClient } from '../../../../../../interface/api';
import { AnonymousSettings, RealmType, DEFAULT_ANONYMOUS_SETTINGS } from './types';

const { REST } = APIConstants;

/**
 * Form data shape for anonymous access settings
 */
export type AnonymousFormData = AnonymousSettings;

/**
 * Extended form context with realm types reference data
 */
interface AnonymousFormContext extends FormContext<AnonymousFormData> {
  realmTypes: RealmType[];
}

/**
 * Validate anonymous form data.
 * UserId is always required regardless of enabled state.
 */
function validateAnonymous(data: AnonymousFormData): ValidationErrors {
  const errors: ValidationErrors = {};

  if (!data.userId?.trim()) {
    errors.userId = 'Username is required';
  }

  if (!data.realmName?.trim()) {
    errors.realmName = 'Realm is required';
  }

  return errors;
}

/**
 * Create an anonymous access form machine.
 *
 * This is a simple settings form with no type variants:
 * - Loads current anonymous settings + available realm types on mount
 * - Validates userId is non-empty
 * - Save service is provided via useForm options
 */
export function createAnonymousFormMachine() {
  return createFormMachine({
    id: 'anonymous-form',
    context: {
      data: { ...DEFAULT_ANONYMOUS_SETTINGS } as AnonymousFormData,
      realmTypes: [] as RealmType[],
    } as AnonymousFormContext,
    stayEditableAfterSave: true, // Allow continued editing after save
    actions: {
      validate: assign((ctx: AnonymousFormContext) => ({
        validationErrors: validateAnonymous(ctx.data),
      })),
    },
    services: {
      load: async () => {
        const [settings, realmTypes] = await Promise.all([
          restClient.get<AnonymousSettings>(REST.INTERNAL.ANONYMOUS_SETTINGS).catch((err: unknown) => {
            console.error('Failed to load anonymous settings:', err);
            throw err;
          }),
          restClient.get<RealmType[]>(REST.INTERNAL.REALMS_TYPES).then((data: unknown) => {
            return Array.isArray(data) ? (data as RealmType[]) : [];
          }).catch((err: unknown) => {
            console.warn('Could not load realm types:', err);
            return [] as RealmType[];
          }),
        ]);

        const initialData: AnonymousFormData = {
          enabled: settings.enabled,
          userId: settings.userId,
          realmName: settings.realmName,
        };

        return {
          data: initialData,
          realmTypes,
        };
      },
    },
  });
}

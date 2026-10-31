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

import { useCallback, useMemo, useState } from 'react';
import { restClient, ENDPOINTS, parseApiError } from '../../../../../../interface/api';
import { useForm, type FormContext } from '../../../../../../interface/form';
import { useToast } from '../../../../shared';
import { createDataStoreFormMachine, toUpdatePayload, type DataStoreFormData } from './dataStoreFormMachine';
import { validateJdbcParameters, calculateEffectiveConfig, type EffectiveParameter } from './types';
import { JdbcParameter } from './JdbcParameterEditor';

function humanizeDataStoreError(message: string): string {
  if (message.includes('invalid or contains unknown')) {
    return 'One or more advanced JDBC parameters are not recognized by the database. Remove unknown parameters and try again.';
  }
  return message;
}

export function useDataStoreForm() {
  const toast = useToast();
  const machine = useMemo(() => createDataStoreFormMachine(), []);

  const form = useForm<DataStoreFormData>(machine, {
    services: {
      save: async (ctx: FormContext<DataStoreFormData>) => {
        try {
          const result = await restClient.put(ENDPOINTS.DATASTORE, toUpdatePayload(ctx.data));
          toast.success('Data store configuration saved successfully');
          return result;
        } catch (err) {
          throw new Error(humanizeDataStoreError(parseApiError(err).message));
        }
      },
    },
  });

  const parameters = form.data.jdbcParameters;

  // Local UI-only state: reset-confirm banner + "show all row errors" after a blocked save.
  const [showResetConfirm, setShowResetConfirm] = useState(false);
  const [showAllValidation, setShowAllValidation] = useState(false);

  const { validations: parameterValidations, hasBlockingErrors: hasParameterErrors } = useMemo(
    () => validateJdbcParameters(parameters),
    [parameters],
  );

  const databaseType = useMemo(() => {
    const url = form.data.jdbcUrl;
    if (!url) return 'Not configured';
    if (url.includes('postgresql')) return 'PostgreSQL';
    if (url.includes('h2')) return 'H2';
    if (url.includes('mysql')) return 'MySQL';
    if (url.includes('oracle')) return 'Oracle';
    if (url.includes('sqlserver')) return 'SQL Server';
    return 'Unknown';
  }, [form.data.jdbcUrl]);

  const effectiveConfig = useMemo<EffectiveParameter[]>(() => {
    const jdbc = calculateEffectiveConfig(parameters);
    const poolEntry: EffectiveParameter = {
      name: 'maximumConnectionPool',
      value: String(form.data.maximumConnectionPool || 10),
      source: form.data.maximumConnectionPool !== form.state.context.pristineData.maximumConnectionPool
        ? 'Custom' : 'Default',
    };
    return [poolEntry, ...jdbc];
  }, [parameters, form.data.maximumConnectionPool, form.state.context.pristineData.maximumConnectionPool]);

  const setParameters = useCallback(
    (next: JdbcParameter[]) => form.send({ type: 'UPDATE', name: 'jdbcParameters', value: next } as never),
    [form],
  );

  const setMaxPool = useCallback(
    (value: string) => form.send({ type: 'UPDATE', name: 'maximumConnectionPool', value } as never),
    [form],
  );

  const requestResetParams = useCallback(() => setShowResetConfirm(true), []);
  const cancelResetParams = useCallback(() => setShowResetConfirm(false), []);
  const confirmResetParams = useCallback(() => {
    setParameters(parameters.filter((p) => p.isDefault && !p.isCustom));
    setShowResetConfirm(false);
  }, [parameters, setParameters]);

  const canSave = !form.isPristine && !hasParameterErrors && !form.validationErrors.maximumConnectionPool;

  const submit = useCallback(() => {
    if (hasParameterErrors) {
      setShowAllValidation(true);
      return;
    }
    setShowAllValidation(false);
    form.submit();
  }, [form, hasParameterErrors]);

  const reset = useCallback(() => {
    form.reset();
    setShowAllValidation(false);
    setShowResetConfirm(false);
  }, [form]);

  return {
    ...form,
    submit,
    reset,
    databaseType,
    effectiveConfig,
    parameterValidations,
    hasParameterErrors,
    canSave,
    showAllValidation,
    setParameters,
    setMaxPool,
    showResetConfirm,
    requestResetParams,
    confirmResetParams,
    cancelResetParams,
  };
}

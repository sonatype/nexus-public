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

/**
 * Form utilities for building XState-powered forms with Radix UI components.
 *
 * @example
 * ```tsx
 * import { createFormMachine, useForm } from '@sonatype/nexus-ui-plugin/interface/form';
 *
 * const userFormMachine = createFormMachine({
 *   id: 'user-form',
 *   context: { data: { name: '', email: '' } },
 *   actions: {
 *     validate: (ctx) => ({
 *       name: !ctx.data.name ? 'Required' : null,
 *     }),
 *   },
 *   services: {
 *     save: async (ctx) => api.saveUser(ctx.data),
 *   },
 * });
 *
 * function UserForm() {
 *   const form = useForm(userFormMachine, { formId: 'user-form' });
 *
 *   return (
 *     <form onSubmit={(e) => { e.preventDefault(); form.submit(); }}>
 *       <input {...form.field('name')} />
 *       <button type="submit">Save</button>
 *     </form>
 *   );
 * }
 * ```
 */

// Machine factory
export { createFormMachine } from './createFormMachine';
export type { FormMachineConfig } from './createFormMachine';

// React hook
export { useForm } from './useForm';

// Types
export type {
  FormContext,
  FormEvent,
  ValidationErrors,
  ValidateFn,
  FieldProps,
  CheckboxProps,
  SelectProps,
  UseFormReturn,
} from './types';

// Utilities
export { hasValidationErrors, extractErrorMessage, toPathArray } from './utils';

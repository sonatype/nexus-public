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

import { RepositoryFormErrors } from '../types';

/**
 * Errors passed to a repository facet, in either of two shapes:
 *
 * - a flat `Record<string, string>` keyed by dot notation (`'proxy.remoteUrl'`), which is
 *   what `validateRepository` in `repositoryFormMachine.ts` produces and what
 *   `createFormMachine` carries in `context.validationErrors`. **This is the only shape any
 *   production caller passes today**: `useRepositoryForm` types its `errors` as
 *   `Record<string, string | undefined>` and `RepositoryForm.tsx` forwards exactly that to
 *   every facet;
 * - a nested {@link RepositoryFormErrors} object (`{ proxy: { remoteUrl } }`), which is the
 *   shape the sibling facets still *declare* as their `errors` prop type. No in-repo caller
 *   constructs one — it is a type annotation only.
 *
 * **Transitional shim.** The nested branch is unreachable in production and exists so the
 * two shapes cannot silently diverge while the facets' declared prop types still say nested.
 * NEXUS-54771 fixed ProxyFacet and HttpClientFacet, which read nested-only keys and therefore
 * rendered no message at all under the form machine. The correct end state is one shape (flat,
 * as `DockerFacet` already reads) and deleting this union — but that is a cross-format change
 * across ~14 facets and is tracked separately, not in this ticket.
 */
export type FacetErrors = RepositoryFormErrors | Record<string, string | undefined>;

/**
 * Resolve an error value from either a flat dot-notation errors map or a nested errors object.
 *
 * The flat key wins when both are present, because the form machine is the authoritative
 * source of validation state during editing.
 */
export function resolveError(
  errors: FacetErrors | undefined,
  flatKey: string,
  nested: (e: RepositoryFormErrors) => string | undefined
): string | undefined {
  if (!errors) return undefined;
  // Try flat key first (form machine produces flat dot-notation keys)
  const flat = (errors as Record<string, string | undefined>)[flatKey];
  if (flat) return flat;
  // Fall back to nested access
  return nested(errors as RepositoryFormErrors);
}

export default resolveError;

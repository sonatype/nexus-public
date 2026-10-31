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

import { ExtJS } from '../../../../interface/ExtJS';
import { EDITION_ID_STATE_KEY } from '../../../../interface/editionUtils';

/**
 * Reactive hook that returns the edition identifier published by the backend
 * (Rapture state key {@code editionId}, e.g. {@code nexus-professional-edition}).
 *
 * Returns {@code undefined} while the state subsystem has not yet resolved.
 * Callers that need a stable display string should pass the result to
 * {@code getEditionDisplayName} from {@code editionUtils}.
 */
export function useEditionId(): string | undefined {
  return ExtJS.useState?.(() =>
    ExtJS.state()?.getValue?.(EDITION_ID_STATE_KEY),
  );
}

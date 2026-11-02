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

import React from 'react';
import { PackageX } from 'lucide-react';

import { EmptyState } from '../../../../shared/EmptyState';

// TODO(Hermes): final copy. Replaced by the live tab in E1-03 / E1-06.
export function QuarantineLivePlaceholder(): JSX.Element {
  return (
    <EmptyState
      icon={PackageX}
      title="Quarantine: coming soon"
      description="Coming soon. Quarantined components will appear here."
    />
  );
}

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

import type React from 'react';

import AnonymousAccessStep from './AnonymousAccessStep';
import ChangePasswordStep from './ChangePasswordStep';
import CommunityDiscoverStep from './steps/CommunityDiscoverStep/CommunityDiscoverStep';
import EulaStep from './steps/EulaStep/EulaStep';

export type StepComponent = React.FC;

// Maps onboarding step type (from the server) to its React component. To add a
// new step, import its component and add an entry to this literal.
export const stepRegistry: Record<string, StepComponent> = {
  ChangeAdminPassword: ChangePasswordStep,
  ConfigureAnonymousAccess: AnonymousAccessStep,
  CommunityDiscover: CommunityDiscoverStep,
  CommunityEula: EulaStep,
};

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

import React, { useEffect } from 'react';
import { UIView, useCurrentStateAndParams, useRouter } from '@uirouter/react';
import { Page } from '../../layout';
import { NxH1, NxP, NxTile } from '@sonatype/react-shared-components';
import { RouteNames } from '../../../constants/RouteNames';
import { isVisible } from '../../../interface/NavigationUtils';

export function DirectoryPage({ routeName, text, description, children, ...attr }) {
  const { state } = useCurrentStateAndParams();
  const router = useRouter();

  useEffect(() => {
    if (state.name === routeName) {
      const allStates = router.stateRegistry.get();
      const visibleChildRoutes = allStates.filter(
        (childState) =>
          childState.name.startsWith(`${routeName}.`) &&
          !childState?.data?.visibilityRequirements?.ignoreForMenuVisibilityCheck &&
          isVisible(childState.data?.visibilityRequirements)
      );

      if (visibleChildRoutes.length === 0) {
        router.stateService.go(RouteNames.MISSING_ROUTE);
      }
    }
  }, [state.name, routeName, router]);

  if (state.name !== routeName) {
    return <UIView />;
  }

  return (
    <Page {...attr}>
      <NxH1>{text}</NxH1>

      <NxP>{description}</NxP>

      <NxTile>
        <NxTile.Content>{children}</NxTile.Content>
      </NxTile>
    </Page>
  );
}

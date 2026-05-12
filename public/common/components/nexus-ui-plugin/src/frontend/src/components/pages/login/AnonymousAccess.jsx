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
import { Flex, Link } from '@radix-ui/themes';
import { useRouter } from '@uirouter/react';
import UIStrings from '../../../constants/UIStrings';
import { RouteNames } from "../../../constants/RouteNames";

const { CONTINUE_WITHOUT_LOGIN } = UIStrings;

/**
 * Link component that allows users to continue without logging in when anonymous access is enabled.
 * Properly handles returnTo parameter for redirect after anonymous navigation.
 */
export default function AnonymousAccess() {
  const router = useRouter();

  const handleContinueWithoutLogin = (e) => {
    e.preventDefault();
    try {
      const returnTo = router.globals.params.returnTo;
      if (returnTo) {
        const decodedReturnTo = atob(returnTo);
        // Validate decoded URL is a safe relative path (prevents open redirect)
        if (decodedReturnTo.startsWith('/') || decodedReturnTo.startsWith('#')) {
          router.urlService.url(decodedReturnTo);
        } else {
          router.stateService.go(RouteNames.MISSING_ROUTE);
        }
      } else {
        router.stateService.go('browse.welcome');
      }
    } catch (ex) {
      console.warn('redirection unsuccessful: ', ex);
      router.stateService.go(RouteNames.MISSING_ROUTE);
    }
  };

  return (
    <Flex justify="center" pt="3" className="login-footer">
      <Link
        href="#browse/welcome"
        onClick={handleContinueWithoutLogin}
        className="login-continue"
        size="2"
        data-testid="continue-without-login-button"
        data-analytics-id="nxrm-login-anonymous"
      >
        {CONTINUE_WITHOUT_LOGIN}
      </Link>
    </Flex>
  );
}

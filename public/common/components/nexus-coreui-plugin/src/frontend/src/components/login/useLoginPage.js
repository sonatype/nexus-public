/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Open Source Version is distributed with Sencha Ext JS pursuant to a FLOSS Exception agreed upon
 * between Sonatype, Inc. and Sencha Inc. Sencha Ext JS is licensed under GPL v3 and cannot be redistributed as part of a
 * closed source work.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */

import {useCallback, useEffect, useMemo, useState} from 'react';
import {ExtJS, useForm} from '@sonatype/nexus-ui-plugin';
import {useRouter} from '@uirouter/react';
import {createLoginFormMachine} from './loginFormMachine';

export default function useLoginPage() {
  const router = useRouter();

  const machine = useMemo(() => createLoginFormMachine(), []);
  const form = useForm(machine);

  const edition = ExtJS.useState(() => ExtJS.state().getEdition?.()) || 'PRO';
  const samlEnabled = !!ExtJS.useState(() => ExtJS.state().getValue('samlEnabled', false));
  const oauth2Enabled = !!ExtJS.useState(() => ExtJS.state().getValue('oauth2Enabled', false));
  const contextPath = ExtJS.useState(() => ExtJS.state().getValue('nexus-context-path', '')) || '';
  const showSsoLogin = samlEnabled || oauth2Enabled;
  const showAnonymousAccess = !!ExtJS.useState(() => ExtJS.state().getValue('anonymousUsername', false));

  // Initial admin password file path info
  const adminPasswordFilePath = ExtJS.useState(() => ExtJS.state().getValue('admin.password.file', null));
  const onboardingRequired = !!ExtJS.useState(() => ExtJS.state().getValue('onboarding.required', false));
  const isCloudEnvironment = !!ExtJS.useState(() => ExtJS.state().getValue('isCloud', false));

  const contextPrefix = contextPath === '/' ? '' : contextPath;

  const handleSsoLogin = useCallback(() => {
    const returnTo = router.globals.params.returnTo;
    const hash = returnTo ? `hash=${encodeURIComponent(returnTo)}` : '';
    let basePath = '';
    if (samlEnabled) {
      basePath = `${contextPrefix}/saml`;
    } else if (oauth2Enabled) {
      basePath = `${contextPrefix}/oidc/login`;
    }
    form.send({type: 'SSO_LOGIN', url: `${basePath}?${hash}`});
  }, [form.send, router, samlEnabled, oauth2Enabled, contextPrefix]);

  const isSsoRedirecting = form.state.context.isSsoRedirecting ?? false;
  const rateLimitWarning = form.state.context.rateLimitWarning ?? false;
  const retryAfterSeconds = form.state.context.retryAfterSeconds ?? null;

  const [secondsLeft, setSecondsLeft] = useState(null);

  useEffect(() => {
    if (retryAfterSeconds == null) {
      setSecondsLeft(null);
      return;
    }
    setSecondsLeft(retryAfterSeconds);
    const intervalId = setInterval(() => {
      setSecondsLeft((prev) => {
        if (prev !== null && prev <= 1) {
          clearInterval(intervalId);
          return null;
        }
        return prev !== null ? prev - 1 : null;
      });
    }, 1000);
    return () => clearInterval(intervalId);
  }, [retryAfterSeconds]);

  const isRateLimited = secondsLeft !== null;

  const handleContinueWithoutLogin = useCallback(() => {
    window.location.hash = '#browse/welcome';
  }, []);

  return {
    ...form,
    edition,
    showSsoLogin,
    showAnonymousAccess,
    isSsoRedirecting,
    rateLimitWarning,
    secondsLeft,
    isRateLimited,
    handleSsoLogin,
    handleContinueWithoutLogin,
    adminPasswordFilePath,
    onboardingRequired,
    isCloudEnvironment,
  };
}

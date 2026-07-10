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
import {Box, Callout, Card, Flex, Heading, Text, TextField, Button, Link} from '@radix-ui/themes';
import {Info} from 'lucide-react';
import {ExtJS, UIStrings} from '@sonatype/nexus-ui-plugin';
import {useTheme} from '../../contexts/ThemeContext';
import {ThemeSwitcher} from '../ThemeSwitcher/ThemeSwitcher';
import useLoginPage from './useLoginPage';

const {
  SIGNIN_TITLE, SIGNIN_BUTTON, SIGNIN_BUTTON_LOADING, SIGNIN_FOOTER_LEAD,
  SSO_BUTTON, SSO_BUTTON_LOADING, SSO_DIVIDER_LABEL,
  USERNAME_LABEL, PASSWORD_LABEL, CONTINUE_WITHOUT_LOGIN,
  INITIAL_PASSWORD_MESSAGE, ERRORS,
} = UIStrings;

import proLogo from '../../../../art/logos/logo-pro-edition-header.svg';
import proDarkLogo from '../../../../art/logos/logo-pro-edition-header-dark.svg';
import ceLogo from '../../../../art/logos/logo-community-edition-header.svg';
import ceDarkLogo from '../../../../art/logos/logo-community-edition-header-dark.svg';
import coreLogo from '../../../../art/logos/logo-core-edition-header.svg';
import coreDarkLogo from '../../../../art/logos/logo-core-edition-header-dark.svg';

import './LoginPageRadix.scss';

const LOGOS = {
  COMMUNITY: {light: ceLogo, dark: ceDarkLogo},
  PRO: {light: proLogo, dark: proDarkLogo},
  CORE: {light: coreLogo, dark: coreDarkLogo},
};

function getMarkOnlyLogoUrl() {
  try {
    return ExtJS.urlOf('static/rapture/resources/favicon.svg');
  } catch {
    return '/static/rapture/resources/favicon.svg';
  }
}

export default function LoginPageRadix() {
  const {
    field,
    submit,
    isSaving,
    saveError,
    data,
    isSsoRedirecting,
    edition,
    showSsoLogin,
    showAnonymousAccess,
    handleSsoLogin,
    adminPasswordFilePath,
    onboardingRequired,
    isCloudEnvironment,
    rateLimitWarning,
    isRateLimited,
    secondsLeft,
  } = useLoginPage();

  const {effectiveTheme} = useTheme();
  const isDark = effectiveTheme === 'dark';
  const logos = LOGOS[edition] || LOGOS.PRO;
  const wordmarkLogo = isDark ? logos.dark : logos.light;
  const markOnlyLogoUrl = getMarkOnlyLogoUrl();

  const showInitialPasswordInfo = !!adminPasswordFilePath && !isCloudEnvironment && onboardingRequired;

  const handleFormSubmit = (e) => {
    e.preventDefault();
    submit();
  };

  return (
    <div className="nxrm-login-page-radix">
      <header className="nxrm-login-header" role="banner">
        <a
          href="#browse/welcome"
          className="nxrm-login-header__link"
          title="Home"
          aria-label="Sonatype Nexus Repository home"
          onClick={(e) => {
            e.preventDefault();
            window.location.hash = '#browse/welcome';
          }}
        >
          <img
            src={wordmarkLogo}
            alt=""
            className="nxrm-login-header__logo"
            width={220}
            height={32}
            decoding="async"
          />
        </a>
        <ThemeSwitcher />
      </header>

      <div className="nxrm-login-content">
        <Card className="nxrm-login-card" size="4">
          <form id="nxrm-login-form" className="nxrm-login-card__content" onSubmit={handleFormSubmit}>
            <Flex direction="column" gap="5">
              <Flex direction="column" align="center" gap="4" className="nxrm-login-card__brand">
                <img
                  src={markOnlyLogoUrl}
                  alt=""
                  className="nxrm-login-card__mark"
                  width={48}
                  height={48}
                  decoding="async"
                />
                <Heading as="h1" size="6" weight="medium" className="nxrm-login-card__title">
                  {SIGNIN_TITLE}
                </Heading>
              </Flex>

              {/* Initial Password Info */}
              {showInitialPasswordInfo && (
                <Callout.Root color="blue" size="2">
                  <Callout.Icon>
                    <Info size={16} />
                  </Callout.Icon>
                  <Callout.Text>
                    <Flex direction="column" gap="1">
                      <Text size="2">{INITIAL_PASSWORD_MESSAGE}</Text>
                      <Text as="p" weight="medium" size="2" className="nxrm-login-filepath">
                        {adminPasswordFilePath}
                      </Text>
                    </Flex>
                  </Callout.Text>
                </Callout.Root>
              )}

              {/* Rate Limit Warning */}
              {rateLimitWarning && (
                <Callout.Root role="alert" color="amber" size="2">
                  <Callout.Icon>
                    <Info size={16} />
                  </Callout.Icon>
                  <Callout.Text>
                    {ERRORS.RATE_LIMITED(secondsLeft)}
                  </Callout.Text>
                </Callout.Root>
              )}

              {/* Error Message */}
              {saveError && (
                <Box className="nxrm-login-error">
                  <Text size="2" color="red">{saveError}</Text>
                </Box>
              )}

              {/* SSO Login */}
              {showSsoLogin && (
                <Button
                  type="button"
                  size="3"
                  onClick={handleSsoLogin}
                  disabled={isSsoRedirecting}
                  className="nxrm-login-submit"
                >
                  {isSsoRedirecting ? SSO_BUTTON_LOADING : SSO_BUTTON}
                </Button>
              )}

              {/* Divider between SSO and local login */}
              {showSsoLogin && (
                <Flex align="center" gap="3" className="nxrm-login-sso-divider">
                  <Box className="nxrm-login-divider" style={{flex: 1}} />
                  <Text size="2" color="gray">{SSO_DIVIDER_LABEL}</Text>
                  <Box className="nxrm-login-divider" style={{flex: 1}} />
                </Flex>
              )}

              {/* Username */}
              <Flex direction="column" gap="2">
                <Text as="label" size="2" weight="bold" id="nxrm-login-username-label" className="nxrm-login-card__label">
                  {USERNAME_LABEL}
                </Text>
                <TextField.Root
                  placeholder={USERNAME_LABEL}
                  value={field('username').value}
                  onChange={(e) => field('username').onChange(e.target.value)}
                  onBlur={field('username').onBlur}
                  size="3"
                  required
                  autoFocus={!showSsoLogin}
                  autoComplete="username"
                  aria-labelledby="nxrm-login-username-label"
                />
              </Flex>

              {/* Password */}
              <Flex direction="column" gap="2">
                <Text as="label" size="2" weight="bold" id="nxrm-login-password-label" className="nxrm-login-card__label">
                  {PASSWORD_LABEL}
                </Text>
                <TextField.Root
                  type="password"
                  placeholder={PASSWORD_LABEL}
                  value={field('password').value}
                  onChange={(e) => field('password').onChange(e.target.value)}
                  onBlur={field('password').onBlur}
                  size="3"
                  required
                  autoComplete="current-password"
                  aria-labelledby="nxrm-login-password-label"
                />
              </Flex>

              {/* Sign In Button */}
              <Button
                type="submit"
                size="3"
                variant={showSsoLogin ? 'outline' : 'solid'}
                className="nxrm-login-submit"
                disabled={isSaving || !data.username || !data.password || rateLimitWarning}
              >
                {isSaving ? SIGNIN_BUTTON_LOADING : SIGNIN_BUTTON}
              </Button>

              {/* Continue without login */}
              {showAnonymousAccess && (
                <Flex justify="center" pt="1" className="nxrm-login-footer">
                  <Text as="p" size="2" className="nxrm-login-footer__line">
                    <span className="nxrm-login-footer__lead">{SIGNIN_FOOTER_LEAD}</span>{' '}
                    <Link
                      href="#browse/welcome"
                      className="nxrm-login-continue"
                      size="2"
                    >
                      {CONTINUE_WITHOUT_LOGIN}
                    </Link>
                  </Text>
                </Flex>
              )}
            </Flex>
          </form>
        </Card>
      </div>
    </div>
  );
}

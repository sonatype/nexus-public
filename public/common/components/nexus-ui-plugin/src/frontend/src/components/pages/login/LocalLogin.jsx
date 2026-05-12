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

import { Flex, Text, TextField, Button } from '@radix-ui/themes';
import { AlertCircle } from 'lucide-react';
import { useMachine } from '@xstate/react';
import { useRouter } from '@uirouter/react';
import PropTypes from 'prop-types';
import React, { useEffect } from 'react';
import { ExtJS } from '@sonatype/nexus-ui-plugin';
import LoginPageStrings from '../../../constants/LoginPageStrings';
import { RouteNames } from '../../../constants/RouteNames';
import FormUtils from '../../../interface/FormUtils';
import LoginFormMachine from './LocalLoginMachine';

const { LOGIN_BUTTON_LOADING, USERNAME_LABEL, PASSWORD_LABEL, LOGIN_BUTTON } = LoginPageStrings;

/**
 * Local authentication form component that handles username/password login.
 *
 * @param {Object} props - Component props
 * @param {boolean} props.primaryButton - REQUIRED: If true, login button uses primary variant and username field will be focused on mount
 * @param {Function} props.onError - Optional callback to report server errors to parent component
 */
export default function LocalLogin({ primaryButton, onError }) {
  const router = useRouter();

  const handleLoginSuccess = async () => {
    onError?.(null);
    try {
      await ExtJS.waitForNextPermissionChange();
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

  const [current, send] = useMachine(LoginFormMachine, {
    actions: {
      onSaveSuccess: async (context, event) => {
        const loginData = {
          username: event.data?.username ?? context.data.username,
          response: event.data?.response ?? event.data
        };
        await handleLoginSuccess(loginData);
      },
      logSaveError: (_, event) => {
        console.error('Login failed:', event.data);
        const error = event.data;
        const status = error.response?.status;
        if (onError && status !== 403) {
          const errorMessage = status === 0
            ? LoginPageStrings.ERRORS.CONNECTION_FAILED
            : error.response?.data?.message || LoginPageStrings.ERRORS.AUTHENTICATION_FAILED;
          onError(errorMessage);
        }
      }
    }
  });

  const formProps = FormUtils.formProps(current, send);

  const { saveError: serverError, saveErrorData, saveErrors = {}, validationErrors = {} } = current.context;
  const isLoading = current.matches('saving');
  const hasAuthError = Boolean(saveErrors.username && saveErrors.password);

  // Check for validation errors (empty fields) or server errors (wrong credentials)
  const usernameHasError = Boolean(validationErrors.username || saveErrors.username);
  const passwordHasError = Boolean(validationErrors.password || saveErrors.password);

  const shouldShowInlineError = serverError && hasAuthError;

  const handleFieldChange = (fieldName) => (value) => {
    if (serverError) {
      send({ type: 'CLEAR_SAVE_ERROR' });
    }
    FormUtils.handleUpdate(fieldName, send)(value);
  };

  const handleSubmit = () => {
    formProps.onSubmit();
  };

  useEffect(() => {
    if (!serverError) {
      onError?.(null);
    }
    if (serverError && saveErrorData?.username) {
      const inputElement = document.getElementById('username');
      if (inputElement) {
        inputElement.focus();
      }
    }
  }, [serverError, saveErrorData?.username, onError]);

  const { data } = current.context;
  const isFormValid = data.username?.trim() && data.password;

  const handleFormSubmit = (e) => {
    e.preventDefault();
    if (isFormValid && !isLoading) {
      handleSubmit();
    }
  };

  return (
    <form onSubmit={handleFormSubmit}>
      <Flex direction="column" gap="4" width="100%">
        {/* Username */}
      <Flex direction="column" gap="2">
        <Text as="label" size="2" weight="bold" htmlFor="username" className="login-form-label">
          {USERNAME_LABEL}
        </Text>
        <TextField.Root
          id="username"
          placeholder={USERNAME_LABEL}
          value={data.username || ''}
          onChange={(e) => handleFieldChange('username')(e.target.value)}
          size="3"
          required
          autoFocus={primaryButton}
          autoComplete="username"
          disabled={isLoading}
          data-analytics-id="login-username-input"
          aria-required="true"
          aria-invalid={usernameHasError || undefined}
        />
      </Flex>

      {/* Password */}
      <Flex direction="column" gap="2">
        <Text as="label" size="2" weight="bold" htmlFor="password" className="login-form-label">
          {PASSWORD_LABEL}
        </Text>
        <TextField.Root
          id="password"
          type="password"
          placeholder={PASSWORD_LABEL}
          value={data.password || ''}
          onChange={(e) => handleFieldChange('password')(e.target.value)}
          size="3"
          required
          autoComplete="current-password"
          disabled={isLoading}
          data-analytics-id="login-password-input"
          aria-required="true"
          aria-invalid={passwordHasError || undefined}
        />
      </Flex>

      {/* Server Error */}
      {shouldShowInlineError && (
        <Flex align="center" gap="2" className="server-error-message">
          <AlertCircle size={16} />
          <Text size="2" color="red">{serverError}</Text>
        </Flex>
      )}

      {/* Sign In Button */}
      <Button
        type="submit"
        size="3"
        variant={primaryButton ? 'solid' : 'outline'}
        className="login-submit"
        disabled={isLoading || !isFormValid}
        data-analytics-id="nxrm-login-local"
        data-testid="login-primary-button"
      >
        {isLoading ? LOGIN_BUTTON_LOADING : LOGIN_BUTTON}
      </Button>
      </Flex>
    </form>
  );
}

LocalLogin.propTypes = {
  primaryButton: PropTypes.bool.isRequired,
  onError: PropTypes.func
};

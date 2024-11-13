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
import {
  render,
  screen,
  waitForElementToBeRemoved,
  within,
} from '@testing-library/react';
import {when} from 'jest-when';
import Axios from 'axios';
import Http from './Http';
import {APIConstants, ExtAPIUtils, ExtJS} from '@sonatype/nexus-ui-plugin';
import TestUtils from '@sonatype/nexus-ui-plugin/src/frontend/src/interface/TestUtils';
import userEvent from '@testing-library/user-event';
import UIStrings from '../../../../constants/UIStrings';

jest.mock('axios', () => ({
  ...jest.requireActual('axios'),
  get: jest.fn(),
  post: jest.fn(),
}));

jest.mock('@sonatype/nexus-ui-plugin', () => ({
  ...jest.requireActual('@sonatype/nexus-ui-plugin'),
  ExtJS: {
    requestConfirmation: jest.fn(),
    checkPermission: jest.fn(),
    showErrorMessage: jest.fn(),
    showSuccessMessage: jest.fn(),
    state: jest.fn().mockReturnValue({
      getValue: jest.fn(),
    }),
  },
}));

const {
  EXT: {
    URL,
    HTTP: {ACTION, METHODS},
  },
} = APIConstants;

const {
  HTTP: {CONFIGURATION: LABELS},
  ERROR,
} = UIStrings;

const UPDATE = ExtAPIUtils.createRequestBody(ACTION, METHODS.UPDATE);

const selectors = {
  ...TestUtils.selectors,
  ...TestUtils.formSelectors,
  userAgent: () => screen.getByText(LABELS.USER_AGENT.LABEL),
  userAgentInput: () => screen.getByLabelText(LABELS.USER_AGENT.LABEL),
  timeout: () => screen.getByText(LABELS.TIMEOUT.LABEL),
  timeoutInput: () => screen.getByLabelText(LABELS.TIMEOUT.LABEL),
  retries: () => screen.getByText(LABELS.ATTEMPTS.LABEL),
  retriesInput: () => screen.getByLabelText(LABELS.ATTEMPTS.LABEL),
  proxy: {
    setting: () => screen.getByText(LABELS.PROXY.LABEL),
    httpHost: () => screen.getByLabelText(LABELS.PROXY.HTTP_HOST),
    queryHttpHost: () => screen.queryByLabelText(LABELS.PROXY.HTTP_HOST),
    httpPort: () => screen.getByLabelText(LABELS.PROXY.HTTP_PORT),
    queryHttpPort: () => screen.queryByLabelText(LABELS.PROXY.HTTP_PORT),
    httpCheckbox: () => screen.getByLabelText(LABELS.PROXY.HTTP_CHECKBOX),
    httpAuthCheckbox: () => screen.getByLabelText(LABELS.PROXY.HTTP_AUTH_CHECKBOX),
    httpsHost: () => screen.getByLabelText(LABELS.PROXY.HTTPS_HOST),
    queryHttpsHost: () => screen.queryByLabelText(LABELS.PROXY.HTTPS_HOST),
    httpsPort: () => screen.getByLabelText(LABELS.PROXY.HTTPS_PORT),
    queryHttpsPort: () => screen.queryByLabelText(LABELS.PROXY.HTTPS_PORT),
    httpsCheckbox: () => screen.getByLabelText(LABELS.PROXY.HTTPS_CHECKBOX),
    httpsAuthCheckbox: () => screen.getByLabelText(LABELS.PROXY.HTTPS_AUTH_CHECKBOX),
    username: (container) =>
      within(container).getByLabelText(LABELS.PROXY.USERNAME),
    password: (container) =>
      within(container).getByLabelText(LABELS.PROXY.PASSWORD),
    ntlmHost: (container) =>
      within(container).getByLabelText(LABELS.PROXY.HOST_NAME),
    ntlmDomain: (container) =>
      within(container).getByLabelText(LABELS.PROXY.DOMAIN),
    httpAccordion: () =>
      selectors.proxy.httpAccordionButton().closest('.nx-accordion'),
    queryHttpAccordionTitle: () =>
      screen.queryByText(LABELS.PROXY.HTTP_AUTHENTICATION),
    httpsAccordion: () =>
      selectors.proxy.httpsAccordionButton().closest('.nx-accordion'),
    queryHttpsAccordionTitle: () =>
      screen.queryByText(LABELS.PROXY.HTTPS_AUTHENTICATION),
    httpAccordionButton: () =>
      screen.getByText(LABELS.PROXY.HTTP_AUTHENTICATION),
    httpsAccordionButton: () =>
      screen.getByText(LABELS.PROXY.HTTPS_AUTHENTICATION),
    exclude: () => screen.getByLabelText(LABELS.EXCLUDE.LABEL),
    queryExcludeTitle: () => screen.queryByText(LABELS.EXCLUDE.LABEL),
    addButton: (container) =>
      container.querySelector('[data-icon="plus-circle"]'),
    removeButton: (container) =>
      container.querySelector('[data-icon="trash-alt"]'),
  },
};

describe('Http', () => {
  const mock = {
    userAgentSuffix: null,
    timeout: null,
    retries: null,
    httpEnabled: null,
    httpHost: null,
    httpPort: null,
    httpAuthEnabled: null,
    httpAuthUsername: null,
    httpAuthPassword: null,
    httpAuthNtlmHost: null,
    httpAuthNtlmDomain: null,
    httpsEnabled: null,
    httpsHost: null,
    httpsPort: null,
    httpsAuthEnabled: null,
    httpsAuthUsername: null,
    httpsAuthPassword: null,
    httpsAuthNtlmHost: null,
    httpsAuthNtlmDomain: null,
    nonProxyHosts: null,
  };

  const dummyData = {
    userAgentSuffix: 'agent',
    timeout: '1000',
    retries: '0',
    httpAuthEnabled: false,
    httpEnabled: false,
    httpsAuthEnabled: false,
    httpsEnabled: false,
    nonProxyHosts: []
  };

  const dummyHttpProxy = {
    httpPort: '123',
    httpHost: 'http.host',
    httpEnabled: true,
  };

  const dummyHttpProxyWithAuth = {
    ...dummyHttpProxy,
    httpAuthEnabled: true,
    httpAuthUsername: 'http.username',
    httpAuthPassword: 'password',
    httpAuthNtlmHost: 'http.ntlm.host',
    httpAuthNtlmDomain: 'http.ntlm.domain',
  };

  const dummyHttpsProxy = {
    httpsPort: '456',
    httpsHost: 'https.host',
    httpsEnabled: true,
  };

  const dummyHttpsProxyWithAuth = {
    ...dummyHttpsProxy,
    httpsAuthEnabled: true,
    httpsAuthUsername: 'https.username',
    httpsAuthPassword: 'password',
    httpsAuthNtlmHost: 'https.ntlm.host',
    httpsAuthNtlmDomain: 'https.ntlm.domain',
  };

  const nonProxy = 'http.nonProxyHosts';

  const mockResponse = (data = mock) => {
    Axios.post = jest
      .fn()
      .mockResolvedValueOnce({data: TestUtils.makeExtResult(data)});
  };

  const renderAndWaitForLoad = async () => {
    const result = render(<Http />);
    await waitForElementToBeRemoved(selectors.queryLoadingMask());
    return result;
  };

  beforeEach(() => {
    when(ExtJS.checkPermission)
      .calledWith('nexus:settings:update')
      .mockReturnValue(true);

    when(ExtJS.state().getValue)
      .calledWith('requestTimeout')
      .mockReturnValue({value: 20});

    when(ExtJS.state().getValue)
      .calledWith('retryCount')
      .mockReturnValue(2);

    mockResponse();

    when(Axios.post)
      .calledWith(URL, UPDATE)
      .mockResolvedValue({
        data: TestUtils.makeExtResult({}),
      });
  });

  it('renders correctly', async () => {
    const {retries, userAgent, timeout} = selectors;
    await renderAndWaitForLoad();

    expect(userAgent()).toBeInTheDocument();
    expect(timeout()).toBeInTheDocument();
    expect(retries()).toBeInTheDocument();
  });

  it('has the correct placeholders for timeout and retries', async () => {
    const {timeoutInput, retriesInput} = selectors;
    await renderAndWaitForLoad();

    expect(timeoutInput().getAttribute('placeholder')).toBe('20');
    expect(retriesInput().getAttribute('placeholder')).toBe('2');
  });

  it('connection timeout should be greater than 0, less than or equal to 3600', async () => {
    const {timeoutInput} = selectors;

    await renderAndWaitForLoad();

    userEvent.click(selectors.querySubmitButton());
    expect(selectors.queryFormError(TestUtils.NO_CHANGES_MESSAGE)).toBeInTheDocument();

    await TestUtils.changeField(timeoutInput, 'text');
    expect(screen.getByText(ERROR.NAN)).toBeInTheDocument();

    await TestUtils.changeField(timeoutInput, '0');
    expect(selectors.queryFormError(TestUtils.VALIDATION_ERRORS_MESSAGE)).toBeInTheDocument();

    await TestUtils.changeField(timeoutInput, '3601');
    expect(selectors.queryFormError(TestUtils.VALIDATION_ERRORS_MESSAGE)).toBeInTheDocument();

    await TestUtils.changeField(timeoutInput, '1');
    expect(timeoutInput()).toHaveValue('1');
    expect(selectors.queryFormError()).not.toBeInTheDocument();
  });

  it('connection attempts should be greater or equal to 0, less than or equal to 10', async () => {
    const {retriesInput} = selectors;

    await renderAndWaitForLoad();

    userEvent.click(selectors.querySubmitButton());
    expect(selectors.queryFormError(TestUtils.NO_CHANGES_MESSAGE)).toBeInTheDocument();

    await TestUtils.changeField(retriesInput, 'text');
    expect(screen.getByText(ERROR.NAN)).toBeInTheDocument();

    await TestUtils.changeField(retriesInput, '11');
    expect(selectors.queryFormError(TestUtils.VALIDATION_ERRORS_MESSAGE)).toBeInTheDocument();

    await TestUtils.changeField(retriesInput, '1');
    expect(retriesInput()).toHaveValue('1');
    expect(selectors.queryFormError()).not.toBeInTheDocument();
  });

  describe('Proxy', () => {
    it('renders correctly', async () => {
      const {
        setting,
        httpHost,
        httpPort,
        httpCheckbox,
        httpsHost,
        httpsPort,
        httpsCheckbox,
      } = selectors.proxy;

      await renderAndWaitForLoad();

      expect(setting()).toBeInTheDocument();
      expect(httpHost()).toBeInTheDocument();
      expect(httpPort()).toBeInTheDocument();
      expect(httpCheckbox()).toBeInTheDocument();
      expect(httpsHost()).toBeInTheDocument();
      expect(httpsPort()).toBeInTheDocument();
      expect(httpsCheckbox()).toBeInTheDocument();
    });

    it('requires http checkbox marked as checked to be able to fill the http host and port', async () => {
      const {
        httpHost,
        httpPort,
        httpCheckbox
      } = selectors.proxy;

      await renderAndWaitForLoad();

      expect(httpCheckbox()).not.toBeChecked();
      expect(httpHost()).toBeDisabled();
      expect(httpPort()).toBeDisabled();

      userEvent.click(httpCheckbox());

      expect(httpHost()).not.toBeDisabled();

      expect(httpPort()).toBeDisabled();

      await TestUtils.changeField(httpHost, 'test');

      expect(httpPort()).not.toBeDisabled();
    });

    it('requires http authentication accordion to be open to be able to fill the username, password, hostname and domain', async () => {
      const {
        username,
        password,
        ntlmHost,
        ntlmDomain,
        httpAccordion,
        httpAccordionButton,
        httpCheckbox,
      } = selectors.proxy;

      await renderAndWaitForLoad();
      const accordion = httpAccordion();

      expect(username(accordion)).toBeDisabled();
      expect(password(accordion)).toBeDisabled();
      expect(ntlmHost(accordion)).toBeDisabled();
      expect(ntlmDomain(accordion)).toBeDisabled();

      userEvent.click(httpCheckbox());

      expect(httpCheckbox()).toBeChecked();
      expect(accordion).toHaveAttribute('aria-expanded', 'false');
      expect(username(accordion)).not.toBeVisible();
      expect(password(accordion)).not.toBeVisible();
      expect(ntlmHost(accordion)).not.toBeVisible();
      expect(ntlmDomain(accordion)).not.toBeVisible();

      userEvent.click(httpAccordionButton());

      expect(accordion).toHaveAttribute('aria-expanded', 'true');
      expect(username(accordion)).toBeVisible();
      expect(password(accordion)).toBeVisible();
      expect(ntlmHost(accordion)).toBeVisible();
      expect(ntlmDomain(accordion)).toBeVisible();
    });

    it('requires https checkbox marked as checked to be able to fill the https host, port and open the https authentication accordion', async () => {
      const {
        httpsHost,
        httpsPort,
        httpCheckbox,
        httpsCheckbox
      } = selectors.proxy;

      await renderAndWaitForLoad();

      userEvent.click(httpCheckbox());

      expect(httpCheckbox()).toBeChecked();
      expect(httpsCheckbox()).not.toBeChecked();
      expect(httpsHost()).toBeDisabled();
      expect(httpsPort()).toBeDisabled();

      userEvent.click(httpsCheckbox());

      expect(httpsHost()).not.toBeDisabled();
      expect(httpsPort()).toBeDisabled();

      await TestUtils.changeField(httpsHost, 'test');

      expect(httpsPort()).not.toBeDisabled();
    });

    it('adds or removes items to the excludes from HTTP/HTTPS Proxy list', async () => {
      const {exclude, addButton, removeButton, httpCheckbox} = selectors.proxy;

      const {container} = await renderAndWaitForLoad();

      userEvent.click(httpCheckbox());

      await TestUtils.changeField(exclude, nonProxy);

      expect(addButton(container)).toBeInTheDocument();

      userEvent.click(addButton(container));

      expect(screen.getByText(nonProxy)).toBeInTheDocument();
      expect(removeButton(container)).toBeInTheDocument();

      userEvent.click(removeButton(container));
      expect(screen.queryByText(nonProxy)).not.toBeInTheDocument();
    });

    it('show error message if the non proxy host value is not valid', async () => {
      const {exclude, httpCheckbox} = selectors.proxy;

      await renderAndWaitForLoad();

      userEvent.click(httpCheckbox());

      await TestUtils.changeField(exclude, ` ${nonProxy} `);

      expect(screen.getByText(ERROR.WHITE_SPACE_ERROR)).toBeInTheDocument();
    });
  });

  describe('Save change', () => {
    async function submitAndExpect(data) {
      mockResponse();

      userEvent.click(selectors.querySubmitButton());
      await waitForElementToBeRemoved(selectors.querySavingMask());

      expect(Axios.post).toHaveBeenCalledWith(
          URL,
          ExtAPIUtils.createRequestBody(ACTION, METHODS.UPDATE, {data})
      );
      expect(NX.Messages.success).toHaveBeenCalledWith(UIStrings.SAVE_SUCCESS);
    }

    it('Base form only', async () => {
      const {retriesInput, userAgentInput, timeoutInput} =
        selectors;

      await renderAndWaitForLoad();

      await TestUtils.changeField(userAgentInput, dummyData.userAgentSuffix);
      await TestUtils.changeField(retriesInput, dummyData.retries);
      await TestUtils.changeField(timeoutInput, dummyData.timeout);

      await submitAndExpect([dummyData]);
    });

    it('Http Proxy requires host and port', async () => {
      const data = {
        ...dummyData,
        ...dummyHttpProxy,
      };
      const {
        userAgentInput,
        retriesInput,
        timeoutInput,
        proxy: {httpHost, httpPort, httpCheckbox},
      } = selectors;

      await renderAndWaitForLoad();

      await TestUtils.changeField(userAgentInput, data.userAgentSuffix);
      await TestUtils.changeField(retriesInput, data.retries);
      await TestUtils.changeField(timeoutInput, data.timeout);

      userEvent.click(httpCheckbox());

      await TestUtils.changeField(httpHost, data.httpHost);

      userEvent.click(selectors.querySubmitButton());
      expect(selectors.queryFormError(TestUtils.VALIDATION_ERRORS_MESSAGE)).toBeInTheDocument();

      await TestUtils.changeField(httpHost, '');
      await TestUtils.changeField(httpPort, data.httpPort);

      expect(selectors.queryFormError(TestUtils.VALIDATION_ERRORS_MESSAGE)).toBeInTheDocument();

      await TestUtils.changeField(httpHost, data.httpHost);

      expect(selectors.queryFormError()).not.toBeInTheDocument();

      await submitAndExpect([data]);
    });

    it('Http Proxy with Authentication requires username', async () => {
      const data = {
        ...dummyData,
        ...dummyHttpProxyWithAuth,
      };
      const {
        userAgentInput,
        retriesInput,
        timeoutInput,
        proxy: {
          httpHost,
          httpPort,
          httpCheckbox,
          httpAccordionButton,
          httpAccordion,
          username,
          password,
          ntlmHost,
          ntlmDomain,
          httpAuthCheckbox
        },
      } = selectors;

      await renderAndWaitForLoad();
      const accordion = httpAccordion();

      await TestUtils.changeField(userAgentInput, data.userAgentSuffix);
      await TestUtils.changeField(retriesInput, data.retries);
      await TestUtils.changeField(timeoutInput, data.timeout);

      userEvent.click(httpCheckbox());

      await TestUtils.changeField(httpHost, data.httpHost);
      await TestUtils.changeField(httpPort, data.httpPort);

      userEvent.click(httpAccordionButton());

      userEvent.click(httpAuthCheckbox());

      await TestUtils.changeField(
        () => password(accordion),
        data.httpAuthPassword
      );
      await TestUtils.changeField(
        () => ntlmHost(accordion),
        data.httpAuthNtlmHost
      );
      await TestUtils.changeField(
        () => ntlmDomain(accordion),
        data.httpAuthNtlmDomain
      );

      userEvent.click(selectors.querySubmitButton());
      expect(selectors.queryFormError(TestUtils.VALIDATION_ERRORS_MESSAGE)).toBeInTheDocument();

      await TestUtils.changeField(
        () => username(accordion),
        data.httpAuthUsername
      );

      expect(selectors.queryFormError()).not.toBeInTheDocument();

      await submitAndExpect([data]);
    });

    it('Https Proxy requires host and port', async () => {
      const data = {
        ...dummyData,
        ...dummyHttpProxy,
        ...dummyHttpsProxy,
      };
      const {
        userAgentInput,
        retriesInput,
        timeoutInput,
        proxy: {
          httpHost,
          httpPort,
          httpCheckbox,
          httpsCheckbox,
          httpsHost,
          httpsPort,
        },
      } = selectors;

      await renderAndWaitForLoad();

      await TestUtils.changeField(userAgentInput, data.userAgentSuffix);
      await TestUtils.changeField(retriesInput, data.retries);
      await TestUtils.changeField(timeoutInput, data.timeout);

      userEvent.click(httpCheckbox());

      await TestUtils.changeField(httpHost, data.httpHost);
      await TestUtils.changeField(httpPort, data.httpPort);

      userEvent.click(httpsCheckbox());

      userEvent.click(selectors.querySubmitButton());
      expect(selectors.queryFormError(TestUtils.VALIDATION_ERRORS_MESSAGE)).toBeInTheDocument();

      await TestUtils.changeField(httpsHost, data.httpsHost);
      await TestUtils.changeField(httpsPort, data.httpsPort);

      expect(selectors.queryFormError()).not.toBeInTheDocument();

      await submitAndExpect([data]);
    });

    it('Https Proxy with Authentication requires username', async () => {
      const data = {
        ...dummyData,
        ...dummyHttpsProxyWithAuth,
      };
      const {
        userAgentInput,
        retriesInput,
        timeoutInput,
        proxy: {
          httpsCheckbox,
          httpsHost,
          httpsPort,
          httpsAccordionButton,
          httpsAccordion,
          password,
          ntlmHost,
          ntlmDomain,
          username,
          httpsAuthCheckbox
        },
      } = selectors;

      await renderAndWaitForLoad();
      const accordion = httpsAccordion();

      await TestUtils.changeField(userAgentInput, data.userAgentSuffix);
      await TestUtils.changeField(retriesInput, data.retries);
      await TestUtils.changeField(timeoutInput, data.timeout);

      userEvent.click(httpsCheckbox());

      await TestUtils.changeField(httpsHost, data.httpsHost);
      await TestUtils.changeField(httpsPort, data.httpsPort);

      userEvent.click(httpsAccordionButton());

      userEvent.click(httpsAuthCheckbox());

      await TestUtils.changeField(
        () => password(accordion),
        data.httpsAuthPassword
      );
      await TestUtils.changeField(
        () => ntlmHost(accordion),
        data.httpsAuthNtlmHost
      );
      await TestUtils.changeField(
        () => ntlmDomain(accordion),
        data.httpsAuthNtlmDomain
      );

      userEvent.click(selectors.querySubmitButton());
      expect(selectors.queryFormError(TestUtils.VALIDATION_ERRORS_MESSAGE)).toBeInTheDocument();

      await TestUtils.changeField(
        () => username(accordion),
        data.httpsAuthUsername
      );

      expect(selectors.queryFormError()).not.toBeInTheDocument();

      await submitAndExpect([data]);
    });

    it('Exclude nonProxy host', async () => {
      const data = {
        httpAuthEnabled: false,
        httpEnabled: false,
        httpsAuthEnabled: false,
        httpsEnabled: false,
        nonProxyHosts: [nonProxy],
        ...dummyHttpProxy,
        retries: null,
        timeout: null
      };
      const {
        proxy: {exclude, addButton, httpCheckbox, httpHost, httpPort}
      } = selectors;

      const {container} = await renderAndWaitForLoad();

      userEvent.click(httpCheckbox());

      await TestUtils.changeField(httpHost, data.httpHost);
      await TestUtils.changeField(httpPort, data.httpPort);

      await TestUtils.changeField(exclude, nonProxy);

      userEvent.click(addButton(container));

      await submitAndExpect([data]);
    });
  });

  describe('Delete changes', () => {
    it('Http Proxy removes authentication if the authentication section closes but does not remove the values until the form is saved', async () => {
      const data = {
        ...dummyData,
        ...dummyHttpProxy,
        httpAuthEnabled: true,
        httpAuthUsername: 'username',
        httpAuthPassword: 'password',
        httpAuthNtlmHost: 'ntlm.host',
        httpAuthNtlmDomain: 'ntlm.domain',
      };
      const {
        userAgentInput,
        retriesInput,
        timeoutInput,
        proxy: {
          httpHost,
          httpPort,
          httpCheckbox,
          httpAccordionButton,
          httpAccordion,
          username,
          password,
          ntlmHost,
          ntlmDomain,
          httpAuthCheckbox
        },
        querySubmitButton,
        querySavingMask
      } = selectors;

      await renderAndWaitForLoad();
      const accordion = httpAccordion();

      await TestUtils.changeField(userAgentInput, data.userAgentSuffix);
      await TestUtils.changeField(retriesInput, data.retries);
      await TestUtils.changeField(timeoutInput, data.timeout);

      userEvent.click(httpCheckbox());

      await TestUtils.changeField(httpHost, data.httpHost);
      await TestUtils.changeField(httpPort, data.httpPort);

      userEvent.click(httpAccordionButton());

      userEvent.click(httpAuthCheckbox());

      await TestUtils.changeField(
        () => password(accordion),
        data.httpAuthPassword
      );
      await TestUtils.changeField(
        () => ntlmHost(accordion),
        data.httpAuthNtlmHost
      );
      await TestUtils.changeField(
        () => ntlmDomain(accordion),
        data.httpAuthNtlmDomain
      );

      await TestUtils.changeField(
        () => username(accordion),
        data.httpAuthUsername
      );

      mockResponse(data);

      when(Axios.post)
        .calledWith(URL, ExtAPIUtils.createRequestBody(ACTION, METHODS.READ, null))
        .mockResolvedValueOnce({data: TestUtils.makeExtResult(data)});

      userEvent.click(querySubmitButton());
      await waitForElementToBeRemoved(querySavingMask());

      const expected = {
        data: [data],
      };

      expect(Axios.post).toHaveBeenCalledWith(
        URL,
        ExtAPIUtils.createRequestBody(ACTION, METHODS.UPDATE, expected)
      );
      expect(NX.Messages.success).toHaveBeenCalledWith(UIStrings.SAVE_SUCCESS);

      userEvent.click(httpAuthCheckbox());

      mockResponse();

      userEvent.click(querySubmitButton());
      await waitForElementToBeRemoved(querySavingMask());

      const newResponse = {
        ...dummyData,
        ...dummyHttpProxy,
      };

      expect(Axios.post).toHaveBeenCalledWith(
        URL,
        ExtAPIUtils.createRequestBody(ACTION, METHODS.UPDATE, {
          data: [newResponse],
        })
      );
      expect(NX.Messages.success).toHaveBeenCalledWith(UIStrings.SAVE_SUCCESS);
    });

    it('Https Proxy removes authentication if the authentication section closes but does not remove the values until the form is saved', async () => {
      const data = {
        ...dummyData,
        ...dummyHttpProxy,
        ...dummyHttpsProxy,
        httpsAuthEnabled: true,
        httpsAuthUsername: 'username',
        httpsAuthPassword: 'password',
        httpsAuthNtlmHost: 'ntlm.host',
        httpsAuthNtlmDomain: 'ntlm.domain',
      };
      const {
        userAgentInput,
        retriesInput,
        timeoutInput,
        proxy: {
          httpHost,
          httpPort,
          httpsHost,
          httpsPort,
          httpCheckbox,
          httpsCheckbox,
          httpsAccordionButton,
          httpsAccordion,
          username,
          password,
          ntlmHost,
          ntlmDomain,
          httpsAuthCheckbox
        },
      } = selectors;

      await renderAndWaitForLoad();
      const accordion = httpsAccordion();

      await TestUtils.changeField(userAgentInput, data.userAgentSuffix);
      await TestUtils.changeField(retriesInput, data.retries);
      await TestUtils.changeField(timeoutInput, data.timeout);

      userEvent.click(httpCheckbox());

      await TestUtils.changeField(httpHost, data.httpHost);
      await TestUtils.changeField(httpPort, data.httpPort);

      userEvent.click(httpsCheckbox());

      await TestUtils.changeField(httpsHost, data.httpsHost);
      await TestUtils.changeField(httpsPort, data.httpsPort);

      userEvent.click(httpsAccordionButton());

      userEvent.click(httpsAuthCheckbox());

      await TestUtils.changeField(
        () => password(accordion),
        data.httpsAuthPassword
      );
      await TestUtils.changeField(
        () => ntlmHost(accordion),
        data.httpsAuthNtlmHost
      );
      await TestUtils.changeField(
        () => ntlmDomain(accordion),
        data.httpsAuthNtlmDomain
      );

      await TestUtils.changeField(
        () => username(accordion),
        data.httpsAuthUsername
      );

      mockResponse(data);

      when(Axios.post)
        .calledWith(URL, ExtAPIUtils.createRequestBody(ACTION, METHODS.READ, null))
        .mockResolvedValueOnce({data: TestUtils.makeExtResult(data)});

      userEvent.click(selectors.querySubmitButton());
      await waitForElementToBeRemoved(selectors.querySavingMask());

      const expected = {
        data: [data],
      };

      expect(Axios.post).toHaveBeenCalledWith(
        URL,
        ExtAPIUtils.createRequestBody(ACTION, METHODS.UPDATE, expected)
      );
      expect(NX.Messages.success).toHaveBeenCalledWith(UIStrings.SAVE_SUCCESS);

      userEvent.click(httpsAuthCheckbox());

      mockResponse();

      userEvent.click(selectors.querySubmitButton());
      await waitForElementToBeRemoved(selectors.querySavingMask());

      const newResponse = {
        ...dummyData,
        ...dummyHttpProxy,
        ...dummyHttpsProxy,
      };

      expect(Axios.post).toHaveBeenCalledWith(
        URL,
        ExtAPIUtils.createRequestBody(ACTION, METHODS.UPDATE, {
          data: [newResponse],
        })
      );
      expect(NX.Messages.success).toHaveBeenCalledWith(UIStrings.SAVE_SUCCESS);
    });
  });

  describe('Read only', () => {
    beforeEach(() => {
      when(ExtJS.checkPermission)
        .calledWith('nexus:settings:update')
        .mockReturnValue(false);

      mockResponse();
    });

    it('Basic form', async () => {
      const response = {
        ...mock,
        ...dummyData,
        ...dummyHttpProxyWithAuth,
        ...dummyHttpsProxyWithAuth,
        nonProxyHosts: [nonProxy],
      };

      mockResponse(response);

      const {retries, userAgent, timeout} = selectors;
      await renderAndWaitForLoad();

      expect(userAgent()).toBeInTheDocument();
      expect(timeout()).toBeInTheDocument();
      expect(retries()).toBeInTheDocument();
      expect(screen.getByText(response.userAgentSuffix)).toBeInTheDocument();
      expect(screen.getByText(response.retries)).toBeInTheDocument();
      expect(screen.getByText(response.timeout)).toBeInTheDocument();
      expect(screen.getByText(response.httpHost)).toBeInTheDocument();
      expect(screen.getByText(response.httpPort)).toBeInTheDocument();
      expect(screen.getByText(response.httpAuthUsername)).toBeInTheDocument();
      expect(screen.getByText(response.httpAuthNtlmDomain)).toBeInTheDocument();
      expect(screen.getByText(response.httpAuthNtlmHost)).toBeInTheDocument();
      expect(screen.getByText(response.httpsHost)).toBeInTheDocument();
      expect(screen.getByText(response.httpsPort)).toBeInTheDocument();
      expect(screen.getByText(response.httpsAuthUsername)).toBeInTheDocument();
      expect(
        screen.getByText(response.httpsAuthNtlmDomain)
      ).toBeInTheDocument();
      expect(screen.getByText(response.httpsAuthNtlmHost)).toBeInTheDocument();
      expect(screen.getByText(nonProxy)).toBeInTheDocument();
    });

    it('does not show HTTP Proxy setting if it is not enabled', async () => {
      const {queryHttpPort, queryHttpHost, queryHttpAccordionTitle} =
        selectors.proxy;

      await renderAndWaitForLoad();

      expect(queryHttpPort()).not.toBeInTheDocument();
      expect(queryHttpHost()).not.toBeInTheDocument();
      expect(queryHttpAccordionTitle()).not.toBeInTheDocument();
    });

    it('does not show HTTPS Proxy setting if it is not enabled', async () => {
      const {queryHttpsPort, queryHttpsHost, queryHttpsAccordionTitle} =
        selectors.proxy;

      await renderAndWaitForLoad();

      expect(queryHttpsPort()).not.toBeInTheDocument();
      expect(queryHttpsHost()).not.toBeInTheDocument();
      expect(queryHttpsAccordionTitle()).not.toBeInTheDocument();
    });

    it('does not show exclude list if it is empty', async () => {
      const {queryExcludeTitle} = selectors.proxy;

      await renderAndWaitForLoad();
      expect(queryExcludeTitle()).not.toBeInTheDocument();
    });
  });
});

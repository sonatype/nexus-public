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
import axios from 'axios';
import { render, screen, waitFor, waitForElementToBeRemoved } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { ExtJS, ExtAPIUtils } from '@sonatype/nexus-ui-plugin';

import Welcome from './Welcome.jsx';

const simpleSuccessResponse = {
  data: [{
    action: 'outreach_Outreach',
    method: 'isAvailableLog4jDisclaimer',
    result: { success: true }
  }, {
    action: 'outreach_Outreach',
    method: 'readStatus',
    result: { success: true }
  }, {
    action: 'outreach_Outreach',
    method: 'getProxyDownloadNumbers',
    result: { success: true }
  }]
};

// Creates a selector function that uses getByRole by default but which can be customized per-use to use
// queryByRole, findByRole, etc instead
const selectorQuery = (...queryParams) => queryType => screen[`${queryType ?? 'get'}ByRole`].apply(screen, queryParams);

const selectors = {
  main: () => screen.getByRole('main'),
  loadingStatus: () => screen.getByRole('status'),
  errorAlert: selectorQuery('alert'),
  errorRetryBtn: selectorQuery('button', { name: 'Retry' }),
  log4jCapabiltyNotice: selectorQuery('region', { name: 'Log4j Capability Notice' }),
  enableCapabilityBtn: selectorQuery('button', { name: 'Enable Capability' }),
  outreachFrame: selectorQuery('document', { name: 'Outreach Frame' })
};

describe('Welcome', function() {
  let user;

  beforeEach(function() {
    user = null;

    jest.spyOn(axios, 'post').mockResolvedValue(simpleSuccessResponse)
    jest.spyOn(ExtJS, 'useStatus').mockReturnValue({});
    jest.spyOn(ExtJS, 'useLicense').mockReturnValue({});
    jest.spyOn(ExtJS, 'useUser').mockImplementation(() => user);
    jest.spyOn(ExtJS, 'state').mockReturnValue({ getUser: () => user });
  });

  it('renders a main content area', function() {
    // resolving the promise in this otherwise-synchronous test causes act errors, so just leave it unresolved here
    jest.spyOn(axios, 'post').mockReturnValue(new Promise(() => {}));
    render(<Welcome />);

    expect(selectors.main()).toBeInTheDocument();
  });

  it('renders headings saying "Welcome" and "Learn about Sonatype Nexus Repository Manager"', async function() {
    jest.spyOn(axios, 'post').mockReturnValue(new Promise(() => {}));
    render(<Welcome />);

    expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent('Welcome');
    expect(screen.getByRole('heading', { level: 2 }))
        .toHaveTextContent('Learn about Sonatype Nexus Repository Manager');
  });

  // Since the logo is next to the name of the product, it is supplementary from an a11y standpoint.
  // See the spec referenced in the impl
  it('renders a logo image that does NOT have the img role or an accessible name', async function() {
    jest.spyOn(axios, 'post').mockReturnValue(new Promise(() => {}));
    const { container } = render(<Welcome />),
        img = container.querySelector('img');

    expect(img).toBeInTheDocument();
    expect(img).not.toHaveAccessibleName();
    expect(screen.queryByRole('img')).not.toBeInTheDocument();
  });

  describe('loading', function() {
    it('calls necessary outreach backend calls after rendering', async function() {
      jest.spyOn(axios, 'post').mockReturnValue(new Promise(() => {}));
      render(<Welcome />);

      expect(axios.post).toHaveBeenCalledWith('/service/extdirect', [
        expect.objectContaining({ action: 'outreach_Outreach', method: 'readStatus' }),
        expect.objectContaining({ action: 'outreach_Outreach', method: 'getProxyDownloadNumbers' })
      ]);
    });

    it('renders a loading spinner until the outreach backend calls complete', async function() {
      render(<Welcome />);

      const status = selectors.loadingStatus();
      expect(status).toBeInTheDocument();
      expect(status).toHaveTextContent('Loading');

      await waitForElementToBeRemoved(status);
    });

    describe('when the user is an admin', function() {
      beforeEach(function() {
        user = { administrator: true };
      });

      it('calls the isAvailableLog4jDisclaimer outreach call', async function() {
        jest.spyOn(axios, 'post').mockReturnValue(new Promise(() => {}));
        render(<Welcome />);

        expect(axios.post).toHaveBeenCalledWith('/service/extdirect', [
          expect.objectContaining({ action: 'outreach_Outreach', method: 'readStatus' }),
          expect.objectContaining({ action: 'outreach_Outreach', method: 'getProxyDownloadNumbers' }),
          expect.objectContaining({ action: 'outreach_Outreach', method: 'isAvailableLog4jDisclaimer' })
        ]);
      });

      it('renders a loading spinner until the outreach backend calls complete incl the log4j call', async function() {
        render(<Welcome />);

        const status = selectors.loadingStatus();
        expect(status).toBeInTheDocument();
        expect(status).toHaveTextContent('Loading');

        await waitForElementToBeRemoved(status);
      });
    });

    describe('when the user is not an admin', function() {
      beforeEach(function() {
        user = { administrator: false };
      });

      it('does not call the isAvailableLog4jDisclaimer outreach call', async function() {
        jest.spyOn(axios, 'post').mockReturnValue(new Promise(() => {}));
        render(<Welcome />);

        expect(axios.post).toHaveBeenCalledWith('/service/extdirect', [
          expect.objectContaining({ action: 'outreach_Outreach', method: 'readStatus' }),
          expect.objectContaining({ action: 'outreach_Outreach', method: 'getProxyDownloadNumbers' })
        ]);
      });
    });

    describe('when the user is not logged in', function() {
      it('does not call the isAvailableLog4jDisclaimer outreach call', async function() {
        jest.spyOn(axios, 'post').mockReturnValue(new Promise(() => {}));
        render(<Welcome />);

        expect(axios.post).toHaveBeenCalledWith('/service/extdirect', [
          expect.objectContaining({ action: 'outreach_Outreach', method: 'readStatus' }),
          expect.objectContaining({ action: 'outreach_Outreach', method: 'getProxyDownloadNumbers' })
        ]);
      });
    });
  });

  describe('error handling', function() {
    beforeEach(function() {
      user = { administrator: true };
      jest.spyOn(axios, 'post').mockRejectedValue({ message: 'foobar' });
    });

    it('renders an error alert when the extdirect call fails', async function() {
      render(<Welcome />);

      const error = await selectors.errorAlert('find');
      expect(error).toHaveTextContent(/error/i);
      expect(error).toHaveTextContent('foobar');
    });

    it('renders a Retry button within the error alert', async function() {
      render(<Welcome />);

      const error = await selectors.errorAlert('find'),
          retryBtn = selectors.errorRetryBtn();

      expect(error).toContainElement(retryBtn);
    });

    it('re-executes the backend call when Retry is clicked', async function() {
      render(<Welcome />);

      expect(axios.post).toHaveBeenCalledTimes(1);

      const retryBtn = await selectors.errorRetryBtn('find');
      await userEvent.click(retryBtn);

      await waitFor(() => expect(axios.post).toHaveBeenCalledTimes(2));
      expect(axios.post).toHaveBeenLastCalledWith('/service/extdirect', [
        expect.objectContaining({ action: 'outreach_Outreach', method: 'readStatus' }),
        expect.objectContaining({ action: 'outreach_Outreach', method: 'getProxyDownloadNumbers' }),
        expect.objectContaining({ action: 'outreach_Outreach', method: 'isAvailableLog4jDisclaimer' })
      ]);
    });
  });

  describe('log4j alert', function() {
    beforeEach(function() {
      user = { administrator: true };
    });

    function mockLog4jResponse(returnValue) {
      // NOTE: response array order does not necessarily match request array order
      jest.spyOn(axios, 'post').mockReturnValue({
        data: [{
          action: 'outreach_Outreach',
          method: 'isAvailableLog4jDisclaimer',
          result: { success: true, data: returnValue.toString() }
        }, {
          action: 'outreach_Outreach',
          method: 'readStatus',
          result: { success: true }
        }, {
          action: 'outreach_Outreach',
          method: 'getProxyDownloadNumbers',
          result: { success: true }
        }]
      });
    }

    it('renders a region named "Log4j Capability Notice" if the isAvailableLog4jDisclaimer call returns false',
        async function() {
          mockLog4jResponse(false);

          render(<Welcome />);

          expect(await selectors.log4jCapabiltyNotice('find')).toHaveTextContent(/log4j/i);
        }
    );

    it('does not render the log4j region if isAvailableLog4jDisclaimer returns true', async function() {
      mockLog4jResponse(true);

      render(<Welcome />);

      const loadingSpinner = selectors.loadingStatus();
      await waitForElementToBeRemoved(loadingSpinner);
      expect(selectors.log4jCapabiltyNotice('query')).not.toBeInTheDocument();
    });

    it('does not render the log4j region if the user is not an admin', async function() {
      user = { administrator: false };
      mockLog4jResponse(false);

      render(<Welcome />);

      const loadingSpinner = selectors.loadingStatus();
      await waitForElementToBeRemoved(loadingSpinner);
      expect(selectors.log4jCapabiltyNotice('query')).not.toBeInTheDocument();
    });

    it('does not render the log4j region if the user is not logged in', async function() {
      user = null;

      mockLog4jResponse(false);
      render(<Welcome />);

      const loadingSpinner = selectors.loadingStatus();
      await waitForElementToBeRemoved(loadingSpinner);
      expect(selectors.log4jCapabiltyNotice('query')).not.toBeInTheDocument();
    });

    describe('enable capability button', function() {
      let originalLocationDescriptor, mockLocation;

      beforeEach(function() {
        originalLocationDescriptor = Object.getOwnPropertyDescriptor(window, 'location');
        mockLocation = Object.create(null, {
          hash: {
            configurable: true,
            enumerable: true,
            set: () => {}
          }
        });

        Object.defineProperty(window, 'location', {
          get: () => mockLocation
        });
      });

      afterEach(function() {
        Object.defineProperty(window, 'location', originalLocationDescriptor);
      });

      it('renders within the log4j notice', async function() {
        mockLog4jResponse(false);
        render(<Welcome />);

        const notice = await selectors.log4jCapabiltyNotice('find'),
            btn = selectors.enableCapabilityBtn();

        expect(notice).toContainElement(btn);
      });

      it('calls the setLog4JVisualizerEnabled RPC call with an argument of true, and then navigates ' +
          'to #admin/repository/insightfrontend', async function() {
        let log4jCallArgs, log4jCallResolve;

        jest.spyOn(axios, 'post').mockImplementation((path, requestBody) => {
          return new Promise(resolve => {
            if (requestBody instanceof Array) {
              resolve({
                data: [{
                  action: 'outreach_Outreach',
                  method: 'isAvailableLog4jDisclaimer',
                  result: { success: true, data: 'false' }
                }, {
                  action: 'outreach_Outreach',
                  method: 'readStatus',
                  result: { success: true }
                }, {
                  action: 'outreach_Outreach',
                  method: 'getProxyDownloadNumbers',
                  result: { success: true }
                }]
              });
            }
            else if (requestBody.method === 'setLog4JVisualizerEnabled') {
              log4jCallArgs = requestBody.data;
              log4jCallResolve = resolve;
            }
            else {
              throw new Error('Unexpected requestBody');
            }
          });
        });

        const hashSpy = jest.spyOn(mockLocation, 'hash', 'set');

        render(<Welcome />);

        const btn = await selectors.enableCapabilityBtn('find');
        expect(log4jCallArgs).toBe(undefined);

        await userEvent.click(btn);
        expect(log4jCallArgs).toEqual([true]);
        expect(hashSpy).not.toHaveBeenCalled();

        log4jCallResolve({ data: { result: { success: true } } });
        await waitFor(() => expect(hashSpy).toHaveBeenCalledWith('#admin/repository/insightfrontend'));
      });
    });
  });

  describe('outreach iframe', function() {
    it('renders if the readStatus backend call does not fail', async function() {
      render(<Welcome />);

      const frame = await selectors.outreachFrame('find');
      expect(frame.tagName).toBe('IFRAME');
      expect(frame).toBeInTheDocument();
    });

    it('does not render if the extdirect call fails', async function() {
      jest.spyOn(axios, 'post').mockRejectedValue();

      render(<Welcome />);

      const loadingSpinner = selectors.loadingStatus();
      await waitForElementToBeRemoved(loadingSpinner);
      expect(selectors.outreachFrame('query')).not.toBeInTheDocument();
    });

    it('sets the iframe URL with the appropriate query parameters based on the status and license', async function() {
      jest.spyOn(ExtJS, 'useStatus').mockReturnValue({ version: '1.2.3-foo', edition: 'bar' });
      jest.spyOn(ExtJS, 'useLicense').mockReturnValue({ daysToExpiry: 42 });

      render(<Welcome />);

      const frame = await selectors.outreachFrame('find');
      expect(frame).toHaveAttribute('src', expect.stringMatching(/\?(.+&)?version=1\.2\.3-foo/));
      expect(frame).toHaveAttribute('src', expect.stringMatching(/\?(.+&)?versionMm=1\.2/));
      expect(frame).toHaveAttribute('src', expect.stringMatching(/\?(.+&)?edition=bar/));
      expect(frame).toHaveAttribute('src', expect.stringMatching(/\?(.+&)?daysToExpiry=42/));
    });

    it('sets the usertype query param to "admin" if the user is logged in as an admin', async function() {
      user = { administrator: true };

      render(<Welcome />);

      const frame = await selectors.outreachFrame('find');
      expect(frame).toHaveAttribute('src', expect.stringMatching(/\?(.*&)?usertype=admin/));
    });

    it('sets the usertype query param to "normal" if the user is logged and is not an admin', async function() {
      user = { administrator: false };

      render(<Welcome />);

      const frame = await selectors.outreachFrame('find');
      expect(frame).toHaveAttribute('src', expect.stringMatching(/\?(.*&)?usertype=normal/));
    });

    it('sets the usertype query param to "anonymous" if the user is not logged in', async function() {
      render(<Welcome />);

      const frame = await selectors.outreachFrame('find');
      expect(frame).toHaveAttribute('src', expect.stringMatching(/\?(.*&)?usertype=anonymous/));
    });

    it('sets iframe query parameters based on the getProxyDownloadNumbers API response', async function() {
      jest.spyOn(axios, 'post').mockResolvedValue({
        data: [{
          action: 'outreach_Outreach',
          method: 'isAvailableLog4jDisclaimer',
          result: { success: true }
        }, {
          action: 'outreach_Outreach',
          method: 'readStatus',
          result: { success: true }
        }, {
          action: 'outreach_Outreach',
          method: 'getProxyDownloadNumbers',
          result: { success: true, data: '&abc=123&def=9000'  }
        }]
      });

      render(<Welcome />);

      const frame = await selectors.outreachFrame('find');
      expect(frame).toHaveAttribute('src', expect.stringMatching(/\?(.*&)?abc=123/));
      expect(frame).toHaveAttribute('src', expect.stringMatching(/\?(.*&)?def=9000/));
    });
  });
});

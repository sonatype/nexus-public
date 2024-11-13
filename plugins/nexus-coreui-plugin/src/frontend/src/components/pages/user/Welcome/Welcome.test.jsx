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
import {render, screen, waitFor, within, waitForElementToBeRemoved} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {when} from 'jest-when';

import {APIConstants, ExtJS, Permissions} from '@sonatype/nexus-ui-plugin';

import UIStrings from '../../../../constants/UIStrings';
import Welcome from './Welcome.jsx';
import * as testData from './Welcome.testdata.js';
import {METRICS_CONTENT} from './UsageMetrics.testdata';

const {WELCOME: {
  ACTIONS: {
    SYSTEM_HEALTH,
    CLEANUP_POLICIES,
    BROWSE,
    SEARCH,
    RELEASE_NOTES,
    DOCUMENTATION,
    COMMUNITY,
    CONNECT,
  },
  CONNECT_MODAL,
}} = UIStrings;

const {USAGE_METRICS} = APIConstants.REST.INTERNAL;

// Creates a selector function that uses getByRole by default but which can be customized per-use to use
// queryByRole, findByRole, etc instead
const selectorQuery = (...queryParams) => queryType => screen[`${queryType ?? 'get'}ByRole`].apply(screen, queryParams);

const selectors = {
  main: () => screen.getByRole('main'),
  loadingStatus: () => screen.getByRole('status'),
  errorAlert: selectorQuery('alert'),
  errorRetryBtn: selectorQuery('button', { name: 'Retry' }),
  outreachFrame: selectorQuery('document', { name: 'Outreach Frame' }),
  quickAction: (name) => screen.queryByRole('button', {name: new RegExp(`${name}`)}),
  connectModal: () => screen.queryByRole('dialog', {name: CONNECT_MODAL.TITLE}),
  connectModalCloseButton: () => within(selectors.connectModal()).getByRole('button', {name: UIStrings.CLOSE}),
  queryAllCards: () => screen.queryAllByRole('region'),
};

const browseableFormats = [{id: 'test'}];

describe('Welcome', function() {
  let user;
  let status;

  beforeEach(function() {
    user = null;
    status = {edition: 'OSS'};

    jest.spyOn(axios, 'post').mockResolvedValue(testData.simpleSuccessResponse);
    jest.spyOn(axios, 'get').mockReturnValue(jest.fn());
    jest.spyOn(ExtJS, 'useStatus').mockReturnValue(status);
    jest.spyOn(ExtJS, 'useLicense').mockReturnValue({});
    jest.spyOn(ExtJS, 'checkPermission').mockReturnValue({});
    jest.spyOn(ExtJS, 'useUser').mockImplementation(() => user);
    jest.spyOn(ExtJS, 'state').mockReturnValue({ getUser: () => user, getValue: jest.fn()});
    jest.spyOn(ExtJS, 'isProEdition').mockReturnValue({});
    jest.spyOn(ExtJS, 'useState').mockReturnValue({});
    jest.spyOn(ExtJS, 'usePermission').mockReturnValue({});
    jest.spyOn(Object.getPrototypeOf(localStorage), 'getItem').mockImplementation(() => null);
    jest.spyOn(Object.getPrototypeOf(localStorage), 'setItem');

    when(ExtJS.state().getValue).calledWith('browseableformats').mockReturnValue([]);
    when(ExtJS.state().getValue).calledWith('status').mockReturnValue(status);
  });

  it('renders a main content area', function() {
    // resolving the promise in this otherwise-synchronous test causes act errors, so just leave it unresolved here
    jest.spyOn(axios, 'post').mockReturnValue(new Promise(() => {}));
    render(<Welcome />);

    expect(selectors.main()).toBeInTheDocument();
  });

  it('renders headings saying "Welcome"', async function() {
    jest.spyOn(axios, 'post').mockReturnValue(new Promise(() => {}));
    render(<Welcome />);

    expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent('Welcome');
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

      expect(axios.post).toHaveBeenCalledWith('service/extdirect', [
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
      expect(axios.post).toHaveBeenLastCalledWith('service/extdirect', [
        expect.objectContaining({ action: 'outreach_Outreach', method: 'readStatus' }),
        expect.objectContaining({ action: 'outreach_Outreach', method: 'getProxyDownloadNumbers' })
      ]);
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
      jest.spyOn(ExtJS, 'useStatus').mockReturnValue({ version: '1.2.3-foo', edition: 'OSS' });
      jest.spyOn(ExtJS, 'useLicense').mockReturnValue({ daysToExpiry: 42 });

      render(<Welcome />);

      const frame = await selectors.outreachFrame('find');
      expect(frame).toHaveAttribute('src', expect.stringMatching(/\?(.+&)?version=1\.2\.3-foo/));
      expect(frame).toHaveAttribute('src', expect.stringMatching(/\?(.+&)?versionMm=1\.2/));
      expect(frame).toHaveAttribute('src', expect.stringMatching(/\?(.+&)?edition=OSS/));
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

    it('sets the source of iframe to empty if user is not logged and edition is PRO', async function() {
      when(ExtJS.state().getValue).calledWith('status').mockReturnValue({edition: 'PRO'});

      render(<Welcome />);

      expect(selectors.outreachFrame('query')).not.toBeInTheDocument();
    });

    it('sets the usertype query param to "anonymous" if the user is not logged and edition is OSS', async function() {
      render(<Welcome />);

      const frame = await selectors.outreachFrame('find');
      expect(frame).toHaveAttribute('src', expect.stringMatching(/\?(.*&)?usertype=anonymous/));
    });

    it('sets iframe query parameters based on the getProxyDownloadNumbers API response', async function() {
      jest.spyOn(axios, 'post').mockResolvedValue({
        data: [
          testData.outreachReadStatusBasicSuccess,
          testData.outreachGetProxyDownloadNumbers('&abc=123&def=9000')
        ]
      });

      render(<Welcome />);

      const frame = await selectors.outreachFrame('find');
      expect(frame).toHaveAttribute('src', expect.stringMatching(/\?(.*&)?abc=123/));
      expect(frame).toHaveAttribute('src', expect.stringMatching(/\?(.*&)?def=9000/));
    });
  });

  describe('quick actions', function() {
    beforeEach(function() {
      when(ExtJS.checkPermission).calledWith(expect.anything()).mockReturnValue(false);
      when(ExtJS.state().getValue).calledWith('browseableformats').mockReturnValue([]);
    });

    it('shows "Release Notes", "Documentation" and "Community" for unauthorized user', async function() {
      const {quickAction, loadingStatus} = selectors;

      render(<Welcome />);
      await waitForElementToBeRemoved(loadingStatus());

      expect(quickAction(RELEASE_NOTES.subTitle)).toBeVisible();
      expect(quickAction(DOCUMENTATION.subTitle)).toBeVisible();
      expect(quickAction(COMMUNITY.subTitle)).toBeVisible();
    });

    it('shows "Browse", "Search" and "Connect" for anonymous user', async function() {
      const {quickAction, loadingStatus} = selectors;

      when(ExtJS.checkPermission).calledWith(Permissions.SEARCH.READ).mockReturnValue(true);
      when(ExtJS.state().getValue).calledWith('browseableformats').mockReturnValue(browseableFormats);

      render(<Welcome />);
      await waitForElementToBeRemoved(loadingStatus());

      expect(quickAction(BROWSE.subTitle)).toBeVisible();
      expect(quickAction(SEARCH.subTitle)).toBeVisible();
      expect(quickAction(CONNECT.subTitle)).toBeVisible();
    });

    it('shows "System Health", "Cleanup Policies" and "Browse" for admin', async function() {
      const {quickAction, loadingStatus} = selectors;
      user = { administrator: true };

      when(ExtJS.checkPermission).calledWith(Permissions.METRICS.READ).mockReturnValue(true);
      when(ExtJS.checkPermission).calledWith(Permissions.ADMIN).mockReturnValue(true);
      when(ExtJS.checkPermission).calledWith(Permissions.SEARCH.READ).mockReturnValue(true);
      when(ExtJS.state().getValue).calledWith('browseableformats').mockReturnValue(browseableFormats);

      render(<Welcome />);
      await waitForElementToBeRemoved(loadingStatus());

      expect(quickAction(SYSTEM_HEALTH.subTitle)).toBeVisible();
      expect(quickAction(CLEANUP_POLICIES.subTitle)).toBeVisible();
      expect(quickAction(BROWSE.subTitle)).toBeVisible();
    });

    it('shows the "Obtaining a Repository URL" modal', async function() {
      const {quickAction, connectModal, connectModalCloseButton, loadingStatus} = selectors;

      when(ExtJS.state().getValue).calledWith('browseableformats').mockReturnValue(browseableFormats);

      render(<Welcome />);
      await waitForElementToBeRemoved(loadingStatus());

      expect(localStorage.getItem).toHaveBeenLastCalledWith('nx-welcome-show-connect-action')
      expect(quickAction(CONNECT.subTitle)).toBeVisible();
      expect(connectModal()).not.toBeInTheDocument();

      userEvent.click(quickAction(CONNECT.subTitle));
      expect(connectModal()).toBeVisible();

      userEvent.click(connectModalCloseButton());
      expect(localStorage.setItem).toHaveBeenLastCalledWith('nx-welcome-show-connect-action', 'false');
    });

    it('doesn\'t show the "Connect" quick action', async function() {
      const {quickAction, loadingStatus} = selectors;

      when(ExtJS.state().getValue).calledWith('browseableformats').mockReturnValue(browseableFormats);
      when(localStorage.getItem).calledWith('nx-welcome-show-connect-action').mockReturnValue('false');

      render(<Welcome />);
      await waitForElementToBeRemoved(loadingStatus());

      expect(quickAction(CONNECT.subTitle)).not.toBeInTheDocument();
    });
  });

  describe('usage section', function() {
    it("renders metrics cards when an administrator", async () => {
      user = {administrator: true};

      when(axios.get)
          .calledWith(USAGE_METRICS).mockResolvedValue({data: METRICS_CONTENT});

      render(<Welcome />);
      await waitForElementToBeRemoved(selectors.loadingStatus());

      expect(selectors.queryAllCards().length).toBe(3);
    });

    it("does not render any card when not an administrator", async () => {
      user = {administrator: false};

      when(axios.get)
          .calledWith(USAGE_METRICS).mockResolvedValue({data: METRICS_CONTENT});

      render(<Welcome />);
      await waitForElementToBeRemoved(selectors.loadingStatus());

      expect(selectors.queryAllCards().length).toBe(0);
    });
  });
});

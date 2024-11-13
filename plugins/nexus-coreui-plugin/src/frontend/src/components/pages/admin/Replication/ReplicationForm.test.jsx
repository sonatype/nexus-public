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
import {render, screen, waitFor, waitForElementToBeRemoved} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {when} from 'jest-when';

import {ExtJS} from '@sonatype/nexus-ui-plugin';
import TestUtils from '@sonatype/nexus-ui-plugin/src/frontend/src/interface/TestUtils';

import ReplicationForm from './ReplicationForm';

import UIStrings from '../../../../constants/UIStrings';

const {
  USE_TRUST_STORE,
  CLOSE,
  SETTINGS
} = UIStrings;

jest.mock('axios', () => ({
  ...jest.requireActual('axios'),
  get: jest.fn(),
  put: jest.fn(),
  post: jest.fn(),
  delete: jest.fn()
}));

jest.mock('@sonatype/nexus-ui-plugin', () => ({
  ...jest.requireActual('@sonatype/nexus-ui-plugin'),
  ExtJS: {
    requestConfirmation: jest.fn(),
    checkPermission: jest.fn().mockReturnValue(true)
  },
  SslCertificateDetailsModal: ({onCancel}) => <div><button type="button" onClick={onCancel}>Close</button></div>
}));

const selectors = {
  ...TestUtils.selectors,
  getDeleteButton: () => screen.getByText('Delete'),
  getName: () => screen.getByLabelText('Name'),
  getSourceRepositoryName: () => screen.getByLabelText('Source Repository Name'),
  getSourceRepository: (optionText) => screen.getByText(optionText, {selector: '[name=sourceRepositoryName] option'}),
  getReplicateAllContentRadio: () => screen.getByLabelText('Replicate all content files in the Source Repository'),
  getReplicateSpecificContentRadio: () => screen.getByLabelText('Replicate specific content files in the Source Repository'),
  getContentFilter: () => screen.getByLabelText('Content Filter'),
  getDestinationInstanceUrl: () => screen.getByLabelText('Instance URL'),
  getDestinationInstanceUrlError: (message) => {
    const id = screen.getByLabelText('Instance URL').getAttribute('aria-errormessage');
    return screen.getByText(message, {selector: '#' + id});
  },
  getDestinationInstanceUsername: () => screen.getByLabelText('Username'),
  getDestinationInstancePassword: () => screen.getByLabelText('Password'),
  getTruststoreCheckbox: () => screen.getByLabelText('Use certificate connected to the Nexus Repository Truststore'),
  getViewCertificate: () => screen.getByText(USE_TRUST_STORE.VIEW_CERTIFICATE),
  getTestButton: () => screen.getByText('Test Repository Connection'),
  getTestSuccessStatus: () => screen.getByRole('status'),
  getDestinationRepositoryName: () => screen.getByLabelText('Target Repository Name'),
  getCreateButton: () => screen.getByText('Create Replication Connection'),
  getSaveButton: () => screen.getByText(SETTINGS.SAVE_BUTTON_LABEL),
  getCancelButton: () => screen.getByText(SETTINGS.CANCEL_BUTTON_LABEL),
  getCloseButton: () => screen.queryByText(CLOSE)
};

describe('ReplicationForm', function() {
  const CONFIRM = Promise.resolve();
  const onDone = jest.fn();
  const HOSTED_REPOSITORIES = [
    {
      "id": "maven-releases",
      "name": "maven-releases",
      "format": "maven2"
    }, {
      "id": "maven-snapshots",
      "name": "maven-snapshots",
      "format": "maven2"
    }, {
      "id": "nuget-hosted",
      "name": "nuget-hosted",
      "format": "nuget"
    }
  ];
  const REPLICATION_URL = (name) => `/service/rest/beta/replication/connection/${name}`;
  const REPLICATION_CREATE_URL = '/service/rest/beta/replication/connection/';
  const TEST_URL = 'service/rest/internal/ui/replication/test-connection/';

  function renderView(itemId = '') {
    return render(<ReplicationForm itemId={itemId} onDone={onDone}/>);
  }

  when(axios.get).calledWith(expect.stringContaining('service/rest/internal/ui/replication/repositories')).mockResolvedValue({
    data: HOSTED_REPOSITORIES
  });

  beforeEach(() => {
    when(global.NX.Permissions.check)
      .calledWith('nexus:ssl-truststore:read')
      .mockReturnValue(true);
    when(global.NX.Permissions.check)
      .calledWith('nexus:ssl-truststore:create')
      .mockReturnValue(true);
    when(global.NX.Permissions.check)
      .calledWith('nexus:ssl-truststore:update')
      .mockReturnValue(true);
  });

  it('renders the create form', async function() {
    renderView();

    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    expect(selectors.getSourceRepositoryName()).toContainElement(selectors.getSourceRepository(''));
    expect(selectors.getSourceRepositoryName()).toContainElement(selectors.getSourceRepository('maven-releases'));
    expect(selectors.getSourceRepositoryName()).toContainElement(selectors.getSourceRepository('maven-snapshots'));
    expect(selectors.getSourceRepositoryName()).toContainElement(selectors.getSourceRepository('nuget-hosted'));
  });

  it('calls onDone when cancelled', async function() {
    renderView();

    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    userEvent.click(selectors.getCancelButton());

    await waitFor(() => expect(onDone).toHaveBeenCalled());
  });

  it('creates a new replication connection', async function() {
    renderView();

    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    await TestUtils.changeField(selectors.getName, 'name');
    await TestUtils.changeField(selectors.getSourceRepositoryName, 'maven-releases');
    userEvent.click(selectors.getReplicateAllContentRadio());

    await TestUtils.changeField(selectors.getDestinationInstanceUrl, 'test');
    expect(selectors.getDestinationInstanceUrl()).toBeInvalid();
    expect(selectors.getDestinationInstanceUrlError('URL should be in the format "http://www.example.com"')).toBeInTheDocument();

    await TestUtils.changeField(selectors.getDestinationInstanceUrl, 'https://example.com');
    expect(selectors.getDestinationInstanceUrl()).toBeValid();

    userEvent.click(selectors.getTruststoreCheckbox());
    expect(selectors.getTruststoreCheckbox()).toBeChecked();

    await TestUtils.changeField(selectors.getDestinationInstanceUsername, 'user');
    expect(selectors.getTestButton()).toBeDisabled();

    await TestUtils.changeField(selectors.getDestinationInstancePassword, 'password');
    expect(selectors.getCreateButton()).toHaveAttribute('aria-disabled', 'true');
    expect(selectors.getTestButton()).not.toBeDisabled();

    const request = {
      name: '',
      destinationInstanceUrl: 'https://example.com',
      destinationInstanceUsername: 'user',
      destinationInstancePassword: 'password',
      repositoryFormat: "maven2",
      useTrustStore: true
    };
    when(axios.post).calledWith(TEST_URL, request).mockResolvedValue({
      data: HOSTED_REPOSITORIES
    });
    userEvent.click(selectors.getTestButton());
    expect(axios.post).toHaveBeenCalledWith(TEST_URL, request);

    await waitFor(() => expect(selectors.getDestinationRepositoryName()).not.toBeDisabled());
    expect(selectors.getTestSuccessStatus()).toBeInTheDocument();

    await TestUtils.changeField(selectors.getDestinationRepositoryName, 'maven-releases');
    expect(selectors.getCreateButton()).not.toHaveAttribute('aria-disabled', 'true');

    userEvent.click(selectors.getCreateButton());

    expect(axios.post).toHaveBeenLastCalledWith(REPLICATION_URL(''), {
      id: '',
      name: 'name',
      sourceRepositoryName: 'maven-releases',
      destinationInstanceUrl: 'https://example.com',
      destinationInstanceUsername: 'user',
      destinationInstancePassword: 'password',
      destinationRepositoryName: 'maven-releases',
      replicatedContent: 'all',
      includeExistingContent: false,
      contentRegexes: [],
      useTrustStore: true
    });
  });

  it('creates a new replication with content regexes', async function() {
    renderView();

    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    await TestUtils.changeField(selectors.getName, 'name');
    await TestUtils.changeField(selectors.getSourceRepositoryName, 'maven-releases');
    userEvent.click(selectors.getReplicateSpecificContentRadio());
    await waitFor(() => expect(selectors.getContentFilter()).toBeInTheDocument());
    await TestUtils.changeField(selectors.getContentFilter, '.*')

    await TestUtils.changeField(selectors.getDestinationInstanceUrl, 'http://example.com');
    await TestUtils.changeField(selectors.getDestinationInstanceUsername, 'user');
    await TestUtils.changeField(selectors.getDestinationInstancePassword, 'password');

    const request = {
      name: '',
      destinationInstanceUrl: 'http://example.com',
      destinationInstanceUsername: 'user',
      destinationInstancePassword: 'password',
      repositoryFormat: 'maven2',
      useTrustStore: false
    };
    when(axios.post).calledWith(TEST_URL, request).mockResolvedValue({
      data: HOSTED_REPOSITORIES
    });
    userEvent.click(selectors.getTestButton());
    expect(axios.post).toHaveBeenCalledWith(TEST_URL, request);

    await waitFor(() => expect(selectors.getDestinationRepositoryName()).not.toBeDisabled());
    await TestUtils.changeField(selectors.getDestinationRepositoryName, 'maven-releases');

    userEvent.click(selectors.getCreateButton());

    expect(axios.post).toHaveBeenLastCalledWith(REPLICATION_URL(''), {
      id: '',
      name: 'name',
      sourceRepositoryName: 'maven-releases',
      destinationInstanceUrl: 'http://example.com',
      destinationInstanceUsername: 'user',
      destinationInstancePassword: 'password',
      destinationRepositoryName: 'maven-releases',
      replicatedContent: 'regex',
      includeExistingContent: false,
      contentRegex: '.*',
      contentRegexes: ['.*']
    });
  });

  describe('edit replication', function() {
    const replicationConnection = {
      id: '1f707fb9-b85f-4f99-a9b2-1ad54b3e8245',
      name: 'name',
      sourceRepositoryName: 'maven-releases',
      destinationInstanceUrl: 'http://example.com',
      destinationInstanceUsername: 'user',
      destinationInstancePassword: '#~NXRM~PLACEHOLDER~PASSWORD~#',
      destinationRepositoryName: 'maven-releases',
      includeExistingContent: false,
      contentRegexes: null
    };
    when(axios.get).calledWith(REPLICATION_URL(replicationConnection.name)).mockResolvedValue({
      data: replicationConnection
    });
    when(axios.post).calledWith(TEST_URL, {
      name: replicationConnection.name,
      destinationInstanceUrl: replicationConnection.destinationInstanceUrl,
      destinationInstanceUsername: replicationConnection.destinationInstanceUsername,
      destinationInstancePassword: replicationConnection.destinationInstancePassword,
      useTrustStore: false,
      repositoryFormat: "maven2"
    }).mockResolvedValue({
      data: HOSTED_REPOSITORIES
    });

    it('edits an existing replication', async function() {
      renderView(replicationConnection.name);

      await waitForElementToBeRemoved(selectors.queryLoadingMask());

      expect(axios.get).toHaveBeenCalledWith(REPLICATION_URL(replicationConnection.name));

      expect(selectors.getSaveButton()).toHaveAttribute('aria-disabled', 'true');

      expect(selectors.getDestinationRepositoryName()).toBeDisabled();
      await TestUtils.changeField(selectors.getDestinationInstancePassword, 'password');

      const request = {
        name: replicationConnection.name,
        destinationInstanceUrl: replicationConnection.destinationInstanceUrl,
        destinationInstanceUsername: replicationConnection.destinationInstanceUsername,
        destinationInstancePassword: 'password',
        repositoryFormat: 'maven2',
        useTrustStore: false
      };
      when(axios.post).calledWith(TEST_URL, request).mockResolvedValueOnce({
        data: HOSTED_REPOSITORIES
      })
      userEvent.click(selectors.getTestButton());
      expect(axios.post).toHaveBeenCalledWith(TEST_URL, request);
      await waitFor(() => expect(selectors.getDestinationRepositoryName()).not.toBeDisabled());

      await TestUtils.changeField(selectors.getDestinationRepositoryName, 'maven-snapshots');

      expect(selectors.getSaveButton()).not.toHaveAttribute('aria-disabled', 'true');
      userEvent.click(selectors.getSaveButton());

      expect(axios.put).toHaveBeenLastCalledWith(REPLICATION_URL(replicationConnection.name), {
        id: replicationConnection.id,
        name: replicationConnection.name,
        sourceRepositoryName: replicationConnection.sourceRepositoryName,
        destinationInstanceUrl: replicationConnection.destinationInstanceUrl,
        destinationInstanceUsername: replicationConnection.destinationInstanceUsername,
        destinationInstancePassword: 'password',
        destinationRepositoryName: 'maven-snapshots',
        includeExistingContent: false,
        replicatedContent: 'all',
        contentRegexes: []
      });
    });

    it('requires re-validation when source has been changed', async function() {
      renderView(replicationConnection.name);

      await waitForElementToBeRemoved(selectors.queryLoadingMask());

      expect(selectors.getSourceRepositoryName()).toHaveValue("maven-releases");
      expect(selectors.getDestinationRepositoryName()).toHaveValue("maven-releases");
      expect(selectors.getDestinationRepositoryName()).toBeDisabled();

      await TestUtils.changeField(selectors.getSourceRepositoryName, 'nuget-hosted');

      expect(selectors.getDestinationRepositoryName()).toHaveValue("");
      expect(selectors.getDestinationRepositoryName()).toBeDisabled();

      when(axios.post).calledWith(TEST_URL, expect.anything()).mockResolvedValueOnce({
        data: HOSTED_REPOSITORIES
      })
      userEvent.click(selectors.getTestButton());

      await waitFor(() => expect(selectors.getDestinationRepositoryName()).not.toBeDisabled());

      await TestUtils.changeField(selectors.getDestinationRepositoryName, 'nuget-hosted');

      expect(selectors.getSaveButton()).not.toHaveAttribute('aria-disabled', 'true');
    });

    it('deletes an existing replication', async function() {
      renderView(replicationConnection.name);

      await waitForElementToBeRemoved(selectors.queryLoadingMask());

      expect(axios.get).toHaveBeenCalledWith(REPLICATION_URL(replicationConnection.name));

      ExtJS.requestConfirmation.mockReturnValue(CONFIRM);
      axios.delete.mockResolvedValue();
      userEvent.click(selectors.getDeleteButton());

      await waitFor(() => expect(axios.delete).toHaveBeenCalledWith(REPLICATION_URL(replicationConnection.name)));
      expect(onDone).toHaveBeenCalled();
    });

    it('enables the Nexus Repository truststore checkbox when an https url is used', async function() {
      renderView();

      await waitForElementToBeRemoved(selectors.queryLoadingMask());

      expect(selectors.getTruststoreCheckbox()).toBeDisabled();

      await TestUtils.changeField(selectors.getDestinationInstanceUrl, 'http://example.com');

      expect(selectors.getTruststoreCheckbox()).toBeDisabled();

      await TestUtils.changeField(selectors.getDestinationInstanceUrl, 'https://example.com');

      expect(selectors.getTruststoreCheckbox()).not.toBeDisabled();
    });
  });

  describe('test connection error handling', function() {
    async function setTestConfigurationFields() {
      await TestUtils.changeField(selectors.getDestinationInstanceUrl, 'http://example.com');
      await TestUtils.changeField(selectors.getDestinationInstanceUsername, 'user');
      await TestUtils.changeField(selectors.getDestinationInstancePassword, 'pass');
    }

    async function testConnection() {
      userEvent.click(selectors.getTestButton());
      await waitFor(() => expect(axios.post).toHaveBeenCalledWith(TEST_URL, expect.anything()));
    }

    function mockServerError(status, data) {
      when(axios.post).calledWith(TEST_URL, expect.anything()).mockRejectedValueOnce({
        response: {
          status,
          data
        }
      });
    }

    it('handles a 400 error', async function() {
      renderView();

      await waitForElementToBeRemoved(selectors.queryLoadingMask());

      await setTestConfigurationFields();

      mockServerError(400, 'Source version 3.35.0 does not match target version 3.34.0');

      await testConnection();

      expect(screen.getByText('HTTP status code: 400. ' +
          'A problem was detected when testing the connection to the target instance. ' +
          'Message from the target instance was "Source version 3.35.0 does not match target version 3.34.0". ' +
          'Please resolve the error and try again.')
      ).toBeInTheDocument();
    });

    it('handles a 401 error', async function() {
      renderView();

      await waitForElementToBeRemoved(selectors.queryLoadingMask());

      await setTestConfigurationFields();

      mockServerError(401);

      await testConnection();

      expect(screen.getByText('HTTP status code: 401. ' +
          'The tested target Instance URL and User Authentication credentials are not valid and cannot retrieve a list ' +
          'of available repositories. Please make sure the User Authentication credentials are correct and try again.')
      );
    });

    it('handles a 403 error', async function() {
      renderView();

      await waitForElementToBeRemoved(selectors.queryLoadingMask());

      await setTestConfigurationFields();

      mockServerError(403);

      await testConnection();

      expect(screen.getByText('HTTP status code: 403. ' +
          'The tested target Instance URL and User Authentication credentials are not acknowledged and cannot retrieve a list ' +
          'of available repositories. Please make sure the target Instance URL and User Authentication credentials are correct and try again.')
      );
    });

    it('handles a 404 error', async function() {
      renderView();

      await waitForElementToBeRemoved(selectors.queryLoadingMask());

      await setTestConfigurationFields();

      mockServerError(404);

      await testConnection();

      expect(screen.getByText('HTTP status code: 404. ' +
          'The test connection cannot retrieve a list of available repositories because Replication is not yet supported on ' +
          'this target instance URL. Please enter a new target Instance URL and User Authentication credentials and try again.')
      );
    });

    it('handles a 500 error', async function() {
      renderView();

      await waitForElementToBeRemoved(selectors.queryLoadingMask());

      await setTestConfigurationFields();

      mockServerError(500);

      await testConnection();

      expect(screen.getByText('HTTP status code: 500. ' +
          'The test connection cannot retrieve a list of available repositories because of a target server error. ' +
          'Please try again.')
      );
    });

    it('handles an unknown error > 400', async function() {
      renderView();

      await waitForElementToBeRemoved(selectors.queryLoadingMask());

      await setTestConfigurationFields();

      mockServerError(499);

      await testConnection();

      expect(screen.findByText('An unknown error occurred while testing the connection. ' +
          'The test connection cannot retrieve a list of available repositories because of a target server error. Please try again.')
      );
    });
  });

  describe('save target connection error handling', function() {
    async function saveConnectionError(status, errorMessage) {
      await TestUtils.changeField(selectors.getName, 'name');
      await TestUtils.changeField(selectors.getSourceRepositoryName, 'maven-releases');
      await TestUtils.changeField(selectors.getDestinationInstanceUrl, 'http://example.com');

      await TestUtils.changeField(selectors.getDestinationInstanceUrl, 'http://example.com');
      await TestUtils.changeField(selectors.getDestinationInstanceUsername, 'user');
      await TestUtils.changeField(selectors.getDestinationInstancePassword, 'pass');

      when(axios.post).calledWith(TEST_URL, expect.anything()).mockResolvedValueOnce({
        data: HOSTED_REPOSITORIES
      });

      userEvent.click(selectors.getTestButton());
      await waitFor(() => expect(selectors.getDestinationRepositoryName()).not.toBeDisabled());
      expect(selectors.getTestSuccessStatus()).toBeInTheDocument();

      await TestUtils.changeField(selectors.getDestinationRepositoryName, 'maven-releases');
      expect(selectors.getCreateButton()).not.toHaveAttribute('aria-disabled', 'true');

      when(axios.post).calledWith(REPLICATION_CREATE_URL, expect.anything()).mockRejectedValue({
        response: {
          data: [
          {
            'id': 'connectionStatus',
            'message': status
          },
          {
            'id': 'connectionMessage',
            'message': 'target instance message'
          }]
      }
      });

      userEvent.click(selectors.getCreateButton());

      expect(axios.post).toHaveBeenCalledWith(REPLICATION_CREATE_URL, {
        'contentRegexes': [],
        'destinationInstancePassword': 'pass',
        'destinationInstanceUrl': 'http://example.com',
        'destinationInstanceUsername': 'user',
        'destinationRepositoryName': 'maven-releases',
        'id': '',
        'name': 'name',
        'includeExistingContent': false,
        'sourceRepositoryName': 'maven-releases',
        'replicatedContent': 'all'
      });

      const alert = await screen.findByText(errorMessage);
      expect(alert).toBeInTheDocument();
    }

    it('handles a 400 error from target', async function() {
      renderView();

      await waitForElementToBeRemoved(selectors.queryLoadingMask());

      await saveConnectionError("400",
          'HTTP status code: 400. ' +
          'Replication could not be enabled on the target instance. Message from the target instance was "target instance message". ' +
          'Please resolve the error and try again.');
    });

    it('handles a 401 error from target', async function() {
      renderView();

      await waitForElementToBeRemoved(selectors.queryLoadingMask());

      await saveConnectionError("401",
          'HTTP status code: 401. ' +
          'The target Instance URL and User Authentication credentials are not valid and cannot enable replication on the target. ' +
          'Please make sure the User Authentication credentials are correct and try again.');
    });

    it('handles a 403 error from target', async function() {
      renderView();

      await waitForElementToBeRemoved(selectors.queryLoadingMask());

      await saveConnectionError("403",
          'HTTP status code: 403. ' +
          'The target Instance URL and User Authentication credentials are not acknowledged and cannot enable replication on ' +
          'the target. Please make sure the target Instance URL and User Authentication credentials are correct and try again.');
    });

    it('handles a 404 error from target', async function() {
      renderView();

      await waitForElementToBeRemoved(selectors.queryLoadingMask());

      await saveConnectionError("404",
          'HTTP status code: 404. ' +
          'The target Instance URL cannot be used to enable replication because Replication is not yet supported on ' +
          'this target instance URL. Please enter a new target Instance URL and User Authentication credentials and try again.');
    });

    it('handles a 500 error from target', async function() {
      renderView();

      await waitForElementToBeRemoved(selectors.queryLoadingMask());

      await saveConnectionError("500",
          'HTTP status code: 500. ' +
          'The target Instance URL cannot be used to enable replication because of a server error. Please try again.');
    });
  });

  describe('SSL Certificate Modal', function() {
    it('fetches the details of the destination url', async function() {
      renderView();

      await waitForElementToBeRemoved(selectors.queryLoadingMask());

      await TestUtils.changeField(selectors.getDestinationInstanceUrl, 'https://localhost');

      expect(selectors.getViewCertificate()).toBeInTheDocument();

      userEvent.click(selectors.getViewCertificate());

      expect(selectors.getCloseButton()).toBeInTheDocument();

      userEvent.click(selectors.getCloseButton());

      expect(selectors.getCloseButton()).not.toBeInTheDocument();
    });
  });
});

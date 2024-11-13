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
import Axios from 'axios';
import {
  render,
  cleanup,
  screen,
  waitFor,
  waitForElementToBeRemoved,
  getAllByRole,
  getByRole,
  queryByRole,
  within
} from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import {when} from 'jest-when';
import {mergeDeepRight} from 'ramda';

import {ExtAPIUtils, APIConstants, ExtJS} from '@sonatype/nexus-ui-plugin';
import TestUtils from '@sonatype/nexus-ui-plugin/src/frontend/src/interface/TestUtils';
import UIStrings from '../../../../constants/UIStrings';

import RepositoriesForm from './RepositoriesForm';

import {getRepositoryUrl, saveRepositoryUrl, deleteRepositoryUrl} from './RepositoriesFormMachine';
import {RECIPES_URL} from './facets/GenericFormatConfiguration';
import {ROUTING_RULES_URL} from './facets/GenericOptionsConfiguration';
import {genericDefaultValues} from './RepositoryFormDefaultValues';
import {DOCKER_HUB_URL} from './facets/DockerIndexConfiguration';

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
    checkPermission: jest.fn(),
    requestConfirmation: jest.fn(),
    state: () => ({
      getValue: jest.fn(() => false),
    }),
    isProEdition: jest.fn().mockReturnValue(true),
  }
}));

const REPOSITORIES_SERVICE = [
  {
    context: {
      data: [
        {name: 'docker-proxy-1', format: 'docker', type: 'proxy'},
        {name: 'docker-hosted-1', format: 'docker', type: 'hosted'},
        {name: 'maven-central', format: 'maven2'},
        {name: 'maven-releases', format: 'maven2'},
        {name: 'maven-snapshots', format: 'maven2'},
        {name: 'raw-group', format: 'raw'},
        {name: 'raw-hosted', format: 'raw'},
        {name: 'raw-proxy', format: 'raw'}
      ]
    }
  }
];
jest.mock('./RepositoriesContextProvider', () => ({
  useRepositoriesService: jest.fn(() => REPOSITORIES_SERVICE)
}));

const {
  REPOSITORIES: {
    EDITOR,
    EDITOR: {
      APT,
      DOCKER: {INDEX, CONNECTORS}
    }
  },
  SETTINGS,
  USE_TRUST_STORE,
  CLOSE
} = UIStrings;

const {
  EXT: {URL: EXT_URL},
  REST: {
    PUBLIC: {
      REPOSITORIES: REST_PUB_URL,
      SSL_CERTIFICATES,
      CERTIFICATE_DETAILS
    }
  }
} = APIConstants;

const BLOB_STORE_EXT_REQUEST = ExtAPIUtils.createRequestBody('coreui_Blobstore', 'readNames');

function CLEANUP_EXT_REQUEST(format = 'maven2') {
  return ExtAPIUtils.createRequestBody('cleanup_CleanupPolicy', 'readByFormat', {
    filterField: 'format',
    filterValue: format
  });
}

const getFormatOptions = (recipes) => {
  const formats = recipes.map(({format}) => format);
  const uniqFormats = [...new Set(formats)];
  const formatOptions = uniqFormats.map((format) => ({name: format}));
  return [{name: EDITOR.SELECT_FORMAT_OPTION}, ...formatOptions];
};

const getSaveUrl = (repoData) => {
  const {format, type} = repoData;
  return saveRepositoryUrl(format, type);
};

describe('RepositoriesForm', () => {
  const getCheckbox = (fieldsetLabel) => {
    const container = screen.queryByRole('group', {name: fieldsetLabel});
    return container ? queryByRole(container, 'checkbox') : null;
  };

  const selectors = {
    ...TestUtils.selectors,
    ...TestUtils.formSelectors,
    getCreateButton: () => screen.getByText(EDITOR.CREATE_BUTTON, {selector: 'button'}),
    getDeleteButton: () => screen.queryByRole('button', {name: SETTINGS.DELETE_BUTTON_LABEL}),

    getCancelButton: () => screen.queryByText(SETTINGS.CANCEL_BUTTON_LABEL),
    getReadOnlyUrl: () => screen.getByText(EDITOR.URL_LABEL, {selector: 'dt'}),
    getFormatSelect: () => screen.getByLabelText(EDITOR.FORMAT_LABEL),
    getTypeSelect: () => screen.getByLabelText(EDITOR.TYPE_LABEL),
    getNameInput: () => screen.getByLabelText(EDITOR.NAME_LABEL),
    getBlobStoreSelect: () => screen.getByLabelText(EDITOR.BLOB_STORE_LABEL),
    getDeploymentPolicySelect: () => screen.queryByLabelText(EDITOR.DEPLOYMENT_POLICY_LABEL),
    getStatusCheckbox: () => screen.getByRole('checkbox', {name: EDITOR.STATUS_DESCR}),
    getProprietaryComponentsCheckbox: () =>
      screen.queryByRole('checkbox', {
        name: EDITOR.PROPRIETARY_COMPONENTS_DESCR
      }),
    getHostedSectionTitle: () => screen.queryByText(EDITOR.HOSTED_CAPTION),
    getCleanupSectionTitle: () => screen.queryByText(EDITOR.CLEANUP_CAPTION),
    getGroupSectionTitle: () => screen.queryByText(EDITOR.GROUP_CAPTION),
    getTransferListOption: (optionLabel) => screen.getByText(optionLabel),
    getRoutingRuleSelect: () => screen.queryByLabelText(EDITOR.ROUTING_RULE_LABEL),
    getRemoteUrlInput: () => screen.queryByLabelText(EDITOR.REMOTE_STORAGE_LABEL),
    getContentMaxAgeInput: () => screen.getByLabelText(EDITOR.MAX_COMP_AGE_LABEL),
    getMetadataMaxAgeInput: () => screen.getByLabelText(EDITOR.MAX_META_AGE_LABEL),
    getAuthTypeSelect: () => screen.getByLabelText(EDITOR.AUTH_TYPE_LABEL),
    getUsernameInput: () => screen.queryByLabelText(EDITOR.USERNAME_LABEL),
    getPasswordInput: () => screen.queryByLabelText(EDITOR.PASSWORD_LABEL),
    getNtlmHostInput: () => screen.queryByLabelText(EDITOR.NTLM_HOST_LABEL),
    getNtlmDomainInput: () => screen.queryByLabelText(EDITOR.NTLM_DOMAIN_LABEL),
    getUserAgentSuffixInput: () => screen.getByLabelText(EDITOR.USER_AGENT_LABEL),
    getRetriesInput: () => screen.getByLabelText(EDITOR.RETRIES_LABEL),
    getTimeoutInput: () => screen.getByLabelText(EDITOR.TIMEOUT_LABEL),
    getTimeToLiveInput: () => screen.getByLabelText(EDITOR.NEGATIVE_CACHE_TTL_LABEL),
    getBlockedCheckbox: () => screen.getByRole('checkbox', {name: EDITOR.BLOCK_DESCR}),
    getAutoBlockCheckbox: () => screen.getByRole('checkbox', {name: EDITOR.AUTO_BLOCK_DESCR}),
    getCookiesCheckbox: () => getCheckbox(EDITOR.COOKIES_LABEL),
    getRedirectsCheckbox: () => getCheckbox(EDITOR.REDIRECTS_LABEL),
    getContentValidationCheckbox: () => getCheckbox(EDITOR.CONTENT_VALIDATION_LABEL),
    getRewritePackageUrlsCheckbox: () => getCheckbox(EDITOR.REWRITE_URLS_LABEL),
    getRepodataDepthSelect: () => screen.getByLabelText(EDITOR.REPODATA_DEPTH_LABEL),
    getLayoutPolicySelect: () => screen.getByLabelText(EDITOR.LAYOUT_POLICY_LABEL),
    getContentDispositionSelect: () => screen.getByLabelText(EDITOR.CONTENT_DISPOSITION_LABEL),
    getRemoveQuarantinedCheckbox: () =>
      screen.getByRole('checkbox', {name: EDITOR.NPM.REMOVE_QUARANTINED.DESCR}),
    getVersionPolicySelect: () => screen.getByLabelText(EDITOR.VERSION_POLICY_LABEL),
    getDockerSubdomainCheckbox: () =>
      screen.getAllByRole('checkbox', {name: 'Toggle Text Input'})[0],
    getDockerConnectorHttpPortCheckbox: () =>
      screen.getAllByRole('checkbox', {name: 'Toggle Text Input'})[1],
    getDockerConnectorHttpsPortCheckbox: () =>
      screen.getAllByRole('checkbox', {name: 'Toggle Text Input'})[2],
    getDockerSubdomainInput: () =>
      screen.getByPlaceholderText(CONNECTORS.SUBDOMAIN.PLACEHOLDER),
    getDockerConnectorHttpPortInput: () =>
      screen.getAllByPlaceholderText(CONNECTORS.HTTP.PLACEHOLDER)[0],
    getDockerConnectorHttpsPortInput: () =>
      screen.getAllByPlaceholderText(CONNECTORS.HTTPS.PLACEHOLDER)[1],
    getDockerConnectorSamePortsError: () =>
      screen.getAllByText(CONNECTORS.SAME_PORTS_ERROR),
    getDockerApiVersionCheckbox: () =>
      screen.getByRole('checkbox', {name: EDITOR.REGISTRY_API_SUPPORT_DESCR}),
    getDockerAnonimousPullCheckbox: () =>
      screen.getByRole('checkbox', {name: CONNECTORS.ALLOW_ANON_DOCKER_PULL.DESCR}),
    getDockerWritableRepositorySelect: () => screen.getByLabelText(EDITOR.WRITABLE.LABEL),
    getDockerRedeployLatestCheckbox: () => screen.getByLabelText(EDITOR.REDEPLOY_LATEST.DESCRIPTION),
    getDockerRedeployLatestLabel: () => screen.getByText(EDITOR.REDEPLOY_LATEST.DESCRIPTION),
    getUseNexusTruststoreCheckbox: () =>
      screen.getByRole('checkbox', {name: USE_TRUST_STORE.DESCRIPTION}),
    getUseNexusTruststoreButton: () =>
      screen.getByRole('button', {name: USE_TRUST_STORE.VIEW_CERTIFICATE}),
    getCloseCertificateButton: () => screen.getByText(CLOSE, {selector: 'button'}),
    getDockerIndexRadioButtons: () => ({
      registry: screen.getByLabelText(INDEX.OPTIONS.REGISTRY),
      hub: screen.getByLabelText(INDEX.OPTIONS.HUB),
      custom: screen.getByLabelText(INDEX.OPTIONS.CUSTOM)
    }),
    getDockerIndexUrlInput: () => screen.queryByLabelText(EDITOR.DOCKER.INDEX.URL.LABEL),
    getPreemptiveAuthCheckbox: () => {
      const fieldset = screen.queryByRole('group', {name: EDITOR.PRE_EMPTIVE_AUTH.LABEL});
      return fieldset ? within(fieldset).getByRole('checkbox') : null;
    },
    getForeignLayerCheckbox: () => screen.getByLabelText(EDITOR.FOREIGN_LAYER.CHECKBOX),
    getForeignLayerInput: () => screen.getByLabelText(EDITOR.FOREIGN_LAYER.URL),
    getForeignLayerAddButton: (container) => container.querySelector('[data-icon="plus-circle"]'),
    getForeignLayerRemoveButton: (item) => {
      const listItem = screen.getByText(item).closest('.nx-list__item');
      return item && within(listItem).getByRole('img', {hidden: true}).closest('button');
    },
    getAptDistributionInput: () => screen.getByLabelText(APT.DISTRIBUTION.LABEL),
    getAptFlatCheckbox: () => screen.getByRole('checkbox', {name: APT.FLAT.DESCR}),
    getAptKeyInput: () => screen.getByLabelText(APT.SIGNING.KEY.LABEL),
    getAptPassphraseInput: () => screen.getByLabelText(APT.SIGNING.PASSPHRASE.LABEL)
  };

  const renderView = (itemId = '') => {
    return render(<RepositoriesForm itemId={itemId} onDone={onDone} />);
  };

  const renderViewAndSetRequiredFields = async (repoData) => {
    const {queryLoadingMask, getFormatSelect, getTypeSelect, getNameInput} = selectors;
    const {format, type, name} = repoData;

    renderView();
    await waitForElementToBeRemoved(queryLoadingMask());

    await TestUtils.changeField(getFormatSelect, format);
    await TestUtils.changeField(getTypeSelect, type);
    await TestUtils.changeField(getNameInput, name || '');
  };

  const onDone = jest.fn();

  const RECIPES_RESPONSE = [
    {format: 'apt', type: 'hosted'},
    {format: 'apt', type: 'proxy'},
    {format: 'docker', type: 'proxy'},
    {format: 'docker', type: 'hosted'},
    {format: 'docker', type: 'group'},
    {format: 'maven2', type: 'group'},
    {format: 'maven2', type: 'hosted'},
    {format: 'maven2', type: 'proxy'},
    {format: 'npm', type: 'proxy'},
    {format: 'nuget', type: 'hosted'},
    {format: 'nuget', type: 'proxy'},
    {format: 'nuget', type: 'group'},
    {format: 'p2', type: 'proxy'},
    {format: 'raw', type: 'proxy'},
    {format: 'raw', type: 'hosted'},
    {format: 'raw', type: 'group'},
    {format: 'yum', type: 'hosted'}
  ];

  const FORMAT_OPTIONS = getFormatOptions(RECIPES_RESPONSE);

  const BLOB_STORES_RESPONSE = [{name: 'default'}, {name: 'blob-store-1'}, {name: 'blob-store-2'}];

  const BLOB_STORES_OPTIONS = [{name: EDITOR.SELECT_STORE_OPTION}, ...BLOB_STORES_RESPONSE];

  const TYPE_OPTIONS = [
    {name: EDITOR.SELECT_TYPE_OPTION},
    {name: 'group'},
    {name: 'proxy'},
    {name: 'hosted'}
  ];

  const MAVEN_REPOS_RESPONSE = [
    {id: 'maven-central', name: 'maven-central', format: 'maven2'},
    {id: 'maven-releases', name: 'maven-releases', format: 'maven2'},
    {id: 'maven-snapshots', name: 'maven-snapshots', format: 'maven2'}
  ];

  const MAVEN_CLEANUP_RESPONSE = [
    {id: 'policy-all-fomats', name: 'policy-all-fomats'},
    {id: 'policy-maven-1', name: 'policy-maven-1'},
    {id: 'policy-maven-2', name: 'policy-maven-2'}
  ];

  const ROUTING_RULES_RESPONSE = [
    {id: 'routing-rule-1', name: 'routing-rule-1'},
    {id: 'routing-rule-2', name: 'routing-rule-2'}
  ];

  beforeEach(() => {
    when(Axios.get)
      .calledWith(expect.stringContaining(RECIPES_URL))
      .mockResolvedValue({data: RECIPES_RESPONSE});
    when(Axios.post)
      .calledWith(EXT_URL, BLOB_STORE_EXT_REQUEST)
      .mockResolvedValue({data: TestUtils.makeExtResult(BLOB_STORES_RESPONSE)});
    when(Axios.post)
      .calledWith(EXT_URL, CLEANUP_EXT_REQUEST())
      .mockResolvedValue({data: TestUtils.makeExtResult(MAVEN_CLEANUP_RESPONSE)});
    when(Axios.get)
      .calledWith(expect.stringContaining(ROUTING_RULES_URL))
      .mockResolvedValue({data: ROUTING_RULES_RESPONSE});
  });

  it('filters types by format', async () => {
    renderView();

    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    await TestUtils.changeField(selectors.getFormatSelect, 'maven2');
    await TestUtils.changeField(selectors.getTypeSelect, 'group');
    expect(selectors.getGroupSectionTitle()).toBeInTheDocument();

    await TestUtils.changeField(selectors.getFormatSelect, 'nuget');
    expect(selectors.getTypeSelect()).toHaveValue('group');
    expect(selectors.getContentValidationCheckbox()).not.toBeInTheDocument();

    await TestUtils.changeField(selectors.getFormatSelect, 'p2');
    expect(selectors.getTypeSelect()).toHaveValue('');
    expect(selectors.getTypeSelect()).not.toContainElement(screen.queryByText('hosted'));
    expect(selectors.getTypeSelect()).not.toContainElement(screen.queryByText('group'));
  });

  it('calls onDone when cancelled', async () => {
    await renderViewAndSetRequiredFields({format: 'maven2', type: 'group'});
    userEvent.click(selectors.getCancelButton());
    await waitFor(() => expect(onDone).toHaveBeenCalled());
  });

  it('renders raw fields with correct default values', async () => {
    renderView();
    await waitForElementToBeRemoved(selectors.queryLoadingMask());
    await TestUtils.changeField(selectors.getFormatSelect, 'raw');

    await TestUtils.changeField(selectors.getTypeSelect, 'proxy');
    validateSelect(selectors.getContentDispositionSelect(), null, 'ATTACHMENT');

    await TestUtils.changeField(selectors.getTypeSelect, 'hosted');
    validateSelect(selectors.getContentDispositionSelect(), null, 'ATTACHMENT');

    await TestUtils.changeField(selectors.getTypeSelect, 'group');
    validateSelect(selectors.getContentDispositionSelect(), null, 'ATTACHMENT');
  });

  describe('hosted', () => {
    it('renders the form and populates dropdowns when type is hosted', async () => {
      const blobStoreResponse = [{name: 'default'}];
      const blobStoreOptions = [{name: EDITOR.SELECT_STORE_OPTION}, ...blobStoreResponse];
      when(Axios.post)
        .calledWith(EXT_URL, BLOB_STORE_EXT_REQUEST)
        .mockResolvedValue({data: TestUtils.makeExtResult(blobStoreResponse)});

      await renderViewAndSetRequiredFields({format: 'maven2', type: 'hosted'});

      await waitFor(() =>
        validateSelect(selectors.getBlobStoreSelect(), blobStoreOptions, 'default')
      );

      const deploymentPolicyOptions = Object.values(EDITOR.DEPLOYMENT_POLICY_OPTIONS).map(
        (name) => ({
          name
        })
      );
      validateSelect(selectors.getDeploymentPolicySelect(), deploymentPolicyOptions, 'ALLOW_ONCE');

      await waitFor(() => expect(Axios.post).toHaveBeenCalledWith(EXT_URL, CLEANUP_EXT_REQUEST()));

      MAVEN_CLEANUP_RESPONSE.forEach((policy) => {
        expect(selectors.getTransferListOption(policy.name)).toBeInTheDocument();
      });

      expect(selectors.getContentValidationCheckbox()).toBeInTheDocument();
      expect(selectors.getGroupSectionTitle()).not.toBeInTheDocument();
    });

    it('creates hosted repository', async () => {
      const repo = {
        format: 'maven2',
        type: 'hosted',
        name: 'maven-hosted-1',
        online: true,
        storage: {
          blobStoreName: 'blob-store-1',
          strictContentTypeValidation: false,
          writePolicy: 'ALLOW'
        },
        component: {proprietaryComponents: true},
        cleanup: {policyNames: ['policy-all-fomats', 'policy-maven-1']},
        maven: {
          contentDisposition: 'INLINE',
          layoutPolicy: 'STRICT',
          versionPolicy: 'RELEASE'
        }
      };

      await renderViewAndSetRequiredFields(repo);

      await TestUtils.changeField(selectors.getDeploymentPolicySelect, repo.storage.writePolicy);
      await TestUtils.changeField(selectors.getBlobStoreSelect, repo.storage.blobStoreName);

      userEvent.click(selectors.getContentValidationCheckbox());
      userEvent.click(selectors.getProprietaryComponentsCheckbox());
      userEvent.click(selectors.getTransferListOption(repo.cleanup.policyNames[0]));
      userEvent.click(selectors.getTransferListOption(repo.cleanup.policyNames[1]));
      userEvent.click(selectors.getCreateButton());

      await waitFor(() => expect(Axios.post).toHaveBeenCalledWith(getSaveUrl(repo), repo));
    });

    it('edits raw hosted repositories', async function () {
      const repo = {
        name: 'raw-hosted',
        format: 'raw',
        url: 'http://localhost:8081/repository/raw-hosted',
        online: true,
        storage: {
          blobStoreName: 'default',
          strictContentTypeValidation: true,
          writePolicy: 'ALLOW_ONCE'
        },
        cleanup: {
          policyNames: []
        },
        component: {
          proprietaryComponents: false
        },
        type: 'hosted'
      };
      when(Axios.get).calledWith(getRepositoryUrl(repo.name)).mockResolvedValueOnce({
        data: repo
      });

      renderView('raw-hosted');

      await waitForElementToBeRemoved(selectors.queryLoadingMask());

      validateSelect(selectors.getNameInput(), null, repo.name);
      validateSelect(selectors.getFormatSelect(), null, repo.format);
      validateSelect(selectors.getTypeSelect(), null, repo.type);
      expect(selectors.getReadOnlyUrl().nextSibling).toHaveTextContent(repo.url);
      expect(selectors.getContentValidationCheckbox()).toBeChecked();

      userEvent.click(selectors.getContentValidationCheckbox());

      expect(selectors.getContentValidationCheckbox()).not.toBeChecked();

      userEvent.click(selectors.querySubmitButton());

      expect(Axios.put).toBeCalledWith(
        REST_PUB_URL + 'raw/hosted/raw-hosted',
        mergeDeepRight(repo, {
          storage: {
            strictContentTypeValidation: false
          }
        })
      );
    });

    it('renders yum hosted fields with correct default values', async () => {
      await renderViewAndSetRequiredFields({format: 'yum', type: 'hosted'});
      validateSelect(selectors.getRepodataDepthSelect(), null, 0);
      validateSelect(selectors.getLayoutPolicySelect(), null, 'STRICT');
    });

    it('creates docker hosted repository', async () => {
      const repo = {
        format: 'docker',
        type: 'hosted',
        name: 'docker-hosted-1',
        online: true,
        storage: {
          blobStoreName: 'default',
          strictContentTypeValidation: true,
          writePolicy: 'ALLOW_ONCE',
          latestPolicy: true
        },
        component: {
          proprietaryComponents: false
        },
        cleanup: null,
        docker: {
          httpPort: null,
          httpsPort: null,
          forceBasicAuth: false,
          v1Enabled: false,
          subdomain: null
        }
      };

      await renderViewAndSetRequiredFields(repo);

      expect(selectors.getDockerRedeployLatestCheckbox()).toBeDisabled();
      await TestUtils.expectToSeeTooltipOnHover(selectors.getDockerRedeployLatestLabel(), EDITOR.REDEPLOY_LATEST.TOOLTIP);

      await TestUtils.changeField(selectors.getDeploymentPolicySelect, repo.storage.writePolicy);
      await TestUtils.changeField(selectors.getBlobStoreSelect, repo.storage.blobStoreName);

      expect(selectors.getDockerRedeployLatestCheckbox()).toBeEnabled();

      userEvent.click(selectors.getDockerRedeployLatestCheckbox());

      userEvent.click(selectors.getDockerSubdomainCheckbox());
      await TestUtils.changeField(selectors.getDockerSubdomainInput, 'docker-sub-domain');
      userEvent.click(selectors.getDockerSubdomainCheckbox());
      expect(selectors.getDockerSubdomainInput()).toHaveValue('');

      userEvent.click(selectors.getCreateButton());

      await waitFor(() => expect(Axios.post).toHaveBeenCalledWith(getSaveUrl(repo), repo));
    });

    it('creates apt hosted repository', async () => {
      const repo = {
        format: 'apt',
        type: 'hosted',
        name: 'apt-hosted-1',
        online: true,
        storage: {
          blobStoreName: 'default',
          strictContentTypeValidation: true,
          writePolicy: 'ALLOW_ONCE'
        },
        component: {
          proprietaryComponents: false
        },
        cleanup: null,
        apt: {
          distribution: 'bionic'
        },
        aptSigning: {
          keypair: 'apt-key-pair',
          passphrase: 'apt-key-pass'
        }
      };

      await renderViewAndSetRequiredFields(repo);

      await TestUtils.changeField(selectors.getBlobStoreSelect, repo.storage.blobStoreName);

      await TestUtils.changeField(selectors.getAptDistributionInput, repo.apt.distribution);
      await TestUtils.changeField(selectors.getAptKeyInput, repo.aptSigning.keypair);
      await TestUtils.changeField(selectors.getAptPassphraseInput, repo.aptSigning.passphrase);

      userEvent.click(selectors.getCreateButton());

      await waitFor(() => expect(Axios.post).toHaveBeenCalledWith(getSaveUrl(repo), repo));
    });
  });

  describe('proxy', () => {
    const data = {
      format: 'maven2',
      type: 'proxy',
      name: 'maven-proxy-1',
      online: true,
      routingRule: '',
      storage: {
        blobStoreName: 'default',
        strictContentTypeValidation: true
      },
      cleanup: null,
      proxy: {
        remoteUrl: 'https://repo123.net',
        contentMaxAge: -1,
        metadataMaxAge: 1440
      },
      negativeCache: {
        enabled: true,
        timeToLive: 1440
      },
      httpClient: {
        blocked: false,
        autoBlock: true,
        connection: {
          retries: '3',
          userAgentSuffix: 'user-agent-suffix-1',
          timeout: '100',
          enableCircularRedirects: true,
          enableCookies: true
        },
        authentication: {
          type: 'ntlm',
          username: 'user1',
          password: 'pass1',
          ntlmHost: 'ntlmhost1',
          ntlmDomain: 'ntlm.domain'
        }
      },
      replication: {
        preemptivePullEnabled: false,
        assetPathRegex: ''
      },
      maven: {
        contentDisposition: 'INLINE',
        layoutPolicy: 'STRICT',
        versionPolicy: 'RELEASE'
      }
    };

    const url = getSaveUrl(data);

    it('renders the form and shows/hides auth fields when type is proxy', async () => {
      await renderViewAndSetRequiredFields(data);

      expect(selectors.getRemoteUrlInput()).toBeInTheDocument();
      expect(selectors.getRoutingRuleSelect()).toBeInTheDocument();
      expect(selectors.getAuthTypeSelect()).toBeInTheDocument();

      expect(selectors.getGroupSectionTitle()).not.toBeInTheDocument();
      expect(selectors.getDeploymentPolicySelect()).not.toBeInTheDocument();

      expect(selectors.getUsernameInput()).not.toBeInTheDocument();
      expect(selectors.getPasswordInput()).not.toBeInTheDocument();
      expect(selectors.getNtlmHostInput()).not.toBeInTheDocument();
      expect(selectors.getNtlmDomainInput()).not.toBeInTheDocument();

      await TestUtils.changeField(selectors.getAuthTypeSelect, 'username');

      expect(selectors.getUsernameInput()).toBeInTheDocument();
      expect(selectors.getUsernameInput()).toBeInTheDocument();
      expect(selectors.getNtlmHostInput()).not.toBeInTheDocument();
      expect(selectors.getNtlmDomainInput()).not.toBeInTheDocument();

      await TestUtils.changeField(selectors.getAuthTypeSelect, 'ntlm');

      expect(selectors.getUsernameInput()).toBeInTheDocument();
      expect(selectors.getUsernameInput()).toBeInTheDocument();
      expect(selectors.getNtlmHostInput()).toBeInTheDocument();
      expect(selectors.getNtlmDomainInput()).toBeInTheDocument();
    });

    it('expands/collapses HTTP Request section properly', async function () {
      const repo1 = {
        ...genericDefaultValues.proxy,
        name: 'raw-proxy',
        format: 'raw',
        type: 'proxy',
        url: 'http://localhost:8081/repository/raw-proxy'
      };

      const repo2 = mergeDeepRight(repo1, {
        httpClient: {
          connection: {
            retries: 3
          }
        }
      });

      when(Axios.get).calledWith(getRepositoryUrl(repo1.name)).mockResolvedValueOnce({
        data: repo1
      });

      renderView('raw-proxy');
      await waitForElementToBeRemoved(selectors.queryLoadingMask());

      expect(selectors.getRetriesInput()).not.toBeVisible();

      cleanup();

      when(Axios.get).calledWith(getRepositoryUrl(repo2.name)).mockResolvedValueOnce({
        data: repo2
      });

      renderView('raw-proxy');
      await waitForElementToBeRemoved(selectors.queryLoadingMask());

      expect(selectors.getRetriesInput()).toBeVisible();
    });

    it('creates proxy repository', async () => {
      const repo = {
        format: 'p2',
        type: 'proxy',
        name: 'p2-proxy-1',
        online: true,
        routingRule: 'routing-rule-1',
        storage: {
          blobStoreName: 'blob-store-1',
          strictContentTypeValidation: true
        },
        cleanup: null,
        proxy: {
          remoteUrl: 'https://repo123.net',
          contentMaxAge: '600',
          metadataMaxAge: '700'
        },
        negativeCache: {
          enabled: true,
          timeToLive: '800'
        },
        httpClient: {
          blocked: true,
          autoBlock: false,
          connection: null,
          authentication: null
        },
        replication: {
          preemptivePullEnabled: false,
          assetPathRegex: ''
        }
      };

      await renderViewAndSetRequiredFields(repo);

      await TestUtils.changeField(selectors.getBlobStoreSelect, repo.storage.blobStoreName);
      await TestUtils.changeField(selectors.getRemoteUrlInput, repo.proxy.remoteUrl);
      await TestUtils.changeField(selectors.getRoutingRuleSelect, repo.routingRule);
      await TestUtils.changeField(selectors.getContentMaxAgeInput, repo.proxy.contentMaxAge);
      await TestUtils.changeField(selectors.getMetadataMaxAgeInput, repo.proxy.metadataMaxAge);
      await TestUtils.changeField(selectors.getTimeToLiveInput, repo.negativeCache.timeToLive);

      userEvent.click(selectors.getBlockedCheckbox());
      userEvent.click(selectors.getAutoBlockCheckbox());

      userEvent.click(selectors.getCreateButton());

      await waitFor(() => expect(Axios.post).toHaveBeenCalledWith(getSaveUrl(repo), repo));
    });

    it('creates proxy repository with authentication type settings', async () => {
      const repo = mergeDeepRight(data, {
        httpClient: {
          connection: null,
          authentication: {
            preemptive: true
          }
        }
      });

      await renderViewAndSetRequiredFields(repo);

      await TestUtils.changeField(
        selectors.getRemoteUrlInput,
        repo.proxy.remoteUrl.replace('https', 'http')
      );
      await TestUtils.changeField(selectors.getBlobStoreSelect, repo.storage.blobStoreName);

      expect(selectors.getPreemptiveAuthCheckbox()).not.toBeInTheDocument();

      await TestUtils.changeField(selectors.getAuthTypeSelect, repo.httpClient.authentication.type);

      expect(selectors.getPreemptiveAuthCheckbox()).toBeDisabled();

      await TestUtils.changeField(selectors.getRemoteUrlInput, repo.proxy.remoteUrl);

      expect(selectors.getPreemptiveAuthCheckbox()).toBeEnabled();
      userEvent.click(selectors.getPreemptiveAuthCheckbox());

      await TestUtils.changeField(
        selectors.getUsernameInput,
        repo.httpClient.authentication.username
      );
      await TestUtils.changeField(
        selectors.getPasswordInput,
        repo.httpClient.authentication.password
      );
      await TestUtils.changeField(
        selectors.getNtlmHostInput,
        repo.httpClient.authentication.ntlmHost
      );
      await TestUtils.changeField(
        selectors.getNtlmDomainInput,
        repo.httpClient.authentication.ntlmDomain
      );

      userEvent.click(selectors.getCreateButton());

      await waitFor(() => expect(Axios.post).toHaveBeenCalledWith(url, repo));
    });

    it('creates proxy repository with http connection settings', async () => {
      const repo = mergeDeepRight(data, {httpClient: {authentication: null}});

      await renderViewAndSetRequiredFields(repo);

      await TestUtils.changeField(selectors.getBlobStoreSelect, repo.storage.blobStoreName);
      await TestUtils.changeField(selectors.getRemoteUrlInput, repo.proxy.remoteUrl);

      await TestUtils.changeField(
        selectors.getUserAgentSuffixInput,
        repo.httpClient.connection.userAgentSuffix
      );
      await TestUtils.changeField(selectors.getRetriesInput, repo.httpClient.connection.retries);
      await TestUtils.changeField(selectors.getTimeoutInput, repo.httpClient.connection.timeout);

      userEvent.click(selectors.getRedirectsCheckbox());
      userEvent.click(selectors.getCookiesCheckbox());

      userEvent.click(selectors.getCreateButton());

      await waitFor(() => expect(Axios.post).toHaveBeenCalledWith(url, repo));
    });

    it('edits raw proxy repositories', async function () {
      const repo = {
        format: 'raw',
        type: 'proxy',
        name: 'raw-proxy',
        url: 'http://localhost:8081/repository/raw-proxy',
        online: true,
        storage: {
          blobStoreName: 'default',
          strictContentTypeValidation: true
        },
        cleanup: {
          policyNames: []
        },
        proxy: {
          remoteUrl: 'http://example.com',
          contentMaxAge: 1440,
          metadataMaxAge: 1440
        },
        negativeCache: {
          enabled: true,
          timeToLive: 1440
        },
        httpClient: {
          blocked: false,
          autoBlock: true,
          connection: {
            retries: 0,
            userAgentSuffix: '',
            timeout: 60,
            enableCircularRedirects: false,
            enableCookies: false,
            useTrustStore: false
          },
          authentication: null
        },
        routingRuleName: null
      };
      when(Axios.get).calledWith(getRepositoryUrl(repo.name)).mockResolvedValueOnce({
        data: repo
      });

      renderView('raw-proxy');

      await waitForElementToBeRemoved(selectors.queryLoadingMask());

      await TestUtils.changeField(selectors.getRemoteUrlInput, 'http://other.com');

      userEvent.click(selectors.querySubmitButton());

      expect(Axios.put).toBeCalledWith(
        REST_PUB_URL + 'raw/proxy/raw-proxy',
        mergeDeepRight(repo, {
          proxy: {
            remoteUrl: 'http://other.com'
          },
          routingRule: null
        })
      );
    });

    it('adds or removes patterns to the foreign layer url white list', async () => {
      const repo = {
        name: "docker-proxy",
        url: "http://localhost:8081/repository/docker-proxy",
        online: true,
        storage: {
          blobStoreName: "default",
          strictContentTypeValidation: true,
          writePolicy: "ALLOW"
        },
        cleanup: null,
        docker: {
          v1Enabled: false,
          forceBasicAuth: true,
          httpPort: null,
          httpsPort: null,
          subdomain: null
        },
        dockerProxy: {
          indexType: "REGISTRY",
          indexUrl: null,
          cacheForeignLayers: false,
          foreignLayerUrlWhitelist: []
        },
        proxy: {
          remoteUrl: "https://test.com",
          contentMaxAge: 1440,
          metadataMaxAge: 1440
        },
        negativeCache: {
          enabled: true,
          timeToLive: 1440
        },
        httpClient: {
          blocked: false,
          autoBlock: true,
          connection: {
            retries: null,
            userAgentSuffix: null,
            timeout: null,
            enableCircularRedirects: false,
            enableCookies: false,
            useTrustStore: false
          },
          authentication: null
        },
        routingRuleName: null,
        format: "docker",
        type: "proxy"
      }

      const patternUrl = '.test.*';
      const defaultPatternUrl = '.*';

      when(Axios.get).calledWith(getRepositoryUrl(repo.name)).mockResolvedValueOnce({
        data: repo
      });

      const {container} = renderView(repo.name);

      await waitForElementToBeRemoved(selectors.queryLoadingMask());

      expect(selectors.getForeignLayerCheckbox()).toBeInTheDocument();
      expect(selectors.getForeignLayerCheckbox()).not.toBeChecked();

      await userEvent.click(selectors.getForeignLayerCheckbox());

      expect(selectors.getForeignLayerCheckbox()).toBeChecked();
      expect(screen.getByText(defaultPatternUrl)).toBeInTheDocument();

      await TestUtils.changeField(selectors.getForeignLayerInput, patternUrl);

      await userEvent.click(selectors.getForeignLayerAddButton(container));

      expect(screen.getByText(patternUrl)).toBeInTheDocument();

      await userEvent.click(selectors.getForeignLayerRemoveButton(defaultPatternUrl));

      expect(screen.queryByText(defaultPatternUrl)).not.toBeInTheDocument();
      expect(selectors.getForeignLayerRemoveButton(patternUrl)).toHaveClass('disabled');

      await userEvent.click(selectors.querySubmitButton());

      expect(Axios.put).toBeCalledWith(
        `${REST_PUB_URL}docker/proxy/${repo.name}`,
        mergeDeepRight(repo, {
          dockerProxy: {
            cacheForeignLayers: true,
            foreignLayer: "",
            foreignLayerUrlWhitelist:[patternUrl]
          },
          routingRule: null
        })
      );
    });

    it('renders npm proxy fields with correct default values', async () => {
      await renderViewAndSetRequiredFields({format: 'npm', type: 'proxy'});
      expect(selectors.getRemoveQuarantinedCheckbox()).not.toBeChecked();
    });

    it('renders maven proxy fields with correct default values', async () => {
      await renderViewAndSetRequiredFields({format: 'maven2', type: 'proxy'});
      validateSelect(selectors.getContentDispositionSelect(), null, 'INLINE');
      validateSelect(selectors.getLayoutPolicySelect(), null, 'STRICT');
      validateSelect(selectors.getVersionPolicySelect(), null, 'RELEASE');
    });

    it('creates docker proxy repository', async () => {
      when(global.NX.Permissions.check)
        .calledWith('nexus:ssl-truststore:read')
        .mockReturnValue(true);
      when(global.NX.Permissions.check)
        .calledWith('nexus:ssl-truststore:create')
        .mockReturnValue(true);
      when(global.NX.Permissions.check)
        .calledWith('nexus:ssl-truststore:update')
        .mockReturnValue(true);
      const repo = {
        format: 'docker',
        type: 'proxy',
        name: 'docker-proxy-2',
        online: true,
        routingRule: '',
        storage: {
          blobStoreName: 'default',
          strictContentTypeValidation: true
        },
        cleanup: null,
        proxy: {
          remoteUrl: 'https://foo.bar',
          contentMaxAge: 1440,
          metadataMaxAge: 1440
        },
        negativeCache: {
          enabled: true,
          timeToLive: 1440
        },
        httpClient: {
          blocked: false,
          autoBlock: true,
          connection: {
            useTrustStore: true
          },
          authentication: null
        },
        replication: {
          preemptivePullEnabled: false,
          assetPathRegex: ''
        },
        docker: {
          httpPort: null,
          httpsPort: null,
          forceBasicAuth: false,
          v1Enabled: false,
          subdomain: 'docker-proxy-2'
        },
        dockerProxy: {
          indexType: 'CUSTOM',
          indexUrl: 'https://custom.index.com/',
          cacheForeignLayers: false,
          foreignLayerUrlWhitelist: []
        }
      };
      const certificateDetails = {
        'expiresOn': 1654300799000,
        'fingerprint': 'C2:56:90:5E:91:65:A5:D1:6E:DC:98:65:CD:8D:34:32:B2:B1:45:40',
        'id': 'C2:56:90:5E:91:65:A5:D1:6E:DC:98:65:CD:8D:34:32:B2:B1:45:40',
        'issuedOn': 1622764800000,
        'issuerCommonName': 'issuer common name',
        'issuerOrganization': 'issuer organization',
        'issuerOrganizationalUnit': 'issuer unit',
        'pem': '-----BEGIN CERTIFICATE-----\ncertificate_text\n-----END CERTIFICATE-----\n',
        'serialNumber': '8987777501424561459122707745365601310',
        'subjectCommonName': 'subject common name',
        'subjectOrganization': 'subject organization',
        'subjectOrganizationalUnit': 'subject organizational unit'
      };

      when(Axios.get)
        .calledWith(CERTIFICATE_DETAILS)
        .mockResolvedValue({data: certificateDetails});
      when(Axios.get)
        .calledWith(SSL_CERTIFICATES)
        .mockResolvedValue({data: [certificateDetails]});

      await renderViewAndSetRequiredFields(repo);

      await TestUtils.changeField(selectors.getBlobStoreSelect, repo.storage.blobStoreName);

      await TestUtils.changeField(
        selectors.getRemoteUrlInput,
        repo.proxy.remoteUrl.replace('https', 'http')
      );

      expect(selectors.getUseNexusTruststoreCheckbox()).toBeDisabled();
      expect(selectors.getUseNexusTruststoreButton()).toHaveClass('disabled');
      expect(selectors.getUseNexusTruststoreButton()).toHaveAttribute('aria-disabled', 'true');
      await TestUtils.changeField(selectors.getRemoteUrlInput, repo.proxy.remoteUrl);
      userEvent.click(selectors.getUseNexusTruststoreCheckbox());
      userEvent.click(selectors.getUseNexusTruststoreButton());
      userEvent.click(selectors.getCloseCertificateButton());

      const indexRadioButtons = selectors.getDockerIndexRadioButtons();
      expect(indexRadioButtons.registry).toBeChecked();
      expect(selectors.getDockerIndexUrlInput()).not.toBeInTheDocument();
      userEvent.click(indexRadioButtons.hub);
      expect(selectors.getDockerIndexUrlInput()).toBeDisabled();
      expect(selectors.getDockerIndexUrlInput()).toHaveValue(DOCKER_HUB_URL);
      userEvent.click(indexRadioButtons.custom);
      await TestUtils.changeField(selectors.getDockerIndexUrlInput, repo.dockerProxy.indexUrl);
      userEvent.click(selectors.getDockerSubdomainCheckbox());

      userEvent.click(selectors.getCreateButton());

      await waitFor(() => expect(Axios.post).toHaveBeenCalledWith(getSaveUrl(repo), repo));
    });

    it('creates apt proxy repository', async () => {
      const repo = {
        format: 'apt',
        type: 'proxy',
        name: 'apt-proxy-1',
        online: true,
        routingRule: '',
        storage: {
          blobStoreName: 'default',
          strictContentTypeValidation: true
        },
        cleanup: null,
        proxy: {
          remoteUrl: 'https://foo.bar',
          contentMaxAge: 1440,
          metadataMaxAge: 1440
        },
        negativeCache: {
          enabled: true,
          timeToLive: 1440
        },
        httpClient: {
          blocked: false,
          autoBlock: true,
          authentication: null,
          connection: null
        },
        replication: {
          preemptivePullEnabled: false,
          assetPathRegex: ''
        },
        apt: {
          distribution: 'bionic',
          flat: true
        }
      };

      await renderViewAndSetRequiredFields(repo);

      await TestUtils.changeField(selectors.getRemoteUrlInput, repo.proxy.remoteUrl);
      await TestUtils.changeField(selectors.getBlobStoreSelect, repo.storage.blobStoreName);

      await TestUtils.changeField(selectors.getAptDistributionInput, repo.apt.distribution);
      userEvent.click(selectors.getAptFlatCheckbox());

      userEvent.click(selectors.getCreateButton());

      await waitFor(() => expect(Axios.post).toHaveBeenCalledWith(getSaveUrl(repo), repo));
    });
  });

  describe('group', () => {
    it('renders the form and populates dropdowns when type is group', async () => {
      const format = 'maven2';

      renderView();

      await waitForElementToBeRemoved(selectors.queryLoadingMask());

      validateSelect(selectors.getFormatSelect(), FORMAT_OPTIONS);

      expect(selectors.getTypeSelect()).toBeDisabled();

      await TestUtils.changeField(selectors.getFormatSelect, format);

      expect(selectors.getTypeSelect()).toBeEnabled();

      validateSelect(selectors.getTypeSelect(), TYPE_OPTIONS);

      await TestUtils.changeField(selectors.getTypeSelect, 'group');

      await waitFor(() => validateSelect(selectors.getBlobStoreSelect(), BLOB_STORES_OPTIONS, ''));

      MAVEN_REPOS_RESPONSE.forEach((repo) => {
        expect(selectors.getTransferListOption(repo.name)).toBeInTheDocument();
      });

      expect(selectors.getContentValidationCheckbox()).not.toBeInTheDocument();
      expect(selectors.getHostedSectionTitle()).not.toBeInTheDocument();
      expect(selectors.getCleanupSectionTitle()).not.toBeInTheDocument();
    });

    it('creates group repository', async () => {
      const repo = {
        format: 'maven2',
        type: 'group',
        name: 'maven-group-1',
        online: false,
        storage: {
          blobStoreName: 'blob-store-1',
          strictContentTypeValidation: true
        },
        group: {
          memberNames: ['maven-releases', 'maven-snapshots']
        },
        maven: {
          contentDisposition: 'INLINE',
          layoutPolicy: 'STRICT',
          versionPolicy: 'RELEASE'
        }
      };

      await renderViewAndSetRequiredFields(repo);

      await TestUtils.changeField(selectors.getNameInput, repo.name);

      userEvent.click(selectors.getStatusCheckbox());
      expect(selectors.getStatusCheckbox()).not.toBeChecked();

      await TestUtils.changeField(selectors.getBlobStoreSelect, repo.storage.blobStoreName);

      userEvent.click(selectors.getTransferListOption(repo.group.memberNames[0]));
      userEvent.click(selectors.getTransferListOption(repo.group.memberNames[1]));

      userEvent.click(selectors.getCreateButton());
      expect(selectors.querySavingMask()).toBeInTheDocument();
      expect(Axios.post).toHaveBeenCalledWith(getSaveUrl(repo), repo);
    });

    it('edits raw group repositories', async function () {
      const repo = {
        name: 'raw-group',
        format: 'raw',
        url: 'http://localhost:8081/repository/raw-group',
        online: true,
        storage: {
          blobStoreName: 'default',
          strictContentTypeValidation: true
        },
        group: {
          memberNames: ['raw-hosted', 'raw-proxy']
        },
        type: 'group'
      };

      when(Axios.get).calledWith(getRepositoryUrl(repo.name)).mockResolvedValueOnce({
        data: repo
      });

      renderView('raw-group');

      await waitForElementToBeRemoved(selectors.queryLoadingMask());

      userEvent.click(screen.getByText('raw-hosted'));

      userEvent.click(selectors.querySubmitButton());

      expect(Axios.put).toBeCalledWith(
        REST_PUB_URL + 'raw/group/raw-group',
        mergeDeepRight(repo, {
          group: {
            memberNames: ['raw-proxy']
          },
          routingRule: undefined
        })
      );
    });

    it('creates docker group repository', async () => {
      const repo = {
        format: 'docker',
        type: 'group',
        name: 'docker-group-1',
        online: true,
        storage: {
          blobStoreName: 'default',
          strictContentTypeValidation: true
        },
        group: {
          memberNames: ['docker-proxy-1', 'docker-hosted-1'],
          writableMember: 'docker-hosted-1'
        },
        docker: {
          v1Enabled: true,
          forceBasicAuth: true,
          httpPort: '333',
          httpsPort: '444',
          subdomain: 'docker-sub-domain'
        }
      };

      await renderViewAndSetRequiredFields(repo);

      await TestUtils.changeField(selectors.getBlobStoreSelect, repo.storage.blobStoreName);

      expect(selectors.getDockerConnectorHttpPortInput()).toBeDisabled();
      expect(selectors.getDockerConnectorHttpsPortInput()).toBeDisabled();
      expect(selectors.getDockerSubdomainInput()).toBeDisabled();

      userEvent.click(selectors.getDockerConnectorHttpPortCheckbox());
      userEvent.click(selectors.getDockerConnectorHttpsPortCheckbox());
      userEvent.click(selectors.getDockerSubdomainCheckbox());

      expect(selectors.getDockerConnectorHttpPortInput()).toBeEnabled();
      expect(selectors.getDockerConnectorHttpsPortInput()).toBeEnabled();
      expect(selectors.getDockerSubdomainInput()).toBeEnabled();
      expect(selectors.getDockerSubdomainInput()).toHaveValue(repo.name);

      await TestUtils.changeField(selectors.getDockerConnectorHttpPortInput, '1111');
      await TestUtils.changeField(selectors.getDockerConnectorHttpsPortInput, '1111');
      expect(selectors.getDockerConnectorSamePortsError()).toHaveLength(2);

      await TestUtils.changeField(selectors.getDockerConnectorHttpPortInput, repo.docker.httpPort);
      await TestUtils.changeField(selectors.getDockerConnectorHttpsPortInput, repo.docker.httpsPort);
      userEvent.click(selectors.getDockerConnectorHttpPortCheckbox());
      userEvent.click(selectors.getDockerConnectorHttpsPortCheckbox());
      expect(selectors.getDockerConnectorHttpPortInput()).toHaveValue('');
      expect(selectors.getDockerConnectorHttpsPortInput()).toHaveValue('');
      userEvent.click(selectors.getDockerConnectorHttpPortCheckbox());
      userEvent.click(selectors.getDockerConnectorHttpsPortCheckbox());
      expect(selectors.getDockerConnectorHttpPortInput()).toHaveValue(repo.docker.httpPort);
      expect(selectors.getDockerConnectorHttpsPortInput()).toHaveValue(repo.docker.httpsPort);

      await TestUtils.changeField(selectors.getDockerSubdomainInput, repo.docker.subdomain);

      userEvent.click(selectors.getDockerApiVersionCheckbox());
      userEvent.click(selectors.getDockerAnonimousPullCheckbox());

      userEvent.click(selectors.getTransferListOption(repo.group.memberNames[0]));

      expect(selectors.getDockerWritableRepositorySelect()).toBeDisabled();

      userEvent.click(selectors.getTransferListOption(repo.group.memberNames[1]));

      expect(selectors.getDockerWritableRepositorySelect()).toBeEnabled();

      await TestUtils.changeField(
        selectors.getDockerWritableRepositorySelect,
        repo.group.writableMember
      );

      userEvent.click(selectors.getCreateButton());

      await waitFor(() => expect(Axios.post).toHaveBeenCalledWith(getSaveUrl(repo), repo));
    });

    it('invalidates form when docker writable repository is not a group member', async () => {
      const repo = {
        format: 'docker',
        type: 'group',
        name: 'docker-group-1',
        online: true,
        storage: {
          blobStoreName: 'default',
          strictContentTypeValidation: true
        },
        group: {
          memberNames: ['docker-proxy-1', 'docker-hosted-1'],
          writableMember: 'docker-hosted-ghost'
        },
        docker: {
          v1Enabled: false,
          forceBasicAuth: false,
          httpPort: null,
          httpsPort: null,
          subdomain: null
        }
      };

      when(Axios.get).calledWith(getRepositoryUrl(repo.name)).mockResolvedValueOnce({
        data: repo
      });

      const consoleErrorSpy = jest.spyOn(global.console, 'error');

      renderView(repo.name);

      await waitForElementToBeRemoved(selectors.queryLoadingMask());

      userEvent.click(selectors.getDockerApiVersionCheckbox());

      expect(consoleErrorSpy).toHaveBeenCalledWith(
        EDITOR.WRITABLE.VALIDATION_ERROR(repo.group.writableMember)
      );

      userEvent.click(selectors.querySubmitButton());
      expect(selectors.queryFormError(TestUtils.VALIDATION_ERRORS_MESSAGE)).toBeInTheDocument();

      await TestUtils.changeField(
        selectors.getDockerWritableRepositorySelect,
        repo.group.memberNames[1]
      );

      expect(selectors.queryFormError()).not.toBeInTheDocument();
    });
  });

  describe('delete', () => {
    const repo = {
      name: 'repo',
      format: 'raw',
      type: 'hosted'
    };

    it('does not display delete button in create mode', async () => {
      renderView();
      await waitForElementToBeRemoved(selectors.queryLoadingMask());

      expect(selectors.getDeleteButton()).not.toBeInTheDocument();
    });

    it('disables delete button when permission check is unsatisfied', async () => {
      when(Axios.get).calledWith(getRepositoryUrl(repo.name)).mockResolvedValueOnce({
        data: repo
      });

      when(ExtJS.checkPermission)
        .calledWith(`nexus:repository-admin:${repo.format}:${repo.name}:delete`)
        .mockReturnValue(false);

      renderView('repo');
      await waitForElementToBeRemoved(selectors.queryLoadingMask());

      expect(selectors.getDeleteButton()).toHaveClass('disabled');
    });

    it('deletes repository when user has permissions', async () => {
      when(Axios.get).calledWith(getRepositoryUrl(repo.name)).mockResolvedValueOnce({
        data: repo
      });

      when(ExtJS.checkPermission)
        .calledWith(`nexus:repository-admin:${repo.format}:${repo.name}:delete`)
        .mockReturnValue(true);

      ExtJS.requestConfirmation.mockReturnValue(Promise.resolve());

      renderView('repo');
      await waitForElementToBeRemoved(selectors.queryLoadingMask());

      expect(selectors.getDeleteButton()).toBeEnabled();

      userEvent.click(selectors.getDeleteButton());

      await waitFor(() =>
        expect(Axios.delete).toHaveBeenCalledWith(deleteRepositoryUrl(repo.name))
      );
    });
  });
});

const validateSelect = (selectElement, options, value) => {
  expect(selectElement).toBeInTheDocument();
  if (options) {
    options.forEach((option) => {
      expect(getByRole(selectElement, 'option', option)).toBeInTheDocument();
    });
    expect(getAllByRole(selectElement, 'option')).toHaveLength(options.length);
  }
  value && expect(selectElement).toHaveValue(value);
};

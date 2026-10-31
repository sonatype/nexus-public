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
import {render, screen, fireEvent, waitFor, act} from '@testing-library/react';

import {ExtJS, ListMachineUtils} from '@sonatype/nexus-ui-plugin';

import UIStrings from '../../../../constants/UIStrings';
import ServiceAccountTokens from './ServiceAccountTokens';
import ServiceAccountTokensCreateModal from './ServiceAccountTokensCreateModal';
import ServiceAccountTokensTokenModal from './ServiceAccountTokensTokenModal';
import ServiceAccountTokensRevokeModal from './ServiceAccountTokensRevokeModal';

jest.mock('axios', () => ({
  get: jest.fn(),
  post: jest.fn(),
  delete: jest.fn(),
}));

const LABELS = UIStrings.SERVICE_ACCOUNT_TOKENS;

const MOCK_ROLES = [
  {id: 'nx-admin', name: 'nx-admin'},
  {id: 'nx-deployer', name: 'nx-deployer'},
];

beforeEach(() => {
  Axios.get.mockImplementation((url) => {
    if (url.includes('roles')) {
      return Promise.resolve({data: MOCK_ROLES});
    }
    return Promise.resolve({data: []});
  });
  Axios.post.mockResolvedValue({data: {}});
  Axios.delete.mockResolvedValue({data: {}});
  jest.spyOn(ExtJS, 'checkPermission').mockReturnValue(true);
});

afterEach(() => {
  jest.restoreAllMocks();
  jest.clearAllMocks();
});

const fillRequiredFields = (name = 'svc') => {
  fireEvent.change(screen.getByRole('textbox', {name: /Name/i}), {target: {value: name}});
  const roleSelect = screen.getByRole('combobox', {name: /Role/i});
  const firstRole = roleSelect.querySelector('option:not([value=""])');
  fireEvent.change(roleSelect, {target: {value: firstRole.value}});
};

describe('ServiceAccountTokensListMachine filter predicate', () => {
  const tokens = [
    {id: '1', name: 'jenkins-token', roleId: 'nx-deployer', createdBy: 'admin'},
    {id: '2', name: 'github-actions', roleId: 'nx-readonly', createdBy: 'user1'},
  ];

  const filterBy = (filter) =>
    tokens.filter(({name, roleId, createdBy}) =>
      ListMachineUtils.hasAnyMatches([name, roleId, createdBy], filter)
    );

  it('filters by name', () => {
    expect(filterBy('jenkins')).toHaveLength(1);
  });

  it('filters by roleId', () => {
    expect(filterBy('deployer')).toHaveLength(1);
  });

  it('filters by createdBy', () => {
    expect(filterBy('user1')).toHaveLength(1);
  });

  it('does not filter by description (dropped)', () => {
    const withDescription = [
      {id: '1', name: 'token-a', roleId: 'nx-role', createdBy: 'admin', description: 'special CI token'},
    ];
    const result = withDescription.filter(({name, roleId, createdBy}) =>
      ListMachineUtils.hasAnyMatches([name, roleId, createdBy], 'special CI')
    );
    expect(result).toHaveLength(0);
  });
});

describe('ServiceAccountTokens page', () => {
  it('renders the empty state message when there are no tokens', async () => {
    render(<ServiceAccountTokens />);

    expect(await screen.findByText(LABELS.LIST.EMPTY_LIST)).toBeInTheDocument();
  });

  it('renders the filtered empty state message when the filter matches nothing', async () => {
    render(<ServiceAccountTokens />);
    await screen.findByText(LABELS.LIST.EMPTY_LIST);

    fireEvent.change(screen.getByPlaceholderText(LABELS.LIST.FILTER_PLACEHOLDER), {
      target: {value: 'no-such-token'},
    });

    expect(await screen.findByText(LABELS.LIST.EMPTY_LIST_FILTERED)).toBeInTheDocument();
  });
  it('renders about alert with external docs link', async () => {
    render(<ServiceAccountTokens />);

    const learnMoreLink = await screen.findByText(LABELS.LIST.ABOUT.LINK);
    expect(learnMoreLink.closest('a')).toHaveAttribute(
      'href',
      'https://links.sonatype.com/products/nxrm3/docs/service-account-tokens'
    );
    expect(learnMoreLink.closest('a')).toHaveAttribute('target', '_blank');
  });

  it('renders filter with correct placeholder', async () => {
    render(<ServiceAccountTokens />);

    await waitFor(() => {
      expect(screen.getByPlaceholderText(LABELS.LIST.FILTER_PLACEHOLDER)).toBeInTheDocument();
    });
  });

  it('truncates description in the table when longer than 50 characters', async () => {
    const longDescription = 'This description is intentionally long enough to exceed the threshold';
    Axios.get.mockImplementation((url) => {
      if (url.includes('roles')) return Promise.resolve({data: MOCK_ROLES});
      return Promise.resolve({
        data: [{id: '1', name: 'long-desc', roleId: 'nx-admin', description: longDescription, createdBy: 'admin'}],
      });
    });

    render(<ServiceAccountTokens />);

    const descSpan = await screen.findByText(longDescription);
    expect(descSpan).toHaveClass('nxrm-sa-token-description--truncated');
  });

  it('does not truncate description in the table when 50 characters or fewer', async () => {
    const shortDescription = 'A brief note';
    Axios.get.mockImplementation((url) => {
      if (url.includes('roles')) return Promise.resolve({data: MOCK_ROLES});
      return Promise.resolve({
        data: [{id: '2', name: 'short-desc', roleId: 'nx-admin', description: shortDescription, createdBy: 'admin'}],
      });
    });

    render(<ServiceAccountTokens />);

    const descSpan = await screen.findByText(shortDescription);
    expect(descSpan).not.toHaveClass('nxrm-sa-token-description--truncated');
  });
});

describe('ServiceAccountTokensCreateModal', () => {
  it('renders the expiry options', async () => {
    render(<ServiceAccountTokensCreateModal onClose={jest.fn()} onCreate={jest.fn()} roles={MOCK_ROLES} />);

    await waitFor(() => {
      expect(screen.getByText('30 days')).toBeInTheDocument();
    });

    expect(screen.getByText('60 days')).toBeInTheDocument();
    expect(screen.getByText('90 days')).toBeInTheDocument();
    expect(screen.getByText('1 year')).toBeInTheDocument();
    expect(screen.getByText('Never')).toBeInTheDocument();
  });

  it('moves focus to the never-expires warning so screen readers announce it', async () => {
    render(<ServiceAccountTokensCreateModal onClose={jest.fn()} onCreate={jest.fn()} roles={MOCK_ROLES} />);

    const expirationSelect = await screen.findByRole('combobox', {name: /Token expiration/i});
    fireEvent.change(expirationSelect, {target: {value: '-1'}});

    await waitFor(() => {
      const warning = screen.getByText(LABELS.CREATE_MODAL.NEVER_EXPIRES_WARNING);
      const wrapper = warning.closest('[tabindex="-1"]');
      expect(wrapper).toBeInTheDocument();
      expect(wrapper).toHaveFocus();
    });
  });

  it('does not show never-expires warning by default', async () => {
    render(<ServiceAccountTokensCreateModal onClose={jest.fn()} onCreate={jest.fn()} roles={MOCK_ROLES} />);

    await waitFor(() => {
      expect(screen.getByText('30 days')).toBeInTheDocument();
    });

    expect(screen.queryByText(LABELS.CREATE_MODAL.NEVER_EXPIRES_WARNING)).not.toBeInTheDocument();
  });

  it('omits expirationDays from the payload when never is selected', async () => {
    const onCreate = jest.fn();
    render(<ServiceAccountTokensCreateModal onClose={jest.fn()} onCreate={onCreate} roles={MOCK_ROLES} />);

    fireEvent.change(await screen.findByRole('combobox', {name: /Token expiration/i}), {
      target: {value: '-1'},
    });
    fillRequiredFields();

    fireEvent.click(screen.getByRole('button', {name: LABELS.CREATE_MODAL.CREATE_BUTTON}));

    expect(onCreate).toHaveBeenCalledWith(
      expect.not.objectContaining({expirationDays: expect.anything()})
    );
  });

  it('includes expirationDays in the payload for non-Never options', async () => {
    const onCreate = jest.fn();
    render(<ServiceAccountTokensCreateModal onClose={jest.fn()} onCreate={onCreate} roles={MOCK_ROLES} />);

    fireEvent.change(await screen.findByRole('combobox', {name: /Token expiration/i}), {
      target: {value: '90'},
    });
    fillRequiredFields();

    fireEvent.click(screen.getByRole('button', {name: LABELS.CREATE_MODAL.CREATE_BUTTON}));

    expect(onCreate).toHaveBeenCalledWith(expect.objectContaining({expirationDays: 90}));
  });

  it('trims the description and includes it in the payload', async () => {
    const onCreate = jest.fn();
    render(<ServiceAccountTokensCreateModal onClose={jest.fn()} onCreate={onCreate} roles={MOCK_ROLES} />);

    await screen.findByRole('combobox', {name: /Token expiration/i});
    fillRequiredFields();
    fireEvent.change(screen.getByRole('textbox', {name: /Description/i}), {
      target: {value: '  CI build agent  '},
    });

    fireEvent.click(screen.getByRole('button', {name: LABELS.CREATE_MODAL.CREATE_BUTTON}));

    expect(onCreate).toHaveBeenCalledWith(expect.objectContaining({description: 'CI build agent'}));
  });

  it('shows duplicate-name error and blocks submit when name already exists', async () => {
    const onCreate = jest.fn();
    render(
      <ServiceAccountTokensCreateModal
        onClose={jest.fn()}
        onCreate={onCreate}
        roles={MOCK_ROLES}
        existingNames={['jenkins-prod']}
      />
    );

    await screen.findByRole('combobox', {name: /Token expiration/i});
    fireEvent.change(screen.getByRole('textbox', {name: /Name/i}), {target: {value: 'jenkins-prod'}});

    expect(
      screen.getByText(LABELS.CREATE_MODAL.NAME_DUPLICATE_ERROR('jenkins-prod'))
    ).toBeInTheDocument();

    const roleSelect = screen.getByRole('combobox', {name: /Role/i});
    const firstRole = roleSelect.querySelector('option:not([value=""])');
    fireEvent.change(roleSelect, {target: {value: firstRole.value}});

    fireEvent.click(screen.getByRole('button', {name: LABELS.CREATE_MODAL.CREATE_BUTTON}));
    expect(onCreate).not.toHaveBeenCalled();
  });

  it('shows pattern error and blocks submit when name contains spaces or special chars', async () => {
    const onCreate = jest.fn();
    render(<ServiceAccountTokensCreateModal onClose={jest.fn()} onCreate={onCreate} roles={MOCK_ROLES} />);

    await screen.findByRole('combobox', {name: /Token expiration/i});
    fireEvent.change(screen.getByRole('textbox', {name: /Name/i}), {target: {value: 'a cool token'}});

    expect(screen.getByText(LABELS.CREATE_MODAL.NAME_INVALID_CHARS_ERROR)).toBeInTheDocument();

    const roleSelect = screen.getByRole('combobox', {name: /Role/i});
    const firstRole = roleSelect.querySelector('option:not([value=""])');
    fireEvent.change(roleSelect, {target: {value: firstRole.value}});

    fireEvent.click(screen.getByRole('button', {name: LABELS.CREATE_MODAL.CREATE_BUTTON}));
    expect(onCreate).not.toHaveBeenCalled();
  });

  it('accepts names with letters, numbers, hyphens, and underscores', async () => {
    const onCreate = jest.fn();
    render(<ServiceAccountTokensCreateModal onClose={jest.fn()} onCreate={onCreate} roles={MOCK_ROLES} />);

    await screen.findByRole('combobox', {name: /Token expiration/i});
    fillRequiredFields('my-token_v2');

    fireEvent.click(screen.getByRole('button', {name: LABELS.CREATE_MODAL.CREATE_BUTTON}));
    expect(onCreate).toHaveBeenCalledWith(expect.objectContaining({name: 'my-token_v2'}));
  });

  it('caps description at 256 characters and renders the live counter', async () => {
    render(<ServiceAccountTokensCreateModal onClose={jest.fn()} onCreate={jest.fn()} roles={MOCK_ROLES} />);

    const descriptionInput = await screen.findByRole('textbox', {name: /Description/i});
    expect(descriptionInput).toHaveAttribute('maxLength', '256');

    fireEvent.change(descriptionInput, {target: {value: 'hello'}});
    expect(screen.getByText('5 / 256')).toBeInTheDocument();
  });

  it('does not call onCreate when name or role is empty', async () => {
    const onCreate = jest.fn();
    render(<ServiceAccountTokensCreateModal onClose={jest.fn()} onCreate={onCreate} roles={MOCK_ROLES} />);

    await screen.findByRole('combobox', {name: /Token expiration/i});

    // Name only — role still empty
    fireEvent.change(screen.getByRole('textbox', {name: /Name/i}), {target: {value: 'svc'}});
    fireEvent.click(screen.getByRole('button', {name: LABELS.CREATE_MODAL.CREATE_BUTTON}));
    expect(onCreate).not.toHaveBeenCalled();

    // Clear name, set role only
    fireEvent.change(screen.getByRole('textbox', {name: /Name/i}), {target: {value: ''}});
    const roleSelect = screen.getByRole('combobox', {name: /Role/i});
    const firstRole = roleSelect.querySelector('option:not([value=""])');
    fireEvent.change(roleSelect, {target: {value: firstRole.value}});
    fireEvent.click(screen.getByRole('button', {name: LABELS.CREATE_MODAL.CREATE_BUTTON}));
    expect(onCreate).not.toHaveBeenCalled();
  });

  it('renders the loading mask and disables cancel while isCreating', async () => {
    render(
      <ServiceAccountTokensCreateModal
        onClose={jest.fn()}
        onCreate={jest.fn()}
        roles={MOCK_ROLES}
        isCreating
      />
    );

    expect(await screen.findByText(LABELS.CREATE_MODAL.CREATING_MASK)).toBeInTheDocument();
    expect(screen.getByRole('button', {name: LABELS.CREATE_MODAL.CANCEL_BUTTON})).toBeDisabled();
  });
});

describe('ServiceAccountTokensTokenModal', () => {
  beforeEach(() => {
    jest.useFakeTimers();
  });

  afterEach(() => {
    jest.useRealTimers();
  });

  it('renders the warning copy', async () => {
    render(<ServiceAccountTokensTokenModal name="jenkins-prod" token="sat.test-token" onClose={jest.fn()} />);

    await waitFor(() => {
      expect(screen.getByText(LABELS.TOKEN_MODAL.WARNING)).toBeInTheDocument();
    });
  });

  it('renders the auto-close countdown in the footer', async () => {
    render(<ServiceAccountTokensTokenModal name="jenkins-prod" token="sat.test-token" onClose={jest.fn()} />);

    await waitFor(() => {
      expect(screen.getByText(LABELS.TOKEN_MODAL.AUTO_CLOSE_NOTICE(60))).toBeInTheDocument();
    });
  });

  it('warning has role="alert" for accessibility', async () => {
    render(<ServiceAccountTokensTokenModal name="jenkins-prod" token="sat.test-token" onClose={jest.fn()} />);

    await waitFor(() => {
      const warning = screen.getByText(LABELS.TOKEN_MODAL.WARNING);
      expect(warning.closest('[role="alert"]')).toBeInTheDocument();
    });
  });

  it('auto-closes when countdown reaches 0', async () => {
    const onClose = jest.fn();
    render(<ServiceAccountTokensTokenModal name="jenkins-prod" token="sat.test-token" onClose={onClose} />);

    await waitFor(() => {
      expect(screen.getByText(LABELS.TOKEN_MODAL.AUTO_CLOSE_NOTICE(60))).toBeInTheDocument();
    });

    act(() => {
      jest.advanceTimersByTime(60000);
    });

    expect(onClose).toHaveBeenCalled();
  });

  it('renders Basic username field with the account name', () => {
    render(<ServiceAccountTokensTokenModal name="jenkins-prod" token="sat.test-token" onClose={jest.fn()} />);
    expect(screen.getByTestId('sat-basic-username')).toHaveTextContent('jenkins-prod');
  });

  it('renders Basic password field with the token', () => {
    render(<ServiceAccountTokensTokenModal name="jenkins-prod" token="sat.test-token" onClose={jest.fn()} />);
    expect(screen.getByTestId('sat-basic-password')).toHaveTextContent('sat.test-token');
  });

  it('still renders the Bearer token value', () => {
    render(<ServiceAccountTokensTokenModal name="jenkins-prod" token="sat.test-token" onClose={jest.fn()} />);
    expect(screen.getByTestId('sat-token-value')).toHaveTextContent('sat.test-token');
  });

  it('clicking username copy writes the name to clipboard', async () => {
    const writeText = jest.fn().mockResolvedValue(undefined);
    Object.defineProperty(navigator, 'clipboard', {value: {writeText}, writable: true, configurable: true});

    render(<ServiceAccountTokensTokenModal name="jenkins-prod" token="sat.test-token" onClose={jest.fn()} />);
    fireEvent.click(screen.getByTestId('sat-basic-username-copy'));

    await act(async () => { await Promise.resolve(); });

    expect(writeText).toHaveBeenCalledWith('jenkins-prod');
  });

  it('clicking password copy writes the token to clipboard', async () => {
    const writeText = jest.fn().mockResolvedValue(undefined);
    Object.defineProperty(navigator, 'clipboard', {value: {writeText}, writable: true, configurable: true});

    render(<ServiceAccountTokensTokenModal name="jenkins-prod" token="sat.test-token" onClose={jest.fn()} />);
    fireEvent.click(screen.getByTestId('sat-basic-password-copy'));

    await act(async () => { await Promise.resolve(); });

    expect(writeText).toHaveBeenCalledWith('sat.test-token');
  });

  it('encoded value hidden initially', () => {
    render(<ServiceAccountTokensTokenModal name="jenkins-prod" token="sat.test-token" onClose={jest.fn()} />);
    expect(screen.queryByTestId('sat-basic-encoded-value')).toBeNull();
    expect(screen.getByTestId('sat-basic-encoded-reveal')).toBeInTheDocument();
  });

  it('after clicking reveal, shows encoded Authorization header value', () => {
    render(<ServiceAccountTokensTokenModal name="jenkins-prod" token="sat.test-token" onClose={jest.fn()} />);
    fireEvent.click(screen.getByTestId('sat-basic-encoded-reveal'));
    const expected = `Authorization: Basic ${btoa('jenkins-prod:sat.test-token')}`;
    expect(screen.getByTestId('sat-basic-encoded-value')).toHaveTextContent(expected);
  });

  it('AT-074 encoded-once: base64 decodes back to name:token', () => {
    render(<ServiceAccountTokensTokenModal name="jenkins-prod" token="sat.test-token" onClose={jest.fn()} />);
    fireEvent.click(screen.getByTestId('sat-basic-encoded-reveal'));
    const text = screen.getByTestId('sat-basic-encoded-value').textContent;
    const base64Part = text.replace('Authorization: Basic ', '');
    expect(atob(base64Part)).toBe('jenkins-prod:sat.test-token');
  });

  it('non-ASCII name: encodes correctly via UTF-8 round-trip', () => {
    const nonAsciiName = 'usér-café-名前';
    // Documents why safeBtoa is required: a bare btoa on these characters throws.
    // eslint-disable-next-line no-undef
    expect(() => btoa(nonAsciiName)).toThrow();
    render(<ServiceAccountTokensTokenModal name={nonAsciiName} token="sat.test-token" onClose={jest.fn()} />);
    fireEvent.click(screen.getByTestId('sat-basic-encoded-reveal'));
    const text = screen.getByTestId('sat-basic-encoded-value').textContent;
    const base64Part = text.replace('Authorization: Basic ', '');
    // eslint-disable-next-line no-undef
    const expected = btoa(unescape(encodeURIComponent(`${nonAsciiName}:sat.test-token`)));
    expect(base64Part).toBe(expected);
    expect(base64Part.length).toBeGreaterThan(0);
  });

  it('clicking encoded copy writes the full Authorization header to clipboard', async () => {
    const writeText = jest.fn().mockResolvedValue(undefined);
    Object.defineProperty(navigator, 'clipboard', {value: {writeText}, writable: true, configurable: true});

    render(<ServiceAccountTokensTokenModal name="jenkins-prod" token="sat.test-token" onClose={jest.fn()} />);
    fireEvent.click(screen.getByTestId('sat-basic-encoded-reveal'));
    fireEvent.click(screen.getByTestId('sat-basic-encoded-copy'));

    await act(async () => { await Promise.resolve(); });

    expect(writeText).toHaveBeenCalledWith(`Authorization: Basic ${btoa('jenkins-prod:sat.test-token')}`);
  });

  it('shows the copy-failure message when clipboard write rejects', async () => {
    const writeText = jest.fn().mockRejectedValue(new Error('denied'));
    Object.defineProperty(navigator, 'clipboard', {value: {writeText}, writable: true, configurable: true});

    render(<ServiceAccountTokensTokenModal name="jenkins-prod" token="sat.test-token" onClose={jest.fn()} />);
    fireEvent.click(screen.getByTestId('sat-basic-username-copy'));

    await act(async () => { await Promise.resolve(); });

    expect(screen.getByTestId('sa-token-copy-error')).toHaveTextContent(LABELS.TOKEN_MODAL.COPY_FAILED);
  });

  it('AT-072 docs link has correct href and text', () => {
    render(<ServiceAccountTokensTokenModal name="jenkins-prod" token="sat.test-token" onClose={jest.fn()} />);
    const link = screen.getByTestId('sat-docs-link');
    expect(link.tagName.toLowerCase()).toBe('a');
    expect(link).toHaveAttribute('href', LABELS.TOKEN_MODAL.DOCS_LINK_HREF);
    expect(link).toHaveTextContent(LABELS.TOKEN_MODAL.DOCS_LINK);
    expect(link).toHaveAttribute('target', '_blank');
    // NxTextLink `external` guards against reverse-tabnabbing; noreferrer also implies noopener.
    expect(link.getAttribute('rel')).toContain('noreferrer');
  });

  it('username copy button aria-label contains "username"', () => {
    render(<ServiceAccountTokensTokenModal name="jenkins-prod" token="sat.test-token" onClose={jest.fn()} />);
    const btn = screen.getByTestId('sat-basic-username-copy');
    expect(btn.getAttribute('aria-label').toLowerCase()).toContain('username');
  });

  it('password copy button aria-label contains "password"', () => {
    render(<ServiceAccountTokensTokenModal name="jenkins-prod" token="sat.test-token" onClose={jest.fn()} />);
    const btn = screen.getByTestId('sat-basic-password-copy');
    expect(btn.getAttribute('aria-label').toLowerCase()).toContain('password');
  });
});

describe('ServiceAccountTokensRevokeModal', () => {
  it('disables submit until the token name is typed exactly', async () => {
    const onConfirm = jest.fn();
    render(
      <ServiceAccountTokensRevokeModal
        tokenName="jenkins-prod"
        onConfirm={onConfirm}
        onClose={jest.fn()}
      />
    );

    const input = await screen.findByRole('textbox');
    const submit = screen.getByRole('button', {name: LABELS.REVOKE_MODAL.REVOKE_BUTTON});

    fireEvent.change(input, {target: {value: 'wrong-name'}});
    fireEvent.click(submit);
    expect(onConfirm).not.toHaveBeenCalled();

    fireEvent.change(input, {target: {value: 'jenkins-prod'}});
    fireEvent.click(submit);
    expect(onConfirm).toHaveBeenCalled();
  });

  it('shows the validation error message when input does not match', async () => {
    render(
      <ServiceAccountTokensRevokeModal
        tokenName="jenkins-prod"
        onConfirm={jest.fn()}
        onClose={jest.fn()}
      />
    );

    const input = await screen.findByRole('textbox');
    fireEvent.change(input, {target: {value: 'oops'}});

    expect(screen.getByText(LABELS.REVOKE_MODAL.VALIDATION_ERROR('jenkins-prod'))).toBeInTheDocument();
  });

  it('renders the loading mask and disables cancel while isRevoking', async () => {
    render(
      <ServiceAccountTokensRevokeModal
        tokenName="jenkins-prod"
        onConfirm={jest.fn()}
        onClose={jest.fn()}
        isRevoking
      />
    );

    expect(await screen.findByText(LABELS.REVOKE_MODAL.REVOKING_MASK)).toBeInTheDocument();
    expect(screen.getByRole('button', {name: LABELS.REVOKE_MODAL.CANCEL_BUTTON})).toBeDisabled();
  });
});

describe('ServiceAccountTokens permissions gating', () => {
  it('hides the Create button when user lacks the create permission', async () => {
    ExtJS.checkPermission.mockImplementation((p) => p !== 'nexus:service-accounts:create');

    render(<ServiceAccountTokens />);

    await waitFor(() => {
      expect(screen.getByPlaceholderText(LABELS.LIST.FILTER_PLACEHOLDER)).toBeInTheDocument();
    });
    expect(screen.queryByRole('button', {name: LABELS.LIST.CREATE_BUTTON})).not.toBeInTheDocument();
  });

  it('does not fetch roles when user lacks the create permission', async () => {
    ExtJS.checkPermission.mockImplementation((p) => p !== 'nexus:service-accounts:create');

    render(<ServiceAccountTokens />);

    await waitFor(() => {
      expect(screen.getByPlaceholderText(LABELS.LIST.FILTER_PLACEHOLDER)).toBeInTheDocument();
    });

    const rolesCalls = Axios.get.mock.calls.filter(([url]) => url.includes('roles'));
    expect(rolesCalls).toHaveLength(0);
  });

  it('still loads tokens when the roles fetch fails (admin without nexus:roles:read)', async () => {
    Axios.get.mockImplementation((url) => {
      if (url.includes('roles')) {
        const err = new Error('Forbidden');
        err.response = {status: 403};
        return Promise.reject(err);
      }
      return Promise.resolve({data: [{
        id: 't1', name: 'jenkins-prod', roleId: 'nx-deployer', createdBy: 'admin',
        expiresAt: null, lastUsedAt: null, description: '',
      }]});
    });

    render(<ServiceAccountTokens />);

    expect(await screen.findByText('jenkins-prod')).toBeInTheDocument();
  });

  it('shows a forbidden roles error inside the create modal and disables submit', async () => {
    Axios.get.mockImplementation((url) => {
      if (url.includes('roles')) {
        const err = new Error('Forbidden');
        err.response = {status: 403};
        return Promise.reject(err);
      }
      return Promise.resolve({data: []});
    });

    render(<ServiceAccountTokens />);

    fireEvent.click(await screen.findByRole('button', {name: LABELS.LIST.CREATE_BUTTON}));

    const codeNode = await screen.findByText('nexus:roles:read');
    const alert = codeNode.closest('.nx-alert');
    expect(alert).not.toBeNull();
    expect(alert).toHaveTextContent(
      'Missing privilege: nexus:roles:read. Contact your administrator.'
    );
    fireEvent.change(screen.getByRole('textbox', {name: /Name/i}), {target: {value: 'svc'}});
    expect(screen.getByRole('button', {name: LABELS.CREATE_MODAL.CREATE_BUTTON})).toBeDisabled();
  });

  it('rows are focusable so screen readers announce row content while tabbing', async () => {
    Axios.get.mockImplementation((url) => {
      if (url.includes('roles')) return Promise.resolve({data: MOCK_ROLES});
      return Promise.resolve({data: [{
        id: 't1', name: 'jenkins-prod', roleId: 'nx-deployer', createdBy: 'admin',
        expiresAt: null, lastUsedAt: null, description: '',
      }]});
    });

    render(<ServiceAccountTokens />);

    const cell = await screen.findByText('jenkins-prod');
    const row = cell.closest('tr');
    expect(row).toHaveAttribute('tabindex', '0');
  });

  it('returns focus to the row actions trigger when the revoke modal is canceled', async () => {
    Axios.get.mockImplementation((url) => {
      if (url.includes('roles')) return Promise.resolve({data: MOCK_ROLES});
      return Promise.resolve({data: [{
        id: 't1', name: 'jenkins-prod', roleId: 'nx-deployer', createdBy: 'admin',
        expiresAt: null, lastUsedAt: null, description: '',
      }]});
    });

    render(<ServiceAccountTokens />);

    await screen.findByText('jenkins-prod');
    const dropdownToggle = document.querySelector('.nx-icon-dropdown__toggle');
    fireEvent.click(dropdownToggle);
    fireEvent.click(screen.getByRole('button', {name: LABELS.LIST.ACTIONS.REVOKE}));

    // Modal is now open. Cancel it.
    fireEvent.click(screen.getByRole('button', {name: LABELS.REVOKE_MODAL.CANCEL_BUTTON}));

    await waitFor(() => {
      expect(document.querySelector('.nx-icon-dropdown__toggle')).toHaveFocus();
    });
  });

  it('hides the row action menu when user lacks the delete permission', async () => {
    Axios.get.mockImplementation((url) => {
      if (url.includes('roles')) return Promise.resolve({data: MOCK_ROLES});
      return Promise.resolve({data: [{
        id: 't1', name: 'jenkins-prod', roleId: 'nx-deployer', createdBy: 'admin',
        expiresAt: null, lastUsedAt: null, description: '',
      }]});
    });
    ExtJS.checkPermission.mockImplementation((p) => p !== 'nexus:service-accounts:delete');

    render(<ServiceAccountTokens />);

    expect(await screen.findByText('jenkins-prod')).toBeInTheDocument();
    expect(screen.queryByRole('button', {name: /Actions/i})).not.toBeInTheDocument();
  });
  it('names the token in the revoke error when the token is already gone (404)', async () => {
    Axios.get.mockImplementation((url) => {
      if (url.includes('roles')) return Promise.resolve({data: MOCK_ROLES});
      return Promise.resolve({data: [{
        id: 't1', name: 'jenkins-prod', roleId: 'nx-deployer', createdBy: 'admin',
        expiresAt: null, lastUsedAt: null, description: '',
      }]});
    });
    Axios.delete.mockRejectedValue({response: {status: 404}});
    const showErrorMessage = jest.spyOn(ExtJS, 'showErrorMessage').mockImplementation(() => {});

    render(<ServiceAccountTokens />);

    await screen.findByText('jenkins-prod');
    fireEvent.click(document.querySelector('.nx-icon-dropdown__toggle'));
    fireEvent.click(screen.getByRole('button', {name: LABELS.LIST.ACTIONS.REVOKE}));
    // Full page render, so scope to the confirmation field (the filter is also a textbox).
    fireEvent.change(document.getElementById('revokeConfirmationString'), {
      target: {value: 'jenkins-prod'},
    });
    fireEvent.click(screen.getByRole('button', {name: LABELS.REVOKE_MODAL.REVOKE_BUTTON}));

    await waitFor(() => {
      expect(showErrorMessage).toHaveBeenCalledWith(
        LABELS.MESSAGES.REVOKE_ERROR_NOT_FOUND('jenkins-prod')
      );
    });
    // Guards the context wiring: without it the message would interpolate "undefined".
    expect(showErrorMessage.mock.calls[0][0]).toContain('jenkins-prod');
  });
});

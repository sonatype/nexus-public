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
    render(<ServiceAccountTokensTokenModal token="sat.test-token" onClose={jest.fn()} />);

    await waitFor(() => {
      expect(screen.getByText(LABELS.TOKEN_MODAL.WARNING)).toBeInTheDocument();
    });
  });

  it('renders the auto-close countdown in the footer', async () => {
    render(<ServiceAccountTokensTokenModal token="sat.test-token" onClose={jest.fn()} />);

    await waitFor(() => {
      expect(screen.getByText(LABELS.TOKEN_MODAL.AUTO_CLOSE_NOTICE(60))).toBeInTheDocument();
    });
  });

  it('warning has role="alert" for accessibility', async () => {
    render(<ServiceAccountTokensTokenModal token="sat.test-token" onClose={jest.fn()} />);

    await waitFor(() => {
      const warning = screen.getByText(LABELS.TOKEN_MODAL.WARNING);
      expect(warning.closest('[role="alert"]')).toBeInTheDocument();
    });
  });

  it('auto-closes when countdown reaches 0', async () => {
    const onClose = jest.fn();
    render(<ServiceAccountTokensTokenModal token="sat.test-token" onClose={onClose} />);

    await waitFor(() => {
      expect(screen.getByText(LABELS.TOKEN_MODAL.AUTO_CLOSE_NOTICE(60))).toBeInTheDocument();
    });

    act(() => {
      jest.advanceTimersByTime(60000);
    });

    expect(onClose).toHaveBeenCalled();
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

    expect(screen.getByText(LABELS.REVOKE_MODAL.VALIDATION_ERROR)).toBeInTheDocument();
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
});

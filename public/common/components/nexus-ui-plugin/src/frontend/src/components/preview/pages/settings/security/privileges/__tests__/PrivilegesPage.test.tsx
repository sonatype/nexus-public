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
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { Theme } from '@radix-ui/themes';
import { UIRouterContext } from '@uirouter/react';
import { PrivilegesPage } from '../PrivilegesPage';
import { usePrivilegesApi } from '../usePrivilegesApi';
import { ExtJS } from '@sonatype/nexus-ui-plugin';
import { ToastProvider } from '../../../../../shared/Toast';

// Mock dependencies
jest.mock('../usePrivilegesApi');
jest.mock('@sonatype/nexus-ui-plugin', () => ({
  ExtJS: {
    checkPermission: jest.fn(),
  },
}));

// Mock child components
jest.mock('../PrivilegesList', () => ({
  PrivilegesList: function MockPrivilegesList(props: { onSelect: (id: string) => void }) {
    const { onSelect } = props;
    return (
      <div data-testid="privileges-list">
        <button onClick={() => onSelect('test-priv')}>Select Privilege</button>
      </div>
    );
  },
}));

jest.mock('../PrivilegeDetail', () => ({
  PrivilegeDetail: ({ onCancel, onDelete, onSave }: any) => (
    <div data-testid="privilege-detail">
      <button onClick={onCancel}>Back</button>
      <button onClick={onDelete}>Delete</button>
      <button onClick={() => onSave({ name: 'test', description: '', type: 'wildcard', properties: {} })}>
        Save
      </button>
    </div>
  ),
}));

jest.mock('../PrivilegeForm', () => ({
  PrivilegeForm: ({ onCancel, onSave, onSubmitRef, onValidationChange, wizardStep }: any) => {
    // Register the submit ref
    if (onSubmitRef) {
      onSubmitRef.current = async () => {
        await onSave({ name: 'new-priv', description: '', type: 'wildcard', properties: { pattern: '*' } });
      };
    }

    // Signal valid immediately so wizard can advance
    onValidationChange?.(true);

    return (
      <div data-testid="privilege-form">
        <div>Step: {wizardStep}</div>
        <button onClick={onCancel}>Cancel</button>
        <button onClick={onSubmitRef?.current}>Create</button>
      </div>
    );
  },
}));

jest.mock('../PrivilegeTypeSelector', () => ({
  PrivilegeTypeSelector: ({ onSelect }: any) => (
    <div data-testid="privilege-type-selector">
      <button onClick={() => onSelect('wildcard')}>Select Wildcard</button>
    </div>
  ),
}));

jest.mock('../PrivilegeProfilePage', () => ({
  PrivilegeProfilePage: ({ privilegeId, onBack, activeTab, onTabChange }: any) => (
    <div data-testid="privilege-profile">
      <span>Profile for: {privilegeId}</span>
      {/* Surfaced so routing tests can assert which tab the URL resolved to. */}
      <span data-testid="active-tab">{activeTab}</span>
      <button onClick={() => onTabChange?.('users')}>Go Users Tab</button>
      <button onClick={() => onTabChange?.('roles')}>Go Roles Tab</button>
      <button onClick={onBack}>Back</button>
    </div>
  ),
}));

const mockUsePrivilegesApi = usePrivilegesApi as jest.MockedFunction<typeof usePrivilegesApi>;
const mockCheckPermission = ExtJS.checkPermission as jest.Mock;

const mockPrivilege = {
  id: 'test-priv',
  version: '1',
  name: 'Test Privilege',
  description: 'Test Description',
  type: 'wildcard',
  readOnly: false,
  properties: { pattern: 'test:*' },
  permission: 'test:*',
};

const renderWithTheme = (component: React.ReactNode) => {
  return render(
    <Theme>
      <ToastProvider>{component}</ToastProvider>
    </Theme>
  );
};

describe('PrivilegesPage', () => {
  const originalLocation = window.location;

  beforeEach(() => {
    jest.clearAllMocks();
    mockCheckPermission.mockReturnValue(true);
    mockUsePrivilegesApi.mockReturnValue({
      loading: false,
      error: null,
      setError: jest.fn(),
      fetchPrivileges: jest.fn().mockResolvedValue({ data: [mockPrivilege], total: 1 }),
      fetchPrivilegeReferences: jest.fn().mockResolvedValue([]),
      fetchPrivilegeTypes: jest.fn().mockResolvedValue([]),
      findPrivilege: jest.fn().mockResolvedValue(mockPrivilege),
      createPrivilege: jest.fn().mockResolvedValue(mockPrivilege),
      updatePrivilege: jest.fn().mockResolvedValue(mockPrivilege),
      deletePrivilege: jest.fn().mockResolvedValue(undefined),
    });

    // Reset hash
    window.location.hash = '';
  });

  afterAll(() => {
    window.location = originalLocation;
  });

  it('should render the page header', () => {
    renderWithTheme(<PrivilegesPage />);
    
    expect(screen.getAllByText('Privileges').length).toBeGreaterThanOrEqual(1);
    expect(screen.getByText('Manage privileges and their permissions')).toBeInTheDocument();
  });

  it('should show Create Privilege button when user has permission', () => {
    mockCheckPermission.mockReturnValue(true);
    renderWithTheme(<PrivilegesPage />);
    
    expect(screen.getByRole('button', { name: /Create Privilege/ })).toBeInTheDocument();
  });

  it('should hide Create Privilege button when user lacks permission', () => {
    mockCheckPermission.mockImplementation((perm: string) => perm !== 'nexus:privileges:create');
    (global as any).NX.Permissions.check.mockImplementation((perm: string) => perm !== 'nexus:privileges:create');
    renderWithTheme(<PrivilegesPage />);
    
    expect(screen.queryByRole('button', { name: /Create Privilege/ })).not.toBeInTheDocument();
  });

  it('should show PrivilegesList by default', () => {
    renderWithTheme(<PrivilegesPage />);
    
    expect(screen.getByTestId('privileges-list')).toBeInTheDocument();
  });

  it('should navigate to select-type view when Create Privilege is clicked', async () => {
    renderWithTheme(<PrivilegesPage />);
    
    fireEvent.click(screen.getByRole('button', { name: /Create Privilege/ }));
    
    expect(window.location.hash).toContain('/create');
    
    // Simulate hash change
    fireEvent(window, new HashChangeEvent('hashchange'));

    await waitFor(() => {
      expect(screen.getByTestId('privilege-type-selector')).toBeInTheDocument();
    });
  });

  it('should navigate through the 3-step create wizard', async () => {
    window.location.hash = '#preview/admin/security/privileges/create';
    renderWithTheme(<PrivilegesPage />);
    
    // Step 1: Select Type
    await waitFor(() => {
      expect(screen.getByTestId('privilege-type-selector')).toBeInTheDocument();
    });
    fireEvent.click(screen.getByText('Select Wildcard'));
    
    // Click Next to go to Step 2: Setup
    fireEvent.click(screen.getByRole('button', { name: /Continue/i }));

    expect(window.location.hash).toContain('/create/wildcard');
    fireEvent(window, new HashChangeEvent('hashchange'));

    await waitFor(() => {
      expect(screen.getByTestId('privilege-form')).toBeInTheDocument();
      // Should show Setup description in header
      expect(screen.getByText(/Step 2: Basic setup/i)).toBeInTheDocument();
    });

    // Click Next to go to Step 3: Configuration
    fireEvent.click(screen.getByRole('button', { name: /Continue/i }));

    await waitFor(() => {
      expect(screen.getByTestId('privilege-form')).toBeInTheDocument();
      // Should show Configuration description in header
      expect(screen.getByText(/Step 3: Configure privilege settings/i)).toBeInTheDocument();
    });
    
    // Final step - Create
    fireEvent.click(screen.getByText('Create Privilege'));

    await waitFor(() => {
      expect(window.location.hash).toBe('#preview/admin/security/privileges');
    });
  });

  it('should navigate to profile view when a privilege is selected', async () => {
    renderWithTheme(<PrivilegesPage />);
    
    fireEvent.click(screen.getByText('Select Privilege'));
    
    expect(window.location.hash).toContain('/test-priv');
    
    // Simulate hash change
    fireEvent(window, new HashChangeEvent('hashchange'));

    await waitFor(() => {
      expect(screen.getByTestId('privilege-profile')).toBeInTheDocument();
      expect(screen.getByText(/Profile for: test-priv/)).toBeInTheDocument();
    });
  });

  it('should navigate back to list after creating a privilege', async () => {
    // Start at Step 3: Configuration
    window.location.hash = '#preview/admin/security/privileges/create/wildcard';
    renderWithTheme(<PrivilegesPage />);

    // Move to Step 3 manually since useEffect handles the transition from Step 1 to 2 in code
    // but here we might need to trigger it or just click Next.
    // Actually, PrivilegesPage logic: if typeId is present, internalWizardStep becomes 1 (Step 2).
    // We need to click "Next: Configure" to get to Step 3 where "Create Privilege" button exists.
    
    await waitFor(() => {
      expect(screen.getByText(/Step 2: Basic setup/i)).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: /Continue/i }));

    await waitFor(() => {
      expect(screen.getByText(/Step 3: Configure privilege settings/i)).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('Create Privilege'));

    await waitFor(() => {
      // Should navigate back to list after successful creation
      expect(window.location.hash).toBe('#preview/admin/security/privileges');
    });
  });

  it('should show error alert when there is an error', () => {
    mockUsePrivilegesApi.mockReturnValue({
      ...mockUsePrivilegesApi(),
      error: 'Test error message',
    });

    renderWithTheme(<PrivilegesPage />);

    expect(screen.getByText('Test error message')).toBeInTheDocument();
  });

  /**
   * NEXUS-52167 follow-up: from the profile's "Users With Access" tab, clicking a
   * User ID then pressing Back landed on Overview, because the active tab was
   * local state destroyed on unmount. It now lives in the URL.
   */
  describe('profile tab is preserved in the URL (NEXUS-52167)', () => {
    it('defaults to overview for a legacy /profile URL with no tab segment', async () => {
      window.location.hash = '#preview/admin/security/privileges/test-priv/profile';
      renderWithTheme(<PrivilegesPage />);

      await waitFor(() => {
        expect(screen.getByTestId('active-tab')).toHaveTextContent('overview');
      });
    });

    it('restores the users tab from the URL', async () => {
      window.location.hash =
        '#preview/admin/security/privileges/test-priv/profile?tab=users';
      renderWithTheme(<PrivilegesPage />);

      await waitFor(() => {
        expect(screen.getByTestId('active-tab')).toHaveTextContent('users');
      });
    });

    it('restores the roles tab from the URL', async () => {
      window.location.hash =
        '#preview/admin/security/privileges/test-priv/profile?tab=roles';
      renderWithTheme(<PrivilegesPage />);

      await waitFor(() => {
        expect(screen.getByTestId('active-tab')).toHaveTextContent('roles');
      });
    });

    it('falls back to overview for an unknown tab segment', async () => {
      window.location.hash =
        '#preview/admin/security/privileges/test-priv/profile?tab=not-a-tab';
      renderWithTheme(<PrivilegesPage />);

      await waitFor(() => {
        expect(screen.getByTestId('privilege-profile')).toBeInTheDocument();
        expect(screen.getByTestId('active-tab')).toHaveTextContent('overview');
      });
    });

    it('writes the tab into the hash when the tab changes', async () => {
      window.location.hash = '#preview/admin/security/privileges/test-priv/profile';
      renderWithTheme(<PrivilegesPage />);

      await waitFor(() => {
        expect(screen.getByTestId('privilege-profile')).toBeInTheDocument();
      });

      fireEvent.click(screen.getByText('Go Users Tab'));

      await waitFor(() => {
        expect(window.location.hash).toBe(
          '#preview/admin/security/privileges/test-priv/profile?tab=users'
        );
        expect(screen.getByTestId('active-tab')).toHaveTextContent('users');
      });
    });

    // The reported bug, end to end.
    it('reproduces the reported round trip: users tab survives leaving and returning', async () => {
      window.location.hash = '#preview/admin/security/privileges/test-priv/profile';
      const { unmount } = renderWithTheme(<PrivilegesPage />);

      await waitFor(() => {
        expect(screen.getByTestId('privilege-profile')).toBeInTheDocument();
      });

      // User switches to the Users With Access tab.
      fireEvent.click(screen.getByText('Go Users Tab'));
      await waitFor(() => {
        expect(window.location.hash).toContain('tab=users');
      });
      const urlBeforeLeaving = window.location.hash;

      // Clicking a User ID navigates away; PrivilegesPage unmounts.
      unmount();
      window.location.hash = '#preview/admin/security/users/test1/default/profile';

      // Browser Back restores the previous hash and remounts the page.
      window.location.hash = urlBeforeLeaving;
      renderWithTheme(<PrivilegesPage />);

      await waitFor(() => {
        // Previously this was 'overview' — the bug.
        expect(screen.getByTestId('active-tab')).toHaveTextContent('users');
      });
    });

    /**
     * The tab must be a query param, not a path segment: the profile route is the
     * terminal pattern `/:privilegeId/profile`, so `/profile/users` matches no
     * route and Back falls through to `rules.otherwise()`. Locked down here because
     * it is invisible in jsdom, where no router is mounted and any hash "works".
     */
    it('encodes the tab as a ?tab query param, never as a path segment', async () => {
      window.location.hash = '#preview/admin/security/privileges/test-priv/profile';
      renderWithTheme(<PrivilegesPage />);

      await waitFor(() => {
        expect(screen.getByTestId('privilege-profile')).toBeInTheDocument();
      });

      fireEvent.click(screen.getByText('Go Users Tab'));

      await waitFor(() => {
        expect(window.location.hash).toContain('tab=users');
      });

      // The path must remain exactly the registered route pattern.
      const [path] = window.location.hash.replace(/^#/, '').split('?');
      expect(path).toBe('preview/admin/security/privileges/test-priv/profile');
      expect(window.location.hash).not.toContain('/profile/users');
    });

    // When a router is present it must own the URL write, so its state params and
    // the address bar cannot disagree.
    it('routes tab changes through UI-Router when one is mounted', async () => {
      const go = jest.fn();
      window.location.hash = '#preview/admin/security/privileges/test-priv/profile';

      render(
        <Theme>
          <ToastProvider>
            <UIRouterContext.Provider value={{ stateService: { go } } as any}>
              <PrivilegesPage />
            </UIRouterContext.Provider>
          </ToastProvider>
        </Theme>
      );

      await waitFor(() => {
        expect(screen.getByTestId('privilege-profile')).toBeInTheDocument();
      });

      fireEvent.click(screen.getByText('Go Users Tab'));

      expect(go).toHaveBeenCalledWith(
        'preview.admin.security.privileges.profile',
        { privilegeId: 'test-priv', tab: 'users' },
        { notify: false, location: 'replace' }
      );
      // The tab still renders immediately, not gated on the router.
      expect(screen.getByTestId('active-tab')).toHaveTextContent('users');
    });

    it('falls back to writing the hash when no router is mounted', async () => {
      window.location.hash = '#preview/admin/security/privileges/test-priv/profile';
      renderWithTheme(<PrivilegesPage />);

      await waitFor(() => {
        expect(screen.getByTestId('privilege-profile')).toBeInTheDocument();
      });

      fireEvent.click(screen.getByText('Go Users Tab'));

      await waitFor(() => {
        expect(screen.getByTestId('active-tab')).toHaveTextContent('users');
        expect(window.location.hash).toContain('tab=users');
      });
    });

    it('does not push history entries for tab switches', async () => {
      window.location.hash = '#preview/admin/security/privileges/test-priv/profile';
      renderWithTheme(<PrivilegesPage />);

      await waitFor(() => {
        expect(screen.getByTestId('privilege-profile')).toBeInTheDocument();
      });

      const replaceSpy = jest.spyOn(window.history, 'replaceState');
      const pushSpy = jest.spyOn(window.history, 'pushState');

      fireEvent.click(screen.getByText('Go Users Tab'));
      fireEvent.click(screen.getByText('Go Roles Tab'));

      // Back must mean "the page before this one", not "the previous tab".
      expect(replaceSpy).toHaveBeenCalledTimes(2);
      expect(pushSpy).not.toHaveBeenCalled();

      replaceSpy.mockRestore();
      pushSpy.mockRestore();
    });
  });
});

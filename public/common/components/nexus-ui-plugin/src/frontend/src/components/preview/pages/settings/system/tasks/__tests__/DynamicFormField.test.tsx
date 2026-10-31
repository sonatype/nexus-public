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
import { render, screen, fireEvent, within } from '@testing-library/react';
import { Theme } from '@radix-ui/themes';
import { DynamicFormField } from '../TaskTypeSelector';
import { TaskType } from '../types';
import { DYNAMIC_FORM_FIELDS } from '../TaskStrings';

// Render real shared/form components (incl. SettingsTransferList); only stub the API module.
jest.mock('../../../../../../../interface/api', () => ({
  restClient: { get: jest.fn().mockResolvedValue([]) },
}));

// ExtJS mock for isCloud detection in staticInfo field type
import { ExtJS } from '../../../../../../../interface/ExtJS';
jest.mock('../../../../../../../interface/ExtJS', () => ({
  ExtJS: { state: jest.fn(() => ({ getValue: jest.fn(() => false) })) },
}));

const renderWithTheme = (component: React.ReactElement) => render(<Theme>{component}</Theme>);

type FormField = NonNullable<TaskType['formFields']>[0];

const repositoryField: FormField = { id: 'repositoryName', type: 'repo', label: 'Repository' };

// Options as the parent (DynamicFormFields) supplies them: already filtered per
// TASK_TYPE_REPO_FILTERS and prepended with the "(All Repositories)" entry.
const repoOptions = [
  { value: DYNAMIC_FORM_FIELDS.ALL_REPOSITORIES_VALUE, label: DYNAMIC_FORM_FIELDS.ALL_REPOSITORIES_LABEL },
  { value: 'maven-releases', label: 'maven-releases' },
  { value: 'maven-central', label: 'maven-central' },
  { value: 'pypi-hosted', label: 'pypi-hosted' },
];

describe('DynamicFormField - multi-repository transfer list', () => {
  it('renders a dual-list transfer selector (Available + Selected) when multiSelect', () => {
    renderWithTheme(
      <DynamicFormField field={repositoryField} value="" onChange={jest.fn()} multiSelect repoOptions={repoOptions} />
    );

    expect(screen.getByRole('listbox', { name: 'Available' })).toBeInTheDocument();
    expect(screen.getByRole('listbox', { name: 'Selected' })).toBeInTheDocument();
  });

  it('renders a single-repository combobox (not a transfer list) when not multiSelect', () => {
    renderWithTheme(
      <DynamicFormField field={repositoryField} value="" onChange={jest.fn()} repoOptions={repoOptions} />
    );

    expect(screen.queryByRole('listbox', { name: 'Available' })).not.toBeInTheDocument();
    expect(screen.queryByRole('listbox', { name: 'Selected' })).not.toBeInTheDocument();
  });

  it('shows previously-saved repositories on the Selected side (load/round-trip)', () => {
    renderWithTheme(
      <DynamicFormField
        field={repositoryField}
        value="maven-releases,maven-central"
        onChange={jest.fn()}
        multiSelect
        repoOptions={repoOptions}
      />
    );

    const selected = screen.getByRole('listbox', { name: 'Selected' });
    const available = screen.getByRole('listbox', { name: 'Available' });
    expect(within(selected).getByText('maven-releases')).toBeInTheDocument();
    expect(within(selected).getByText('maven-central')).toBeInTheDocument();
    expect(within(available).queryByText('maven-releases')).not.toBeInTheDocument();
    expect(within(available).getByText('pypi-hosted')).toBeInTheDocument();
  });

  it('serializes the selection back to a comma-separated string on change', () => {
    const onChange = jest.fn();
    renderWithTheme(
      <DynamicFormField
        field={repositoryField}
        value="maven-releases"
        onChange={onChange}
        multiSelect
        repoOptions={repoOptions}
      />
    );

    const available = screen.getByRole('listbox', { name: 'Available' });
    fireEvent.dblClick(within(available).getByText('maven-central'));

    expect(onChange).toHaveBeenCalledWith('maven-releases,maven-central');
  });

  describe('"(All Repositories)" / "*" support', () => {
    it('offers the "(All Repositories)" option supplied by the parent', () => {
      renderWithTheme(
        <DynamicFormField field={repositoryField} value="" onChange={jest.fn()} multiSelect repoOptions={repoOptions} />
      );

      const available = screen.getByRole('listbox', { name: 'Available' });
      expect(within(available).getByText(DYNAMIC_FORM_FIELDS.ALL_REPOSITORIES_LABEL)).toBeInTheDocument();
    });

    it('displays a task saved with "*" on the Selected side', () => {
      renderWithTheme(
        <DynamicFormField field={repositoryField} value="*" onChange={jest.fn()} multiSelect repoOptions={repoOptions} />
      );

      const selected = screen.getByRole('listbox', { name: 'Selected' });
      expect(within(selected).getByText(DYNAMIC_FORM_FIELDS.ALL_REPOSITORIES_LABEL)).toBeInTheDocument();
    });

    it('serializes a selection of "(All Repositories)" to "*"', () => {
      const onChange = jest.fn();
      renderWithTheme(
        <DynamicFormField field={repositoryField} value="" onChange={onChange} multiSelect repoOptions={repoOptions} />
      );

      const available = screen.getByRole('listbox', { name: 'Available' });
      fireEvent.dblClick(within(available).getByText(DYNAMIC_FORM_FIELDS.ALL_REPOSITORIES_LABEL));

      expect(onChange).toHaveBeenCalledWith('*');
    });

    it('shows the friendly label for "*" on the Selected side even when repoOptions has not loaded yet', () => {
      // Simulates the initial-load race: the task was saved with "*" but the repo list
      // is still in flight (empty). The fallback must resolve "*" to the friendly label
      // so the user never sees a raw asterisk.
      renderWithTheme(
        <DynamicFormField field={repositoryField} value="*" onChange={jest.fn()} multiSelect repoOptions={[]} />
      );

      const selected = screen.getByRole('listbox', { name: 'Selected' });
      expect(within(selected).getByText(DYNAMIC_FORM_FIELDS.ALL_REPOSITORIES_LABEL)).toBeInTheDocument();
      expect(within(selected).queryByText('*')).not.toBeInTheDocument();
    });
  });

  describe('accessibility', () => {
    it('exposes the selector as a labelled group', () => {
      renderWithTheme(
        <DynamicFormField field={repositoryField} value="" onChange={jest.fn()} multiSelect repoOptions={repoOptions} />
      );

      expect(screen.getByRole('group', { name: 'Repository' })).toBeInTheDocument();
    });

    it('marks the group invalid and shows an associated error message', () => {
      renderWithTheme(
        <DynamicFormField
          field={repositoryField}
          value=""
          onChange={jest.fn()}
          multiSelect
          repoOptions={repoOptions}
          error="At least one repository is required"
        />
      );

      const group = screen.getByRole('group', { name: 'Repository' });
      const alert = screen.getByRole('alert');
      expect(alert).toHaveTextContent('At least one repository is required');
      expect(group).toHaveAttribute('aria-invalid', 'true');
      expect(group).toHaveAttribute('aria-describedby', alert.id);
    });

    it('moves a repository to the selected list with the Space key', () => {
      const onChange = jest.fn();
      renderWithTheme(
        <DynamicFormField field={repositoryField} value="" onChange={onChange} multiSelect repoOptions={repoOptions} />
      );

      const available = screen.getByRole('listbox', { name: 'Available' });
      fireEvent.keyDown(within(available).getByText('maven-releases'), { key: ' ' });

      expect(onChange).toHaveBeenCalledWith('maven-releases');
    });
  });

  // NEXUS-53485: field types specific to the Data Repair Plan task (resolved via the per-task
  // override map keyed by taskTypeId).
  const PLAN = 'blobstore.planReconciliation';

  describe('alertBanner field type', () => {
    it('renders the hard-coded banner copy and no input control', () => {
      const field: FormField = { id: 'topAlertBanner', type: 'templateOnly' as any, label: '', required: false };
      renderWithTheme(<DynamicFormField field={field} value="" onChange={jest.fn()} taskTypeId={PLAN} />);

      expect(screen.getByText(/This task generates recovery plans/)).toBeInTheDocument();
      expect(screen.queryByTestId('input-topAlertBanner')).not.toBeInTheDocument();
    });
  });

  describe('malware.remediator banners (NEXUS-53359)', () => {
    const MALWARE = 'malware.remediator';

    it('renders the requirements field as an info banner with no input control', () => {
      const field: FormField = { id: 'malwareRemediatorTaskRequirements', type: 'string', label: '' };
      renderWithTheme(
        <DynamicFormField field={field} value="" onChange={jest.fn()} taskTypeId={MALWARE} />
      );
      expect(
        screen.getByText(/Repository Firewall enabled with the Security-Malicious policy/)
      ).toBeInTheDocument();
      expect(screen.queryByTestId('input-malwareRemediatorTaskRequirements')).not.toBeInTheDocument();
    });

    it('renders the cleanup message field as a warning banner with no input control', () => {
      const field: FormField = { id: 'enableMalwareCleanupMessage', type: 'string', label: '' };
      renderWithTheme(
        <DynamicFormField field={field} value="" onChange={jest.fn()} taskTypeId={MALWARE} />
      );
      expect(
        screen.getByText(/may remove dependencies currently in use/)
      ).toBeInTheDocument();
      expect(screen.queryByTestId('input-enableMalwareCleanupMessage')).not.toBeInTheDocument();
    });
  });

  describe('taskScope field type', () => {
    const field: FormField = { id: 'taskScope', type: 'taskScope' as any, label: 'Timespan:', required: true };

    it('renders a radio group with the current value selected', () => {
      renderWithTheme(<DynamicFormField field={field} value="duration" onChange={jest.fn()} taskTypeId={PLAN} />);

      expect(screen.getByRole('radiogroup')).toBeInTheDocument();
      expect(screen.getByText('Duration')).toBeInTheDocument();
      expect(screen.getByText('Start/End Dates')).toBeInTheDocument();
    });

    it('emits the selected scope value on change', () => {
      const onChange = jest.fn();
      renderWithTheme(<DynamicFormField field={field} value="duration" onChange={onChange} taskTypeId={PLAN} />);

      fireEvent.click(screen.getByTestId('input-taskScope-dates'));
      expect(onChange).toHaveBeenCalledWith('dates');
    });
  });

  describe('date field type (m/d/Y <-> input ISO)', () => {
    const field: FormField = { id: 'reconcileStartDate', type: 'date' as any, label: 'Start date', required: false };

    it('displays a stored m/d/Y value as a YYYY-MM-DD input', () => {
      renderWithTheme(<DynamicFormField field={field} value="06/24/2026" onChange={jest.fn()} taskTypeId={PLAN} />);
      expect(screen.getByTestId('input-reconcileStartDate')).toHaveValue('2026-06-24');
    });

    it('emits the picked date back as m/d/Y (not ISO) so it round-trips into Classic', () => {
      const onChange = jest.fn();
      renderWithTheme(<DynamicFormField field={field} value="" onChange={onChange} taskTypeId={PLAN} />);

      fireEvent.change(screen.getByTestId('input-reconcileStartDate'), { target: { value: '2026-06-24' } });
      expect(onChange).toHaveBeenCalledWith('06/24/2026');
    });
  });

  describe('blobstoreName itemselect (sentinel handling)', () => {
    const field: FormField = { id: 'blobstoreName', type: 'itemselect' as any, label: 'Blob store', required: false };
    const blobStoreOptions = [
      { value: 'default', label: 'default' },
      { value: 'other', label: 'other' },
    ];

    it('treats the "(All Blob Stores)" sentinel as nothing selected', () => {
      renderWithTheme(
        <DynamicFormField
          field={field}
          value="(All Blob Stores)"
          onChange={jest.fn()}
          multiSelect
          blobStoreOptions={blobStoreOptions}
          taskTypeId={PLAN}
        />
      );

      const available = screen.getByRole('listbox', { name: 'Available' });
      const selected = screen.getByRole('listbox', { name: 'Selected' });
      expect(within(available).getByText('default')).toBeInTheDocument();
      expect(within(available).getByText('other')).toBeInTheDocument();
      expect(within(selected).queryByText('default')).not.toBeInTheDocument();
    });

    it('emits a comma-separated string when a blob store is selected', () => {
      const onChange = jest.fn();
      renderWithTheme(
        <DynamicFormField
          field={field}
          value="(All Blob Stores)"
          onChange={onChange}
          multiSelect
          blobStoreOptions={blobStoreOptions}
          taskTypeId={PLAN}
        />
      );

      const available = screen.getByRole('listbox', { name: 'Available' });
      fireEvent.keyDown(within(available).getByText('default'), { key: ' ' });
      expect(onChange).toHaveBeenCalledWith('default');
    });
  });

  // NEXUS-53484: staticInfo field type renders a read-only section header + help text.
  // The Execute Data Repair Plan task uses this for planOptionsLabelId and planInformationLabelId.
  const EXECUTE_PLAN = 'blobstore.executeReconciliationPlan';

  describe('staticInfo field', () => {
    const baseField = { id: 'planOptionsLabelId', label: '', type: 'staticInfo', required: false } as any;

    it('renders the self-hosted section label and help text', () => {
      (ExtJS.state as jest.Mock).mockReturnValue({ getValue: jest.fn(() => false) });
      renderWithTheme(
        <DynamicFormField
          field={baseField}
          value=""
          onChange={jest.fn()}
          taskTypeId={EXECUTE_PLAN}
        />
      );
      expect(screen.getByText('Execution Information')).toBeInTheDocument();
      expect(screen.getByText(/Execution details for recovery plan/)).toBeInTheDocument();
    });

    it('renders the cloud label when isCloud is true', () => {
      (ExtJS.state as jest.Mock).mockReturnValue({ getValue: jest.fn((k: string) => k === 'isCloud') });
      renderWithTheme(
        <DynamicFormField
          field={baseField}
          value=""
          onChange={jest.fn()}
          taskTypeId={EXECUTE_PLAN}
        />
      );
      expect(screen.getByText('Plan Options')).toBeInTheDocument();
      expect(screen.queryByText('Execution Information')).not.toBeInTheDocument();
    });

    it('does not call onChange (display-only)', () => {
      const onChange = jest.fn();
      (ExtJS.state as jest.Mock).mockReturnValue({ getValue: jest.fn(() => false) });
      renderWithTheme(
        <DynamicFormField field={baseField} value="" onChange={onChange} taskTypeId={EXECUTE_PLAN} />
      );
      expect(onChange).not.toHaveBeenCalled();
    });
  });

  describe('read-only fields (Execute Data Repair Plan)', () => {
    it('renders a read-only itemselect as a Selected-only list, not a transfer list', () => {
      renderWithTheme(
        <DynamicFormField
          field={{ id: 'blobstoreName', label: 'Blob store', type: 'itemselect', required: false } as any}
          value="default"
          onChange={jest.fn()}
          taskTypeId={EXECUTE_PLAN}
        />
      );
      const box = screen.getByTestId('input-blobstoreName');
      expect(within(box).getByText('default')).toBeInTheDocument();
      expect(screen.getByText('Selected')).toBeInTheDocument();
      expect(screen.queryByText('Available')).not.toBeInTheDocument();
      expect(screen.queryByRole('button')).not.toBeInTheDocument();
    });

    it('renders "(All Blob Stores)" as a visible item when blobstoreName is the all-stores sentinel', () => {
      renderWithTheme(
        <DynamicFormField
          field={{ id: 'blobstoreName', label: 'Blob store', type: 'itemselect', required: false } as any}
          value="(All Blob Stores)"
          onChange={jest.fn()}
          taskTypeId={EXECUTE_PLAN}
        />
      );
      const box = screen.getByTestId('input-blobstoreName');
      expect(within(box).getByText('(All Blob Stores)')).toBeInTheDocument();
    });

    it('renders a read-only taskScope radio as disabled', () => {
      renderWithTheme(
        <DynamicFormField
          field={{ id: 'taskScope', label: 'Timespan:', type: 'taskScope', required: true } as any}
          value="dates"
          onChange={jest.fn()}
          taskTypeId={EXECUTE_PLAN}
        />
      );
      expect(screen.getByTestId('input-taskScope-dates')).toBeDisabled();
      expect(screen.getByTestId('input-taskScope-duration')).toBeDisabled();
    });

    it('renders a read-only date input as disabled', () => {
      renderWithTheme(
        <DynamicFormField
          field={{ id: 'reconcileStartDate', label: 'Start date', type: 'date', required: false } as any}
          value="06/01/2026"
          onChange={jest.fn()}
          taskTypeId={EXECUTE_PLAN}
        />
      );
      expect(screen.getByTestId('input-reconcileStartDate')).toBeDisabled();
    });
  });

  // NEXUS-53741: a single-repository combobox whose descriptor ships a server-only storeFilter
  // (facets / versionPolicies) must NOT re-filter the options the parent already fetched
  // server-side. Those options are supplied as {value,label} (no format/type/facets), so
  // applying a `facets` filter client-side would drop every option — emptying the dropdown for
  // "Repository - Delete unused components" and the other facet-filtered repo tasks.
  describe('server-only storeFilters on a single-repository combobox (NEXUS-53741)', () => {
    // As DynamicFormFields supplies them for a facet-filtered task: already server-filtered,
    // projected to {value,label}, with "(All Repositories)" prepended.
    const serverFilteredOptions = [
      { value: DYNAMIC_FORM_FIELDS.ALL_REPOSITORIES_VALUE, label: DYNAMIC_FORM_FIELDS.ALL_REPOSITORIES_LABEL },
      { value: 'maven-central', label: 'maven-central' },
      { value: 'pypi-proxy', label: 'pypi-proxy' },
    ];

    const openMenu = (fieldId: string) => fireEvent.focus(screen.getByTestId(`combobox-${fieldId}`));

    it('keeps the server-filtered repositories when the field ships a facets storeFilter', async () => {
      const field: FormField = {
        id: 'repositoryName',
        type: 'repo',
        label: 'Repository',
        storeFilters: { facets: 'org.sonatype.nexus.repository.purge.PurgeUnusedFacet' },
      };
      renderWithTheme(
        <DynamicFormField field={field} value="" onChange={jest.fn()} repoOptions={serverFilteredOptions} />
      );

      openMenu('repositoryName');
      expect(await screen.findByRole('option', { name: 'maven-central' })).toBeInTheDocument();
      expect(screen.getByRole('option', { name: 'pypi-proxy' })).toBeInTheDocument();
      expect(
        screen.getByRole('option', { name: DYNAMIC_FORM_FIELDS.ALL_REPOSITORIES_LABEL })
      ).toBeInTheDocument();
    });

    it('keeps the repositories when the field ships facets + versionPolicies (maven snapshot purge)', async () => {
      const field: FormField = {
        id: 'repositoryName',
        type: 'repo',
        label: 'Repository',
        storeFilters: {
          facets: 'org.sonatype.nexus.repository.maven.PurgeUnusedSnapshotsFacet',
          versionPolicies: 'SNAPSHOT,MIXED',
        },
      };
      renderWithTheme(
        <DynamicFormField field={field} value="" onChange={jest.fn()} repoOptions={serverFilteredOptions} />
      );

      openMenu('repositoryName');
      expect(await screen.findByRole('option', { name: 'maven-central' })).toBeInTheDocument();
    });

    it('renders the parent-supplied options verbatim, ignoring field.storeFilters', async () => {
      // Filtering now happens entirely in the parent (via the internal endpoint); the child must
      // render whatever repoOptions it is given and must NOT re-filter by field.storeFilters.
      const field: FormField = {
        id: 'repositoryName',
        type: 'repo',
        label: 'Repository',
        storeFilters: { format: 'maven2' },
      };
      const supplied = [
        { value: 'maven-releases', label: 'maven-releases' },
        { value: 'npm-proxy', label: 'npm-proxy' },
      ];
      renderWithTheme(
        <DynamicFormField field={field} value="" onChange={jest.fn()} repoOptions={supplied} />
      );

      openMenu('repositoryName');
      expect(await screen.findByRole('option', { name: 'maven-releases' })).toBeInTheDocument();
      expect(screen.getByRole('option', { name: 'npm-proxy' })).toBeInTheDocument();
    });

    it('returns every option when storeFilters is an empty object', async () => {
      const field: FormField = {
        id: 'repositoryName',
        type: 'repo',
        label: 'Repository',
        storeFilters: {},
      };
      renderWithTheme(
        <DynamicFormField field={field} value="" onChange={jest.fn()} repoOptions={serverFilteredOptions} />
      );

      openMenu('repositoryName');
      expect(await screen.findByRole('option', { name: 'maven-central' })).toBeInTheDocument();
      expect(screen.getByRole('option', { name: 'pypi-proxy' })).toBeInTheDocument();
    });
  });
});

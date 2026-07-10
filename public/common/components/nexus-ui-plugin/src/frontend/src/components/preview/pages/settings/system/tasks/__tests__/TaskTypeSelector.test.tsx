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
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Theme } from '@radix-ui/themes';
import { TaskTypeSelector, DynamicFormFields } from '../TaskTypeSelector';
import { SettingsCombobox } from '../../../../../shared/form';
import { restClient } from '../../../../../../../interface/api';
import { TaskType } from '../types';

jest.mock('../../../../../../../interface/api', () => ({
  restClient: { get: jest.fn().mockResolvedValue([]) },
}));

jest.mock('../../../../../shared/form', () => ({
  SettingsSelect: jest.fn(() => null),
  SettingsCombobox: jest.fn(() => null),
}));


const renderWithTheme = (component: React.ReactElement) => {
  return render(<Theme>{component}</Theme>);
};

describe('TaskTypeSelector', () => {
  const mockTaskTypes: TaskType[] = [
    { id: 'repository.cleanup', name: 'Cleanup repositories', exposed: true, formFields: [] },
    { id: 'repository.rebuild-index', name: 'Rebuild repository index', exposed: true, formFields: [] },
    { id: 'db.backup', name: 'Database backup', exposed: true, formFields: [] },
    { id: 'blobstore.compact', name: 'Compact blob store', exposed: true, formFields: [] },
    { id: 'repository.maven.rebuild-metadata', name: 'Rebuild Maven metadata', exposed: true, formFields: [] },
    { id: 'repository.docker.gc', name: 'Docker garbage collection', exposed: false, formFields: [] },
    { id: 'assetBlob.cleanup', name: 'Cleanup asset blobs', exposed: true, formFields: [] },
  ];

  const mockOnSelect = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('flat table rendering', () => {
    it('renders filter input', () => {
      renderWithTheme(
        <TaskTypeSelector taskTypes={mockTaskTypes} onSelect={mockOnSelect} />
      );

      expect(screen.getByPlaceholderText('Filter task types...')).toBeInTheDocument();
    });

    it('renders all task types in a flat list', () => {
      renderWithTheme(
        <TaskTypeSelector taskTypes={mockTaskTypes} onSelect={mockOnSelect} />
      );

      expect(screen.getByText('Cleanup repositories')).toBeInTheDocument();
      expect(screen.getByText('Rebuild repository index')).toBeInTheDocument();
      expect(screen.getByText('Database backup')).toBeInTheDocument();
      expect(screen.getByText('Compact blob store')).toBeInTheDocument();
      expect(screen.getByText('Rebuild Maven metadata')).toBeInTheDocument();
      expect(screen.getByText('Docker garbage collection')).toBeInTheDocument();
      expect(screen.getByText('Cleanup asset blobs')).toBeInTheDocument();
    });

    it('shows task type count', () => {
      renderWithTheme(
        <TaskTypeSelector taskTypes={mockTaskTypes} onSelect={mockOnSelect} />
      );

      expect(screen.getByText('7 task types')).toBeInTheDocument();
    });

    it('shows category column for each type', () => {
      renderWithTheme(
        <TaskTypeSelector taskTypes={mockTaskTypes} onSelect={mockOnSelect} />
      );

      expect(screen.getAllByText('Admin').length).toBeGreaterThan(0);
      expect(screen.getAllByText('Repository').length).toBeGreaterThan(0);
      expect(screen.getAllByText('Maven').length).toBeGreaterThan(0);
      expect(screen.getAllByText('Docker').length).toBeGreaterThan(0);
      expect(screen.getAllByText('Cleanup').length).toBeGreaterThan(0);
    });

    it('shows PRO badge for non-exposed types', () => {
      renderWithTheme(
        <TaskTypeSelector taskTypes={mockTaskTypes} onSelect={mockOnSelect} />
      );

      expect(screen.getByText('PRO')).toBeInTheDocument();
    });

    it('shows descriptions for task types', () => {
      renderWithTheme(
        <TaskTypeSelector taskTypes={mockTaskTypes} onSelect={mockOnSelect} />
      );

      expect(screen.getByText(/Creates a backup of the embedded database/)).toBeInTheDocument();
      expect(screen.getByText(/Removes components matching cleanup policy criteria/)).toBeInTheDocument();
    });
  });

  describe('filtering', () => {
    it('filters task types by name', async () => {
      renderWithTheme(
        <TaskTypeSelector taskTypes={mockTaskTypes} onSelect={mockOnSelect} />
      );

      const filterInput = screen.getByPlaceholderText('Filter task types...');
      await userEvent.type(filterInput, 'Maven');

      await waitFor(() => {
        expect(screen.getByText('Rebuild Maven metadata')).toBeInTheDocument();
        expect(screen.queryByText('Database backup')).not.toBeInTheDocument();
      });
    });

    it('filters task types by category', async () => {
      renderWithTheme(
        <TaskTypeSelector taskTypes={mockTaskTypes} onSelect={mockOnSelect} />
      );

      const filterInput = screen.getByPlaceholderText('Filter task types...');
      await userEvent.type(filterInput, 'Admin');

      await waitFor(() => {
        expect(screen.getByText('Database backup')).toBeInTheDocument();
        expect(screen.getByText('Compact blob store')).toBeInTheDocument();
        expect(screen.queryByText('Cleanup repositories')).not.toBeInTheDocument();
      });
    });

    it('filters task types by description', async () => {
      renderWithTheme(
        <TaskTypeSelector taskTypes={mockTaskTypes} onSelect={mockOnSelect} />
      );

      const filterInput = screen.getByPlaceholderText('Filter task types...');
      await userEvent.type(filterInput, 'garbage');

      await waitFor(() => {
        expect(screen.getByText('Docker garbage collection')).toBeInTheDocument();
        expect(screen.queryByText('Database backup')).not.toBeInTheDocument();
      });
    });

    it('is case insensitive', async () => {
      renderWithTheme(
        <TaskTypeSelector taskTypes={mockTaskTypes} onSelect={mockOnSelect} />
      );

      const filterInput = screen.getByPlaceholderText('Filter task types...');
      await userEvent.type(filterInput, 'docker');

      await waitFor(() => {
        expect(screen.getByText('Docker garbage collection')).toBeInTheDocument();
      });
    });

    it('shows empty state when no types match filter', async () => {
      renderWithTheme(
        <TaskTypeSelector taskTypes={mockTaskTypes} onSelect={mockOnSelect} />
      );

      const filterInput = screen.getByPlaceholderText('Filter task types...');
      await userEvent.type(filterInput, 'nonexistent');

      await waitFor(() => {
        expect(screen.getByText('No task types match your filter')).toBeInTheDocument();
      });
    });

    it('updates count when filtering', async () => {
      renderWithTheme(
        <TaskTypeSelector taskTypes={mockTaskTypes} onSelect={mockOnSelect} />
      );

      const filterInput = screen.getByPlaceholderText('Filter task types...');
      await userEvent.type(filterInput, 'Admin');

      await waitFor(() => {
        expect(screen.getByText('2 task types')).toBeInTheDocument();
      });
    });
  });

  describe('selection', () => {
    it('calls onSelect when a row is clicked', async () => {
      renderWithTheme(
        <TaskTypeSelector taskTypes={mockTaskTypes} onSelect={mockOnSelect} />
      );

      await userEvent.click(screen.getByText('Database backup'));

      expect(mockOnSelect).toHaveBeenCalledWith(
        expect.objectContaining({
          id: 'db.backup',
          name: 'Database backup',
        })
      );
    });

    it('highlights selected row when selectedType is provided', () => {
      const selected = mockTaskTypes.find(t => t.id === 'db.backup')!;
      renderWithTheme(
        <TaskTypeSelector taskTypes={mockTaskTypes} onSelect={mockOnSelect} selectedType={selected} />
      );

      const selectedRow = screen.getByTestId('task-type-row-db.backup');
      expect(selectedRow.className).toContain('entity-table__row--selected');
    });

    it('does not highlight rows when no selectedType is provided', () => {
      renderWithTheme(
        <TaskTypeSelector taskTypes={mockTaskTypes} onSelect={mockOnSelect} />
      );

      const row = screen.getByTestId('task-type-row-db.backup');
      expect(row.className).not.toContain('entity-table__row--selected');
    });

    it('only highlights the selected row, not others', () => {
      const selected = mockTaskTypes.find(t => t.id === 'db.backup')!;
      renderWithTheme(
        <TaskTypeSelector taskTypes={mockTaskTypes} onSelect={mockOnSelect} selectedType={selected} />
      );

      const selectedRow = screen.getByTestId('task-type-row-db.backup');
      const otherRow = screen.getByTestId('task-type-row-repository.cleanup');

      expect(selectedRow.className).toContain('entity-table__row--selected');
      expect(otherRow.className).not.toContain('entity-table__row--selected');
    });

    it('sets aria-selected on the selected row', () => {
      const selected = mockTaskTypes.find(t => t.id === 'db.backup')!;
      renderWithTheme(
        <TaskTypeSelector taskTypes={mockTaskTypes} onSelect={mockOnSelect} selectedType={selected} />
      );

      const selectedRow = screen.getByTestId('task-type-row-db.backup');
      const otherRow = screen.getByTestId('task-type-row-repository.cleanup');

      expect(selectedRow).toHaveAttribute('aria-selected', 'true');
      expect(otherRow).toHaveAttribute('aria-selected', 'false');
    });
  });

  describe('sorting', () => {
    it('sorts by name ascending by default', () => {
      renderWithTheme(
        <TaskTypeSelector taskTypes={mockTaskTypes} onSelect={mockOnSelect} />
      );

      const rows = screen.getAllByTestId(/^task-type-row-/);
      expect(rows[0]).toHaveAttribute('data-testid', 'task-type-row-assetBlob.cleanup');
    });

    it('toggles sort direction when clicking the same column header', async () => {
      renderWithTheme(
        <TaskTypeSelector taskTypes={mockTaskTypes} onSelect={mockOnSelect} />
      );

      const nameHeader = screen.getByText('Name');
      await userEvent.click(nameHeader);

      const rows = screen.getAllByTestId(/^task-type-row-/);
      expect(rows[0]).toHaveAttribute('data-testid', 'task-type-row-repository.rebuild-index');
    });

    it('sorts by category when category header is clicked', async () => {
      renderWithTheme(
        <TaskTypeSelector taskTypes={mockTaskTypes} onSelect={mockOnSelect} />
      );

      const categoryHeader = screen.getByText('Category');
      await userEvent.click(categoryHeader);

      const rows = screen.getAllByTestId(/^task-type-row-/);
      const firstRowCategory = rows[0].querySelectorAll('td')[1];
      expect(firstRowCategory.textContent).toBe('Admin');
    });
  });

  describe('keyboard navigation', () => {
    it('selects a row when Enter is pressed', () => {
      renderWithTheme(
        <TaskTypeSelector taskTypes={mockTaskTypes} onSelect={mockOnSelect} />
      );

      const row = screen.getByTestId('task-type-row-db.backup');
      fireEvent.keyDown(row, { key: 'Enter' });

      expect(mockOnSelect).toHaveBeenCalledWith(
        expect.objectContaining({ id: 'db.backup' })
      );
    });

    it('selects a row when Space is pressed', () => {
      renderWithTheme(
        <TaskTypeSelector taskTypes={mockTaskTypes} onSelect={mockOnSelect} />
      );

      const row = screen.getByTestId('task-type-row-db.backup');
      fireEvent.keyDown(row, { key: ' ' });

      expect(mockOnSelect).toHaveBeenCalledWith(
        expect.objectContaining({ id: 'db.backup' })
      );
    });
  });

  describe('sorting with filter', () => {
    it('preserves sort direction after applying a filter', async () => {
      renderWithTheme(
        <TaskTypeSelector taskTypes={mockTaskTypes} onSelect={mockOnSelect} />
      );

      // Sort by name descending
      const nameHeader = screen.getByText('Name');
      await userEvent.click(nameHeader);

      // Apply a filter
      const filterInput = screen.getByPlaceholderText('Filter task types...');
      await userEvent.type(filterInput, 'repository');

      await waitFor(() => {
        const rows = screen.getAllByTestId(/^task-type-row-/);
        // Should still be descending: Rebuild repository index before Cleanup repositories
        expect(rows[0]).toHaveAttribute('data-testid', 'task-type-row-repository.rebuild-index');
      });
    });
  });

  describe('empty state', () => {
    it('renders gracefully with an empty taskTypes array', () => {
      renderWithTheme(
        <TaskTypeSelector taskTypes={[]} onSelect={mockOnSelect} />
      );

      expect(screen.getByText('No task types match your filter')).toBeInTheDocument();
      expect(screen.getByText('0 task types')).toBeInTheDocument();
    });
  });

  describe('loading state', () => {
    it('shows loading indicator when loading', () => {
      renderWithTheme(
        <TaskTypeSelector taskTypes={[]} onSelect={mockOnSelect} loading={true} />
      );

      expect(screen.getByText('Loading task types...')).toBeInTheDocument();
    });
  });

  describe('error state', () => {
    it('shows error message when error prop is set', () => {
      renderWithTheme(
        <TaskTypeSelector taskTypes={[]} onSelect={mockOnSelect} error="Failed to load task types" />
      );

      expect(screen.getByText('Failed to load task types')).toBeInTheDocument();
    });
  });
});

describe('DynamicFormFields', () => {
  const mockGet = (restClient.get as jest.Mock);
  const mockCombobox = (SettingsCombobox as jest.Mock);

  function makeTaskType(fieldId: string, taskId = 'test.task') {
    return {
      id: taskId,
      name: 'Test Task',
      exposed: true,
      formFields: [{ id: fieldId, label: fieldId, type: 'string' }],
    };
  }

  function makeTaskTypeWithFields(taskId: string, fields: { id: string; label: string; type: string }[]) {
    return { id: taskId, name: 'Test Task', exposed: true, formFields: fields };
  }

  beforeEach(() => {
    jest.clearAllMocks();
    mockGet.mockImplementation((url: string) => {
      // Internal filtered endpoint
      if (url.startsWith('/service/rest/internal/ui/repositories')) {
        const params = new URL(url, 'http://localhost').searchParams;
        const withAll = params.get('withAll') === 'true';
        const repos = [
          { id: 'apt-hosted-1', name: 'apt-hosted-1' },
          { id: 'apt-proxy-1', name: 'apt-proxy-1' },
        ];
        return Promise.resolve(
          withAll ? [{ id: '*', name: '(All Repositories)' }, ...repos] : repos
        );
      }
      // Public unfiltered endpoint (legacy fallback)
      if (url === '/service/rest/v1/repositories') {
        return Promise.resolve([{ name: 'repo-a' }, { name: 'repo-b' }]);
      }
      return Promise.resolve([]);
    });
  });

  it('moveRepositoryName field does not include (All Repositories)', async () => {
    renderWithTheme(
      <DynamicFormFields
        taskType={makeTaskType('moveRepositoryName')}
        values={{}}
        onChange={jest.fn()}
      />
    );

    await waitFor(() => {
      const repoCalls = mockCombobox.mock.calls.filter(
        ([props]: [{ name: string }]) => props.name === 'moveRepositoryName'
      );
      expect(repoCalls.length).toBeGreaterThan(0);
      const lastCall = repoCalls[repoCalls.length - 1];
      const values = lastCall[0].options.map((o: { value: string }) => o.value);
      expect(values).not.toContain('*');
    });
  });

  it('external.metadata.repository.format renders as text input despite "repository" in the id', async () => {
    // Regression: id contains "repository" so the smart-detection heuristic used to
    // override the explicit metadata and render this as a SettingsCombobox. With an
    // explicit TASK_FIELD_UI entry it must fall through to a plain text input.
    renderWithTheme(
      <DynamicFormFields
        taskType={makeTaskType('external.metadata.repository.format')}
        values={{}}
        onChange={jest.fn()}
      />
    );

    await waitFor(() => {
      const input = screen.getByTestId('input-external.metadata.repository.format');
      expect(input).toBeInTheDocument();
      expect(input.tagName).toBe('INPUT');
      expect(input.getAttribute('type')).toBe('text');
      // Render label uses the metadata label, not the humanized id
      expect(screen.getByText('Repository format')).toBeInTheDocument();
    });

    // SettingsCombobox must NOT have been invoked for this field
    const comboboxCallNames = mockCombobox.mock.calls.map(([props]: [{ name: string }]) => props.name);
    expect(comboboxCallNames).not.toContain('external.metadata.repository.format');
  });

  it('repositoryName on an unfiltered task type includes (All Repositories) as first option', async () => {
    renderWithTheme(
      <DynamicFormFields
        taskType={makeTaskType('repositoryName')}
        values={{}}
        onChange={jest.fn()}
      />
    );

    await waitFor(() => {
      const repoCalls = mockCombobox.mock.calls.filter(
        ([props]: [{ name: string }]) => props.name === 'repositoryName'
      );
      expect(repoCalls.length).toBeGreaterThan(0);
      const lastCall = repoCalls[repoCalls.length - 1];
      const options: { value: string; label: string }[] = lastCall[0].options;
      expect(options[0]).toEqual({ value: '*', label: '(All Repositories)' });
    });
  });

  it('sets aria-required, aria-invalid, and aria-describedby on native string inputs', async () => {
    const taskType = {
      id: 'tags.cleanup',
      name: 'Tags',
      exposed: true,
      formFields: [{ id: 'nameRegex', label: 'Tag Name Pattern', type: 'string', required: false }],
    };
    renderWithTheme(
      <DynamicFormFields
        taskType={taskType}
        values={{ nameRegex: '[unclosed' }}
        onChange={jest.fn()}
        errors={{ nameRegex: 'Tag name regex is not a valid regular expression' }}
      />
    );

    await waitFor(() => {
      const input = screen.getByTestId('input-nameRegex');
      expect(input).toHaveAttribute('aria-required', 'false');
      expect(input).toHaveAttribute('aria-invalid', 'true');
      const describedBy = input.getAttribute('aria-describedby');
      expect(describedBy).toContain(`dynamic-field-error-nameRegex`);
      const errorEl = document.getElementById('dynamic-field-error-nameRegex');
      expect(errorEl).toBeTruthy();
      expect(errorEl?.getAttribute('role')).toBe('alert');
    });
  });

  describe('per-task repository filters', () => {
    beforeEach(() => {
      mockGet.mockImplementation((url: string) => {
        if (url === '/service/rest/v1/repositories') {
          return Promise.resolve([
            { name: 'maven-central', format: 'maven2', type: 'proxy' },
            { name: 'maven-releases', format: 'maven2', type: 'hosted' },
            { name: 'maven-group', format: 'maven2', type: 'group' },
            { name: 'pypi-hosted', format: 'pypi', type: 'hosted' },
            { name: 'pypi-proxy', format: 'pypi', type: 'proxy' },
            { name: 'npm-hosted', format: 'npm', type: 'hosted' },
          ]);
        }
        return Promise.resolve([]);
      });
    });

    function makeTaskTypeWithId(taskTypeId: string, fieldId = 'repositoryName') {
      return {
        id: taskTypeId,
        name: 'Test',
        exposed: true,
        formFields: [{ id: fieldId, label: fieldId, type: 'string' }],
      };
    }

    function valuesPassedTo(fieldId: string): string[] {
      // Use the LAST matching call so we read the state after async repo load resolved.
      const calls = mockCombobox.mock.calls.filter(
        ([props]: [{ name: string }]) => props.name === fieldId
      );
      const call = calls[calls.length - 1];
      return call ? call[0].options.map((o: { value: string }) => o.value) : [];
    }

    it('PublishMavenIndexTask shows only Maven repos plus (All Repositories)', async () => {
      renderWithTheme(
        <DynamicFormFields
          taskType={makeTaskTypeWithId('repository.maven.publish-dotindex')}
          values={{}}
          onChange={jest.fn()}
        />
      );

      await waitFor(() => {
        const values = valuesPassedTo('repositoryName');
        expect(values).toEqual(['*', 'maven-central', 'maven-group', 'maven-releases']);
      });
    });

    it('UnpublishMavenIndexTask shows only Maven repos plus (All Repositories)', async () => {
      renderWithTheme(
        <DynamicFormFields
          taskType={makeTaskTypeWithId('repository.maven.unpublish-dotindex')}
          values={{}}
          onChange={jest.fn()}
        />
      );

      await waitFor(() => {
        const values = valuesPassedTo('repositoryName');
        expect(values).toEqual(['*', 'maven-central', 'maven-group', 'maven-releases']);
      });
    });

    it('RepairMaven2BaseVersionTask shows only hosted Maven repos plus (All Repositories)', async () => {
      renderWithTheme(
        <DynamicFormFields
          taskType={makeTaskTypeWithId('repository.maven.repair-base-version')}
          values={{}}
          onChange={jest.fn()}
        />
      );

      await waitFor(() => {
        const values = valuesPassedTo('repositoryName');
        expect(values).toEqual(['*', 'maven-releases']);
      });
    });

    it('PyPiMarkMetadataForRebuildTask shows only hosted PyPi repos and does NOT include (All Repositories)', async () => {
      renderWithTheme(
        <DynamicFormFields
          taskType={makeTaskTypeWithId('pypi.mark.for.rebuild')}
          values={{}}
          onChange={jest.fn()}
        />
      );

      await waitFor(() => {
        const values = valuesPassedTo('repositoryName');
        expect(values).toEqual(['pypi-hosted']);
      });
    });

    it('ExternalMetadataTask shows every repository but does NOT prepend (All Repositories)', async () => {
      // Descriptor uses a bare RepositoryCombobox — no facet/format/versionPolicy filter
      // and crucially no includeAnEntryForAllRepositories() — so the field shows every
      // repo from /v1/repositories without the "*" wildcard option.
      renderWithTheme(
        <DynamicFormFields
          taskType={makeTaskTypeWithId('external.blobstore.metadata')}
          values={{}}
          onChange={jest.fn()}
        />
      );

      await waitFor(() => {
        const values = valuesPassedTo('repositoryName');
        // All real repos appear (sorted alphabetically by the fetch effect), no "*" entry.
        expect(values).not.toContain('*');
        expect(values).toContain('maven-central');
        expect(values).toContain('pypi-hosted');
      });
    });

    it('RebuildIndexTask keeps the existing "all repositories" behavior when no format filter is configured', async () => {
      renderWithTheme(
        <DynamicFormFields
          taskType={makeTaskTypeWithId('repository.rebuild-index')}
          values={{}}
          onChange={jest.fn()}
        />
      );

      await waitFor(() => {
        const values = valuesPassedTo('repositoryName');
        expect(values[0]).toBe('*');
        expect(values).toContain('maven-central');
        expect(values).toContain('pypi-hosted');
      });
    });

    it('PurgeMavenUnusedSnapshotsTask passes facets AND versionPolicies=!RELEASE to the server', async () => {
      // Maven snapshots descriptor uses includingAnyOfFacets(PurgeUnusedSnapshotsFacet) +
      // excludingAnyOfVersionPolicies(RELEASE). Both must reach the REST endpoint —
      // facet alone would also match Maven RELEASE hosted repos, which the classic UI hides.
      let capturedUrl = '';
      mockGet.mockImplementation((url: string) => {
        if (url.startsWith('/service/rest/internal/ui/repositories')) {
          capturedUrl = url;
          return Promise.resolve([
            { id: 'maven-snapshots', name: 'maven-snapshots' },
            { id: 'maven-public', name: 'maven-public' },
          ]);
        }
        return Promise.resolve([]);
      });

      renderWithTheme(
        <DynamicFormFields
          taskType={makeTaskTypeWithId('repository.maven.purge-unused-snapshots')}
          values={{}}
          onChange={jest.fn()}
        />
      );

      await waitFor(() => {
        expect(capturedUrl).toContain('facets=org.sonatype.nexus.repository.maven.PurgeUnusedSnapshotsFacet');
        expect(capturedUrl).toContain('versionPolicies=%21RELEASE');
        const values = valuesPassedTo('repositoryName');
        expect(values).toEqual(['*', 'maven-snapshots', 'maven-public']);
      });
    });

    it('DockerGCTask fetches by DockerGCFacet (hosted+proxy only — group has no facet)', async () => {
      let capturedUrl = '';
      mockGet.mockImplementation((url: string) => {
        if (url.startsWith('/service/rest/internal/ui/repositories')) {
          capturedUrl = url;
          // Server response would exclude any docker-group since it lacks DockerGCFacet.
          return Promise.resolve([
            { id: 'docker-hosted', name: 'docker-hosted' },
            { id: 'docker-proxy', name: 'docker-proxy' },
          ]);
        }
        return Promise.resolve([]);
      });

      renderWithTheme(
        <DynamicFormFields
          taskType={makeTaskTypeWithId('repository.docker.gc')}
          values={{}}
          onChange={jest.fn()}
        />
      );

      await waitFor(() => {
        expect(capturedUrl).toContain('facets=com.sonatype.nexus.repository.docker.DockerGCFacet');
        const values = valuesPassedTo('repositoryName');
        expect(values).toEqual(['*', 'docker-hosted', 'docker-proxy']);
      });
    });

    it('PurgeUnusedTask fetches the facet-filtered repo list and prepends (All Repositories)', async () => {
      // Backend returns only repos that have PurgeUnusedFacet (proxies + AnsibleGalaxy hosted).
      // The client must hit /internal/ui/repositories?facets=... and use that list verbatim
      // — it cannot approximate this from format/type alone.
      mockGet.mockImplementation((url: string) => {
        if (url.startsWith('/service/rest/internal/ui/repositories')) {
          expect(url).toContain('facets=org.sonatype.nexus.repository.purge.PurgeUnusedFacet');
          return Promise.resolve([
            { id: 'maven-central', name: 'maven-central' },
            { id: 'pypi-proxy', name: 'pypi-proxy' },
          ]);
        }
        if (url === '/service/rest/v1/repositories') {
          return Promise.resolve([
            { name: 'maven-central', format: 'maven2', type: 'proxy' },
            { name: 'maven-releases', format: 'maven2', type: 'hosted' }, // not in purge list
            { name: 'pypi-proxy', format: 'pypi', type: 'proxy' },
          ]);
        }
        return Promise.resolve([]);
      });

      renderWithTheme(
        <DynamicFormFields
          taskType={makeTaskTypeWithId('repository.purge-unused')}
          values={{}}
          onChange={jest.fn()}
        />
      );

      await waitFor(() => {
        const values = valuesPassedTo('repositoryName');
        expect(values).toEqual(['*', 'maven-central', 'pypi-proxy']);
      });
    });
  });

  it('re-fetches repositories when taskType.id changes', async () => {
    const taskA = { id: 'task.type.a', name: 'Task A', exposed: true, formFields: [] };
    const taskB = { id: 'task.type.b', name: 'Task B', exposed: true, formFields: [] };

    const { rerender } = renderWithTheme(
      <DynamicFormFields taskType={taskA} values={{}} onChange={jest.fn()} />
    );

    // Expect 2 calls on first render: repos + blobstores (isCloud is false by default)
    await waitFor(() => expect(mockGet).toHaveBeenCalledTimes(2));

    rerender(
      <Theme>
        <DynamicFormFields taskType={taskB} values={{}} onChange={jest.fn()} />
      </Theme>
    );

    // After taskType.id change, expect 2 more calls (repos + blobstores again)
    await waitFor(() => expect(mockGet).toHaveBeenCalledTimes(4));
  });

  // ────────────────────────────────────────────────────────────────────────────
  // Repository filtering — NEXUS-53043
  // Verifies that DynamicFormFields applies the per-task repository filter
  // (formats/types/includeAll) for each of the 5 rebuild metadata task types,
  // matching the descriptor configuration.
  // ────────────────────────────────────────────────────────────────────────────
  describe('repository filtering per task type (NEXUS-53043)', () => {
    const repoField = { id: 'repositoryName', label: 'Repository', type: 'string' };

    beforeEach(() => {
      mockGet.mockImplementation((url: string) => {
        if (url === '/service/rest/v1/repositories') {
          return Promise.resolve([
            { name: 'apt-hosted-1', format: 'apt', type: 'hosted' },
            { name: 'apt-proxy-1', format: 'apt', type: 'proxy' },
            { name: 'helm-hosted-1', format: 'helm', type: 'hosted' },
            { name: 'helm-proxy-1', format: 'helm', type: 'proxy' },
            { name: 'alpine-hosted-1', format: 'alpine', type: 'hosted' },
            { name: 'alpine-proxy-1', format: 'alpine', type: 'proxy' },
            { name: 'yum-hosted-1', format: 'yum', type: 'hosted' },
            { name: 'yum-proxy-1', format: 'yum', type: 'proxy' },
            { name: 'rubygems-hosted-1', format: 'rubygems', type: 'hosted' },
            { name: 'rubygems-proxy-1', format: 'rubygems', type: 'proxy' },
            { name: 'maven-hosted-1', format: 'maven2', type: 'hosted' },
          ]);
        }
        return Promise.resolve([]);
      });
    });

    function valuesPassedTo(fieldId: string): string[] {
      const calls = mockCombobox.mock.calls.filter(
        ([props]: [{ name: string }]) => props.name === fieldId
      );
      const call = calls[calls.length - 1];
      return call ? call[0].options.map((o: { value: string }) => o.value) : [];
    }

    it('APT task shows hosted+proxy APT repos plus (All Repositories)', async () => {
      renderWithTheme(
        <DynamicFormFields
          taskType={makeTaskTypeWithFields('repository.apt.rebuild.metadata', [repoField])}
          values={{}}
          onChange={jest.fn()}
        />
      );

      await waitFor(() => {
        const values = valuesPassedTo('repositoryName');
        expect(values).toEqual(['*', 'apt-hosted-1', 'apt-proxy-1']);
      });
    });

    it('APT task includes "All Repositories" option as first entry', async () => {
      renderWithTheme(
        <DynamicFormFields
          taskType={makeTaskTypeWithFields('repository.apt.rebuild.metadata', [repoField])}
          values={{}}
          onChange={jest.fn()}
        />
      );

      await waitFor(() => {
        const values = valuesPassedTo('repositoryName');
        expect(values[0]).toBe('*');
      });
    });

    it('Helm task shows only hosted Helm repos plus (All Repositories)', async () => {
      renderWithTheme(
        <DynamicFormFields
          taskType={makeTaskTypeWithFields('repository.helm.rebuild.metadata', [repoField])}
          values={{}}
          onChange={jest.fn()}
        />
      );

      await waitFor(() => {
        const values = valuesPassedTo('repositoryName');
        expect(values).toEqual(['*', 'helm-hosted-1']);
      });
    });

    it('Alpine task shows hosted+proxy Alpine repos plus (All Repositories)', async () => {
      renderWithTheme(
        <DynamicFormFields
          taskType={makeTaskTypeWithFields('repository.alpine.rebuild.metadata', [repoField])}
          values={{}}
          onChange={jest.fn()}
        />
      );

      await waitFor(() => {
        const values = valuesPassedTo('repositoryName');
        expect(values).toEqual(['*', 'alpine-hosted-1', 'alpine-proxy-1']);
      });
    });

    it('Yum task shows only hosted Yum repos and does NOT include (All Repositories)', async () => {
      renderWithTheme(
        <DynamicFormFields
          taskType={makeTaskTypeWithFields('repository.yum.rebuild.metadata', [repoField])}
          values={{}}
          onChange={jest.fn()}
        />
      );

      await waitFor(() => {
        const values = valuesPassedTo('repositoryName');
        expect(values).toEqual(['yum-hosted-1']);
      });
    });

    it('RubyGems task shows only hosted RubyGems repos and does NOT include (All Repositories)', async () => {
      renderWithTheme(
        <DynamicFormFields
          taskType={makeTaskTypeWithFields('repository.ruby.rebuild.versions', [repoField])}
          values={{}}
          onChange={jest.fn()}
        />
      );

      await waitFor(() => {
        const values = valuesPassedTo('repositoryName');
        expect(values).toEqual(['rubygems-hosted-1']);
      });
    });

    it('an unrelated task type falls back to all repositories with (All Repositories)', async () => {
      renderWithTheme(
        <DynamicFormFields
          taskType={makeTaskType('repositoryName', 'repository.cleanup')}
          values={{}}
          onChange={jest.fn()}
        />
      );

      await waitFor(() => {
        const values = valuesPassedTo('repositoryName');
        expect(values[0]).toBe('*');
        expect(values).toContain('apt-hosted-1');
        expect(values).toContain('maven-hosted-1');
      });
    });
  });

  // ────────────────────────────────────────────────────────────────────────────
  // Checkbox field rendering — NEXUS-53043
  // ────────────────────────────────────────────────────────────────────────────
  describe('checkbox field rendering', () => {
    function makeCheckboxTaskType(taskId: string, checkboxFieldId: string, checkboxLabel: string) {
      return makeTaskTypeWithFields(taskId, [
        { id: 'repositoryName', label: 'Repository', type: 'string' },
        { id: checkboxFieldId, label: checkboxLabel, type: 'checkbox' },
      ]);
    }

    it('Yum task renders yumMetadataCaching checkbox with label from TASK_FIELD_UI ("Soft repair")', async () => {
      renderWithTheme(
        <DynamicFormFields
          taskType={makeCheckboxTaskType(
            'repository.yum.rebuild.metadata',
            'yumMetadataCaching',
            'Soft repair'
          )}
          values={{ yumMetadataCaching: 'false' }}
          onChange={jest.fn()}
        />
      );

      await waitFor(() => {
        expect(screen.getByLabelText('Soft repair')).toBeInTheDocument();
      });
    });

    it('Yum task renders yumMetadataCaching checkbox unchecked when value is "false"', async () => {
      renderWithTheme(
        <DynamicFormFields
          taskType={makeCheckboxTaskType(
            'repository.yum.rebuild.metadata',
            'yumMetadataCaching',
            'Soft repair'
          )}
          values={{ yumMetadataCaching: 'false' }}
          onChange={jest.fn()}
        />
      );

      await waitFor(() => {
        const checkbox = screen.getByTestId('input-yumMetadataCaching') as HTMLInputElement;
        expect(checkbox.checked).toBe(false);
      });
    });

    it('Yum task renders yumMetadataCaching checkbox checked when value is "true"', async () => {
      renderWithTheme(
        <DynamicFormFields
          taskType={makeCheckboxTaskType(
            'repository.yum.rebuild.metadata',
            'yumMetadataCaching',
            'Soft repair'
          )}
          values={{ yumMetadataCaching: 'true' }}
          onChange={jest.fn()}
        />
      );

      await waitFor(() => {
        const checkbox = screen.getByTestId('input-yumMetadataCaching') as HTMLInputElement;
        expect(checkbox.checked).toBe(true);
      });
    });

    it('RubyGems task renders forceRebuild checkbox with correct label', async () => {
      renderWithTheme(
        <DynamicFormFields
          taskType={makeCheckboxTaskType(
            'repository.ruby.rebuild.versions',
            'forceRebuild',
            'Force rebuild'
          )}
          values={{ forceRebuild: 'false' }}
          onChange={jest.fn()}
        />
      );

      await waitFor(() => {
        expect(screen.getByLabelText('Force rebuild')).toBeInTheDocument();
      });
    });

    it('APT task renders rebuildAptMetadataFullRebuild checkbox with correct label', async () => {
      renderWithTheme(
        <DynamicFormFields
          taskType={makeTaskTypeWithFields('repository.apt.rebuild.metadata', [
            { id: 'repositoryName', label: 'Repository', type: 'string' },
            { id: 'rebuildAptMetadataFullRebuild', label: 'Full rebuild (hosted only)', type: 'checkbox' },
            { id: 'resetProxyMetadata', label: 'Reset proxy metadata', type: 'checkbox' },
          ])}
          values={{ rebuildAptMetadataFullRebuild: 'false', resetProxyMetadata: 'false' }}
          onChange={jest.fn()}
        />
      );

      await waitFor(() => {
        expect(screen.getByLabelText('Full rebuild (hosted only)')).toBeInTheDocument();
        expect(screen.getByLabelText('Reset proxy metadata')).toBeInTheDocument();
      });
    });

    it('checkbox onChange fires with "true" when toggled on', async () => {
      const handleChange = jest.fn();
      renderWithTheme(
        <DynamicFormFields
          taskType={makeCheckboxTaskType(
            'repository.yum.rebuild.metadata',
            'yumMetadataCaching',
            'Soft repair'
          )}
          values={{ yumMetadataCaching: 'false' }}
          onChange={handleChange}
        />
      );

      await waitFor(() => screen.getByTestId('input-yumMetadataCaching'));

      fireEvent.click(screen.getByTestId('input-yumMetadataCaching'));
      expect(handleChange).toHaveBeenCalledWith('yumMetadataCaching', 'true');
    });

    it('checkbox onChange fires with "false" when toggled off', async () => {
      const handleChange = jest.fn();
      renderWithTheme(
        <DynamicFormFields
          taskType={makeCheckboxTaskType(
            'repository.yum.rebuild.metadata',
            'yumMetadataCaching',
            'Soft repair'
          )}
          values={{ yumMetadataCaching: 'true' }}
          onChange={handleChange}
        />
      );

      await waitFor(() => screen.getByTestId('input-yumMetadataCaching'));

      fireEvent.click(screen.getByTestId('input-yumMetadataCaching'));
      expect(handleChange).toHaveBeenCalledWith('yumMetadataCaching', 'false');
    });
  });

  // ────────────────────────────────────────────────────────────────────────────
  // APT conditional checkbox visibility — NEXUS-53043
  // Mirrors Classic/ExtJS updateAptRebuildCheckboxVisibility behavior:
  //   hosted → show rebuildAptMetadataFullRebuild, hide resetProxyMetadata
  //   proxy  → hide rebuildAptMetadataFullRebuild, show resetProxyMetadata
  //   * / no selection → show both
  // ────────────────────────────────────────────────────────────────────────────
  describe('APT conditional checkbox visibility (NEXUS-53043)', () => {
    const aptTaskType = makeTaskTypeWithFields('repository.apt.rebuild.metadata', [
      { id: 'repositoryName', label: 'Repository', type: 'string' },
      { id: 'rebuildAptMetadataFullRebuild', label: 'Full rebuild (hosted only)', type: 'checkbox' },
      { id: 'resetProxyMetadata', label: 'Reset proxy metadata', type: 'checkbox' },
    ]);

    /**
     * Mock /v1/repositories to return APT hosted + proxy entries with format/type fields.
     * Field visibility is derived client-side by looking up the selected repo's `type`
     * in this list, so the mock is parameterless — both repos are always present and
     * the test selects one via the `repositoryName` value.
     */
    function configureAptRepoListMock() {
      mockGet.mockImplementation((url: string) => {
        if (url === '/service/rest/v1/repositories') {
          return Promise.resolve([
            { name: 'apt-hosted-1', format: 'apt', type: 'hosted' },
            { name: 'apt-proxy-1', format: 'apt', type: 'proxy' },
          ]);
        }
        return Promise.resolve([]);
      });
    }

    it('shows both checkboxes when no repositoryName is selected', async () => {
      renderWithTheme(
        <DynamicFormFields taskType={aptTaskType} values={{}} onChange={jest.fn()} />
      );
      await waitFor(() => {
        expect(screen.getByLabelText('Full rebuild (hosted only)')).toBeInTheDocument();
        expect(screen.getByLabelText('Reset proxy metadata')).toBeInTheDocument();
      });
    });

    it('shows both checkboxes when All Repositories (*) is selected', async () => {
      renderWithTheme(
        <DynamicFormFields taskType={aptTaskType} values={{ repositoryName: '*' }} onChange={jest.fn()} />
      );
      await waitFor(() => {
        expect(screen.getByLabelText('Full rebuild (hosted only)')).toBeInTheDocument();
        expect(screen.getByLabelText('Reset proxy metadata')).toBeInTheDocument();
      });
    });

    it('shows only rebuildAptMetadataFullRebuild when a hosted repo is selected', async () => {
      configureAptRepoListMock();
      renderWithTheme(
        <DynamicFormFields
          taskType={aptTaskType}
          values={{ repositoryName: 'apt-hosted-1' }}
          onChange={jest.fn()}
        />
      );
      await waitFor(() => {
        expect(screen.getByLabelText('Full rebuild (hosted only)')).toBeInTheDocument();
        expect(screen.queryByLabelText('Reset proxy metadata')).not.toBeInTheDocument();
      });
    });

    it('shows only resetProxyMetadata when a proxy repo is selected', async () => {
      configureAptRepoListMock();
      renderWithTheme(
        <DynamicFormFields
          taskType={aptTaskType}
          values={{ repositoryName: 'apt-proxy-1' }}
          onChange={jest.fn()}
        />
      );
      await waitFor(() => {
        expect(screen.queryByLabelText('Full rebuild (hosted only)')).not.toBeInTheDocument();
        expect(screen.getByLabelText('Reset proxy metadata')).toBeInTheDocument();
      });
    });

    it('shows both checkboxes when the repo list fetch fails (safe fallback)', async () => {
      mockGet.mockImplementation((url: string) => {
        if (url === '/service/rest/v1/repositories') {
          return Promise.reject(new Error('Network error'));
        }
        return Promise.resolve([]);
      });

      renderWithTheme(
        <DynamicFormFields
          taskType={aptTaskType}
          values={{ repositoryName: 'apt-hosted-1' }}
          onChange={jest.fn()}
        />
      );

      // Both visible immediately (before fetch resolves) and remain visible after failure
      expect(screen.getByLabelText('Full rebuild (hosted only)')).toBeInTheDocument();
      expect(screen.getByLabelText('Reset proxy metadata')).toBeInTheDocument();
    });

    it('resolves hosted visibility correctly when loading an existing saved task (create flow)', async () => {
      configureAptRepoListMock();
      renderWithTheme(
        <DynamicFormFields
          taskType={aptTaskType}
          values={{ repositoryName: 'apt-hosted-1', rebuildAptMetadataFullRebuild: 'true' }}
          onChange={jest.fn()}
        />
      );
      await waitFor(() => {
        expect(screen.getByLabelText('Full rebuild (hosted only)')).toBeInTheDocument();
        expect(screen.queryByLabelText('Reset proxy metadata')).not.toBeInTheDocument();
      });
    });

    it('resolves proxy visibility correctly when loading an existing saved task (edit flow)', async () => {
      configureAptRepoListMock();
      renderWithTheme(
        <DynamicFormFields
          taskType={aptTaskType}
          values={{ repositoryName: 'apt-proxy-1', resetProxyMetadata: 'true' }}
          onChange={jest.fn()}
        />
      );
      await waitFor(() => {
        expect(screen.queryByLabelText('Full rebuild (hosted only)')).not.toBeInTheDocument();
        expect(screen.getByLabelText('Reset proxy metadata')).toBeInTheDocument();
      });
    });

    it('does not call the per-repo detail endpoint for task types without dependsOnRepo fields', async () => {
      renderWithTheme(
        <DynamicFormFields
          taskType={makeTaskTypeWithFields('repository.helm.rebuild.metadata', [
            { id: 'repositoryName', label: 'Repository', type: 'string' },
            { id: 'rebuildHelmMetadataFullRebuild', label: 'Full rebuild', type: 'checkbox' },
          ])}
          values={{ repositoryName: 'helm-hosted-1' }}
          onChange={jest.fn()}
        />
      );

      // Wait for the component to stabilize after the list fetch
      await waitFor(() => expect(screen.getByLabelText('Full rebuild')).toBeInTheDocument());

      const calledUrls = mockGet.mock.calls.map(([url]: [string]) => url);
      expect(calledUrls.some((u: string) => u.includes('/repository/'))).toBe(false);
    });

    // ── Regression tests: NEXUS-53043 combobox options must never be filtered by visibleForRepoTypes ──
    // The repo SELECTOR filter (formats=[apt], types=[hosted,proxy]) comes from TASK_TYPE_REPO_FILTERS
    // and is fixed at the task-type level. visibleForRepoTypes only controls checkbox field visibility;
    // it must never restrict which repositories appear in the RepositoryCombobox.

    it('repo combobox includes both hosted and proxy APT repos when a hosted repo is selected', async () => {
      configureAptRepoListMock();
      renderWithTheme(
        <DynamicFormFields
          taskType={aptTaskType}
          values={{ repositoryName: 'apt-hosted-1' }}
          onChange={jest.fn()}
        />
      );

      await waitFor(() => {
        const repoCalls = mockCombobox.mock.calls.filter(
          ([props]: [{ name: string }]) => props.name === 'repositoryName'
        );
        expect(repoCalls.length).toBeGreaterThan(0);
        const lastCall = repoCalls[repoCalls.length - 1];
        const optionValues: string[] = lastCall[0].options.map((o: { value: string }) => o.value);
        // Both hosted and proxy must be present — visibleForRepoTypes must NOT filter the repo list
        expect(optionValues).toContain('apt-hosted-1');
        expect(optionValues).toContain('apt-proxy-1');
      });
    });

    it('repo combobox includes both hosted and proxy APT repos when a proxy repo is selected', async () => {
      configureAptRepoListMock();
      renderWithTheme(
        <DynamicFormFields
          taskType={aptTaskType}
          values={{ repositoryName: 'apt-proxy-1' }}
          onChange={jest.fn()}
        />
      );

      await waitFor(() => {
        const repoCalls = mockCombobox.mock.calls.filter(
          ([props]: [{ name: string }]) => props.name === 'repositoryName'
        );
        expect(repoCalls.length).toBeGreaterThan(0);
        const lastCall = repoCalls[repoCalls.length - 1];
        const optionValues: string[] = lastCall[0].options.map((o: { value: string }) => o.value);
        // Both hosted and proxy must be present — visibleForRepoTypes must NOT filter the repo list
        expect(optionValues).toContain('apt-hosted-1');
        expect(optionValues).toContain('apt-proxy-1');
      });
    });
  });
});

describe('DynamicFormFields with includeFormatEntries', () => {
  const mockGet = restClient.get as jest.Mock;
  const mockCombobox = SettingsCombobox as jest.Mock;

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('fetches with-formats repo list when a field has includeFormatEntries', async () => {
    mockGet.mockImplementation((url: string) => {
      if (url === '/service/rest/v1/repositories') {
        return Promise.resolve([{name: 'maven-public'}, {name: 'npm-hosted'}]);
      }
      if (url === '/service/rest/internal/ui/repositories?withFormats=true&withAll=true') {
        // Backend omits (All Repositories) in this mock to prove the frontend adds it regardless
        return Promise.resolve([
          {id: '*-maven2', name: '(All maven2 Repositories)'},
          {id: '*-npm', name: '(All npm Repositories)'},
          {id: 'maven-public', name: 'maven-public'},
          {id: 'npm-hosted', name: 'npm-hosted'},
        ]);
      }
      return Promise.resolve([]);
    });

    const taskType = {
      id: 'tags.cleanup',
      name: 'Admin - Cleanup tags',
      exposed: true,
      formFields: [
        {id: 'restrictComponentDelete', label: 'Restrict Delete', type: 'repo', required: false, initialValue: ''},
      ],
    };

    renderWithTheme(
      <DynamicFormFields taskType={taskType as any} values={{}} onChange={jest.fn()} />,
    );

    await waitFor(() => {
      expect(mockGet).toHaveBeenCalledWith('/service/rest/internal/ui/repositories?withFormats=true&withAll=true');
    });

    await waitFor(() => {
      const comboboxCalls = mockCombobox.mock.calls.filter(
        ([props]: [{name: string}]) => props.name === 'restrictComponentDelete',
      );
      expect(comboboxCalls.length).toBeGreaterThan(0);
      const lastOptions: {value: string; label: string}[] = comboboxCalls[comboboxCalls.length - 1][0].options;
      // (All Repositories) must be first, added by the frontend regardless of backend response
      expect(lastOptions[0]).toEqual({value: '*', label: '(All Repositories)'});
      expect(lastOptions.some((o) => o.value === '*-maven2')).toBe(true);
      expect(lastOptions.some((o) => o.label === '(All maven2 Repositories)')).toBe(true);
    });
  });

  it('passes loading=true to the combobox while the with-formats fetch is in-flight and loading=false after it resolves', async () => {
    let resolveWithFormats!: (value: {id: string; name: string}[]) => void;
    const withFormatsPromise = new Promise<{id: string; name: string}[]>((resolve) => {
      resolveWithFormats = resolve;
    });

    mockGet.mockImplementation((url: string) => {
      if (url === '/service/rest/internal/ui/repositories?withFormats=true&withAll=true') {
        return withFormatsPromise;
      }
      return Promise.resolve([]);
    });

    const taskType = {
      id: 'tags.cleanup',
      name: 'Admin - Cleanup tags',
      exposed: true,
      formFields: [
        {id: 'restrictComponentDelete', label: 'Restrict Delete', type: 'repo', required: false, initialValue: ''},
      ],
    };

    renderWithTheme(
      <DynamicFormFields taskType={taskType as any} values={{}} onChange={jest.fn()} />,
    );

    // While the fetch is in-flight, loading should be true
    await waitFor(() => {
      const calls = mockCombobox.mock.calls.filter(
        ([props]: [{name: string}]) => props.name === 'restrictComponentDelete',
      );
      expect(calls.length).toBeGreaterThan(0);
      expect(calls[calls.length - 1][0].loading).toBe(true);
    });

    // Resolve the fetch and confirm loading is false
    resolveWithFormats([{id: '*-maven2', name: '(All maven2 Repositories)'}]);

    await waitFor(() => {
      const calls = mockCombobox.mock.calls.filter(
        ([props]: [{name: string}]) => props.name === 'restrictComponentDelete',
      );
      expect(calls[calls.length - 1][0].loading).toBe(false);
    });
  });

  it('does NOT call the with-formats endpoint when no field needs it', async () => {
    mockGet.mockResolvedValue([]);

    const taskType = {
      id: 'repository.cleanup',
      name: 'Admin - Cleanup repositories',
      exposed: true,
      formFields: [
        {id: 'repositoryName', label: 'Repository', type: 'repo', required: false, initialValue: ''},
      ],
    };

    renderWithTheme(
      <DynamicFormFields taskType={taskType as any} values={{}} onChange={jest.fn()} />,
    );

    await waitFor(() => {
      expect(mockGet).toHaveBeenCalledWith('/service/rest/v1/repositories');
    });

    expect(mockGet).not.toHaveBeenCalledWith('/service/rest/internal/ui/repositories?withFormats=true&withAll=true');
  });
});

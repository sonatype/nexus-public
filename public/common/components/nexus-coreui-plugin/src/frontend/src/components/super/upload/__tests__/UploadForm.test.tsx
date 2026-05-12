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
import userEvent from '@testing-library/user-event';
import { Theme } from '@radix-ui/themes';

import { UploadFormContainer } from '../UploadFormContainer';

// Mock the hooks
jest.mock('../hooks/useUploadDefinition');
jest.mock('../hooks/useUploadForm');
jest.mock('../hooks/useUploadableRepositories');

import { useUploadDefinition } from '../hooks/useUploadDefinition';
import { useUploadForm } from '../hooks/useUploadForm';
import { useUploadableRepositories } from '../hooks/useUploadableRepositories';

const mockUseUploadDefinition = useUploadDefinition as jest.MockedFunction<typeof useUploadDefinition>;
const mockUseUploadForm = useUploadForm as jest.MockedFunction<typeof useUploadForm>;
const mockUseUploadableRepositories = useUploadableRepositories as jest.MockedFunction<typeof useUploadableRepositories>;

// Mock UI Router
const mockGo = jest.fn();
jest.mock('@uirouter/react', () => ({
  useCurrentStateAndParams: () => ({
    params: { repoName: 'maven-releases' },
  }),
  useRouter: () => ({
    stateService: {
      go: mockGo,
    },
  }),
}));

// Mock @sonatype/nexus-ui-plugin without requireActual to avoid circular init
jest.mock('@sonatype/nexus-ui-plugin', () => ({
  restClient: {
    get: jest.fn(),
    post: jest.fn(),
    put: jest.fn(),
    delete: jest.fn(),
  },
  parseApiError: jest.fn((err) => ({
    message: err?.message || 'An error occurred',
  })),
  APIConstants: {
    REST: {
      INTERNAL: {
        UPLOAD: 'service/rest/internal/ui/upload/',
      },
    },
    EXT: {
      UPLOAD: {
        ACTION: 'coreui_Upload',
        METHODS: {
          GET_UPLOAD_DEFINITIONS: 'getUploadDefinitions',
        },
      },
      REPOSITORY: {
        ACTION: 'coreui_Repository',
        METHODS: {
          READ_REFERENCES: 'readReferences',
        },
      },
    },
  },
  ExtAPIUtils: {
    extAPIRequest: jest.fn(),
    checkForErrorAndExtract: jest.fn(),
  },
  ExtJS: {
    showSuccessMessage: jest.fn(),
    showErrorMessage: jest.fn(),
    setDirtyStatus: jest.fn(),
    checkPermission: jest.fn().mockReturnValue(true),
  },
  Utils: {
    isBlank: jest.fn((v) => !v || !v.trim()),
    notBlank: jest.fn((v) => v && v.trim()),
  },
  FormUtils: {
    fieldProps: jest.fn(() => ({})),
    saveTooltip: jest.fn(() => ''),
    discardTooltip: jest.fn(() => ''),
  },
}));

// Mock axios
jest.mock('axios', () => ({
  post: jest.fn(),
}));

const defaultDefinitionResult = {
  loading: false,
  error: null,
  uploadDefinition: {
    format: 'maven2',
    uiUpload: true,
    multipleUpload: true,
  },
  repositorySettings: {
    name: 'maven-releases',
    format: 'maven2',
    type: 'hosted' as const,
    url: 'http://localhost:8081/repository/maven-releases',
  },
  componentFields: [
    { name: 'groupId', type: 'STRING' as const, displayName: 'Group ID', group: 'Component coordinates', optional: false },
    { name: 'artifactId', type: 'STRING' as const, displayName: 'Artifact ID', group: 'Component coordinates', optional: false },
    { name: 'version', type: 'STRING' as const, displayName: 'Version', group: 'Component coordinates', optional: false },
  ],
  componentFieldsByGroup: {
    'Component coordinates': [
      { name: 'groupId', type: 'STRING' as const, displayName: 'Group ID', group: 'Component coordinates', optional: false },
      { name: 'artifactId', type: 'STRING' as const, displayName: 'Artifact ID', group: 'Component coordinates', optional: false },
      { name: 'version', type: 'STRING' as const, displayName: 'Version', group: 'Component coordinates', optional: false },
    ],
  },
  assetFields: [
    { name: 'extension', type: 'STRING' as const, displayName: 'Extension', optional: false },
  ],
  multipleUpload: true,
  regexMap: null,
  refetch: jest.fn(),
};

const defaultFormResult = {
  formData: {
    assets: [{ file: null, extension: '' }],
    componentFields: { groupId: '', artifactId: '', version: '' },
  },
  validationErrors: {},
  isSubmitting: false,
  isValid: true,
  isDirty: false,
  setAssetFile: jest.fn(),
  setAssetField: jest.fn(),
  addAsset: jest.fn(),
  removeAsset: jest.fn(),
  setComponentField: jest.fn(),
  validate: jest.fn().mockReturnValue(true),
  submit: jest.fn().mockResolvedValue({ success: true, componentName: 'test-component' }),
  reset: jest.fn(),
};

const renderWithTheme = (component: React.ReactElement) => {
  return render(<Theme>{component}</Theme>);
};

describe('UploadFormContainer', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockUseUploadDefinition.mockReturnValue(defaultDefinitionResult);
    mockUseUploadForm.mockReturnValue(defaultFormResult);
    mockUseUploadableRepositories.mockReturnValue({
      repositories: [
        { name: 'maven-releases', format: 'maven2', url: 'http://localhost:8081/repository/maven-releases' },
        { name: 'npm-hosted', format: 'npm', url: 'http://localhost:8081/repository/npm-hosted' },
      ],
      loading: false,
      error: null,
      filterText: '',
      selectedFormats: [],
      availableFormats: ['maven2', 'npm'],
      hasActiveFilters: false,
      sortColumn: null,
      sortDirection: null,
      handleSort: jest.fn(),
      handleFilterChange: jest.fn(),
      clearFilter: jest.fn(),
      toggleFormat: jest.fn(),
      clearAllFilters: jest.fn(),
      refetch: jest.fn(),
    });
  });

  it('renders loading state', () => {
    mockUseUploadDefinition.mockReturnValue({
      ...defaultDefinitionResult,
      loading: true,
    });

    renderWithTheme(<UploadFormContainer />);

    expect(screen.getByText('Loading upload configuration...')).toBeInTheDocument();
  });

  it('renders error state', () => {
    mockUseUploadDefinition.mockReturnValue({
      ...defaultDefinitionResult,
      loading: false,
      error: 'Repository not found',
    });

    renderWithTheme(<UploadFormContainer />);

    expect(screen.getByText('Repository not found')).toBeInTheDocument();
    expect(screen.getByText('Back to Upload')).toBeInTheDocument();
  });

  it('renders repository header', () => {
    renderWithTheme(<UploadFormContainer />);

    expect(screen.getByText('Upload to maven-releases')).toBeInTheDocument();
    // Format is shown as "Maven" for maven2 format
    expect(screen.getByText('Maven')).toBeInTheDocument();
  });

  it('renders file dropzone', () => {
    renderWithTheme(<UploadFormContainer />);

    // File dropzone should be present with aria-label
    expect(screen.getByLabelText('File')).toBeInTheDocument();
  });

  it('renders component field groups', () => {
    renderWithTheme(<UploadFormContainer />);

    expect(screen.getByText(/Component coordinates/i)).toBeInTheDocument();
    expect(screen.getAllByText(/Group ID/i).length).toBeGreaterThan(0);
    expect(screen.getAllByText(/Artifact ID/i).length).toBeGreaterThan(0);
    expect(screen.getAllByText(/Version/i).length).toBeGreaterThan(0);
  });

  it('renders upload button in action bar', () => {
    renderWithTheme(<UploadFormContainer />);

    expect(screen.getByTestId('form-submit')).toBeInTheDocument();
    expect(screen.getByTestId('form-submit')).toHaveTextContent(/upload component/i);
  });

  it('navigates back when back button is clicked', async () => {
    renderWithTheme(<UploadFormContainer />);

    const backButton = screen.getByText('Back to Upload');
    await userEvent.click(backButton);

    expect(mockGo).toHaveBeenCalledWith('preview.browse.upload.list');
  });

  it('renders add asset button when multipleUpload is true', () => {
    renderWithTheme(<UploadFormContainer />);

    expect(screen.getByText('Add Another Asset')).toBeInTheDocument();
  });

  it('does not render add asset button when multipleUpload is false', () => {
    mockUseUploadDefinition.mockReturnValue({
      ...defaultDefinitionResult,
      multipleUpload: false,
    });

    renderWithTheme(<UploadFormContainer />);

    expect(screen.queryByText('Add Another Asset')).not.toBeInTheDocument();
  });

  it('calls addAsset when add asset button is clicked', async () => {
    const mockAddAsset = jest.fn();
    mockUseUploadForm.mockReturnValue({
      ...defaultFormResult,
      addAsset: mockAddAsset,
    });

    renderWithTheme(<UploadFormContainer />);

    const addButton = screen.getByText('Add Another Asset');
    await userEvent.click(addButton);

    expect(mockAddAsset).toHaveBeenCalled();
  });

  it('renders remove asset button when multiple assets exist', () => {
    mockUseUploadForm.mockReturnValue({
      ...defaultFormResult,
      formData: {
        assets: [
          { file: null, extension: '' },
          { file: null, extension: '' },
        ],
        componentFields: { groupId: '', artifactId: '', version: '' },
      },
    });

    renderWithTheme(<UploadFormContainer />);

    const removeButtons = screen.getAllByLabelText('Remove asset');
    expect(removeButtons).toHaveLength(2);
  });

  it('calls removeAsset when remove button is clicked', async () => {
    const mockRemoveAsset = jest.fn();
    mockUseUploadForm.mockReturnValue({
      ...defaultFormResult,
      formData: {
        assets: [
          { file: null, extension: '' },
          { file: null, extension: '' },
        ],
        componentFields: { groupId: '', artifactId: '', version: '' },
      },
      removeAsset: mockRemoveAsset,
    });

    renderWithTheme(<UploadFormContainer />);

    const removeButtons = screen.getAllByLabelText('Remove asset');
    await userEvent.click(removeButtons[0]);

    expect(mockRemoveAsset).toHaveBeenCalledWith(0);
  });

  it('renders validation errors for asset fields', () => {
    mockUseUploadForm.mockReturnValue({
      ...defaultFormResult,
      validationErrors: {
        assets: [{ extension: 'This field is required' }],
        componentFields: {},
      },
    });

    renderWithTheme(<UploadFormContainer />);

    expect(screen.getByText('This field is required')).toBeInTheDocument();
  });

  it('renders validation errors for component fields', () => {
    mockUseUploadForm.mockReturnValue({
      ...defaultFormResult,
      validationErrors: {
        assets: [{}],
        componentFields: { groupId: 'This field is required' },
      },
    });

    renderWithTheme(<UploadFormContainer />);

    expect(screen.getByText('This field is required')).toBeInTheDocument();
  });

  it('shows loading state during submission', () => {
    mockUseUploadForm.mockReturnValue({
      ...defaultFormResult,
      isSubmitting: true,
      isDirty: true,
    });

    renderWithTheme(<UploadFormContainer />);

    const submitButton = screen.getByTestId('form-submit');
    expect(submitButton).toHaveAttribute('aria-busy', 'true');
  });

  it('disables buttons during submission', () => {
    mockUseUploadForm.mockReturnValue({
      ...defaultFormResult,
      isSubmitting: true,
      isDirty: true,
    });

    renderWithTheme(<UploadFormContainer />);

    const uploadButton = screen.getByTestId('form-submit');
    const cancelButton = screen.getByTestId('form-cancel');

    expect(uploadButton).toBeDisabled();
    expect(cancelButton).toBeDisabled();
  });

  it('calls submit when form is submitted', async () => {
    const mockSubmit = jest.fn().mockResolvedValue({ success: true, componentName: 'test' });
    const mockFile = new File(['content'], 'test.jar', { type: 'application/java-archive' });
    mockUseUploadForm.mockReturnValue({
      ...defaultFormResult,
      formData: {
        assets: [{ file: mockFile, extension: 'jar' }],
        componentFields: { groupId: 'com.test', artifactId: 'test', version: '1.0.0' },
      },
      isDirty: true,
      submit: mockSubmit,
    });

    renderWithTheme(<UploadFormContainer />);

    const uploadButton = screen.getByTestId('form-submit');
    await userEvent.click(uploadButton);

    expect(mockSubmit).toHaveBeenCalled();
  });

  it('renders select field type correctly', () => {
    const fields = [
      {
        name: 'packaging',
        type: 'SELECT' as const,
        displayName: 'Packaging',
        group: 'Options',
        optional: false,
        selectOptions: ['jar', 'war', 'pom'],
      },
    ];
    mockUseUploadDefinition.mockReturnValue({
      ...defaultDefinitionResult,
      componentFields: fields,
      componentFieldsByGroup: {
        Options: fields,
      },
    });
    mockUseUploadForm.mockReturnValue({
      ...defaultFormResult,
      formData: {
        assets: [{ file: null, extension: '' }],
        componentFields: { packaging: '' },
      },
    });

    renderWithTheme(<UploadFormContainer />);

    expect(screen.getByText('Options')).toBeInTheDocument();
    expect(screen.getAllByText(/Packaging/i).length).toBeGreaterThan(0);
  });

  it('renders boolean field type correctly', () => {
    const fields = [
      {
        name: 'generate-pom',
        type: 'BOOLEAN' as const,
        displayName: 'Generate a POM file',
        group: 'Options',
        optional: true,
      },
    ];
    mockUseUploadDefinition.mockReturnValue({
      ...defaultDefinitionResult,
      componentFields: fields,
      componentFieldsByGroup: {
        Options: fields,
      },
    });
    mockUseUploadForm.mockReturnValue({
      ...defaultFormResult,
      formData: {
        assets: [{ file: null, extension: '' }],
        componentFields: { 'generate-pom': false },
      },
    });

    renderWithTheme(<UploadFormContainer />);

    // The boolean field renders as a checkbox - look for the checkbox role
    expect(screen.getByRole('checkbox')).toBeInTheDocument();
    expect(screen.getByText('Options')).toBeInTheDocument();
  });

  it('renders Maven POM notice when hasPom is true', () => {
    const mockFile = new File(['content'], 'test.pom');
    mockUseUploadForm.mockReturnValue({
      ...defaultFormResult,
      formData: {
        assets: [{ file: mockFile, extension: '' }],
        componentFields: {},
      },
    });

    renderWithTheme(<UploadFormContainer />);

    expect(screen.getByText(/extracted from the provided POM file/i)).toBeInTheDocument();
  });

  it('calls setComponentField when text field changes', async () => {
    renderWithTheme(<UploadFormContainer />);

    const groupIdInput = screen.getByTestId('input-groupId');
    await userEvent.type(groupIdInput, 'com.test');

    expect(mockUseUploadForm().setComponentField).toHaveBeenCalled();
  });

  it('renders asset fields', () => {
    renderWithTheme(<UploadFormContainer />);

    expect(screen.getAllByText(/Extension/i).length).toBeGreaterThan(0);
  });

  it('calls setAssetField when asset field changes', async () => {
    const mockSetAssetField = jest.fn();
    mockUseUploadForm.mockReturnValue({
      ...defaultFormResult,
      setAssetField: mockSetAssetField,
    });

    renderWithTheme(<UploadFormContainer />);

    // Find the extension input specifically in the Assets section or use the last one
    const extensionInputs = screen.getAllByTestId('input-extension');
    const extensionInput = extensionInputs[extensionInputs.length - 1];
    await userEvent.type(extensionInput, 'jar');

    expect(mockSetAssetField).toHaveBeenCalled();
  });

  it('renders help text for fields', () => {
    const fields = [
      {
        name: 'groupId',
        type: 'STRING' as const,
        displayName: 'Group ID',
        helpText: 'The Maven group ID',
        group: 'Component coordinates',
        optional: false,
      },
    ];
    mockUseUploadDefinition.mockReturnValue({
      ...defaultDefinitionResult,
      componentFields: fields,
      componentFieldsByGroup: {
        'Component coordinates': fields,
      },
    });

    renderWithTheme(<UploadFormContainer />);

    expect(screen.queryByText(/The Maven group ID/i)).toBeInTheDocument();
  });

  it('renders required indicator for required fields', () => {
    renderWithTheme(<UploadFormContainer />);

    // Required fields should have asterisks
    const requiredIndicators = screen.getAllByText('*');
    expect(requiredIndicators.length).toBeGreaterThan(0);
  });

  it('renders repository selection combobox', () => {
    renderWithTheme(<UploadFormContainer />);
    expect(screen.getByText('Target Repository')).toBeInTheDocument();
    expect(screen.getByText('Repository')).toBeInTheDocument();
  });
});

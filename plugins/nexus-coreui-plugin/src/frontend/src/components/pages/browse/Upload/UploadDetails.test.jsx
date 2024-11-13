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
import { render as rtlRender, screen, waitFor, waitForElementToBeRemoved, within }
  from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { when } from 'jest-when';


import UploadDetails from './UploadDetails.jsx';
import * as testData from './UploadDetails.testdata';

// Creates a selector function that uses getByRole by default but which can be customized per-use to use
// queryByRole, findByRole, etc instead
const selectorQuery = (...queryParams) => (queryType, container) => {
  const queryRoot = container ? within(container) : screen;
  return queryRoot[`${queryType ?? 'get'}ByRole`].apply(queryRoot, queryParams);
}

const selectors = {
  main: () => screen.getByRole('main'),
  h1: selectorQuery('heading', { level: 1 }),
  loadingStatus: () => screen.getByRole('status'),
  errorAlert: selectorQuery('alert'),
  errorRetryBtn: selectorQuery('button', { name: 'Retry' }),
  form: selectorQuery('form', { name: 'Upload' }),
  chooseAssetsRegion: selectorQuery('region', { name: /choose assets/i }),
  regionA: selectorQuery('region', { name: 'A' }),
  regionB: selectorQuery('region', { name: 'B' }),
  regionC: selectorQuery('region', { name: 'C' }),
  cancelBtn: selectorQuery('button', { name: 'Cancel' }),
  uploadBtn: selectorQuery('button', { name: 'Upload' }),
  dismissUploadBtn: selectorQuery('button', { name: 'Dismiss Upload' }),
  addAssetBtn: selectorQuery('button', { name: 'Add another asset' }),
  assetGroup: (groupDisplayNum, queryType) => selectorQuery('group', { name: `Asset ${groupDisplayNum}` })(queryType),
  fieldByNumAndGroup: (fieldNum, assetGroup, queryType) =>
      selectorQuery('textbox', { name: `Field ${fieldNum}` })(queryType, assetGroup),
  checkboxFieldByNumAndGroup: (fieldNum, assetGroup, queryType) =>
      selectorQuery('checkbox', { name: `Field ${fieldNum}` })(queryType, assetGroup),
  deleteBtnByGroup: (assetGroup, queryType) =>
      selectorQuery('button', { name: 'Delete' })(queryType, assetGroup),
  fileUploadByGroup: (assetGroup) => assetGroup.querySelector('input[type=file]'),
  mavenExtensionField: (assetGroup, queryType) =>
      selectorQuery('textbox', { name: 'Extension' })(queryType, assetGroup),
  mavenClassifierField: (assetGroup, queryType) =>
      selectorQuery('textbox', { name: 'Classifier' })(queryType, assetGroup),
  mavenGroupField: selectorQuery('textbox', { name: 'Group ID' }),
  mavenArtifactField: selectorQuery('textbox', { name: 'Artifact ID' }),
  mavenVersionField: selectorQuery('textbox', { name: 'Version' }),
  mavenGeneratePomField: selectorQuery('checkbox', { name: 'Generate a POM file with these coordinates' }),
  mavenPackagingField: selectorQuery('textbox', { name: 'Packaging' })
};


function getEmptyFileList() {
  const input = document.createElement('input');
  input.type = 'file';
  return input.files;
}

/**
 * JSDOM has no way to construct a true FileList object programmatically. At the same time, the
 * HTMLInputElement.prototype.files property will _only_ accept a true FileList object, and not any sort
 * of mocked implementation, including the one that userEvent.upload() constructs. Because the RSC
 * NxFileUpload component expects the FileList that gets passed through its onChange callback to be passed
 * back into the HTMLInputElement `files` property, this causes a problem. In this function we address this
 * by hacking the file input to accept arbitrary objects on its files property
 */
function mockFileInputFilesSetter() {

  let realFilesDescriptor, realValueDescriptor;

  beforeEach(function() {
    realFilesDescriptor = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'files');
    realValueDescriptor = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value');

    // The files property descriptor must be redefined at the prototype level, not the instance level.
    // This map keeps track of which input element each files list is associated with
    const filesMap = new WeakMap(),
        valueMap = new WeakMap(),
        emptyFileList = getEmptyFileList();

    Object.defineProperty(HTMLInputElement.prototype, 'files', {
      enumerable: true,
      set(val) {
        const file1 = val.item(0);

        filesMap.set(this, val);
        valueMap.set(this, file1 ? `C:\\fakepath\\${file1.name}` : '');
      },
      get() {
        return filesMap.get(this) ?? emptyFileList;
      }
    });

    Object.defineProperty(HTMLInputElement.prototype, 'value', {
      enumerable: true,
      set(val) {
        if (val === '') {
          // In real file inputs, setting the value to the empty string clears the file selection
          filesMap.set(this, emptyFileList);
        }

        valueMap.set(this, val);
      },
      get() {
        return valueMap.get(this) ?? '';
      }
    });
  });

  afterEach(function() {
    Object.defineProperty(HTMLInputElement.prototype, 'files', realFilesDescriptor);
    Object.defineProperty(HTMLInputElement.prototype, 'value', realValueDescriptor);
  });
}

function mockWindowLocation() {
  let originalLocationDescriptor, mockLocation;

  beforeEach(function() {
    originalLocationDescriptor = Object.getOwnPropertyDescriptor(window, 'location');
    mockLocation = Object.create(null, {
      hash: {
        configurable: true,
        enumerable: true,
        set: () => {},
        get: () => 'browse/upload:simple-repo'
      }
    });

    Object.defineProperty(window, 'location', {
      get: () => mockLocation
    });
  });

  afterEach(function() {
    Object.defineProperty(window, 'location', originalLocationDescriptor);
  });
}

function fakeFileList(...files) {
  const retval = {
    ...files,
    item(i) {
      return files[i];
    },
    toString() {
      return `fakeFileList ${files}`;
    },
    length: files.length
  };

  Object.setPrototypeOf(retval, FileList.prototype);

  return retval;
}

/**
 * Set the files as a FileList on the file input.
 * Not using userEvent.upload because it incorrectly makes the input's `files` property not-writable,
 * and not using fireEvent.change because it sets its own property descriptor on the input's `files` property,
 * preventing the descriptor we set in mockFileInputFilesSetter from operating
 */
function setFileUploadValue(fileUpload, ...files) {
  fileUpload.files = fakeFileList(...files);
  fileUpload.dispatchEvent(new Event('change', { bubbles: true }));
}

describe('UploadDetails', function() {
  mockFileInputFilesSetter();
  mockWindowLocation();

  beforeEach(function() {
    jest.spyOn(axios, 'post');

    when(axios.post).calledWith(
        'service/extdirect',
        expect.objectContaining({ action: 'coreui_Repository', method: 'readReferences' })
    ).mockResolvedValue(testData.sampleRepoSettings);

    when(axios.post).calledWith(
        'service/extdirect',
        expect.objectContaining({ action: 'coreui_Upload', method: 'getUploadDefinitions' })
    ).mockResolvedValue(testData.sampleUploadDefinitions);
  });

  function render(itemId = 'simple-repo') {
    return rtlRender(<UploadDetails itemId={itemId} />);
  }

  it('renders a main content area', function() {
    // resolving the promise in this otherwise-synchronous test causes act errors, so just leave it unresolved here
    jest.spyOn(axios, 'get').mockReturnValue(new Promise(() => {}));
    render();

    expect(selectors.main()).toBeInTheDocument();
  });

  describe('initial loading', function() {
    it('renders a loading spinner before the readReferences call completes', function() {
      when(axios.post).calledWith(
          'service/extdirect',
          expect.objectContaining({ action: 'coreui_Repository', method: 'readReferences' })
      ).mockReturnValue(new Promise(() => {}));

      render();

      const loadingStatus = selectors.loadingStatus();
      expect(loadingStatus).toBeInTheDocument();
      expect(loadingStatus).toHaveTextContent(/loading/i);
    });

    it('renders a loading spinner before the getUploadDefinitions call completes', function() {
      when(axios.post).calledWith(
          'service/extdirect',
          expect.objectContaining({ action: 'coreui_Upload', method: 'getUploadDefinitions' })
      ).mockReturnValue(new Promise(() => {}));

      render();

      const loadingStatus = selectors.loadingStatus();
      expect(loadingStatus).toBeInTheDocument();
      expect(loadingStatus).toHaveTextContent(/loading/i);
    });

    it('dismisses the loading spinner and renders a form when the REST calls complete', async function() {
      render();

      const loadingStatus = selectors.loadingStatus();

      await waitForElementToBeRemoved(loadingStatus);
      expect(selectors.form()).toBeInTheDocument();
    });

    it('renders an error alert with a Retry button if the readReferences call fails at the HTTP level',
        async function() {
          when(axios.post).calledWith(
              'service/extdirect',
              expect.objectContaining({ action: 'coreui_Repository', method: 'readReferences' })
          ).mockRejectedValue({ message: 'foobar' });

          render();

          const loadingStatus = selectors.loadingStatus();

          await waitForElementToBeRemoved(loadingStatus);
          expect(selectors.form('query')).not.toBeInTheDocument();
          expect(selectors.errorAlert()).toBeInTheDocument();
          expect(selectors.errorAlert()).toHaveTextContent('foobar');
          expect(selectors.errorRetryBtn()).toBeInTheDocument();
        }
    );

    it('renders an error alert with a Retry button if the readReferences call fails at the extdirect level',
        async function() {
          when(axios.post).calledWith(
              'service/extdirect',
              expect.objectContaining({ action: 'coreui_Repository', method: 'readReferences' })
          ).mockResolvedValue({ data: { result: { success: false, message: 'foobar' } } });

          render();

          const loadingStatus = selectors.loadingStatus();

          await waitForElementToBeRemoved(loadingStatus);
          expect(selectors.form('query')).not.toBeInTheDocument();
          expect(selectors.errorAlert()).toBeInTheDocument();
          expect(selectors.errorAlert()).toHaveTextContent('foobar');
          expect(selectors.errorRetryBtn()).toBeInTheDocument();
        }
    );

    it('renders an error alert with a Retry button if the getUploadDefinitions call fails at the HTTP level',
        async function() {
          when(axios.post).calledWith(
              'service/extdirect',
              expect.objectContaining({ action: 'coreui_Upload', method: 'getUploadDefinitions' })
          ).mockRejectedValue({ message: 'foobar' });

          render();

          const loadingStatus = selectors.loadingStatus();

          await waitForElementToBeRemoved(loadingStatus);
          expect(selectors.form('query')).not.toBeInTheDocument();
          expect(selectors.errorAlert()).toBeInTheDocument();
          expect(selectors.errorAlert()).toHaveTextContent('foobar');
          expect(selectors.errorRetryBtn()).toBeInTheDocument();
        }
    );

    it('renders an error alert with a Retry button if the getUploadDefinitions call fails at the extdirect level',
        async function() {
          when(axios.post).calledWith(
              'service/extdirect',
              expect.objectContaining({ action: 'coreui_Upload', method: 'getUploadDefinitions' })
          ).mockResolvedValue({ data: { result: { success: false, message: 'foobar' } } });

          render();

          const loadingStatus = selectors.loadingStatus();

          await waitForElementToBeRemoved(loadingStatus);
          expect(selectors.form('query')).not.toBeInTheDocument();
          expect(selectors.errorAlert()).toBeInTheDocument();
          expect(selectors.errorAlert()).toHaveTextContent('foobar');
          expect(selectors.errorRetryBtn()).toBeInTheDocument();
        }
    );

    it('re-runs the REST calls when the Retry button is clicked, and renders the form if they succeed',
        async function() {
          when(axios.post)
              .calledWith(
                  'service/extdirect',
                  expect.objectContaining({ action: 'coreui_Repository', method: 'readReferences' })
              )
              .mockRejectedValueOnce({ message: 'foobar' })
              .mockResolvedValueOnce(testData.sampleRepoSettings);

          render();

          const loadingStatus = selectors.loadingStatus();

          await waitForElementToBeRemoved(loadingStatus);
          await userEvent.click(selectors.errorRetryBtn());

          expect(await selectors.form('find')).toBeInTheDocument();
        }
    );

    it('renders an error alert with a Retry button if the repository in question does not exist in the repositorySettings', async function() {
      render('no-such-repo');

      const loadingStatus = selectors.loadingStatus();

      await waitForElementToBeRemoved(loadingStatus);

      expect(selectors.form('query')).not.toBeInTheDocument();
      expect(selectors.errorAlert()).toBeInTheDocument();
      expect(selectors.errorAlert()).toHaveTextContent('no-such-repo');
      expect(selectors.errorAlert()).toHaveTextContent('Unable to find');
      expect(selectors.errorRetryBtn()).toBeInTheDocument();
    });

    it('renders an error alert with a Retry button if the repository in question is a proxy repo', async function() {
      render('proxy-repo');

      const loadingStatus = selectors.loadingStatus();

      await waitForElementToBeRemoved(loadingStatus);

      expect(selectors.form('query')).not.toBeInTheDocument();
      expect(selectors.errorAlert()).toBeInTheDocument();
      expect(selectors.errorAlert()).toHaveTextContent('proxy-repo');
      expect(selectors.errorAlert()).toHaveTextContent('does not support upload');
      expect(selectors.errorRetryBtn()).toBeInTheDocument();
    });

    it('renders an error alert with a Retry button if the repository in question is a group repo', async function() {
      render('group-repo');

      const loadingStatus = selectors.loadingStatus();

      await waitForElementToBeRemoved(loadingStatus);

      expect(selectors.form('query')).not.toBeInTheDocument();
      expect(selectors.errorAlert()).toBeInTheDocument();
      expect(selectors.errorAlert()).toHaveTextContent('group-repo');
      expect(selectors.errorAlert()).toHaveTextContent('does not support upload');
      expect(selectors.errorRetryBtn()).toBeInTheDocument();
    });

    it('renders an error alert with a Retry button if the repository in question is offline', async function() {
      render('offline-repo');

      const loadingStatus = selectors.loadingStatus();

      await waitForElementToBeRemoved(loadingStatus);

      expect(selectors.form('query')).not.toBeInTheDocument();
      expect(selectors.errorAlert()).toBeInTheDocument();
      expect(selectors.errorAlert()).toHaveTextContent('offline-repo');
      expect(selectors.errorAlert()).toHaveTextContent('does not support upload');
      expect(selectors.errorRetryBtn()).toBeInTheDocument();
    });

    it('renders an error alert with a Retry button if the repository in question is a maven SNAPSHOT repo',
        async function() {
          render('snapshot-repo');

          const loadingStatus = selectors.loadingStatus();

          await waitForElementToBeRemoved(loadingStatus);

          expect(selectors.form('query')).not.toBeInTheDocument();
          expect(selectors.errorAlert()).toBeInTheDocument();
          expect(selectors.errorAlert()).toHaveTextContent('snapshot-repo');
          expect(selectors.errorAlert()).toHaveTextContent('does not support upload');
          expect(selectors.errorRetryBtn()).toBeInTheDocument();
        }
    );

    it('renders an error alert with a Retry button if the repository in question is for a format with uiUpload set ' +
        'to false', async function() {
      render('ui-upload-disabled-repo');

      const loadingStatus = selectors.loadingStatus();

      await waitForElementToBeRemoved(loadingStatus);

      expect(selectors.form('query')).not.toBeInTheDocument();
      expect(selectors.errorAlert()).toBeInTheDocument();
      expect(selectors.errorAlert()).toHaveTextContent('ui-upload-disabled-repo');
      expect(selectors.errorAlert()).toHaveTextContent('does not support upload');
      expect(selectors.errorRetryBtn()).toBeInTheDocument();
    });
  });

  it('renders a first-level heading of "Upload" and sets that as the accessible name of the form', async function() {
    render();

    const h1 = selectors.h1();
    expect(h1).toHaveTextContent('Upload');

    const form = await selectors.form('find');
    expect(form).toHaveAccessibleName('Upload');
  });

  it('renders the text "Upload content to the hosted repository" and sets it as the accessible description of the form',
      async function() {
        render();

        expect(selectors.main()).toHaveTextContent('Upload content to the hosted repository');

        const form = await selectors.form('find');
        expect(form).toHaveAccessibleDescription('Upload content to the hosted repository');
      }
  );

  it('renders the "Required fields are marked with an asterisk" helper text within the form', async function() {
    render();
    expect(await selectors.form('find')).toHaveTextContent('Required fields are marked with an asterisk');
  });

  it('renders a region within the form with a heading and accessible name of ' +
      '"Choose Assets/Components for <props.itemId> Repository"', async function() {
    const expectedHeading = 'Choose Assets/Components for simple-repo Repository';

    render();

    const form = await selectors.form('find'),
        region = within(form).getByRole('region', { name: expectedHeading }),
        h2 = within(region).getByRole('heading', { level: 2 });

    expect(region).toBeInTheDocument();
    expect(h2).toHaveTextContent(expectedHeading);
  });

  it('renders a group within the "Choose Asset..." region with a name of "Asset 1"', async function() {
    render();

    const region = await selectors.chooseAssetsRegion('find'),
        group = selectors.assetGroup('1');

    expect(region).toContainElement(group);
  });

  it('renders a file upload within the "Asset 1" group with an accessible name of "File", and an ' +
      'initial accessible description of "No file selected"', async function() {
    render();

    const region = await selectors.chooseAssetsRegion('find'),
        fileUpload = selectors.fileUploadByGroup(region);

    expect(fileUpload).toBeInTheDocument();
    expect(fileUpload).toHaveAccessibleName('File');
    expect(fileUpload).toHaveAccessibleDescription('No file selected');
  });

  it('renders an aria-hidden "Choose File" button within the "Asset 1" group', async function() {
    render();

    const region = await selectors.chooseAssetsRegion('find'),
        fileUpload = selectors.fileUploadByGroup(region),
        button = within(region).getByRole('button', { hidden: true });

    expect(button).toHaveTextContent('Choose File');
    expect(button).toHaveAttribute('aria-hidden', 'true');

    // NOTE: it's not really possible in jest to test that the button activates the file input and
    // opens the file dialog
  });

  it('renders the name and size of the selected file and sets them as the file input\'s accessible description',
      async function() {
        render();

        const region = await selectors.chooseAssetsRegion('find'),
            fileUpload = selectors.fileUploadByGroup(region);

        setFileUploadValue(fileUpload, new File(['123456'], 'numbers.txt', { type: 'text/plain' }));

        expect(region).toHaveTextContent('numbers.txt');
        expect(region).toHaveTextContent('6.0 B');
        expect(fileUpload).toHaveAccessibleDescription('numbers.txt 6.0 B');
      }
  );

  it('renders the asset fields within the "Asset 1" group', async function() {
      render();

      const region = await selectors.chooseAssetsRegion('find'),
        field6 = selectors.fieldByNumAndGroup(6),
        field7 = selectors.fieldByNumAndGroup(7);

    expect(region).toContainElement(field6);
    expect(region).toContainElement(field7);
  });

  describe('"Add another asset" button', function() {
    it('does not render if multipleUpload is not true', async function() {
      render();

      const form = await selectors.form('find');

      expect(selectors.addAssetBtn('query')).not.toBeInTheDocument();
    });

    it('renders if multipleUpload is true', async function() {
      render('multi-repo');

      const form = await selectors.form('find');

      expect(selectors.addAssetBtn()).toBeInTheDocument();
    });

    it('adds another asset group with the next higher number, containing another file upload and set of asset fields',
        async function() {
          render('multi-repo');

          const region = await selectors.chooseAssetsRegion('find'),
              firstAssetGroup = selectors.assetGroup('1'),
              fileUpload_1 = selectors.fileUploadByGroup(firstAssetGroup),
              field6_1 = selectors.fieldByNumAndGroup(6, firstAssetGroup),
              field7_1 = selectors.fieldByNumAndGroup(7, firstAssetGroup),
              addAssetBtn = selectors.addAssetBtn();

          expect(selectors.assetGroup('2', 'query')).not.toBeInTheDocument();

          await userEvent.click(addAssetBtn);

          const secondAssetGroup = selectors.assetGroup('2'),
              fileUpload_2 = selectors.fileUploadByGroup(secondAssetGroup),
              field6_2 = selectors.fieldByNumAndGroup(6, secondAssetGroup),
              field7_2 = selectors.fieldByNumAndGroup(7, secondAssetGroup);

          expect(secondAssetGroup).toBeInTheDocument();
          expect(region).toContainElement(secondAssetGroup);
          expect(fileUpload_2).toBeInTheDocument();
          expect(fileUpload_2).toHaveAccessibleName('File');
          expect(fileUpload_2).toHaveAccessibleDescription('No file selected');
          expect(fileUpload_1).toBeInTheDocument();
          expect(field6_2).toBeInTheDocument();
          expect(field7_2).toBeInTheDocument();
          expect(field6_1).toBeInTheDocument();
          expect(field6_1).toBeInTheDocument();

          await userEvent.click(addAssetBtn);

          const thirdAssetGroup = selectors.assetGroup('2'),
              fileUpload_3 = selectors.fileUploadByGroup(thirdAssetGroup),
              field6_3 = selectors.fieldByNumAndGroup(6, thirdAssetGroup),
              field7_3 = selectors.fieldByNumAndGroup(7, thirdAssetGroup);

          expect(thirdAssetGroup).toBeInTheDocument();
          expect(region).toContainElement(thirdAssetGroup);
          expect(fileUpload_3).toBeInTheDocument();
          expect(fileUpload_3).toHaveAccessibleName('File');
          expect(fileUpload_3).toHaveAccessibleDescription('No file selected');
          expect(fileUpload_1).toBeInTheDocument();
          expect(fileUpload_2).toBeInTheDocument();
          expect(field6_3).toBeInTheDocument();
          expect(field7_3).toBeInTheDocument();
          expect(field6_1).toBeInTheDocument();
          expect(field6_1).toBeInTheDocument();
          expect(field6_2).toBeInTheDocument();
          expect(field6_2).toBeInTheDocument();
        }
    );
  });

  describe('regexMap', function() {
    it('fills in the corresponding asset fields according to the regexMap when a file is selected', async function() {
      render('regex-map-repo');

      await userEvent.click(await selectors.addAssetBtn('find'));

      const assetGroup1 = selectors.assetGroup('1'),
          assetGroup2 = selectors.assetGroup('2'),
          fileUpload1 = assetGroup1.querySelector('input[type=file]'),
          field6 = selectors.fieldByNumAndGroup(6, assetGroup1),
          field7 = selectors.fieldByNumAndGroup(7, assetGroup1),
          field6_2 = selectors.fieldByNumAndGroup(6, assetGroup2),
          field7_2 = selectors.fieldByNumAndGroup(7, assetGroup2);

      setFileUploadValue(fileUpload1, new File(['123456'], 'numbers.txt', { type: 'text/plain' }));

      expect(field6).toHaveValue('numbers');
      expect(field7).toHaveValue('txt');

      expect(field6_2).toHaveValue('');
      expect(field7_2).toHaveValue('');
    });

    it('does overwrite existing content in the asset fields', async function() {
      render('regex-map-repo');

      const form = await selectors.form('find'),
          fileUpload = form.querySelector('input[type=file]'),
          field6 = selectors.fieldByNumAndGroup(6),
          field7 = selectors.fieldByNumAndGroup(7);

      await userEvent.type(field7, 'csv');
      setFileUploadValue(fileUpload, new File(['123456'], 'numbers.txt', { type: 'text/plain' }));

      expect(field6).toHaveValue('numbers');
      expect(field7).toHaveValue('txt');
    });

    it('does trigger validation on regex autofill', async function() {
      render('regex-map-repo');

      const form = await selectors.form('find'),
          fileUpload = form.querySelector('input[type=file]'),
          field6 = selectors.fieldByNumAndGroup(6),
          field7 = selectors.fieldByNumAndGroup(7);

      await userEvent.type(field7, 'a');
      setFileUploadValue(fileUpload, new File(['123456'], 'numbers.', { type: 'text/plain' }));

      expect(field6).toHaveValue('numbers');
      expect(field6).toBeValid();
      expect(field6).not.toHaveErrorMessage();

      expect(field7).toHaveValue('');
      expect(field7).toBeInvalid();
      expect(field7).toHaveErrorMessage('This field is required');
    });

    it('does not change the field values, trigger validation, or show an error if the regex does not match',
        async function() {
          render('regex-map-repo');

          const form = await selectors.form('find'),
              fileUpload = form.querySelector('input[type=file]'),
              field6 = selectors.fieldByNumAndGroup(6),
              field7 = selectors.fieldByNumAndGroup(7);

          setFileUploadValue(fileUpload, new File(['123456'], '-.txt', { type: 'text/plain' }));

          expect(field6).toHaveValue('');
          expect(field6).toBeValid();
          expect(field6).not.toHaveErrorMessage();

          expect(field7).toHaveValue('');
          expect(field7).toBeValid();
          expect(field7).not.toHaveErrorMessage();

          // NOTE: can't test presence of form-level validation alert because it is in the DOM but hidden
          // via CSS (which isn't loaded in unit tests)
        }
    );

    it('removes validation errors on fields as appropriate', async function() {
      render('regex-map-repo');

      const form = await selectors.form('find'),
          fileUpload = form.querySelector('input[type=file]'),
          field6 = selectors.fieldByNumAndGroup(6),
          field7 = selectors.fieldByNumAndGroup(7);

      await userEvent.type(field6, 'a');
      await userEvent.type(field7, 'a');
      await userEvent.clear(field6);
      await userEvent.clear(field7);

      // both fields invalid before selecting file
      expect(field6).toBeInvalid();
      expect(field7).toBeInvalid();

      setFileUploadValue(fileUpload, new File(['123456'], 'numbers.', { type: 'text/plain' }));

      // file was selected and field 6 was autofilled; no longer invalid
      expect(field6).toBeValid();
      expect(field6).not.toHaveErrorMessage();

      // field7 however was autofilled with an empty capture; still invalid
      expect(field7).toHaveValue('');
      expect(field7).toBeInvalid();
      expect(field7).toHaveErrorMessage('This field is required');
    });
  });

  describe('"Delete" button', function() {
    it('does not appear if multipleUpload is not enabled', async function() {
      render();

      const form = await selectors.form('find');

      expect(selectors.deleteBtnByGroup(undefined, 'query')).not.toBeInTheDocument();
    });

    it('appears on all asset groups once multiple asset groups are present', async function() {
      render('multi-repo');

      const form = await selectors.form('find');

      expect(selectors.deleteBtnByGroup(undefined, 'query')).not.toBeInTheDocument();

      await userEvent.click(selectors.addAssetBtn());

      const assetGroup1 = selectors.assetGroup('1'),
          assetGroup2 = selectors.assetGroup('2');

      expect(selectors.deleteBtnByGroup(assetGroup1)).toBeInTheDocument();
      expect(selectors.deleteBtnByGroup(assetGroup2)).toBeInTheDocument();
    });

    it('removes the asset group when clicked', async function() {
      render('multi-repo');

      await userEvent.click(await selectors.addAssetBtn('find'));

      const assetGroup1 = selectors.assetGroup('1'),
          assetGroup2 = selectors.assetGroup('2'),
          fileUpload = selectors.fileUploadByGroup(assetGroup1),
          fileUpload2 = selectors.fileUploadByGroup(assetGroup2),
          field6 = selectors.fieldByNumAndGroup(6, assetGroup1),
          field7 = selectors.fieldByNumAndGroup(7, assetGroup1),
          field6_2 = selectors.fieldByNumAndGroup(6, assetGroup2),
          field7_2 = selectors.fieldByNumAndGroup(7, assetGroup2),
          file = new File(['test'], 'test.txt', { type: 'text-plain' }),
          file2 = new File(['tset'], 'tset.txt', { type: 'text-plain' });

      await userEvent.type(field6, 'a');
      await userEvent.type(field7, 'b');
      await userEvent.type(field6_2, 'c');
      await userEvent.type(field7_2, 'd');
      setFileUploadValue(fileUpload, file);
      setFileUploadValue(fileUpload2, file2);

      // Delete the first asset group. The result should be that only a single asset group remains, and it has the
      // values of the formerly-second asset group
      await userEvent.click(selectors.deleteBtnByGroup(assetGroup1));

      const assetGroupAfterDelete = selectors.assetGroup('1'),
          fileUploadAfterDelete = selectors.fileUploadByGroup(assetGroupAfterDelete),
          field6AfterDelete = selectors.fieldByNumAndGroup(6, assetGroupAfterDelete),
          field7AfterDelete = selectors.fieldByNumAndGroup(7, assetGroupAfterDelete);

      expect(selectors.assetGroup('2', 'query')).not.toBeInTheDocument();
      expect(fileUploadAfterDelete.files.item(0)).toBe(file2);
      expect(field6AfterDelete).toHaveValue('c');
      expect(field7AfterDelete).toHaveValue('d');
    });

    it('hides the Delete button when the number of asset groups is reduced to one', async function() {
      render('multi-repo');

      await userEvent.click(await selectors.addAssetBtn('find'));

      const assetGroup1 = selectors.assetGroup('1'),
          assetGroup2 = selectors.assetGroup('2');

      await userEvent.click(selectors.deleteBtnByGroup(assetGroup1));

      expect(selectors.deleteBtnByGroup(undefined, 'query')).not.toBeInTheDocument();
    });
  });

  it('renders a region for each group in the upload definition component fields', async function() {
    render();

    const regionA = await selectors.regionA('find'),
        regionB = selectors.regionB(),
        regionC = selectors.regionC();

    expect(within(regionA).getByRole('heading', { level: 2 })).toHaveTextContent('A');
    expect(within(regionB).getByRole('heading', { level: 2 })).toHaveTextContent('B');
    expect(within(regionC).getByRole('heading', { level: 2 })).toHaveTextContent('C');
  });

  it('renders a text field for each component field with type: STRING in its corresponding group', async function() {
    render();

    const regionA = await selectors.regionA('find'),
        regionB = selectors.regionB(),
        regionC = selectors.regionC(),
        field1 = selectors.fieldByNumAndGroup(1),
        field2 = selectors.fieldByNumAndGroup(2),
        field3 = selectors.fieldByNumAndGroup(3),
        field4 = selectors.fieldByNumAndGroup(4);

    expect(regionA).toContainElement(field1);
    expect(regionA).toContainElement(field3);
    expect(regionB).toContainElement(field2);
    expect(regionC).toContainElement(field4);
  });

  it('renders a checkbox field for each component field with type: BOOLEAN in its corresponding group',
      async function() {
        render();

        const regionC = await selectors.regionC('find'),
            field5 = selectors.checkboxFieldByNumAndGroup(5);

        expect(regionC).toContainElement(field5);
      }
  );

  it('renders the helpText for each component STRING field as a sublabel and a11y description', async function() {
    render();

    const field1 = await selectors.fieldByNumAndGroup(1, undefined, 'find'),
        field2 = selectors.fieldByNumAndGroup(2),
        field3 = selectors.fieldByNumAndGroup(3),
        field4 = selectors.fieldByNumAndGroup(4);

    expect(field1).not.toHaveAccessibleDescription();
    expect(field2).toHaveAccessibleDescription('This is the second field');
    expect(screen.getByText('This is the second field')).toBeInTheDocument();
    expect(field3).not.toHaveAccessibleDescription();
    expect(field4).toHaveAccessibleDescription('FOUR');
    expect(screen.getByText('FOUR')).toBeInTheDocument();
  });

  it('marks the text fields that do not have the optional flag set as required', async function() {
    render('multi-repo');

    const form = await selectors.form('find'),
        field1 = selectors.fieldByNumAndGroup(1),
        field2 = selectors.fieldByNumAndGroup(2),
        field3 = selectors.fieldByNumAndGroup(3),
        field4 = selectors.fieldByNumAndGroup(4),
        field6 = selectors.fieldByNumAndGroup(6),
        field7 = selectors.fieldByNumAndGroup(7);

    expect(field1).not.toBeRequired();
    expect(field2).toBeRequired();
    expect(field3).not.toBeRequired();
    expect(field4).toBeRequired();
    expect(field6).toBeRequired();
    expect(field7).not.toBeRequired();

    await userEvent.click(selectors.addAssetBtn());
    const assetGroup2 = selectors.assetGroup('2'),
        field6_2 = selectors.fieldByNumAndGroup(6, assetGroup2),
        field7_2 = selectors.fieldByNumAndGroup(7, assetGroup2);

    expect(field6_2).toBeRequired();
    expect(field7_2).not.toBeRequired();
  });

  it('allows values to be set into the text fields', async function() {
    render('multi-repo');

    const field1 = await selectors.fieldByNumAndGroup(1, undefined, 'find'),
        field2 = selectors.fieldByNumAndGroup(2),
        field3 = selectors.fieldByNumAndGroup(3),
        field4 = selectors.fieldByNumAndGroup(4),
        field6 = selectors.fieldByNumAndGroup(6),
        field7 = selectors.fieldByNumAndGroup(7);

    await userEvent.type(field1, 'foo');
    await userEvent.type(field2, 'bar');
    await userEvent.type(field3, 'baz');
    await userEvent.type(field4, 'qwerty');
    await userEvent.type(field6, 'asdf');
    await userEvent.type(field7, '12345');

    expect(field1).toHaveValue('foo');
    expect(field2).toHaveValue('bar');
    expect(field3).toHaveValue('baz');
    expect(field4).toHaveValue('qwerty');
    expect(field6).toHaveValue('asdf');
    expect(field7).toHaveValue('12345');

    await userEvent.click(selectors.addAssetBtn());
    const assetGroup2 = selectors.assetGroup('2'),
        field6_2 = selectors.fieldByNumAndGroup(6, assetGroup2),
        field7_2 = selectors.fieldByNumAndGroup(7, assetGroup2);

    await userEvent.type(field6_2, 'zxcv');
    await userEvent.type(field7_2, '098-0');

    expect(field6_2).toHaveValue('zxcv');
    expect(field7_2).toHaveValue('098-0');
    expect(field6).toHaveValue('asdf');
    expect(field7).toHaveValue('12345');
  });

  it('allows a file to be set into the file input and renders its name and size', async function() {
    render('multi-repo');

    const form = await selectors.form('find'),
        fileUpload = selectors.fileUploadByGroup(form);

    setFileUploadValue(fileUpload, new File(['123456'], 'numbers.txt', { type: 'text/plain' }));
    expect(screen.getByText('numbers.txt')).toBeInTheDocument();
    expect(screen.getByText('6.0 B')).toBeInTheDocument();
    expect(fileUpload).toHaveAccessibleDescription('numbers.txt 6.0 B');

    await userEvent.click(selectors.addAssetBtn());
    const assetGroup2 = selectors.assetGroup('2'),
        fileUpload2 = selectors.fileUploadByGroup(assetGroup2);

    setFileUploadValue(fileUpload2, new File(['@@'], 'at.txt', { type: 'text/plain' }));

    expect(within(assetGroup2).getByText('at.txt')).toBeInTheDocument();
    expect(within(assetGroup2).getByText('2.0 B')).toBeInTheDocument();
    expect(fileUpload2).toHaveAccessibleDescription('at.txt 2.0 B');
  });

  it('renders a button to clear the selected file when there is one', async function() {
    render('regex-map-repo');

    const form = await selectors.form('find'),
        fileUpload = selectors.fileUploadByGroup(form);

    expect(selectors.dismissUploadBtn('query')).not.toBeInTheDocument();

    setFileUploadValue(fileUpload, new File(['123456'], 'numbers.txt', { type: 'text/plain' }));

    const dismissBtn = selectors.dismissUploadBtn();
    expect(dismissBtn).toBeInTheDocument();

    await userEvent.click(selectors.addAssetBtn());
    const assetGroup2 = selectors.assetGroup('2'),
        fileUpload2 = selectors.fileUploadByGroup(assetGroup2);

    setFileUploadValue(fileUpload2, new File(['@@'], 'at.txt', { type: 'text/plain' }));

    const dismissBtn2 = within(assetGroup2).getByRole('button', { name: 'Dismiss Upload' });
    expect(dismissBtn2).toBeInTheDocument();

    expect(fileUpload).not.toHaveValue('');

    await userEvent.click(dismissBtn);

    expect(fileUpload).toHaveValue('');
    expect(fileUpload.files).toHaveLength(0);
    expect(screen.queryByText('numbers.txt')).not.toBeInTheDocument();
    expect(screen.queryByText('6.0 B')).not.toBeInTheDocument();
    expect(fileUpload).toHaveAccessibleDescription('No file selected');
    expect(dismissBtn).not.toBeInTheDocument();

    // File upload 2 unaffected by file upload 1's dismiss btn
    expect(fileUpload2).not.toHaveValue('');
    expect(fileUpload2.files).toHaveLength(1);
    expect(within(assetGroup2).getByText('at.txt')).toBeInTheDocument();
    expect(within(assetGroup2).getByText('2.0 B')).toBeInTheDocument();
    expect(fileUpload2).toHaveAccessibleDescription('at.txt 2.0 B');

    await userEvent.click(dismissBtn2);

    expect(fileUpload2).toHaveValue('');
    expect(fileUpload2.files).toHaveLength(0);
    expect(screen.queryByText('at.txt')).not.toBeInTheDocument();
    expect(screen.queryByText('2.0 B')).not.toBeInTheDocument();
    expect(fileUpload2).toHaveAccessibleDescription('No file selected');
  });

  describe('field validation', function() {
    it('adds "This field is required" error text to required text inputs when they are empty and non-pristine',
        async function() {
          render('multi-repo');

          await userEvent.click(await selectors.addAssetBtn('find'));

          const assetGroup1 = selectors.assetGroup('1'),
              assetGroup2 = selectors.assetGroup('2'),
              field1 = await selectors.fieldByNumAndGroup(1),
              field2 = selectors.fieldByNumAndGroup(2),
              field3 = selectors.fieldByNumAndGroup(3),
              field4 = selectors.fieldByNumAndGroup(4),
              field6 = selectors.fieldByNumAndGroup(6, assetGroup1),
              field7 = selectors.fieldByNumAndGroup(7, assetGroup1),
              field6_2 = selectors.fieldByNumAndGroup(6, assetGroup2),
              field7_2 = selectors.fieldByNumAndGroup(7, assetGroup2);

          expect(screen.queryByText('This field is required')).not.toBeInTheDocument();
          expect(field1).not.toHaveErrorMessage();
          expect(field2).not.toHaveErrorMessage();
          expect(field3).not.toHaveErrorMessage();
          expect(field4).not.toHaveErrorMessage();
          expect(field6).not.toHaveErrorMessage();
          expect(field7).not.toHaveErrorMessage();
          expect(field6_2).not.toHaveErrorMessage();
          expect(field7_2).not.toHaveErrorMessage();
          expect(field1).toBeValid();
          expect(field2).toBeValid();
          expect(field3).toBeValid();
          expect(field4).toBeValid();
          expect(field6).toBeValid();
          expect(field7).toBeValid();
          expect(field6_2).toBeValid();
          expect(field7_2).toBeValid();

          await userEvent.type(field1, 'foo');
          await userEvent.type(field2, 'bar');
          await userEvent.type(field3, 'baz');
          await userEvent.type(field4, 'qwerty');
          await userEvent.type(field6, 'asdf');
          await userEvent.type(field7, '12345');
          await userEvent.type(field6_2, 'zxcv');
          await userEvent.type(field7_2, '0987');

          expect(screen.queryByText('This field is required')).not.toBeInTheDocument();
          expect(field1).not.toHaveErrorMessage();
          expect(field2).not.toHaveErrorMessage();
          expect(field3).not.toHaveErrorMessage();
          expect(field4).not.toHaveErrorMessage();
          expect(field6).not.toHaveErrorMessage();
          expect(field7).not.toHaveErrorMessage();
          expect(field6_2).not.toHaveErrorMessage();
          expect(field7_2).not.toHaveErrorMessage();
          expect(field1).toBeValid();
          expect(field2).toBeValid();
          expect(field3).toBeValid();
          expect(field4).toBeValid();
          expect(field6).toBeValid();
          expect(field7).toBeValid();
          expect(field6_2).toBeValid();
          expect(field7_2).toBeValid();

          await userEvent.clear(field1);
          await userEvent.clear(field2);
          await userEvent.clear(field3);
          await userEvent.clear(field4);
          await userEvent.clear(field6);
          await userEvent.clear(field7);
          await userEvent.clear(field6_2);
          await userEvent.clear(field7_2);

          expect(screen.getAllByText('This field is required')).toHaveLength(4);
          expect(field1).not.toHaveErrorMessage();
          expect(field2).toHaveErrorMessage('This field is required');
          expect(field3).not.toHaveErrorMessage();
          expect(field4).toHaveErrorMessage('This field is required');
          expect(field6).toHaveErrorMessage('This field is required');
          expect(field7).not.toHaveErrorMessage();
          expect(field6_2).toHaveErrorMessage('This field is required');
          expect(field7_2).not.toHaveErrorMessage();
          expect(field1).toBeValid();
          expect(field2).toBeInvalid();
          expect(field3).toBeValid();
          expect(field4).toBeInvalid();
          expect(field6).toBeInvalid();
          expect(field7).toBeValid();
          expect(field6_2).toBeInvalid();
          expect(field7_2).toBeValid();
        }
    );

    it('adds "This field is required!" error text to the file upload when it is empty and non-pristine',
        async function() {
          render('multi-repo');

          await userEvent.click(await selectors.addAssetBtn('find'));

          const assetGroup1 = selectors.assetGroup('1'),
              assetGroup2 = selectors.assetGroup('2'),
              fileUpload = selectors.fileUploadByGroup(assetGroup1),
              fileUpload2 = selectors.fileUploadByGroup(assetGroup2);

          expect(screen.queryByText('This field is required!')).not.toBeInTheDocument();
          expect(fileUpload).not.toHaveErrorMessage();
          expect(fileUpload).toBeValid();
          expect(fileUpload2).not.toHaveErrorMessage();
          expect(fileUpload2).toBeValid();

          setFileUploadValue(fileUpload, new File(['test'], 'test.txt', { type: 'text-plain' }));
          setFileUploadValue(fileUpload2, new File(['tset'], 'tset.txt', { type: 'text-plain' }));

          expect(screen.queryByText('This field is required!')).not.toBeInTheDocument();
          expect(fileUpload).not.toHaveErrorMessage();
          expect(fileUpload).toBeValid();
          expect(fileUpload2).not.toHaveErrorMessage();
          expect(fileUpload2).toBeValid();

          setFileUploadValue(fileUpload);

          expect(within(assetGroup1).getByText('This field is required!')).toBeInTheDocument();
          expect(fileUpload).toHaveErrorMessage('This field is required!');
          expect(fileUpload).toBeInvalid();
          expect(within(assetGroup2).queryByText('This field is required!')).not.toBeInTheDocument();
          expect(fileUpload2).not.toHaveErrorMessage();
          expect(fileUpload2).toBeValid();

          setFileUploadValue(fileUpload2);

          expect(within(assetGroup1).getByText('This field is required!')).toBeInTheDocument();
          expect(fileUpload).toHaveErrorMessage('This field is required!');
          expect(fileUpload).toBeInvalid();
          expect(within(assetGroup2).getByText('This field is required!')).toBeInTheDocument();
          expect(fileUpload2).toHaveErrorMessage('This field is required!');
          expect(fileUpload2).toBeInvalid();
        }
    );

    it('does not immediately add validation errors to asset fields that have been deleted and re-added', async function() {
      render('multi-repo');

      await userEvent.click(await selectors.addAssetBtn('find'));

      const assetGroup2 = selectors.assetGroup('2'),
          fileUpload2 = selectors.fileUploadByGroup(assetGroup2),
          field6_2 = selectors.fieldByNumAndGroup(6, assetGroup2),
          field7_2 = selectors.fieldByNumAndGroup(7, assetGroup2),
          file2 = new File(['tset'], 'tset.txt', { type: 'text-plain' });

      await userEvent.type(field6_2, 'zxcv');
      await userEvent.type(field7_2, '0987');

      await userEvent.click(selectors.deleteBtnByGroup(assetGroup2));
      await userEvent.click(selectors.addAssetBtn());

      expect(screen.queryByText('This field is required')).not.toBeInTheDocument();
      expect(field6_2).not.toHaveErrorMessage();
      expect(field7_2).not.toHaveErrorMessage();
      expect(field6_2).toBeValid();
      expect(field7_2).toBeValid();
    });

    it('adds "Asset not unique" text to all relevant asset fields when they match between multiple asset groups',
        async function() {
          render('multi-repo');

          await userEvent.click(await selectors.addAssetBtn('find'));
          await userEvent.click(selectors.addAssetBtn());
          await userEvent.click(selectors.addAssetBtn());

          const assetGroup1 = selectors.assetGroup('1'),
              assetGroup2 = selectors.assetGroup('2'),
              assetGroup3 = selectors.assetGroup('3'),
              assetGroup4 = selectors.assetGroup('4'),
              field6 = selectors.fieldByNumAndGroup(6, assetGroup1),
              field7 = selectors.fieldByNumAndGroup(7, assetGroup1),
              field6_2 = selectors.fieldByNumAndGroup(6, assetGroup2),
              field7_2 = selectors.fieldByNumAndGroup(7, assetGroup2),
              field6_3 = selectors.fieldByNumAndGroup(6, assetGroup3),
              field7_3 = selectors.fieldByNumAndGroup(7, assetGroup3),
              field6_4 = selectors.fieldByNumAndGroup(6, assetGroup4),
              field7_4 = selectors.fieldByNumAndGroup(7, assetGroup4);

          // groups 1, 2, and 4 are the same while 3 is different
          await userEvent.type(field6, 'a');
          await userEvent.type(field7, 'b');
          await userEvent.type(field6_2, 'a');
          await userEvent.type(field7_2, 'b');
          await userEvent.type(field6_3, 'c');
          await userEvent.type(field7_3, 'd');
          await userEvent.type(field6_4, 'a');
          await userEvent.type(field7_4, 'b');

          expect(field6).toBeInvalid();
          expect(field6).toHaveErrorMessage('Asset not unique');
          expect(field7).toBeInvalid();
          expect(field7).toHaveErrorMessage('Asset not unique');

          expect(field6_2).toBeInvalid();
          expect(field6_2).toHaveErrorMessage('Asset not unique');
          expect(field7_2).toBeInvalid();
          expect(field7_2).toHaveErrorMessage('Asset not unique');

          expect(field6_3).toBeValid();
          expect(field6_3).not.toHaveErrorMessage();
          expect(field7_3).toBeValid();
          expect(field7_3).not.toHaveErrorMessage();

          expect(field6_4).toBeInvalid();
          expect(field6_4).toHaveErrorMessage('Asset not unique');
          expect(field7_4).toBeInvalid();
          expect(field7_4).toHaveErrorMessage('Asset not unique');
        }
    );

    it('shows the required field message in favor of the unique asset message', async function() {
      render('multi-repo');

      await userEvent.click(await selectors.addAssetBtn('find'));

      const assetGroup1 = selectors.assetGroup('1'),
          assetGroup2 = selectors.assetGroup('2'),
          field6 = selectors.fieldByNumAndGroup(6, assetGroup1),
          field7 = selectors.fieldByNumAndGroup(7, assetGroup1),
          field6_2 = selectors.fieldByNumAndGroup(6, assetGroup2),
          field7_2 = selectors.fieldByNumAndGroup(7, assetGroup2);

      await userEvent.type(field6, 'a');
      await userEvent.type(field7, 'b');
      await userEvent.type(field6_2, 'a');
      await userEvent.type(field7_2, 'b');

      await userEvent.clear(field6);
      await userEvent.clear(field7);
      await userEvent.clear(field6_2);
      await userEvent.clear(field7_2);

      expect(field6).toBeInvalid();
      expect(field6).toHaveErrorMessage('This field is required');
      expect(field7).toBeValid();
      expect(field7).not.toHaveErrorMessage();

      expect(field6_2).toBeInvalid();
      expect(field6_2).toHaveErrorMessage('This field is required');
      expect(field7_2).toBeValid();
      expect(field7_2).not.toHaveErrorMessage();
    });
  });

  it('has a cancel button that navigates to the Upload List page when clicked', async function() {
    const hashSpy = jest.spyOn(window.location, 'hash', 'set');

    render();

    expect(hashSpy).not.toHaveBeenCalled();

    await userEvent.click(await selectors.cancelBtn('find'));

    expect(hashSpy).toHaveBeenCalledWith('browse/upload');
  });

  describe('form submit', function() {
    it('has an upload button that submits the form data when clicked', async function() {
      let postedFormData;

      when(axios.post).calledWith('service/rest/internal/ui/upload/multi-repo', expect.anything())
          .mockImplementation((_, formData) => {
            postedFormData = formData;
            return new Promise(() => {});
          });

      render('multi-repo');

      await userEvent.click(await selectors.addAssetBtn('find'));

      const uploadBtn = selectors.uploadBtn(),
          assetGroup1 = selectors.assetGroup('1'),
          assetGroup2 = selectors.assetGroup('2'),
          fileUpload = selectors.fileUploadByGroup(assetGroup1),
          fileUpload2 = selectors.fileUploadByGroup(assetGroup2),
          field2 = selectors.fieldByNumAndGroup(2),
          field4 = selectors.fieldByNumAndGroup(4),
          field6 = selectors.fieldByNumAndGroup(6, assetGroup1),
          field6_2 = selectors.fieldByNumAndGroup(6, assetGroup2),
          file = new File(['test'], 'test.txt', { type: 'text-plain' }),
          file2 = new File(['tset'], 'tset.txt', { type: 'text-plain' });

      await userEvent.type(field2, 'bar');
      await userEvent.type(field4, 'qwerty');
      await userEvent.type(field6, 'asdf');
      await userEvent.type(field6_2, 'zxcv');
      setFileUploadValue(fileUpload, file);
      setFileUploadValue(fileUpload2, file2);

      expect(postedFormData).not.toBeDefined();

      await userEvent.click(uploadBtn);

      await waitFor(() => expect(postedFormData).toBeDefined());
    });

    it('adds the component fields to the FormData by name', async function() {
      let postedFormData;

      when(axios.post).calledWith('service/rest/internal/ui/upload/simple-repo', expect.anything())
          .mockImplementation((_, formData) => {
            postedFormData = formData;
            return new Promise(() => {});
          });

      render();

      const form = await selectors.form('find'),
          uploadBtn = selectors.uploadBtn(),
          fileUpload = selectors.fileUploadByGroup(form),
          field1 = selectors.fieldByNumAndGroup(1),
          field2 = selectors.fieldByNumAndGroup(2),
          field3 = selectors.fieldByNumAndGroup(3),
          field4 = selectors.fieldByNumAndGroup(4),
          field6 = selectors.fieldByNumAndGroup(6),
          field7 = selectors.fieldByNumAndGroup(7),
          file = new File(['test'], 'test.txt', { type: 'text-plain' });

      await userEvent.type(field1, 'foo');
      await userEvent.type(field2, 'bar');
      await userEvent.type(field3, 'baz');
      await userEvent.type(field4, 'qwerty');
      await userEvent.type(field6, 'asdf');
      setFileUploadValue(fileUpload, file);

      await userEvent.click(uploadBtn);

      await waitFor(() => expect(postedFormData).toBeDefined());
      expect(postedFormData.get('field1')).toBe('foo');
      expect(postedFormData.get('field2')).toBe('bar');
      expect(postedFormData.get('field3')).toBe('baz');
      expect(postedFormData.get('field4')).toBe('qwerty');
    });

    it('adds the files to the FormData under the names "asset0", "asset1", ...', async function() {
      let postedFormData;

      when(axios.post).calledWith('service/rest/internal/ui/upload/multi-repo', expect.anything())
          .mockImplementation((_, formData) => {
            postedFormData = formData;
            return new Promise(() => {});
          });

      render('multi-repo');

      await userEvent.click(await selectors.addAssetBtn('find'));
      await userEvent.click(selectors.addAssetBtn());

      const uploadBtn = selectors.uploadBtn(),
          assetGroup1 = selectors.assetGroup('1'),
          assetGroup2 = selectors.assetGroup('2'),
          assetGroup3 = selectors.assetGroup('3'),
          fileUpload = selectors.fileUploadByGroup(assetGroup1),
          fileUpload2 = selectors.fileUploadByGroup(assetGroup2),
          fileUpload3 = selectors.fileUploadByGroup(assetGroup3),
          field2 = selectors.fieldByNumAndGroup(2),
          field4 = selectors.fieldByNumAndGroup(4),
          field6 = selectors.fieldByNumAndGroup(6, assetGroup1),
          field6_2 = selectors.fieldByNumAndGroup(6, assetGroup2),
          field6_3 = selectors.fieldByNumAndGroup(6, assetGroup3),
          file = new File(['test'], 'test.txt', { type: 'text-plain' }),
          file2 = new File(['tset'], 'tset.txt', { type: 'text-plain' }),
          file3 = new File(['asdf'], 'asdf.txt', { type: 'text-plain' });

      await userEvent.type(field2, 'bar');
      await userEvent.type(field4, 'qwerty');
      await userEvent.type(field6, 'asdf');
      await userEvent.type(field6_2, 'zxvc');
      await userEvent.type(field6_3, 'bnm,');
      setFileUploadValue(fileUpload, file);
      setFileUploadValue(fileUpload2, file2);
      setFileUploadValue(fileUpload3, file3);

      await userEvent.click(uploadBtn);

      await waitFor(() => expect(postedFormData).toBeDefined());
      expect(postedFormData.get('asset0')).toBe(file);
      expect(postedFormData.get('asset1')).toBe(file2);
      expect(postedFormData.get('asset2')).toBe(file3);
    });

    it('adds the assetFields to the FormData under their name prefixed by "asset0.", "asset1.", etc', async function() {
      let postedFormData;

      when(axios.post).calledWith('service/rest/internal/ui/upload/multi-repo', expect.anything())
          .mockImplementation((_, formData) => {
            postedFormData = formData;
            return new Promise(() => {});
          });

      render('multi-repo');

      await userEvent.click(await selectors.addAssetBtn('find'));
      await userEvent.click(selectors.addAssetBtn());

      const uploadBtn = selectors.uploadBtn(),
          assetGroup1 = selectors.assetGroup('1'),
          assetGroup2 = selectors.assetGroup('2'),
          assetGroup3 = selectors.assetGroup('3'),
          fileUpload = selectors.fileUploadByGroup(assetGroup1),
          fileUpload2 = selectors.fileUploadByGroup(assetGroup2),
          fileUpload3 = selectors.fileUploadByGroup(assetGroup3),
          field2 = selectors.fieldByNumAndGroup(2),
          field4 = selectors.fieldByNumAndGroup(4),
          field6 = selectors.fieldByNumAndGroup(6, assetGroup1),
          field7 = selectors.fieldByNumAndGroup(7, assetGroup1),
          field6_2 = selectors.fieldByNumAndGroup(6, assetGroup2),
          field7_2 = selectors.fieldByNumAndGroup(7, assetGroup2),
          field6_3 = selectors.fieldByNumAndGroup(6, assetGroup3),
          field7_3 = selectors.fieldByNumAndGroup(7, assetGroup3),
          file = new File(['test'], 'test.txt', { type: 'text-plain' }),
          file2 = new File(['tset'], 'tset.txt', { type: 'text-plain' }),
          file3 = new File(['asdf'], 'asdf.txt', { type: 'text-plain' });

      await userEvent.type(field2, 'bar');
      await userEvent.type(field4, 'qwerty');
      await userEvent.type(field6, 'asdf');
      await userEvent.type(field7, '12345');
      await userEvent.type(field6_2, 'zxcv');
      await userEvent.type(field7_2, '0987');
      await userEvent.type(field6_3, 'xcvb');
      await userEvent.type(field7_3, ';lkj');
      setFileUploadValue(fileUpload, file);
      setFileUploadValue(fileUpload2, file2);
      setFileUploadValue(fileUpload3, file3);

      await userEvent.click(uploadBtn);

      await waitFor(() => expect(postedFormData).toBeDefined());
      expect(postedFormData.get('asset0.field6')).toBe('asdf');
      expect(postedFormData.get('asset0.field7')).toBe('12345');
      expect(postedFormData.get('asset1.field6')).toBe('zxcv');
      expect(postedFormData.get('asset1.field7')).toBe('0987');
      expect(postedFormData.get('asset2.field6')).toBe('xcvb');
      expect(postedFormData.get('asset2.field7')).toBe(';lkj');
    });

    it('adds asset fields and files according to the asset key matching their final order after ' +
        'deletions are considered', async function() {
      let postedFormData;

      when(axios.post).calledWith('service/rest/internal/ui/upload/multi-repo', expect.anything())
          .mockImplementation((_, formData) => {
            postedFormData = formData;
            return new Promise(() => {});
          });

      render('multi-repo');

      await userEvent.click(await selectors.addAssetBtn('find'));
      await userEvent.click(selectors.addAssetBtn());

      const uploadBtn = selectors.uploadBtn(),
          assetGroup1 = selectors.assetGroup('1'),
          assetGroup2 = selectors.assetGroup('2'),
          assetGroup3 = selectors.assetGroup('3'),
          fileUpload = selectors.fileUploadByGroup(assetGroup1),
          fileUpload2 = selectors.fileUploadByGroup(assetGroup2),
          fileUpload3 = selectors.fileUploadByGroup(assetGroup3),
          field2 = selectors.fieldByNumAndGroup(2),
          field4 = selectors.fieldByNumAndGroup(4),
          field5 = selectors.checkboxFieldByNumAndGroup(5),
          field6 = selectors.fieldByNumAndGroup(6, assetGroup1),
          field7 = selectors.fieldByNumAndGroup(7, assetGroup1),
          field6_2 = selectors.fieldByNumAndGroup(6, assetGroup2),
          field7_2 = selectors.fieldByNumAndGroup(7, assetGroup2),
          field6_3 = selectors.fieldByNumAndGroup(6, assetGroup3),
          field7_3 = selectors.fieldByNumAndGroup(7, assetGroup3),
          file = new File(['test'], 'test.txt', { type: 'text-plain' }),
          file2 = new File(['tset'], 'tset.txt', { type: 'text-plain' }),
          file3 = new File(['asdf'], 'asdf.txt', { type: 'text-plain' });

      await userEvent.type(field2, 'bar');
      await userEvent.type(field4, 'qwerty');
      await userEvent.type(field6, 'asdf');
      await userEvent.type(field7, '12345');
      await userEvent.type(field6_2, 'zxcv');
      await userEvent.type(field7_2, '0987');
      await userEvent.type(field6_3, 'xcvb');
      await userEvent.type(field7_3, ';lkj');
      await userEvent.click(field5);
      setFileUploadValue(fileUpload, file);
      setFileUploadValue(fileUpload2, file2);
      setFileUploadValue(fileUpload3, file3);

      await userEvent.click(selectors.deleteBtnByGroup(assetGroup2));
      await userEvent.click(uploadBtn);

      await waitFor(() => expect(postedFormData).toBeDefined());
      expect(postedFormData.get('asset0')).toBe(file);
      expect(postedFormData.get('asset0.field6')).toBe('asdf');
      expect(postedFormData.get('asset0.field7')).toBe('12345');

      // not asset2
      expect(postedFormData.get('asset1')).toBe(file3);
      expect(postedFormData.get('asset1.field6')).toBe('xcvb');
      expect(postedFormData.get('asset1.field7')).toBe(';lkj');

      expect(postedFormData.get('field2')).toBe('bar');
      expect(postedFormData.get('field4')).toBe('qwerty');
      expect(postedFormData.get('field5')).toBe('true');
    });

    it('redirects to the search page with the keyword param set to the response data after the form is submitted',
        async function() {
          const hashSpy = jest.spyOn(window.location, 'hash', 'set');
          when(axios.post).calledWith('service/rest/internal/ui/upload/simple-repo', expect.anything())
              .mockResolvedValue({ data: { success: true, data: 'foo#?% bar' } });

          render();

          const form = await selectors.form('find'),
              uploadBtn = selectors.uploadBtn(),
              fileUpload = selectors.fileUploadByGroup(form),
              field1 = selectors.fieldByNumAndGroup(1),
              field2 = selectors.fieldByNumAndGroup(2),
              field3 = selectors.fieldByNumAndGroup(3),
              field4 = selectors.fieldByNumAndGroup(4),
              field6 = selectors.fieldByNumAndGroup(6),
              field7 = selectors.fieldByNumAndGroup(7),
              file = new File(['test'], 'test.txt', { type: 'text-plain' });

          await userEvent.type(field1, 'foo');
          await userEvent.type(field2, 'bar');
          await userEvent.type(field3, 'baz');
          await userEvent.type(field4, 'qwerty');
          await userEvent.type(field6, 'asdf');
          await userEvent.type(field7, '12345');
          setFileUploadValue(fileUpload, file);

          await userEvent.click(uploadBtn);

          await waitFor(
              () => expect(hashSpy).toHaveBeenCalledWith('browse/search=keyword%3D%22foo%23%3F%25%20bar%22'),
              { timeout: 1500 }
          );
        }
    );

    it('renders a submit mask while the form is submitting', async function() {
        const hashSpy = jest.spyOn(window.location, 'hash', 'set');
        when(axios.post).calledWith('service/rest/internal/ui/upload/simple-repo', expect.anything())
            .mockResolvedValue({ data: { success: true, data: 'foo#?% bar' } });

        render();

        const form = await selectors.form('find'),
            uploadBtn = selectors.uploadBtn(),
            fileUpload = selectors.fileUploadByGroup(form),
            field1 = selectors.fieldByNumAndGroup(1),
            field2 = selectors.fieldByNumAndGroup(2),
            field3 = selectors.fieldByNumAndGroup(3),
            field4 = selectors.fieldByNumAndGroup(4),
            field6 = selectors.fieldByNumAndGroup(6),
            field7 = selectors.fieldByNumAndGroup(7),
            file = new File(['test'], 'test.txt', { type: 'text-plain' });

        await userEvent.type(field1, 'foo');
        await userEvent.type(field2, 'bar');
        await userEvent.type(field3, 'baz');
        await userEvent.type(field4, 'qwerty');
        await userEvent.type(field6, 'adsf');
        await userEvent.type(field7, '12345');
        setFileUploadValue(fileUpload, file);

        await userEvent.click(uploadBtn);

        expect(screen.getByRole('status')).toHaveTextContent('Saving...');
        await waitFor(() => expect(screen.getByRole('status')).toHaveTextContent('Success!'));

        await waitFor(() => expect(hashSpy).toHaveBeenCalledWith('browse/search=keyword%3D%22foo%23%3F%25%20bar%22'));
    });

    it('renders validation error messages when Upload is clicked with data missing', async function() {
      render('multi-repo');

      await userEvent.click(await selectors.addAssetBtn('find'));

      const uploadBtn = selectors.uploadBtn(),
          assetGroup1 = selectors.assetGroup('1'),
          assetGroup2 = selectors.assetGroup('2'),
          fileUpload = selectors.fileUploadByGroup(assetGroup1),
          fileUpload2 = selectors.fileUploadByGroup(assetGroup2),
          field1 = selectors.fieldByNumAndGroup(1),
          field2 = selectors.fieldByNumAndGroup(2),
          field3 = selectors.fieldByNumAndGroup(3),
          field4 = selectors.fieldByNumAndGroup(4),
          field6 = selectors.fieldByNumAndGroup(6, assetGroup1),
          field7 = selectors.fieldByNumAndGroup(7, assetGroup1),
          field6_2 = selectors.fieldByNumAndGroup(6, assetGroup2),
          field7_2 = selectors.fieldByNumAndGroup(7, assetGroup2);

      await userEvent.click(uploadBtn);

      const formValidationAlert = selectors.errorAlert('getAll').find(el => el.textContent.includes('validation'));
      expect(formValidationAlert).toBeInTheDocument();
      expect(selectors.errorRetryBtn('query')).not.toBeInTheDocument();

      expect(fileUpload).toHaveErrorMessage('This field is required!');
      expect(fileUpload).toBeInvalid();

      expect(field1).not.toHaveErrorMessage();
      expect(field1).toBeValid();

      expect(field2).toHaveErrorMessage('This field is required');
      expect(field2).toBeInvalid();

      expect(field3).not.toHaveErrorMessage();
      expect(field3).toBeValid();

      expect(field4).toHaveErrorMessage('This field is required');
      expect(field4).toBeInvalid();

      expect(field5).not.toHaveErrorMessage();
      expect(field5).toBeValid();

      expect(field6).toHaveErrorMessage('This field is required');
      expect(field6).toBeInvalid();

      expect(field7).not.toHaveErrorMessage();
      expect(field7).toBeValid();

      expect(field6_2).toHaveErrorMessage('This field is required');
      expect(field6_2).toBeInvalid();

      expect(field7_2).not.toHaveErrorMessage();
      expect(field7_2).toBeValid();
    });

    it('renders validation error messages when Upload is clicked with only the file missing', async function() {
      render();

      const form = await selectors.form('find'),
          uploadBtn = selectors.uploadBtn(),
          fileUpload = selectors.fileUploadByGroup(form),
          field1 = selectors.fieldByNumAndGroup(1),
          field2 = selectors.fieldByNumAndGroup(2),
          field3 = selectors.fieldByNumAndGroup(3),
          field4 = selectors.fieldByNumAndGroup(4),
          field6 = selectors.fieldByNumAndGroup(6),
          field7 = selectors.fieldByNumAndGroup(7);

      await userEvent.type(field2, 'bar');
      await userEvent.type(field4, 'bar');
      await userEvent.type(field6, 'bar');
      await userEvent.type(field7, 'bar');
      await userEvent.click(uploadBtn);

      const formValidationAlert = selectors.errorAlert('getAll').find(el => el.textContent.includes('validation'));
      expect(formValidationAlert).toBeInTheDocument();
      expect(selectors.errorRetryBtn('query')).not.toBeInTheDocument();

      expect(fileUpload).toHaveErrorMessage('This field is required!');
      expect(fileUpload).toBeInvalid();
    });

    it('renders validation error messages when Upload is clicked with non-unique assets', async function() {
      render('multi-repo');

      await userEvent.click(await selectors.addAssetBtn('find'));

      const uploadBtn = selectors.uploadBtn(),
          assetGroup1 = selectors.assetGroup('1'),
          assetGroup2 = selectors.assetGroup('2'),
          fileUpload = selectors.fileUploadByGroup(assetGroup1),
          fileUpload2 = selectors.fileUploadByGroup(assetGroup2),
          field1 = selectors.fieldByNumAndGroup(1),
          field2 = selectors.fieldByNumAndGroup(2),
          field3 = selectors.fieldByNumAndGroup(3),
          field4 = selectors.fieldByNumAndGroup(4),
          field6 = selectors.fieldByNumAndGroup(6, assetGroup1),
          field7 = selectors.fieldByNumAndGroup(7, assetGroup1),
          field6_2 = selectors.fieldByNumAndGroup(6, assetGroup2),
          field7_2 = selectors.fieldByNumAndGroup(7, assetGroup2);

      await userEvent.type(field2, 'asdf');
      await userEvent.type(field4, 'qwerty');

      await userEvent.type(field6, 'a');
      await userEvent.type(field7, 'b');
      await userEvent.type(field6_2, 'a');
      await userEvent.type(field7_2, 'b');

      await userEvent.click(uploadBtn);

      const formValidationAlert = selectors.errorAlert('getAll').find(el => el.textContent.includes('validation'));
      expect(formValidationAlert).toBeInTheDocument();
      expect(selectors.errorRetryBtn('query')).not.toBeInTheDocument();

      expect(field6).toHaveErrorMessage('Asset not unique');
      expect(field6).toBeInvalid();

      expect(field7).toHaveErrorMessage('Asset not unique');
      expect(field7).toBeInvalid();

      expect(field6_2).toHaveErrorMessage('Asset not unique');
      expect(field6_2).toBeInvalid();

      expect(field7_2).toHaveErrorMessage('Asset not unique');
      expect(field7_2).toBeInvalid();
    });

    it('submits the upload successfully after validation errors are fixed', async function() {
      const hashSpy = jest.spyOn(window.location, 'hash', 'set');
      when(axios.post).calledWith('service/rest/internal/ui/upload/multi-repo', expect.anything())
          .mockResolvedValue({ data: { success: true, data: 'foobar' } });

      render('multi-repo');

      await userEvent.click(await selectors.addAssetBtn('find'));

      const uploadBtn = selectors.uploadBtn(),
          assetGroup1 = selectors.assetGroup('1'),
          assetGroup2 = selectors.assetGroup('2'),
          fileUpload = selectors.fileUploadByGroup(assetGroup1),
          fileUpload2 = selectors.fileUploadByGroup(assetGroup2),
          field1 = selectors.fieldByNumAndGroup(1),
          field2 = selectors.fieldByNumAndGroup(2),
          field3 = selectors.fieldByNumAndGroup(3),
          field4 = selectors.fieldByNumAndGroup(4),
          field6 = selectors.fieldByNumAndGroup(6, assetGroup1),
          field7 = selectors.fieldByNumAndGroup(7, assetGroup1),
          field6_2 = selectors.fieldByNumAndGroup(6, assetGroup2),
          field7_2 = selectors.fieldByNumAndGroup(7, assetGroup2),
          file = new File(['test'], 'test.txt', { type: 'text-plain' }),
          file2 = new File(['tset'], 'tset.txt', { type: 'text-plain' });

      await userEvent.click(uploadBtn);

      const formValidationAlert = selectors.errorAlert('getAll').find(el => el.textContent.includes('validation'));
      expect(formValidationAlert).toBeInTheDocument();
      expect(selectors.errorRetryBtn('query')).not.toBeInTheDocument();

      expect(fileUpload).toHaveErrorMessage('This field is required!');
      expect(fileUpload).toBeInvalid();
      expect(fileUpload2).toHaveErrorMessage('This field is required!');
      expect(fileUpload2).toBeInvalid();
      expect(field2).toHaveErrorMessage('This field is required');
      expect(field2).toBeInvalid();
      expect(field4).toHaveErrorMessage('This field is required');
      expect(field4).toBeInvalid();
      expect(field6).toHaveErrorMessage('This field is required');
      expect(field6).toBeInvalid();
      expect(field6_2).toHaveErrorMessage('This field is required');
      expect(field6_2).toBeInvalid();

      setFileUploadValue(fileUpload, file);
      expect(fileUpload).not.toHaveErrorMessage();
      expect(fileUpload).toBeValid();

      setFileUploadValue(fileUpload2, file2);
      expect(fileUpload2).not.toHaveErrorMessage();
      expect(fileUpload2).toBeValid();

      await userEvent.type(field2, 'bar');
      expect(field2).not.toHaveErrorMessage();
      expect(field2).toBeValid();

      await userEvent.type(field4, 'qwerty');
      expect(field4).not.toHaveErrorMessage();
      expect(field4).toBeValid();

      await userEvent.type(field6, 'qwerty');
      expect(field6).not.toHaveErrorMessage();
      expect(field6).toBeValid();

      await userEvent.type(field6_2, 'qwerty');
      expect(field6).toHaveErrorMessage('Asset not unique');
      expect(field6).toBeInvalid();
      expect(field6_2).toHaveErrorMessage('Asset not unique');
      expect(field6_2).toBeInvalid();

      await userEvent.type(field6_2, '2');
      expect(field6).not.toHaveErrorMessage();
      expect(field6).toBeValid();
      expect(field7).not.toHaveErrorMessage();
      expect(field7).toBeValid();
      expect(field6_2).not.toHaveErrorMessage();
      expect(field6_2).toBeValid();
      expect(field7_2).not.toHaveErrorMessage();
      expect(field7_2).toBeValid();
      expect(formValidationAlert).not.toBeInTheDocument();

      await userEvent.click(selectors.uploadBtn());

      await waitFor(
          () => expect(hashSpy).toHaveBeenCalledWith('browse/search=keyword%3D%22foobar%22'),
          { timeout: 1500 }
      );
    });

    it('renders an error with a retry button if the form POST fails', async function() {
      const hashSpy = jest.spyOn(window.location, 'hash', 'set');
      when(axios.post).calledWith('service/rest/internal/ui/upload/simple-repo', expect.anything())
          .mockRejectedValue({ message: 'foobar' });

      render();

      const form = await selectors.form('find'),
          uploadBtn = selectors.uploadBtn(),
          fileUpload = selectors.fileUploadByGroup(form),
          field1 = selectors.fieldByNumAndGroup(1),
          field2 = selectors.fieldByNumAndGroup(2),
          field3 = selectors.fieldByNumAndGroup(3),
          field4 = selectors.fieldByNumAndGroup(4),
          field6 = selectors.fieldByNumAndGroup(6),
          field7 = selectors.fieldByNumAndGroup(7),
          file = new File(['test'], 'test.txt', { type: 'text-plain' });

      await userEvent.type(field1, 'foo');
      await userEvent.type(field2, 'bar');
      await userEvent.type(field3, 'baz');
      await userEvent.type(field4, 'qwerty');
      await userEvent.type(field6, 'qwerty');
      await userEvent.type(field7, 'qwerty');
      setFileUploadValue(fileUpload, file);

      await userEvent.click(uploadBtn);

      const submitAlert = await selectors.errorAlert('find');
      expect(submitAlert).toBeInTheDocument();
      expect(submitAlert).toHaveTextContent('foobar');

      const retryBtn = selectors.errorRetryBtn();
      expect(retryBtn).toBeInTheDocument();
    });

    it('retries the POST if the submit retry button is clicked', async function() {
      const hashSpy = jest.spyOn(window.location, 'hash', 'set');
      when(axios.post).calledWith('service/rest/internal/ui/upload/simple-repo', expect.anything())
          .mockRejectedValueOnce({ message: 'foobar' });
      when(axios.post).calledWith('service/rest/internal/ui/upload/simple-repo', expect.anything())
          .mockResolvedValueOnce({ data: { success: true, data: 'asdf' } });

      render();

      const form = await selectors.form('find'),
          uploadBtn = selectors.uploadBtn(),
          fileUpload = selectors.fileUploadByGroup(form),
          field2 = selectors.fieldByNumAndGroup(2),
          field4 = selectors.fieldByNumAndGroup(4),
          field6 = selectors.fieldByNumAndGroup(6),
          field7 = selectors.fieldByNumAndGroup(7),
          file = new File(['test'], 'test.txt', { type: 'text-plain' });

      await userEvent.type(field2, 'bar');
      await userEvent.type(field4, 'qwerty');
      await userEvent.type(field6, 'qwerty');
      await userEvent.type(field7, 'qwerty');
      setFileUploadValue(fileUpload, file);

      await userEvent.click(uploadBtn);
      await userEvent.click(await selectors.errorRetryBtn('find'));

      await waitFor(
          () => expect(hashSpy).toHaveBeenCalledWith('browse/search=keyword%3D%22asdf%22'),
          { timeout: 1500 }
      );
    });
  });

  describe('maven special rules', function() {
    it('disables the Group ID, Artifact ID, Version, "Generate a POM file...", and Packaging fields when at least ' +
        'one asset extension is "pom" after trimming', async function() {
      render('maven-repo');

      await userEvent.click(await selectors.addAssetBtn('find'));

      const assetGroup1 = selectors.assetGroup('1'),
          assetGroup2 = selectors.assetGroup('2'),
          fileUpload = selectors.fileUploadByGroup(assetGroup1),
          fileUpload2 = selectors.fileUploadByGroup(assetGroup2),
          extension1 = selectors.mavenExtensionField(assetGroup1),
          extension2 = selectors.mavenExtensionField(assetGroup2),
          group = selectors.mavenGroupField(),
          artifact = selectors.mavenArtifactField(),
          version = selectors.mavenVersionField(),
          generatePom = selectors.mavenGeneratePomField(),
          packaging = selectors.mavenPackagingField(),
          file = new File(['test'], 'test-1.0.jar', { type: 'text-plain' }),
          file2 = new File(['tset'], 'tset-1.0.pom', { type: 'text-plain' });

      expect(group).toBeEnabled();
      expect(artifact).toBeEnabled();
      expect(version).toBeEnabled();
      expect(generatePom).toBeEnabled();

      await userEvent.type(extension1, ' po');

      expect(group).toBeEnabled();
      expect(artifact).toBeEnabled();
      expect(version).toBeEnabled();
      expect(generatePom).toBeEnabled();

      await userEvent.type(extension1, 'm ');

      expect(group).toBeDisabled();
      expect(artifact).toBeDisabled();
      expect(version).toBeDisabled();
      expect(generatePom).toBeDisabled();
      expect(packaging).toBeDisabled();

      await userEvent.type(extension1, 'm');

      expect(group).toBeEnabled();
      expect(artifact).toBeEnabled();
      expect(version).toBeEnabled();
      expect(generatePom).toBeEnabled();

      await userEvent.clear(extension1);

      expect(group).toBeEnabled();
      expect(artifact).toBeEnabled();
      expect(version).toBeEnabled();
      expect(generatePom).toBeEnabled();

      setFileUploadValue(fileUpload, file);

      expect(group).toBeEnabled();
      expect(artifact).toBeEnabled();
      expect(version).toBeEnabled();
      expect(generatePom).toBeEnabled();

      setFileUploadValue(fileUpload2, file2);

      expect(extension2).toHaveValue('pom');
      expect(group).toBeDisabled();
      expect(artifact).toBeDisabled();
      expect(version).toBeDisabled();
      expect(generatePom).toBeDisabled();
      expect(packaging).toBeDisabled();

      await userEvent.click(selectors.deleteBtnByGroup(assetGroup2));

      expect(group).toBeEnabled();
      expect(artifact).toBeEnabled();
      expect(version).toBeEnabled();
      expect(generatePom).toBeEnabled();
    });

    it('disables the Packaging field whenever the "Generate..." checkbox is unchecked', async function() {
      render('maven-repo');

      const extension = await selectors.mavenExtensionField(undefined, 'find'),
          generatePom = selectors.mavenGeneratePomField(),
          packaging = selectors.mavenPackagingField();

      expect(generatePom).not.toBeChecked();
      expect(packaging).toBeDisabled();

      await userEvent.click(generatePom);
      expect(packaging).toBeEnabled();

      await userEvent.click(generatePom);
      expect(packaging).toBeDisabled();

      await userEvent.click(generatePom);
      expect(packaging).toBeEnabled();

      // If there's a pom extension, packaging should be disabled regardless of the checkbox state
      await userEvent.type(extension, 'pom');
      expect(packaging).toBeDisabled();
    });

    it('excludes the group, artifact, and version fields from validation when they are disabled', async function() {
      let postedFormData;

      when(axios.post).calledWith('service/rest/internal/ui/upload/maven-repo', expect.anything())
          .mockImplementation((_, formData) => {
            postedFormData = formData;
            return new Promise(() => {});
          });

      render('maven-repo');

      const assetGroup1 = await selectors.assetGroup('1', 'find'),
          fileUpload = selectors.fileUploadByGroup(assetGroup1),
          extension = selectors.mavenExtensionField(),
          group = selectors.mavenGroupField(),
          artifact = selectors.mavenArtifactField(),
          version = selectors.mavenVersionField(),
          generatePom = selectors.mavenGeneratePomField(),
          packaging = selectors.mavenPackagingField(),
          submit = selectors.uploadBtn(),
          file = new File(['tset'], 'tset-1.0.pom', { type: 'text-plain' });

      setFileUploadValue(fileUpload, file);
      await userEvent.clear(extension);
      await userEvent.click(submit);

      expect(group).toHaveErrorMessage('This field is required');
      expect(artifact).toHaveErrorMessage('This field is required');
      expect(version).toHaveErrorMessage('This field is required');

      await userEvent.type(extension, 'pom');

      // check validation on all three
      expect(group).toBeValid();
      expect(artifact).toBeValid();
      expect(version).toBeValid();

      // temporarily re-enable the fields so we can set values into some of them so we can assert those values
      // don't end up in the submitted FormData.  Don't add values to all of them however, because we also want
      // to check that the form is submittable when these otherwise-required fields are empty and disabled
      await userEvent.clear(extension);
      await userEvent.type(group, 'g');
      await userEvent.type(extension, 'pom');

      await userEvent.click(submit);

      expect(postedFormData.has('groupId')).toBe(false);
      expect(postedFormData.has('artifactId')).toBe(false);
      expect(postedFormData.has('version')).toBe(false);
    });

    it('sets "" as the uploaded value of the classifier field when nothing is extracted from the regex for that field',
        async function() {
          let postedFormData;

          when(axios.post).calledWith('service/rest/internal/ui/upload/maven-repo', expect.anything())
              .mockImplementation((_, formData) => {
                postedFormData = formData;
                return new Promise(() => {});
              });

          render('maven-repo');

          await userEvent.click(await selectors.addAssetBtn('find'));

          const assetGroup1 = selectors.assetGroup('1'),
              assetGroup2 = selectors.assetGroup('2'),
              fileUpload = selectors.fileUploadByGroup(assetGroup1),
              fileUpload2 = selectors.fileUploadByGroup(assetGroup2),
              classifier1 = selectors.mavenClassifierField(assetGroup1),
              classifier2 = selectors.mavenClassifierField(assetGroup2),
              submit = selectors.uploadBtn(),
              file = new File(['tset'], 'tset-1.0-sources.pom', { type: 'text-plain' }),
              file2 = new File(['tset'], 'tset-1.0.pom', { type: 'text-plain' });

          setFileUploadValue(fileUpload, file);
          setFileUploadValue(fileUpload2, file2);

          expect(classifier1).toHaveValue('sources');
          expect(classifier2).toHaveValue('');

          await userEvent.click(submit);

          expect(postedFormData.get('asset0.classifier')).toBe('sources');
          expect(postedFormData.get('asset1.classifier')).toBe('');
        }
    );
  });
});

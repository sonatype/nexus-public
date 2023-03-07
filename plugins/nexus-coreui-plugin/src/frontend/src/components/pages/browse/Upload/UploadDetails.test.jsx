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
import { fireEvent, render as rtlRender, screen, waitFor, waitForElementToBeRemoved, within }
  from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { when } from 'jest-when';

import UploadDetails from './UploadDetails.jsx';

// Creates a selector function that uses getByRole by default but which can be customized per-use to use
// queryByRole, findByRole, etc instead
const selectorQuery = (...queryParams) => queryType => screen[`${queryType ?? 'get'}ByRole`].apply(screen, queryParams);

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
  dismissUploadBtn: selectorQuery('button', { name: 'Dismiss Upload' })
};

const sampleRepoSettings = {
  data: [
    { name: 'other-repo', format: 'maven2' },
    { name: 'repo-id', format: 'nuget' }
  ]
};

const sampleUploadDefinitions = {
  data: {
    result: {
      success: true,
      data: [{
        format: 'foo'
      }, {
        format: 'nuget',
        componentFields: [{
          displayName: 'Field 1',
          group: 'A',
          helpText: null,
          name: 'field1',
          optional: true,
          type: 'STRING'
        }, {
          displayName: 'Field 2',
          group: 'B',
          helpText: 'This is the second field',
          name: 'field2',
          optional: false,
          type: 'STRING'
        }, {
          displayName: 'Field 3',
          group: 'A',
          helpText: null,
          name: 'field3',
          optional: true,
          type: 'STRING'
        }, {
          displayName: 'Field 4',
          group: 'C',
          helpText: 'FOUR',
          name: 'field4',
          optional: false,
          type: 'STRING'
        }, {
          displayName: 'Field 5',
          group: 'C',
          helpText: null,
          name: 'field5',
          optional: true,
          type: 'CHECKBOX'
        }],
        assetFields: [{
          displayName: 'Field 6',
          helpText: 'SIX',
          name: 'field6',
          optional: false,
          type: 'STRING'
        }, {
          displayName: 'Field 7',
          helpText: 'SEVEN',
          name: 'field7',
          optional: true,
          type: 'STRING'
        }]
      }, {
        format: 'bar'
      }]
    }
  }
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
        filesMap.set(this, val);
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
          this.files = emptyFileList;
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
        get: () => 'browse/upload:repo-id'
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
    length: files.length
  };

  Object.setPrototypeOf(retval, FileList.prototype);

  return retval;
}

function setFileUploadValue(fileUpload, ...files) {
  fireEvent.change(fileUpload, {
    target: {
      files: fakeFileList(...files)
    }
  });
}

describe('UploadDetails', function() {
  mockFileInputFilesSetter();
  mockWindowLocation();

  beforeEach(function() {
    jest.spyOn(axios, 'get');
    jest.spyOn(axios, 'post');

    when(axios.get).calledWith('/service/rest/v1/repositorySettings')
        .mockResolvedValue(sampleRepoSettings);

    when(axios.post).calledWith(
        '/service/extdirect',
        expect.objectContaining({ action: 'coreui_Upload', method: 'getUploadDefinitions' })
    ).mockResolvedValue(sampleUploadDefinitions);
  });

  function render() {
    return rtlRender(<UploadDetails itemId="repo-id" />);
  }

  it('renders a main content area', function() {
    // resolving the promise in this otherwise-synchronous test causes act errors, so just leave it unresolved here
    jest.spyOn(axios, 'get').mockReturnValue(new Promise(() => {}));
    render();

    expect(selectors.main()).toBeInTheDocument();
  });

  describe('initial loading', function() {
    it('renders a loading spinner before the GET completes', function() {
      when(axios.get).calledWith('/service/rest/v1/repositorySettings')
          .mockReturnValue(new Promise(() => {}));

      render();

      const loadingStatus = selectors.loadingStatus();
      expect(loadingStatus).toBeInTheDocument();
      expect(loadingStatus).toHaveTextContent(/loading/i);
    });

    it('renders a loading spinner before the extdirect POST completes', function() {
      when(axios.post).calledWith(
          '/service/extdirect',
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

    it('renders an error alert with a Retry button if the GET fails', async function() {
      when(axios.get).calledWith('/service/rest/v1/repositorySettings')
          .mockRejectedValue({ message: 'foobar' });

      render();

      const loadingStatus = selectors.loadingStatus();

      await waitForElementToBeRemoved(loadingStatus);
      expect(selectors.form('query')).not.toBeInTheDocument();
      expect(selectors.errorAlert()).toBeInTheDocument();
      expect(selectors.errorAlert()).toHaveTextContent('foobar');
      expect(selectors.errorRetryBtn()).toBeInTheDocument();
    });

    it('renders an error alert with a Retry button if extdirect call fails at the HTTP level', async function() {
      when(axios.post).calledWith(
          '/service/extdirect',
          expect.objectContaining({ action: 'coreui_Upload', method: 'getUploadDefinitions' })
      ).mockRejectedValue({ message: 'foobar' });

      render();

      const loadingStatus = selectors.loadingStatus();

      await waitForElementToBeRemoved(loadingStatus);
      expect(selectors.form('query')).not.toBeInTheDocument();
      expect(selectors.errorAlert()).toBeInTheDocument();
      expect(selectors.errorAlert()).toHaveTextContent('foobar');
      expect(selectors.errorRetryBtn()).toBeInTheDocument();
    });

    it('renders an error alert with a Retry button if extdirect call fails at the extdirect level', async function() {
      when(axios.post).calledWith(
          '/service/extdirect',
          expect.objectContaining({ action: 'coreui_Upload', method: 'getUploadDefinitions' })
      ).mockResolvedValue({ data: { result: { success: false, message: 'foobar' } } });

      render();

      const loadingStatus = selectors.loadingStatus();

      await waitForElementToBeRemoved(loadingStatus);
      expect(selectors.form('query')).not.toBeInTheDocument();
      expect(selectors.errorAlert()).toBeInTheDocument();
      expect(selectors.errorAlert()).toHaveTextContent('foobar');
      expect(selectors.errorRetryBtn()).toBeInTheDocument();
    });

    it('re-runs the GET and the extdirect call when the Retry button is clicked, and renders the form if they succeed',
        async function() {
          when(axios.get).calledWith('/service/rest/v1/repositorySettings')
              .mockRejectedValueOnce({ message: 'foobar' })
              .mockResolvedValueOnce(sampleRepoSettings);

          render();

          const loadingStatus = selectors.loadingStatus();

          await waitForElementToBeRemoved(loadingStatus);
          await userEvent.click(selectors.errorRetryBtn());

          expect(await selectors.form('find')).toBeInTheDocument();
        }
    );
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
    const expectedHeading = 'Choose Assets/Components for repo-id Repository';

    render();

    const form = await selectors.form('find'),
        region = within(form).getByRole('region', { name: expectedHeading }),
        h2 = within(region).getByRole('heading', { level: 2 });

    expect(region).toBeInTheDocument();
    expect(h2).toHaveTextContent(expectedHeading);
  });

  it('renders a file upload within the "Choose Assets..." region with an accessible name of "File", and an ' +
      'initial accessible description of "No file selected"', async function() {
    render();

    const region = await selectors.chooseAssetsRegion('find'),
        fileUpload = region.querySelector('input[type=file]');

    expect(fileUpload).toBeInTheDocument();
    expect(fileUpload).toHaveAccessibleName('File');
    expect(fileUpload).toHaveAccessibleDescription('No file selected');
  });

  it('renders an aria-hidden "Choose File" button within the "Choose Assets..." region', async function() {
    render();

    const region = await selectors.chooseAssetsRegion('find'),
        fileUpload = region.querySelector('input[type=file]'),
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
            fileUpload = region.querySelector('input[type=file]');

        // NOTE: not using userEvent.upload because it incorrectly makes the input's files' property not-writable
        setFileUploadValue(fileUpload, new File(['123456'], 'numbers.txt', { type: 'text/plain' }));

        expect(region).toHaveTextContent('numbers.txt');
        expect(region).toHaveTextContent('6.0 B');
        expect(fileUpload).toHaveAccessibleDescription('numbers.txt 6.0 B');
      }
  );

  it('renders the asset fields within the "Choose Assets..." region', async function() {
      render();

      const region = await selectors.chooseAssetsRegion('find'),
        field6 = screen.getByRole('textbox', { name: 'Field 6' }),
        field7 = screen.getByRole('textbox', { name: 'Field 7' });

    expect(region).toContainElement(field6);
    expect(region).toContainElement(field7);
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
        field1 = screen.getByRole('textbox', { name: 'Field 1' }),
        field2 = screen.getByRole('textbox', { name: 'Field 2' }),
        field3 = screen.getByRole('textbox', { name: 'Field 3' }),
        field4 = screen.getByRole('textbox', { name: 'Field 4' });

    expect(regionA).toContainElement(field1);
    expect(regionA).toContainElement(field3);
    expect(regionB).toContainElement(field2);
    expect(regionC).toContainElement(field4);
  });

  it('marks the text fields that do not have the optional flag set as required', async function() {
    render();

    const form = await selectors.form('find'),
        field1 = screen.getByRole('textbox', { name: 'Field 1' }),
        field2 = screen.getByRole('textbox', { name: 'Field 2' }),
        field3 = screen.getByRole('textbox', { name: 'Field 3' }),
        field4 = screen.getByRole('textbox', { name: 'Field 4' }),
        field6 = screen.getByRole('textbox', { name: 'Field 6' }),
        field7 = screen.getByRole('textbox', { name: 'Field 7' });

    expect(field1).not.toBeRequired();
    expect(field2).toBeRequired();
    expect(field3).not.toBeRequired();
    expect(field4).toBeRequired();
    expect(field6).toBeRequired();
    expect(field7).not.toBeRequired();
  });

  it('allows values to be set into the text fields', async function() {
    render();

    const field1 = await screen.findByRole('textbox', { name: 'Field 1' }),
        field2 = screen.getByRole('textbox', { name: 'Field 2' }),
        field3 = screen.getByRole('textbox', { name: 'Field 3' }),
        field4 = screen.getByRole('textbox', { name: 'Field 4' }),
        field6 = screen.getByRole('textbox', { name: 'Field 6' }),
        field7 = screen.getByRole('textbox', { name: 'Field 7' });

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
  });

  it('allows a file to be set into the file input and renders its name and size', async function() {
    render();

    const form = await selectors.form('find'),
        fileUpload = form.querySelector('input[type=file]');

    setFileUploadValue(fileUpload, new File(['123456'], 'numbers.txt', { type: 'text/plain' }));
    expect(screen.getByText('numbers.txt')).toBeInTheDocument();
    expect(screen.getByText('6.0 B')).toBeInTheDocument();
    expect(fileUpload).toHaveAccessibleDescription('numbers.txt 6.0 B');
  });

  it('renders a button to clear the selected file when there is one', async function() {
    render();

    const form = await selectors.form('find'),
        fileUpload = form.querySelector('input[type=file]');

    expect(selectors.dismissUploadBtn('query')).not.toBeInTheDocument();

    setFileUploadValue(fileUpload, new File(['123456'], 'numbers.txt', { type: 'text/plain' }));

    const dismissBtn = selectors.dismissUploadBtn();
    expect(dismissBtn).toBeInTheDocument();

    await userEvent.click(dismissBtn);

    expect(fileUpload).toHaveValue('');
    expect(fileUpload.files).toHaveLength(0);
    expect(screen.queryByText('numbers.txt')).not.toBeInTheDocument();
    expect(screen.queryByText('6.0 B')).not.toBeInTheDocument();
    expect(fileUpload).toHaveAccessibleDescription('No file selected');
    expect(dismissBtn).not.toBeInTheDocument();
  });

  describe('field validation', function() {
    it('adds "This field is required" error text to required text inputs when they are empty and non-pristine',
        async function() {
          render();

          const field1 = await screen.findByRole('textbox', { name: 'Field 1' }),
              field2 = screen.getByRole('textbox', { name: 'Field 2' }),
              field3 = screen.getByRole('textbox', { name: 'Field 3' }),
              field4 = screen.getByRole('textbox', { name: 'Field 4' }),
              field6 = screen.getByRole('textbox', { name: 'Field 6' }),
              field7 = screen.getByRole('textbox', { name: 'Field 7' });

          expect(screen.queryByText('This field is required')).not.toBeInTheDocument();
          expect(field1).not.toHaveErrorMessage();
          expect(field2).not.toHaveErrorMessage();
          expect(field3).not.toHaveErrorMessage();
          expect(field4).not.toHaveErrorMessage();
          expect(field6).not.toHaveErrorMessage();
          expect(field7).not.toHaveErrorMessage();
          expect(field1).toBeValid();
          expect(field2).toBeValid();
          expect(field3).toBeValid();
          expect(field4).toBeValid();
          expect(field6).toBeValid();
          expect(field7).toBeValid();

          await userEvent.type(field1, 'foo');
          await userEvent.type(field2, 'bar');
          await userEvent.type(field3, 'baz');
          await userEvent.type(field4, 'qwerty');
          await userEvent.type(field6, 'asdf');
          await userEvent.type(field7, '12345');

          expect(screen.queryByText('This field is required')).not.toBeInTheDocument();
          expect(field1).not.toHaveErrorMessage();
          expect(field2).not.toHaveErrorMessage();
          expect(field3).not.toHaveErrorMessage();
          expect(field4).not.toHaveErrorMessage();
          expect(field6).not.toHaveErrorMessage();
          expect(field7).not.toHaveErrorMessage();
          expect(field1).toBeValid();
          expect(field2).toBeValid();
          expect(field3).toBeValid();
          expect(field4).toBeValid();
          expect(field6).toBeValid();
          expect(field7).toBeValid();

          await userEvent.clear(field1);
          await userEvent.clear(field2);
          await userEvent.clear(field3);
          await userEvent.clear(field4);
          await userEvent.clear(field6);
          await userEvent.clear(field7);

          expect(screen.getAllByText('This field is required')).toHaveLength(3);
          expect(field1).not.toHaveErrorMessage();
          expect(field2).toHaveErrorMessage('This field is required');
          expect(field3).not.toHaveErrorMessage();
          expect(field4).toHaveErrorMessage('This field is required');
          expect(field6).toHaveErrorMessage('This field is required');
          expect(field7).not.toHaveErrorMessage();
          expect(field1).toBeValid();
          expect(field2).toBeInvalid();
          expect(field3).toBeValid();
          expect(field4).toBeInvalid();
          expect(field6).toBeInvalid();
          expect(field7).toBeValid();
        }
    );

    it('adds "This field is required!" error text to the file upload when it is empty and non-pristine',
        async function() {
          render();

          const form = await selectors.form('find'),
              fileUpload = form.querySelector('input[type=file]');

          expect(screen.queryByText('This field is required!')).not.toBeInTheDocument();
          expect(fileUpload).not.toHaveErrorMessage();
          expect(fileUpload).toBeValid();

          setFileUploadValue(fileUpload, new File(['test'], 'test.txt', { type: 'text-plain' }));

          expect(screen.queryByText('This field is required!')).not.toBeInTheDocument();
          expect(fileUpload).not.toHaveErrorMessage();
          expect(fileUpload).toBeValid();

          setFileUploadValue(fileUpload);

          expect(screen.getByText('This field is required!')).toBeInTheDocument();
          expect(fileUpload).toHaveErrorMessage('This field is required!');
          expect(fileUpload).toBeInvalid();
        }
    );
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

      when(axios.post).calledWith('service/rest/internal/ui/upload/repo-id', expect.anything())
          .mockImplementation((_, formData) => {
            postedFormData = formData;
            return new Promise(() => {});
          });

      render();

      const form = await selectors.form('find'),
          uploadBtn = selectors.uploadBtn(),
          fileUpload = form.querySelector('input[type=file]'),
          field2 = screen.getByRole('textbox', { name: 'Field 2' }),
          field4 = screen.getByRole('textbox', { name: 'Field 4' }),
          field6 = screen.getByRole('textbox', { name: 'Field 6' }),
          file = new File(['test'], 'test.txt', { type: 'text-plain' });

      await userEvent.type(field2, 'bar');
      await userEvent.type(field4, 'qwerty');
      await userEvent.type(field6, 'asdf');
      setFileUploadValue(fileUpload, file);

      expect(postedFormData).not.toBeDefined();

      await userEvent.click(uploadBtn);

      await waitFor(() => expect(postedFormData).toBeDefined());
    });

    it('adds the component fields to the FormData by name', async function() {
      let postedFormData;

      when(axios.post).calledWith('service/rest/internal/ui/upload/repo-id', expect.anything())
          .mockImplementation((_, formData) => {
            postedFormData = formData;
            return new Promise(() => {});
          });

      render();

      const form = await selectors.form('find'),
          uploadBtn = selectors.uploadBtn(),
          fileUpload = form.querySelector('input[type=file]'),
          field1 = screen.getByRole('textbox', { name: 'Field 1' }),
          field2 = screen.getByRole('textbox', { name: 'Field 2' }),
          field3 = screen.getByRole('textbox', { name: 'Field 3' }),
          field4 = screen.getByRole('textbox', { name: 'Field 4' }),
          field6 = screen.getByRole('textbox', { name: 'Field 6' }),
          field7 = screen.getByRole('textbox', { name: 'Field 7' }),
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

    it('adds the file to the FormData under the name "asset0"', async function() {
      let postedFormData;

      when(axios.post).calledWith('service/rest/internal/ui/upload/repo-id', expect.anything())
          .mockImplementation((_, formData) => {
            postedFormData = formData;
            return new Promise(() => {});
          });

      render();

      const form = await selectors.form('find'),
          uploadBtn = selectors.uploadBtn(),
          fileUpload = form.querySelector('input[type=file]'),
          field2 = screen.getByRole('textbox', { name: 'Field 2' }),
          field4 = screen.getByRole('textbox', { name: 'Field 4' }),
          field6 = screen.getByRole('textbox', { name: 'Field 6' }),
          file = new File(['test'], 'test.txt', { type: 'text-plain' });

      await userEvent.type(field2, 'bar');
      await userEvent.type(field4, 'qwerty');
      await userEvent.type(field6, 'asdf');
      setFileUploadValue(fileUpload, file);

      await userEvent.click(uploadBtn);

      await waitFor(() => expect(postedFormData).toBeDefined());
      expect(postedFormData.get('asset0')).toBe(file);
    });

    it('adds the assetFields to the FormData under their name prefixed by "asset0."', async function() {
      let postedFormData;

      when(axios.post).calledWith('service/rest/internal/ui/upload/repo-id', expect.anything())
          .mockImplementation((_, formData) => {
            postedFormData = formData;
            return new Promise(() => {});
          });

      render();

      const form = await selectors.form('find'),
          uploadBtn = selectors.uploadBtn(),
          fileUpload = form.querySelector('input[type=file]'),
          field2 = screen.getByRole('textbox', { name: 'Field 2' }),
          field4 = screen.getByRole('textbox', { name: 'Field 4' }),
          field6 = screen.getByRole('textbox', { name: 'Field 6' }),
          field7 = screen.getByRole('textbox', { name: 'Field 7' }),
          file = new File(['test'], 'test.txt', { type: 'text-plain' });

      await userEvent.type(field2, 'bar');
      await userEvent.type(field4, 'qwerty');
      await userEvent.type(field6, 'asdf');
      await userEvent.type(field7, '12345');
      setFileUploadValue(fileUpload, file);

      await userEvent.click(uploadBtn);

      await waitFor(() => expect(postedFormData).toBeDefined());
      expect(postedFormData.get('asset0.field6')).toBe('asdf');
      expect(postedFormData.get('asset0.field7')).toBe('12345');
    });

    it('redirects to the search page with the keyword param set to the response data after the form is submitted',
        async function() {
          const hashSpy = jest.spyOn(window.location, 'hash', 'set');
          when(axios.post).calledWith('service/rest/internal/ui/upload/repo-id', expect.anything())
              .mockResolvedValue({ data: { success: true, data: 'foo#?% bar' } });

          render();

          const form = await selectors.form('find'),
              uploadBtn = selectors.uploadBtn(),
              fileUpload = form.querySelector('input[type=file]'),
              field1 = screen.getByRole('textbox', { name: 'Field 1' }),
              field2 = screen.getByRole('textbox', { name: 'Field 2' }),
              field3 = screen.getByRole('textbox', { name: 'Field 3' }),
              field4 = screen.getByRole('textbox', { name: 'Field 4' }),
              field6 = screen.getByRole('textbox', { name: 'Field 6' }),
              field7 = screen.getByRole('textbox', { name: 'Field 7' }),
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
        when(axios.post).calledWith('service/rest/internal/ui/upload/repo-id', expect.anything())
            .mockResolvedValue({ data: { success: true, data: 'foo#?% bar' } });

        render();

        const form = await selectors.form('find'),
            uploadBtn = selectors.uploadBtn(),
            fileUpload = form.querySelector('input[type=file]'),
            field1 = screen.getByRole('textbox', { name: 'Field 1' }),
            field2 = screen.getByRole('textbox', { name: 'Field 2' }),
            field3 = screen.getByRole('textbox', { name: 'Field 3' }),
            field4 = screen.getByRole('textbox', { name: 'Field 4' }),
            field6 = screen.getByRole('textbox', { name: 'Field 6' }),
            field7 = screen.getByRole('textbox', { name: 'Field 7' }),
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
      render();

      const form = await selectors.form('find'),
          uploadBtn = selectors.uploadBtn(),
          fileUpload = form.querySelector('input[type=file]'),
          field1 = screen.getByRole('textbox', { name: 'Field 1' }),
          field2 = screen.getByRole('textbox', { name: 'Field 2' }),
          field3 = screen.getByRole('textbox', { name: 'Field 3' }),
          field4 = screen.getByRole('textbox', { name: 'Field 4' }),
          field6 = screen.getByRole('textbox', { name: 'Field 6' }),
          field7 = screen.getByRole('textbox', { name: 'Field 7' });

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
    });

    it('renders validation error messages when Upload is clicked with only the file missing', async function() {
      render();

      const form = await selectors.form('find'),
          uploadBtn = selectors.uploadBtn(),
          fileUpload = form.querySelector('input[type=file]'),
          field1 = screen.getByRole('textbox', { name: 'Field 1' }),
          field2 = screen.getByRole('textbox', { name: 'Field 2' }),
          field3 = screen.getByRole('textbox', { name: 'Field 3' }),
          field4 = screen.getByRole('textbox', { name: 'Field 4' }),
          field6 = screen.getByRole('textbox', { name: 'Field 6' }),
          field7 = screen.getByRole('textbox', { name: 'Field 7' });

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

    it('submits the upload successfully after validation errors are fixed', async function() {
      const hashSpy = jest.spyOn(window.location, 'hash', 'set');
      when(axios.post).calledWith('service/rest/internal/ui/upload/repo-id', expect.anything())
          .mockResolvedValue({ data: { success: true, data: 'foobar' } });

      render();

      const form = await selectors.form('find'),
          uploadBtn = selectors.uploadBtn(),
          fileUpload = form.querySelector('input[type=file]'),
          field1 = screen.getByRole('textbox', { name: 'Field 1' }),
          field2 = screen.getByRole('textbox', { name: 'Field 2' }),
          field3 = screen.getByRole('textbox', { name: 'Field 3' }),
          field4 = screen.getByRole('textbox', { name: 'Field 4' }),
          field6 = screen.getByRole('textbox', { name: 'Field 6' }),
          field7 = screen.getByRole('textbox', { name: 'Field 7' }),
          file = new File(['test'], 'test.txt', { type: 'text-plain' });

      await userEvent.click(uploadBtn);

      const formValidationAlert = selectors.errorAlert('getAll').find(el => el.textContent.includes('validation'));
      expect(formValidationAlert).toBeInTheDocument();
      expect(selectors.errorRetryBtn('query')).not.toBeInTheDocument();

      expect(fileUpload).toHaveErrorMessage('This field is required!');
      expect(fileUpload).toBeInvalid();
      expect(field2).toHaveErrorMessage('This field is required');
      expect(field2).toBeInvalid();
      expect(field4).toHaveErrorMessage('This field is required');
      expect(field4).toBeInvalid();
      expect(field6).toHaveErrorMessage('This field is required');
      expect(field6).toBeInvalid();

      setFileUploadValue(fileUpload, file);
      expect(fileUpload).not.toHaveErrorMessage();
      expect(fileUpload).toBeValid();

      await userEvent.type(field2, 'bar');
      expect(field2).not.toHaveErrorMessage();
      expect(field2).toBeValid();

      await userEvent.type(field4, 'qwerty');
      expect(field4).not.toHaveErrorMessage();
      expect(field4).toBeValid();

      await userEvent.type(field6, 'qwerty');
      expect(field6).not.toHaveErrorMessage();
      expect(field6).toBeValid();
      expect(formValidationAlert).not.toBeInTheDocument();

      await userEvent.click(selectors.uploadBtn());

      await waitFor(
          () => expect(hashSpy).toHaveBeenCalledWith('browse/search=keyword%3D%22foobar%22'),
          { timeout: 1500 }
      );
    });

    it('renders an error with a retry button if the form POST fails', async function() {
      const hashSpy = jest.spyOn(window.location, 'hash', 'set');
      when(axios.post).calledWith('service/rest/internal/ui/upload/repo-id', expect.anything())
          .mockRejectedValue({ message: 'foobar' });

      render();

      const form = await selectors.form('find'),
          uploadBtn = selectors.uploadBtn(),
          fileUpload = form.querySelector('input[type=file]'),
          field1 = screen.getByRole('textbox', { name: 'Field 1' }),
          field2 = screen.getByRole('textbox', { name: 'Field 2' }),
          field3 = screen.getByRole('textbox', { name: 'Field 3' }),
          field4 = screen.getByRole('textbox', { name: 'Field 4' }),
          field6 = screen.getByRole('textbox', { name: 'Field 6' }),
          field7 = screen.getByRole('textbox', { name: 'Field 7' }),
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
      when(axios.post).calledWith('service/rest/internal/ui/upload/repo-id', expect.anything())
          .mockRejectedValueOnce({ message: 'foobar' });
      when(axios.post).calledWith('service/rest/internal/ui/upload/repo-id', expect.anything())
          .mockResolvedValueOnce({ data: { success: true, data: 'asdf' } });

      render();

      const form = await selectors.form('find'),
          uploadBtn = selectors.uploadBtn(),
          fileUpload = form.querySelector('input[type=file]'),
          field2 = screen.getByRole('textbox', { name: 'Field 2' }),
          field4 = screen.getByRole('textbox', { name: 'Field 4' }),
          field6 = screen.getByRole('textbox', { name: 'Field 6' }),
          field7 = screen.getByRole('textbox', { name: 'Field 7' }),
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
});

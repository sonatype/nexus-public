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
import { render, screen, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Theme } from '@radix-ui/themes';
import '@testing-library/jest-dom';

import { FileDropzone, formatFileSize } from '../components/FileDropzone';

// Helper to render with Radix Theme
const renderWithTheme = (ui: React.ReactElement) => {
  return render(<Theme>{ui}</Theme>);
};

// Helper to create mock File
const createMockFile = (
  name: string,
  size: number,
  type: string
): File => {
  const file = new File([''], name, { type });
  Object.defineProperty(file, 'size', { value: size });
  return file;
};

// Helper to create mock FileList for JSDOM
const createMockFileList = (files: File[]): FileList => {
  const fileList = {
    length: files.length,
    item: (index: number) => files[index] || null,
    [Symbol.iterator]: function* () {
      for (const file of files) {
        yield file;
      }
    },
  };
  // Add indexed access
  files.forEach((file, index) => {
    Object.defineProperty(fileList, index, { value: file, enumerable: true });
  });
  return fileList as unknown as FileList;
};

// Helper to create mock DataTransfer for drag/drop events
const createMockDataTransfer = (files: File[]): Partial<DataTransfer> => {
  return {
    files: createMockFileList(files),
    items: {
      length: files.length,
      add: jest.fn(),
      remove: jest.fn(),
      clear: jest.fn(),
    } as unknown as DataTransferItemList,
    types: ['Files'],
    getData: jest.fn(),
    setData: jest.fn(),
    clearData: jest.fn(),
    setDragImage: jest.fn(),
    dropEffect: 'none',
    effectAllowed: 'all',
  };
};

describe('FileDropzone', () => {
  describe('formatFileSize', () => {
    it('formats 0 bytes', () => {
      expect(formatFileSize(0)).toBe('0 B');
    });

    it('formats bytes', () => {
      expect(formatFileSize(500)).toBe('500 B');
    });

    it('formats kilobytes', () => {
      expect(formatFileSize(1024)).toBe('1.0 KB');
      expect(formatFileSize(1536)).toBe('1.5 KB');
    });

    it('formats megabytes', () => {
      expect(formatFileSize(1024 * 1024)).toBe('1.0 MB');
      expect(formatFileSize(2.5 * 1024 * 1024)).toBe('2.5 MB');
    });

    it('formats gigabytes', () => {
      expect(formatFileSize(1024 * 1024 * 1024)).toBe('1.0 GB');
    });
  });

  describe('rendering', () => {
    it('renders empty dropzone with prompt', () => {
      renderWithTheme(
        <FileDropzone files={[]} onChange={jest.fn()} />
      );

      expect(screen.getByText('Drop files here')).toBeInTheDocument();
      expect(screen.getByText('or click to browse')).toBeInTheDocument();
    });

    it('renders with label', () => {
      renderWithTheme(
        <FileDropzone
          files={[]}
          onChange={jest.fn()}
          label="Upload artifact"
        />
      );

      expect(screen.getByText('Upload artifact')).toBeInTheDocument();
    });

    it('renders required indicator', () => {
      renderWithTheme(
        <FileDropzone
          files={[]}
          onChange={jest.fn()}
          label="Upload artifact"
          required
        />
      );

      expect(screen.getByText('*')).toBeInTheDocument();
    });

    it('renders helpText when provided (UX-FORM-STANDARD)', () => {
      const helpText =
        'The artifact or asset file to upload. Accepted types vary by repository format.';
      renderWithTheme(
        <FileDropzone
          files={[]}
          onChange={jest.fn()}
          label="File"
          helpText={helpText}
        />
      );

      expect(screen.getByText(helpText)).toBeInTheDocument();
    });

    it('has data-testid for testability (UX-FORM-STANDARD)', () => {
      renderWithTheme(
        <FileDropzone
          files={[]}
          onChange={jest.fn()}
          testId="input-asset-file-0"
        />
      );

      expect(screen.getByTestId('input-asset-file-0')).toBeInTheDocument();
    });

    it('renders accepted file types', () => {
      renderWithTheme(
        <FileDropzone
          files={[]}
          onChange={jest.fn()}
          accept=".jar,.pom"
        />
      );

      expect(screen.getByText('Supports: .jar, .pom')).toBeInTheDocument();
    });

    it('renders file list when files are selected', () => {
      const files = [
        createMockFile('artifact.jar', 1024 * 1024, 'application/java-archive'),
        createMockFile('artifact.pom', 1024, 'application/xml'),
      ];

      renderWithTheme(
        <FileDropzone files={files} onChange={jest.fn()} multiple />
      );

      expect(screen.getByText('artifact.jar')).toBeInTheDocument();
      expect(screen.getByText('(1.0 MB)')).toBeInTheDocument();
      expect(screen.getByText('artifact.pom')).toBeInTheDocument();
      expect(screen.getByText('(1.0 KB)')).toBeInTheDocument();
    });

    it('renders remove buttons for each file', () => {
      const files = [
        createMockFile('file1.jar', 1024, 'application/java-archive'),
        createMockFile('file2.jar', 1024, 'application/java-archive'),
      ];

      renderWithTheme(
        <FileDropzone files={files} onChange={jest.fn()} multiple />
      );

      expect(screen.getByRole('button', { name: 'Remove file1.jar' })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: 'Remove file2.jar' })).toBeInTheDocument();
    });

    it('renders "Add more files" when multiple and has files', () => {
      const files = [createMockFile('file.jar', 1024, 'application/java-archive')];

      renderWithTheme(
        <FileDropzone files={files} onChange={jest.fn()} multiple />
      );

      expect(screen.getByText('Add more files')).toBeInTheDocument();
    });

    it('does not render "Add more files" when not multiple', () => {
      const files = [createMockFile('file.jar', 1024, 'application/java-archive')];

      renderWithTheme(
        <FileDropzone files={files} onChange={jest.fn()} />
      );

      expect(screen.queryByText('Add more files')).not.toBeInTheDocument();
    });

    it('renders error message', () => {
      renderWithTheme(
        <FileDropzone
          files={[]}
          onChange={jest.fn()}
          error="File upload failed"
        />
      );

      expect(screen.getByText('File upload failed')).toBeInTheDocument();
      expect(screen.getByRole('alert')).toBeInTheDocument();
    });

    it('applies disabled state', () => {
      renderWithTheme(
        <FileDropzone files={[]} onChange={jest.fn()} disabled />
      );

      const dropzone = screen.getByRole('button', { name: 'File upload dropzone' });
      expect(dropzone).toHaveAttribute('aria-disabled', 'true');
    });
  });

  describe('file selection via click', () => {
    it('opens file picker when dropzone is clicked', async () => {
      const onChange = jest.fn();
      renderWithTheme(
        <FileDropzone files={[]} onChange={onChange} />
      );

      const input = document.querySelector('input[type="file"]') as HTMLInputElement;
      const clickSpy = jest.spyOn(input, 'click');

      const dropzone = screen.getByRole('button', { name: 'File upload dropzone' });
      await userEvent.click(dropzone);

      expect(clickSpy).toHaveBeenCalled();
    });

    it('does not open file picker when disabled', async () => {
      renderWithTheme(
        <FileDropzone files={[]} onChange={jest.fn()} disabled />
      );

      const input = document.querySelector('input[type="file"]') as HTMLInputElement;
      const clickSpy = jest.spyOn(input, 'click');

      const dropzone = screen.getByRole('button', { name: 'File upload dropzone' });
      await userEvent.click(dropzone);

      expect(clickSpy).not.toHaveBeenCalled();
    });

    it('handles file input change', () => {
      const onChange = jest.fn();
      renderWithTheme(
        <FileDropzone files={[]} onChange={onChange} />
      );

      const input = document.querySelector('input[type="file"]') as HTMLInputElement;
      const file = createMockFile('test.jar', 1024, 'application/java-archive');
      
      // Create a FileList-like object
      const fileList = createMockFileList([file]);
      
      fireEvent.change(input, { target: { files: fileList } });

      expect(onChange).toHaveBeenCalledWith([file]);
    });
  });

  describe('keyboard navigation', () => {
    it('opens file picker on Enter key', async () => {
      renderWithTheme(
        <FileDropzone files={[]} onChange={jest.fn()} />
      );

      const input = document.querySelector('input[type="file"]') as HTMLInputElement;
      const clickSpy = jest.spyOn(input, 'click');

      const dropzone = screen.getByRole('button', { name: 'File upload dropzone' });
      dropzone.focus();
      fireEvent.keyDown(dropzone, { key: 'Enter' });

      expect(clickSpy).toHaveBeenCalled();
    });

    it('opens file picker on Space key', async () => {
      renderWithTheme(
        <FileDropzone files={[]} onChange={jest.fn()} />
      );

      const input = document.querySelector('input[type="file"]') as HTMLInputElement;
      const clickSpy = jest.spyOn(input, 'click');

      const dropzone = screen.getByRole('button', { name: 'File upload dropzone' });
      dropzone.focus();
      fireEvent.keyDown(dropzone, { key: ' ' });

      expect(clickSpy).toHaveBeenCalled();
    });

    it('does not open file picker on Enter when disabled', () => {
      renderWithTheme(
        <FileDropzone files={[]} onChange={jest.fn()} disabled />
      );

      const input = document.querySelector('input[type="file"]') as HTMLInputElement;
      const clickSpy = jest.spyOn(input, 'click');

      const dropzone = screen.getByRole('button', { name: 'File upload dropzone' });
      fireEvent.keyDown(dropzone, { key: 'Enter' });

      expect(clickSpy).not.toHaveBeenCalled();
    });
  });

  describe('drag and drop', () => {
    it('handles drag enter', () => {
      renderWithTheme(
        <FileDropzone files={[]} onChange={jest.fn()} />
      );

      const dropzone = screen.getByRole('button', { name: 'File upload dropzone' });
      
      fireEvent.dragEnter(dropzone);
      
      // Check that dragging class is applied (via parent container)
      expect(dropzone.closest('.nxrm-file-dropzone')).toHaveClass('nxrm-file-dropzone--dragging');
    });

    it('handles drag leave', () => {
      renderWithTheme(
        <FileDropzone files={[]} onChange={jest.fn()} />
      );

      const dropzone = screen.getByRole('button', { name: 'File upload dropzone' });
      
      fireEvent.dragEnter(dropzone);
      fireEvent.dragLeave(dropzone, { relatedTarget: document.body });
      
      expect(dropzone.closest('.nxrm-file-dropzone')).not.toHaveClass('nxrm-file-dropzone--dragging');
    });

    it('handles file drop', () => {
      const onChange = jest.fn();
      renderWithTheme(
        <FileDropzone files={[]} onChange={onChange} />
      );

      const dropzone = screen.getByRole('button', { name: 'File upload dropzone' });
      const file = createMockFile('dropped.jar', 1024, 'application/java-archive');
      const dataTransfer = createMockDataTransfer([file]);

      fireEvent.drop(dropzone, { dataTransfer });

      expect(onChange).toHaveBeenCalledWith([file]);
    });

    it('does not handle drop when disabled', () => {
      const onChange = jest.fn();
      renderWithTheme(
        <FileDropzone files={[]} onChange={onChange} disabled />
      );

      const dropzone = screen.getByRole('button', { name: 'File upload dropzone' });
      const file = createMockFile('dropped.jar', 1024, 'application/java-archive');
      const dataTransfer = createMockDataTransfer([file]);

      fireEvent.drop(dropzone, { dataTransfer });

      expect(onChange).not.toHaveBeenCalled();
    });
  });

  describe('file validation', () => {
    it('validates file size', () => {
      const onChange = jest.fn();
      renderWithTheme(
        <FileDropzone
          files={[]}
          onChange={onChange}
          maxSize={1024} // 1 KB max
        />
      );

      const input = document.querySelector('input[type="file"]') as HTMLInputElement;
      const largefile = createMockFile('large.jar', 2048, 'application/java-archive');
      const fileList = createMockFileList([largefile]);

      fireEvent.change(input, { target: { files: fileList } });

      // File should be rejected, onChange not called with the file
      expect(onChange).not.toHaveBeenCalled();
      // Error should be displayed
      expect(screen.getByText(/exceeds maximum size/i)).toBeInTheDocument();
    });

    it('validates file extension', () => {
      const onChange = jest.fn();
      renderWithTheme(
        <FileDropzone
          files={[]}
          onChange={onChange}
          accept=".jar,.pom"
        />
      );

      const input = document.querySelector('input[type="file"]') as HTMLInputElement;
      const wrongFile = createMockFile('file.txt', 1024, 'text/plain');
      const fileList = createMockFileList([wrongFile]);

      fireEvent.change(input, { target: { files: fileList } });

      expect(onChange).not.toHaveBeenCalled();
      expect(screen.getByText(/not an accepted file type/i)).toBeInTheDocument();
    });

    it('accepts valid file extension', () => {
      const onChange = jest.fn();
      renderWithTheme(
        <FileDropzone
          files={[]}
          onChange={onChange}
          accept=".jar,.pom"
        />
      );

      const input = document.querySelector('input[type="file"]') as HTMLInputElement;
      const validFile = createMockFile('artifact.jar', 1024, 'application/java-archive');
      const fileList = createMockFileList([validFile]);

      fireEvent.change(input, { target: { files: fileList } });

      expect(onChange).toHaveBeenCalledWith([validFile]);
    });

    it('validates MIME type with wildcard', () => {
      const onChange = jest.fn();
      renderWithTheme(
        <FileDropzone
          files={[]}
          onChange={onChange}
          accept="image/*"
        />
      );

      const input = document.querySelector('input[type="file"]') as HTMLInputElement;
      const imageFile = createMockFile('photo.png', 1024, 'image/png');
      const fileList = createMockFileList([imageFile]);

      fireEvent.change(input, { target: { files: fileList } });

      expect(onChange).toHaveBeenCalledWith([imageFile]);
    });
  });

  describe('multiple files', () => {
    it('adds files to existing list when multiple', () => {
      const existingFiles = [createMockFile('existing.jar', 1024, 'application/java-archive')];
      const onChange = jest.fn();

      renderWithTheme(
        <FileDropzone
          files={existingFiles}
          onChange={onChange}
          multiple
        />
      );

      const input = document.querySelector('input[type="file"]') as HTMLInputElement;
      const newFile = createMockFile('new.jar', 1024, 'application/java-archive');
      const fileList = createMockFileList([newFile]);

      fireEvent.change(input, { target: { files: fileList } });

      expect(onChange).toHaveBeenCalledWith([...existingFiles, newFile]);
    });

    it('replaces file when not multiple', () => {
      const existingFiles = [createMockFile('existing.jar', 1024, 'application/java-archive')];
      const onChange = jest.fn();

      renderWithTheme(
        <FileDropzone
          files={existingFiles}
          onChange={onChange}
          multiple={false}
        />
      );

      const input = document.querySelector('input[type="file"]') as HTMLInputElement;
      const newFile = createMockFile('new.jar', 1024, 'application/java-archive');
      const fileList = createMockFileList([newFile]);

      fireEvent.change(input, { target: { files: fileList } });

      expect(onChange).toHaveBeenCalledWith([newFile]);
    });
  });

  describe('removing files', () => {
    it('removes file when remove button is clicked', async () => {
      const files = [
        createMockFile('file1.jar', 1024, 'application/java-archive'),
        createMockFile('file2.jar', 1024, 'application/java-archive'),
      ];
      const onChange = jest.fn();

      renderWithTheme(
        <FileDropzone files={files} onChange={onChange} multiple />
      );

      const removeButton = screen.getByRole('button', { name: 'Remove file1.jar' });
      await userEvent.click(removeButton);

      expect(onChange).toHaveBeenCalledWith([files[1]]);
    });

    it('does not remove file when disabled', async () => {
      const files = [createMockFile('file.jar', 1024, 'application/java-archive')];
      const onChange = jest.fn();

      renderWithTheme(
        <FileDropzone files={files} onChange={onChange} disabled />
      );

      const removeButton = screen.getByRole('button', { name: 'Remove file.jar' });
      await userEvent.click(removeButton);

      expect(onChange).not.toHaveBeenCalled();
    });
  });

  describe('accessibility', () => {
    it('has proper ARIA attributes', () => {
      renderWithTheme(
        <FileDropzone
          files={[]}
          onChange={jest.fn()}
          label="Upload artifact"
          required
          id="upload-input"
        />
      );

      const input = document.querySelector('input[type="file"]') as HTMLInputElement;
      expect(input).toHaveAttribute('aria-required', 'true');

      const dropzone = screen.getByRole('button', { name: 'Upload artifact' });
      expect(dropzone).toHaveAttribute('tabindex', '0');
    });

    it('links error message to input', () => {
      renderWithTheme(
        <FileDropzone
          files={[]}
          onChange={jest.fn()}
          error="Upload failed"
          id="test-input"
        />
      );

      const input = document.querySelector('input[type="file"]') as HTMLInputElement;
      expect(input).toHaveAttribute('aria-describedby', 'test-input-error');

      const errorElement = document.getElementById('test-input-error');
      expect(errorElement).toHaveTextContent('Upload failed');
    });

    it('has tabIndex -1 when disabled', () => {
      renderWithTheme(
        <FileDropzone files={[]} onChange={jest.fn()} disabled />
      );

      const dropzone = screen.getByRole('button', { name: 'File upload dropzone' });
      expect(dropzone).toHaveAttribute('tabindex', '-1');
    });
  });
});


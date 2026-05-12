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

import React, { useCallback, useRef, useState } from 'react';
import { Flex, Text } from '@radix-ui/themes';
import { Upload, File, X, Plus, AlertCircle } from 'lucide-react';

import './FileDropzone.scss';

/**
 * Props for the FileDropzone component.
 */
export interface FileDropzoneProps {
  /** Accepted file types (e.g., '.jar,.pom', 'image/*') */
  accept?: string;
  /** Allow multiple files */
  multiple?: boolean;
  /** Max file size in bytes */
  maxSize?: number;
  /** Currently selected files */
  files: File[];
  /** Callback when files change */
  onChange: (files: File[]) => void;
  /** Error message */
  error?: string;
  /** Disabled state */
  disabled?: boolean;
  /** Label text */
  label?: string;
  /** Help text explaining the field per UX-FORM-STANDARD */
  helpText?: string;
  /** Required field */
  required?: boolean;
  /** ID for the input element */
  id?: string;
  /** Test ID for the input (pattern: input-{name}) */
  testId?: string;
  /** Custom class name */
  className?: string;
}

/**
 * Format file size for display (e.g., "2.3 MB", "150 KB").
 */
export function formatFileSize(bytes: number): string {
  if (bytes === 0) return '0 B';
  const units = ['B', 'KB', 'MB', 'GB', 'TB'];
  const i = Math.floor(Math.log(bytes) / Math.log(1024));
  const size = bytes / Math.pow(1024, i);
  return `${size.toFixed(i > 0 ? 1 : 0)} ${units[i]}`;
}

/**
 * Validate a file against size and type constraints.
 */
function validateFile(
  file: File,
  accept?: string,
  maxSize?: number
): { valid: boolean; error?: string } {
  // Check file size
  if (maxSize && file.size > maxSize) {
    return {
      valid: false,
      error: `File "${file.name}" exceeds maximum size of ${formatFileSize(maxSize)}`,
    };
  }

  // Check file type if accept is specified
  if (accept) {
    const acceptedTypes = accept.split(',').map((t) => t.trim().toLowerCase());
    const fileName = file.name.toLowerCase();
    const fileType = file.type.toLowerCase();

    const isAccepted = acceptedTypes.some((accepted) => {
      // Handle extension (e.g., ".jar")
      if (accepted.startsWith('.')) {
        return fileName.endsWith(accepted);
      }
      // Handle MIME type with wildcard (e.g., "image/*")
      if (accepted.endsWith('/*')) {
        const baseType = accepted.slice(0, -2);
        return fileType.startsWith(baseType);
      }
      // Handle exact MIME type
      return fileType === accepted;
    });

    if (!isAccepted) {
      return {
        valid: false,
        error: `File "${file.name}" is not an accepted file type`,
      };
    }
  }

  return { valid: true };
}

/**
 * FileDropzone provides a drag-and-drop file upload interface.
 *
 * Features:
 * - Drag and drop support
 * - Click to browse
 * - Multiple file support (configurable)
 * - File type validation
 * - File size display
 * - Remove file button
 * - Accessible (keyboard, screen reader)
 * - Dark mode support
 *
 * @example
 * ```tsx
 * <FileDropzone
 *   files={files}
 *   onChange={setFiles}
 *   accept=".jar,.pom"
 *   multiple
 *   maxSize={50 * 1024 * 1024} // 50MB
 *   label="Upload artifact"
 *   required
 * />
 * ```
 */
export function FileDropzone({
  accept,
  multiple = false,
  maxSize,
  files,
  onChange,
  error,
  disabled = false,
  label,
  helpText,
  required = false,
  id,
  testId = 'input-file',
  className,
}: FileDropzoneProps): JSX.Element {
  const inputRef = useRef<HTMLInputElement>(null);
  const [isDragging, setIsDragging] = useState(false);
  const [validationError, setValidationError] = useState<string | null>(null);

  const inputId = id || 'file-dropzone-input';

  /**
   * Handle file selection from input or drop.
   */
  const handleFiles = useCallback(
    (fileList: FileList | null) => {
      if (!fileList || disabled) return;

      setValidationError(null);
      const newFiles: File[] = [];
      const errors: string[] = [];

      Array.from(fileList).forEach((file) => {
        const validation = validateFile(file, accept, maxSize);
        if (validation.valid) {
          newFiles.push(file);
        } else if (validation.error) {
          errors.push(validation.error);
        }
      });

      if (errors.length > 0) {
        setValidationError(errors[0]); // Show first error
      }

      if (newFiles.length > 0) {
        if (multiple) {
          // Add to existing files
          onChange([...files, ...newFiles]);
        } else {
          // Replace with new file
          onChange(newFiles.slice(0, 1));
        }
      }
    },
    [accept, maxSize, multiple, files, onChange, disabled]
  );

  /**
   * Handle click on dropzone to open file picker.
   */
  const handleClick = useCallback(() => {
    if (!disabled && inputRef.current) {
      inputRef.current.click();
    }
  }, [disabled]);

  /**
   * Handle keyboard activation (Enter/Space).
   */
  const handleKeyDown = useCallback(
    (event: React.KeyboardEvent) => {
      if ((event.key === 'Enter' || event.key === ' ') && !disabled) {
        event.preventDefault();
        handleClick();
      }
    },
    [handleClick, disabled]
  );

  /**
   * Handle file input change.
   */
  const handleInputChange = useCallback(
    (event: React.ChangeEvent<HTMLInputElement>) => {
      handleFiles(event.target.files);
      // Reset input so the same file can be selected again
      if (inputRef.current) {
        inputRef.current.value = '';
      }
    },
    [handleFiles]
  );

  /**
   * Handle drag enter.
   */
  const handleDragEnter = useCallback(
    (event: React.DragEvent) => {
      event.preventDefault();
      event.stopPropagation();
      if (!disabled) {
        setIsDragging(true);
      }
    },
    [disabled]
  );

  /**
   * Handle drag leave.
   */
  const handleDragLeave = useCallback(
    (event: React.DragEvent) => {
      event.preventDefault();
      event.stopPropagation();
      // Only set dragging to false if we're leaving the dropzone entirely
      const relatedTarget = event.relatedTarget as Node | null;
      if (!event.currentTarget.contains(relatedTarget)) {
        setIsDragging(false);
      }
    },
    []
  );

  /**
   * Handle drag over.
   */
  const handleDragOver = useCallback(
    (event: React.DragEvent) => {
      event.preventDefault();
      event.stopPropagation();
    },
    []
  );

  /**
   * Handle drop.
   */
  const handleDrop = useCallback(
    (event: React.DragEvent) => {
      event.preventDefault();
      event.stopPropagation();
      setIsDragging(false);

      if (!disabled) {
        handleFiles(event.dataTransfer.files);
      }
    },
    [handleFiles, disabled]
  );

  /**
   * Remove a file from the list.
   */
  const handleRemoveFile = useCallback(
    (index: number, event: React.MouseEvent) => {
      event.stopPropagation();
      if (disabled) return;

      const newFiles = files.filter((_, i) => i !== index);
      onChange(newFiles);
      setValidationError(null);
    },
    [files, onChange, disabled]
  );

  /**
   * Get accepted types display string.
   */
  const getAcceptedTypesDisplay = (): string | null => {
    if (!accept) return null;
    const types = accept.split(',').map((t) => t.trim());
    return `Supports: ${types.join(', ')}`;
  };

  const hasFiles = files.length > 0;
  const displayError = error || validationError;
  const dropzoneClasses = [
    'nxrm-file-dropzone',
    isDragging && 'nxrm-file-dropzone--dragging',
    disabled && 'nxrm-file-dropzone--disabled',
    displayError && 'nxrm-file-dropzone--error',
    hasFiles && 'nxrm-file-dropzone--has-files',
    className,
  ]
    .filter(Boolean)
    .join(' ');

  const helpId = `${inputId}-help`;
  const errorId = displayError ? `${inputId}-error` : undefined;
  const ariaDescribedBy = [helpText && helpId, errorId].filter(Boolean).join(' ') || undefined;

  return (
    <div className={dropzoneClasses} data-testid={testId}>
      {label && (
        <label className="nxrm-file-dropzone__label" htmlFor={inputId}>
          {label}
          {required && <span className="nxrm-file-dropzone__required">*</span>}
        </label>
      )}
      {helpText && (
        <Text size="1" color="gray" id={helpId} className="nxrm-file-dropzone__help" as="p" mb="2">
          {helpText}
        </Text>
      )}

      {/* Hidden file input */}
      <input
        ref={inputRef}
        id={inputId}
        type="file"
        accept={accept}
        multiple={multiple}
        onChange={handleInputChange}
        disabled={disabled}
        className="nxrm-file-dropzone__input"
        aria-describedby={ariaDescribedBy}
        aria-required={required}
        aria-invalid={!!displayError}
        data-testid={`${testId}-input`}
      />

      {/* Dropzone area */}
      <div
        className="nxrm-file-dropzone__area"
        onClick={handleClick}
        onKeyDown={handleKeyDown}
        onDragEnter={handleDragEnter}
        onDragLeave={handleDragLeave}
        onDragOver={handleDragOver}
        onDrop={handleDrop}
        role="button"
        tabIndex={disabled ? -1 : 0}
        aria-label={label || 'File upload dropzone'}
        aria-disabled={disabled}
      >
        {!hasFiles ? (
          // Empty state - show drop prompt
          <Flex direction="column" align="center" gap="2" className="nxrm-file-dropzone__prompt">
            <Upload
              size={32}
              className="nxrm-file-dropzone__icon"
              aria-hidden="true"
            />
            <Text size="2" weight="medium">
              Drop files here
            </Text>
            <Text size="1" color="gray">
              or click to browse
            </Text>
            {accept && (
              <Text size="1" color="gray" className="nxrm-file-dropzone__types">
                {getAcceptedTypesDisplay()}
              </Text>
            )}
          </Flex>
        ) : (
          // Files selected - show file list
          <Flex direction="column" gap="2" className="nxrm-file-dropzone__files">
            {files.map((file, index) => (
              <Flex
                key={`${file.name}-${index}`}
                align="center"
                justify="between"
                gap="3"
                className="nxrm-file-dropzone__file"
              >
                <Flex align="center" gap="2" className="nxrm-file-dropzone__file-info">
                  <File size={16} aria-hidden="true" />
                  <Text size="2" className="nxrm-file-dropzone__file-name">
                    {file.name}
                  </Text>
                  <Text size="1" color="gray" className="nxrm-file-dropzone__file-size">
                    ({formatFileSize(file.size)})
                  </Text>
                </Flex>
                <button
                  type="button"
                  className="nxrm-file-dropzone__remove"
                  onClick={(e) => handleRemoveFile(index, e)}
                  disabled={disabled}
                  aria-label={`Remove ${file.name}`}
                >
                  <X size={16} />
                </button>
              </Flex>
            ))}

            {/* Add more files button (only for multiple) */}
            {multiple && (
              <Flex
                align="center"
                justify="center"
                gap="1"
                className="nxrm-file-dropzone__add-more"
              >
                <Plus size={14} aria-hidden="true" />
                <Text size="1">Add more files</Text>
              </Flex>
            )}
          </Flex>
        )}
      </div>

      {/* Error message */}
      {displayError && (
        <Flex
          id={`${inputId}-error`}
          align="center"
          gap="1"
          className="nxrm-file-dropzone__error"
          role="alert"
        >
          <AlertCircle size={14} aria-hidden="true" />
          <Text size="1">{displayError}</Text>
        </Flex>
      )}
    </div>
  );
}

export default FileDropzone;


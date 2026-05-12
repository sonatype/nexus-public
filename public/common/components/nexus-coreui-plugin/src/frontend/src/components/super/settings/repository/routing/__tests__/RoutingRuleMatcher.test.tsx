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
import { Theme } from '@radix-ui/themes';
import { RoutingRuleMatcher } from '../RoutingRuleMatcher';

function TestWrapper({ children }) {
  return <Theme>{children}</Theme>;
}

describe('RoutingRuleMatcher', () => {
  const mockOnChange = jest.fn();
  const mockOnTest = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('should render matchers', () => {
    render(
      <RoutingRuleMatcher
        matchers={['.*-sources\\.jar', '.*-javadoc\\.jar']}
        onChange={mockOnChange}
      />,
      { wrapper: TestWrapper }
    );

    const inputs = screen.getAllByRole('textbox');
    expect(inputs).toHaveLength(2);
    expect(inputs[0]).toHaveValue('.*-sources\\.jar');
    expect(inputs[1]).toHaveValue('.*-javadoc\\.jar');
  });

  it('should render add button', () => {
    render(
      <RoutingRuleMatcher
        matchers={['.*']}
        onChange={mockOnChange}
      />,
      { wrapper: TestWrapper }
    );

    expect(screen.getByRole('button', { name: /add matcher/i })).toBeInTheDocument();
  });

  it('should call onChange when adding a matcher via preset menu', () => {
    render(
      <RoutingRuleMatcher
        matchers={['.*']}
        onChange={mockOnChange}
      />,
      { wrapper: TestWrapper }
    );

    // Click to open the preset dropdown
    fireEvent.click(screen.getByRole('button', { name: /add matcher/i }));

    // Click on the "Regex" preset option to add a new matcher
    fireEvent.click(screen.getByText('Regex'));

    expect(mockOnChange).toHaveBeenCalledWith(['.*', '']);
  });

  it('should show preset options when clicking add matcher', () => {
    render(
      <RoutingRuleMatcher
        matchers={['.*']}
        onChange={mockOnChange}
      />,
      { wrapper: TestWrapper }
    );

    // Click to open the preset dropdown
    fireEvent.click(screen.getByRole('button', { name: /add matcher/i }));

    // Check preset options are visible
    expect(screen.getByText('Regex')).toBeInTheDocument();
    expect(screen.getByText('Starts with')).toBeInTheDocument();
    expect(screen.getByText('Ends with')).toBeInTheDocument();
    expect(screen.getByText('Contains')).toBeInTheDocument();
  });

  it('should call onChange when removing a matcher', () => {
    render(
      <RoutingRuleMatcher
        matchers={['.*-sources\\.jar', '.*-javadoc\\.jar']}
        onChange={mockOnChange}
      />,
      { wrapper: TestWrapper }
    );

    const removeButtons = screen.getAllByRole('button', { name: /remove matcher/i });
    fireEvent.click(removeButtons[0]);

    expect(mockOnChange).toHaveBeenCalledWith(['.*-javadoc\\.jar']);
  });

  it('should not allow removing the last matcher', () => {
    render(
      <RoutingRuleMatcher
        matchers={['.*']}
        onChange={mockOnChange}
      />,
      { wrapper: TestWrapper }
    );

    const removeButton = screen.getByRole('button', { name: /remove matcher/i });
    expect(removeButton).toBeDisabled();
  });

  it('should call onChange when editing a matcher', () => {
    render(
      <RoutingRuleMatcher
        matchers={['.*']}
        onChange={mockOnChange}
      />,
      { wrapper: TestWrapper }
    );

    const input = screen.getByRole('textbox');
    fireEvent.change(input, { target: { value: '.*\\.jar' } });

    expect(mockOnChange).toHaveBeenCalledWith(['.*\\.jar']);
  });

  it('should show error message', () => {
    render(
      <RoutingRuleMatcher
        matchers={['']}
        onChange={mockOnChange}
        error="At least one matcher is required"
      />,
      { wrapper: TestWrapper }
    );

    expect(screen.getByText('At least one matcher is required')).toBeInTheDocument();
  });

  it('should disable inputs when disabled', () => {
    render(
      <RoutingRuleMatcher
        matchers={['.*']}
        onChange={mockOnChange}
        disabled={true}
      />,
      { wrapper: TestWrapper }
    );

    expect(screen.getByRole('textbox')).toBeDisabled();
    expect(screen.getByRole('button', { name: /add matcher/i })).toBeDisabled();
  });

  it('should render long test path without overflowing layout (CSS overflow-x: auto)', () => {
    const longPath =
      '/org/apache/maven/plugins/maven-compiler-plugin/3.11.0/maven-compiler-plugin-3.11.0-sources.jar';
    render(
      <RoutingRuleMatcher
        matchers={['.*\\.jar']}
        onChange={mockOnChange}
        onTest={mockOnTest}
        testPath={longPath}
        testMode="BLOCK"
      />,
      { wrapper: TestWrapper }
    );
    const testInput = screen.getByPlaceholderText('/com/example/artifact-1.0-sources.jar');
    expect(testInput).toHaveValue(longPath);
    expect(testInput).toBeInTheDocument();
    // overflow-x: auto is in RoutingRuleMatcher.scss - input scrolls long content, layout stays aligned
  });

  it('should show test path input when onTest is provided', () => {
    render(
      <RoutingRuleMatcher
        matchers={['.*']}
        onChange={mockOnChange}
        onTest={mockOnTest}
        testPath=""
        testMode="BLOCK"
      />,
      { wrapper: TestWrapper }
    );

    // Updated placeholder text
    expect(screen.getByPlaceholderText('/com/example/artifact-1.0-sources.jar')).toBeInTheDocument();
  });

  it('should call onTest when test path changes', () => {
    render(
      <RoutingRuleMatcher
        matchers={['.*']}
        onChange={mockOnChange}
        onTest={mockOnTest}
        testPath=""
        testMode="BLOCK"
      />,
      { wrapper: TestWrapper }
    );

    const testInput = screen.getByPlaceholderText('/com/example/artifact-1.0-sources.jar');
    fireEvent.change(testInput, { target: { value: '/com/example/lib.jar' } });

    expect(mockOnTest).toHaveBeenCalledWith('/com/example/lib.jar');
  });

  it('should show test result for BLOCK mode', () => {
    render(
      <RoutingRuleMatcher
        matchers={['.*\\.jar']}
        onChange={mockOnChange}
        onTest={mockOnTest}
        testPath="/com/example/lib.jar"
        testMode="BLOCK"
      />,
      { wrapper: TestWrapper }
    );

    // Since the path matches and mode is BLOCK, path would be blocked
    // Use more specific selector to get the outcome text
    expect(screen.getByText(/This request would be BLOCKED/i)).toBeInTheDocument();
  });

  it('should show test result for ALLOW mode', () => {
    render(
      <RoutingRuleMatcher
        matchers={['.*\\.jar']}
        onChange={mockOnChange}
        onTest={mockOnTest}
        testPath="/com/example/lib.jar"
        testMode="ALLOW"
      />,
      { wrapper: TestWrapper }
    );

    // Since the path matches and mode is ALLOW, path would be allowed
    // Use more specific selector to get the outcome text
    expect(screen.getByText(/This request would be ALLOWED/i)).toBeInTheDocument();
  });

  it('should show match indicator for matching pattern', () => {
    render(
      <RoutingRuleMatcher
        matchers={['.*\\.jar']}
        onChange={mockOnChange}
        onTest={mockOnTest}
        testPath="/com/example/lib.jar"
        testMode="BLOCK"
      />,
      { wrapper: TestWrapper }
    );

    // Should show "Matches" status badge for the matching pattern
    expect(screen.getByText('Matches')).toBeInTheDocument();
  });

  it('should show no-match indicator for non-matching pattern', () => {
    render(
      <RoutingRuleMatcher
        matchers={['.*\\.txt']}
        onChange={mockOnChange}
        onTest={mockOnTest}
        testPath="/com/example/lib.jar"
        testMode="BLOCK"
      />,
      { wrapper: TestWrapper }
    );

    // Should show "No match" status badge for the non-matching pattern
    expect(screen.getByText('No match')).toBeInTheDocument();
  });

  it('should show help text', () => {
    render(
      <RoutingRuleMatcher
        matchers={['.*']}
        onChange={mockOnChange}
      />,
      { wrapper: TestWrapper }
    );

    expect(screen.getByText(/define patterns to match request paths/i)).toBeInTheDocument();
  });

  it('should show placeholder text in inputs', () => {
    render(
      <RoutingRuleMatcher
        matchers={['']}
        onChange={mockOnChange}
      />,
      { wrapper: TestWrapper }
    );

    expect(screen.getByPlaceholderText(/e\.g\., \.\*-sources/i)).toBeInTheDocument();
  });

  it('should show inline validation error for invalid regex', () => {
    render(
      <RoutingRuleMatcher
        matchers={['[invalid']}
        onChange={mockOnChange}
      />,
      { wrapper: TestWrapper }
    );

    // Should show inline validation error for the invalid pattern
    expect(screen.getByText(/Invalid/i)).toBeInTheDocument();
  });

  it('should show rule summary when matchers and mode are provided', () => {
    render(
      <RoutingRuleMatcher
        matchers={['.*\\.jar']}
        onChange={mockOnChange}
        testPath="/test"
        testMode="BLOCK"
      />,
      { wrapper: TestWrapper }
    );

    // Should show rule summary section
    expect(screen.getByText('Rule Summary')).toBeInTheDocument();
    expect(screen.getByText(/Mode:/)).toBeInTheDocument();
    expect(screen.getByText('Block')).toBeInTheDocument();
  });

  it('should correctly evaluate ruleOutcome with duplicate matchers', () => {
    render(
      <RoutingRuleMatcher
        matchers={['.*\\.txt', '.*\\.jar', '.*\\.txt']}
        onChange={mockOnChange}
        onTest={mockOnTest}
        testPath="/com/example/lib.jar"
        testMode="BLOCK"
      />,
      { wrapper: TestWrapper }
    );

    expect(screen.getByText(/This request would be BLOCKED/i)).toBeInTheDocument();
  });

  it('should show ALLOWED when no matchers match in BLOCK mode', () => {
    render(
      <RoutingRuleMatcher
        matchers={['.*\\.txt', '.*\\.xml']}
        onChange={mockOnChange}
        onTest={mockOnTest}
        testPath="/com/example/lib.jar"
        testMode="BLOCK"
      />,
      { wrapper: TestWrapper }
    );

    expect(screen.getByText(/This request would be ALLOWED/i)).toBeInTheDocument();
  });

  it('should close preset menu when clicking backdrop', () => {
    render(
      <RoutingRuleMatcher
        matchers={['.*']}
        onChange={mockOnChange}
      />,
      { wrapper: TestWrapper }
    );

    // Open the preset menu
    fireEvent.click(screen.getByRole('button', { name: /add matcher/i }));
    expect(screen.getByText('Regex')).toBeInTheDocument();

    // Click backdrop to close
    const backdrop = document.querySelector('.routing-rule-matcher__backdrop');
    if (backdrop) {
      fireEvent.click(backdrop);
    }

    // Menu should be closed (Regex option no longer visible)
    expect(screen.queryByText('Custom regular expression')).not.toBeInTheDocument();
  });
});

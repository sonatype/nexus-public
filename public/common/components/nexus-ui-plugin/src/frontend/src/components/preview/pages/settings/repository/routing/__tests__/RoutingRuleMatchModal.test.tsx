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
import { RoutingRuleMatchModal } from '../RoutingRuleMatchModal';
import { RoutingRule } from '../types';

function TestWrapper({ children }) {
  return <Theme>{children}</Theme>;
}

describe('RoutingRuleMatchModal', () => {
  const mockOnClose = jest.fn();

  const mockRule: RoutingRule = {
    id: 'block-sources-123',
    name: 'block-sources',
    description: 'Blocks source JAR files from being downloaded',
    mode: 'BLOCK',
    matchers: ['.*-sources\\.jar$', '.*-javadoc\\.jar$'],
    assignedRepositoryCount: 2,
    assignedRepositoryNames: ['maven-central', 'maven-public'],
  };

  const testPath = '/com/example/artifact-1.0-sources.jar';

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('should not render content when isOpen is false', () => {
    render(
      <RoutingRuleMatchModal
        isOpen={false}
        onClose={mockOnClose}
        rule={mockRule}
        path={testPath}
      />,
      { wrapper: TestWrapper }
    );
    expect(screen.queryByRole('heading', { name: mockRule.name })).not.toBeInTheDocument();
  });

  it('should not render when rule is null', () => {
    render(
      <RoutingRuleMatchModal
        isOpen={true}
        onClose={mockOnClose}
        rule={null}
        path={testPath}
      />,
      { wrapper: TestWrapper }
    );

    // When rule is null, modal should not show rule name (returns empty fragment)
    expect(screen.queryByRole('heading')).not.toBeInTheDocument();
    // No description, mode, path, or matchers sections should be rendered
    expect(screen.queryByText(/description/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/mode/i)).not.toBeInTheDocument();
  });

  it('should render modal with rule details when open', () => {
    render(
      <RoutingRuleMatchModal
        isOpen={true}
        onClose={mockOnClose}
        rule={mockRule}
        path={testPath}
      />,
      { wrapper: TestWrapper }
    );

    expect(screen.getByRole('heading', { name: mockRule.name })).toBeInTheDocument();
  });

  it('should display rule description', () => {
    render(
      <RoutingRuleMatchModal
        isOpen={true}
        onClose={mockOnClose}
        rule={mockRule}
        path={testPath}
      />,
      { wrapper: TestWrapper }
    );

    expect(screen.getByText(mockRule.description)).toBeInTheDocument();
  });

  it('should not show description section when description is empty', () => {
    const ruleWithoutDescription = { ...mockRule, description: '' };

    render(
      <RoutingRuleMatchModal
        isOpen={true}
        onClose={mockOnClose}
        rule={ruleWithoutDescription}
        path={testPath}
      />,
      { wrapper: TestWrapper }
    );

    expect(screen.queryByText(/description/i)).not.toBeInTheDocument();
  });

  it('should display mode with BLOCK icon and label', () => {
    render(
      <RoutingRuleMatchModal
        isOpen={true}
        onClose={mockOnClose}
        rule={mockRule}
        path={testPath}
      />,
      { wrapper: TestWrapper }
    );

    expect(screen.getByText('Block')).toBeInTheDocument();
  });

  it('should display mode with ALLOW icon and label', () => {
    const allowRule = { ...mockRule, mode: 'ALLOW' as const };

    render(
      <RoutingRuleMatchModal
        isOpen={true}
        onClose={mockOnClose}
        rule={allowRule}
        path={testPath}
      />,
      { wrapper: TestWrapper }
    );

    expect(screen.getByText('Allow')).toBeInTheDocument();
  });

  it('should display the matched path', () => {
    render(
      <RoutingRuleMatchModal
        isOpen={true}
        onClose={mockOnClose}
        rule={mockRule}
        path={testPath}
      />,
      { wrapper: TestWrapper }
    );

    expect(screen.getByText(testPath)).toBeInTheDocument();
  });

  it('should display configured patterns in a list', () => {
    render(
      <RoutingRuleMatchModal
        isOpen={true}
        onClose={mockOnClose}
        rule={mockRule}
        path={testPath}
      />,
      { wrapper: TestWrapper }
    );

    // Should show the "Configured Patterns" label, not "All Patterns Matched"
    expect(screen.getByText(/configured patterns/i)).toBeInTheDocument();
    // Should show explanatory text
    expect(screen.getByText(/this rule will match if any of the following patterns/i)).toBeInTheDocument();
    // Should display all configured matchers
    mockRule.matchers.forEach(matcher => {
      expect(screen.getByText(matcher)).toBeInTheDocument();
    });
  });

  it('should display matchers section only when matchers exist', () => {
    const ruleWithoutMatchers = { ...mockRule, matchers: [] };

    render(
      <RoutingRuleMatchModal
        isOpen={true}
        onClose={mockOnClose}
        rule={ruleWithoutMatchers}
        path={testPath}
      />,
      { wrapper: TestWrapper }
    );

    expect(screen.queryByText(/configured patterns/i)).not.toBeInTheDocument();
  });

  it('should display rule name in the reference section', () => {
    render(
      <RoutingRuleMatchModal
        isOpen={true}
        onClose={mockOnClose}
        rule={mockRule}
        path={testPath}
      />,
      { wrapper: TestWrapper }
    );

    expect(screen.getByText('Rule Name')).toBeInTheDocument();
    expect(screen.getAllByText(mockRule.name)).toHaveLength(2);
    expect(screen.getAllByText(mockRule.name).some(element =>
      element.classList.contains('routing-rule-match-modal__id')
    )).toBe(true);
  });

  it('should call onClose when footer close button is clicked', () => {
    render(
      <RoutingRuleMatchModal
        isOpen={true}
        onClose={mockOnClose}
        rule={mockRule}
        path={testPath}
      />,
      { wrapper: TestWrapper }
    );

    // Get all close buttons and find the one in the footer
    const closeButtons = screen.getAllByRole('button', { name: /close/i });
    // The footer close button is in the modal footer
    const footerCloseButton = closeButtons.find(button =>
      button.classList.contains('routing-rule-match-modal__close-button')
    );
    expect(footerCloseButton).toBeDefined();
    fireEvent.click(footerCloseButton!);

    expect(mockOnClose).toHaveBeenCalled();
  });

  it('should call onClose exactly once when X button is clicked', () => {
    render(
      <RoutingRuleMatchModal
        isOpen={true}
        onClose={mockOnClose}
        rule={mockRule}
        path={testPath}
      />,
      { wrapper: TestWrapper }
    );

    const xButton = screen.getByLabelText('Close');
    fireEvent.click(xButton);

    // Verify onClose is called exactly once (not twice due to Dialog.Close + onClick)
    expect(mockOnClose).toHaveBeenCalledTimes(1);
  });

  it('should render with single matcher', () => {
    const ruleWithSingleMatcher = {
      ...mockRule,
      matchers: ['.*\\.jar$'],
    };

    render(
      <RoutingRuleMatchModal
        isOpen={true}
        onClose={mockOnClose}
        rule={ruleWithSingleMatcher}
        path={testPath}
      />,
      { wrapper: TestWrapper }
    );

    expect(screen.getByText('.*\\.jar$')).toBeInTheDocument();
    expect(screen.getByText(/configured patterns/i)).toBeInTheDocument();
  });

  it('should handle rule with special characters in name', () => {
    const ruleWithSpecialName = {
      ...mockRule,
      name: 'test-rule_with.special:chars',
    };

    render(
      <RoutingRuleMatchModal
        isOpen={true}
        onClose={mockOnClose}
        rule={ruleWithSpecialName}
        path={testPath}
      />,
      { wrapper: TestWrapper }
    );

    expect(screen.getByRole('heading', { name: ruleWithSpecialName.name })).toBeInTheDocument();
  });

  it('should handle long path', () => {
    const longPath = '/very/long/path/to/the/artifact/that/matches/the/pattern/artifact-1.0-sources.jar';

    render(
      <RoutingRuleMatchModal
        isOpen={true}
        onClose={mockOnClose}
        rule={mockRule}
        path={longPath}
      />,
      { wrapper: TestWrapper }
    );

    expect(screen.getByText(longPath)).toBeInTheDocument();
  });
});

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
import { render, screen } from '@testing-library/react';
import { Theme } from '@radix-ui/themes';
import { TaskScheduler, describeCron } from '../TaskScheduler';
import { ScheduleData } from '../types';

const renderWithTheme = (component: React.ReactElement) => {
  return render(<Theme>{component}</Theme>);
};

describe('TaskScheduler', () => {
  const mockOnChange = jest.fn();

  const manualSchedule: ScheduleData = {
    schedule: 'manual',
    timeZoneOffset: '+00:00',
    startDate: null,
    startTime: undefined,
    recurringDays: [],
    cronExpression: undefined,
  };

  const onceSchedule: ScheduleData = {
    schedule: 'once',
    timeZoneOffset: '+00:00',
    startDate: new Date('2026-01-22'),
    startTime: '10:00',
    recurringDays: [],
    cronExpression: undefined,
  };

  const dailySchedule: ScheduleData = {
    schedule: 'daily',
    timeZoneOffset: '+00:00',
    startDate: new Date('2026-01-22'),
    startTime: '10:00',
    recurringDays: [],
    cronExpression: undefined,
  };

  const weeklySchedule: ScheduleData = {
    schedule: 'weekly',
    timeZoneOffset: '+00:00',
    startDate: new Date('2026-01-22'),
    startTime: '10:00',
    recurringDays: [1], // Monday
    cronExpression: undefined,
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('manual schedule', () => {
    it('renders schedule selector', () => {
      renderWithTheme(<TaskScheduler value={manualSchedule} onChange={mockOnChange} />);

      expect(screen.getByText('Schedule')).toBeInTheDocument();
    });

    it('shows manual trigger message', () => {
      renderWithTheme(<TaskScheduler value={manualSchedule} onChange={mockOnChange} />);

      expect(screen.getByText(/manually triggered/i)).toBeInTheDocument();
    });
  });

  describe('once schedule', () => {
    it('shows date and time inputs', () => {
      renderWithTheme(<TaskScheduler value={onceSchedule} onChange={mockOnChange} />);

      expect(screen.getByText('Date')).toBeInTheDocument();
      expect(screen.getByText('Time')).toBeInTheDocument();
    });
  });

  describe('daily schedule', () => {
    it('shows start date and time inputs', () => {
      renderWithTheme(<TaskScheduler value={dailySchedule} onChange={mockOnChange} />);

      expect(screen.getByText(/start date/i)).toBeInTheDocument();
      expect(screen.getByText('Time')).toBeInTheDocument();
    });
  });

  describe('weekly schedule', () => {
    it('shows days to run section', () => {
      renderWithTheme(<TaskScheduler value={weeklySchedule} onChange={mockOnChange} />);

      expect(screen.getByText(/days to run/i)).toBeInTheDocument();
    });

    it('shows weekday checkboxes', () => {
      renderWithTheme(<TaskScheduler value={weeklySchedule} onChange={mockOnChange} />);

      // Weekdays are displayed with full names
      expect(screen.getByText('Monday')).toBeInTheDocument();
      expect(screen.getByText('Tuesday')).toBeInTheDocument();
      expect(screen.getByText('Wednesday')).toBeInTheDocument();
    });
  });

  describe('disabled state', () => {
    it('disables inputs when disabled prop is true', () => {
      renderWithTheme(<TaskScheduler value={onceSchedule} onChange={mockOnChange} disabled={true} />);

      const dateInput = screen.getByLabelText(/date/i);
      expect(dateInput).toBeDisabled();
    });
  });

  describe('cron preview', () => {
    it('shows human-readable preview for valid cron expression', () => {
      expect(describeCron('0 0 3 * * ?')).toBe('Every day at 3:00 AM');
      expect(describeCron('0 0 */4 * * ?')).toBe('Every 4 hours');
      expect(describeCron('0 30 1 ? * MON-FRI')).toBe('Weekdays at 1:30 AM');
      expect(describeCron('0 0 9 1 * ?')).toBe('Day 1 of each month at 9:00 AM');
    });

    it('returns empty string for invalid expression', () => {
      expect(describeCron('invalid')).toBe('');
      expect(describeCron('')).toBe('');
    });

    it('describes specific day-of-week schedules', () => {
      expect(describeCron('0 0 8 ? * SAT,SUN')).toBe('Weekends at 8:00 AM');
      expect(describeCron('0 0 12 ? * MON')).toBe('Monday at 12:00 PM');
    });

    it('describes minute-interval schedules', () => {
      expect(describeCron('0 */15 * * * ?')).toBe('Every 15 minutes');
    });

    it('describes last day of month', () => {
      expect(describeCron('0 0 6 L * ?')).toBe('Last day of month at 6:00 AM');
    });
  });
});










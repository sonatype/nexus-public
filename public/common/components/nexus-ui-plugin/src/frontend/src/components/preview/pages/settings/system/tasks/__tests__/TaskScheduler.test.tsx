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
import { TaskScheduler, describeCron } from '../TaskScheduler';
import { ScheduleData } from '../types';

// Mock Radix UI Select to render items inline (no portal) so option labels are queryable in jsdom
jest.mock('@radix-ui/themes', () => {
  const actual = jest.requireActual('@radix-ui/themes');
  return {
    ...actual,
    Select: {
      Root: ({ children, value, onValueChange, disabled }: any) => (
        <div data-testid="select-root" data-value={value} data-disabled={disabled}>
          {children}
        </div>
      ),
      Trigger: ({ children, id, placeholder, className, ...props }: any) => (
        <button id={id} role="combobox" className={className} disabled={props.disabled}
          aria-invalid={props['aria-invalid']} {...props}>
          {children || placeholder}
        </button>
      ),
      Content: ({ children }: any) => <div>{children}</div>,
      Item: ({ children, value }: any) => <div data-value={value}>{children}</div>,
    },
  };
});

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
    startDate: new Date(2026, 0, 22),
    startTime: '10:00',
    recurringDays: [],
    cronExpression: undefined,
  };

  const dailySchedule: ScheduleData = {
    schedule: 'daily',
    timeZoneOffset: '+00:00',
    startDate: new Date(2026, 0, 22),
    startTime: '10:00',
    recurringDays: [],
    cronExpression: undefined,
  };

  const weeklySchedule: ScheduleData = {
    schedule: 'weekly',
    timeZoneOffset: '+00:00',
    startDate: new Date(2026, 0, 22),
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

    it('renders date and time inputs as Radix TextField roots', () => {
      const { container } = renderWithTheme(
        <TaskScheduler value={onceSchedule} onChange={mockOnChange} />,
      );

      const dateInput = container.querySelector('input[type="date"]');
      const timeInput = container.querySelector('input[type="time"]');
      expect(dateInput).not.toBeNull();
      expect(timeInput).not.toBeNull();
      expect(dateInput!.closest('.rt-TextFieldRoot')).not.toBeNull();
      expect(timeInput!.closest('.rt-TextFieldRoot')).not.toBeNull();
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

  describe('advanced (cron) schedule', () => {
    const advancedSchedule: ScheduleData = {
      schedule: 'advanced',
      timeZoneOffset: '+00:00',
      startDate: null,
      startTime: undefined,
      recurringDays: [],
      cronExpression: '0 0 3 * * ?',
    };

    it('renders the cron expression input', () => {
      renderWithTheme(<TaskScheduler value={advancedSchedule} onChange={mockOnChange} />);

      expect(screen.getByText('Cron Expression')).toBeInTheDocument();
    });

    // NEXUS-52338: a cron expression is positional, so it must render in the monospace
    // stack rather than the Super UI default. The examples below the field already use it.
    it('renders the cron expression input with monospace styling', () => {
      const { container } = renderWithTheme(
        <TaskScheduler value={advancedSchedule} onChange={mockOnChange} />,
      );

      const cronInput = container.querySelector('input[name="cronExpression"]');
      expect(cronInput).not.toBeNull();
      expect(cronInput!.closest('.settings-text-input__input--mono')).not.toBeNull();
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

    it('describes bare * in hour field as "Every hour" without NaN (NEXUS-53358)', () => {
      // "0 0 * * * ?" means "every hour at :00" — bare * is not caught by the
      // startsWith('*/') guard, causing parseInt('*') = NaN in the preview.
      expect(describeCron('0 0 * * * ?')).toBe('Every hour');
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

  describe('a11y', () => {
    it('links schedule date error to the input via aria-describedby', () => {
      const onceWithError: ScheduleData = {
        ...onceSchedule,
        startDate: null,
      };
      const { container } = renderWithTheme(
        <TaskScheduler
          value={onceWithError}
          onChange={mockOnChange}
          errors={{ startDate: 'Date is required' }}
        />,
      );

      const dateInput = container.querySelector('input[type="date"]');
      const describedBy = dateInput!.getAttribute('aria-describedby');
      expect(describedBy).toBeTruthy();
      const helper = describedBy ? container.querySelector(`#${describedBy}`) : null;
      expect(helper).not.toBeNull();
      expect(helper!.textContent).toContain('Date is required');
    });
  });

  /**
   * NEXUS-53358 — start-date timezone bug.
   *
   * new Date("YYYY-MM-DD") parses as UTC midnight, which in negative-offset
   * timezones (e.g. CDT UTC-5) produces the previous calendar day at local time.
   * combineDateAndTime then sets local hours on that wrong local date, causing
   * the backend to receive the wrong day (confirmed via curl: user picks 06/30,
   * API stores 2026-06-29T05:00:00Z = midnight CDT June 29).
   *
   * Fix: parse date-only strings with the local-time constructor new Date(y, m-1, d)
   * and format back with getFullYear/getMonth/getDate (not toISOString which is UTC).
   *
   * Note: these tests run in a UTC Jest environment where new Date("2026-06-30")
   * and new Date(2026, 5, 30) both represent midnight June 30, so the original bug
   * does not manifest here. The tests assert the POST-FIX invariant and protect
   * against regression; the curl response above is the failing-test evidence.
   */
  describe('start-date round-trip does not shift the calendar date (NEXUS-53358)', () => {
    it('onChange receives a Date whose local calendar date matches the typed date string', () => {
      let capturedDate: Date | null = null;
      const onChange = jest.fn().mockImplementation((data: ScheduleData) => {
        capturedDate = data.startDate;
      });

      const { container } = renderWithTheme(
        <TaskScheduler value={onceSchedule} onChange={onChange} />,
      );

      const dateInput = container.querySelector('input[type="date"]')!;
      fireEvent.change(dateInput, { target: { value: '2026-06-30' } });

      expect(capturedDate).not.toBeNull();
      // Use local getters — if the Date were constructed as UTC midnight the local
      // getDate() would differ in negative-offset timezones (CDT: 29, not 30).
      expect(capturedDate!.getFullYear()).toBe(2026);
      expect(capturedDate!.getMonth()).toBe(5); // 0-indexed June
      expect(capturedDate!.getDate()).toBe(30);
    });

    it('date input displays the local calendar date stored in startDate', () => {
      // Construct startDate as local midnight so getDate/Month/FullYear are unambiguous.
      const localMidnightJune30 = new Date(2026, 5, 30);
      const schedule: ScheduleData = { ...onceSchedule, startDate: localMidnightJune30 };

      const { container } = renderWithTheme(
        <TaskScheduler value={schedule} onChange={jest.fn()} />,
      );

      const dateInput = container.querySelector('input[type="date"]') as HTMLInputElement;
      // toISOString().split('T')[0] reads back the UTC date, which differs from the
      // local date in UTC+ timezones (e.g. UTC+1: midnight local = 23:00 prev day UTC).
      // getFullYear/getMonth/getDate always return the local calendar date.
      expect(dateInput.value).toBe('2026-06-30');
    });
  });

  describe('allowedSchedules', () => {
    it('renders only the allowed options when allowedSchedules is provided', () => {
      renderWithTheme(
        <TaskScheduler
          value={manualSchedule}
          onChange={mockOnChange}
          allowedSchedules={['manual', 'once']}
        />,
      );

      expect(screen.getByText('Manual')).toBeInTheDocument();
      expect(screen.getByText('Once')).toBeInTheDocument();
      expect(screen.queryByText('Hourly')).not.toBeInTheDocument();
      expect(screen.queryByText('Daily')).not.toBeInTheDocument();
      expect(screen.queryByText('Weekly')).not.toBeInTheDocument();
      expect(screen.queryByText('Monthly')).not.toBeInTheDocument();
      expect(screen.queryByText('Advanced (Cron)')).not.toBeInTheDocument();
    });

    it('renders all seven schedule options when allowedSchedules is not provided', () => {
      renderWithTheme(
        <TaskScheduler value={manualSchedule} onChange={mockOnChange} />,
      );

      expect(screen.getByText('Manual')).toBeInTheDocument();
      expect(screen.getByText('Once')).toBeInTheDocument();
      expect(screen.getByText('Hourly')).toBeInTheDocument();
      expect(screen.getByText('Daily')).toBeInTheDocument();
      expect(screen.getByText('Weekly')).toBeInTheDocument();
      expect(screen.getByText('Monthly')).toBeInTheDocument();
      expect(screen.getByText('Advanced (Cron)')).toBeInTheDocument();
    });
  });
});

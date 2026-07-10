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

import React, { useCallback, useMemo } from 'react';
import { Box, Flex, Text, Grid, Checkbox, TextField } from '@radix-ui/themes';
import { Calendar, Clock, Info } from 'lucide-react';

import {
  SettingsSelect,
  SettingsTextInput,
} from '../../../../shared/form';

import {
  ScheduleType,
  ScheduleData,
  TaskSchedulerProps,
  SCHEDULE_OPTIONS,
  WEEKDAYS,
  MONTH_DAYS,
  DEFAULT_SCHEDULE_DATA,
  getTimezoneOffset,
  validateCronExpression,
} from './types';

const DAY_NAMES: Record<string, string> = {
  SUN: 'Sunday', MON: 'Monday', TUE: 'Tuesday', WED: 'Wednesday',
  THU: 'Thursday', FRI: 'Friday', SAT: 'Saturday',
  '1': 'Sunday', '2': 'Monday', '3': 'Tuesday', '4': 'Wednesday',
  '5': 'Thursday', '6': 'Friday', '7': 'Saturday',
};

export function describeCron(expr: string): string {
  const parts = expr.trim().split(/\s+/);
  if (parts.length < 6) return '';

  const [sec, min, hour, dom, , dow] = parts;

  const formatTime = (h: string, m: string) => {
    const hh = parseInt(h, 10);
    const mm = parseInt(m, 10);
    const ampm = hh >= 12 ? 'PM' : 'AM';
    const h12 = hh === 0 ? 12 : hh > 12 ? hh - 12 : hh;
    return `${h12}:${String(mm).padStart(2, '0')} ${ampm}`;
  };

  try {
    if (hour.startsWith('*/')) {
      return `Every ${hour.slice(2)} hours`;
    }
    if (min.startsWith('*/')) {
      return `Every ${min.slice(2)} minutes`;
    }

    const isEveryDom = dom === '*' || dom === '?';
    const isEveryDow = dow === '*' || dow === '?';

    if (dow === 'MON-FRI' && isEveryDom) {
      return `Weekdays at ${formatTime(hour, min)}`;
    }
    if (dow === 'SAT,SUN' && isEveryDom) {
      return `Weekends at ${formatTime(hour, min)}`;
    }

    if (!isEveryDow && isEveryDom) {
      const days = dow.split(',').map((d) => DAY_NAMES[d] || d).join(', ');
      return `${days} at ${formatTime(hour, min)}`;
    }

    if (!isEveryDom && isEveryDow) {
      if (dom === 'L') return `Last day of month at ${formatTime(hour, min)}`;
      return `Day ${dom} of each month at ${formatTime(hour, min)}`;
    }

    if (isEveryDom && isEveryDow) {
      return `Every day at ${formatTime(hour, min)}`;
    }

    return `Runs at ${formatTime(hour, min)}`;
  } catch {
    return '';
  }
}

import './TaskScheduler.scss';

/**
 * Get local timezone name for display
 */
const getTimezoneName = (): string => {
  try {
    return Intl.DateTimeFormat().resolvedOptions().timeZone;
  } catch {
    return 'local timezone';
  }
};

/**
 * TaskScheduler - Schedule picker component with support for all schedule types
 * 
 * Schedule types:
 * - Manual: No automatic scheduling
 * - Once: Run once at a specific date/time
 * - Hourly: Run every hour starting at a specific time
 * - Daily: Run daily at a specific time
 * - Weekly: Run on specific days of the week at a specific time
 * - Monthly: Run on specific days of the month at a specific time
 * - Advanced: Custom cron expression
 */
export function TaskScheduler({
  value = DEFAULT_SCHEDULE_DATA,
  onChange,
  errors = {},
  disabled = false,
  allowedSchedules,
}: TaskSchedulerProps) {
  const timezone = useMemo(() => getTimezoneName(), []);

  // Handle schedule type change.
  const handleScheduleChange = useCallback((newSchedule: string) => {
    const scheduleType = newSchedule as ScheduleType;
    const isTimeBased = scheduleType !== 'manual' && scheduleType !== 'advanced';
    const carriedStartDate = value.startDate ? new Date(value.startDate) : null;
    onChange({
      ...DEFAULT_SCHEDULE_DATA,
      schedule: scheduleType,
      timeZoneOffset: getTimezoneOffset(),
      startDate: isTimeBased ? (carriedStartDate || new Date()) : null,
      startTime: isTimeBased ? (value.startTime || '00:00') : undefined,
    });
  }, [onChange, value.startDate, value.startTime]);

  // Handle date change
  const handleDateChange = useCallback((dateStr: string) => {
    const date = dateStr ? new Date(dateStr) : null;
    onChange({
      ...value,
      startDate: date,
      timeZoneOffset: getTimezoneOffset(),
    });
  }, [onChange, value]);

  // Handle time change
  const handleTimeChange = useCallback((time: string) => {
    onChange({
      ...value,
      startTime: time,
      timeZoneOffset: getTimezoneOffset(),
    });
  }, [onChange, value]);

  // Handle recurring day toggle (for weekly/monthly)
  const handleDayToggle = useCallback((day: number) => {
    const currentDays = value.recurringDays || [];
    const newDays = currentDays.includes(day)
      ? currentDays.filter(d => d !== day)
      : [...currentDays, day].sort((a, b) => a - b);
    
    onChange({
      ...value,
      recurringDays: newDays,
      timeZoneOffset: getTimezoneOffset(),
    });
  }, [onChange, value]);

  // Handle cron expression change
  const handleCronChange = useCallback((cron: string) => {
    onChange({
      ...value,
      cronExpression: cron,
      timeZoneOffset: getTimezoneOffset(),
    });
  }, [onChange, value]);

  // Format date for input
  const dateValue = useMemo(() => {
    if (!value.startDate) return '';
    const d = new Date(value.startDate);
    return d.toISOString().split('T')[0];
  }, [value.startDate]);

  // Render schedule-specific fields based on type
  const renderScheduleFields = () => {
    switch (value.schedule) {
      case 'manual':
        return (
          <Box className="task-scheduler__info">
            <Info size={16} />
            <Text size="2">This task will only run when manually triggered.</Text>
          </Box>
        );

      case 'once':
        return (
          <Flex direction="column" gap="3">
            <Text size="2" weight="medium" className="task-scheduler__section-title">
              Run Once At
            </Text>
            <Flex gap="3" wrap="wrap">
              <Box className="task-scheduler__field task-scheduler__field--date">
                <label htmlFor="schedule-date" className="task-scheduler__label">
                  <Calendar size={14} />
                  Date
                </label>
                <TextField.Root
                  id="schedule-date"
                  type="date"
                  value={dateValue}
                  onChange={(e) => handleDateChange(e.target.value)}
                  disabled={disabled}
                  aria-describedby={errors.startDate ? 'schedule-date-error' : undefined}
                  aria-invalid={errors.startDate ? true : undefined}
                />
                {errors.startDate && (
                  <Text id="schedule-date-error" size="1" className="task-scheduler__error">{errors.startDate}</Text>
                )}
              </Box>
              <Box className="task-scheduler__field task-scheduler__field--time">
                <label htmlFor="schedule-time" className="task-scheduler__label">
                  <Clock size={14} />
                  Time
                </label>
                <TextField.Root
                  id="schedule-time"
                  type="time"
                  value={value.startTime || '00:00'}
                  onChange={(e) => handleTimeChange(e.target.value)}
                  disabled={disabled}
                  autoComplete="off"
                  aria-describedby={errors.startTime ? 'schedule-time-error' : undefined}
                  aria-invalid={errors.startTime ? true : undefined}
                />
                <Text size="1" color="gray" mt="1">Times are shown in your local timezone ({timezone})</Text>
                {errors.startTime && (
                  <Text id="schedule-time-error" size="1" className="task-scheduler__error">{errors.startTime}</Text>
                )}
              </Box>
            </Flex>
          </Flex>
        );

      case 'hourly':
        return (
          <Flex direction="column" gap="3">
            <Text size="2" weight="medium" className="task-scheduler__section-title">
              Run Every Hour Starting At
            </Text>
            <Flex gap="3" wrap="wrap">
              <Box className="task-scheduler__field task-scheduler__field--date">
                <label htmlFor="schedule-date" className="task-scheduler__label">
                  <Calendar size={14} />
                  Start Date
                </label>
                <TextField.Root
                  id="schedule-date"
                  type="date"
                  value={dateValue}
                  onChange={(e) => handleDateChange(e.target.value)}
                  disabled={disabled}
                  aria-describedby={errors.startDate ? 'schedule-date-error' : undefined}
                  aria-invalid={errors.startDate ? true : undefined}
                />
                {errors.startDate && (
                  <Text id="schedule-date-error" size="1" className="task-scheduler__error">{errors.startDate}</Text>
                )}
              </Box>
              <Box className="task-scheduler__field task-scheduler__field--time">
                <label htmlFor="schedule-time" className="task-scheduler__label">
                  <Clock size={14} />
                  Start Time ({timezone})
                </label>
                <TextField.Root
                  id="schedule-time"
                  type="time"
                  value={value.startTime || '00:00'}
                  onChange={(e) => handleTimeChange(e.target.value)}
                  disabled={disabled}
                  aria-describedby={errors.startTime ? 'schedule-time-error' : undefined}
                  aria-invalid={errors.startTime ? true : undefined}
                />
                {errors.startTime && (
                  <Text id="schedule-time-error" size="1" className="task-scheduler__error">{errors.startTime}</Text>
                )}
              </Box>
            </Flex>
          </Flex>
        );

      case 'daily':
        return (
          <Flex direction="column" gap="3">
            <Text size="2" weight="medium" className="task-scheduler__section-title">
              Run Daily At
            </Text>
            <Flex gap="3" wrap="wrap">
              <Box className="task-scheduler__field task-scheduler__field--date">
                <label htmlFor="schedule-date" className="task-scheduler__label">
                  <Calendar size={14} />
                  Start Date
                </label>
                <TextField.Root
                  id="schedule-date"
                  type="date"
                  value={dateValue}
                  onChange={(e) => handleDateChange(e.target.value)}
                  disabled={disabled}
                  aria-describedby={errors.startDate ? 'schedule-date-error' : undefined}
                  aria-invalid={errors.startDate ? true : undefined}
                />
                {errors.startDate && (
                  <Text id="schedule-date-error" size="1" className="task-scheduler__error">{errors.startDate}</Text>
                )}
              </Box>
              <Box className="task-scheduler__field task-scheduler__field--time">
                <label htmlFor="schedule-time" className="task-scheduler__label">
                  <Clock size={14} />
                  Time
                </label>
                <TextField.Root
                  id="schedule-time"
                  type="time"
                  value={value.startTime || '00:00'}
                  onChange={(e) => handleTimeChange(e.target.value)}
                  disabled={disabled}
                  autoComplete="off"
                  aria-describedby={errors.startTime ? 'schedule-time-error' : undefined}
                  aria-invalid={errors.startTime ? true : undefined}
                />
                <Text size="1" color="gray" mt="1">Times are shown in your local timezone ({timezone})</Text>
                {errors.startTime && (
                  <Text id="schedule-time-error" size="1" className="task-scheduler__error">{errors.startTime}</Text>
                )}
              </Box>
            </Flex>
          </Flex>
        );

      case 'weekly':
        return (
          <Flex direction="column" gap="4">
            <Flex gap="3" wrap="wrap">
              <Box className="task-scheduler__field task-scheduler__field--date">
                <label htmlFor="schedule-date" className="task-scheduler__label">
                  <Calendar size={14} />
                  Start Date
                </label>
                <TextField.Root
                  id="schedule-date"
                  type="date"
                  value={dateValue}
                  onChange={(e) => handleDateChange(e.target.value)}
                  disabled={disabled}
                  aria-describedby={errors.startDate ? 'schedule-date-error' : undefined}
                  aria-invalid={errors.startDate ? true : undefined}
                />
                {errors.startDate && (
                  <Text id="schedule-date-error" size="1" className="task-scheduler__error">{errors.startDate}</Text>
                )}
              </Box>
              <Box className="task-scheduler__field task-scheduler__field--time">
                <label htmlFor="schedule-time" className="task-scheduler__label">
                  <Clock size={14} />
                  Time
                </label>
                <TextField.Root
                  id="schedule-time"
                  type="time"
                  value={value.startTime || '00:00'}
                  onChange={(e) => handleTimeChange(e.target.value)}
                  disabled={disabled}
                  autoComplete="off"
                  aria-describedby={errors.startTime ? 'schedule-time-error' : undefined}
                  aria-invalid={errors.startTime ? true : undefined}
                />
                <Text size="1" color="gray" mt="1">Times are shown in your local timezone ({timezone})</Text>
                {errors.startTime && (
                  <Text id="schedule-time-error" size="1" className="task-scheduler__error">{errors.startTime}</Text>
                )}
              </Box>
            </Flex>
            <Box className="task-scheduler__days">
              <Text size="2" weight="medium" className="task-scheduler__section-title">
                Days to Run
              </Text>
              <Flex gap="3" wrap="wrap" className="task-scheduler__days-grid">
                {WEEKDAYS.map((day) => (
                  <label key={day.value} className="task-scheduler__day-checkbox">
                    <Checkbox
                      checked={(value.recurringDays || []).includes(day.value)}
                      onCheckedChange={() => handleDayToggle(day.value)}
                      disabled={disabled}
                    />
                    <Text size="2">{day.label}</Text>
                  </label>
                ))}
              </Flex>
              {errors.recurringDays && (
                <Text size="1" className="task-scheduler__error">{errors.recurringDays}</Text>
              )}
            </Box>
          </Flex>
        );

      case 'monthly':
        return (
          <Flex direction="column" gap="4">
            <Flex gap="3" wrap="wrap">
              <Box className="task-scheduler__field task-scheduler__field--date">
                <label htmlFor="schedule-date" className="task-scheduler__label">
                  <Calendar size={14} />
                  Start Date
                </label>
                <TextField.Root
                  id="schedule-date"
                  type="date"
                  value={dateValue}
                  onChange={(e) => handleDateChange(e.target.value)}
                  disabled={disabled}
                  aria-describedby={errors.startDate ? 'schedule-date-error' : undefined}
                  aria-invalid={errors.startDate ? true : undefined}
                />
                {errors.startDate && (
                  <Text id="schedule-date-error" size="1" className="task-scheduler__error">{errors.startDate}</Text>
                )}
              </Box>
              <Box className="task-scheduler__field task-scheduler__field--time">
                <label htmlFor="schedule-time" className="task-scheduler__label">
                  <Clock size={14} />
                  Time
                </label>
                <TextField.Root
                  id="schedule-time"
                  type="time"
                  value={value.startTime || '00:00'}
                  onChange={(e) => handleTimeChange(e.target.value)}
                  disabled={disabled}
                  autoComplete="off"
                  aria-describedby={errors.startTime ? 'schedule-time-error' : undefined}
                  aria-invalid={errors.startTime ? true : undefined}
                />
                <Text size="1" color="gray" mt="1">Times are shown in your local timezone ({timezone})</Text>
                {errors.startTime && (
                  <Text id="schedule-time-error" size="1" className="task-scheduler__error">{errors.startTime}</Text>
                )}
              </Box>
            </Flex>
            <Box className="task-scheduler__days">
              <Text size="2" weight="medium" className="task-scheduler__section-title">
                Days of Month to Run
              </Text>
              <Grid columns="7" gap="2" className="task-scheduler__month-grid">
                {MONTH_DAYS.map((day) => (
                  <label 
                    key={day.value} 
                    className={`task-scheduler__month-day ${
                      (value.recurringDays || []).includes(day.value) 
                        ? 'task-scheduler__month-day--selected' 
                        : ''
                    }`}
                  >
                    <Checkbox
                      checked={(value.recurringDays || []).includes(day.value)}
                      onCheckedChange={() => handleDayToggle(day.value)}
                      disabled={disabled}
                      className="task-scheduler__month-checkbox"
                    />
                    <Text size="1">{day.label}</Text>
                  </label>
                ))}
              </Grid>
              {errors.recurringDays && (
                <Text size="1" className="task-scheduler__error">{errors.recurringDays}</Text>
              )}
            </Box>
          </Flex>
        );

      case 'advanced': {
        const cronResult = value.cronExpression
          ? validateCronExpression(value.cronExpression)
          : { valid: false };

        return (
          <Flex direction="column" gap="3">
            <SettingsTextInput
              name="cronExpression"
              label="Cron Expression"
              value={value.cronExpression || ''}
              onChange={handleCronChange}
              placeholder="0 0 * * * ?"
              helpText="Enter a Quartz cron expression. Format: [seconds] [minutes] [hours] [day-of-month] [month] [day-of-week] [year(optional)]"
              error={errors.cronExpression || (
                value.cronExpression && !cronResult.valid ? cronResult.reason || 'Invalid cron expression' : ''
              )}
              required
              disabled={disabled}
            />
            {value.cronExpression && cronResult.valid && (
              <Text size="1" color="green" data-testid="cron-preview" className="task-scheduler__cron-preview">
                {describeCron(value.cronExpression) || 'Valid cron expression'}
              </Text>
            )}
            <Box className="task-scheduler__cron-help">
              <Text size="1" className="task-scheduler__cron-examples">
                <strong>Examples:</strong><br />
                <code>0 0 3 * * ?</code> - Daily at 3:00 AM<br />
                <code>0 0 */4 * * ?</code> - Every 4 hours<br />
                <code>0 0 9 ? * MON-FRI</code> - Weekdays at 9:00 AM<br />
                <code>0 30 8 1 * ?</code> - 1st of each month at 8:30 AM
              </Text>
            </Box>
          </Flex>
        );
      }

      default:
        return null;
    }
  };

  return (
    <Box className="task-scheduler">
      <SettingsSelect
        name="schedule"
        label="Schedule"
        value={value.schedule}
        onChange={handleScheduleChange}
        options={allowedSchedules
          ? SCHEDULE_OPTIONS.filter((o) => allowedSchedules.includes(o.value))
          : SCHEDULE_OPTIONS}
        placeholder="Select a schedule"
        helpText="Choose when this task should run"
        error={errors.schedule}
        required
        disabled={disabled}
      />

      {value.schedule !== 'manual' && (
        <Box className="task-scheduler__fields">
          {renderScheduleFields()}
        </Box>
      )}

      {value.schedule === 'manual' && renderScheduleFields()}
    </Box>
  );
}

export default TaskScheduler;

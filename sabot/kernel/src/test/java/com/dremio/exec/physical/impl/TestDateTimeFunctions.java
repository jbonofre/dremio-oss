/*
 * Copyright (C) 2017-2019 Dremio Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dremio.exec.physical.impl;

import static com.dremio.common.util.JodaDateUtility.formatDate;
import static com.dremio.sabot.Fixtures.ts;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormatter;
import org.junit.Test;

import com.dremio.common.exceptions.UserException;
import com.dremio.exec.expr.fn.impl.DateFunctionsUtils;
import com.dremio.sabot.BaseTestFunction;

public class TestDateTimeFunctions extends BaseTestFunction {

  @Test
  public void timezone() {
    testFunctions(new Object[][]{
      {"convert_timezone('+02:00', c0)", ts("2020-06-03T21:33:20.007"), ts("2020-06-03T23:33:20.007")},
      {"convert_timezone('+04:00', c0)", ts("2020-06-03T23:33:20.007"), ts("2020-06-04T03:33:20.007")},
      {"convert_timezone('+01:00', '+02:00', c0)", ts("2020-06-03T21:33:20.007"), ts("2020-06-03T22:33:20.007")},
      {"convert_timezone('+02:00', '+01:00', c0)", ts("2020-06-03T21:33:20.007"), ts("2020-06-03T20:33:20.007")},
      {"convert_timezone('Asia/Kolkata', c0)", ts("2020-06-03T23:33:20.007"), ts("2020-06-04T05:03:20.007")},
      {"convert_timezone('America/New_York', c0)", ts("2020-06-03T23:33:20.007"), ts("2020-06-03T19:33:20.007")},
      {"convert_timezone('Asia/Kolkata', 'America/New_York', c0)", ts("2020-06-03T23:33:20.007"), ts("2020-06-03T14:03:20.007")},
      // America/New_York will be 1 more hour behind during winter
      {"convert_timezone('America/New_York', c0)", ts("2020-12-03T23:33:20.007"), ts("2020-12-03T18:33:20.007")},
      {"convert_timezone('Asia/Kolkata', 'America/New_York', c0)", ts("2020-12-03T23:33:20.007"), ts("2020-12-03T13:03:20.007")},
    });
  }

  @Test
  public void timezoneWithAbbr() {
    testFunctions(new Object[][]{
      {"convert_timezone('UTC', c0)", ts("2020-06-03T23:33:20.007"), ts("2020-06-03T23:33:20.007")},
      {"convert_timezone('IST', c0)", ts("2020-06-03T23:33:20.007"), ts("2020-06-04T01:33:20.007")},
      {"convert_timezone('EDT', c0)", ts("2020-06-03T23:33:20.007"), ts("2020-06-03T19:33:20.007")},
      {"convert_timezone('IST', 'EDT', c0)", ts("2020-06-03T23:33:20.007"), ts("2020-06-03T17:33:20.007")},
      {"convert_timezone('IST', 'UTC', c0)", ts("2020-06-03T23:33:20.007"), ts("2020-06-03T21:33:20.007")},
    });
  }

  @Test
  public void toTimstamp() {
    testFunctions(new Object[][]{
      {"to_timestamp(c0, 'YYYY-MM-DD\"T\"HH24:MI:SS.FFFTZD')", "2018-11-23T15:36:00.000PST", ts("2018-11-23T23:36:00.000")},
      {"to_timestamp(c0, 'YYYY-MM-DD\"T\"HH24:MI:SS.FFFTZD')", "2018-11-23T15:36:00.000PDT", ts("2018-11-23T22:36:00.000")},
      {"to_timestamp(c0, 'YYYY-MM-DD\"T\"HH24:MI:SS.FFFTZO')", "2018-11-23T15:36:00.000+01:00", ts("2018-11-23T14:36:00.000")},
      {"extractDay(to_date(c0, 'YYYY-MM-DD\"T\"HH24:MI:SS.FFFTZD'))", "2018-11-23T19:36:00.000PST", 24L},
    });
  }

  @Test
  public void testUnixTimestampWithTZOffset() {
    testFunctions(new Object[][]{
      {"unix_timestamp(c0, 'YYYY-MM-DD\"T\"HH24:MI:SS.FFFTZO')", "1970-01-01T01:00:00.000-01:00", 7200L /*1970-01-01T02:00:00*/},
      {"unix_timestamp(c0, 'YYYY-MM-DD\"T\"HH24:MI:SS.FFFTZO')", "1970-01-01T01:00:00.000+01:00", 0L /*1970-01-01T00:00:00*/},
      {"unix_timestamp(c0, 'YYYY-MM-DD\"T\"HH24:MI:SS.FFFTZO')", "1970-01-01T02:00:00.000+01:00", 3600L /*1970-01-01T01:00:00*/},
    });
  }

  @Test
  public void invalidTimezoneAbbrev() {
    try {
      testFunctions(new Object[][]{
        {"convert_timezone('abc', 'UTC', c0)", ts("2020-06-03T23:33:20.007"), ts("2020-06-03T21:33:20.007")},
      });
      fail("Expected to fail");
    } catch (Exception e) {
      UserException uex =  (UserException) e.getCause().getCause();
      assertTrue(uex.getMessage().contains("Unknown time-zone ID: abc"));
    }

    try {
      testFunctions(new Object[][]{
              {"convert_timezone('+24:00', 'UTC', c0)", ts("2020-06-03T23:33:20.007"), ts("2020-06-03T21:33:20.007")},
      });
      fail("Expected to fail");
    } catch (Exception e) {
      UserException uex =  (UserException) e.getCause().getCause();
      assertTrue(uex.getMessage().contains("Zone offset hours not in valid range: value 24 is not in the range -18 to 18"));
    }

    try {
      testFunctions(new Object[][]{
        {"to_timestamp(c0, 'YYYY-MM-DD\"T\"HH24:MI:SS.FFFTZD')", "2018-11-23T15:36:00.000ABC", ts("2018-11-23T23:36:00.000")},
      });
      fail("Expected to fail");
    } catch (Exception e) {
      UserException uex =  (UserException) e.getCause().getCause();
      assertTrue(uex.getContextStrings().stream().anyMatch(s -> s.contains("Invalid timezone abbreviation ABC")));
    }
  }

  @Test
  public void testAddTimesTypes() {
    LocalDateTime fixedDate = formatDate.parseLocalDateTime("2003-07-09");
    DateTimeFormatter dateTimeFormatter = DateFunctionsUtils.getISOFormatterForFormatString("YYYY-MM-DD HH24:MI:SS").withZone(DateTimeZone.UTC);
    testFunctions(new Object[][]{
      // Test add days
      {"add_Days(c0, c1)", fixedDate, 4, formatDate.parseLocalDateTime("2003-07-13")},
      {"add_Days(c0, c1)", fixedDate, 0, fixedDate},
      {"add_Days(c0, c1)", fixedDate, -2, formatDate.parseLocalDateTime("2003-07-07")},
      {"add_Days(c0, c1)", fixedDate, 12, formatDate.parseLocalDateTime("2003-07-21")},
      {"add_Days(c0, c1)", fixedDate, -7, formatDate.parseLocalDateTime("2003-07-02")},

      // Test add days inverted inputs
      {"add_Days(c0, c1)", 4, fixedDate, formatDate.parseLocalDateTime("2003-07-13")},
      {"add_Days(c0, c1)", 0, fixedDate, fixedDate},
      {"add_Days(c0, c1)", -2, fixedDate, formatDate.parseLocalDateTime("2003-07-07")},
      {"add_Days(c0, c1)", 12, fixedDate, formatDate.parseLocalDateTime("2003-07-21")},
      {"add_Days(c0, c1)", -7, fixedDate, formatDate.parseLocalDateTime("2003-07-02")},

      // Test add months
      {"add_Months(c0, c1)", fixedDate, 4, formatDate.parseLocalDateTime("2003-11-09")},
      {"add_Months(c0, c1)", fixedDate, 0, fixedDate},
      {"add_Months(c0, c1)", fixedDate, -2, formatDate.parseLocalDateTime("2003-05-09")},
      {"add_Months(c0, c1)", fixedDate, 12, formatDate.parseLocalDateTime("2004-07-09")},
      {"add_Months(c0, c1)", fixedDate, -7, formatDate.parseLocalDateTime("2002-12-09")},

      // Test add months inverted inputs
      {"add_Months(c0, c1)", 4, fixedDate, formatDate.parseLocalDateTime("2003-11-09")},
      {"add_Months(c0, c1)", 0, fixedDate, fixedDate},
      {"add_Months(c0, c1)", -2, fixedDate, formatDate.parseLocalDateTime("2003-05-09")},
      {"add_Months(c0, c1)", 12, fixedDate, formatDate.parseLocalDateTime("2004-07-09")},
      {"add_Months(c0, c1)", -7, fixedDate, formatDate.parseLocalDateTime("2002-12-09")},

      // Test add years
      {"add_Years(c0, c1)", fixedDate, 4, formatDate.parseLocalDateTime("2007-07-09")},
      {"add_Years(c0, c1)", fixedDate, 0, fixedDate},
      {"add_Years(c0, c1)", fixedDate, -2, formatDate.parseLocalDateTime("2001-07-09")},
      {"add_Years(c0, c1)", fixedDate, 12, formatDate.parseLocalDateTime("2015-07-09")},
      {"add_Years(c0, c1)", fixedDate, -7, formatDate.parseLocalDateTime("1996-07-09")},

      // Test add years inverted inputs
      {"add_Years(c0, c1)", 4, fixedDate, formatDate.parseLocalDateTime("2007-07-09")},
      {"add_Years(c0, c1)", 0, fixedDate, fixedDate},
      {"add_Years(c0, c1)", -2, fixedDate, formatDate.parseLocalDateTime("2001-07-09")},
      {"add_Years(c0, c1)", 12, fixedDate, formatDate.parseLocalDateTime("2015-07-09")},
      {"add_Years(c0, c1)", -7, fixedDate, formatDate.parseLocalDateTime("1996-07-09")},

      // Test add times
      {"add_Seconds(c0, c1)", 30, fixedDate, dateTimeFormatter.parseLocalDateTime("2003-07-09 00:00:30")},
      {"add_Minutes(c0, c1)", -30L, fixedDate, dateTimeFormatter.parseLocalDateTime("2003-07-08 23:30:00")},
      {"add_Hours(c0, c1)", 20, fixedDate, dateTimeFormatter.parseLocalDateTime("2003-07-09 20:00:00")},

      // Test add times inverted inputs
      {"add_Seconds(c0, c1)",fixedDate, 30, dateTimeFormatter.parseLocalDateTime("2003-07-09 00:00:30")},
      {"add_Minutes(c0, c1)", fixedDate, -30L, dateTimeFormatter.parseLocalDateTime("2003-07-08 23:30:00")},
      {"add_Hours(c0, c1)", fixedDate, 20, dateTimeFormatter.parseLocalDateTime("2003-07-09 20:00:00")},

      // Test add epoch cycles
      {"add_Weeks(c0, c1)", 4L, fixedDate, formatDate.parseLocalDateTime("2003-08-06")},
      {"add_Quarters(c0, c1)", 4, fixedDate, formatDate.parseLocalDateTime("2004-07-09")},

      // Test add epoch cycles inverted inputs
      {"add_Weeks(c0, c1)", fixedDate, 4L, formatDate.parseLocalDateTime("2003-08-06")},
      {"add_Quarters(c0, c1)", fixedDate, 4, formatDate.parseLocalDateTime("2004-07-09")},

      // Test timestamp add all
      {"timestampaddDay(c0, c1)", fixedDate, 4, formatDate.parseLocalDateTime("2003-07-13")},
      {"timestampaddMonth(c0, c1)", fixedDate, 4, formatDate.parseLocalDateTime("2003-11-09")},
      {"timestampaddYear(c0, c1)", fixedDate, 4, formatDate.parseLocalDateTime("2007-07-09")},
      {"timestampaddWeek(c0, c1)", 4L, fixedDate, formatDate.parseLocalDateTime("2003-08-06")},
      {"timestampaddQuarter(c0, c1)", 4, fixedDate, formatDate.parseLocalDateTime("2004-07-09")},
      {"timestampaddSecond(c0, c1)", fixedDate, 30, dateTimeFormatter.parseLocalDateTime("2003-07-09 00:00:30")},
      {"timestampaddMinute(c0, c1)", fixedDate, -30L, dateTimeFormatter.parseLocalDateTime("2003-07-08 23:30:00")},
      {"timestampaddHour(c0, c1)", fixedDate, 20, dateTimeFormatter.parseLocalDateTime("2003-07-09 20:00:00")},

      // Test timestamp add all inverted inputs
      {"timestampaddDay(c0, c1)", 4, fixedDate, formatDate.parseLocalDateTime("2003-07-13")},
      {"timestampaddMonth(c0, c1)", 4, fixedDate, formatDate.parseLocalDateTime("2003-11-09")},
      {"timestampaddYear(c0, c1)", 4, fixedDate, formatDate.parseLocalDateTime("2007-07-09")},
      {"timestampaddWeek(c0, c1)", 4L, fixedDate, formatDate.parseLocalDateTime("2003-08-06")},
      {"timestampaddQuarter(c0, c1)", 4, fixedDate, formatDate.parseLocalDateTime("2004-07-09")},
      {"timestampaddSecond(c0, c1)", 30, fixedDate, dateTimeFormatter.parseLocalDateTime("2003-07-09 00:00:30")},
      {"timestampaddMinute(c0, c1)", -30L, fixedDate, dateTimeFormatter.parseLocalDateTime("2003-07-08 23:30:00")},
      {"timestampaddHour(c0, c1)", 20, fixedDate, dateTimeFormatter.parseLocalDateTime("2003-07-09 20:00:00")},

    });
  }
}

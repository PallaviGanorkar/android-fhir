/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.fhir.datacapture.utilities

import android.icu.text.DateFormat
import android.os.Build
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.time.chrono.IsoChronology
import java.time.format.DateTimeFormatterBuilder
import java.time.format.FormatStyle
import java.util.Date
import java.util.Locale

internal val LocalDate.localizedString: String
  get() {
    val date = Date.from(atStartOfDay(ZoneId.systemDefault())?.toInstant())
    return if (isAndroidIcuSupported()) DateFormat.getDateInstance(DateFormat.DEFAULT).format(date)
    else SimpleDateFormat.getDateInstance(DateFormat.DEFAULT, Locale.getDefault()).format(date)
  }

// Android ICU is supported API level 24 onwards.
private fun isAndroidIcuSupported() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N

/** Gives date format for current locale of device */
fun getDefaultDatePattern(): String {
  val dateFormat =
    DateTimeFormatterBuilder.getLocalizedDateTimePattern(
      FormatStyle.SHORT,
      null,
      IsoChronology.INSTANCE,
      Locale.getDefault()
    )
  return dateFormat
}

/*
 * Copyright 2019 Attribyte, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied.
 *
 * See the License for the specific language governing permissions
 * and limitations under the License.
 */

package org.attribyte.essem.reporter;

import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;

/**
 * A reported alert.
 */
public class Alert {

   /**
    * Alert severity.
    */
   public enum Severity {

      /**
       * Unknown.
       */
      UNKNOWN,

      /**
       * Informational.
       */
      INFO,

      /**
       * Warning.
       */
      WARN,

      /**
       * Error.
       */
      ERROR,

      /**
       * Fatal error.
       */
      FATAL;

      /**
       * Convert a string to a {@code Severity}.
       * @param str The string.
       * @return The severity.
       */
      public static final Severity fromString(final String str) {
         switch(Strings.nullToEmpty(str).trim().toLowerCase()) {
            case "info":
               return INFO;
            case "warn":
            case "warning":
               return WARN;
            case "error":
            case "err":
               return ERROR;
            case "fatal":
               return FATAL;
            default:
               return UNKNOWN;
         }
      }
   }

   /**
    * Creates an alert.
    * @param name The name.
    * @param severity The severity.
    * @param message The message.
    */
   public Alert(final String name, final Severity severity, final String message) {
      this.name = name;
      this.severity = severity;
      this.message = message;
   }


   @Override
   public String toString() {
      return MoreObjects.toStringHelper(this)
              .add("name", name)
              .add("severity", severity)
              .add("message", message)
              .toString();
   }

   /**
    * The alert name.
    */
   public final String name;

   /**
    * The alert severity.
    */
   public final Severity severity;

   /**
    * The alert value.
    */
   public final String message;
}

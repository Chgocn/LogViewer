package com.tibagni.logviewer.log;

import java.util.HashMap;
import java.util.Map;

public enum LogStream {
  MAIN,
  SYSTEM,
  RADIO,
  EVENTS,
  UNKNOWN;

  private static Map<LogStream, String[]> logNamesMap = new HashMap<>();
  static {
    String[] mainLogNames = {"main", "-m."};
    String[] systemLogNames = {"system", "-s."};
    String[] radioLogNames = {"radio", "-r."};
    String[] eventsLogNames = {"events", "-e."};
    logNamesMap.put(MAIN, mainLogNames);
    logNamesMap.put(SYSTEM, systemLogNames);
    logNamesMap.put(RADIO, radioLogNames);
    logNamesMap.put(EVENTS, eventsLogNames);
  }

  public static LogStream inferLogStreamFromName(String logName) {
    for (LogStream stream : logNamesMap.keySet()) {
      String[] possibilities = logNamesMap.get(stream);
      if (matchesWith(possibilities, logName)) {
        return stream;
      }
    }

    return UNKNOWN;
  }

  private static boolean matchesWith(String[] possibilities, String actualName) {
    if (possibilities == null || possibilities.length == 0 ||
        actualName == null || actualName.isEmpty()) {
      return false;
    }

    for (String possibility : possibilities) {
      if (actualName.toUpperCase().contains(possibility.toUpperCase())) {
        return true;
      }
    }

    return false;
  }
}

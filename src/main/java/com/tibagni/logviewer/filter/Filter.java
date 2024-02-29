package com.tibagni.logviewer.filter;

import com.tibagni.logviewer.log.LogEntry;
import com.tibagni.logviewer.log.LogLevel;
import com.tibagni.logviewer.log.LogStream;
import com.tibagni.logviewer.util.StringUtils;

import java.awt.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class Filter {
  public static final String FILE_EXTENSION = "filter";

  private boolean applied;
  private String name;
  private Color color;
  private LogLevel verbosity = LogLevel.VERBOSE;
  private Pattern pattern;
  private int flags = Pattern.CASE_INSENSITIVE;
  private ContextInfo temporaryInfo;
  private boolean isSimpleFilter;

  public boolean wasLoadedFromLegacyFile = false;

  // We intentionally don't copy the temporary info as it is temporary
  // We intentionally don't copy 'wasLoadedFromLegacyFile' as the copied filter would not have been loaded from a file
  @SuppressWarnings("CopyConstructorMissesField")
  public Filter(Filter from) throws FilterException {
    name = from.name;
    color = new Color(from.color.getRGB());
    flags = from.flags;
    applied = from.isApplied();
    pattern = getPattern(from.pattern.pattern());
    verbosity = from.verbosity;
    isSimpleFilter = from.isSimpleFilter;
  }

  public Filter(String name, String pattern, Color color, LogLevel verbosity) throws FilterException {
    this(name, pattern, color, verbosity, false);
  }

  public Filter(String name, String pattern, Color color, LogLevel verbosity, boolean caseSensitive)
      throws FilterException {
    updateFilter(name, pattern, color, verbosity, caseSensitive);
  }

  boolean nameIsPattern() {
    return StringUtils.areEquals(getName(), getPatternString());
  }

  public void updateFilter(String name, String pattern, Color color, LogLevel verbosity, boolean caseSensitive)
      throws FilterException {

    if (StringUtils.isEmpty(name) || StringUtils.isEmpty(pattern) || color == null) {
      throw new FilterException("You must provide a name, a regex pattern and a color for the filter");
    }

    if (caseSensitive) {
      flags &= ~Pattern.CASE_INSENSITIVE;
    } else {
      flags |= Pattern.CASE_INSENSITIVE;
    }

    this.name = name;
    this.color = color;
    this.pattern = getPattern(pattern);
    this.verbosity = verbosity;
    this.isSimpleFilter = !StringUtils.isPotentialRegex(pattern);
  }

  public static Filter createFromString(String filterString) throws FilterException {
    // See format in 'serializeFilter'
    try {
      String[] params = filterString.split(",");
      if (params.length < 4) {
        throw new IllegalArgumentException();
      }

      String[] rgb = params[3].split(":");
      if (rgb.length != 3) {
        throw new IllegalArgumentException("Wrong color format");
      }

      boolean isLegacy = params.length == 4;

      String name = params[0];
      String pattern = StringUtils.decodeBase64(params[1]);
      Color color = new Color(Integer.parseInt(rgb[0]), Integer.parseInt(rgb[1]), Integer.parseInt(rgb[2]));
      LogLevel verbosity = isLegacy ? LogLevel.VERBOSE : LogLevel.valueOf(params[4]);
      int flags = Integer.parseInt(params[2]);
      boolean isCaseSensitive = (flags & Pattern.CASE_INSENSITIVE) == 0;

      Filter filter = new Filter(name, pattern, color, verbosity, isCaseSensitive);
      filter.wasLoadedFromLegacyFile = isLegacy;
      return filter;
    } catch (Exception e) {
      throw new FilterException("Wrong filter format: " + filterString, e);
    }
  }

  public boolean isApplied() {
    return applied;
  }

  public void setApplied(boolean applied) {
    this.applied = applied;
  }

  public String getName() {
    return name;
  }

  public LogLevel getVerbosity() {
    return verbosity;
  }

  public Color getColor() {
    return color;
  }

  public String getPatternString() {
    return pattern.toString();
  }

  public ContextInfo getTemporaryInfo() {
    return temporaryInfo;
  }

  public void resetTemporaryInfo() {
    this.temporaryInfo = null;
  }

  void initTemporaryInfo() {
    temporaryInfo = new ContextInfo();
  }

  public boolean isCaseSensitive() {
    // Check if the CASE_INSENSITIVE is OFF!!
    return (flags & Pattern.CASE_INSENSITIVE) == 0;
  }

  /**
   * Take a single String and return whether it appliesTo this filter or not
   *
   * @param entry A single log line entry
   * @return true if this filter is applicable to the input line. False otherwise
   */
  public boolean appliesTo(LogEntry entry) {
    String inputLine = entry.getLogText();
    boolean foundPattern = isSimpleFilter ? simpleMatch(inputLine) : regexMatch(inputLine);
    boolean isVerbosityAllowed = verbosity.ordinal() <= entry.logLevel.ordinal();

    return foundPattern && isVerbosityAllowed;
  }

  private boolean simpleMatch(String inputLine) {
    if (isCaseSensitive()) {
      return inputLine.contains(getPatternString());
    }
    return inputLine.toLowerCase().contains(getPatternString().toLowerCase());
  }

  private boolean regexMatch(String inputLine) {
    return pattern.matcher(inputLine).find();
  }

  private Pattern getPattern(String pattern) throws FilterException {
    try {
      return Pattern.compile(pattern, flags);
    } catch (PatternSyntaxException e) {
      throw new FilterException("Invalid pattern: " + pattern, e);
    }
  }

  @Override
  public String toString() {
    return String.format("Filter: [Name=%s, pattern=%s, regexFlags=%d, color=%s, verbosity=%s, applied=%b]",
        name, pattern, flags, color, verbosity, applied);
  }

  public String serializeFilter() {
    return String.format("%s,%s,%d,%d:%d:%d,%s",
        name.replaceAll(",", " "),
        StringUtils.encodeBase64(getPatternString()),
        flags,
        color.getRed(),
        color.getGreen(),
        color.getBlue(),
        verbosity);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Filter filter = (Filter) o;
    return flags == filter.flags &&
        Objects.equals(name, filter.name) &&
        Objects.equals(color, filter.color) &&
        Objects.equals(getPatternString(), filter.getPatternString()) &&
        Objects.equals(temporaryInfo, filter.temporaryInfo);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, color, pattern, flags, temporaryInfo);
  }

  public static class ContextInfo {
    private final Map<LogStream, Integer> linesFound;
    private Set<LogStream> allowedStreams;

    private ContextInfo() {
      linesFound = new HashMap<>();
    }
    public void setAllowedStreams(Set<LogStream> allowedStreams) {
      this.allowedStreams = allowedStreams;
    }

    public int getTotalLinesFound() {
      int totalLinesFound = 0;
      for (Map.Entry<LogStream, Integer> entry : linesFound.entrySet()) {
        if (allowedStreams == null || allowedStreams.contains(entry.getKey())) {
          totalLinesFound += entry.getValue();
        }
      }

      return totalLinesFound;
    }

    // This method must be synchronized as the filters can be applied in parallel
    public synchronized void incrementLineCount(LogStream stream) {
      int currentCount = 0;
      if (linesFound.containsKey(stream)) {
        currentCount = linesFound.get(stream);
      }

      linesFound.put(stream, currentCount + 1);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      ContextInfo that = (ContextInfo) o;
      return Objects.equals(linesFound, that.linesFound) && Objects.equals(allowedStreams, that.allowedStreams);
    }

    @Override
    public int hashCode() {
      return Objects.hash(linesFound, allowedStreams);
    }
  }
}
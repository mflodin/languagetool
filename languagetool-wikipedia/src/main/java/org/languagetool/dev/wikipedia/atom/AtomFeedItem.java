/* LanguageTool, a natural language style checker
 * Copyright (C) 2013 Daniel Naber (http://www.danielnaber.de)
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301
 * USA
 */
package org.languagetool.dev.wikipedia.atom;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * One item in the Atom feed, i.e. one or more differences for an article.
 * @since 2.4
 */
class AtomFeedItem {

  private static final Pattern TABLE_DATA_CONTENT = Pattern.compile("<td.*?>(.*)</td>");

  private final String id;
  private final String title;
  private final String summary;

  AtomFeedItem(String id, String title, String summary) {
    this.id = Objects.requireNonNull(id);
    this.title = Objects.requireNonNull(title);
    this.summary = Objects.requireNonNull(summary);
  }

  String getId() {
    return id;
  }

  String getTitle() {
    return title;
  }

  String getSummary() {
    return summary;
  }

  List<String> getOldContent() {
    return getMarkedContent("−");  // note: that's not the standard minus ("-")
  }

  List<String> getNewContent() {
    return getMarkedContent("+");
  }

  private List<String> getMarkedContent(String plusMinusMarker) {
    List<String> result = new ArrayList<>();
    String[] lines = summary.split("\n");
    boolean expectingChange = false;
    for (String line : lines) {
      if (line.trim().startsWith("<td class=\"diff-marker\">" + plusMinusMarker + "</td>")) {
        expectingChange = true;
      } else if (expectingChange) {
        Matcher matcher = TABLE_DATA_CONTENT.matcher(line);
        if (matcher.find()) {
          String cleanContent = matcher.group(1)
                  .replaceAll("<span.*?>", "").replace("</span>", "")
                  .replaceAll("<div.*?>", "").replace("</div>", "");
          result.add(cleanContent);
        } else {
          throw new RuntimeException("Expected change ('" + plusMinusMarker + "') not found in line: " + line);
        }
        expectingChange = false;
      }
    }
    return result;
  }

  /**
   * Get the diff id from the 'id' element, or {@code 0} if the diff is the creation of a new article.
   */
  public long getDiffId() {
    Pattern idPattern = Pattern.compile("diff=(\\d+)");
    Matcher matcher = idPattern.matcher(id);
    if (matcher.find()) {
      return Long.parseLong(matcher.group(1));
    } else {
      // newly created article
      return 0;
    }
  }

  @Override
  public String toString() {
    return "AtomFeedItem{" +
            "id='" + id + '\'' +
            ", title='" + title + "\'}";
  }
}

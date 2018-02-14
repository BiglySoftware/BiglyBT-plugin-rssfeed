/*
 * RSSFeed - Azureus2 Plugin
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston,
 * MA 02111-1307 USA
 */

package org.kmallan.azureus.rssfeed;


import java.io.*;
import java.util.*;
import java.util.regex.*;

public class FilterBean implements Serializable {

  static final long serialVersionUID = -979691945084080240L;

  	// can't change type+mode to integer due to serialization
  
  public static final int MODE_PASS	= 0;
  public static final int MODE_FAIL	= 1;

  public static final int TYPE_TVSHOW	= 0;
  public static final int TYPE_MOVIE	= 1;
  public static final int TYPE_OTHER	= 2;
  public static final int TYPE_NONE		= 3;
  
  private String name, storeDir, expression, exclude, category, type, mode;
  private List<String> excludes;
  private int state, rateUpload, rateDownload, startSeason, startEpisode, endSeason, endEpisode;
  private long filtId, urlId = 0;
  private boolean isRegex, isFilename, matchTitle, matchLink, moveTop, customRate, renameFile, renameIncEpisode, disableAfter, cleanFile, enabled;
  private boolean smartHistory = true;

  private String exprLower;
  private Pattern exprPat;

  private long	minTorrentSize;
  private long  maxTorrentSize;

  // Match quality indicators like 720p or 1080i etc.
  public static final String qualityPattern = "\\b\\d{3,4}[ip]\\b";

  // Most movie torrents will follow the naming convention <title><delimiters><year>
  public static Pattern moviePattern = Pattern.compile("^(.*)[._\\-\\s]+\\(?(\\d{4})");

  private static final Pattern[] episodePatterns = new Pattern[] {
      // S##E## Style patterns
      //

      // s02e03-s02e5
      Pattern.compile("(.*?)" + "s([0-9]+)e([0-9]+)[\\-\\+]s([0-9]+)e([0-9]+)" + ".*?"),
      // s02e03-e05
      Pattern.compile("(.*?)" + "s([0-9]+)e([0-9]+)[\\-\\+]e([0-9]+)" + ".*?"),
      // s02e03-05
      Pattern.compile("(.*?)" + "s([0-9]+)e([0-9]+)[\\-\\+]([0-9]+)" + ".*?"),
      // s02e03
      Pattern.compile("(.*?)" + "s([0-9]+)e([0-9]+)" + ".*?"),

      // Miniseries style patterns - no season specified, set to '1'
      //

      // part3
      Pattern.compile("(.*?)" + "(part)[-._ ]([0-9]+)" + ".*?"),

      // Number only patterns - keep these last because they tend to match too easily and disrupt valid patterns
      //

      // 02x03-02x05
      Pattern.compile("(.*?)" + "([0-9]+)x([0-9]+)[\\-\\+]([0-9]+)x([0-9]+)" + ".*?"),
      // 02x03-05
      Pattern.compile("(.*?)" + "([0-9]+)x([0-9]+)[\\-\\+]([0-9]+)" + ".*?"),
      // 02x03
      Pattern.compile("(.*?)" + "([0-9]+)x([0-9]+)" + ".*?"),
      // 203-205
      Pattern.compile("(.*?)" + "([0-9]+)([0-9]{2})[\\-\\+]([0-9]+)([0-9]{2})" + ".*?"),
      // 203-05
      Pattern.compile("(.*?)" + "([0-9]+)([0-9]{2})[\\-\\+]([0-9]{2})" + ".*?"),
      // 2-3
      Pattern.compile("(.*?)" + "([0-9]+) - ([0-9]+)" + ".*?"),
      // 203
      Pattern.compile("(.*?)" + "([0-9]+)([0-9]{2})" + ".*?"),
  };

  private static final Pattern episodeDatePattern =
  Pattern.compile("(.*?)" + "([0-9]{4}).([0-9]{2}).([0-9]{2})" + ".*?");


  // Proper torrents are published if originals are bad.
  public static Pattern properPattern = Pattern.compile("\\bproper\\b");

  public FilterBean() {
    filtId = System.currentTimeMillis();

    exprLower = "";
    exprPat = Pattern.compile(".*" + exprLower + ".*");
  }

  public long getID() {
    return filtId;
  }

  public void setID(long filtId) {
    this.filtId = filtId;
  }

  public String getName() {
    if(name == null) name = "";
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getStoreDir() {
    if(storeDir == null) storeDir = "";
    return storeDir;
  }

  public void setStoreDir(String storeDir) {
    this.storeDir = storeDir;
  }

  public String getExpression() {
    if(expression == null) expression = "";
    return expression;
  }

  public void setExpression(String expression) {
    this.expression = expression;
    exprLower = expression.toLowerCase();
    try {
      exprPat = Pattern.compile(".*" + exprLower + ".*");
    } catch (PatternSyntaxException e) {
      exprPat = null;
    }
  }

  public boolean getIsRegex() {
    return isRegex;
  }

  public void setIsRegex(boolean isRegex) {
    this.isRegex = isRegex;
  }
  public boolean getIsFilename() {
    return isFilename;
  }

  public void setIsFilename(boolean isFilename) {
    this.isFilename = isFilename;
  }

  public boolean getMatchTitle() {
    return matchTitle;
  }

  public void setMatchTitle(boolean matchTitle) {
    this.matchTitle = matchTitle;
  }

  public boolean getMatchLink() {
    return matchLink;
  }

  public void setMatchLink(boolean matchLink) {
    this.matchLink = matchLink;
  }

  public boolean getMoveTop() {
    return moveTop;
  }

  public void setMoveTop(boolean moveTop) {
    this.moveTop = moveTop;
  }

  public int getState() {
    return state;
  }

  public void setState(int state) {
    this.state = state;
  }

  public boolean getRateUseCustom() {
    return customRate;
  }

  public void setRateUseCustom(boolean customRate) {
    this.customRate = customRate;
  }

  public int getRateUpload() {
    return rateUpload;
  }

  public void setRateUpload(int rateUpload) {
    this.rateUpload = rateUpload;
  }

  public int getRateDownload() {
    return rateDownload;
  }

  public void setRateDownload(int rateDownload) {
    this.rateDownload = rateDownload;
  }

  public String getExclude() {
    if(exclude == null) exclude = "";
    return exclude;
  }

  public void setExclude(String exclude) {
    this.exclude = exclude;
    excludes = new ArrayList<String>();
    final String[] split = exclude.split("[,;]");
    for (String s : split) {
      final String trim = s.trim();
      if (trim.length() > 0 ) {
        excludes.add(trim.toLowerCase());
      }
    }
  }

  public String getCategory() {
    if(category == null) category = "";
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public long getFeed() {
    return urlId;
  }

  public void setFeed(long urlId) {
    this.urlId = urlId;
  }

  public int getTypeIndex() {
    return View.convertTypeFromString( type );
  }

  public void setTypeIndex(int type) {
    this.type = String.valueOf( type );
  }

  public int getStartSeason() {
    return startSeason;
  }

  public void setStartSeason(int startSeason) {
    this.startSeason = startSeason;
  }

  public int getStartEpisode() {
    return startEpisode;
  }

  public void setStartEpisode(int startEpisode) {
    this.startEpisode = startEpisode;
  }

  public int getEndSeason() {
    return endSeason;
  }

  public void setEndSeason(int endSeason) {
    this.endSeason = endSeason;
  }

  public int getEndEpisode() {
    return endEpisode;
  }

  public void setEndEpisode(int endEpisode) {
    this.endEpisode = endEpisode;
  }

  public boolean getRenameFile() {
    return renameFile;
  }

  public void setRenameFile(boolean renameFile) {
    this.renameFile = renameFile;
  }

  public boolean getRenameIncEpisode() {
    return renameIncEpisode;
  }

  public void setRenameIncEpisode(boolean renameIncEpisode) {
    this.renameIncEpisode = renameIncEpisode;
  }

  public boolean getDisableAfter() {
    return disableAfter;
  }

  public void setDisableAfter(boolean disableAfter) {
    this.disableAfter = disableAfter;
  }

  public boolean getCleanFile() {
    return cleanFile;
  }

  public void setCleanFile(boolean cleanFile) {
    this.cleanFile = cleanFile;
  }

  public int getModeIndex() {
	 return View.convertModeFromString( type );
  }

  public void setModeIndex(int mode) {
    this.mode = String.valueOf( mode );
  }

  public boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public boolean matches(long urlId, String title, String link) {
    if(!getEnabled()) return false;
    if(getFeed() != 0 && urlId != getFeed()) return false;

    boolean matched = false;
    if((getMatchTitle()) && (match(title))) matched = true;
    else if((getMatchLink()) && (match(link))) matched = true;
    if(!matched) return false;

    if (excludes != null) {
      for (String e : excludes) {
        if ((matchTitle && title.contains(e)) || (matchLink && link.contains(e))) {
          return false;
        }
      }
    }

    if(getTypeIndex() == TYPE_TVSHOW && getStartSeason() + getEndSeason() >= 0) {
      Episode e = getSeason(title);
      if(e == null) e = getSeason(link);
      if(e == null) return false;

      if(getStartSeason() >= 0 && getEndSeason() > 0) {
        if(!e.inRange(getStartSeason(), getStartEpisode(), getEndSeason(), getEndEpisode())) return false;
      } else if(getStartSeason() >= 0) {
        if(!e.isFrom(getStartSeason(), getStartEpisode())) return false;
      } else {
        if(!e.isUpto(getEndSeason(), getEndEpisode())) return false;
      }
    }
    return true;
  }

  public static Episode getSeason(String str) {
    str = cleanupUrl(str.toLowerCase());

    String showTitle = "";
    int seasonStart, seasonEnd, episodeStart, episodeEnd;
    Episode e = null;

    Matcher m = null;

    // get rid of quality indicators (720p, 1080i etc) so they don't incorrectly match as season/episode
    str = str.replaceAll(qualityPattern, "");
    // First, try by date because we might get caught by a catch-all episode regex
    m = episodeDatePattern.matcher(str);
    if (m.matches()) {
      final int end = m.end(m.groupCount());
      final boolean isProper = properPattern.matcher(str.substring(end)).find();
      showTitle = stringClean(m.group(1));
      seasonStart = Integer.parseInt(m.group(2));
      episodeStart = Integer.parseInt(m.group(3)) * 100 + Integer.parseInt(m.group(4));
      e = new Episode(showTitle, seasonStart, episodeStart, isProper);
      return e;
    }

    for (Pattern pattern : episodePatterns) {
      m = pattern.matcher(str);
      if (m.matches()) {
        break;
      }
    }
    if (m.matches()) {
      showTitle = stringClean(m.group(1));
      final int end = m.end(m.groupCount());
      final boolean isProper = properPattern.matcher(str.substring(end)).find();

      switch(m.groupCount()) {
        case 3:
          final String season = m.group(2);
          seasonStart = Character.isDigit(season.charAt(0)) ? Integer.parseInt(season) : 1;
          episodeStart = Integer.parseInt(m.group(3));
          e = new Episode(showTitle, seasonStart, episodeStart, isProper);
          break;
        case 4:
          seasonStart = Integer.parseInt(m.group(2));
          episodeStart = Integer.parseInt(m.group(3));
          seasonEnd = Integer.parseInt(m.group(2));
          episodeEnd = Integer.parseInt(m.group(4));
          e = new Episode(showTitle, seasonStart, episodeStart, seasonEnd, episodeEnd, isProper);
          break;
        case 5:
          seasonStart = Integer.parseInt(m.group(2));
          episodeStart = Integer.parseInt(m.group(3));
          seasonEnd = Integer.parseInt(m.group(4));
          episodeEnd = Integer.parseInt(m.group(5));
          e = new Episode(showTitle, seasonStart, episodeStart, seasonEnd, episodeEnd, isProper);
          break;
      }
    }

    return e;
  }

  public static Movie getMovie(String str) {
    final Matcher m = moviePattern.matcher(cleanupUrl(str.toLowerCase()));
    if(!m.find()) {
      Plugin.debugOut("No match: " + str);
      return null;
    }

    final String title = stringClean(m.group(1));
    final int year = Integer.parseInt(m.group(2));
    final int end = m.end(m.groupCount());
    final boolean isProper = properPattern.matcher(str.substring(end)).find();

    final Movie movie = new Movie(title, year, isProper);
    Plugin.debugOut("Matched movie: " + movie);
    return movie;
  }

  private static String cleanupUrl(String str) {
    Pattern lmp = Pattern.compile("(ht|f)tp:.*/(.*?)");
    Matcher lmm = lmp.matcher(str);
    if(lmm.matches()) str = lmm.group(2); // strip if url
    return str;
  }

  public static String stringClean(String str) {
    str = str.replaceAll("[ \\._\\-]+", " ");
    str = str.replaceAll("\\[.*\\]", "");
    str = str.trim();
    if(!str.equals(str.toLowerCase())) return str;

    String[] strp = str.split("[^\\w\\d]+");
    for(int iLoop = 0; iLoop < strp.length; iLoop++) {
      if(strp[iLoop].length() == 0) continue;
      String c = String.valueOf(strp[iLoop].charAt(0));
      String nstrp = strp[iLoop].replaceFirst(c, c.toUpperCase());
      str = str.replaceAll(strp[iLoop], nstrp);
    }
    return str;
  }

  private boolean match(String matchee) {
    if (getIsFilename()) {
      return matchFromFile(matchee);
    } else {
      if(getIsRegex()){
        if ( exprPat == null ){
          return( false );	// invalid expression, always fail
        }
        Matcher m = exprPat.matcher(matchee.toLowerCase());
        return m.find();
      } else {
        if(matchee.toLowerCase().contains(exprLower)) return true;
      }
    }
    return false;
  }

  private boolean matchFromFile(String matchee) {
    try {
      final BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(expression)));
      try {
        while (true) {
          final String line = in.readLine();
          if (line == null) {
            break;
          }
          if (line.trim().length() == 0) {
            continue;
          }
          final boolean isMatch;
          if (getIsRegex()) {
            try {
              final Pattern pattern = Pattern.compile(line.toLowerCase());
              isMatch = pattern.matcher(matchee).find();
            } catch (PatternSyntaxException e) {
              continue;
            }
          } else {
            isMatch = line.toLowerCase().contains(matchee);
          }
          if (isMatch) {
            return true;
          }
        }
      } finally {
        in.close();
      }
    } catch (FileNotFoundException e) {
      return false;
    } catch (IOException e) {
      return false;
    }
    return false;
  }


  public boolean getUseSmartHistory() {
    if( TYPE_TVSHOW == getTypeIndex()) return smartHistory;
    else return true;
  }

  public void setUseSmartHistory(boolean smartHistory) {
    this.smartHistory = smartHistory;
  }
  
  public long
  getMinTorrentSize()
  {
	  return( minTorrentSize );
  }
  
  public void
  setMinTorrentSize(
	long		l )
  {
	  minTorrentSize = l;
  }
  
  public long
  getMaxTorrentSize()
  {
	  return( maxTorrentSize );
  }
  
  public void
  setMaxTorrentSize(
	long		l )
  {
	  maxTorrentSize = l;
  }
}

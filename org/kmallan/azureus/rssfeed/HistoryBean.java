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

import java.io.Serializable;
import java.util.regex.*;

public class HistoryBean implements Serializable, Comparable {

  static final long serialVersionUID = -4112775156287555070L;

  private String fileData, location, filtType, filtName;
  private long histId, filtId;
  private String title;
  private int year;
  private int seasonStart, seasonEnd, episodeStart, episodeEnd;
  private boolean proper;

  public HistoryBean() {
    histId = System.currentTimeMillis();
  }

  public long getID() {
    return histId;
  }

  public void setID(long histId) {
    this.histId = histId;
  }

  public long getFiltID() {
    return filtId;
  }

  public void setFiltID(long filtId) {
    this.filtId = filtId;
  }

  public String getFileData() {
    if(fileData == null) fileData = "";
    return fileData;
  }

  public void setFileData(String fileData) {
    this.fileData = fileData;
  }

  public String getLocation() {
    if(location == null) location = "";
    return location;
  }

  public void setLocation(String location) {
    this.location = location;
  }

  public boolean setSeason(String str) {
    final Episode episode = FilterBean.getSeason(str);

    if (episode == null) {
      return false;
    }

    setTitle(episode.showTitle);
    setProper(episode.isProper());
    setSeasonStart(episode.seasonStart);
    setSeasonEnd(episode.seasonEnd);
    setEpisodeStart(episode.episodeStart);
    setEpisodeEnd(episode.episodeEnd);
    return true;
  }

  public boolean setMovie(String str) {
    final Movie movie = FilterBean.getMovie(str);
//    final Movie movie = null;
    if (movie != null) {
      setTitle(movie.getTitle());
      setYear(movie.getYear());
      setProper(movie.isProper());
      return true;
    }
    return false;
  }


  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public int getYear() {
    return year;
  }

  public void setYear(int year) {
    this.year = year;
  }

  public boolean isProper() {
    return proper;
  }

  public void setProper(boolean proper) {
    this.proper = proper;
  }

  public int getSeasonStart() {
    return seasonStart;
  }

  public void setSeasonStart(int seasonStart) {
    this.seasonStart = seasonStart;
  }

  public int getSeasonEnd() {
    return seasonEnd;
  }

  public void setSeasonEnd(int seasonEnd) {
    this.seasonEnd = seasonEnd;
  }

  public int getEpisodeStart() {
    return episodeStart;
  }

  public void setEpisodeStart(int episodeStart) {
    this.episodeStart = episodeStart;
  }

  public int getEpisodeEnd() {
    return episodeEnd;
  }

  public void setEpisodeEnd(int episodeEnd) {
    this.episodeEnd = episodeEnd;
  }

  public String toString() {
    return location;
  }

  @Override
  public int compareTo(Object o) {
    return -(new Long(histId).compareTo(new Long(((HistoryBean)o).histId)));
  }

  public void setFilter(FilterBean filter) {
    if(filter != null) {
      this.filtId = filter.getID();
      this.filtName = filter.getName();
      this.filtType = String.valueOf( filter.getTypeIndex());
    }
  }

  public String getFiltName() {
    return filtName;
  }

  public int getFiltTypeIndex() {
	  return View.convertTypeFromString( filtType );
  }
}

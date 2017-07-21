package org.kmallan.azureus.rssfeed;

public class Movie {
  public String title = "";
  public int year;
  private boolean proper;

  public Movie(String title, int year, boolean proper) {
    this.title = title;
    this.year = year;
    this.proper = proper;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title.toLowerCase();
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

  public String toString() {
    return "Movie{" +
        "title='" + title + '\'' +
        ", year=" + year +
        ", proper=" + proper +
        '}';
  }
}

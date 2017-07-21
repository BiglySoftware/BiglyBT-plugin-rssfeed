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

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.*;
import com.biglybt.core.internat.MessageText;

import java.io.*;

public class Help extends StyledText {
  private Display display;

  private static final String lineSeparator = System.getProperty("line.separator");

  private String helpFile = "";

  public Help(Composite parent, int style) {
    super(parent, style);

    display = parent.getDisplay();
  }

  public Help(Composite parent) {
    this(parent, SWT.NULL);
  }

  public void load() throws Exception {
    String curLocale = MessageText.getCurrentLocale().toString();
    String newHelpFile = "/org/kmallan/resource/lang/Help" + (curLocale.equalsIgnoreCase("")?"":"_" + curLocale) + ".stf";
    if(helpFile.equalsIgnoreCase(newHelpFile)) return;

    InputStream stream = getClass().getResourceAsStream(newHelpFile);
    if(stream == null) {
      newHelpFile = "/org/kmallan/resource/lang/Help.stf";
      if(helpFile.equalsIgnoreCase(newHelpFile)) return;

      stream = getClass().getResourceAsStream(newHelpFile);
    }

    helpFile = newHelpFile;

    this.setText("");
    if(stream == null) {
      System.err.println("RSSFeed: Error loading resource: /org/kmallan/resource/help.stf");
    } else {
      BufferedReader in = new BufferedReader(new InputStreamReader(stream));

      this.setRedraw(false);
      this.setWordWrap(true);

      Color black = new Color((Device) display, 0, 0, 0);
      Color white = new Color((Device) display, 255, 255, 255);
      Color light = new Color((Device) display, 200, 200, 200);
      Color grey = new Color((Device) display, 50, 50, 50);
      Color green = new Color((Device) display, 30, 80, 30);
      Color blue = new Color((Device) display, 20, 20, 80);
      Color fg, bg;
      int style;
      boolean setStyle;

      this.setForeground(grey);

      String line;
      while((line = in.readLine()) != null) {

        setStyle = false;
        fg = grey;
        bg = white;
        style = SWT.NORMAL;

        char styleChar;
        String text;

        if(line.length() < 2) {
          styleChar = ' ';
          text = " " + lineSeparator;
        } else {
          styleChar = line.charAt(0);
          text = line.substring(1) + lineSeparator;
        }

        switch(styleChar) {
          case '*':
            text = "  * " + text;
            fg = green;
            setStyle = true;
            break;
          case '+':
            text = "     " + text;
            fg = black;
            bg = light;
            style = SWT.BOLD;
            setStyle = true;
            break;
          case '!':
            style = SWT.BOLD;
            setStyle = true;
            break;
          case '@':
            fg = blue;
            setStyle = true;
            break;
          case '$':
            bg = blue;
            fg = white;
            style = SWT.BOLD;
            setStyle = true;
            break;
          case ' ':
            text = "  " + text;
            break;
        }

        this.append(text);

        if(setStyle) {
          int lineCount = this.getLineCount() - 1;
          int charCount = this.getCharCount();

          int lineOfs = this.getOffsetAtLine(lineCount - 1);
          int lineLen = charCount - lineOfs;
          this.setStyleRange(new StyleRange(lineOfs, lineLen, fg, bg, style));
          this.setLineBackground(lineCount - 1, 1, bg);
        }
      }
    }

    this.setRedraw(true);
  }
}

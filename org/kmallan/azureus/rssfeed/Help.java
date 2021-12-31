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
import com.biglybt.ui.swt.Utils;

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

      Color fg_black;
      Color fg_white;
      Color bg_white;
      Color bg_light;
      Color fg_grey;
      Color fg_green;
      Color fg_blue;
      Color bg_blue;
      
      if ( Utils.isDarkAppearanceNative()){
    	  	// white on black, better than nothing...
	      fg_black = new Color((Device) display, 255, 255, 255);
	      fg_white = new Color((Device) display, 255, 255, 255);
	      bg_white = new Color((Device) display, 0, 0, 0);
	      bg_light = new Color((Device) display, 0, 0, 0);
	      fg_grey = new Color((Device) display, 255, 255, 255);
	      fg_green = new Color((Device) display, 255, 255, 255);
	      fg_blue = new Color((Device) display, 255, 255, 255);
	      bg_blue = new Color((Device) display, 0, 0, 0);
	      
	      setBackground( bg_white );
      }else{
	      fg_black = new Color((Device) display, 0, 0, 0);
	      fg_white = new Color((Device) display, 255, 255, 255);
	      bg_white = new Color((Device) display, 255, 255, 255);
	      bg_light = new Color((Device) display, 200, 200, 200);
	      fg_grey = new Color((Device) display, 50, 50, 50);
	      fg_green = new Color((Device) display, 30, 80, 30);
	      fg_blue = new Color((Device) display, 20, 20, 80);
	      bg_blue = new Color((Device) display, 20, 20, 80);
      }
      
      addListener( SWT.Dispose, (ev)->{
    	  fg_black.dispose();
    	  fg_white.dispose();
    	  bg_white.dispose();
    	  bg_light.dispose();
    	  fg_grey.dispose();
    	  fg_green.dispose();
    	  fg_blue.dispose();
    	  bg_blue.dispose();
      });
      
      Color fg, bg;
      int style;
      boolean setStyle;

      this.setForeground(fg_grey);

      String line;
      while((line = in.readLine()) != null) {

        setStyle = false;
        fg = fg_grey;
        bg = bg_white;
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
            fg = fg_green;
            setStyle = true;
            break;
          case '+':
            text = "     " + text;
            fg = fg_black;
            bg = bg_light;
            style = SWT.BOLD;
            setStyle = true;
            break;
          case '!':
            style = SWT.BOLD;
            setStyle = true;
            break;
          case '@':
            fg = fg_blue;
            setStyle = true;
            break;
          case '$':
            bg = bg_blue;
            fg = fg_white;
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

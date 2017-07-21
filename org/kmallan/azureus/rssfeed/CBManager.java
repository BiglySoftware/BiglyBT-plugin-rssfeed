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

import java.awt.*;
import java.awt.datatransfer.*;
import java.io.IOException;

public class CBManager implements ClipboardOwner {

  @Override
  public void lostOwnership(Clipboard clipboard, Transferable contents) {
  }

  public void setClipboardContents(String str) {
    StringSelection strSel = new StringSelection(str);
    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
    clipboard.setContents(strSel, this);
  }

  public String getClipboardContents() {
    String result = "";
    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
    // odd: the Object param of getContents is not currently used
    Transferable contents = clipboard.getContents(null);
    boolean hasTransferableText = (contents != null) && contents.isDataFlavorSupported(DataFlavor.stringFlavor);
    if(hasTransferableText) {
      try {
        result = (String) contents.getTransferData(DataFlavor.stringFlavor);
      } catch(UnsupportedFlavorException e) {
        // highly unlikely since we are using a standard DataFlavor
        System.err.println("Unable to get clipboard contents: " + e);
      } catch(IOException e) {
        System.err.println("Unable to get clipboard contents: " + e);
      }
    }
    return result;
  }
}

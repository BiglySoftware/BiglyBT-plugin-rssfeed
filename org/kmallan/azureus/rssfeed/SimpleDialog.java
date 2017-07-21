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

import com.biglybt.ui.swt.imageloader.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import com.biglybt.ui.swt.*;

public class SimpleDialog extends Dialog {
  Object result;

  private String title, info;

  public SimpleDialog(Shell parent, int style) {
    super(parent, style);
  }

  public SimpleDialog(Shell parent) {
    this(parent, SWT.NULL);
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getTitle() {
    if(title == null) title = "";
    return title;
  }

  public void setInfo(String info) {
    this.info = info;
  }

  public String getInfo() {
    if(info == null) info = "";
    return info;
  }

  public Object open() {
    if(getInfo().equalsIgnoreCase("")) return null;

    GridLayout layout;
    GridData layoutData;

    Shell parent = getParent();
    final Shell myShell = new Shell(parent, SWT.MODELESS | SWT.DIALOG_TRIM | SWT.MAX | SWT.RESIZE);
    myShell.setSize(640, 480);
    myShell.setImage(com.biglybt.ui.swt.imageloader.ImageLoader.getInstance().getImage("logo16"));
    myShell.setText(getText());

    layout = new GridLayout();
    layout.marginHeight = 0;
    layout.marginWidth = 0;
    layoutData = new GridData(GridData.FILL_BOTH);
    myShell.setLayout(layout);
    myShell.setLayoutData(layoutData);

    Display display = parent.getDisplay();

    Composite myComp = new Composite(myShell, SWT.NULL);
    layout = new GridLayout();
    layout.marginHeight = 0;
    layout.marginWidth = 0;
    layoutData = new GridData(GridData.FILL_BOTH);
    myComp.setLayout(layout);
    myComp.setLayoutData(layoutData);

    Composite tComp = new Composite(myComp, SWT.BORDER);
    layout = new GridLayout();
    layout.marginHeight = 3;
    layout.marginWidth = 0;
    layoutData = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_CENTER);
    tComp.setLayout(layout);
    tComp.setLayoutData(layoutData);

    tComp.setBackground(display.getSystemColor(SWT.COLOR_LIST_SELECTION));
    tComp.setForeground(display.getSystemColor(SWT.COLOR_LIST_SELECTION_TEXT));

    Label tLabel = new Label(tComp, SWT.NULL);
    layoutData = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_CENTER);
    tLabel.setLayoutData(layoutData);
    tLabel.setText(getTitle());

    tLabel.setBackground(display.getSystemColor(SWT.COLOR_LIST_SELECTION));
    tLabel.setForeground(display.getSystemColor(SWT.COLOR_LIST_SELECTION_TEXT));
    FontData[] curTLabel = tLabel.getFont().getFontData();
    curTLabel[0].setStyle(SWT.BOLD);
    curTLabel[0].setHeight((int) (curTLabel[0].getHeight() * 1.2));
    tLabel.setFont(new Font((Device) display, curTLabel));

    final ScrolledComposite lScrollComp = new ScrolledComposite(myComp, SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
    layout = new GridLayout();
    layoutData = new GridData(GridData.FILL_BOTH);
    lScrollComp.setLayout(layout);
    lScrollComp.setLayoutData(layoutData);

    lScrollComp.setExpandHorizontal(true);
    lScrollComp.setExpandVertical(true);

    final Composite lMainComp = new Composite(lScrollComp, SWT.NULL);
    lScrollComp.setContent(lMainComp);
    layout = new GridLayout();
    layoutData = new GridData(GridData.FILL_BOTH);
    lMainComp.setLayout(layout);
    lMainComp.setLayoutData(layoutData);

    Composite lComp = new Composite(lMainComp, SWT.NULL);
    layout = new GridLayout();
    layout.numColumns = 5;
    layout.marginHeight = 0;
    layout.marginWidth = 0;
    layoutData = new GridData(GridData.FILL_HORIZONTAL);
    lComp.setLayout(layout);
    lComp.setLayoutData(layoutData);

    StyledText st = new StyledText(lComp, SWT.NULL);
    FontData[] curLbl = st.getFont().getFontData();
    curLbl[0].setName("courier");
    st.setFont(new Font((Device) display, curLbl));
    st.setText(getInfo());
    st.setEditable(false);

    lMainComp.setBackground(st.getBackground());
    lComp.setBackground(st.getBackground());

    lScrollComp.setMinSize(lMainComp.computeSize(SWT.DEFAULT, SWT.DEFAULT));

    Composite bComp = new Composite(myComp, SWT.NULL);
    layout = new GridLayout();
    layout.marginHeight = 3;
    layout.marginWidth = 3;
    layoutData = new GridData(GridData.HORIZONTAL_ALIGN_CENTER | GridData.VERTICAL_ALIGN_CENTER);
    bComp.setLayout(layout);
    bComp.setLayoutData(layoutData);

    Button btnHide = new Button(bComp, SWT.PUSH);
    Messages.setLanguageText(btnHide, "RSSFeed.InfoWin.Hide");

    myShell.layout();
    myShell.open();

    btnHide.addListener(SWT.Selection, new Listener() {
      @Override
      public void handleEvent(Event e) {
        myShell.close();
      }
    });
    return result;
  }
}

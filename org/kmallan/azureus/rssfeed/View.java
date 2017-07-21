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

import com.biglybt.pif.PluginInterface;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.*;
import com.biglybt.core.config.*;
import com.biglybt.core.internat.MessageText;
import com.biglybt.core.util.Constants;
import com.biglybt.core.util.Debug;
import com.biglybt.pif.download.Download;
import com.biglybt.pif.torrent.Torrent;
import com.biglybt.ui.swt.*;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Timer;

public class View implements MouseListener, SelectionListener, MenuListener, ModifyListener,
    Listener, ParameterListener {

  private Plugin plugin;
  private PluginInterface pluginInterface;
  public Config rssfeedConfig;
  private Scheduler scheduler;
  private Timer timer;
  public Display display;
  public Shell shell;

  public TorrentDownloader torrentDownloader;

  private CTabFolder tabFolder;
  private CTabItem tabStatus, tabOptions, tabHist, tabHelp;
  private Composite status, options, history, help;
  public Label reloadLabel;
  public TableTree listTable;
  public Table histTable;

  public Composite optParamComp;
  private ScrolledComposite optParamScrollComp;

  public Composite filtParamComp, filtSpecificTVShow, filtSpecificOther;
  public Table filtTable;
  private ToolItem btnFiltUp, btnFiltAdd, btnFiltCopy, btnFiltRemove, btnFiltDown;
  private Button btnFiltStoreDir, btnFiltFileBrowse, btnFiltFileEdit, btnFiltAccept, btnFiltReset, btnFiltCancel;

  public Composite urlParamComp, urlOptCompCustReferer, urlOptCompCookie, urlOptCompNone;
  public Table urlTable;
  public Text urlStoreDir, urlDelay, urlReferer, urlCookie;
  private ToolItem btnUrlUp, btnUrlAdd, btnUrlRemove, btnUrlDown;
  private Button btnUrlAccept, btnUrlReset, btnUrlCancel;
  public Button btnUrlStoreDir, urlObeyTTL, urlLocRef, urlUseCookie;

  public Composite filtRatesCustom, filtRatesNone;
  public Text filtName, filtStoreDir, filtExpression, filtExclude, filtRateUpload, filtRateDownload, filtCategory, filtStartSeason, filtStartEpisode, filtEndSeason, filtEndEpisode, filtTestMatch, filtMinTorrentSize, filtMaxTorrentSize;
  public Button filtIsRegex, filtIsFilename, filtMatchTitle, filtMatchLink, filtMoveTop, filtRateUseCustom, filtRename, filtRenameEppTitle, filtDisable, filtCleanup, filtEnabled, filtSmartHist;
  public Combo filtState, filtPriority, filtType, filtMode, filtFeed;

  public Text urlName, urlLocation;
  public Button urlEnabled;

  private UrlTableItem selUrlItem = null;
  private FilterTableItem selFilterItem = null;
  private ListTreeItem selListItem = null;

  public Button btnFiltTest, btnConfigAccept, btnConfigReset, configRenEpisodeWCEnabled, configRenEpisodeFLEnabled, configRenTitleWCEnabled, configRenTitleFLEnabled, configRenKeepTitle, configRenKeepLink, configRenKeepFileName, configRenPrefsTitle, configRenPrefsLink, configRenPrefsFileName, configEnabled;
  public Text configCleanup, configRenEpisodeWCIgnore, configRenTitleWCIgnore, configRenSeperator, configDelay;
  public Combo configRenEppIndicator;

  private Menu histTableMenu, listTableMenu;
  private MenuItem itemCopyFile, itemCopyTorrent, itemDelete;
  private MenuItem itemRefresh, itemRefreshAll, itemDownload, itemDownloadTo, itemCancel, itemCreateFilter, itemCopyLink, itemOpenLink, itemShowInfo;
  private MenuItem itemExpandAll, itemCollapseAll;

  public TreeViewManager treeViewManager;

  private Help helpPanel;

  private boolean isOpen = false;

  public boolean isOpen() { return isOpen; }

  private final View thisView;

  public View(Plugin plugin,PluginInterface pluginInterface, Config rssfeedConfig) {
	this.plugin = plugin;
    this.pluginInterface = pluginInterface;
    this.rssfeedConfig = rssfeedConfig;
    this.treeViewManager = new TreeViewManager(this);
    this.torrentDownloader = new TorrentDownloader(this, pluginInterface.getTorrentManager(), pluginInterface.getDownloadManager());

    scheduler = new Scheduler();
    timer = new Timer(true);
    scheduler.setView(this);
    timer.schedule(scheduler, 100L, 1000L);

    thisView = this;
  }

  public Plugin
  getPlugin()
  {
	  return( plugin );
  }
  
  @Override
  public void finalize() {
    Plugin.debugOut("Killing RSSFeed");
    timer.cancel();
  }

  public String getPluginViewName() {
    return MessageText.getString("RSSFeed.Name");
  }

  public String getFullTitle() {
    if(helpPanel != null && !helpPanel.isDisposed())
      try {
        helpPanel.load();
      } catch(Exception e) {
        System.err.println("Unable to load help contents: " + e);
      }
    return MessageText.getString("RSSFeed.Title");
  }

  private GridLayout setupGridLayout(int nc, int hs, int vs, int mh, int mw) {
    GridLayout layout = new GridLayout();
    if(nc != -1) layout.numColumns = nc;
    if(hs != -1) layout.horizontalSpacing = hs;
    if(vs != -1) layout.verticalSpacing = vs;
    if(mh != -1) layout.marginHeight = mh;
    if(mw != -1) layout.marginWidth = mw;
    return layout;
  }

  private Composite setupComposite(Composite parent, GridLayout layout, int gdsStyle) {
    return setupComposite(parent, SWT.NULL, layout, gdsStyle);
  }

  private Composite setupComposite(Composite parent, int i, GridLayout layout, int gdsStyle) {
    Composite cmp = new Composite(parent, i);
    cmp.setLayout(layout);
    cmp.setLayoutData(gdsStyle == -1?new GridData():new GridData(gdsStyle));
    return cmp;
  }

  private ToolItem setupToolItem(ToolBar parent, String icon) {
    ToolItem ti = new ToolItem(parent, SWT.PUSH);
    ti.setImage(getImage("/org/kmallan/resource/icons/" + icon));
    ti.addListener(SWT.Selection, this);
    return ti;
  }

  private Button setupBtn(Composite parent, String text) {
    Button btn = new Button(parent, SWT.PUSH);
    Messages.setLanguageText(btn, text);
    btn.addListener(SWT.Selection, this);
    return btn;
  }

  private Label setupBoldLabel(Composite parent, String text, int gdsStyle) {
    Label label = new Label(parent, SWT.NULL);
    if(gdsStyle != -1) label.setLayoutData(new GridData(gdsStyle));
    Messages.setLanguageText(label, text);
    FontData[] curFontDataFilter = label.getFont().getFontData();
    curFontDataFilter[0].setStyle(SWT.BOLD);
    curFontDataFilter[0].setHeight((int)(curFontDataFilter[0].getHeight() * 1.2));
    label.setFont(new Font((Device)display, curFontDataFilter));
    return label;
  }

  private Label setupLabel(Composite parent, String text, int wh) {
    Label lbl = new Label(parent, SWT.NULL);
    GridData layoutData = new GridData();
    layoutData.widthHint = wh;
    lbl.setLayoutData(layoutData);
    Messages.setLanguageText(lbl, text);
    return lbl;
  }

  public void initialize(final Composite parent) {
    GridLayout layout;
    GridData layoutData;

    this.display = parent.getDisplay();
    this.shell = parent.getShell();

    Composite tComp = setupComposite(parent, SWT.BORDER, setupGridLayout(-1, -1, -1, 3, 0), GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_CENTER);
    tComp.setBackground(display.getSystemColor(SWT.COLOR_LIST_SELECTION));
    tComp.setForeground(display.getSystemColor(SWT.COLOR_LIST_SELECTION_TEXT));

    Label tLabel = new Label(tComp, SWT.NULL);
    layoutData = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_CENTER);
    tLabel.setLayoutData(layoutData);
    Messages.setLanguageText(tLabel, "RSSFeed.Title");

    tLabel.setBackground(display.getSystemColor(SWT.COLOR_LIST_SELECTION));
    tLabel.setForeground(display.getSystemColor(SWT.COLOR_LIST_SELECTION_TEXT));
    FontData[] curTLabel = tLabel.getFont().getFontData();
    curTLabel[0].setStyle(SWT.BOLD);
    curTLabel[0].setHeight((int)(curTLabel[0].getHeight() * 1.2));
    tLabel.setFont(new Font((Device)display, curTLabel));

    this.tabFolder = new CTabFolder(parent, SWT.FLAT);
    tabFolder.setLayoutData(new GridData(GridData.FILL_BOTH));
    try {
      tabFolder.setMinimumCharacters(75);
    } catch(NoSuchMethodError e) {/** < SWT 3.0M8 **/}
    try {
      tabFolder.setSelectionBackground(new Color[]{display.getSystemColor(SWT.COLOR_LIST_BACKGROUND),
                                                   display.getSystemColor(SWT.COLOR_LIST_BACKGROUND),
                                                   tabFolder.getBackground()},
          new int[]{10, 90}, true);
    } catch(NoSuchMethodError e) {/** < SWT 3.0M8 **/
      tabFolder.setSelectionBackground(new Color[]{display.getSystemColor(SWT.COLOR_LIST_BACKGROUND)}, new int[0]);
    }
    tabFolder.setSelectionForeground(display.getSystemColor(SWT.COLOR_LIST_FOREGROUND));
    try {
      tabFolder.setSimple(!COConfigurationManager.getBooleanParameter("GUI_SWT_bFancyTab"));
    } catch(NoSuchMethodError e) {/** < SWT 3.0M8 **/}
    COConfigurationManager.addParameterListener("GUI_SWT_bFancyTab", this);

    this.tabStatus = new CTabItem(tabFolder, SWT.NULL);
    Messages.setLanguageText(tabStatus, "RSSFeed.Tab.Status");
    this.tabOptions = new CTabItem(tabFolder, SWT.NULL);
    Messages.setLanguageText(tabOptions, "RSSFeed.Tab.Options");
    this.tabHist = new CTabItem(tabFolder, SWT.NULL);
    Messages.setLanguageText(tabHist, "RSSFeed.Tab.History");
    this.tabHelp = new CTabItem(tabFolder, SWT.NULL);
    Messages.setLanguageText(tabHelp, "RSSFeed.Tab.Help");
    
    int selTab = pluginInterface.getPluginconfig().getPluginIntParameter( "ui.tabFolder.sel.index", 0 );
    if ( selTab >= 0 && selTab < tabFolder.getItemCount()){
    	tabFolder.setSelection(selTab);
    }else{
    	tabFolder.setSelection(tabStatus);
    }

    tabFolder.addSelectionListener(
    	new SelectionAdapter() {
    		@Override
    		public void widgetSelected(SelectionEvent e) {
    			int sel = tabFolder.getSelectionIndex();
    			pluginInterface.getPluginconfig().setPluginParameter( "ui.tabFolder.sel.index", sel );
    		}
		});
    
    // Options Folder
    this.options = setupComposite(tabFolder, setupGridLayout(2, -1, -1, -1, -1), GridData.FILL_HORIZONTAL | GridData.FILL_VERTICAL);
    tabOptions.setControl(options);

    // Options Folder - Tables
    ScrolledComposite optValTblScrollComp = new ScrolledComposite(options, SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
    layout = new GridLayout();
    layoutData = new GridData(GridData.FILL_VERTICAL);
    layoutData.widthHint = 380;
    optValTblScrollComp.setLayout(layout);
    optValTblScrollComp.setLayoutData(layoutData);
    optValTblScrollComp.setExpandHorizontal(true);
    optValTblScrollComp.setExpandVertical(true);

    Composite optValTblComp = setupComposite(optValTblScrollComp, setupGridLayout(1, 0, 0, 0, 0), GridData.FILL_BOTH);
    optValTblScrollComp.setContent(optValTblComp);

    // Options Folder - RSS Feed URLs Table
    setupBoldLabel(optValTblComp, "RSSFeed.Options.Feed.Title", GridData.VERTICAL_ALIGN_BEGINNING | GridData.FILL_HORIZONTAL);
    Composite urlComp = setupComposite(optValTblComp, setupGridLayout(2, 0, 0, 0, 0), GridData.FILL_HORIZONTAL);
    urlTable = new Table(urlComp, SWT.CHECK | SWT.SINGLE | SWT.FULL_SELECTION | SWT.BORDER);
    layoutData = new GridData(GridData.FILL_HORIZONTAL);
    layoutData.heightHint = 130;
    urlTable.setLayoutData(layoutData);
    urlTable.setHeaderVisible(true);

    String urlColumnNames = "RSSFeed.Options.Feed.Table.Col";
    int[] urlColumnWidths = {320};
    for(int i = 0; i < urlColumnWidths.length; i++) {
      TableColumn column = new TableColumn(urlTable, SWT.NULL);
      Messages.setLanguageText(column, urlColumnNames + i);
      column.setWidth(urlColumnWidths[i]);
    }
    urlTable.addMouseListener(this);

    ToolBar urlCompBar = new ToolBar(urlComp, SWT.FLAT | SWT.VERTICAL);
    btnUrlUp = setupToolItem(urlCompBar, "ItemMoveUp.gif");
    btnUrlAdd = setupToolItem(urlCompBar, "ItemAdd.gif");
    btnUrlRemove = setupToolItem(urlCompBar, "ItemRemove.gif");
    btnUrlDown = setupToolItem(urlCompBar, "ItemMoveDown.gif");

    // Options Folder - Filters Table
    setupBoldLabel(optValTblComp, "RSSFeed.Options.Filter.Title", GridData.VERTICAL_ALIGN_BEGINNING | GridData.FILL_HORIZONTAL);

    Composite filtComp = setupComposite(optValTblComp, setupGridLayout(2, 0, 0, 0, 0), GridData.FILL_VERTICAL | GridData.FILL_HORIZONTAL);
    filtTable = new Table(filtComp, SWT.CHECK | SWT.SINGLE | SWT.FULL_SELECTION | SWT.BORDER);
    layoutData = new GridData(GridData.FILL_VERTICAL | GridData.FILL_HORIZONTAL);
    filtTable.setLayoutData(layoutData);
    filtTable.setHeaderVisible(true);

    String filtColumnNames = "RSSFeed.Options.Filter.Table.Col";
    java.util.List<Integer> filtColumnWidthDefaults = Arrays.asList( 195, 65, 60 );
    
    final java.util.List<Number> filtColumnWidths = (java.util.List<Number>)pluginInterface.getPluginconfig().getPluginListParameter( "ui.filtTable.col.widths", filtColumnWidthDefaults );
    
    for(int i = 0; i < filtColumnWidths.size(); i++) {
      final TableColumn column = new TableColumn(filtTable, SWT.NULL);
      final int f_i = i;
      
      Messages.setLanguageText(column, filtColumnNames + i);
      
      column.setWidth(filtColumnWidths.get(i).intValue());

      column.addControlListener(
    		 new ControlAdapter() {
    			 @Override
    			public void controlResized(ControlEvent e) {
    				 filtColumnWidths.set( f_i , column.getWidth());
    				 pluginInterface.getPluginconfig().setPluginListParameter( "ui.filtTable.col.widths", new ArrayList<Number>( filtColumnWidths ));
    			}
    		 });      
    }
    
    filtTable.addMouseListener(this);

    ToolBar filtCompBar = new ToolBar(filtComp, SWT.FLAT | SWT.VERTICAL);
    btnFiltUp = setupToolItem(filtCompBar, "ItemMoveUp.gif");
    btnFiltAdd = setupToolItem(filtCompBar, "ItemAdd.gif");
    btnFiltCopy = setupToolItem(filtCompBar, "Copy.gif");
    btnFiltRemove = setupToolItem(filtCompBar, "ItemRemove.gif");
    btnFiltDown = setupToolItem(filtCompBar, "ItemMoveDown.gif");

    optValTblScrollComp.setMinSize(optValTblComp.computeSize(SWT.DEFAULT, SWT.DEFAULT));

    // Options Folder - Params
    optParamScrollComp = new ScrolledComposite(options, SWT.V_SCROLL | SWT.BORDER);
    layout = new GridLayout();
    layoutData = new GridData(GridData.FILL_BOTH);
    optParamScrollComp.setLayout(layout);
    optParamScrollComp.setLayoutData(layoutData);
    optParamScrollComp.setExpandHorizontal(true);
    optParamScrollComp.setExpandVertical(true);

    optParamComp = setupComposite(optParamScrollComp, setupGridLayout(1, 0, 0, 0, 0), GridData.FILL_BOTH);
    optParamScrollComp.setContent(optParamComp);

    setupBoldLabel(optParamComp, "RSSFeed.Options.Title", -1);

    // Options Folder - URL Params
    urlParamComp = setupComposite(optParamComp, setupGridLayout(2, 5, 3, 5, 5), GridData.FILL_BOTH);
    setupLabel(urlParamComp, "RSSFeed.Options.Feed.urlName", 75);
    (urlName = new Text(urlParamComp, SWT.BORDER)).setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    setupLabel(urlParamComp, "RSSFeed.Options.Feed.urlLocation", 75);
    (urlLocation = new Text(urlParamComp, SWT.BORDER)).setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    Messages.setLanguageText(new Label(urlParamComp, SWT.NULL), "RSSFeed.Options.Feed.urlStoreDir");
    Composite urlStoreDirComp = setupComposite(urlParamComp, setupGridLayout(2, 0, 0, 0, 0), GridData.FILL_HORIZONTAL);
    (urlStoreDir = new Text(urlStoreDirComp, SWT.BORDER)).setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    btnUrlStoreDir = new Button(urlStoreDirComp, SWT.PUSH);
    layoutData = new GridData();
    layoutData.widthHint = 52;
    layoutData.heightHint = 28;
    btnUrlStoreDir.setLayoutData(layoutData);
    btnUrlStoreDir.setText("...");
    btnUrlStoreDir.addListener(SWT.Selection, this);

    Messages.setLanguageText(new Label(urlParamComp, SWT.NULL), "RSSFeed.Options.Feed.urlDelay");
    (urlDelay = new Text(urlParamComp, SWT.BORDER)).setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    Messages.setLanguageText(new Label(urlParamComp, SWT.NULL), "RSSFeed.Options.Feed.Options");

    Composite urlParamCompOpt = setupComposite(urlParamComp, setupGridLayout(4, 5, 0, 0, 0), -1);

    urlObeyTTL = new Button(urlParamCompOpt, SWT.CHECK);
    Messages.setLanguageText(urlObeyTTL, "RSSFeed.Options.Feed.urlObeyTTL");
    urlLocRef = new Button(urlParamCompOpt, SWT.CHECK);
    Messages.setLanguageText(urlLocRef, "RSSFeed.Options.Feed.urlLocRef");
    urlLocRef.addListener(SWT.Selection, this);
    urlUseCookie = new Button(urlParamCompOpt, SWT.CHECK);
    Messages.setLanguageText(urlUseCookie, "RSSFeed.Options.Feed.urlUseCookie");
    urlUseCookie.addListener(SWT.Selection, this);

    Composite urlOptComp = new Composite(urlParamComp, SWT.NULL);
    layout = setupGridLayout(1, 0, 0, 0, 0);
    layoutData = new GridData(GridData.FILL_HORIZONTAL);
    layoutData.horizontalSpan = 2;
    urlOptComp.setLayout(layout);
    urlOptComp.setLayoutData(layoutData);

    // Options Folder - URL Params - Options - Cust Referer
    urlOptCompCustReferer = setupComposite(urlOptComp, setupGridLayout(3, 0, 0, 0, 0), GridData.FILL_HORIZONTAL);
    (layoutData = new GridData()).widthHint = 35;
    (new Label(urlOptCompCustReferer, SWT.NULL)).setLayoutData(layoutData);
    setupLabel(urlOptCompCustReferer, "RSSFeed.Options.Feed.urlReferer", 100);
    (urlReferer = new Text(urlOptCompCustReferer, SWT.BORDER)).setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    // Options Folder - URL Params - Options - Cookies
    urlOptCompCookie = setupComposite(urlOptComp, setupGridLayout(3, 0, 0, 0, 0), GridData.FILL_HORIZONTAL);
    (layoutData = new GridData()).widthHint = 35;
    (new Label(urlOptCompCookie, SWT.NULL)).setLayoutData(layoutData);
    setupLabel(urlOptCompCookie, "RSSFeed.Options.Feed.urlCookie", 100);
    (urlCookie = new Text(urlOptCompCookie, SWT.BORDER)).setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    // Options Folder - URL Params - Options - None
    urlOptCompNone = new Composite(urlOptComp, SWT.NULL);
    layout = setupGridLayout(1, 0, 0, 0, 0);
    layoutData = new GridData(GridData.FILL_HORIZONTAL);
    layoutData.heightHint = 1;
    urlOptCompNone.setLayout(layout);
    urlOptCompNone.setLayoutData(layoutData);
    new Label(urlOptCompNone, SWT.NULL);

    // Options Folder - URL Params - End Options
    urlChooseOptions();

    Messages.setLanguageText(new Label(urlParamComp, SWT.NULL), "RSSFeed.Options.Feed.urlActive");
    urlEnabled = new Button(urlParamComp, SWT.CHECK);
    Messages.setLanguageText(urlEnabled, "RSSFeed.Options.Feed.urlEnabled");

    new Label(urlParamComp, SWT.NULL);
    Composite urlParamCompButt = setupComposite(urlParamComp, setupGridLayout(3, 5, 0, 0, 0), -1);

    btnUrlAccept = setupBtn(urlParamCompButt, "RSSFeed.Options.Feed.btnUrlAccept");
    btnUrlReset = setupBtn(urlParamCompButt, "RSSFeed.Options.Feed.btnUrlReset");
    btnUrlCancel = setupBtn(urlParamCompButt, "RSSFeed.Options.Feed.btnUrlCancel");

    // Options Folder - Filter Params
    filtParamComp = setupComposite(optParamComp, setupGridLayout(2, 5, 3, 5, 5), GridData.FILL_BOTH);

    setupLabel(filtParamComp, "RSSFeed.Options.Filter.filtName", 75);
    (filtName = new Text(filtParamComp, SWT.BORDER)).setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    setupLabel(filtParamComp, "RSSFeed.Options.Filter.filtStoreDir", 75);
    Composite filtStoreDirComp = setupComposite(filtParamComp, setupGridLayout(2, 0, 0, 0, 0), GridData.FILL_HORIZONTAL);
    (filtStoreDir = new Text(filtStoreDirComp, SWT.BORDER)).setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    btnFiltStoreDir = new Button(filtStoreDirComp, SWT.PUSH);
    layoutData = new GridData();
    layoutData.widthHint = 52;
    layoutData.heightHint = 28;
    btnFiltStoreDir.setLayoutData(layoutData);
    btnFiltStoreDir.setText("...");
    btnFiltStoreDir.addListener(SWT.Selection, this);

    setupLabel(filtParamComp, "RSSFeed.Options.Filter.filtExpression", 75);
    Composite filtExpressionComp = setupComposite(filtParamComp, setupGridLayout(3, 0, 0, 0, 0), GridData.FILL_HORIZONTAL);
    (filtExpression = new Text(filtExpressionComp, SWT.BORDER)).setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    filtExpression.addModifyListener(new ModifyListener() {
      @Override
      public void modifyText(ModifyEvent modifyEvent) {
        if (btnFiltFileEdit != null) {
          btnFiltFileEdit.setEnabled(new File(filtExpression.getText()).exists());
        }
      }
    });
    btnFiltFileBrowse = new Button(filtExpressionComp, SWT.PUSH);
    layoutData = new GridData();
    layoutData.widthHint = 52;
    layoutData.heightHint = 28;
    btnFiltFileBrowse.setLayoutData(layoutData);
    btnFiltFileBrowse.setText("...");
    btnFiltFileBrowse.addListener(SWT.Selection, this);

    btnFiltFileEdit = new Button(filtExpressionComp, SWT.PUSH);
    layoutData = new GridData();
    layoutData.widthHint = 52;
    layoutData.heightHint = 28;
    btnFiltFileEdit.setLayoutData(layoutData);
    Messages.setLanguageText(btnFiltFileEdit, "RSSFeed.Options.Filter.Options.filtFileEdit");
    btnFiltFileEdit.addListener(SWT.Selection, this);

    setupLabel(filtParamComp, "RSSFeed.Options.Filter.Options", 75);
    Composite options = setupComposite(filtParamComp, setupGridLayout(2, 0, 0, 0, 0), -1);
    filtIsRegex = new Button(options, SWT.CHECK);
    Messages.setLanguageText(filtIsRegex, "RSSFeed.Options.Filter.Options.filtIsRegex");
    layoutData = new GridData();
    layoutData.horizontalSpan = 2;
    filtIsRegex.setLayoutData(layoutData);
    filtIsFilename = new Button(options, SWT.CHECK);
    Messages.setLanguageText(filtIsFilename, "RSSFeed.Options.Filter.Options.filtIsFilename");
    layoutData = new GridData();
    layoutData.horizontalSpan = 2;
    filtIsFilename.setLayoutData(layoutData);
    filtMatchTitle = new Button(options, SWT.CHECK);
    Messages.setLanguageText(filtMatchTitle, "RSSFeed.Options.Filter.Options.filtMatch.Title");
    filtMatchLink = new Button(options, SWT.CHECK);
    Messages.setLanguageText(filtMatchLink, "RSSFeed.Options.Filter.Options.filtMatch.Link");
    
    filtMoveTop = new Button(options, SWT.CHECK);
    Messages.setLanguageText(filtMoveTop, "RSSFeed.Options.Filter.Options.MoveTop");
    layoutData = new GridData();
    layoutData.horizontalSpan = 2;
    filtMoveTop.setLayoutData(layoutData);

    setupLabel(filtParamComp, "RSSFeed.Options.Filter.filtExclude", 130);
    (filtExclude = new Text(filtParamComp, SWT.BORDER)).setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    // min/max file size
    
    setupLabel(filtParamComp, "RSSFeed.Options.Filter.MinTorrentSize", 75);
    Composite minOpts = setupComposite(filtParamComp, setupGridLayout(2, 4, 0, 0, 0), -1);

    filtMinTorrentSize = new Text(minOpts, SWT.BORDER);
    layoutData = new GridData();
    layoutData.widthHint = 100;
    filtMinTorrentSize.setLayoutData(layoutData);
    setupLabel(minOpts, "RSSFeed.Options.Filter.TorrentSizeInfo", 200);

    setupLabel(filtParamComp, "RSSFeed.Options.Filter.MaxTorrentSize", 75);
    Composite maxOpts = setupComposite(filtParamComp, setupGridLayout(2, 4, 0, 0, 0), -1);

    filtMaxTorrentSize = new Text(maxOpts, SWT.BORDER);
    layoutData = new GridData();
    layoutData.widthHint = 100;
    filtMaxTorrentSize.setLayoutData(layoutData);
    setupLabel(maxOpts, "RSSFeed.Options.Filter.TorrentSizeInfo", 200);

    
    setupLabel(filtParamComp, "RSSFeed.Options.Filter.State", 75);
    filtState = new Combo(filtParamComp, SWT.DROP_DOWN | SWT.READ_ONLY);
    layoutData = new GridData();
    layoutData.widthHint = 200;
    filtState.setLayoutData(layoutData);
    filtState.add(MessageText.getString("RSSFeed.Options.Filter.State.Queued"));
    filtState.add(MessageText.getString("RSSFeed.Options.Filter.State.Forced"));
    filtState.add(MessageText.getString("RSSFeed.Options.Filter.State.Stopped"));
    filtState.select(0);

    setupLabel(filtParamComp, "RSSFeed.Options.Filter.Rates", 75);
    filtRateUseCustom = new Button(filtParamComp, SWT.CHECK);
    Messages.setLanguageText(filtRateUseCustom, "RSSFeed.Options.Filter.Rates.UseCustom");
    filtRateUseCustom.addListener(SWT.Selection, this);

    Composite filtRatesComp = new Composite(filtParamComp, SWT.NULL);
    layout = setupGridLayout(1, 0, 0, 0, 0);
    layoutData = new GridData(GridData.FILL_HORIZONTAL);
    layoutData.horizontalSpan = 2;
    filtRatesComp.setLayout(layout);
    filtRatesComp.setLayoutData(layoutData);

    // Options Folder - Filter Params - Rates - Custom
    filtRatesCustom = setupComposite(filtRatesComp, setupGridLayout(3, 0, 0, 0, 0), GridData.FILL_HORIZONTAL);

    (layoutData = new GridData()).widthHint = 35;
    (new Label(filtRatesCustom, SWT.NULL)).setLayoutData(layoutData);
    setupLabel(filtRatesCustom, "RSSFeed.Options.Filter.Rates.Upload", 135);
    (filtRateUpload = new Text(filtRatesCustom, SWT.BORDER)).setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    (layoutData = new GridData()).widthHint = 35;
    (new Label(filtRatesCustom, SWT.NULL)).setLayoutData(layoutData);
    setupLabel(filtRatesCustom, "RSSFeed.Options.Filter.Rates.Download", 135);
    (filtRateDownload = new Text(filtRatesCustom, SWT.BORDER)).setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    // Options Folder - Filter Params - Rates - None
    filtRatesNone = new Composite(filtRatesComp, SWT.NULL);
    layout = setupGridLayout(1, 0, 0, 0, 0);
    layoutData = new GridData(GridData.FILL_HORIZONTAL);
    layoutData.heightHint = 1;
    filtRatesNone.setLayout(layout);
    filtRatesNone.setLayoutData(layoutData);
    new Label(urlOptCompNone, SWT.NULL);

    // Options Folder - Filter Params - End Rates
    urlSetRates();

    setupLabel(filtParamComp, "RSSFeed.Options.Filter.filtCategory", 130);
    (filtCategory = new Text(filtParamComp, SWT.BORDER)).setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    setupLabel(filtParamComp, "RSSFeed.Options.Filter.filtFeed", 75);
    filtFeed = new Combo(filtParamComp, SWT.DROP_DOWN | SWT.READ_ONLY);
    layoutData = new GridData(GridData.FILL_HORIZONTAL);
    filtFeed.setLayoutData(layoutData);
    filtFeed.add(MessageText.getString("RSSFeed.Options.Filter.filtFeed.All"));
    filtFeed.select(0);

    setupLabel(filtParamComp, "RSSFeed.Options.Filter.filtType", 75);
    filtType = new Combo(filtParamComp, SWT.DROP_DOWN | SWT.READ_ONLY);
    layoutData = new GridData();
    layoutData.widthHint = 200;
    filtType.setLayoutData(layoutData);
    filtType.add(MessageText.getString("RSSFeed.Options.Filter.filtType.TVShow"));
    filtType.add(MessageText.getString("RSSFeed.Options.Filter.filtType.Movie"));
    filtType.add(MessageText.getString("RSSFeed.Options.Filter.filtType.Other"));
    filtType.add(MessageText.getString("RSSFeed.Options.Filter.filtType.None"));
    filtType.select(0);
    filtType.addModifyListener(this);

    Composite filtSpecific = new Composite(filtParamComp, SWT.NULL);
    layout = setupGridLayout(1, 0, 0, 0, 0);
    layoutData = new GridData(GridData.FILL_HORIZONTAL);
    layoutData.horizontalSpan = 2;
    filtSpecific.setLayout(layout);
    filtSpecific.setLayoutData(layoutData);

    // Options Folder - Filter Params - TVShow Specific
    filtSpecificTVShow = setupComposite(filtSpecific, setupGridLayout(3, 0, 0, 0, 0), GridData.FILL_HORIZONTAL);

    (layoutData = new GridData()).widthHint = 35;
    (new Label(filtSpecificTVShow, SWT.NULL)).setLayoutData(layoutData);
    setupLabel(filtSpecificTVShow, "RSSFeed.Options.Filter.TVShow.filtStart", 75);
    Composite season = setupComposite(filtSpecificTVShow, setupGridLayout(4, 5, 0, 0, 0), -1);

    Messages.setLanguageText(new Label(season, SWT.NULL), "RSSFeed.Options.Filter.TVShow.filtStart.Season");
    layoutData = new GridData();
    layoutData.widthHint = 24;
    (filtStartSeason = new Text(season, SWT.BORDER)).setLayoutData(layoutData);
    Messages.setLanguageText(new Label(season, SWT.NULL), "RSSFeed.Options.Filter.TVShow.filtStart.Episode");
    layoutData = new GridData();
    layoutData.widthHint = 24;
    (filtStartEpisode = new Text(season, SWT.BORDER)).setLayoutData(layoutData);

    (layoutData = new GridData()).widthHint = 35;
    (new Label(filtSpecificTVShow, SWT.NULL)).setLayoutData(layoutData);
    setupLabel(filtSpecificTVShow, "RSSFeed.Options.Filter.TVShow.filtEnd", 75);
    season = setupComposite(filtSpecificTVShow, setupGridLayout(4, 5, 0, 0, 0), -1);

    Messages.setLanguageText(new Label(season, SWT.NULL), "RSSFeed.Options.Filter.TVShow.filtEnd.Season");
    layoutData = new GridData();
    layoutData.widthHint = 24;
    (filtEndSeason = new Text(season, SWT.BORDER)).setLayoutData(layoutData);
    Messages.setLanguageText(new Label(season, SWT.NULL), "RSSFeed.Options.Filter.TVShow.filtEnd.Episode");
    layoutData = new GridData();
    layoutData.widthHint = 24;
    (filtEndEpisode = new Text(season, SWT.BORDER)).setLayoutData(layoutData);

    (layoutData = new GridData()).widthHint = 35;
    (new Label(filtSpecificTVShow, SWT.NULL)).setLayoutData(layoutData);
    setupLabel(filtSpecificTVShow, "RSSFeed.Options.Filter.TVShow.Options", 75);
    filtSmartHist = new Button(filtSpecificTVShow, SWT.CHECK);
    Messages.setLanguageText(filtSmartHist, "RSSFeed.Options.Filter.TVShow.Options.SmartHist");

    // Options Folder - Filter Params - Other Specific
    filtSpecificOther = setupComposite(filtSpecific, setupGridLayout(3, 0, 0, 0, 0), GridData.FILL_HORIZONTAL);
    (layoutData = new GridData()).widthHint = 35;
    (new Label(filtSpecificOther, SWT.NULL)).setLayoutData(layoutData);
    setupLabel(filtSpecificOther, "RSSFeed.Options.Filter.Other.Options", 75);
    filtDisable = new Button(filtSpecificOther, SWT.CHECK);
    Messages.setLanguageText(filtDisable, "RSSFeed.Options.Filter.Other.Options.Disable");

    // Options Folder - Filter Params - End Specific
    filtChooseSpecific(filtType.getSelectionIndex());

    setupLabel(filtParamComp, "RSSFeed.Options.Filter.filtActive", 75);
    filtEnabled = new Button(filtParamComp, SWT.CHECK);
    Messages.setLanguageText(filtEnabled, "RSSFeed.Options.Filter.filtEnabled");

    setupLabel(filtParamComp, "RSSFeed.Options.Filter.filtMode", 75);
    filtMode = new Combo(filtParamComp, SWT.DROP_DOWN | SWT.READ_ONLY);
    layoutData = new GridData();
    layoutData.widthHint = 200;
    filtMode.setLayoutData(layoutData);
    filtMode.add(MessageText.getString("RSSFeed.Options.Filter.filtMode.Pass"));
    filtMode.add(MessageText.getString("RSSFeed.Options.Filter.filtMode.Fail"));
    filtMode.select(0);

    setupLabel(filtParamComp, "RSSFeed.Options.Filter.filtTestMatch", 75);
    (filtTestMatch = new Text(filtParamComp, SWT.BORDER)).setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    new Label(filtParamComp, SWT.NULL);
    Composite filtParamCompButt = setupComposite(filtParamComp, setupGridLayout(4, 5, 0, 0, 0), -1);

    btnFiltAccept = setupBtn(filtParamCompButt, "RSSFeed.Options.Filter.btnFiltAccept");
    btnFiltReset = setupBtn(filtParamCompButt, "RSSFeed.Options.Filter.btnFiltReset");
    btnFiltCancel = setupBtn(filtParamCompButt, "RSSFeed.Options.Filter.btnFiltCancel");
    btnFiltTest = setupBtn(filtParamCompButt, "RSSFeed.Options.Filter.btnFiltTest");

    optParamScrollComp.setMinSize(optParamComp.computeSize(SWT.DEFAULT, SWT.DEFAULT));

    // History
    this.history = setupComposite(tabFolder, setupGridLayout(1, -1, -1, -1, -1), GridData.FILL_BOTH);
    tabHist.setControl(history);

    histTable = new Table(history, SWT.MULTI | SWT.FULL_SELECTION | SWT.BORDER);
    layoutData = new GridData(GridData.FILL_BOTH);
    histTable.setLayoutData(layoutData);
    histTable.setHeaderVisible(true);
    histTableMenu = setupHistMenu(shell);
    histTable.setMenu(histTableMenu);

    String histColumnNames = "RSSFeed.Hist.HistTable.Col";
    int[] histColumnWidths = {120, 300, 300, 160};
    for(int i = 0; i < histColumnWidths.length; i++) {
      TableColumn column = new TableColumn(histTable, SWT.NULL);
      Messages.setLanguageText(column, histColumnNames + i);
      column.setWidth(histColumnWidths[i]);
    }

    histTable.addMouseListener(this);

    // Help
    this.help = setupComposite(tabFolder, setupGridLayout(1, -1, -1, -1, -1), GridData.FILL_BOTH);
    tabHelp.setControl(help);
    helpPanel = new Help(help, SWT.VERTICAL);
    layoutData = new GridData(GridData.FILL_BOTH);
    helpPanel.setLayoutData(layoutData);

    helpPanel.setEditable(false);
    try {
      helpPanel.load();
    } catch(Exception e) {
      System.err.println("Unable to load help contents:" + e);
    }
    getConfig();

    // Status
    this.status = setupComposite(tabFolder, setupGridLayout(1, -1, -1, -1, -1), GridData.FILL_BOTH);
    tabStatus.setControl(status);


    listTable = new TableTree(status, SWT.SINGLE | SWT.FULL_SELECTION | SWT.BORDER);
    Table listTableTree = listTable.getTable();
    listTableTree.setHeaderVisible(true);
    listTableMenu = listMenu(shell);
    listTableTree.setMenu(listTableMenu);

    String columnNames = "RSSFeed.Status.ListTable.Col";
    int[] columnWidths = {350, 450, 80, 100};
    for(int i = 0; i < columnWidths.length; i++) {
      TableColumn column = new TableColumn(listTableTree, SWT.NULL);
      Messages.setLanguageText(column, columnNames + i);
      column.setWidth(columnWidths[i]);
    }

    GridData gdTable = new GridData(GridData.FILL_BOTH);
    listTable.setLayoutData(gdTable);

    listTableTree.addMouseListener(this);
    listTableTree.addSelectionListener(this);

    treeViewManager.display();
    tabFolder.pack();

    selUrlItem = null;
    selFilterItem = null;
    paramHideAll();

    urlTable.addSelectionListener(this);
    filtTable.addSelectionListener(this);

    isOpen = true;
  }

  public void delete() {
    COConfigurationManager.removeParameterListener("GUI_SWT_bFancyTab", this);

    isOpen = false;
    Composite c = getComposite();
    if ( c != null && !c.isDisposed()){
    	c.dispose();
    }
  }

  public void
  setMinTorrentSize(
	long	l )
  {
	  filtMinTorrentSize.setText( getSize( l ));
  }
  
  public long
  getMinTorrentSize()
  {
	  return( getSize( filtMinTorrentSize.getText()));
  }
  
  public void
  setMaxTorrentSize(
	long	l )
  {
	  filtMaxTorrentSize.setText( getSize( l ));
  }
  
  public long
  getMaxTorrentSize()
  {
	  return( getSize( filtMaxTorrentSize.getText()));
  }
  
  private String
  getSize(
	long	l )
  {
	  if ( l == 0 ){
		  return( "" );
	  }
	  
	  if ( l % 1024 == 0 ){
		  
		  l = l/1024;
		  
		  if ( l % 1024 == 0 ){
			  
			  l = l/1024;
			  
			  if ( l % 1024 == 0 ){
				  
				  l = l/1024;
				  
				  
				  return( l + " GB" );
			  }else{
				  
				  return( l + " MB" );
			  }
		  }else{
			  
			  return( l + " KB" );
		  }
	  }else{
		  
		  return( l + " B" );
	  }
  }
  
  private long
  getSize(
	String	text )
  {
	  text = text.trim().toLowerCase();
	  	  
	  String num 	= "";
	  String chars 	= "";
	  
	  for ( char c: text.toCharArray()){
		  
		  if ( Character.isDigit( c ) && chars.length() == 0 ){
			  
			  num += c;
		  }else{
			  
			  chars += c;
		  }
	  }
	  
	  if ( num.length() == 0 ){
		  
		  return( 0 );
	  }
	  
	  try{
		  long size = Long.parseLong( num );
	  
		  chars = chars.trim();
		  
		  if ( chars.length() > 0 ){
			  
			  char c = chars.charAt(0);
			  
			  if ( c == 'b' ){
				  
			  }else if ( c == 'k' ){
				  
				  size *= 1024;
				  
			  }else if ( c == 'm' ){
				  
				  size *= 1024*1024;
				  
			  }else if ( c == 'g' ){
				  
				  size *= 1024*1024*1024L;
				  
			  }else{
				  
			  }
		  }
		  
		  return( size );
		  
	  }catch( Throwable e ){
		  
		  Debug.out( "Invalid size: " + text );
	  }
	  
	  return( 0 );
  }
  
  public void getConfig() {
    UrlTableItem urlItem;
    FilterTableItem filterItem;
    HistoryTableItem histItem;

    for(int i = 0; i < rssfeedConfig.getUrlCount(); i++) {
      urlItem = new UrlTableItem(urlTable, rssfeedConfig);
      urlItem.setBean(i);
    }
    Utils.alternateTableBackground(urlTable);

    for(int i = 0; i < rssfeedConfig.getFilterCount(); i++) {
      filterItem = new FilterTableItem(filtTable, rssfeedConfig);
      filterItem.setBean(i);
    }
    Utils.alternateTableBackground(filtTable);

    for(int i = 0; i < rssfeedConfig.getHistoryCount(); i++) {
      histItem = new HistoryTableItem(histTable, rssfeedConfig, i);
      histItem.setBean(i);
    }
    Utils.alternateTableBackground(histTable);
  }

  private void urlOrder(int move) {
    urlOrder(move, true);
  }

  private void urlOrder(int move, boolean storeIt) {
    if(urlTable == null || urlTable.isDisposed()) return;
    if(urlTable.getSelectionCount() == 1) {
      int curPos = urlTable.getSelectionIndex();
      int newPos = curPos + move;
      if((newPos >= 0) && (newPos < urlTable.getItemCount())) {
        urlTable.setRedraw(false);
        TableItem[] items = urlTable.getItems();
        UrlTableItem curItem = (UrlTableItem)items[curPos];
        UrlTableItem newItem = (UrlTableItem)items[newPos];
        if ( move > 0 ){
	        UrlBean tmpBean = newItem.getBean();
	        newItem.setBean(curItem.getBean());
	        curItem.setBean(tmpBean);
        }else{
	        UrlBean tmpBean = curItem.getBean();
	        curItem.setBean(newItem.getBean());
	        newItem.setBean(tmpBean);
        }
        urlTable.setSelection(newPos);
        selUrlItem = newItem;
        urlTable.setRedraw(true);
      }
    }
    if(storeIt) rssfeedConfig.storeOptions();
    Utils.alternateTableBackground(urlTable);
  }

  private void urlAdd() {
    urlTable.setRedraw(false);
    UrlTableItem newItem = new UrlTableItem(urlTable, rssfeedConfig);
    UrlBean tmpBean = new UrlBean();
    tmpBean.setName("New Item");
    newItem.setBean(tmpBean);
    newItem.setup(thisView);
    int newPos = urlTable.indexOf(newItem);
    urlTable.setSelection(newPos);
    urlTable.setRedraw(true);
    selUrlItem = newItem;
    rssfeedConfig.storeOptions();
    Utils.alternateTableBackground(urlTable);
  }

  private void urlRemove() {
    int curPos = urlTable.getSelectionIndex();
    if(curPos >= 0) {
      ((UrlTableItem)(urlTable.getItem(curPos))).remove();
    }
    urlParamHide();
    rssfeedConfig.storeOptions();
    Utils.alternateTableBackground(urlTable);
  }

  private void urlAccept() {
    int curPos = urlTable.getSelectionIndex();
    if(curPos >= 0) {
      UrlTableItem urlItem = (UrlTableItem)urlTable.getItem(curPos);
      urlItem.save(thisView);
      treeViewManager.getItem(urlItem.getBean()).update();
    }
    rssfeedConfig.storeOptions();
  }

  private void urlReset() {
    int curPos = urlTable.getSelectionIndex();
    if(curPos >= 0) {
      ((UrlTableItem)(urlTable.getItem(curPos))).setup(thisView);
    }
  }

  private void urlCancel() {
    urlParamHide();
  }

  private void filtOrder(int move) {
    filtOrder(move, true);
  }

  private void filtOrder(int move, boolean storeIt) {
    if(filtTable == null || filtTable.isDisposed()) return;
    if(filtTable.getSelectionCount() == 1) {
      int curPos = filtTable.getSelectionIndex();
      int newPos = curPos + move;
      if((newPos >= 0) && (newPos < filtTable.getItemCount())) {
        filtTable.setRedraw(false);
        TableItem[] items = filtTable.getItems();
        FilterTableItem curItem = (FilterTableItem)items[curPos];
        FilterTableItem newItem = (FilterTableItem)items[newPos];
        if ( move > 0 ){
            FilterBean tmpBean = newItem.getBean();
            newItem.setBean(curItem.getBean());
            curItem.setBean(tmpBean);     
        }else{
            FilterBean tmpBean = curItem.getBean();
           	curItem.setBean(newItem.getBean());
        	newItem.setBean(tmpBean);       	
        }
        filtTable.setSelection(newPos);
        selFilterItem = newItem;
        filtTable.setRedraw(true);
      }
    }
    if(storeIt) rssfeedConfig.storeOptions();
    Utils.alternateTableBackground(filtTable);
  }

  private void filtAdd() {
    filtTable.setRedraw(false);
    FilterTableItem newItem = new FilterTableItem(filtTable, rssfeedConfig);
    FilterBean tmpBean = new FilterBean();
    tmpBean.setName("New Item");
    tmpBean.setType("TVShow");
    tmpBean.setMode("Pass");
    tmpBean.setMatchLink(true);
    tmpBean.setMatchTitle(true);
    newItem.setBean(tmpBean);
    newItem.setup(thisView);
    int curPos = filtTable.getSelectionIndex();
    if(curPos >= 0) {
      int topPos = filtTable.getTopIndex();
      int newPos = filtTable.indexOf(newItem);
      filtTable.setSelection(newPos);
      for(int iLoop = 1; iLoop <= newPos - curPos; ++iLoop) filtOrder(-1, false);
      filtTable.setTopIndex(topPos);
    } else {
      int newPos = filtTable.indexOf(newItem);
      filtTable.setSelection(newPos);
    }
    filtTable.setRedraw(true);
    selFilterItem = newItem;
    rssfeedConfig.storeOptions();
    Utils.alternateTableBackground(filtTable);
  }
  
  private void filtCopy() {
	    int curPos = filtTable.getSelectionIndex();
	    if(curPos < 0) {
	    	return;
	    }
	    FilterBean item = (FilterBean)((FilterTableItem)filtTable.getItem(curPos)).getBean();

	  
	  filtTable.setRedraw(false);
	  FilterTableItem newItem = new FilterTableItem(filtTable, rssfeedConfig);
	  FilterBean tmpBean = new FilterBean();
	  tmpBean.setName(item.getName() + " (copy)");
	  tmpBean.setType(item.getType());
	  tmpBean.setMode(item.getMode());
	  tmpBean.setMatchLink(item.getMatchLink());
	  tmpBean.setMatchTitle(item.getMatchTitle());
	  tmpBean.setCategory(item.getCategory());
	  tmpBean.setCleanFile(item.getCleanFile());
	  tmpBean.setDisableAfter(item.getDisableAfter());
	  tmpBean.setEnabled(false);	// request from http://forum.vuze.com/thread.jspa?threadID=113667
	  tmpBean.setEndEpisode(item.getEndEpisode());
	  tmpBean.setEndSeason(item.getEndSeason());
	  tmpBean.setExpression(item.getExpression());
	  tmpBean.setFeed(item.getFeed());
    tmpBean.setIsRegex(item.getIsRegex());
    tmpBean.setIsFilename(item.getIsFilename());
    tmpBean.setMoveTop(item.getMoveTop());
    tmpBean.setExclude(item.getExclude());
	  tmpBean.setRateDownload(item.getRateDownload());
	  tmpBean.setRateUseCustom(item.getRateUseCustom());
	  tmpBean.setRenameFile(item.getRenameFile());
	  tmpBean.setRenameIncEpisode(item.getRenameIncEpisode());
	  tmpBean.setStartEpisode(item.getStartEpisode());
	  tmpBean.setStartSeason(item.getStartSeason());
	  tmpBean.setState(item.getState());
	  tmpBean.setStoreDir(item.getStoreDir());
	  tmpBean.setUseSmartHistory(item.getUseSmartHistory());
	  
	  newItem.setBean(tmpBean);
	  newItem.setup(thisView);
	  
		  int topPos = filtTable.getTopIndex();
		  int newPos = filtTable.indexOf(newItem);
		  filtTable.setSelection(newPos);
		  for(int iLoop = 1; iLoop <= newPos - curPos; ++iLoop) filtOrder(-1, false);
		  filtTable.setTopIndex(topPos);

	  filtTable.setRedraw(true);
	  selFilterItem = newItem;
	  rssfeedConfig.storeOptions();
	  Utils.alternateTableBackground(filtTable);
  }

  private void filtRemove() {
    int curPos = filtTable.getSelectionIndex();
    if(curPos >= 0) {
      ((FilterTableItem)(filtTable.getItem(curPos))).remove();
    }
    filtParamHide();
    rssfeedConfig.storeOptions();
    Utils.alternateTableBackground(filtTable);
  }

  private void filtAccept() {
    int curPos = filtTable.getSelectionIndex();
    if(curPos >= 0) {
      ((FilterTableItem)(filtTable.getItem(curPos))).save(thisView);
    }
    rssfeedConfig.storeOptions();
  }

  private void filtReset() {
    int curPos = filtTable.getSelectionIndex();
    if(curPos >= 0) {
      ((FilterTableItem)(filtTable.getItem(curPos))).setup(thisView);
    }
  }

  private void filtCancel() {
    filtParamHide();
  }

  private void filtTest() {
    String testStr = filtTestMatch.getText();
    if(testStr != null) {
      testStr = testStr.toLowerCase();
      boolean match = FilterTableItem.save(null, thisView).matches(0, testStr, testStr);
      Color green = display.getSystemColor(SWT.COLOR_GREEN);
      Color red = display.getSystemColor(SWT.COLOR_RED);
      filtTestMatch.setBackground(match?green:red);
    }
  }

  public void histAdd(ListBean listBean, Download download, File file) {
    histAdd(listBean, download, file, null);
  }

  public void histAdd(ListBean listBean, Download download, File file, FilterBean filter) {
    String path = file.getPath();
    if(file.isDirectory()) {
      if(path.length() > 0 && !path.endsWith(rssfeedConfig.separator)) path = path + rssfeedConfig.separator;
      path += download.getName();
    }

    HistoryBean tmpBean = new HistoryBean();
    tmpBean.setFileData(path);
    tmpBean.setLocation(listBean.getLocation());

    if(filter != null) {
      tmpBean.setFilter(filter);
      if("TVShow".equalsIgnoreCase(filter.getType())) {
        if(!tmpBean.setSeason(listBean.getName().toLowerCase())) tmpBean.setSeason(listBean.getLocation().toLowerCase());
      } else if ("Movie".equalsIgnoreCase(filter.getType())) {
        if(!tmpBean.setMovie(listBean.getName().toLowerCase())) tmpBean.setMovie(listBean.getLocation().toLowerCase());
      }
    } // else manual download

    rssfeedConfig.addHistory(tmpBean);
    rssfeedConfig.storeOptions();

    final int histIndex = rssfeedConfig.getHistoryIndex(tmpBean);
    if(display == null || display.isDisposed() || !isOpen) return;
    display.asyncExec(new Runnable() {
      @Override
      public void run() {
        HistoryTableItem newItem = new HistoryTableItem(histTable, rssfeedConfig, histIndex);
        newItem.setBean(histIndex);
        Utils.alternateTableBackground(histTable);
      }
    });
  }

  private void histRemove() {
    TableItem[] items = histTable.getSelection();
    for(int iLoop = 0; iLoop < items.length; iLoop++) {
      ((HistoryTableItem)items[iLoop]).remove();
    }
    histTable.deselectAll();
    rssfeedConfig.storeOptions();
    Utils.alternateTableBackground(histTable);
  }

  private void updateStoreDirLoc(final Shell myShell, Text itemStoreDir) {
    DirectoryDialog dDialog = new DirectoryDialog(myShell, SWT.SYSTEM_MODAL);
    String default_path = "";
    if(itemStoreDir.getText().length() > 0) {
      default_path = itemStoreDir.getText();
    } else{
      default_path = COConfigurationManager.getStringParameter("Default save path", "");
    }
    if(default_path.length() > 0) {
      dDialog.setFilterPath(default_path);
    }
    dDialog.setText(filtName.getText() + " - Save Location");
    String savePath = dDialog.open();
    if(savePath == null) return;
    itemStoreDir.setText(savePath);
  }

  private void updateFilterFile() {
    FileDialog dDialog = new FileDialog(shell, SWT.SYSTEM_MODAL);
    final String text = filtExpression.getText();
    if(text.length() > 0) {
      if (new File(text).exists()) {
        dDialog.setFilterPath(text);
      }
    }
    dDialog.setText(filtName.getText() + " - Select Filter File");
    String savePath = dDialog.open();
    if(savePath == null) return;
    filtExpression.setText(savePath);
    filtIsFilename.setSelection(true);
  }

  private void editFilterFile() {
    Program.launch(filtExpression.getText());
  }

  public void urlParamShow() {
    filtParamHide();
    urlChooseOptions();

    if(urlParamComp.isVisible() == true) return;

    GridData layoutData = new GridData(GridData.FILL_BOTH);
    urlParamComp.setLayoutData(layoutData);
    optParamScrollComp.setMinSize(optParamComp.computeSize(SWT.DEFAULT, SWT.DEFAULT));
    optParamComp.layout(true);
    urlParamComp.setVisible(true);
  }

  public void urlParamHide() {
    if(urlParamComp.isVisible() == false) return;

    urlParamComp.setVisible(false);
    GridData layoutData = new GridData();
    layoutData.heightHint = 0;
    urlParamComp.setLayoutData(layoutData);
    optParamScrollComp.setMinSize(optParamComp.computeSize(SWT.DEFAULT, SWT.DEFAULT));
    optParamComp.layout(true);

    selUrlItem = null;
    urlTable.deselectAll();
  }

  public void filtParamShow() {
    urlParamHide();
    filtChooseSpecific(filtType.getSelectionIndex());
    urlSetRates();

    if(filtParamComp.isVisible() == true) return;

    GridData layoutData = new GridData(GridData.FILL_BOTH);
    filtParamComp.setLayoutData(layoutData);
    optParamScrollComp.setMinSize(optParamComp.computeSize(SWT.DEFAULT, SWT.DEFAULT));
    optParamComp.layout(true);
    filtParamComp.setVisible(true);
  }

  public void filtParamHide() {
    if(filtParamComp.isVisible() == false) return;

    filtParamComp.setVisible(false);
    GridData layoutData = new GridData();
    layoutData.heightHint = 0;
    filtParamComp.setLayoutData(layoutData);
    optParamScrollComp.setMinSize(optParamComp.computeSize(SWT.DEFAULT, SWT.DEFAULT));
    optParamComp.layout(true);

    selFilterItem = null;
    filtTable.deselectAll();
  }

  public void paramHideAll() {
    if(optParamComp.isVisible() == true) return;

    GridData layoutData;
    urlParamComp.setVisible(false);
    layoutData = new GridData();
    layoutData.heightHint = 0;
    urlParamComp.setLayoutData(layoutData);
    filtParamComp.setVisible(false);
    layoutData = new GridData();
    layoutData.heightHint = 0;
    filtParamComp.setLayoutData(layoutData);
    optParamScrollComp.setMinSize(optParamComp.computeSize(SWT.DEFAULT, SWT.DEFAULT));
    optParamComp.layout(true);

    selUrlItem = null;
    selFilterItem = null;
    urlTable.deselectAll();
    filtTable.deselectAll();
  }

  private void urlChooseOptions() {
    GridData layoutData;
    boolean displayNone = true;

    if(urlLocRef.getSelection()) {
      urlOptCompCustReferer.setVisible(false);
      layoutData = new GridData();
      layoutData.heightHint = 0;
      urlOptCompCustReferer.setLayoutData(layoutData);
    } else {
      layoutData = new GridData(GridData.FILL_HORIZONTAL);
      urlOptCompCustReferer.setLayoutData(layoutData);
      urlOptCompCustReferer.setVisible(true);
      displayNone = false;
    }
    if(urlUseCookie.getSelection()) {
      layoutData = new GridData(GridData.FILL_HORIZONTAL);
      urlOptCompCookie.setLayoutData(layoutData);
      urlOptCompCookie.setVisible(true);
      displayNone = false;
    } else {
      urlOptCompCookie.setVisible(false);
      layoutData = new GridData();
      layoutData.heightHint = 0;
      urlOptCompCookie.setLayoutData(layoutData);
    }
    if(displayNone) {
      layoutData = new GridData(GridData.FILL_HORIZONTAL);
      layoutData.heightHint = 1;
      urlOptCompNone.setLayoutData(layoutData);
      urlOptCompNone.setVisible(true);
    } else {
      urlOptCompNone.setVisible(false);
      layoutData = new GridData();
      layoutData.heightHint = 0;
      urlOptCompNone.setLayoutData(layoutData);
    }

    optParamScrollComp.setMinSize(optParamComp.computeSize(SWT.DEFAULT, SWT.DEFAULT));
    urlParamComp.layout(true);
  }

  private void urlSetRates() {
    GridData layoutData;

    if(filtRateUseCustom.getSelection()) {
      layoutData = new GridData(GridData.FILL_HORIZONTAL);
      filtRatesCustom.setLayoutData(layoutData);
      filtRatesCustom.setVisible(true);
      filtRatesNone.setVisible(false);
      layoutData = new GridData();
      layoutData.heightHint = 0;
      filtRatesNone.setLayoutData(layoutData);
    } else {
      filtRatesCustom.setVisible(false);
      layoutData = new GridData();
      layoutData.heightHint = 0;
      filtRatesCustom.setLayoutData(layoutData);
      layoutData = new GridData(GridData.FILL_HORIZONTAL);
      layoutData.heightHint = 1;
      filtRatesNone.setLayoutData(layoutData);
      filtRatesNone.setVisible(true);
    }

    optParamScrollComp.setMinSize(optParamComp.computeSize(SWT.DEFAULT, SWT.DEFAULT));
    filtParamComp.layout(true);
  }

  private void filtChooseSpecific(int curType) {
    GridData layoutData;

    switch(curType) {
      case 0:
        filtSpecificOther.setVisible(false);
        layoutData = new GridData();
        layoutData.heightHint = 0;
        filtSpecificOther.setLayoutData(layoutData);
        layoutData = new GridData(GridData.FILL_HORIZONTAL);
        filtSpecificTVShow.setLayoutData(layoutData);
        filtSpecificTVShow.setVisible(true);
        break;
      case 1:
        filtSpecificTVShow.setVisible(false);
        layoutData = new GridData();
        layoutData.heightHint = 0;
        filtSpecificTVShow.setLayoutData(layoutData);
        layoutData = new GridData(GridData.FILL_HORIZONTAL);
        filtSpecificOther.setLayoutData(layoutData);
        filtSpecificOther.setVisible(true);
        break;
      case 2:
        filtSpecificTVShow.setVisible(false);
        layoutData = new GridData();
        layoutData.heightHint = 0;
        filtSpecificTVShow.setLayoutData(layoutData);
        filtSpecificOther.setVisible(false);
        layoutData = new GridData();
        layoutData.heightHint = 0;
        filtSpecificOther.setLayoutData(layoutData);
      case 3:
        filtSpecificTVShow.setVisible(false);
        layoutData = new GridData();
        layoutData.heightHint = 0;
        filtSpecificTVShow.setLayoutData(layoutData);
        filtSpecificOther.setVisible(false);
        layoutData = new GridData();
        layoutData.heightHint = 0;
        filtSpecificOther.setLayoutData(layoutData);
    }

    optParamScrollComp.setMinSize(optParamComp.computeSize(SWT.DEFAULT, SWT.DEFAULT));
    filtParamComp.layout(true);
  }

  private Image getImage(String res) {
    return getImage(res, 255);
  }

  private Image getImage(String res, int alpha) {
    Image image;
    InputStream stream = getClass().getResourceAsStream(res);
    if(stream != null) {
      if(alpha == 255) {
        image = new Image(display, stream);
      } else {
        ImageData imageData = new ImageData(stream);
        imageData.alpha = alpha;
        image = new Image(display, imageData);
      }
      return image;
    } else {
      System.err.println("RSSFeed: Error loading resource: " + res);
    }
    return null;
  }

  private Menu listMenu(final Shell shell) {
    listTableMenu = new Menu(shell, SWT.POP_UP);

    itemRefresh = setupListMenuItem("Refresh", "Refresh.gif");
    listTableMenu.setDefaultItem(itemRefresh);
    itemRefreshAll = setupListMenuItem("RefreshAll", "Refresh.gif");
    new MenuItem(listTableMenu, SWT.SEPARATOR);
    itemExpandAll = setupListMenuItem("Expand", "ItemAdd.gif");
    itemCollapseAll = setupListMenuItem("Collapse", "ItemRemove.gif");
    new MenuItem(listTableMenu, SWT.SEPARATOR);
    itemDownload = setupListMenuItem("Download", "Download.gif");
    itemDownloadTo = setupListMenuItem("DownloadTo", "DownloadTo.gif");
    itemCancel = setupListMenuItem("Cancel", "Cancel.gif");
    new MenuItem(listTableMenu, SWT.SEPARATOR);
    itemCreateFilter = setupListMenuItem("FilterFrom", "Filter.gif");
    itemCopyLink = setupListMenuItem("CopyLink", "Copy.gif");
    itemOpenLink = setupListMenuItem("OpenLink", "Copy.gif");
    new MenuItem(listTableMenu, SWT.SEPARATOR);
    itemShowInfo = setupListMenuItem("ShowInfo", "ShowInfo.gif");
    listTableMenu.addMenuListener(this);

    return listTableMenu;
  }

  private MenuItem setupListMenuItem(String strName, String iconName) {
    MenuItem item = new MenuItem(listTableMenu, SWT.PUSH);
    Messages.setLanguageText(item, "RSSFeed.Status.ListTable.Menu." + strName);
    item.setImage(getImage("/org/kmallan/resource/icons/" + iconName));
    item.addSelectionListener(this);
    return item;
  }

  private Menu setupHistMenu(final Shell shell) {
    histTableMenu = new Menu(shell, SWT.POP_UP);
    itemCopyFile = setupHistMenuItem("CopyFile", "Copy.gif");
    histTableMenu.setDefaultItem(itemCopyFile);
    itemCopyTorrent = setupHistMenuItem("CopyTorrent", "Copy.gif");
    new MenuItem(histTableMenu, SWT.SEPARATOR);
    itemDelete = setupHistMenuItem("Delete", "Remove.gif");
    histTableMenu.addMenuListener(this);
    return histTableMenu;
  }

  private MenuItem setupHistMenuItem(String strName, String iconName) {
    MenuItem item = new MenuItem(histTableMenu, SWT.PUSH);
    Messages.setLanguageText(item, "RSSFeed.Hist.HistTable.Menu." + strName);
    item.setImage(getImage("/org/kmallan/resource/icons/" + iconName));
    item.addSelectionListener(this);
    return item;
  }

  public Composite getComposite() {
    return this.tabFolder;
  }

  @Override
  public void mouseDoubleClick(MouseEvent event) {
    Object src = event.getSource();
    Point pMousePosition = new Point(event.x, event.y);

    if(src == listTable.getTable()) {
      Rectangle rTableArea = listTable.getClientArea();
      if(rTableArea.contains(pMousePosition)) {
        if(listTable == null || listTable.isDisposed()) return;
        TableTreeItem[] items = listTable.getSelection();
        if(items.length == 1) {
          final ListTreeItem listItem = (ListTreeItem)items[0];
          if(listItem.isFeed()) {
            listItem.setExpanded(!listItem.getExpanded());
          } else {
            new Thread("ManualFetcher") {
              @Override
              public void run() {
                ListBean listBean = (ListBean)listItem.getBean();
                torrentDownloader.addTorrent(listBean);
                if(isOpen() && display != null && !display.isDisposed())
                  display.asyncExec(new Runnable() {
                    @Override
                    public void run() {listItem.update();}
                  });
              }
            }.start();
          }
        }
      }
    }

  }

  @Override
  public void mouseDown(MouseEvent event) {
    Object src = event.getSource();
    Point pMousePosition = new Point(event.x, event.y);

    if(src == urlTable) {
      Rectangle rTableArea = urlTable.getClientArea();
      if(rTableArea.contains(pMousePosition)) {
        UrlTableItem urlItem = (UrlTableItem)urlTable.getItem(pMousePosition);
        if(urlItem == null) urlParamHide();
      }

    } else if(src == filtTable) {
      Rectangle rTableArea = filtTable.getClientArea();
      if(rTableArea.contains(pMousePosition)) {
        FilterTableItem filterItem = (FilterTableItem)filtTable.getItem(pMousePosition);
        if(filterItem == null) filtParamHide();
      }

    } else if(src == listTable.getTable()) {
      Rectangle rTableArea = listTable.getClientArea();
      if(rTableArea.contains(pMousePosition)) {
        // Use 2 because of inconsistent behaviour of getItem
        // when table is scrolled right.
        pMousePosition.x = 2;
        TableTreeItem listItem = listTable.getItem(pMousePosition);
        if(listItem == null) {
          listTable.deselectAll();
          selListItem = null;
        }
      }
    }

  }

  @Override
  public void mouseUp(MouseEvent event) {
    Object src = event.getSource();
    Point pMousePosition = new Point(event.x, event.y);

    if(src == urlTable) {
      Rectangle rTableArea = urlTable.getClientArea();
      if(rTableArea.contains(pMousePosition)) {
        UrlTableItem urlItem = (UrlTableItem)urlTable.getItem(pMousePosition);
        if(urlItem == null) return;

        UrlBean urlBean = urlItem.getBean();
        if(urlBean.isEnabled() != urlItem.getChecked()) {
          urlBean.setEnabled(urlItem.getChecked());
          rssfeedConfig.storeOptions();
          if(urlItem.equals(selUrlItem)) urlItem.setup(thisView);
        }
      }

    } else if(src == filtTable) {

      Rectangle rTableArea = filtTable.getClientArea();
      if(rTableArea.contains(pMousePosition)) {
        FilterTableItem filterItem = (FilterTableItem)filtTable.getItem(pMousePosition);
        if(filterItem == null) return;

        FilterBean filterBean = filterItem.getBean();
        if(filterBean.getEnabled() != filterItem.getChecked()) {
          filterBean.setEnabled(filterItem.getChecked());
          rssfeedConfig.storeOptions();
          if(selFilterItem.equals(filterItem)) filterItem.setup(thisView);
        }
      }

    } else if(src == histTable) {

      Rectangle rTableArea = histTable.getClientArea();
      if(rTableArea.contains(pMousePosition)) {
        // Use 2 because of inconsistent behaviour of getItem when table is scrolled right.
        pMousePosition.x = 2;
        HistoryTableItem histItem = (HistoryTableItem)histTable.getItem(pMousePosition);
        if(histItem == null) histTable.deselectAll();
      }
    }
  }

  @Override
  public void widgetSelected(SelectionEvent event) {
    Object src = event.getSource();

    // history table
    TableItem[] histItems = histTable.getSelection();
    if(src == itemDelete) {
      histRemove();
    } else if(src == itemCopyTorrent) {
      if(histItems.length == 1) {
        CBManager clipboard = new CBManager();
        clipboard.setClipboardContents(((HistoryTableItem)histItems[0]).getBean().getLocation());
      }
    } else if(src == itemCopyFile) {
      if(histItems.length == 1) {
        CBManager clipboard = new CBManager();
        clipboard.setClipboardContents(((HistoryTableItem)histItems[0]).getBean().getFileData());
      }
    }

    // list table
    if(src == itemRefresh) {
      UrlBean urlBean;
      if(selListItem.isFeed()) {
        urlBean = (UrlBean)selListItem.getBean();
      } else {
        urlBean = ((ListBean)selListItem.getBean()).getFeed();
      }
      urlBean.refreshGroup();

    } else if(src == itemRefreshAll) {
      for(int iLoop = 0; iLoop < rssfeedConfig.getUrlCount(); iLoop++) {
        UrlBean urlBean = rssfeedConfig.getUrl(iLoop);
        if(urlBean.isEnabled()) urlBean.refreshGroup();
      }

    } else if(src == itemDownload) {
      if(!selListItem.isFeed()) {
        torrentDownloader.addTorrentThreaded((ListBean)selListItem.getBean());
      }

    } else if(src == itemCancel) {
      if(!selListItem.isFeed()) {
        ListBean listBean = (ListBean)selListItem.getBean();
        if(listBean.downloader == null) return;
        listBean.downloader.cancel();
      }

    } else if(src == itemCopyLink) {
      CBManager clipboard = new CBManager();
      if(!selListItem.isFeed()) {
        ListBean listBean = (ListBean)selListItem.getBean();
        clipboard.setClipboardContents(listBean.getLocation());
      } else {
        UrlBean urlBean = (UrlBean)selListItem.getBean();
        clipboard.setClipboardContents(urlBean.getLocation());
      }

    } else if(src == itemOpenLink) {
      if(!selListItem.isFeed()) {
        ListBean listBean = (ListBean)selListItem.getBean();
        Plugin.launchUrl(listBean.getLocation());
      } else {
        UrlBean urlBean = (UrlBean)selListItem.getBean();
        Plugin.launchUrl(urlBean.getBaseURL());
      }

    } else if(src == itemExpandAll) {
      treeViewManager.expandAll();

    } else if(src == itemCollapseAll) {
      treeViewManager.collapseAll();

    } else if(src == itemShowInfo) {
      //
      ListBean lb = (ListBean)selListItem.getBean();
      Plugin.debugOut("showInfo for " + lb.getName() + " - " + lb.getDescription() + " - " + lb.getInfo());
      //
      SimpleDialog dialog = new SimpleDialog(shell);
      dialog.setText(((ListBean)selListItem.getBean()).getName());
      dialog.setTitle(MessageText.getString("RSSFeed.Status.ListTable.Menu.ShowInfo.Dialog.Title"));
      dialog.setInfo(((ListBean)selListItem.getBean()).getDescription());
      dialog.open();

    } else if(src == itemCreateFilter) {
      if(!selListItem.isFeed()) {
        ListBean listBean = (ListBean)selListItem.getBean();
        UrlBean urlBean = listBean.getFeed();

        filtTable.setRedraw(false);
        FilterTableItem newItem = new FilterTableItem(filtTable, rssfeedConfig);
        FilterBean newBean = new FilterBean();
        Episode e = FilterBean.getSeason(listBean.getName());
        if(e == null && listBean.getLocation().toLowerCase().endsWith(".torrent"))
          e = FilterBean.getSeason(listBean.getLocation());

        newBean.setName(listBean.getName());
        newBean.setType("Other");
        newBean.setMode("Pass");
        newBean.setFeed(urlBean.getID());
        if(e != null) {
          newBean.setName(e.showTitle);
          newBean.setExpression((e.showTitle.toLowerCase()).replaceAll(" ", ".").replaceAll("\\(", ".").replaceAll("\\)", "."));
          newBean.setIsRegex(true);
          newBean.setMatchTitle(true);
          newBean.setMatchLink(true);
          newBean.setType("TVShow");
          newBean.setStartSeason(e.seasonStart);
          newBean.setStartEpisode(e.episodeStart);
        }
        newItem.setBean(newBean);
        newItem.setup(thisView);

        int newPos = filtTable.indexOf(newItem);
        filtTable.setSelection(newPos);
        filtTable.setRedraw(true);
        selFilterItem = newItem;
        rssfeedConfig.storeOptions();

        tabFolder.setSelection(tabOptions);
        filtParamShow();
        filtTestMatch.setText(listBean.getName());
      }

    } else if(src == itemDownloadTo) {
      if(!selListItem.isFeed()) {
        downloadToThreaded(); // todo
      }

    } else if(src == listTable.getTable()) {
      if(listTable == null || listTable.isDisposed()) return;
      TableTreeItem[] items = listTable.getSelection();
      if(items.length == 1) {
        ListTreeItem listItem = (ListTreeItem)items[0];
        if(selListItem == null || listItem != selListItem) selListItem = listItem;
      }

    } else if(src == urlTable) {
      if(urlTable == null || urlTable.isDisposed()) return;
      TableItem[] items = urlTable.getSelection();
      if(items.length == 1) {
        UrlTableItem urlItem = (UrlTableItem)items[0];
        if(selUrlItem == null || urlItem != selUrlItem) {
          selUrlItem = urlItem;
          selUrlItem.setup(thisView);
        }

        UrlBean urlBean = selUrlItem.getBean();
        if(urlBean.isEnabled() != selUrlItem.getChecked()) {
          urlBean.setEnabled(selUrlItem.getChecked());
          rssfeedConfig.storeOptions();
          selUrlItem.setup(thisView);
        }
      } else {
        urlParamHide();
      }

    } else if(src == filtTable) {
      if(filtTable == null || filtTable.isDisposed()) return;
      TableItem[] items = filtTable.getSelection();
      if(items.length == 1) {
        FilterTableItem filtItem = (FilterTableItem)items[0];
        if(selFilterItem == null || filtItem != selFilterItem) {
          selFilterItem = filtItem;
          selFilterItem.setup(thisView);
        }

        FilterBean filtBean = selFilterItem.getBean();
        if(filtBean.getEnabled() != selFilterItem.getChecked()) {
          filtBean.setEnabled(selFilterItem.getChecked());
          rssfeedConfig.storeOptions();
          selFilterItem.setup(thisView);
        }
      } else {
        filtParamHide();
      }
    }

  }

  private void downloadToThreaded() {
    Thread t = new Thread() {
      @Override
      public void run() {
        boolean success = false;
        File torrentLocation = null;

        final ListBean listBean = (ListBean)selListItem.getBean();
        UrlBean urlBean = listBean.getFeed();

        String default_path = new String("");
        if(urlBean.getStoreDir().length() > 0) {
          default_path = urlBean.getStoreDir();
        } else{
          default_path = COConfigurationManager.getStringParameter("Default save path", "");
        }

        String link = listBean.getLocation();

        boolean saveTorrents = false;
        String torrentDirectory = new String("");
        try {
          saveTorrents = COConfigurationManager.getBooleanParameter("Save Torrent Files", true);
          torrentDirectory = COConfigurationManager.getDirectoryParameter("General_sDefaultTorrent_Directory");
        } catch(Exception egnore) {}
        if(!saveTorrents || torrentDirectory == null || torrentDirectory.length() == 0) return;

        try {
          torrentLocation = torrentDownloader.getTorrent(link, urlBean, listBean, torrentDirectory);
          if(torrentLocation == null) return;
          Torrent curTorrent = pluginInterface.getTorrentManager().createFromBEncodedFile(torrentLocation);

          final boolean singleFile = (curTorrent.getFiles()).length == 1;
          final String singleFileName = curTorrent.getName();
          final String defaultPath = default_path;

          String storeLoc = null, storeFile = null;
          if(isOpen() && display != null && !display.isDisposed()) {
            Thread t = new Thread() {
              @Override
              public void run() {
                String loc = null;
                try {
                  if(singleFile) {
                    FileDialog fDialog = new FileDialog(shell, SWT.SYSTEM_MODAL | SWT.SAVE);
                    if(defaultPath.length() > 0) {
                      fDialog.setFilterPath(defaultPath);
                    }
                    fDialog.setFileName(singleFileName);
                    fDialog.setText(MessageText.getString("MainWindow.dialog.choose.savepath") + " (" + singleFileName + ")");
                    loc = fDialog.open();
                  } else {
                    DirectoryDialog dDialog = new DirectoryDialog(shell, SWT.SYSTEM_MODAL);
                    if(defaultPath.length() > 0) {
                      dDialog.setFilterPath(defaultPath);
                    }
                    dDialog.setText(MessageText.getString("MainWindow.dialog.choose.savepath") + " (" + singleFileName + ")");
                    loc = dDialog.open();
                  }
                } catch(Exception e) {
                  listBean.setState(ListBean.DOWNLOAD_FAIL);
                  listBean.setError(e.getMessage());
                }
                if(loc == null) loc = new String("");
                setName(loc);
              }
            };
            display.asyncExec(t);
            int counter = 0;
            do {
              try {
                long numMillisecondsToSleep = 1000;
                sleep(numMillisecondsToSleep);
              } catch(InterruptedException e) {
              }
            } while(t.getName().matches("Thread-[0-9]+") && counter++ < 30);
            if(counter >= 30) throw new Exception("Save Timeout (30s)");
            storeLoc = t.getName();
          }
          if(storeLoc != null && storeLoc.length() != 0) {
            if(singleFile) {
              String[] slp = storeLoc.split(rssfeedConfig.separator);
              storeFile = slp[slp.length - 1];
              storeLoc = storeLoc.substring(0, storeLoc.lastIndexOf(rssfeedConfig.separator));
            }
            success = torrentDownloader.addTorrent(curTorrent, torrentLocation, listBean, storeLoc, storeFile, null );
            if(!success) listBean.setState(ListBean.DOWNLOAD_FAIL);
          }
        } catch(Exception e) {
          listBean.setState(ListBean.DOWNLOAD_FAIL);
          listBean.setError(e.getMessage());
        }
        if(isOpen() && display != null && !display.isDisposed())
          display.asyncExec(new Runnable() {
            @Override
            public void run() {
              selListItem.update();
            }
          });

        if(!success && torrentLocation != null) torrentLocation.delete();
      }
    };
    t.start();
  }

  @Override
  public void widgetDefaultSelected(SelectionEvent event) {

  }

  @Override
  public void menuHidden(MenuEvent event) {}

  @Override
  public void menuShown(MenuEvent event) {
    Object src = event.getSource();

    if(src == histTableMenu) {
      TableItem[] items = histTable.getSelection();
      if(items.length == 1) {
        itemCopyFile.setEnabled(true);
        itemCopyTorrent.setEnabled(true);
        itemDelete.setEnabled(true);
      } else if(items.length > 1) {
        itemCopyFile.setEnabled(false);
        itemCopyTorrent.setEnabled(false);
        itemDelete.setEnabled(true);
      } else {
        itemCopyFile.setEnabled(false);
        itemCopyTorrent.setEnabled(false);
        itemDelete.setEnabled(false);
      }
    } else if(src == listTableMenu) {
      itemRefreshAll.setEnabled(true);
      if(selListItem == null) {
        itemRefresh.setEnabled(false);
        itemDownload.setEnabled(false);
        itemDownloadTo.setEnabled(false);
        itemCancel.setEnabled(false);
        itemCreateFilter.setEnabled(false);
        itemCopyLink.setEnabled(false);
        itemOpenLink.setEnabled(false);
        itemShowInfo.setEnabled(false);
      } else {
        itemRefresh.setEnabled(true);
        if(selListItem.isFeed()) {
          itemDownload.setEnabled(false);
          itemDownloadTo.setEnabled(false);
          itemCancel.setEnabled(false);
          itemCreateFilter.setEnabled(false);
          itemCopyLink.setEnabled(true);
          itemOpenLink.setEnabled(true);
          itemShowInfo.setEnabled(false);
        } else {
          itemDownload.setEnabled(true);
          itemDownloadTo.setEnabled(true);
          ListBean listBean = (ListBean)selListItem.getBean();
          if(listBean.getState() != ListBean.NO_DOWNLOAD && !listBean.checkDone()) {
            itemCancel.setEnabled(true);
          } else {
            itemCancel.setEnabled(false);
          }
          itemCreateFilter.setEnabled(true);
          itemCopyLink.setEnabled(true);
          itemOpenLink.setEnabled(true);
          if(((ListBean)selListItem.getBean()).getDescription().equalsIgnoreCase("")) {
            itemShowInfo.setEnabled(false);
          } else {
            itemShowInfo.setEnabled(true);
          }
        }
      }

    }

  }

  @Override
  public void handleEvent(Event event) {
    Widget src = event.widget;

    if(src == btnUrlUp) {
      if(selUrlItem == null) return;
      urlOrder(-1);
    } else if(src == btnUrlAdd) {
      urlAdd();
    } else if(src == btnUrlRemove) {
      if(selUrlItem == null) return;
      urlRemove();
    } else if(src == btnUrlDown) {
      if(selUrlItem == null) return;
      urlOrder(1);
    } else if(src == btnFiltUp) {
      if(selFilterItem == null) return;
      filtOrder(-1);
    } else if(src == btnFiltAdd) {
      filtAdd();
    } else if(src == btnFiltCopy) {
    	filtCopy();
    } else if(src == btnFiltRemove) {
      if(selFilterItem == null) return;
      filtRemove();
    } else if(src == btnFiltDown) {
      if(selFilterItem == null) return;
      filtOrder(1);
    } else if(src == btnUrlStoreDir) {
      updateStoreDirLoc(shell, urlStoreDir);
    } else if(src == urlLocRef || src == urlUseCookie) {
      urlChooseOptions();
    } else if(src == btnUrlAccept) {
      urlAccept();
    } else if(src == btnUrlReset) {
      urlReset();
    } else if(src == btnUrlCancel) {
      urlCancel();
    } else if(src == btnFiltStoreDir) {
      updateStoreDirLoc(shell, filtStoreDir);
    } else if(src == btnFiltFileBrowse) {
      updateFilterFile();
    } else if(src == btnFiltFileEdit) {
      editFilterFile();
    } else if(src == filtRateUseCustom) {
      urlSetRates();
    } else if(src == btnFiltAccept) {
      filtAccept();
    } else if(src == btnFiltReset) {
      filtReset();
    } else if(src == btnFiltCancel) {
      filtCancel();
    } else if(src == btnFiltTest) {
      filtTest();
    }

  }

  @Override
  public void modifyText(ModifyEvent event) {
    Object src = event.getSource();

    if(src == filtType) {
      filtChooseSpecific(filtType.getSelectionIndex());
    }
  }

  @Override
  public void parameterChanged(String parameterName) {
    if(parameterName.equalsIgnoreCase("GUI_SWT_bFancyTab")
        && tabFolder instanceof CTabFolder && tabFolder != null
        && !tabFolder.isDisposed()) {
      try {
        tabFolder.setSimple(!COConfigurationManager.getBooleanParameter("GUI_SWT_bFancyTab"));
      } catch(NoSuchMethodError e) {/** < SWT 3.0M8 **/}
    }

  }
}


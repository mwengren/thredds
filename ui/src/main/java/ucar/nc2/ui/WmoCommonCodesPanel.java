package ucar.nc2.ui;

import ucar.nc2.ui.widget.BAMutil;
import ucar.nc2.ui.widget.FileManager;
import ucar.nc2.ui.widget.IndependentWindow;
import ucar.nc2.ui.widget.TextHistoryPane;
import ucar.nc2.wmo.CommonCodeTable;
import ucar.util.prefs.PreferencesExt;
import ucar.util.prefs.ui.BeanTableSorted;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * WMO Common Codes
 *
 * @author caron
 * @since Aug 25, 2010
 */
public class WmoCommonCodesPanel extends JPanel {
  private PreferencesExt prefs;

  private BeanTableSorted codeTable, entryTable;
  private JSplitPane split, split2;

  private TextHistoryPane compareTA;
  private IndependentWindow infoWindow;

  private FileManager fileChooser;

  public WmoCommonCodesPanel(final PreferencesExt prefs, JPanel buttPanel) {
    this.prefs = prefs;

    codeTable = new BeanTableSorted(TableBean.class, (PreferencesExt) prefs.node("CodeTableBean"), false);
    codeTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        TableBean csb = (TableBean) codeTable.getSelectedBean();
        CommonCodeTable cct = CommonCodeTable.getTable(csb.t.getTableNo());
        setEntries(cct);
      }
    });

    entryTable = new BeanTableSorted(EntryBean.class, (PreferencesExt) prefs.node("EntryBean"), false);
    entryTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        EntryBean csb = (EntryBean) entryTable.getSelectedBean();
      }
    });

    /* thredds.ui.PopupMenu varPopup = new thredds.ui.PopupMenu(codeTable.getJTable(), "Options");
    varPopup.addAction("Show uses", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Formatter out = new Formatter();
        CodeTableBean csb = (CodeTableBean) codeTable.getSelectedBean();
        if (usedDds != null) {
          List<Message> list = usedDds.get(csb.getId());
          if (list != null) {
            for (Message use : list)
              use.dumpHeaderShort(out);
          }
        }
        compareTA.setText(out.toString());
        compareTA.gotoTop();
        infoWindow.setVisible(true);
      }
    });

    AbstractButton compareButton = BAMutil.makeButtcon("Select", "Compare to current table", false);
    compareButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        compareToCurrent();
      }
    });
    buttPanel.add(compareButton);

    AbstractButton dupButton = BAMutil.makeButtcon("Select", "Look for problems in WMO table", false);
    dupButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        lookForProblems();
      }
    });
    buttPanel.add(dupButton);

    AbstractButton modelsButton = BAMutil.makeButtcon("Select", "Check current models", false);
    modelsButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        try {
          checkCurrentModels();
        } catch (IOException e1) {
          e1.printStackTrace();
        }
      }
    });
    buttPanel.add(modelsButton);   */

    // the info window
    compareTA = new TextHistoryPane();
    infoWindow = new IndependentWindow("Extra Information", BAMutil.getImage("netcdfUI"), compareTA);
    infoWindow.setBounds((Rectangle) prefs.getBean("InfoWindowBounds", new Rectangle(300, 300, 800, 600)));

    split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, codeTable, entryTable);
    split.setDividerLocation(prefs.getInt("splitPos", 500));

    setLayout(new BorderLayout());
    add(split, BorderLayout.CENTER);

    ///

    try {
      List<TableBean> tables = new ArrayList<TableBean>();
      for (CommonCodeTable.Table t : CommonCodeTable.Table.values()) {
        tables.add(new TableBean(t));
      }
      codeTable.setBeans(tables);

    } catch (Exception e) {
      e.printStackTrace();
    }

  }

  public void save() {
    codeTable.saveState(false);
    entryTable.saveState(false);
    prefs.putBeanObject("InfoWindowBounds", infoWindow.getBounds());
    //prefs.putBeanObject("InfoWindowBounds2", infoWindow2.getBounds());
    prefs.putInt("splitPos", split.getDividerLocation());
    //prefs.putInt("splitPos2", split2.getDividerLocation());
    if (fileChooser != null) fileChooser.save();
  }

  public void setEntries(CommonCodeTable codeTable) {
    List<EntryBean> beans = new ArrayList<EntryBean>(codeTable.entries.size());
    for (CommonCodeTable.TableEntry d : codeTable.entries) {
      beans.add(new EntryBean(d));
    }
    entryTable.setBeans(beans);
  }

  /* private void lookForProblems() {
    int total = 0;
    int dups = 0;

    HashMap<String, GribCodeTable.TableEntry> paramSet = new HashMap<String, GribCodeTable.TableEntry>();
    Formatter f = new Formatter();
    f.format("Duplicates in WMO parameter table%n");
    for (Object t : codeTable.getBeans()) {
      GribCodeTable gt = ((CodeTableBean) t).code;
      if (!gt.isParameter) continue;
      for (GribCodeTable.TableEntry p : gt.entries) {
        if (p.meaning.equalsIgnoreCase("Reserved")) continue;
        if (p.meaning.equalsIgnoreCase("Missing")) continue;
        if (p.start != p.stop) continue;

        GribCodeTable.TableEntry pdup = paramSet.get(p.name);
        if (pdup != null) {
          f.format("Duplicate %s%n", p);
          f.format("          %s%n%n", pdup);
          dups++;
        } else {
          paramSet.put(p.name, p);
        }
        total++;
      }
    }
    f.format("%nTotal=%d dups=%d%n%n", total, dups);

    total = 0;
    dups = 0;
    f.format("() in WMO parameter table%n");
    for (Object t : codeTable.getBeans()) {
      GribCodeTable gt = ((CodeTableBean) t).code;
      if (!gt.isParameter) continue;
      for (GribCodeTable.TableEntry p : gt.entries) {
        if (p.meaning.indexOf('(') > 0) {
          f.format("  %s:%n  org='%s'%n name='%s' %n%n", p.getId(), p.meaning, p.name);
          dups++;
        }
        total++;
      }
    }
    f.format("%nTotal=%d parens=%d%n%n", total, dups);

    total = 0;
    dups = 0;
    f.format("invalid units in WMO parameter table%n");
    for (Object t : codeTable.getBeans()) {
      GribCodeTable gt = ((CodeTableBean) t).code;
      if (!gt.isParameter) continue;
      for (GribCodeTable.TableEntry p : gt.entries) {
        if (p.unit == null) continue;
        if (p.unit.length() == 0) continue;
        try {
          SimpleUnit su = SimpleUnit.factoryWithExceptions(p.unit);
          if (su.isUnknownUnit())
            f.format("%s UNKNOWN %s%n", p.getId(), p.unit);
        } catch (Exception ioe) {
          f.format("%s FAIL %s%n", p.getId(), p.unit);
        }
        total++;
      }
    }
    f.format("%nTotal=%d parens=%d%n%n", total, dups);

    compareTA.setText(f.toString());
    infoWindow.show();
  }


  private boolean showSame = false, showCase = false, showUnknown = false;
  private void compareToCurrent() {
    int total = 0;
    int nsame = 0;
    int nsameIgn = 0;
    int ndiff = 0;
    int unknownCount = 0;

    Formatter f = new Formatter();
    f.format("DIFFERENCES with current parameter table%n");
    List tables = codeTable.getBeans();
    for (Object t : tables) {
      GribCodeTable gt = ((CodeTableBean) t).code;
      if (!gt.isParameter) continue;
      for (GribCodeTable.TableEntry p : gt.entries) {
        if (p.meaning.equalsIgnoreCase("Reserved")) continue;
        if (p.meaning.equalsIgnoreCase("Missing")) continue;
        if (p.start != p.stop) continue;

        GridParameter gp = ParameterTable.getParameter(gt.discipline, gt.category, p.start);
        String paramDesc = gp.getDescription();
        boolean unknown = paramDesc.startsWith("Unknown");
        if (unknown) unknownCount++;
        boolean same = paramDesc.equals(p.name);
        if (same) nsame++;
        boolean sameIgnore = paramDesc.equalsIgnoreCase(p.name);
        if (sameIgnore) nsameIgn++;
        else ndiff++;
        total++;

        String unitsCurr = gp.getUnit();
        String unitsWmo = p.unit;
        boolean sameUnits = (unitsWmo == null) ? (unitsCurr == null) : unitsWmo.equals(unitsCurr);
        same = same && sameUnits;

        if (unknown && !showUnknown) continue;
        if (same && !showSame) continue;
        if (sameIgnore && !showCase) continue;

        String state = same ? "  " : (sameIgnore ? "* " : "**");
        f.format("%s%d %d %d (%d)%n wmo =%s%n curr=%s%n", state, gt.discipline, gt.category, p.start, p.line, p.name, paramDesc);
        if (!sameUnits) f.format(" units wmo='%s' curr='%s' %n", unitsWmo, unitsCurr);
      }
    }
    f.format("%nTotal=%d same=%d sameIgnoreCase=%d dif=%d unknown=%d%n", total, nsame, nsameIgn, ndiff, unknownCount);
    compareTA.setText(f.toString());
    infoWindow.show();
  }

  private char[] remove = new char[]{'(', ')', ' ', '"', ',', '*', '-'};
  private String[] replace = new String[]{"", "", "", "", "", "", ""};

  private boolean equiv(String org1, String org2) {
    String s1 = StringUtil.replace(org1, remove, replace).toLowerCase();
    String s2 = StringUtil.replace(org2, remove, replace).toLowerCase();
    return s1.equals(s2);
  }

  private boolean equivUnits(String unitS1, String unitS2) {
    String lower1 = unitS1.toLowerCase();
    String lower2 = unitS2.toLowerCase();
    if (lower1.equals(lower2)) return true;
    if (lower1.startsWith("code") && lower2.startsWith("code")) return true;
    if (lower1.startsWith("flag") && lower2.startsWith("flag")) return true;
    if (unitS1.startsWith("CCITT") && unitS2.startsWith("CCITT")) return true;

    try {
      return SimpleUnit.isCompatibleWithExceptions(unitS1, unitS2);

    } catch (Exception e) {
      return equiv(unitS1, unitS2);
    }
  }

  private void checkCurrentModels() throws IOException {
    int total = 0;
    int nsame = 0;
    int nsameIgn = 0;
    int ndiff = 0;
    int unknownCount = 0;

    String dirName = "F:/data/cdmUnitTest/tds/normal";

    Formatter fm = new Formatter();
    fm.format("Check Current Models in directory %s%n", dirName);
    File allDir = new File(dirName);
    File[] allFiles = allDir.listFiles();
    List<File> flist = Arrays.asList(allFiles);
    Collections.sort(flist);

    for (File f : flist) {
      String name = f.getAbsolutePath();
      if (f.isDirectory()) continue;
      if (!name.endsWith(".grib2")) continue;
      fm.format("Check file %s%n", name);

      GridDataset ncfile = null;
      try {
        ncfile = GridDataset.open(name);
        for (GridDatatype dt : ncfile.getGrids()) {
          String currName = dt.getName().toLowerCase();
          Attribute att = dt.findAttributeIgnoreCase("GRIB_param_id");
          int discipline = (Integer) att.getValue(1);
          int category = (Integer) att.getValue(2);
          int number = (Integer) att.getValue(3);
          if (number >= 192) continue;
          
          GribCodeTable.TableEntry entry = GribCodeTable.getEntry(discipline, category, number);
          if (entry == null) {
            fm.format("%n%d %d %d CANT FIND %s%n", discipline, category, number, currName);
            continue;
          }

          String wmoName = entry.name.toLowerCase();
          boolean same = currName.startsWith(wmoName);
          if (same) nsame++;
          else ndiff++;
          total++;

          /* String unitsCurr = dt.findAttributeIgnoreCase("units").getStringValue();
          String unitsWmo = entry.unit;
          boolean sameUnits = (unitsWmo == null) ? (unitsCurr == null) : unitsWmo.equals(unitsCurr);
          same = same && sameUnits; //

          if (same && !showSame) continue;

          fm.format("%d %d %d%n wmo =%s%n curr=%s%n", discipline, category, number, wmoName, currName);
          //if (!sameUnits) fm.format(" units wmo='%s' curr='%s' %n", unitsWmo, unitsCurr);

        }
      } finally {
        if (ncfile != null) ncfile.close();
      }
    }

    fm.format("%nTotal=%d same=%d sameIgnoreCase=%d dif=%d unknown=%d%n", total, nsame, nsameIgn, ndiff, unknownCount);
    compareTA.setText(fm.toString());
    infoWindow.show();
  } */

  public class TableBean {
    CommonCodeTable.Table t;

    // no-arg constructor
    public TableBean() {
    }

    // create from a dataset
    public TableBean(CommonCodeTable.Table t) {
      this.t = t;
    }

    public String getName() {
      return t.getName();
    }

    public String getEnumName() {
      return t.name();
    }

    public int getType() {
      return t.getTableType();
    }

    public String getResource() {
      return t.getResourceName();
    }

  }

  public class EntryBean {
    CommonCodeTable.TableEntry te;

    // no-arg constructor
    public EntryBean() {
    }

    // create from a dataset
    public EntryBean(CommonCodeTable.TableEntry te) {
      this.te = te;
    }

    public String getValue() {
      return te.value;
    }

    public String getComment() {
      return te.comment;
    }

    public String getStatus() {
      return te.status;
    }

    public int getCode() {
      return te.code;
    }

    public int getCode2() {
      return te.code2;
    }

    public int getLine() {
      return te.line;
    }
  }
}
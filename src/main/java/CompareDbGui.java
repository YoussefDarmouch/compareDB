import javax.swing.*;
import javax.swing.border.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;

import services.DbConnectionFactory;

public class CompareDbGui extends JFrame {

    // ── Palette ──────────────────────────────────────────────────────────────
    private static final Color BG_PAGE    = new Color(245, 245, 243);
    private static final Color BG_CARD    = Color.WHITE;
    private static final Color BG_OUTPUT  = new Color(28, 28, 30);
    private static final Color BORDER_CLR = new Color(0, 0, 0, 30);
    private static final Color TEXT_PRI   = new Color(18, 18, 18);
    private static final Color TEXT_SEC   = new Color(110, 110, 110);
    private static final Color TEXT_TER   = new Color(160, 160, 160);
    private static final Color ACCENT_B   = new Color(56, 138, 221);   // DB1 badge
    private static final Color ACCENT_G   = new Color(31, 158, 117);   // DB2 badge
    private static final Color CHIP_IDLE  = new Color(240, 240, 238);
    private static final Color CHIP_IDLEB = new Color(200, 200, 198);
    private static final Color CHIP_ON    = new Color(219, 234, 254);
    private static final Color CHIP_ONT   = new Color(24, 95, 165);
    private static final Color CHIP_ONB   = new Color(147, 197, 253);
    private static final Color OUT_OK     = new Color(74, 222, 128);
    private static final Color OUT_DIFF   = new Color(248, 113, 113);
    private static final Color OUT_HEAD   = new Color(250, 250, 250);
    private static final Color OUT_DEFAULT= new Color(200, 200, 200);
    private static final Color BTN_PRI_BG = new Color(18, 18, 18);
    private static final Color BTN_PRI_FG = Color.WHITE;
    private static final Color BTN_DNG_BG = new Color(254, 226, 226);
    private static final Color BTN_DNG_FG = new Color(185, 28, 28);
    private static final Color BTN_DNG_B  = new Color(252, 165, 165);
    private static final Color STATUS_RUN = new Color(245, 158, 11);
    private static final Color STATUS_OK  = new Color(34, 197, 94);
    private static final Color STATUS_IDL = new Color(180, 180, 178);

    // ── Fonts ─────────────────────────────────────────────────────────────────
    private static final Font FONT_TITLE   = new Font("Segoe UI", Font.PLAIN, 20);
    private static final Font FONT_SUB     = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font FONT_SECTION = new Font("Segoe UI", Font.BOLD,  11);
    private static final Font FONT_LABEL   = new Font("Segoe UI", Font.PLAIN, 12);
    private static final Font FONT_INPUT   = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font FONT_CHIP    = new Font("Segoe UI", Font.PLAIN, 12);
    private static final Font FONT_BTN     = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font FONT_BADGE   = new Font("Segoe UI", Font.BOLD,  11);
    private static final Font FONT_DB_HDR  = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font FONT_MONO    = new Font("Consolas",  Font.PLAIN, 12);
    private static final Font FONT_STATUS  = new Font("Segoe UI", Font.PLAIN, 12);

    // ── Inputs – DB 1 ────────────────────────────────────────────────────────
    private final JTextField     db1NameField = styledField(18);
    private final JTextField     db1HostField = styledField(14);
    private final JTextField     db1PortField = styledField(6);
    private final JTextField     db1UserField = styledField(12);
    private final JPasswordField db1PassField = styledPass(12);

    // ── Inputs – DB 2 ────────────────────────────────────────────────────────
    private final JTextField     db2NameField = styledField(18);
    private final JTextField     db2HostField = styledField(14);
    private final JTextField     db2PortField = styledField(6);
    private final JTextField     db2UserField = styledField(12);
    private final JPasswordField db2PassField = styledPass(12);

    // ── Scope chips ──────────────────────────────────────────────────────────
    private final ChipButton chipTables     = new ChipButton("Tables");
    private final ChipButton chipColumns    = new ChipButton("Columns");
    private final ChipButton chipData       = new ChipButton("Data");
    private final ChipButton chipTypes      = new ChipButton("Types");
    private final ChipButton chipFunctions  = new ChipButton("Functions");
    private final ChipButton chipProcedures = new ChipButton("Procedures");
    private final ChipButton chipTriggers   = new ChipButton("Triggers");
    private final ChipButton chipAll        = new ChipButton("All");

    // ── Actions ───────────────────────────────────────────────────────────────
    private final FlatButton runBtn   = new FlatButton("Run comparison",  BTN_PRI_BG, BTN_PRI_FG, BTN_PRI_BG);
    private final FlatButton clearBtn = new FlatButton("Clear output",    BTN_DNG_BG, BTN_DNG_FG, BTN_DNG_B);

    // ── Output ────────────────────────────────────────────────────────────────
    private final JTextPane  outputPane  = new JTextPane();
    private final StatusDot  statusDot   = new StatusDot(STATUS_IDL);
    private final JLabel     statusLabel = new JLabel("Ready");
    private final JLabel     tsLabel     = new JLabel();

    // ─────────────────────────────────────────────────────────────────────────

    public CompareDbGui() {
        DbConnectionFactory.DbConfig def = DbConnectionFactory.getDefaultConfig("default_db");
        db1HostField.setText(def.getHost());
        db1PortField.setText(def.getPort());
        db1UserField.setText(def.getUser());
        db2HostField.setText(def.getHost());
        db2PortField.setText(def.getPort());
        db2UserField.setText(def.getUser());

        setTitle("Database Comparator");
        setIconImage(DbLogoIcon.createImage(64));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1020, 720));
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG_PAGE);
        root.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.setOpaque(false);

        inner.add(buildHeader());
        inner.add(Box.createVerticalStrut(16));
        inner.add(buildConnectionCard());
        inner.add(Box.createVerticalStrut(12));
        inner.add(buildOutputCard());

        root.add(inner, BorderLayout.CENTER);
        setContentPane(root);
        wireEvents();
    }

    // ── Header ────────────────────────────────────────────────────────────────

    private JPanel buildHeader() {
        JPanel p = new JPanel(new BorderLayout(14, 0));
        p.setOpaque(false);
        p.setAlignmentX(LEFT_ALIGNMENT);

        p.add(new DbLogoIcon(), BorderLayout.WEST);

        JPanel text = new JPanel();
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.setOpaque(false);

        JLabel title = new JLabel("Database comparator");
        title.setFont(FONT_TITLE);
        title.setForeground(TEXT_PRI);

        JLabel sub = new JLabel("Compare tables, columns, data, types, functions, procedures and triggers between two MySQL databases");
        sub.setFont(FONT_SUB);
        sub.setForeground(TEXT_SEC);

        text.add(Box.createVerticalGlue());
        text.add(title);
        text.add(Box.createVerticalStrut(3));
        text.add(sub);
        text.add(Box.createVerticalGlue());

        p.add(text, BorderLayout.CENTER);
        return p;
    }

    // ── Connection card ───────────────────────────────────────────────────────

    private JPanel buildConnectionCard() {
        JPanel card = card();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        card.add(sectionLabel("Connection settings"));
        card.add(Box.createVerticalStrut(12));

        JPanel cols = new JPanel(new GridLayout(1, 2, 20, 0));
        cols.setOpaque(false);
        cols.add(buildDbColumn(1));
        cols.add(buildDbColumn(2));
        card.add(cols);

        card.add(Box.createVerticalStrut(16));
        card.add(separator());
        card.add(Box.createVerticalStrut(14));

        card.add(sectionLabel("Comparison scope"));
        card.add(Box.createVerticalStrut(10));

        JPanel chipsWrapper = new JPanel(new BorderLayout());
        chipsWrapper.setOpaque(false);
        chipsWrapper.setAlignmentX(LEFT_ALIGNMENT);
        chipsWrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        chipsWrapper.add(buildChipsRow(), BorderLayout.WEST);
        card.add(chipsWrapper);

        card.add(Box.createVerticalStrut(12));
        card.add(separator());
        card.add(Box.createVerticalStrut(12));

        JPanel actionsWrapper = new JPanel(new BorderLayout());
        actionsWrapper.setOpaque(false);
        actionsWrapper.setAlignmentX(LEFT_ALIGNMENT);
        actionsWrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        actionsWrapper.add(buildActionsRow(), BorderLayout.WEST);
        card.add(actionsWrapper);

        return card;
    }

    private JPanel buildDbColumn(int num) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);

        // Header row with badge
        JPanel hdrRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        hdrRow.setOpaque(false);

        JLabel dbLabel = new JLabel(num == 1 ? "Source " : "Target ");
        dbLabel.setFont(FONT_DB_HDR);
        dbLabel.setForeground(TEXT_PRI);

        BadgeLabel badge = new BadgeLabel(
                num == 1 ? "DB 1" : "DB 2",
                num == 1 ? ACCENT_B : ACCENT_G
        );

        hdrRow.add(dbLabel);
        hdrRow.add(badge);
        hdrRow.setAlignmentX(LEFT_ALIGNMENT);
        p.add(hdrRow);
        p.add(Box.createVerticalStrut(10));

        JTextField nameF = num == 1 ? db1NameField : db2NameField;
        JTextField hostF = num == 1 ? db1HostField : db2HostField;
        JTextField portF = num == 1 ? db1PortField : db2PortField;
        JTextField userF = num == 1 ? db1UserField : db2UserField;
        JPasswordField passF = num == 1 ? db1PassField : db2PassField;

        p.add(fieldRow("Database", nameF, null, 0));
        p.add(Box.createVerticalStrut(7));
        p.add(fieldRow("Host", hostF, portF, 70));
        p.add(Box.createVerticalStrut(7));
        p.add(fieldRow("User", userF, null, 0));
        p.add(Box.createVerticalStrut(7));
        p.add(fieldRow("Password", passF, null, 0));

        return p;
    }

    /** labelWidth==0 → no port field */
    private JPanel fieldRow(String labelText, JComponent mainField, JTextField portField, int portWidth) {
        JPanel row = new JPanel(new BorderLayout(0, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        row.setAlignmentX(LEFT_ALIGNMENT);

        JLabel lbl = new JLabel(labelText);
        lbl.setFont(FONT_LABEL);
        lbl.setForeground(TEXT_SEC);
        lbl.setPreferredSize(new Dimension(62, 30));
        row.add(lbl, BorderLayout.WEST);

        if (portField != null) {
            JPanel right = new JPanel(new BorderLayout(6, 0));
            right.setOpaque(false);
            portField.setPreferredSize(new Dimension(portWidth, 30));
            portField.setMaximumSize(new Dimension(portWidth, 30));
            right.add(mainField, BorderLayout.CENTER);
            right.add(portField, BorderLayout.EAST);
            row.add(right, BorderLayout.CENTER);
        } else {
            row.add(mainField, BorderLayout.CENTER);
        }
        return row;
    }

    private JPanel buildChipsRow() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));
        p.setOpaque(false);
        p.setAlignmentX(LEFT_ALIGNMENT);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        ChipButton[] chips = {chipTables, chipColumns, chipData, chipTypes,
                chipFunctions, chipProcedures, chipTriggers, chipAll};
        for (int i = 0; i < chips.length; i++) {
            if (i > 0) p.add(Box.createHorizontalStrut(6));
            p.add(chips[i]);
        }
        p.add(Box.createHorizontalGlue());
        return p;
    }

    private JPanel buildActionsRow() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));
        p.setOpaque(false);
        p.setAlignmentX(LEFT_ALIGNMENT);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        p.add(runBtn);
        p.add(Box.createHorizontalStrut(8));
        p.add(clearBtn);
        p.add(Box.createHorizontalGlue());
        return p;
    }

    // ── Output card ───────────────────────────────────────────────────────────

    private JPanel buildOutputCard() {
        JPanel card = card();
        card.setLayout(new BorderLayout(0, 10));

        card.add(sectionLabel("Output"), BorderLayout.NORTH);

        outputPane.setEditable(false);
        outputPane.setFont(FONT_MONO);
        outputPane.setBackground(BG_OUTPUT);
        outputPane.setForeground(OUT_DEFAULT);
        outputPane.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        outputPane.setText("Waiting for comparison to run…");

        JScrollPane scroll = new JScrollPane(outputPane);
        scroll.setBorder(new RoundedBorder(8, BG_OUTPUT));
        scroll.setBackground(BG_OUTPUT);
        scroll.getViewport().setBackground(BG_OUTPUT);
        scroll.setPreferredSize(new Dimension(0, 260));
        card.add(scroll, BorderLayout.CENTER);

        card.add(buildStatusBar(), BorderLayout.SOUTH);
        return card;
    }

    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        left.setOpaque(false);
        statusLabel.setFont(FONT_STATUS);
        statusLabel.setForeground(TEXT_SEC);
        left.add(statusDot);
        left.add(statusLabel);

        tsLabel.setFont(FONT_STATUS);
        tsLabel.setForeground(TEXT_TER);

        bar.add(left, BorderLayout.WEST);
        bar.add(tsLabel, BorderLayout.EAST);
        return bar;
    }

    // ── Events ────────────────────────────────────────────────────────────────

    private void wireEvents() {
        chipAll.addActionListener(e -> {
            boolean on = chipAll.isOn();
            for (ChipButton c : new ChipButton[]{
                    chipTables, chipColumns, chipData, chipTypes,
                    chipFunctions, chipProcedures, chipTriggers}) {
                c.setOn(on);
            }
        });

        clearBtn.addActionListener(e -> {
            outputPane.setText("");
            statusDot.setColor(STATUS_IDL);
            statusLabel.setText("Output cleared");
            tsLabel.setText("");
        });

        runBtn.addActionListener(e -> runComparison());
    }

    // ── Comparison logic (unchanged from original) ────────────────────────────

    private void runComparison() {
        String db1 = db1NameField.getText().trim();
        String db2 = db2NameField.getText().trim();
        if (!validateInputs(db1, db2)) return;

        DbConnectionFactory.DbConfig cfg1 = buildDbConfig(db1, db1HostField, db1PortField, db1UserField, db1PassField);
        DbConnectionFactory.DbConfig cfg2 = buildDbConfig(db2, db2HostField, db2PortField, db2UserField, db2PassField);
        if (cfg1 == null || cfg2 == null) return;

        Set<String> tables = getSelectedTablesForComparison(cfg1, cfg2);
        if (tables == null) { statusLabel.setText("Comparison canceled"); return; }

        runBtn.setEnabled(false);
        statusDot.setColor(STATUS_RUN);
        statusLabel.setText("Running comparison…");

        final DbConnectionFactory.DbConfig f1 = cfg1, f2 = cfg2;
        final Set<String> ft = tables;

        new SwingWorker<String, Void>() {
            @Override protected String doInBackground() {
                return captureComparisonOutput(f1, f2, ft);
            }
            @Override protected void done() {
                try {
                    String out = get();
                    renderColoredOutput(out.trim().isEmpty() ? "No output generated." : out);
                    statusDot.setColor(STATUS_OK);
                    statusLabel.setText("Comparison completed");
                    tsLabel.setText(new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date()));
                } catch (Exception ex) {
                    renderColoredOutput("Error: " + ex.getMessage());
                    statusDot.setColor(new Color(239, 68, 68));
                    statusLabel.setText("Comparison failed");
                } finally {
                    runBtn.setEnabled(true);
                }
            }
        }.execute();
    }

    private DbConnectionFactory.DbConfig buildDbConfig(String name, JTextField host,
            JTextField port, JTextField user, JPasswordField pass) {
        String h = host.getText().trim(), pt = port.getText().trim(), u = user.getText().trim();
        if (h.isEmpty() || pt.isEmpty() || u.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Host, port, and user are required for both databases.",
                "Missing connection info", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        DbConnectionFactory.DbConfig def = DbConnectionFactory.getDefaultConfig(name);
        return new DbConnectionFactory.DbConfig(name, h, pt, u, new String(pass.getPassword()), def.getParams());
    }

    private boolean validateInputs(String db1, String db2) {
        if (db1.isEmpty() || db2.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter both database names.",
                "Missing input", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        if (!hasSelectedOption()) {
            JOptionPane.showMessageDialog(this, "Please select at least one comparison option.",
                "No option selected", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        return true;
    }

    private boolean hasSelectedOption() {
        return chipTables.isOn() || chipColumns.isOn() || chipData.isOn() || chipTypes.isOn()
            || chipFunctions.isOn() || chipProcedures.isOn() || chipTriggers.isOn();
    }

    private Set<String> getSelectedTablesForComparison(DbConnectionFactory.DbConfig c1,
            DbConnectionFactory.DbConfig c2) {
        if (!chipColumns.isOn() && !chipData.isOn() && !chipTypes.isOn()) return Collections.emptySet();
        return promptTableSelection(c1, c2);
    }

    private String captureComparisonOutput(DbConnectionFactory.DbConfig c1,
            DbConnectionFactory.DbConfig c2, Set<String> tables) {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        PrintStream orig = System.out;
        PrintStream cap;
        try { cap = new PrintStream(buf, true, "UTF-8"); }
        catch (UnsupportedEncodingException e) { cap = new PrintStream(buf, true); }
        try {
            System.setOut(cap);
            runSelectedComparisons(c1, c2, tables);
        } finally { cap.flush(); System.setOut(orig); }
        return new String(buf.toByteArray(), StandardCharsets.UTF_8);
    }

    private void runSelectedComparisons(DbConnectionFactory.DbConfig c1,
            DbConnectionFactory.DbConfig c2, Set<String> tables) {
        if (chipTables.isOn())     { System.out.println("\n========== COMPARE TABLES ==========");     CompareTablesDb.compareTables(c1, c2); }
        if (chipColumns.isOn())    { System.out.println("\n========== COMPARE COLUMNS ==========");    CompareDataService.compareColumns(c1, c2, tables); }
        if (chipData.isOn())       { System.out.println("\n========== COMPARE DATA ==========");       CompareDataService.compareData(c1, c2, tables); }
        if (chipTypes.isOn())      { System.out.println("\n========== COMPARE TYPES ==========");      CompareDataService.compareTypes(c1, c2, tables); }
        if (chipFunctions.isOn())  { System.out.println("\n========== COMPARE FUNCTIONS ==========");  CompareFuncDB.CompareFunc(c1, c2); }
        if (chipProcedures.isOn()) { System.out.println("\n========== COMPARE PROCEDURES =========="); CompareFuncDB.compareProcedures(c1, c2); }
        if (chipTriggers.isOn())   { System.out.println("\n========== COMPARE TRIGGERS ==========");   CompareFuncDB.compareTriggers(c1, c2); }
    }

    private Set<String> promptTableSelection(DbConnectionFactory.DbConfig c1, DbConnectionFactory.DbConfig c2) {
        List<String> shared = CompareDataService.getSharedTablesBetweenDbs(c1, c2);
        if (shared.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No common tables found between the selected databases.",
                "No common tables", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        JList<String> list = new JList<>(shared.toArray(new String[0]));
        list.setVisibleRowCount(Math.min(12, shared.size()));
        list.setSelectionInterval(0, shared.size() - 1);
        JScrollPane sp = new JScrollPane(list);
        sp.setPreferredSize(new Dimension(320, 220));
        int opt = JOptionPane.showConfirmDialog(this, sp, "Select tables to compare",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (opt != JOptionPane.OK_OPTION) return null;
        List<String> sel = list.getSelectedValuesList();
        if (sel.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select at least one table.",
                "No table selected", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        return new HashSet<>(sel);
    }

    // ── Output rendering ──────────────────────────────────────────────────────

    private void renderColoredOutput(String output) {
        StyledDocument doc = outputPane.getStyledDocument();
        try {
            doc.remove(0, doc.getLength());
            String marker = "========== COMPARE ";
            int first = output.indexOf(marker);
            if (first < 0) { appendStyled(doc, output, OUT_DEFAULT); return; }
            if (first > 0) appendStyled(doc, output.substring(0, first), OUT_DEFAULT);
            int start = first;
            while (start >= 0) {
                int next = output.indexOf(marker, start + marker.length());
                String section = next < 0 ? output.substring(start) : output.substring(start, next);
                appendStyled(doc, section, sectionColor(section));
                start = next;
            }
        } catch (BadLocationException ex) {
            outputPane.setText(output);
        }
    }

    private void appendStyled(StyledDocument doc, String text, Color color) throws BadLocationException {
        SimpleAttributeSet attr = new SimpleAttributeSet();
        StyleConstants.setForeground(attr, color);
        StyleConstants.setFontFamily(attr, "Consolas");
        StyleConstants.setFontSize(attr, 12);
        doc.insertString(doc.getLength(), text, attr);
    }

    private Color sectionColor(String section) {
        String s = section.toLowerCase();
        if (s.contains("exists only in") || s.contains(" differs") || s.contains("difference in")
                || s.contains("missing in") || s.contains("comparison completed with differences")
                || s.contains("output check failed") || s.contains("could not read")
                || s.contains("error:") || s.contains("comparison failed")) return OUT_DIFF;
        if (s.contains("no ") && s.contains("differences found")) return OUT_OK;
        return OUT_HEAD;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static JPanel card() {
        JPanel p = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_CARD);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));
                g2.setColor(BORDER_CLR);
                g2.setStroke(new BasicStroke(1f));
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 12, 12));
                g2.dispose();
            }
        };
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
        p.setAlignmentX(LEFT_ALIGNMENT);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        return p;
    }

    private static JLabel sectionLabel(String text) {
        JLabel l = new JLabel(text.toUpperCase());
        l.setFont(FONT_SECTION);
        l.setForeground(TEXT_TER);
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }

    private static JSeparator separator() {
        JSeparator sep = new JSeparator();
        sep.setForeground(BORDER_CLR);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        return sep;
    }

    private static JTextField styledField(int cols) {
        JTextField f = new JTextField(cols) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(246, 246, 244));
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 7, 7));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        f.setFont(FONT_INPUT);
        f.setForeground(TEXT_PRI);
        f.setCaretColor(TEXT_PRI);
        f.setBorder(new RoundedBorder(7, new Color(0, 0, 0, 30)));
        f.setOpaque(false);
        f.setPreferredSize(new Dimension(f.getPreferredSize().width, 30));
        return f;
    }

    private static JPasswordField styledPass(int cols) {
        JPasswordField f = new JPasswordField(cols) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(246, 246, 244));
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 7, 7));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        f.setFont(FONT_INPUT);
        f.setForeground(TEXT_PRI);
        f.setCaretColor(TEXT_PRI);
        f.setBorder(new RoundedBorder(7, new Color(0, 0, 0, 30)));
        f.setOpaque(false);
        f.setPreferredSize(new Dimension(f.getPreferredSize().width, 30));
        return f;
    }

    // ── Entry point ───────────────────────────────────────────────────────────

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
            catch (Exception ignored) {}
            new CompareDbGui().setVisible(true);
        });
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Inner components
    // ═════════════════════════════════════════════════════════════════════════

    /** Pill-shaped toggle chip with checkmark when active */
    static class ChipButton extends JComponent {
        private boolean on = false;
        private final String label;
        private final List<ActionListener> listeners = new ArrayList<>();

        ChipButton(String label) {
            this.label = label;
            setFont(FONT_CHIP);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            FontMetrics fm = getFontMetrics(FONT_CHIP);
            int w = fm.stringWidth(label) + 36; // extra room for tick
            setPreferredSize(new Dimension(w, 28));
            setMinimumSize(new Dimension(w, 28));
            setMaximumSize(new Dimension(w, 28));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    on = !on;
                    repaint();
                    ActionEvent ae = new ActionEvent(ChipButton.this, ActionEvent.ACTION_PERFORMED, label);
                    for (ActionListener l : listeners) l.actionPerformed(ae);
                }
                @Override public void mouseEntered(MouseEvent e) { repaint(); }
                @Override public void mouseExited(MouseEvent e)  { repaint(); }
            });
        }

        public boolean isOn() { return on; }
        public void setOn(boolean v) { on = v; repaint(); }
        public void addActionListener(ActionListener l) { listeners.add(l); }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            Color bg  = on ? CHIP_ON   : CHIP_IDLE;
            Color brd = on ? CHIP_ONB  : CHIP_IDLEB;
            Color fg  = on ? CHIP_ONT  : TEXT_SEC;

            // pill background
            g2.setColor(bg);
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), getHeight(), getHeight()));

            // pill border
            g2.setColor(brd);
            g2.setStroke(new BasicStroke(1f));
            g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, getHeight(), getHeight()));

            int cx = 10; // left padding for checkmark area

            if (on) {
                // draw tick ✓
                g2.setColor(CHIP_ONT);
                g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int mid = getHeight() / 2;
                g2.drawLine(cx - 3, mid,     cx,     mid + 4);
                g2.drawLine(cx,     mid + 4, cx + 5, mid - 3);
                cx += 9; // shift label right to make room for tick
            } else {
                cx = 12;
            }

            // label text
            g2.setColor(fg);
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
            g2.drawString(label, cx, y);

            g2.dispose();
        }
    }

    /** Flat rounded button */
    static class FlatButton extends JButton {
        private final Color bg, fg, border;

        FlatButton(String text, Color bg, Color fg, Color border) {
            super(text);
            this.bg = bg; this.fg = fg; this.border = border;
            setFont(FONT_BTN);
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            FontMetrics fm = getFontMetrics(FONT_BTN);
            setPreferredSize(new Dimension(fm.stringWidth(text) + 32, 34));
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color paintBg = isEnabled()
                ? (getModel().isPressed()
                    ? bg.darker()
                    : (getModel().isRollover() ? bg.brighter() : bg))
                : new Color(200, 200, 200);
            g2.setColor(paintBg);
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 8, 8));
            g2.setColor(border);
            g2.setStroke(new BasicStroke(1f));
            g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth()-1, getHeight()-1, 8, 8));
            g2.setColor(isEnabled() ? fg : Color.GRAY);
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            int x = (getWidth()  - fm.stringWidth(getText())) / 2;
            int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
            g2.drawString(getText(), x, y);
            g2.dispose();
        }
    }

    /** Small colored badge label */
    static class BadgeLabel extends JComponent {
        private final String text;
        private final Color accent;
        BadgeLabel(String text, Color accent) {
            this.text = text; this.accent = accent;
            setFont(FONT_BADGE);
            FontMetrics fm = getFontMetrics(FONT_BADGE);
            setPreferredSize(new Dimension(fm.stringWidth(text) + 16, 18));
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color bgC = new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 30);
            g2.setColor(bgC);
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), getHeight(), getHeight()));
            g2.setColor(accent);
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            int x = (getWidth()  - fm.stringWidth(text)) / 2;
            int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
            g2.drawString(text, x, y);
            g2.dispose();
        }
    }

    /** Animated status dot */
    static class StatusDot extends JComponent {
        private Color color;
        StatusDot(Color color) {
            this.color = color;
            setPreferredSize(new Dimension(9, 9));
        }
        void setColor(Color c) { this.color = c; repaint(); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.fillOval(0, (getHeight() - 9) / 2, 9, 9);
            g2.dispose();
        }
    }

    /** Rounded border for inputs and scroll panes */
    static class RoundedBorder extends AbstractBorder {
        private final int radius;
        private final Color color;
        RoundedBorder(int radius, Color color) { this.radius = radius; this.color = color; }
        @Override public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(1f));
            g2.draw(new RoundRectangle2D.Float(x + 0.5f, y + 0.5f, w - 1, h - 1, radius, radius));
            g2.dispose();
        }
        @Override public Insets getBorderInsets(Component c) { return new Insets(4, 8, 4, 8); }
        @Override public Insets getBorderInsets(Component c, Insets i) {
            i.set(4, 8, 4, 8); return i;
        }
    }

    /** Two stacked database cylinders with a VS badge — painted logo icon */
    static class DbLogoIcon extends JComponent {
        DbLogoIcon() {
            setPreferredSize(new Dimension(56, 56));
            setMinimumSize(new Dimension(56, 56));
            setMaximumSize(new Dimension(56, 56));
        }

        /** Render the logo into a BufferedImage of the given size for use as window icon */
        static java.awt.image.BufferedImage createImage(int size) {
            java.awt.image.BufferedImage img =
                new java.awt.image.BufferedImage(size, size, java.awt.image.BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = img.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING,         RenderingHints.VALUE_RENDER_QUALITY);
            paintLogo(g2, size, size);
            g2.dispose();
            return img;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,        RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,   RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING,           RenderingHints.VALUE_RENDER_QUALITY);
            paintLogo(g2, getWidth(), getHeight());
            g2.dispose();
        }

        private static void paintLogo(Graphics2D g2, int W, int H) {
            // ── background pill ───────────────────────────────────────────────
            g2.setColor(new Color(235, 242, 255));
            g2.fill(new RoundRectangle2D.Float(0, 0, W, H, 14, 14));

            // scale factor so drawing works at any size (base = 56px)
            float sx = W / 56f, sy = H / 56f;

            // Left cylinder (DB1, blue)  Right cylinder (DB2, green)
            drawCylinder(g2, sx, sy,  5,  8, 20, 36, 6,
                    new Color(56, 138, 221), new Color(96, 165, 250), new Color(30, 100, 180));
            drawCylinder(g2, sx, sy, 30,  8, 20, 36, 6,
                    new Color(31, 158, 117), new Color(52, 211, 153), new Color(15, 110, 80));

            // ── VS badge in the centre overlap ────────────────────────────────
            int bx = (int)(W / 2f - 8 * sx), by = (int)(H / 2f - 8 * sy);
            int bw = (int)(16 * sx),         bh = (int)(16 * sy);
            g2.setColor(Color.WHITE);
            g2.fillOval(bx - 1, by - 1, bw + 2, bh + 2);
            g2.setColor(new Color(235, 242, 255));
            g2.fillOval(bx, by, bw, bh);
            g2.setColor(new Color(80, 80, 80));
            int fontSize = Math.max(7, (int)(7 * sx));
            g2.setFont(new Font("Segoe UI", Font.BOLD, fontSize));
            FontMetrics fm = g2.getFontMetrics();
            String vs = "VS";
            g2.drawString(vs, bx + (bw - fm.stringWidth(vs)) / 2,
                              by + (bh + fm.getAscent() - fm.getDescent()) / 2);
        }

        private static void drawCylinder(Graphics2D g2, float sx, float sy,
                                          int x, int y, int w, int h, int eh,
                                          Color body, Color topColor, Color botColor) {
            int rx = (int)(x * sx), ry = (int)(y * sy);
            int rw = (int)(w * sx), rh = (int)(h * sy), re = (int)(eh * sy);
            int halfE = re / 2;

            g2.setColor(body);
            g2.fillRect(rx, ry + halfE, rw, rh - re);

            GradientPaint shade = new GradientPaint(
                    rx, 0, new Color(0,0,0,0), rx + rw, 0, new Color(0,0,0,40));
            g2.setPaint(shade);
            g2.fillRect(rx, ry + halfE, rw, rh - re);
            g2.setPaint(null);

            g2.setColor(botColor);
            g2.fillOval(rx, ry + rh - re, rw, re);

            g2.setColor(topColor);
            g2.fillOval(rx, ry, rw, re);

            g2.setColor(new Color(255, 255, 255, 60));
            g2.setStroke(new BasicStroke(0.8f));
            for (int i = 1; i <= 2; i++) {
                int lineY = ry + (re * i / 3);
                g2.drawLine(rx + 3, lineY, rx + rw - 3, lineY);
            }
            g2.setStroke(new BasicStroke(1f));

            g2.setColor(new Color(0, 0, 0, 40));
            g2.drawOval(rx, ry, rw, re);
            g2.drawOval(rx, ry + rh - re, rw, re);
            g2.drawLine(rx,      ry + halfE, rx,      ry + rh - halfE);
            g2.drawLine(rx + rw, ry + halfE, rx + rw, ry + rh - halfE);
        }
    }
}

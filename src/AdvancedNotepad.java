import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.awt.print.PrinterException;
import java.io.*;
import java.util.*;

/**
 * AdvancedNotepad - final deliverable Java single-file application with enhanced tab UI
 */
public class AdvancedNotepad extends JFrame {

    // UI
    private final JTabbedPane tabs = new JTabbedPane();
    private boolean darkMode = true;

    // State per tab
    private final Map<Component, File> tabFileMap = new HashMap<>();
    private final Map<Component, UndoManager> undoMap = new HashMap<>();
    private final Map<Component, Boolean> modifiedMap = new HashMap<>();

    // Modern tab headers
    private final Map<Component, TabHeader> headerMap = new HashMap<>();

    // Recent files handling
    private final LinkedList<String> recentFiles = new LinkedList<>();
    private final int MAX_RECENTS = 8;
    private final File recentFileStore = new File(System.getProperty("user.home"), ".advancednotepad_recent");

    // Autosave / recovery
    private final File autosaveDir = new File(System.getProperty("user.home"), ".advancednotepad_autosave");
    private final int AUTOSAVE_INTERVAL_MS = 60_000; // 60 seconds
    private javax.swing.Timer autosaveTimer;

    // UI components used across methods
    private final JMenu recentMenu = new JMenu("Recent Files");

    public AdvancedNotepad() {
        super("AdvancedNotepad");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setSize(1100, 720);
        setLocationRelativeTo(null);

        // Try to set an application icon
        setAppIcon();

        // Root panel with subtle hover effect
        HoverPanel root = new HoverPanel();
        root.setLayout(new BorderLayout(12, 12));
        root.setBorder(new EmptyBorder(12, 12, 12, 12));
        applyTheme(root);
        setContentPane(root);

        // Top bar with title
        JPanel topBar = new JPanel(new BorderLayout(0, 1));
        topBar.setOpaque(false);

        JPanel titlePanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color c = darkMode ? new Color(32, 34, 38) : new Color(245, 245, 245);
                g2.setColor(c);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        titlePanel.setBorder(new EmptyBorder(12, 16, 12, 16));
        JLabel titleLabel = new JLabel("AdvancedNotepad");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 18f));
        titleLabel.setForeground(getFg());
        titlePanel.add(titleLabel, BorderLayout.WEST);

        topBar.add(titlePanel, BorderLayout.NORTH);
        root.add(topBar, BorderLayout.NORTH);

        // Tabs center with enhanced background
        tabs.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        tabs.setOpaque(false);
        JPanel tabPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = darkMode ?
                        new GradientPaint(0, 0, new Color(28, 30, 34), 0, getHeight(), new Color(18, 20, 23)) :
                        new GradientPaint(0, 0, new Color(245, 245, 245), 0, getHeight(), Color.WHITE);
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(darkMode ? new Color(80, 80, 80) : new Color(200, 200, 200));
                g2.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
                g2.dispose();
            }
        };
        tabPanel.setBorder(new EmptyBorder(4, 4, 4, 4));
        tabPanel.add(tabs, BorderLayout.CENTER);
        root.add(tabPanel, BorderLayout.CENTER);

        // Bottom toolbar
        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        bottomBar.setOpaque(false);
        GradientButton newBtn = new GradientButton("New");
        GradientButton openBtn = new GradientButton("Open");
        GradientButton saveBtn = new GradientButton("Save");
        GradientButton saveAsBtn = new GradientButton("Save As");
        GradientButton findBtn = new GradientButton("Find/Replace");
        GradientButton exportBtn = new GradientButton("Print/Export");
        GradientButton themeBtn = new GradientButton("Toggle Theme");
        Dimension bDim = new Dimension(120, 36);
        newBtn.setPreferredSize(bDim);
        openBtn.setPreferredSize(bDim);
        saveBtn.setPreferredSize(bDim);
        saveAsBtn.setPreferredSize(bDim);
        findBtn.setPreferredSize(bDim);
        exportBtn.setPreferredSize(bDim);
        themeBtn.setPreferredSize(new Dimension(140, 36));
        bottomBar.add(newBtn);
        bottomBar.add(openBtn);
        bottomBar.add(saveBtn);
        bottomBar.add(saveAsBtn);
        bottomBar.add(findBtn);
        bottomBar.add(exportBtn);
        bottomBar.add(Box.createHorizontalStrut(20));
        bottomBar.add(themeBtn);
        root.add(bottomBar, BorderLayout.SOUTH);

        // Menu bar (modern-styled)
        JMenuBar menuBar = new JMenuBar() {
            @Override
            protected void paintComponent(Graphics g) {
                if (g == null) return;
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color c = darkMode ? new Color(28, 30, 34) : new Color(240, 240, 240);
                g2.setColor(c);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(darkMode ? new Color(0, 0, 0, 40) : new Color(0, 0, 0, 15));
                g2.fillRect(0, 0, getWidth(), 1);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        menuBar.setOpaque(true);
        menuBar.setBorder(new EmptyBorder(8, 12, 8, 12));

        JMenu fileMenu = new JMenu("File");
        JMenuItem newItem = new JMenuItem("New");
        JMenuItem openItem = new JMenuItem("Open...");
        JMenuItem saveItem = new JMenuItem("Save");
        JMenuItem saveAsItem = new JMenuItem("Save As...");
        JMenuItem exitItem = new JMenuItem("Exit");
        fileMenu.add(newItem);
        fileMenu.add(openItem);
        fileMenu.add(saveItem);
        fileMenu.add(saveAsItem);
        fileMenu.add(recentMenu);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);

        JMenu editMenu = new JMenu("Edit");
        JMenuItem undoItem = new JMenuItem("Undo");
        JMenuItem redoItem = new JMenuItem("Redo");
        JMenuItem cutItem = new JMenuItem("Cut");
        JMenuItem copyItem = new JMenuItem("Copy");
        JMenuItem pasteItem = new JMenuItem("Paste");
        JMenuItem selectAllItem = new JMenuItem("Select All");
        editMenu.add(undoItem);
        editMenu.add(redoItem);
        editMenu.addSeparator();
        editMenu.add(cutItem);
        editMenu.add(copyItem);
        editMenu.add(pasteItem);
        editMenu.addSeparator();
        editMenu.add(selectAllItem);

        JMenu formatMenu = new JMenu("Format");
        JMenuItem fontChooserItem = new JMenuItem("Font...");
        JMenuItem textColorItem = new JMenuItem("Text Color...");
        JMenuItem bgColorItem = new JMenuItem("Background Color...");
        formatMenu.add(fontChooserItem);
        formatMenu.addSeparator();
        formatMenu.add(textColorItem);
        formatMenu.add(bgColorItem);

        JMenu helpMenu = new JMenu("Help");
        JMenuItem aboutItem = new JMenuItem("About");
        helpMenu.add(aboutItem);

        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(formatMenu);
        menuBar.add(helpMenu);
        setJMenuBar(menuBar);

        // Style menu titles
        for (Component comp : menuBar.getComponents()) {
            if (comp instanceof JMenu jm) {
                jm.setOpaque(false);
                jm.setForeground(getFg());
                jm.setFont(jm.getFont().deriveFont(Font.BOLD, 13f));
                jm.setBorder(new EmptyBorder(6, 10, 6, 10));
                jm.addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) { jm.setForeground(darkMode ? Color.WHITE : new Color(20,20,20)); }
                    @Override public void mouseExited(MouseEvent e)  { jm.setForeground(getFg()); }
                });
            }
        }

        // Actions
        ActionListener newAction = e -> createNewTab(null, null);
        ActionListener openAction = e -> openFileInTab();
        ActionListener saveAction = e -> saveCurrentTab();
        ActionListener saveAsAction = e -> saveAsCurrentTab();
        ActionListener exitAction = e -> exitApplication();
        ActionListener themeAction = e -> toggleTheme();
        ActionListener findAction = e -> showFindReplaceDialog();

        newItem.addActionListener(newAction);
        openItem.addActionListener(openAction);
        saveItem.addActionListener(saveAction);
        saveAsItem.addActionListener(saveAsAction);
        exitItem.addActionListener(exitAction);

        newBtn.addActionListener(newAction);
        openBtn.addActionListener(openAction);
        saveBtn.addActionListener(saveAction);
        saveAsBtn.addActionListener(saveAsAction);
        themeBtn.addActionListener(themeAction);
        findBtn.addActionListener(findAction);
        exportBtn.addActionListener(e -> showPrintExportDialog());

        // Edit actions
        undoItem.addActionListener(e -> {
            UndoManager um = getCurrentUndoManager();
            if (um != null && um.canUndo()) um.undo();
        });
        redoItem.addActionListener(e -> {
            UndoManager um = getCurrentUndoManager();
            if (um != null && um.canRedo()) um.redo();
        });
        cutItem.addActionListener(e -> getCurrentTextArea().ifPresent(JTextArea::cut));
        copyItem.addActionListener(e -> getCurrentTextArea().ifPresent(JTextArea::copy));
        pasteItem.addActionListener(e -> getCurrentTextArea().ifPresent(JTextArea::paste));
        selectAllItem.addActionListener(e -> getCurrentTextArea().ifPresent(JTextArea::selectAll));

        // Format actions
        fontChooserItem.addActionListener(e -> showFontChooser());
        textColorItem.addActionListener(e -> getCurrentTextArea().ifPresent(area -> {
            Color c = JColorChooser.showDialog(this, "Choose Text Color", area.getForeground());
            if (c != null) area.setForeground(c);
        }));
        bgColorItem.addActionListener(e -> getCurrentTextArea().ifPresent(area -> {
            Color c = JColorChooser.showDialog(this, "Choose Background Color", area.getBackground());
            if (c != null) area.setBackground(c);
        }));

        aboutItem.addActionListener(e -> showAboutDialog());

        // Accelerators
        newItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK));
        openItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK));
        saveItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
        undoItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK));
        redoItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK));
        cutItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.CTRL_DOWN_MASK));
        copyItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK));
        pasteItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_DOWN_MASK));
        selectAllItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.CTRL_DOWN_MASK));
        fontChooserItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T, InputEvent.CTRL_DOWN_MASK));

        // Load recent files if exist
        loadRecentFiles();
        rebuildRecentMenu();

        // Check autosave recovery on startup
        checkRecoveryFiles();

        // Start autosave timer
        startAutosaveTimer();

        // Update tab headers when selection changes
        tabs.addChangeListener(e -> {
            Component sel = tabs.getSelectedComponent();
            for (Map.Entry<Component, TabHeader> entry : headerMap.entrySet()) {
                TabHeader h = entry.getValue();
                h.setSelected(entry.getKey() == sel);
            }
        });

        // Window close handler
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                exitApplication();
            }
        });

        // Start with one new tab
        createNewTab(null, null);

        setVisible(true);
    }

    // -------------------- App icon --------------------
    private void setAppIcon() {
        try {
            BufferedImage img = null;
            InputStream res = AdvancedNotepad.class.getResourceAsStream("/icon.png");
            if (res != null) {
                img = ImageIO.read(res);
            } else {
                File f = new File("icon.png");
                if (f.exists()) img = ImageIO.read(f);
            }
            if (img == null) {
                img = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = img.createGraphics();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, new Color(85, 85, 200), 64, 64, new Color(45, 200, 180));
                g.setPaint(gp);
                g.fillRoundRect(0, 0, 64, 64, 14, 14);
                g.setColor(new Color(255, 255, 255, 220));
                g.setFont(g.getFont().deriveFont(Font.BOLD, 28f));
                FontMetrics fm = g.getFontMetrics();
                String s = "AN";
                int tw = fm.stringWidth(s);
                g.drawString(s, (64 - tw) / 2, 42);
                g.dispose();
            }
            setIconImage(img);
        } catch (IOException ignored) {
            // ignore icon load failures
        }
    }

    // -------------------- Tab & Editor helpers --------------------
    private void createNewTab(File fileToOpen, String optionalContent) {
        JTextArea area = new JTextArea();
        area.setFont(new Font("Consolas", Font.PLAIN, 14));
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setMargin(new Insets(8, 10, 8, 10));
        area.setBackground(getBg());
        area.setForeground(getFg());
        area.setCaretColor(getFg());
        AnimatedCaret ac = new AnimatedCaret();
        ac.setBlinkRate(500);
        area.setCaret(ac);

        JScrollPane sp = new JScrollPane(area);
        String title = "Untitled";
        if (fileToOpen != null) {
            title = fileToOpen.getName();
            tabFileMap.put(sp, fileToOpen);
        }

        int idx = tabs.getTabCount();
        tabs.addTab(title, sp);
        tabs.setSelectedIndex(idx);
        undoMap.put(sp, new UndoManager());
        modifiedMap.put(sp, false);

        // document listener for changes
        Document doc = area.getDocument();
        UndoManager um = undoMap.get(sp);
        doc.addUndoableEditListener(e -> {
            um.addEdit(e.getEdit());
            setModifiedFlag(sp, true);
        });

        // right-click popup
        JPopupMenu popup = new JPopupMenu();
        JMenuItem pCut = new JMenuItem("Cut");
        JMenuItem pCopy = new JMenuItem("Copy");
        JMenuItem pPaste = new JMenuItem("Paste");
        JMenuItem pSelectAll = new JMenuItem("Select All");
        JMenuItem pFont = new JMenuItem("Font...");
        popup.add(pCut);
        popup.add(pCopy);
        popup.add(pPaste);
        popup.addSeparator();
        popup.add(pSelectAll);
        popup.addSeparator();
        popup.add(pFont);
        pCut.addActionListener(e -> area.cut());
        pCopy.addActionListener(e -> area.copy());
        pPaste.addActionListener(e -> area.paste());
        pSelectAll.addActionListener(e -> area.selectAll());
        pFont.addActionListener(e -> showFontChooser());
        area.setComponentPopupMenu(popup);
        applyTheme(popup);

        // Add Key bindings for Save (Ctrl+S) per area
        area.getInputMap().put(KeyStroke.getKeyStroke("control S"), "save");
        area.getActionMap().put("save", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                saveCurrentTab();
            }
        });

        // load content if provided
        if (optionalContent != null) {
            area.setText(optionalContent);
            setModifiedFlag(sp, true);
        } else if (fileToOpen != null) {
            try (BufferedReader r = new BufferedReader(new FileReader(fileToOpen))) {
                area.read(r, null);
                setModifiedFlag(sp, false);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error opening: " + ex.getMessage());
            }
        }

        // Tab header with modern header component
        tabs.setTabComponentAt(idx, makeTabHeader(title, sp));
    }

    // create custom tab header with close icon
    private Component makeTabHeader(String title, Component content) {
        TabHeader header = new TabHeader(title, content);
        headerMap.put(content, header);
        boolean isSelected = tabs.getSelectedComponent() == content;
        header.setSelected(isSelected);
        return header;
    }

    private void closeTab(Component content) {
        if (content == null) return;
        if (!confirmSaveForComponent(content)) return;
        int idx = tabs.indexOfComponent(content);
        if (idx >= 0) {
            tabs.removeTabAt(idx);
            tabFileMap.remove(content);
            undoMap.remove(content);
            modifiedMap.remove(content);
            headerMap.remove(content);
            if (tabs.getTabCount() == 0) createNewTab(null, null);
        }
    }

    private Optional<JTextArea> getCurrentTextArea() {
        Component c = tabs.getSelectedComponent();
        if (c instanceof JScrollPane sp) {
            JViewport vp = sp.getViewport();
            Component view = vp.getView();
            if (view instanceof JTextArea t) return Optional.of(t);
        }
        return Optional.empty();
    }

    private UndoManager getCurrentUndoManager() {
        Component c = tabs.getSelectedComponent();
        return undoMap.get(c);
    }

    private void setModifiedFlag(Component tabComponent, boolean modified) {
        modifiedMap.put(tabComponent, modified);
        int idx = tabs.indexOfComponent(tabComponent);
        if (idx >= 0) {
            String title = tabs.getTitleAt(idx);
            if (modified && !title.endsWith("*")) title += "*";
            if (!modified) title = title.replace("*", "");
            tabs.setTitleAt(idx, title);
            tabs.setTabComponentAt(idx, makeTabHeader(title, tabComponent));
        }
    }

    // -------------------- File operations (tab-aware) --------------------
    private void openFileInTab() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Text files", "txt", "text"));
        int option = chooser.showOpenDialog(this);
        if (option == JFileChooser.APPROVE_OPTION) {
            File f = chooser.getSelectedFile();
            // check if already open
            for (Map.Entry<Component, File> e : tabFileMap.entrySet()) {
                if (f.equals(e.getValue())) {
                    tabs.setSelectedComponent(e.getKey());
                    return;
                }
            }
            createNewTab(f, null);
            addToRecent(f.getAbsolutePath());
        }
    }

    private void saveCurrentTab() {
        Component c = tabs.getSelectedComponent();
        if (c == null || !(c instanceof JScrollPane)) return;
        File f = tabFileMap.get(c);
        if (f == null) {
            saveAsCurrentTab();
            return;
        }
        try {
            JScrollPane sp = (JScrollPane) c;
            JTextArea area = (JTextArea) sp.getViewport().getView();
            try (BufferedWriter w = new BufferedWriter(new FileWriter(f))) {
                area.write(w);
            }
            setModifiedFlag(c, false);
            addToRecent(f.getAbsolutePath());
            tabs.setTitleAt(tabs.getSelectedIndex(), f.getName());
            tabs.setTabComponentAt(tabs.getSelectedIndex(), makeTabHeader(f.getName(), c));
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Error saving: " + ex.getMessage());
        }
    }

    private void saveAsCurrentTab() {
        Component c = tabs.getSelectedComponent();
        if (c == null) return;
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Text files", "txt", "text"));
        int option = chooser.showSaveDialog(this);
        if (option == JFileChooser.APPROVE_OPTION) {
            File f = chooser.getSelectedFile();
            if (!f.getName().contains(".")) f = new File(f.getAbsolutePath() + ".txt");
            tabFileMap.put(c, f);
            saveCurrentTab();
        }
    }

    private boolean confirmSaveForComponent(Component comp) {
        Boolean modified = modifiedMap.getOrDefault(comp, false);
        if (modified == null || !modified) return true;
        int opt = JOptionPane.showConfirmDialog(this,
                "Save changes to this document?", "Confirm",
                JOptionPane.YES_NO_CANCEL_OPTION);
        if (opt == JOptionPane.CANCEL_OPTION) return false;
        if (opt == JOptionPane.YES_OPTION) {
            tabs.setSelectedComponent(comp);
            saveCurrentTab();
        }
        return true;
    }

    private boolean confirmSaveForAll() {
        Component[] comps = tabs.getComponents();
        for (Component c : comps) {
            if (!confirmSaveForComponent(c)) return false;
        }
        return true;
    }

    private void exitApplication() {
        if (!confirmSaveForAll()) return;
        cleanupAutosave();
        dispose();
        System.exit(0);
    }

    // -------------------- Recent files --------------------
    private void addToRecent(String path) {
        recentFiles.remove(path);
        recentFiles.addFirst(path);
        while (recentFiles.size() > MAX_RECENTS) recentFiles.removeLast();
        rebuildRecentMenu();
        saveRecentFiles();
    }

    private void rebuildRecentMenu() {
        recentMenu.removeAll();
        if (recentFiles.isEmpty()) {
            JMenuItem empty = new JMenuItem("No recent files");
            empty.setEnabled(false);
            recentMenu.add(empty);
        } else {
            for (String p : recentFiles) {
                JMenuItem it = new JMenuItem(p);
                it.addActionListener(e -> {
                    File f = new File(p);
                    if (f.exists()) {
                        createNewTab(f, null);
                    } else {
                        int r = JOptionPane.showConfirmDialog(this, "File not found. Remove from recent list?", "Missing", JOptionPane.YES_NO_OPTION);
                        if (r == JOptionPane.YES_OPTION) {
                            recentFiles.remove(p);
                            rebuildRecentMenu();
                            saveRecentFiles();
                        }
                    }
                });
                recentMenu.add(it);
            }
            recentMenu.addSeparator();
            JMenuItem clear = new JMenuItem("Clear Recent");
            clear.addActionListener(e -> { recentFiles.clear(); rebuildRecentMenu(); saveRecentFiles(); });
            recentMenu.add(clear);
        }
    }

    private void loadRecentFiles() {
        if (!recentFileStore.exists()) return;
        try (BufferedReader r = new BufferedReader(new FileReader(recentFileStore))) {
            String line;
            while ((line = r.readLine()) != null) {
                if (!line.isBlank()) recentFiles.add(line.trim());
            }
        } catch (IOException ignored) {}
    }

    private void saveRecentFiles() {
        try (BufferedWriter w = new BufferedWriter(new FileWriter(recentFileStore))) {
            for (String s : recentFiles) w.write(s + System.lineSeparator());
        } catch (IOException ignored) {}
    }

    // -------------------- Autosave & Recovery --------------------
    private void startAutosaveTimer() {
        if (!autosaveDir.exists()) autosaveDir.mkdirs();
        if (autosaveTimer != null) autosaveTimer.stop();
        autosaveTimer = new javax.swing.Timer(AUTOSAVE_INTERVAL_MS, e -> autosaveAll());
        autosaveTimer.start();
    }

    private void autosaveAll() {
        try {
            if (!autosaveDir.exists()) autosaveDir.mkdirs();
            int tabCount = tabs.getTabCount();
            for (int i = 0; i < tabCount; i++) {
                Component c = tabs.getComponentAt(i);
                if (!(c instanceof JScrollPane sp)) continue;
                JTextArea area = (JTextArea) sp.getViewport().getView();
                boolean modified = modifiedMap.getOrDefault(c, false);
                if (!modified) continue;
                String name = "autosave_tab" + i + "_" + System.currentTimeMillis() + ".tmp";
                File out = new File(autosaveDir, name);
                try (BufferedWriter w = new BufferedWriter(new FileWriter(out))) {
                    File orig = tabFileMap.get(c);
                    w.write("##ORIG:" + (orig == null ? "" : orig.getAbsolutePath()));
                    w.newLine();
                    area.write(w);
                } catch (IOException ex) {
                    // ignore per-file
                }
            }
        } catch (Exception ignored) {}
    }

    private void checkRecoveryFiles() {
        if (!autosaveDir.exists()) return;
        File[] files = autosaveDir.listFiles((d, n) -> n.startsWith("autosave_tab"));
        if (files == null || files.length == 0) return;
        int opt = JOptionPane.showConfirmDialog(this,
                "Auto-saved files found from previous session. Recover?", "Recovery", JOptionPane.YES_NO_OPTION);
        if (opt == JOptionPane.YES_OPTION) {
            for (File f : files) {
                try (BufferedReader r = new BufferedReader(new FileReader(f))) {
                    String header = r.readLine();
                    String origPath = "";
                    if (header != null && header.startsWith("##ORIG:")) origPath = header.substring(7);
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = r.readLine()) != null) {
                        sb.append(line).append(System.lineSeparator());
                    }
                    File orig = origPath.isBlank() ? null : new File(origPath);
                    createNewTab(orig, sb.toString());
                    f.delete();
                } catch (IOException ignored) {}
            }
        }
    }

    private void cleanupAutosave() {
        if (!autosaveDir.exists()) return;
        File[] files = autosaveDir.listFiles((d, n) -> n.startsWith("autosave_tab"));
        if (files == null) return;
        for (File f : files) f.delete();
    }

    // -------------------- Find & Replace --------------------
    private void showFindReplaceDialog() {
        Optional<JTextArea> maybe = getCurrentTextArea();
        if (maybe.isEmpty()) return;
        JTextArea area = maybe.get();

        JDialog d = new JDialog(this, "Find & Replace", false);
        d.setSize(520, 220);
        d.setLocationRelativeTo(this);
        d.setLayout(new BorderLayout(10, 10));
        d.getContentPane().setBackground(getBg());

        JPanel top = new JPanel(new GridLayout(2, 2, 8, 8));
        top.setOpaque(false);
        JLabel findLbl = new JLabel("Find:");
        findLbl.setForeground(getFg());
        JTextField findField = new JTextField();
        findField.setBackground(darkMode ? new Color(40, 42, 46) : Color.WHITE);
        findField.setForeground(getFg());
        findField.setCaretColor(getFg());
        JLabel replaceLbl = new JLabel("Replace:");
        replaceLbl.setForeground(getFg());
        JTextField replaceField = new JTextField();
        replaceField.setBackground(darkMode ? new Color(40, 42, 46) : Color.WHITE);
        replaceField.setForeground(getFg());
        replaceField.setCaretColor(getFg());
        top.add(findLbl);
        top.add(findField);
        top.add(replaceLbl);
        top.add(replaceField);

        JPanel options = new JPanel(new FlowLayout(FlowLayout.LEFT));
        options.setOpaque(false);
        JCheckBox matchCase = new JCheckBox("Match case");
        matchCase.setOpaque(false);
        matchCase.setForeground(getFg());
        options.add(matchCase);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.setOpaque(false);
        GradientButton findNext = new GradientButton("Find Next");
        GradientButton findPrev = new GradientButton("Find Prev");
        GradientButton replace = new GradientButton("Replace");
        GradientButton replaceAll = new GradientButton("Replace All");
        GradientButton close = new GradientButton("Close");
        buttons.add(findPrev);
        buttons.add(findNext);
        buttons.add(replace);
        buttons.add(replaceAll);
        buttons.add(close);

        d.add(top, BorderLayout.NORTH);
        d.add(options, BorderLayout.CENTER);
        d.add(buttons, BorderLayout.SOUTH);

        final int[] lastIndex = {0};

        ActionListener doFindNext = e -> {
            String text = area.getText();
            String find = findField.getText();
            if (!matchCase.isSelected()) {
                text = text.toLowerCase();
                find = find.toLowerCase();
            }
            if (find.isEmpty()) return;
            int from = area.getSelectionEnd();
            int idx = text.indexOf(find, Math.max(from, lastIndex[0]));
            if (idx >= 0) {
                area.requestFocus();
                area.select(idx, idx + find.length());
                lastIndex[0] = idx + 1;
            } else {
                JOptionPane.showMessageDialog(d, "Not found.");
            }
        };

        ActionListener doFindPrev = e -> {
            String text = area.getText();
            String find = findField.getText();
            if (!matchCase.isSelected()) {
                text = text.toLowerCase();
                find = find.toLowerCase();
            }
            if (find.isEmpty()) return;
            int from = Math.max(0, area.getSelectionStart() - 1);
            int idx = text.lastIndexOf(find, from);
            if (idx >= 0) {
                area.requestFocus();
                area.select(idx, idx + find.length());
                lastIndex[0] = idx;
            } else {
                JOptionPane.showMessageDialog(d, "Not found.");
            }
        };

        ActionListener doReplace = e -> {
            String sel = area.getSelectedText();
            String find = findField.getText();
            if (sel != null && !sel.isEmpty()) {
                if ((matchCase.isSelected() && sel.equals(find)) ||
                        (!matchCase.isSelected() && sel.equalsIgnoreCase(find))) {
                    area.replaceSelection(replaceField.getText());
                }
            }
            doFindNext.actionPerformed(null);
        };

        ActionListener doReplaceAll = e -> {
            String find = findField.getText();
            if (find.isEmpty()) return;
            String replacement = replaceField.getText();
            String content = area.getText();
            if (matchCase.isSelected()) content = content.replace(find, replacement);
            else {
                content = replaceIgnoreCase(content, find, replacement);
            }
            area.setText(content);
        };

        findNext.addActionListener(doFindNext);
        findPrev.addActionListener(doFindPrev);
        replace.addActionListener(doReplace);
        replaceAll.addActionListener(doReplaceAll);
        close.addActionListener(e -> d.dispose());

        d.setVisible(true);
    }

    private static String replaceIgnoreCase(String source, String target, String replacement) {
        StringBuilder sb = new StringBuilder();
        String lowerSource = source.toLowerCase();
        String lowerTarget = target.toLowerCase();
        int idx = 0;
        int found;
        while ((found = lowerSource.indexOf(lowerTarget, idx)) != -1) {
            sb.append(source, idx, found);
            sb.append(replacement);
            idx = found + target.length();
        }
        sb.append(source.substring(idx));
        return sb.toString();
    }

    // -------------------- Print / Export --------------------
    private void showPrintExportDialog() {
        Optional<JTextArea> maybe = getCurrentTextArea();
        if (maybe.isEmpty()) return;
        JTextArea area = maybe.get();

        Object[] options = {"Print...", "Export to PDF (use system PDF printer)", "Cancel"};
        int choice = JOptionPane.showOptionDialog(this,
                "Print or export the current document. On many systems 'Print' allows choosing a 'Save as PDF' target.",
                "Print / Export",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null, options, options[0]);

        if (choice == JOptionPane.CANCEL_OPTION || choice == JOptionPane.CLOSED_OPTION) return;
        try {
            boolean showPrintDialog = (choice == 0);
            boolean interactive = true;
            area.print(null, null, showPrintDialog, null, null, interactive);
        } catch (PrinterException ex) {
            JOptionPane.showMessageDialog(this, "Printing error: " + ex.getMessage());
        }
    }

    // -------------------- Font chooser --------------------
    private void showFontChooser() {
        Optional<JTextArea> maybe = getCurrentTextArea();
        if (maybe.isEmpty()) return;
        JTextArea area = maybe.get();

        Font current = area.getFont();
        JDialog d = new JDialog(this, "Choose Font", true);
        d.setSize(520, 360);
        d.setLocationRelativeTo(this);
        d.setLayout(new BorderLayout(10, 10));
        d.getContentPane().setBackground(getBg());

        java.awt.GraphicsEnvironment ge = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment();
        String[] fonts = ge.getAvailableFontFamilyNames();

        JComboBox<String> fontBox = new JComboBox<>(fonts);
        fontBox.setSelectedItem(current.getFamily());
        fontBox.setBackground(darkMode ? new Color(40, 42, 46) : Color.WHITE);
        fontBox.setForeground(getFg());

        Integer[] sizes = {10, 11, 12, 13, 14, 16, 18, 20, 22, 24, 28, 32, 36};
        JComboBox<Integer> sizeBox = new JComboBox<>(sizes);
        sizeBox.setSelectedItem(current.getSize());
        sizeBox.setBackground(darkMode ? new Color(40, 42, 46) : Color.WHITE);
        sizeBox.setForeground(getFg());

        String[] styles = {"Plain", "Bold", "Italic", "Bold Italic"};
        JComboBox<String> styleBox = new JComboBox<>(styles);
        styleBox.setSelectedIndex(fontStyleToIndex(current.getStyle()));
        styleBox.setBackground(darkMode ? new Color(40, 42, 46) : Color.WHITE);
        styleBox.setForeground(getFg());

        JLabel preview = new JLabel("AaBbCc – The quick brown fox jumps over the lazy dog");
        preview.setOpaque(true);
        preview.setBackground(getBg());
        preview.setForeground(getFg());
        preview.setBorder(new EmptyBorder(8, 8, 8, 8));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        top.setOpaque(false);
        JLabel fontLbl = new JLabel("Font:");
        fontLbl.setForeground(getFg());
        JLabel styleLbl = new JLabel("Style:");
        styleLbl.setForeground(getFg());
        JLabel sizeLbl = new JLabel("Size:");
        sizeLbl.setForeground(getFg());
        top.add(fontLbl); top.add(fontBox);
        top.add(styleLbl); top.add(styleBox);
        top.add(sizeLbl); top.add(sizeBox);

        d.add(top, BorderLayout.NORTH);
        d.add(preview, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.setOpaque(false);
        GradientButton ok = new GradientButton("OK");
        GradientButton cancel = new GradientButton("Cancel");
        bottom.add(cancel); bottom.add(ok);
        d.add(bottom, BorderLayout.SOUTH);

        ActionListener updatePreview = e -> {
            String family = Objects.toString(fontBox.getSelectedItem(), current.getFamily());
            int style = indexToFontStyle(styleBox.getSelectedIndex());
            int size = (Integer) sizeBox.getSelectedItem();
            preview.setFont(new Font(family, style, size));
        };
        fontBox.addActionListener(updatePreview);
        sizeBox.addActionListener(updatePreview);
        styleBox.addActionListener(updatePreview);
        updatePreview.actionPerformed(null);

        ok.addActionListener(e -> {
            String family = Objects.toString(fontBox.getSelectedItem(), current.getFamily());
            int style = indexToFontStyle(styleBox.getSelectedIndex());
            int size = (Integer) sizeBox.getSelectedItem();
            area.setFont(new Font(family, style, size));
            d.dispose();
        });

        cancel.addActionListener(e -> d.dispose());
        d.setVisible(true);
    }

    private int fontStyleToIndex(int style) {
        return switch (style) {
            case Font.BOLD -> 1;
            case Font.ITALIC -> 2;
            case Font.BOLD + Font.ITALIC -> 3;
            default -> 0;
        };
    }

    private int indexToFontStyle(int index) {
        return switch (index) {
            case 1 -> Font.BOLD;
            case 2 -> Font.ITALIC;
            case 3 -> Font.BOLD | Font.ITALIC;
            default -> Font.PLAIN;
        };
    }

    // -------------------- About dialog --------------------
    private void showAboutDialog() {
        String developerName = "Chamindu Lakshan";
        String studentId = "Student ID: 123456";
        String course = "Course: IT3003 - Java";
        String email = "your.email@university.edu";

        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBackground(getBg());
        JLabel lbl = new JLabel("<html><div style='padding:6px; color:" + (darkMode ? "#E6E6E6" : "#111111") +
                "'><b>AdvancedNotepad</b><br/>" + course + "<br/><br/>Created by:<br/>" + developerName + "<br/>" + "</div></html>");
        lbl.setBorder(new EmptyBorder(10, 10, 10, 10));
        panel.add(lbl, BorderLayout.CENTER);

        JOptionPane.showMessageDialog(this, panel, "About AdvancedNotepad", JOptionPane.PLAIN_MESSAGE);
    }

    // -------------------- Animated caret --------------------
    private static class AnimatedCaret extends DefaultCaret {
        private final javax.swing.Timer t;
        private float hue = 0f;

        AnimatedCaret() {
            setBlinkRate(400);
            t = new javax.swing.Timer(80, e -> {
                hue += 0.02f;
                if (hue > 1f) hue = 0f;
                JTextComponent c = getComponent();
                if (c != null) {
                    try {
                        Shape s = c.modelToView2D(getDot());
                        if (s != null) {
                            Rectangle r = s.getBounds();
                            c.repaint(r);
                        }
                    } catch (BadLocationException ignored) {}
                }
            });
            t.start();
        }

        @Override
        public void paint(Graphics g) {
            JTextComponent comp = getComponent();
            if (comp == null) return;
            try {
                Shape s = comp.modelToView2D(getDot());
                if (s == null) return;
                Rectangle r = s.getBounds();
                Graphics2D g2 = (Graphics2D) g.create();
                int cw = 2;
                int ch = r.height;
                int cx = r.x;
                int cy = r.y;
                Color c1 = Color.getHSBColor(hue, 0.9f, 1f);
                Color c2 = Color.getHSBColor((hue + 0.5f) % 1f, 0.8f, 0.9f);
                GradientPaint gp = new GradientPaint(cx, cy, c1, cx + cw, cy + ch, c2);
                g2.setPaint(gp);
                g2.fillRect(cx, cy, cw, ch);
                g2.dispose();
            } catch (BadLocationException ignored) {}
        }

        @Override
        public void deinstall(JTextComponent c) {
            super.deinstall(c);
            t.stop();
        }
    }

    // -------------------- Gradient button --------------------
    private class GradientButton extends JButton {
        private boolean hover = false;

        GradientButton(String text) {
            super(text);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
            setForeground(darkMode ? new Color(245, 245, 245) : new Color(25, 25, 25));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setFont(getFont().deriveFont(Font.PLAIN, 13f));

            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
            });
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            int w = getWidth(), h = getHeight();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            RoundRectangle2D.Float rr = new RoundRectangle2D.Float(0, 0, w, h, 12, 12);

            Color base = darkMode ? new Color(70, 70, 80) : new Color(220, 220, 220);
            Color hov = darkMode ? new Color(90, 90, 100) : new Color(200, 200, 200);
            g2.setColor(hover ? hov : base);
            g2.fill(rr);

            FontMetrics fm = g2.getFontMetrics();
            int tw = fm.stringWidth(getText());
            int th = fm.getAscent();
            g2.setColor(getForeground());
            g2.setFont(getFont());
            g2.drawString(getText(), (w - tw) / 2, (h + th) / 2 - 2);

            g2.dispose();
        }
    }

    // -------------------- Hover panel --------------------
    private class HoverPanel extends JPanel {
        private float highlight = 0f;
        private final javax.swing.Timer anim;
        private boolean targetHover = false;

        HoverPanel() {
            setOpaque(true);
            anim = new javax.swing.Timer(20, e -> {
                if (targetHover && highlight < 0.16f) {
                    highlight = Math.min(0.16f, highlight + 0.02f);
                    repaint();
                } else if (!targetHover && highlight > 0f) {
                    highlight = Math.max(0f, highlight - 0.02f);
                    repaint();
                }
            });
            anim.start();

            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { targetHover = true; }
                @Override public void mouseExited(MouseEvent e) { targetHover = false; }
            });
        }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (highlight > 0f) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, highlight));
                if (darkMode) g2.setColor(new Color(255, 255, 255, 30));
                else g2.setColor(new Color(0, 0, 0, 30));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        }
    }

    // -------------------- Modern Tab Header + Close Button --------------------
    private class TabCloseButton extends JButton {
        private boolean hover = false;
        private float hoverAlpha = 0f;
        private final javax.swing.Timer hoverTimer;

        TabCloseButton() {
            super("×");
            setPreferredSize(new Dimension(20, 20));
            setFocusable(false);
            setBorder(null);
            setContentAreaFilled(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setFont(getFont().deriveFont(Font.BOLD, 12f));
            setForeground(new Color(200, 200, 200));

            hoverTimer = new javax.swing.Timer(30, e -> {
                if (hover && hoverAlpha < 1f) {
                    hoverAlpha = Math.min(1f, hoverAlpha + 0.1f);
                    repaint();
                } else if (!hover && hoverAlpha > 0f) {
                    hoverAlpha = Math.max(0f, hoverAlpha - 0.1f);
                    repaint();
                }
            });
            hoverTimer.start();

            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; }
                @Override public void mouseExited(MouseEvent e) { hover = false; }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();

            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, hoverAlpha));
            g2.setColor(new Color(220, 60, 60));
            g2.fillOval(2, 2, w - 4, h - 4);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
            g2.setColor(hover ? Color.WHITE : getForeground());

            FontMetrics fm = g2.getFontMetrics();
            String txt = getText();
            int tw = fm.stringWidth(txt);
            int th = fm.getAscent();
            g2.setFont(getFont());
            g2.drawString(txt, (w - tw) / 2, (h + th) / 2 - 2);

            g2.dispose();
        }
    }

    private class TabHeader extends JPanel {
        private final JLabel titleLabel;
        private final TabCloseButton closeBtn;
        private final Component contentRef;
        private boolean selected = false;

        TabHeader(String title, Component content) {
            super(new FlowLayout(FlowLayout.LEFT, 8, 6));
            this.contentRef = content;
            setOpaque(false);

            titleLabel = new JLabel(title);
            titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 14f));
            titleLabel.setForeground(getFg());

            closeBtn = new TabCloseButton();
            closeBtn.addActionListener(e -> closeTab(contentRef));

            add(titleLabel);
            add(closeBtn);
            setBorder(new EmptyBorder(6, 10, 6, 10));
        }

        void setSelected(boolean sel) {
            this.selected = sel;
            titleLabel.setForeground(sel ? (darkMode ? Color.WHITE : new Color(20,20,20)) : (darkMode ? getFg() : new Color(110,110,110)));
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            int w = getWidth(), h = getHeight();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            RoundRectangle2D.Float rr = new RoundRectangle2D.Float(0, 0, w - 1, h - 1, 14, 14);

            if (selected) {
                GradientPaint gp = darkMode ?
                        new GradientPaint(0, 0, new Color(78, 82, 110), 0, h, new Color(58, 62, 90)) :
                        new GradientPaint(0, 0, new Color(220, 230, 250), 0, h, new Color(200, 210, 230));
                g2.setPaint(gp);
                g2.fill(rr);
                // Add subtle shadow for active tab
                g2.setColor(darkMode ? new Color(0, 0, 0, 100) : new Color(0, 0, 0, 50));
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
                g2.fill(new RoundRectangle2D.Float(2, 2, w - 1, h + 2, 14, 14));
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
            } else {
                GradientPaint gp = darkMode ?
                        new GradientPaint(0, 0, new Color(45, 47, 52), 0, h, new Color(35, 37, 42)) :
                        new GradientPaint(0, 0, new Color(255, 255, 255), 0, h, new Color(245, 248, 255));
                g2.setPaint(gp);
                g2.fill(rr);
            }

            g2.dispose();
            super.paintComponent(g);
        }
    }

    // -------------------- Theme helpers --------------------
    private Color getBg() { return darkMode ? new Color(18, 20, 23) : Color.WHITE; }
    private Color getFg() { return darkMode ? new Color(235, 235, 240) : new Color(34, 34, 34); }
    private Color getMenuBg() { return darkMode ? new Color(30, 32, 36) : new Color(240, 240, 240); }
    private void applyTheme(JComponent comp) {
        if (comp == null) return;
        comp.setBackground(darkMode ? new Color(26, 28, 33) : Color.WHITE);
        comp.setForeground(darkMode ? new Color(235, 235, 240) : Color.BLACK);
        for (Component c : comp.getComponents()) {
            if (c instanceof JComponent jc) {
                jc.setBackground(darkMode ? new Color(26, 28, 33) : Color.WHITE);
                jc.setForeground(darkMode ? new Color(235, 235, 240) : Color.BLACK);
                applyTheme(jc);
            }
        }
    }

    private void toggleTheme() {
        darkMode = !darkMode;
        applyTheme((JComponent) getContentPane());
        applyTheme(getJMenuBar());
        Component[] comps = tabs.getComponents();
        for (Component comp : comps) {
            if (comp instanceof JScrollPane sp) {
                Component view = sp.getViewport().getView();
                if (view instanceof JTextArea area) {
                    area.setBackground(getBg());
                    area.setForeground(getFg());
                    area.setCaretColor(getFg());
                }
            }
        }
        for (Map.Entry<Component, TabHeader> e : headerMap.entrySet()) {
            TabHeader h = e.getValue();
            boolean sel = tabs.getSelectedComponent() == e.getKey();
            h.setSelected(sel);
        }
        SwingUtilities.updateComponentTreeUI(this);
    }

    // -------------------- Utilities --------------------
    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> new AdvancedNotepad());
    }
}
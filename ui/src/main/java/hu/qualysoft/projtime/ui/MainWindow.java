package hu.qualysoft.projtime.ui;

import hu.qualysoft.tefgin.common.Utils;
import hu.qualysoft.tefgin.daxclient.NTLMAuthenticator;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.prefs.Preferences;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.BevelBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.MaskFormatter;
import javax.swing.text.PlainDocument;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.qualysoft.timesheet.ws.service._200.ActivityListVO;
import com.qualysoft.timesheet.ws.service._200.ProjectListVO;

public class MainWindow {

    private final static Logger LOG = LoggerFactory.getLogger(MainWindow.class.getName());

    // settings tab
    private JFrame frmTefgin;
    private JTextField apUsernameField;
    private JPasswordField apPasswordField;
    private JTextField qsUsernameField;
    private JPasswordField qsPasswordField;

    // tef tab
    private JFormattedTextField tefFromDateField;
    private MaskFormatter formatter;

    private TefBookingTableModel tefBooking;
    private JTextField tefSummaryField;

    // dax tab
    private JFormattedTextField daxFromDateField;
    private JFormattedTextField daxEndDateField;
    private JComboBox<ProjectListVO> daxProjectPickerField;
    private JComboBox<ActivityListVO> daxActivityPickerField;
    private JTable daxSyncTable;
    private JTextField noteField;
    private DaxTableModel daxTableModel;

    private volatile boolean changed = false;
    private final Executor jobs = Executors.newFixedThreadPool(2);

    private Controller controller;

    /**
     * Launch the application.
     */
    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    Utils.setupProxyServerFromEnvironment();
                    NTLMAuthenticator.getInstance().install();

                    final MainWindow window = new MainWindow();
                    window.frmTefgin.setVisible(true);
                } catch (final Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Create the application.
     */
    public MainWindow() {
        initialize();
        loadValues();
        setupListeners();
    }

    /**
     * Initialize the contents of the frame.
     */
    private void initialize() {
        frmTefgin = new JFrame();
        frmTefgin.setTitle("TEF-GIN");
        frmTefgin.setBounds(100, 100, 687, 378);
        frmTefgin.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        final JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
        frmTefgin.getContentPane().add(tabbedPane, BorderLayout.CENTER);

        createSettingsTab(tabbedPane);

        createTefTab(tabbedPane);

        createDaxTab(tabbedPane);

        final ImageIcon icon = createImageIcon("/approved.png");
        final JLabel label = new JLabel(icon, SwingConstants.LEFT);
        label.setVerticalAlignment(SwingConstants.BOTTOM);

        frmTefgin.setGlassPane(label);

        label.setVisible(true);

    }

    private void createDaxTab(final JTabbedPane tabbedPane) {
        final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");

        final JPanel daxPanel = new JPanel();
        tabbedPane.addTab("DAX", null, daxPanel, null);
        final GridBagLayout gbl_daxPanel = new GridBagLayout();
        gbl_daxPanel.columnWidths = new int[] { 0, 0, 0 };
        gbl_daxPanel.rowHeights = new int[] { 0, 0, 0, 0, 0, 0, 0, 0 };
        gbl_daxPanel.columnWeights = new double[] { 1.0, 1.0, Double.MIN_VALUE };
        gbl_daxPanel.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, Double.MIN_VALUE };
        daxPanel.setLayout(gbl_daxPanel);

        final JLabel daxStartDateLabel = new JLabel("Kezdő dátum:");
        final GridBagConstraints gbc_daxStartDateLabel = new GridBagConstraints();
        gbc_daxStartDateLabel.insets = new Insets(0, 0, 5, 5);
        gbc_daxStartDateLabel.anchor = GridBagConstraints.EAST;
        gbc_daxStartDateLabel.gridx = 0;
        gbc_daxStartDateLabel.gridy = 0;
        daxPanel.add(daxStartDateLabel, gbc_daxStartDateLabel);

        daxFromDateField = new JFormattedTextField(df);
        daxStartDateLabel.setLabelFor(daxFromDateField);
        getFormatter().install(daxFromDateField);
        final GridBagConstraints gbc_daxFromDateField = new GridBagConstraints();
        gbc_daxFromDateField.insets = new Insets(0, 0, 5, 0);
        gbc_daxFromDateField.fill = GridBagConstraints.HORIZONTAL;
        gbc_daxFromDateField.gridx = 1;
        gbc_daxFromDateField.gridy = 0;
        daxPanel.add(daxFromDateField, gbc_daxFromDateField);

        daxFromDateField.addPropertyChangeListener("value", new DateChangeListener());

        final JLabel daxEndDateLabel = new JLabel("Befejező dátum:");
        final GridBagConstraints gbc_daxEndDateLabel = new GridBagConstraints();
        gbc_daxEndDateLabel.anchor = GridBagConstraints.EAST;
        gbc_daxEndDateLabel.insets = new Insets(0, 0, 5, 5);
        gbc_daxEndDateLabel.gridx = 0;
        gbc_daxEndDateLabel.gridy = 1;
        daxPanel.add(daxEndDateLabel, gbc_daxEndDateLabel);

        daxEndDateField = new JFormattedTextField(df);
        daxEndDateLabel.setLabelFor(daxEndDateField);
        getFormatter().install(daxEndDateField);
        final GridBagConstraints gbc_daxEndDateField = new GridBagConstraints();
        gbc_daxEndDateField.insets = new Insets(0, 0, 5, 0);
        gbc_daxEndDateField.fill = GridBagConstraints.HORIZONTAL;
        gbc_daxEndDateField.gridx = 1;
        gbc_daxEndDateField.gridy = 1;
        daxPanel.add(daxEndDateField, gbc_daxEndDateField);
        daxEndDateField.addPropertyChangeListener("value", new DateChangeListener());

        final JButton loadProjectCodes = new JButton("Projektek letöltése");
        final GridBagConstraints gbc_loadProjectCodes = new GridBagConstraints();
        gbc_loadProjectCodes.insets = new Insets(0, 0, 5, 0);
        gbc_loadProjectCodes.gridx = 1;
        gbc_loadProjectCodes.gridy = 2;
        daxPanel.add(loadProjectCodes, gbc_loadProjectCodes);

        loadProjectCodes.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                loadProjectCodes.setEnabled(false);
                jobs.execute(new DAXProjectCodeLoader(loadProjectCodes));
            }
        });

        final JLabel daxProjectLabel = new JLabel("Projekt:");
        final GridBagConstraints gbc_daxProjectLabel = new GridBagConstraints();
        gbc_daxProjectLabel.anchor = GridBagConstraints.EAST;
        gbc_daxProjectLabel.insets = new Insets(0, 0, 5, 5);
        gbc_daxProjectLabel.gridx = 0;
        gbc_daxProjectLabel.gridy = 3;
        daxPanel.add(daxProjectLabel, gbc_daxProjectLabel);

        daxProjectPickerField = new JComboBox<>(getController().getProjects());
        daxProjectPickerField.setRenderer(new ProjectListRenderer());
        final GridBagConstraints gbc_daxProjectPickerField = new GridBagConstraints();
        gbc_daxProjectPickerField.insets = new Insets(0, 0, 5, 0);
        gbc_daxProjectPickerField.fill = GridBagConstraints.HORIZONTAL;
        gbc_daxProjectPickerField.gridx = 1;
        gbc_daxProjectPickerField.gridy = 3;
        daxPanel.add(daxProjectPickerField, gbc_daxProjectPickerField);

        final JLabel daxActivityLabel = new JLabel("Aktivítás:");
        final GridBagConstraints gbc_daxActivityLabel = new GridBagConstraints();
        gbc_daxActivityLabel.anchor = GridBagConstraints.EAST;
        gbc_daxActivityLabel.insets = new Insets(0, 0, 5, 5);
        gbc_daxActivityLabel.gridx = 0;
        gbc_daxActivityLabel.gridy = 4;
        daxPanel.add(daxActivityLabel, gbc_daxActivityLabel);

        daxActivityPickerField = new JComboBox<>(getController().getActivities());
        daxActivityPickerField.setRenderer(new ActivityListRenderer());
        final GridBagConstraints gbc_daxActivityPickerField = new GridBagConstraints();
        gbc_daxActivityPickerField.insets = new Insets(0, 0, 5, 0);
        gbc_daxActivityPickerField.fill = GridBagConstraints.HORIZONTAL;
        gbc_daxActivityPickerField.gridx = 1;
        gbc_daxActivityPickerField.gridy = 4;
        daxPanel.add(daxActivityPickerField, gbc_daxActivityPickerField);

        final JScrollPane scrollPane = new JScrollPane();
        final GridBagConstraints gbc_scrollPane = new GridBagConstraints();
        gbc_scrollPane.insets = new Insets(0, 0, 5, 0);
        gbc_scrollPane.gridwidth = 2;
        gbc_scrollPane.fill = GridBagConstraints.BOTH;
        gbc_scrollPane.gridx = 0;
        gbc_scrollPane.gridy = 5;
        daxPanel.add(scrollPane, gbc_scrollPane);

        daxSyncTable = new JTable(getDaxTableModel());
        scrollPane.setViewportView(daxSyncTable);

        final JLabel noteLabel = new JLabel("Megjegyzés:");
        final GridBagConstraints gbc_noteLabel = new GridBagConstraints();
        gbc_noteLabel.anchor = GridBagConstraints.EAST;
        gbc_noteLabel.insets = new Insets(0, 0, 5, 5);
        gbc_noteLabel.gridx = 0;
        gbc_noteLabel.gridy = 6;
        daxPanel.add(noteLabel, gbc_noteLabel);

        noteField = new JTextField();
        final GridBagConstraints gbc_noteField = new GridBagConstraints();
        gbc_noteField.fill = GridBagConstraints.HORIZONTAL;
        gbc_noteField.gridx = 1;
        gbc_noteField.gridy = 6;
        daxPanel.add(noteField, gbc_noteField);

        final JButton createActivitiesButton = new JButton("Létrehozás");
        final GridBagConstraints gbc_createActivitiesButton = new GridBagConstraints();
        gbc_createActivitiesButton.insets = new Insets(0, 0, 0, 5);
        gbc_createActivitiesButton.gridx = 1;
        gbc_createActivitiesButton.gridy = 7;
        daxPanel.add(createActivitiesButton, gbc_createActivitiesButton);

        createActivitiesButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                createActivitiesButton.setEnabled(false);
                jobs.execute(new BookTasks(createActivitiesButton));
            }
        });

    }

    private void createSettingsTab(final JTabbedPane tabbedPane) {
        final JPanel settingsPanel = new JPanel();
        tabbedPane.addTab("Beállítások", null, settingsPanel, null);
        final GridBagLayout gbl_settingsPanel = new GridBagLayout();
        settingsPanel.setLayout(gbl_settingsPanel);

        createAutoPartnerPanel(settingsPanel);

        createQsSettingsPanel(settingsPanel);
    }

    private void createTefTab(final JTabbedPane tabbedPane) {
        final JPanel tefPanel = new JPanel();
        tabbedPane.addTab("TEF", null, tefPanel, null);
        final GridBagLayout gbl_tefPanel = new GridBagLayout();
        gbl_tefPanel.columnWeights = new double[] { 0.0, 1.0 };
        tefPanel.setLayout(gbl_tefPanel);

        final JLabel tefFromDateLabel = new JLabel("Kezdő dátum:");
        final GridBagConstraints gbc_tefFromDateLabel = new GridBagConstraints();
        gbc_tefFromDateLabel.insets = new Insets(0, 0, 5, 5);
        gbc_tefFromDateLabel.weightx = 0.5;
        gbc_tefFromDateLabel.gridx = 0;
        gbc_tefFromDateLabel.gridy = 0;
        tefPanel.add(tefFromDateLabel, gbc_tefFromDateLabel);
        tefFromDateLabel.setLabelFor(tefFromDateField);

        final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        tefFromDateField = new JFormattedTextField(df);
        tefFromDateField.setColumns(12);
        getFormatter().install(tefFromDateField);
        final GridBagConstraints gbc_tefFromDateField = new GridBagConstraints();
        gbc_tefFromDateField.insets = new Insets(0, 0, 5, 0);
        gbc_tefFromDateField.fill = GridBagConstraints.HORIZONTAL;
        gbc_tefFromDateField.weightx = 1.0;
        gbc_tefFromDateField.gridx = 1;
        gbc_tefFromDateField.gridy = 0;
        tefFromDateField.setValue(new Date());
        tefPanel.add(tefFromDateField, gbc_tefFromDateField);

        final JButton load = new JButton("Letöltés");
        final GridBagConstraints gbc_load = new GridBagConstraints();
        gbc_load.insets = new Insets(0, 0, 5, 0);
        gbc_load.weightx = 1.0;
        gbc_load.gridy = 1;
        gbc_load.gridx = 1;
        tefPanel.add(load, gbc_load);

        load.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                load.setEnabled(false);
                jobs.execute(new Downloader(load));
            }
        });

        final JTable resultTable = new JTable();
        resultTable.setModel(getTefBooking());
        resultTable.getColumnModel().getColumn(0).setPreferredWidth(158);
        final GridBagConstraints gbc_resultTable = new GridBagConstraints();
        gbc_resultTable.insets = new Insets(0, 0, 5, 0);
        gbc_resultTable.fill = GridBagConstraints.BOTH;
        gbc_resultTable.gridy = 2;
        gbc_resultTable.gridx = 0;
        gbc_resultTable.gridwidth = 2;
        gbc_resultTable.weighty = 1.0;
        gbc_resultTable.weightx = 1.0;
        tefPanel.add(new JScrollPane(resultTable), gbc_resultTable);

        final JLabel summaryLabel = new JLabel("Összesítve:");
        final GridBagConstraints gbc_summaryLabel = new GridBagConstraints();
        gbc_summaryLabel.insets = new Insets(0, 0, 0, 5);
        gbc_summaryLabel.anchor = GridBagConstraints.EAST;
        gbc_summaryLabel.gridy = 3;
        gbc_summaryLabel.gridx = 0;
        tefPanel.add(summaryLabel, gbc_summaryLabel);

        tefSummaryField = new JTextField(getSummaryDocumentModel(), null, 0);
        summaryLabel.setLabelFor(tefSummaryField);
        tefSummaryField.setEditable(false);
        final GridBagConstraints gbc_tefSummaryField = new GridBagConstraints();
        gbc_tefSummaryField.fill = GridBagConstraints.HORIZONTAL;
        gbc_tefSummaryField.gridx = 1;
        gbc_tefSummaryField.gridy = 3;
        tefPanel.add(tefSummaryField, gbc_tefSummaryField);

    }

    private void createAutoPartnerPanel(final JPanel settingsPanel) {
        final JPanel autoPartnerPanel = new JPanel();
        autoPartnerPanel.setBorder(new TitledBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null), "Auto-partner.net", TitledBorder.LEADING,
                TitledBorder.TOP, null, null));
        final GridBagConstraints gbc_autoPartnerPanel = new GridBagConstraints();
        gbc_autoPartnerPanel.weighty = 1.0;
        gbc_autoPartnerPanel.weightx = 1.0;
        gbc_autoPartnerPanel.fill = GridBagConstraints.BOTH;
        gbc_autoPartnerPanel.gridx = 0;
        gbc_autoPartnerPanel.gridy = 0;
        settingsPanel.add(autoPartnerPanel, gbc_autoPartnerPanel);
        final GridBagLayout gbl_autoPartnerPanel = new GridBagLayout();
        autoPartnerPanel.setLayout(gbl_autoPartnerPanel);

        final JLabel apUsernameLabel = new JLabel("Felhasználó név");
        final GridBagConstraints gbc_apUsernameLabel = new GridBagConstraints();
        gbc_apUsernameLabel.weightx = 0.5;
        gbc_apUsernameLabel.gridx = 0;
        gbc_apUsernameLabel.gridy = 0;
        autoPartnerPanel.add(apUsernameLabel, gbc_apUsernameLabel);

        apUsernameField = new JTextField();
        apUsernameLabel.setLabelFor(apUsernameField);
        final GridBagConstraints gbc_apUsernameField = new GridBagConstraints();
        gbc_apUsernameField.fill = GridBagConstraints.HORIZONTAL;
        gbc_apUsernameField.weightx = 1.0;
        gbc_apUsernameField.gridx = 1;
        gbc_apUsernameField.gridy = 0;
        autoPartnerPanel.add(apUsernameField, gbc_apUsernameField);

        final JLabel apPasswordLabel = new JLabel("Jelszó");
        final GridBagConstraints gbc_apPasswordLabel = new GridBagConstraints();
        gbc_apPasswordLabel.weightx = 0.5;
        gbc_apPasswordLabel.gridx = 0;
        gbc_apPasswordLabel.gridy = 1;
        autoPartnerPanel.add(apPasswordLabel, gbc_apPasswordLabel);

        apPasswordField = new JPasswordField();
        apPasswordLabel.setLabelFor(apPasswordField);
        final GridBagConstraints gbc_apPasswordField = new GridBagConstraints();
        gbc_apPasswordField.fill = GridBagConstraints.HORIZONTAL;
        gbc_apPasswordField.weightx = 1.0;
        gbc_apPasswordField.gridx = 1;
        gbc_apPasswordField.gridy = 1;
        autoPartnerPanel.add(apPasswordField, gbc_apPasswordField);
    }

    private void createQsSettingsPanel(final JPanel settingsPanel) {
        final JPanel qsPanel = new JPanel();
        qsPanel.setBorder(new TitledBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null), "Qualysoft", TitledBorder.LEADING, TitledBorder.TOP,
                null, null));
        final GridBagConstraints gbc_qsPanel = new GridBagConstraints();
        gbc_qsPanel.weightx = 1.0;
        gbc_qsPanel.fill = GridBagConstraints.BOTH;
        gbc_qsPanel.weighty = 1.0;
        gbc_qsPanel.gridx = 0;
        gbc_qsPanel.gridy = 1;
        settingsPanel.add(qsPanel, gbc_qsPanel);
        final GridBagLayout gbl_qsPanel = new GridBagLayout();
        qsPanel.setLayout(gbl_qsPanel);

        final JLabel qsUsernameLabel = new JLabel("Felhasználó név");
        final GridBagConstraints gbc_qsUsernameLabel = new GridBagConstraints();
        gbc_qsUsernameLabel.weightx = 0.5;
        gbc_qsUsernameLabel.gridx = 0;
        gbc_qsUsernameLabel.gridy = 0;
        qsPanel.add(qsUsernameLabel, gbc_qsUsernameLabel);

        qsUsernameField = new JTextField();
        final GridBagConstraints gbc_qsUsernameField = new GridBagConstraints();
        gbc_qsUsernameField.weightx = 1.0;
        gbc_qsUsernameField.fill = GridBagConstraints.HORIZONTAL;
        gbc_qsUsernameField.gridx = 1;
        gbc_qsUsernameField.gridy = 0;
        qsPanel.add(qsUsernameField, gbc_qsUsernameField);

        final JLabel qsPasswordLabel = new JLabel("Jelszó");
        final GridBagConstraints gbc_qsPasswordLabel = new GridBagConstraints();
        gbc_qsPasswordLabel.weightx = 0.5;
        gbc_qsPasswordLabel.gridy = 1;
        gbc_qsPasswordLabel.gridx = 0;
        qsPanel.add(qsPasswordLabel, gbc_qsPasswordLabel);

        qsPasswordField = new JPasswordField();
        qsPasswordLabel.setLabelFor(qsPasswordField);
        final GridBagConstraints gbc_qsPasswordField = new GridBagConstraints();
        gbc_qsPasswordField.weightx = 1.0;
        gbc_qsPasswordField.fill = GridBagConstraints.HORIZONTAL;
        gbc_qsPasswordField.gridx = 1;
        gbc_qsPasswordField.gridy = 1;
        qsPanel.add(qsPasswordField, gbc_qsPasswordField);
    }

    private synchronized MaskFormatter getFormatter() {
        if (formatter == null) {
            try {
                formatter = new MaskFormatter("####-##-##");
            } catch (final ParseException e) {
                throw new RuntimeException(e);
            }
        }
        return formatter;
    }

    private synchronized TefBookingTableModel getTefBooking() {
        if (tefBooking == null) {
            tefBooking = new TefBookingTableModel();
        }
        return tefBooking;
    }

    private synchronized DaxTableModel getDaxTableModel() {
        if (daxTableModel == null) {
            daxTableModel = new DaxTableModel();
        }
        return daxTableModel;
    }

    private void setupListeners() {
        final DelayedStoreListener ds = new DelayedStoreListener();
        apUsernameField.getDocument().addDocumentListener(ds);
        apPasswordField.getDocument().addDocumentListener(ds);
        qsUsernameField.getDocument().addDocumentListener(ds);
        qsPasswordField.getDocument().addDocumentListener(ds);

    }

    private void loadValues() {
        LOG.info("loading values");
        final Preferences preferences = Preferences.userNodeForPackage(MainWindow.class);
        apUsernameField.setText(preferences.get("apUsername", ""));
        apPasswordField.setText(preferences.get("apPassword", ""));
        qsUsernameField.setText(preferences.get("qsUsername", ""));
        qsPasswordField.setText(preferences.get("qsPassword", ""));

        final Calendar c = Calendar.getInstance();
        c.set(Calendar.DAY_OF_MONTH, 1);
        tefFromDateField.setValue(c.getTime());

        daxFromDateField.setValue(c.getTime());
        daxEndDateField.setValue(new Date());
    }

    private void storeValues() {
        LOG.info("storing changed values");
        final Preferences preferences = Preferences.userNodeForPackage(MainWindow.class);
        preferences.put("apUsername", apUsernameField.getText());
        preferences.put("apPassword", new String(apPasswordField.getPassword()));
        preferences.put("qsUsername", qsUsernameField.getText());
        preferences.put("qsPassword", new String(qsPasswordField.getPassword()));
    }

    private Controller getController() {
        if (controller == null) {
            controller = new Controller(getTefBooking(), getDaxTableModel());
        }
        return controller;
    }

    private PlainDocument getSummaryDocumentModel() {
        return getController().getSummaryDocumentModel();
    }

    /** Returns an ImageIcon, or null if the path was invalid. */
    private static ImageIcon createImageIcon(String path) {
        final URL imgURL = MainWindow.class.getResource(path);
        if (imgURL != null) {
            return new ImageIcon(imgURL);
        } else {
            throw new RuntimeException("Image not found!");
        }
    }

    class Downloader extends ButtonTask {

        public Downloader(JButton button) {
            super(button);
        }

        @Override
        public void executeTask() {
            LOG.info("tef download starts...");
            if (getController().getTef().setLogin(apUsernameField.getText(), apPasswordField.getPassword())) {
                final Object value = tefFromDateField.getValue();
                LOG.info("getting bookings since " + value);
                getController().loadBookings((Date) value);
            } else {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        JOptionPane.showMessageDialog(MainWindow.this.frmTefgin, "Belépés nem sikerült!");
                    }
                });
            }
        }
    }

    class DAXProjectCodeLoader extends ButtonTask {

        public DAXProjectCodeLoader(JButton button) {
            super(button);
        }

        @Override
        public void executeTask() throws Exception {
            if (getController().getDax().setLogin(qsUsernameField.getText(), qsPasswordField.getPassword())) {
                final Object start = daxFromDateField.getValue();
                final Object end = daxEndDateField.getValue();
                LOG.info("getting activities, and not booked days from DAX between:" + start + " and " + end);
                getController().setupDaxDateRanges((Date) start, (Date) end);
            }
        }

    }

    class BookTasks extends ButtonTask {
        public BookTasks(JButton button) {
            super(button);
        }

        @Override
        protected void executeTask() throws Exception {
            final ProjectListVO project = (ProjectListVO) daxProjectPickerField.getSelectedItem();
            final ActivityListVO activity = (ActivityListVO) daxActivityPickerField.getSelectedItem();
            final String note = noteField.getText();
            getController().bookTasks(project, activity, note);
        }
    }

    class DelayedStore implements Runnable {

        @Override
        public void run() {
            try {
                Thread.sleep(5000);
            } catch (final InterruptedException e) {
                e.printStackTrace();
            }
            synchronized (MainWindow.this) {
                if (changed) {
                    storeValues();
                    changed = false;
                }
            }
        }
    }

    class DelayedStoreListener implements DocumentListener {

        @Override
        public void insertUpdate(DocumentEvent e) {
            scheduleSave();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            scheduleSave();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            scheduleSave();
        }

        void scheduleSave() {
            synchronized (MainWindow.this) {
                LOG.info("delayed save...");
                if (!changed) {
                    changed = true;
                    jobs.execute(new DelayedStore());
                }
            }
        }
    }

    class ProjectListRenderer extends BasicListRenderer<ProjectListVO> {

        @Override
        protected String getLabelFor(ProjectListVO value) {
            return value.getNameField();
        }
    }

    class ActivityListRenderer extends BasicListRenderer<ActivityListVO> {

        @Override
        protected String getLabelFor(ActivityListVO value) {
            return value.getNameField();

        }
    }

    class DateChangeListener implements PropertyChangeListener {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            final Object start = daxFromDateField.getValue();
            final Object end = daxEndDateField.getValue();
            LOG.info("change ranges between:" + start + " and " + end);
            getController().changeDateRanges((Date) start, (Date) end);
        }
    }
}

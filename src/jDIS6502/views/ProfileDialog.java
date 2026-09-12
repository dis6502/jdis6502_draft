package jDIS6502.views;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;

import dis6502.core.Profile;

/**
 * "View" -&gt; "Profile...": edits every setting in dis6502.core.Profile (directive spelling,
 * number/string formatting, include-file layout, ...), matching profile_dialog.png. "Load
 * Profile..." and "Save Profile..." (load_profile_dialog.png / save_profile.png) read/write a
 * standalone *.prf file via Profile.load/Profile.save -- exactly the same XML format as the
 * project's shipped profiles/*.prf files -- without needing OK to be pressed first.
 *
 * Usage: new ProfileDialog(owner, workspace.getProfile()).showDialog() returns true if the user
 * pressed OK, in which case the passed-in Profile has already been updated in place.
 */
public final class ProfileDialog extends JDialog {

    private final Profile profile;
    private boolean okPressed = false;

    // --- General ---
    private final JTextField fieldCommentPrefix = new JTextField(4);
    private final JTextField fieldHexNotationPrefix = new JTextField(4);
    private final JCheckBox checkIllegalInstructions = new JCheckBox("Illegal Instructions");
    private final JCheckBox checkUseHexNotation = new JCheckBox("Use Hex Notation");
    private final JCheckBox checkAlignInstructions = new JCheckBox("Align Instructions");
    private final JCheckBox checkUseLineNumbers = new JCheckBox("Use Line Numbers");
    private final JCheckBox checkShowLowerCase = new JCheckBox("Show Lowercase Instructions");
    private final JCheckBox checkShowAInAccumulatorMode = new JCheckBox("Show 'A' in Accumulator Mode");
    private final JCheckBox checkShowColonAfterLabel = new JCheckBox("Show ':' after Labels");
    private final JCheckBox checkShowOpcodeAsComment = new JCheckBox("Show Opcode as Comment");
    private final JCheckBox checkShowBRKAsByte0 = new JCheckBox("Show 'BRK' as '.BYTE $00'");
    private final JCheckBox checkShowZPAbsoluteAsByte = new JCheckBox("Show ZP Addr. in Absolute Mode as Bytes");
    private final JTextField fieldForceAbsolute = new JTextField(6);
    private final JCheckBox checkShowNonASCIIAsBytes = new JCheckBox("Show Non-ASCII Characters as Bytes");
    private final JTextField fieldBytesPerLine = new JTextField(6);
    private final JTextField fieldWordsPerLine = new JTextField(6);
    private final JTextField fieldCharsPerString = new JTextField(6);
    private final JTextField fieldQuoteForStrings = new JTextField(4);

    // --- Directive Syntax ---
    private final JCheckBox checkOnlyNumbersInByte = new JCheckBox("Only Numbers in .BYTE");
    private final JTextField fieldDirectiveBYTE = new JTextField(6);
    private final JTextField fieldByteSeparator = new JTextField(3);
    private final JTextField fieldORG = new JTextField(6);
    private final JTextField fieldEQU = new JTextField(6);
    private final JTextField fieldEndHead = new JTextField(8);
    private final JCheckBox checkEndNeedsFilename = new JCheckBox("Add File Name");
    private final JCheckBox checkWordAllowed = new JCheckBox(".WORD Allowed");
    private final JTextField fieldWORD = new JTextField(6);
    private final JTextField fieldLowByteHead = new JTextField(4);
    private final JTextField fieldLowByteTail = new JTextField(4);
    private final JTextField fieldHighByteHead = new JTextField(4);
    private final JTextField fieldHighByteTail = new JTextField(4);
    private final JCheckBox checkSByteAllowed = new JCheckBox(".SBYTE Allowed");
    private final JTextField fieldSBYTE = new JTextField(6);
    private final JCheckBox checkDsAllowed = new JCheckBox(".DS (Data Storage) Allowed");
    private final JTextField fieldDS = new JTextField(6);

    // --- Disassembly Listing ---
    private final JCheckBox checkOmitUnreferenced = new JCheckBox("Omit Unreferenced System Labels");
    private final JCheckBox checkIncludeFilesAllowed = new JCheckBox("Include Files Allowed");
    private final JTextField fieldIncludeHead = new JTextField(6);
    private final JTextField fieldIncludeTail = new JTextField(4);
    private final JRadioButton radioOneFileEquates = new JRadioButton("One File for Equates included in Main File");
    private final JRadioButton radioAllFilesMain = new JRadioButton("All Files Included in Main File");
    private final JRadioButton radioEachFileNext = new JRadioButton("Each File Includes Next File");
    private final JTextField fieldMaxLinesPerFile = new JTextField(6);

    public ProfileDialog(java.awt.Frame owner, Profile profile) {
        super(owner, "Profile", Dialog.ModalityType.APPLICATION_MODAL);
        this.profile = profile;

        ButtonGroup includeLayoutGroup = new ButtonGroup();
        includeLayoutGroup.add(radioOneFileEquates);
        includeLayoutGroup.add(radioAllFilesMain);
        includeLayoutGroup.add(radioEachFileNext);

        JPanel content = new JPanel(new BorderLayout());
        JPanel topPanels = new JPanel(new GridLayout(1, 2));
        topPanels.add(buildGeneralPanel());
        JPanel rightColumn = new JPanel(new BorderLayout());
        rightColumn.add(buildDirectiveSyntaxPanel(), BorderLayout.NORTH);
        rightColumn.add(buildDisassemblyListingPanel(), BorderLayout.CENTER);
        topPanels.add(rightColumn);
        content.add(topPanels, BorderLayout.CENTER);
        content.add(buildButtonBar(), BorderLayout.SOUTH);

        setContentPane(content);
        loadFromProfile(profile);

        pack();
        setLocationRelativeTo(owner);
    }

    /** Shows the dialog modally; returns true if OK was pressed (the Profile has already been updated). */
    public boolean showDialog() {
        setVisible(true);
        return okPressed;
    }

    // =====================================================================================
    // Panel construction
    // =====================================================================================

    private JPanel buildGeneralPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("General"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 4, 2, 4);
        gbc.anchor = GridBagConstraints.WEST;
        int row = 0;

        row = addLabeledField(panel, gbc, row, "Comment:", fieldCommentPrefix);
        row = addLabeledField(panel, gbc, row, "Hex Prefix:", fieldHexNotationPrefix);
        row = addCheckboxRow(panel, gbc, row, checkIllegalInstructions);
        row = addCheckboxRow(panel, gbc, row, checkUseHexNotation);
        row = addCheckboxRow(panel, gbc, row, checkAlignInstructions);
        row = addCheckboxRow(panel, gbc, row, checkUseLineNumbers);
        row = addCheckboxRow(panel, gbc, row, checkShowLowerCase);
        row = addCheckboxRow(panel, gbc, row, checkShowAInAccumulatorMode);
        row = addCheckboxRow(panel, gbc, row, checkShowColonAfterLabel);
        row = addCheckboxRow(panel, gbc, row, checkShowOpcodeAsComment);
        row = addCheckboxRow(panel, gbc, row, checkShowBRKAsByte0);
        row = addCheckboxRow(panel, gbc, row, checkShowZPAbsoluteAsByte);
        row = addLabeledField(panel, gbc, row, "Mnemonic to force Absolute Mode:", fieldForceAbsolute);
        row = addCheckboxRow(panel, gbc, row, checkShowNonASCIIAsBytes);
        row = addLabeledField(panel, gbc, row, "Number of Byte Values per Line:", fieldBytesPerLine);
        row = addLabeledField(panel, gbc, row, "Number of Word Values per Line:", fieldWordsPerLine);
        row = addLabeledField(panel, gbc, row, "Number of Characters per String:", fieldCharsPerString);
        row = addLabeledField(panel, gbc, row, "Quote for ASCII Strings", fieldQuoteForStrings);

        return panel;
    }

    private JPanel buildDirectiveSyntaxPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Directive Syntax"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 4, 2, 4);
        gbc.anchor = GridBagConstraints.WEST;
        int row = 0;

        row = addCheckboxRow(panel, gbc, row, checkOnlyNumbersInByte);
        row = addTwoFieldRow(panel, gbc, row, ".BYTE:", fieldDirectiveBYTE, "Sep:", fieldByteSeparator);
        row = addLabeledField(panel, gbc, row, ".ORG:", fieldORG);
        row = addLabeledField(panel, gbc, row, "EQU:", fieldEQU);
        row = addTwoFieldRow(panel, gbc, row, ".END:", fieldEndHead, null, null);
        row = addCheckboxRow(panel, gbc, row, checkEndNeedsFilename);
        row = addTwoFieldRow(panel, gbc, row, null, checkWordAllowed, null, fieldWORD);
        row = addTwoFieldRow(panel, gbc, row, "Low Byte:", fieldLowByteHead, "ADDR", fieldLowByteTail);
        row = addTwoFieldRow(panel, gbc, row, "High Byte:", fieldHighByteHead, "ADDR", fieldHighByteTail);
        row = addTwoFieldRow(panel, gbc, row, null, checkSByteAllowed, null, fieldSBYTE);
        row = addTwoFieldRow(panel, gbc, row, null, checkDsAllowed, null, fieldDS);

        return panel;
    }

    private JPanel buildDisassemblyListingPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Disassembly Listing"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 4, 2, 4);
        gbc.anchor = GridBagConstraints.WEST;
        int row = 0;

        row = addCheckboxRow(panel, gbc, row, checkOmitUnreferenced);
        row = addCheckboxRow(panel, gbc, row, checkIncludeFilesAllowed);
        row = addTwoFieldRow(panel, gbc, row, ".INCLUDE:", fieldIncludeHead, "FILENAME", fieldIncludeTail);
        row = addFullWidthRow(panel, gbc, row, radioOneFileEquates);
        row = addFullWidthRow(panel, gbc, row, radioAllFilesMain);
        row = addFullWidthRow(panel, gbc, row, radioEachFileNext);
        row = addLabeledField(panel, gbc, row, "Maximum Number of Lines per Include File:", fieldMaxLinesPerFile);

        return panel;
    }

    private JPanel buildButtonBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.CENTER));

        JButton loadProfile = new JButton("Load Profile...");
        loadProfile.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                handleLoadProfile();
            }
        });
        bar.add(loadProfile);

        JButton saveProfile = new JButton("Save Profile...");
        saveProfile.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                handleSaveProfile();
            }
        });
        bar.add(saveProfile);

        JButton ok = new JButton("OK");
        ok.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                handleOk();
            }
        });
        bar.add(ok);

        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                okPressed = false;
                setVisible(false);
            }
        });
        bar.add(cancel);

        return bar;
    }

    // --- Small GridBagLayout helpers (row builders return the next free row index). ---

    private int addLabeledField(JPanel panel, GridBagConstraints gbc, int row, String label, JTextField field) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 1;
        panel.add(new JLabel(label), gbc);
        gbc.gridx = 1;
        panel.add(field, gbc);
        return row + 1;
    }

    private int addCheckboxRow(JPanel panel, GridBagConstraints gbc, int row, JCheckBox checkBox) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        panel.add(checkBox, gbc);
        gbc.gridwidth = 1;
        return row + 1;
    }

    private int addFullWidthRow(JPanel panel, GridBagConstraints gbc, int row, JComponent component) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        panel.add(component, gbc);
        gbc.gridwidth = 1;
        return row + 1;
    }

    /**
     * One row with up to two label+field (or checkbox/field) pairs, e.g. ".BYTE: [.byte] Sep: [,]"
     * or "Low Byte: [<] ADDR[]" -- either label may be null to just place its component directly
     * (e.g. a checkbox standing in for the first label, as used for ".WORD Allowed [x] [.word]").
     */
    private int addTwoFieldRow(JPanel panel, GridBagConstraints gbc, int row,
            String label1, JComponent field1, String label2, JComponent field2) {
        JPanel rowPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        if (label1 != null) {
            rowPanel.add(new JLabel(label1));
        }
        rowPanel.add(field1);
        if (label2 != null) {
            rowPanel.add(new JLabel(label2));
        }
        if (field2 != null) {
            rowPanel.add(field2);
        }
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        panel.add(rowPanel, gbc);
        gbc.gridwidth = 1;
        return row + 1;
    }

    // =====================================================================================
    // Profile <-> widgets
    // =====================================================================================

    private void loadFromProfile(Profile p) {
        fieldCommentPrefix.setText(p.commentPrefix);
        fieldHexNotationPrefix.setText(p.hexNotationPrefix);
        checkIllegalInstructions.setSelected(p.useIllegalOpcodes);
        checkUseHexNotation.setSelected(p.useHexNotation);
        checkAlignInstructions.setSelected(p.alignInstructions);
        checkUseLineNumbers.setSelected(p.useLineNumbers);
        checkShowLowerCase.setSelected(p.showLowerCaseInstructions);
        checkShowAInAccumulatorMode.setSelected(p.showAInAccumulatorMode);
        checkShowColonAfterLabel.setSelected(p.showColonAfterLabel);
        checkShowOpcodeAsComment.setSelected(p.showOpcodeAsComment);
        checkShowBRKAsByte0.setSelected(p.showBRKAsByte0);
        checkShowZPAbsoluteAsByte.setSelected(p.showZPAbsoluteAsByte);
        fieldForceAbsolute.setText(p.directiveForceAbsolute);
        checkShowNonASCIIAsBytes.setSelected(p.showNonASCIIChararactersAsBytes);
        fieldBytesPerLine.setText(String.valueOf(p.directiveBYTENumberOfBytesPerLine));
        fieldWordsPerLine.setText(String.valueOf(p.directiveWORDNumberOfWordsPerLine));
        fieldCharsPerString.setText(String.valueOf(p.directiveBYTENumberOfCharactersPerString));
        fieldQuoteForStrings.setText(p.quoteForASCIIStrings);

        checkOnlyNumbersInByte.setSelected(p.directiveBYTEOnlyNumbersAllowed);
        fieldDirectiveBYTE.setText(p.directiveBYTE);
        fieldByteSeparator.setText(p.directiveBYTESeparator);
        fieldORG.setText(p.directiveORG);
        fieldEQU.setText(p.directiveEQU);
        fieldEndHead.setText(p.directiveENDHead);
        checkEndNeedsFilename.setSelected(p.directiveENDNeedsFilename);
        checkWordAllowed.setSelected(p.directiveWORDAllowed);
        fieldWORD.setText(p.directiveWORD);
        fieldLowByteHead.setText(p.directiveLOWHead);
        fieldLowByteTail.setText(p.directiveLOWTail);
        fieldHighByteHead.setText(p.directiveHIGHHead);
        fieldHighByteTail.setText(p.directiveHIGHTail);
        checkSByteAllowed.setSelected(p.directiveSBYTEAllowed);
        fieldSBYTE.setText(p.directiveSBYTE);
        checkDsAllowed.setSelected(p.directiveDSAllowed);
        fieldDS.setText(p.directiveDS);

        checkOmitUnreferenced.setSelected(p.omitUnreferencedSystemLabels);
        checkIncludeFilesAllowed.setSelected(p.directiveINCLUDEAllowed);
        fieldIncludeHead.setText(p.directiveINCLUDEHead);
        fieldIncludeTail.setText(p.directiveINCLUDETail);
        if (p.directiveINCLUDEAllIncludesInMainFile) {
            radioAllFilesMain.setSelected(true);
        } else if (p.directiveINCLUDEAllEquatesInOneIncludeFile) {
            radioOneFileEquates.setSelected(true);
        } else {
            radioEachFileNext.setSelected(true);
        }
        fieldMaxLinesPerFile.setText(String.valueOf(p.directiveINCLUDEMaximumNumberOfLinesPerFile));
    }

    /** Returns null on success, or an error message (leaving p untouched) if a numeric field is invalid. */
    private String applyToProfile(Profile p) {
        int bytesPerLine, wordsPerLine, charsPerString, maxLinesPerFile;
        try {
            bytesPerLine = parseWord(fieldBytesPerLine.getText(), "Number of Byte Values per Line");
            wordsPerLine = parseWord(fieldWordsPerLine.getText(), "Number of Word Values per Line");
            charsPerString = parseWord(fieldCharsPerString.getText(), "Number of Characters per String");
            maxLinesPerFile = parseWord(fieldMaxLinesPerFile.getText(), "Maximum Number of Lines per Include File");
        } catch (NumberFormatException e) {
            return e.getMessage();
        }

        p.commentPrefix = fieldCommentPrefix.getText();
        p.hexNotationPrefix = fieldHexNotationPrefix.getText();
        p.useIllegalOpcodes = checkIllegalInstructions.isSelected();
        p.useHexNotation = checkUseHexNotation.isSelected();
        p.alignInstructions = checkAlignInstructions.isSelected();
        p.useLineNumbers = checkUseLineNumbers.isSelected();
        p.showLowerCaseInstructions = checkShowLowerCase.isSelected();
        p.showAInAccumulatorMode = checkShowAInAccumulatorMode.isSelected();
        p.showColonAfterLabel = checkShowColonAfterLabel.isSelected();
        p.showOpcodeAsComment = checkShowOpcodeAsComment.isSelected();
        p.showBRKAsByte0 = checkShowBRKAsByte0.isSelected();
        p.showZPAbsoluteAsByte = checkShowZPAbsoluteAsByte.isSelected();
        p.directiveForceAbsolute = fieldForceAbsolute.getText();
        p.showNonASCIIChararactersAsBytes = checkShowNonASCIIAsBytes.isSelected();
        p.directiveBYTENumberOfBytesPerLine = bytesPerLine;
        p.directiveWORDNumberOfWordsPerLine = wordsPerLine;
        p.directiveBYTENumberOfCharactersPerString = charsPerString;
        p.quoteForASCIIStrings = fieldQuoteForStrings.getText();

        p.directiveBYTEOnlyNumbersAllowed = checkOnlyNumbersInByte.isSelected();
        p.directiveBYTE = fieldDirectiveBYTE.getText();
        p.directiveBYTESeparator = fieldByteSeparator.getText();
        p.directiveORG = fieldORG.getText();
        p.directiveEQU = fieldEQU.getText();
        p.directiveENDHead = fieldEndHead.getText();
        p.directiveENDNeedsFilename = checkEndNeedsFilename.isSelected();
        p.directiveWORDAllowed = checkWordAllowed.isSelected();
        p.directiveWORD = fieldWORD.getText();
        p.directiveLOWHead = fieldLowByteHead.getText();
        p.directiveLOWTail = fieldLowByteTail.getText();
        p.directiveHIGHHead = fieldHighByteHead.getText();
        p.directiveHIGHTail = fieldHighByteTail.getText();
        p.directiveSBYTEAllowed = checkSByteAllowed.isSelected();
        p.directiveSBYTE = fieldSBYTE.getText();
        p.directiveDSAllowed = checkDsAllowed.isSelected();
        p.directiveDS = fieldDS.getText();

        p.omitUnreferencedSystemLabels = checkOmitUnreferenced.isSelected();
        p.directiveINCLUDEAllowed = checkIncludeFilesAllowed.isSelected();
        p.directiveINCLUDEHead = fieldIncludeHead.getText();
        p.directiveINCLUDETail = fieldIncludeTail.getText();
        p.directiveINCLUDEAllIncludesInMainFile = radioAllFilesMain.isSelected();
        p.directiveINCLUDEAllEquatesInOneIncludeFile = radioOneFileEquates.isSelected();
        p.directiveINCLUDEMaximumNumberOfLinesPerFile = maxLinesPerFile;

        return null;
    }

    private int parseWord(String text, String fieldLabel) throws NumberFormatException {
        int value;
        try {
            value = Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            throw new NumberFormatException("\"" + fieldLabel + "\" must be a whole number.");
        }
        if (value < 0 || value > 0xFFFF) {
            throw new NumberFormatException("\"" + fieldLabel + "\" must be between 0 and 65535.");
        }
        return value;
    }

    // =====================================================================================
    // Button handlers
    // =====================================================================================

    private void handleOk() {
        String error = applyToProfile(profile);
        if (error != null) {
            JOptionPane.showMessageDialog(this, error, "Profile", JOptionPane.ERROR_MESSAGE);
            return;
        }
        okPressed = true;
        setVisible(false);
    }

    /**
     * "Load Profile...": matches load_profile_dialog.png (title "Open Profile File", filtered to
     * "Profile Files (*.prf)", defaulting to the "profiles" folder shipped with this project --
     * see AppPaths). Loads straight into this dialog's fields; OK/Cancel still apply afterward,
     * same as the screenshot's dialog (the file chooser has its own Open/Cancel, separate from
     * this dialog's own OK/Cancel).
     */
    private void handleLoadProfile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Open Profile File");
        chooser.setFileFilter(new FileNameExtensionFilter("Profile Files (*.prf)", "prf"));
        File profilesDir = AppPaths.findExistingDirectory("profiles");
        if (profilesDir != null) {
            chooser.setCurrentDirectory(profilesDir);
        }

        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        Profile loaded = new Profile();
        try {
            loaded.load(chooser.getSelectedFile().getAbsolutePath());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                    "Could not load \"" + chooser.getSelectedFile().getAbsolutePath() + "\":\n" + e.getMessage(),
                    "Load Profile", JOptionPane.ERROR_MESSAGE);
            return;
        }
        loadFromProfile(loaded);
    }

    /**
     * "Save Profile...": matches save_profile.png (title "Save Profile File", filtered to
     * "Profile Files (*.prf)"). Saves this dialog's current field values (as if OK had been
     * pressed), not the profile's last-applied state, and appends ".prf" if the typed file name
     * doesn't already have an extension.
     */
    private void handleSaveProfile() {
        Profile toSave = new Profile();
        String error = applyToProfile(toSave);
        if (error != null) {
            JOptionPane.showMessageDialog(this, error, "Profile", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save Profile File");
        chooser.setFileFilter(new FileNameExtensionFilter("Profile Files (*.prf)", "prf"));
        File profilesDir = AppPaths.findExistingDirectory("profiles");
        if (profilesDir != null) {
            chooser.setCurrentDirectory(profilesDir);
        }

        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File file = chooser.getSelectedFile();
        if (file.getName().indexOf('.') < 0) {
            file = new File(file.getParentFile(), file.getName() + ".prf");
        }

        try {
            toSave.save(file.getAbsolutePath());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Could not save \"" + file.getAbsolutePath() + "\":\n" + e.getMessage(),
                    "Save Profile", JOptionPane.ERROR_MESSAGE);
        }
    }
}

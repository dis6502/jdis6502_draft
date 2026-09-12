package jDIS6502.views;

import java.awt.BorderLayout;
import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.JPanel;
import javax.swing.BorderFactory;
import javax.swing.border.EmptyBorder;
import java.awt.Toolkit;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JSeparator;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JComboBox;

import java.awt.Color;
import javax.swing.JScrollPane;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.JTextField;
import javax.swing.UIManager;
import java.awt.event.*;
import java.awt.*;
import java.awt.Font;
import java.awt.Component;

import javax.swing.JTextArea;
import javax.swing.SpringLayout;
import javax.swing.JList;
import javax.swing.JSplitPane;
import javax.swing.border.LineBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

import java.awt.FlowLayout;
import java.awt.Rectangle;
import java.awt.Point;
import javax.swing.JTable;
import jDIS6502.views.CustomAboutDialog;
import javax.swing.JOptionPane;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JPopupMenu;
import jDIS6502.views.*;
import jDIS6502.common.MainGlobals;
import jDIS6502.common.Utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.text.Highlighter;
import javax.swing.text.JTextComponent;
import javax.swing.text.DefaultHighlighter;
import javax.swing.JTextPane;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.AbstractAction;
import javax.swing.KeyStroke;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import dis6502.core.Assembler;
import dis6502.core.AtariExecutableReader;
import dis6502.core.ComputerSystemFactory;
import dis6502.core.ComputerSystemType;
import dis6502.core.Comment;
import dis6502.core.Debug;
import dis6502.core.Disassembly;
import dis6502.core.DisassemblyLine;
import dis6502.core.DisassemblyProgressMonitor;
import dis6502.core.DisassemblySectionType;
import dis6502.core.FileHeader;
import dis6502.core.FileIO;
import dis6502.core.InstructionSet;
import dis6502.core.MemoryType;
import dis6502.core.ProcessorType;
import dis6502.core.Segment;
import dis6502.core.SegmentList;
import dis6502.core.SegmentListChangedListener;
import dis6502.core.Workspace;
import dis6502.core.WorkspaceChangedListener;
import dis6502.core.WorkspaceProperty;


public class Dis6502Gui extends JFrame {

	private JPanel contentPane;
	private JMenuBar menuBar;
	private JMenu mnNewMenu;
	private JMenuItem mntmNewWorkspace;
	private JMenuItem mntmOpenworkspace;
	private JMenuItem mntmOpenFile;
	private JMenuItem mntmSaveWorkspace;
	private JMenuItem mntmSaveWorkspaceAs;
	private JMenuItem mntmSaveDisassemblyFiles;
	private JMenuItem mntmSaveDiskImage;
	private JMenuItem mntmRecentWorkspaces;
	private JMenuItem mntmRecentFiles;
	private JMenuItem mntmExit;
	private JMenu mnNewMenu_1;
	private JMenuItem mntmClearSystemEquates;
	private JMenuItem mntmClearUserEquates;
	private JMenuItem mntmEditUserEquates;
	private JMenuItem mntmDefineAddressRange;
	private JMenuItem mntmOpenUserEquates;
	private JMenuItem mntmSaveUserEquates;
	private JMenuItem mntmExportUserEquates;
	private JMenu mnView;
	private JMenuItem mntmDisplayAsScreen;
	private JMenuItem mntmNoDisassembly;
	private JMenuItem mntmDoubleFontHeight;
	private JMenuItem mntmDefaultFolders;
	private JMenuItem mntmProfile;
	private JMenu mnHelp;
	private JMenuItem mntmAbout;
	private JScrollPane scrollPane_FileLoaded;
	private JScrollPane scrollPane_Reference;
	private JScrollPane scrollPane_Dump;
	private JTextField txtNoFileLoaded;
	private JTextField txtDisassembly;
	private JTextField txtNoDump;
	private JTextField txtReferenceList;
	private JTextField txtLog;
	private JScrollPane scrollPane_Disassembly;
	private JScrollPane scrollPane_Log;
	private JTextArea textField_FileLoaded;
	private JTextPane textField_Disassembly;
	private JTextArea textField_Reference;
	private JTextArea textField_Log;
	private JMenuItem mntmOpenRawFile;
	private JMenuItem mntmOpenExecutableFile;
	private JMenuItem mntmOpenRomImage;
	private JMenuItem mntmOpenCassetteImage;
	private JMenuItem mntmOpenDiskImage;
	private JMenuItem mntmOpenDiskImage_1;
	private JMenuItem mntmOpenDiskImage_2;
	private JTable tableDump;
	static JFrame jframe;

	// --- Engine wiring (dis6502.core: the ported C++ disassembly engine). ---
	private Workspace workspace;
	private Disassembly disassembly;
	private HexDumpTableModel hexDumpTableModel;
	private int currentDumpSegmentIndex = -1;

	// --- Hex dump context menu state (see buildDumpPopupMenu/refreshDumpPopupMenuState). ---
	private byte[] dumpClipboard; // In-app-only clipboard for the dump pane's Cut/Copy/Paste, not the system clipboard.
	private byte[] lastFindPattern;
	private final Map<MemoryType, JCheckBoxMenuItem> dumpChangeTypeMenuItems = new HashMap<MemoryType, JCheckBoxMenuItem>();
	private JMenuItem dumpMenuCut;
	private JMenuItem dumpMenuCopy;
	private JMenuItem dumpMenuPasteBefore;
	private JMenuItem dumpMenuPasteAfter;
	private JMenuItem dumpMenuDelete;
	private JMenuItem dumpMenuSplit;
	private JMenuItem dumpMenuFindNext;

	// Linear (not rectangular) byte-range selection state for the hex dump table -- see
	// configureDumpMouseHandling() for why this replaces JTable's default cell-selection model.
	private int dumpSelectionAnchorOffset = -1;
	private int dumpSelectionLeadOffset = -1;

	// Parallel to textField_Disassembly's text: disassemblyLineIndex.get(i) is the
	// DisassemblyLine that produced line i (0-based) of the displayed text, so a click can be
	// mapped back to the DisassemblyLine (and from there its segment/offset/address), which a
	// plain JTextArea has no notion of on its own.
	private final List<DisassemblyLine> disassemblyLineIndex = new ArrayList<DisassemblyLine>();

	// Same idea, parallel to textField_Reference's text: referenceLineIndex.get(i) is the
	// DisassemblyLine that produced line i of the Reference List panel, so clicking a reference
	// can jump both the Disassembly and hex-dump panels to that same address.
	private final List<DisassemblyLine> referenceLineIndex = new ArrayList<DisassemblyLine>();

	/**
	 * A minimal Swing TableModel for the hex-dump panel: one row per 16 bytes of the selected
	 * segment, showing the row's address, 16 hex byte columns, and an ASCII column.
	 */
	private static final class HexDumpTableModel extends AbstractTableModel {
		private static final int BYTES_PER_ROW = 16;
		private Segment segment;
		private int[] effectiveTypeByOffset = new int[0];

		void setSegment(Segment segment) {
			this.segment = segment;
			this.effectiveTypeByOffset = computeEffectiveTypeByOffset(segment);
			fireTableStructureChanged();
		}

		/** The color-palette index to use for offset (see Dis6502Gui.HEX_DUMP_COLOR_PALETTE), or UNKNOWN's index if out of range. */
		int getEffectiveType(int offset) {
			return (offset >= 0 && offset < effectiveTypeByOffset.length) ? effectiveTypeByOffset[offset] : MemoryType.UNKNOWN.toByte();
		}

		/**
		 * Direct translation of the byte-type color-resolution logic in
		 * MemoryInspectorControlImpl::PrintLine (from the user-supplied full C++ project),
		 * computed once per displayed segment instead of once per paint. For every offset, this
		 * decides which MemoryType's color it should actually be drawn with -- almost always its
		 * own type, except: a LOBYTE/HIBYTE marker byte itself is drawn as CODE (it *is* code --
		 * an instruction's operand byte), while the very next byte (which holds the missing half
		 * of the split address as a raw, non-type "value" -- see MemoryType's own class Javadoc)
		 * is drawn using that marker's LOBYTE/HIBYTE color instead of misinterpreting its raw
		 * stored value as if it were a real type. This resolution restarts at each displayed row
		 * (matching PrintLine's own per-row-call local variable), which -- because a fresh row
		 * always behaves as "no chain in progress" until it re-examines the true previous byte --
		 * happens to produce identical results whether or not a marker/value pair straddles a row
		 * boundary, so a plain per-row loop here matches the original exactly.
		 */
		private static int[] computeEffectiveTypeByOffset(Segment segment) {
			if (segment == null) {
				return new int[0];
			}
			int size = segment.getSize();
			int[] effective = new int[size];
			int lobyte = MemoryType.LOBYTE.toByte();
			int hibyte = MemoryType.HIBYTE.toByte();
			int code = MemoryType.CODE.toByte();
			int unknown = MemoryType.UNKNOWN.toByte();
			int typeCount = MemoryType.FIXUP.toByte() + 1; // MEMORY_TYPE_ENUM_ITEM_COUNT

			for (int rowStart = 0; rowStart < size; rowStart += BYTES_PER_ROW) {
				int rowEnd = Math.min(size, rowStart + BYTES_PER_ROW);
				int previousEffectiveType = 0xFF; // Sentinel: matches cOldType's value at the start of each PrintLine call.

				for (int offset = rowStart; offset < rowEnd; offset++) {
					int type = segment.getType(offset).toByte();

					if (offset > 0) {
						int previousRawType = segment.getType(offset - 1).toByte();
						if (previousEffectiveType != lobyte && previousEffectiveType != hibyte
								&& (previousRawType == lobyte || previousRawType == hibyte)) {
							type = previousRawType;
						} else if (type == lobyte || type == hibyte) {
							type = code;
						}
					} else if (type == lobyte || type == hibyte) {
						type = code;
					} else {
						type = unknown;
					}

					if (type >= typeCount) {
						type = unknown;
					}

					previousEffectiveType = type;
					effective[offset] = type;
				}
			}
			return effective;
		}

		public int getRowCount() {
			if (segment == null || segment.isEmpty()) {
				return 0;
			}
			return (segment.getSize() + BYTES_PER_ROW - 1) / BYTES_PER_ROW;
		}

		public int getColumnCount() {
			return 2 + BYTES_PER_ROW;
		}

		public String getColumnName(int column) {
			if (column == 0) {
				return "Address";
			} else if (column <= BYTES_PER_ROW) {
				return String.format("%X", column - 1);
			} else {
				return "ASCII";
			}
		}

		public Object getValueAt(int row, int column) {
			if (segment == null) {
				return "";
			}
			int rowOffset = row * BYTES_PER_ROW;
			if (column == 0) {
				return String.format("%04X", segment.wBegin + rowOffset);
			} else if (column <= BYTES_PER_ROW) {
				int offset = rowOffset + (column - 1);
				if (offset >= segment.getSize()) {
					return "";
				}
				return String.format("%02X", segment.getData(offset));
			} else {
				StringBuilder ascii = new StringBuilder(BYTES_PER_ROW);
				for (int i = 0; i < BYTES_PER_ROW; i++) {
					int offset = rowOffset + i;
					if (offset >= segment.getSize()) {
						break;
					}
					int value = segment.getData(offset);
					ascii.append((value >= 32 && value < 127) ? (char) value : '.');
				}
				return ascii.toString();
			}
		}
	}

	/**
	 * Launch the application. Accepts an optional command-line argument: a path to an Atari
	 * executable file (.xex/.com/.exe/.sys/.bin) to open immediately at startup, equivalent to
	 * using File -&gt; Open Executable File by hand. This is new behavior (not present in the
	 * original C++ app, which has no command-line interface) added because "java -jar
	 * jDIS6502.jar SOMEFILE.COM" is a natural, commonly-expected thing to try.
	 */
	public static void main(final String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					Dis6502Gui frame = new Dis6502Gui();
					jframe = frame;
					frame.setVisible(true);
					if (args.length > 0) {
						frame.openExecutableFile(new File(args[0]));
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the frame.
	 */
	public Dis6502Gui() {
		setMinimumSize(new Dimension(800, 600));
		setBackground(Color.WHITE);
		setIconImage(Toolkit.getDefaultToolkit().getImage(Dis6502Gui.class.getResource("/jDIS6502/resources/dis6502.png")));
		setTitle("  jDIS6502");
		initComponents();
		initEngine();
		createEvents();

	}
	
	/////////////////////////////////////////////////////////////
	// this method contains all of the code for creating
	// and initializing components.
	/////////////////////////////////////////////////////////////
	private void initComponents() {
		
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 1280, 1176);
		
		menuBar = new JMenuBar();
		menuBar.setFont(new Font("Segoe UI", Font.PLAIN, 18));
		menuBar.setBackground(Color.WHITE);
		setJMenuBar(menuBar);
		
		mnNewMenu = new JMenu("File");
		menuBar.add(mnNewMenu);
		
		mntmNewWorkspace = new JMenuItem("New Workspace");
		mntmNewWorkspace.setIcon(new ImageIcon(Dis6502Gui.class.getResource("/jDIS6502/resources/file_new.png")));
		mnNewMenu.add(mntmNewWorkspace);
		
		JSeparator separator = new JSeparator();
		mnNewMenu.add(separator);
		
		mntmOpenworkspace = new JMenuItem("OpenWorkspace");

		mnNewMenu.add(mntmOpenworkspace);

		mntmOpenFile = new JMenu("Open File");
		mnNewMenu.add(mntmOpenFile);
		
		mntmOpenRawFile = new JMenuItem("Open Raw File");

		mntmOpenRawFile.setIcon(new ImageIcon(Dis6502Gui.class.getResource("/jDIS6502/resources/file_open_raw_file.png")));
		mntmOpenFile.add(mntmOpenRawFile);
		
		mntmOpenExecutableFile = new JMenuItem("Open Executable File");

		mntmOpenExecutableFile.setIcon(new ImageIcon(Dis6502Gui.class.getResource("/jDIS6502/resources/file_open_executable_file.png")));
		mntmOpenFile.add(mntmOpenExecutableFile);
		
		mntmOpenRomImage = new JMenuItem("Open ROM Image File");

		mntmOpenRomImage.setIcon(new ImageIcon(Dis6502Gui.class.getResource("/jDIS6502/resources/file_open_rom_image_file.png")));
		mntmOpenFile.add(mntmOpenRomImage);
		
		mntmOpenCassetteImage = new JMenuItem("Open Cassette Image File");

		mntmOpenCassetteImage.setIcon(new ImageIcon(Dis6502Gui.class.getResource("/jDIS6502/resources/file_open_cassette_image_file.png")));
		mntmOpenFile.add(mntmOpenCassetteImage);
		
		mntmOpenDiskImage = new JMenuItem("Open Disk Image Executable File");

		mntmOpenDiskImage.setIcon(new ImageIcon(Dis6502Gui.class.getResource("/jDIS6502/resources/file_open_disk_image_executable_file.png")));
		mntmOpenFile.add(mntmOpenDiskImage);
		
		mntmOpenDiskImage_1 = new JMenuItem("Open Disk Image Boot Sectors");

		mntmOpenDiskImage_1.setIcon(new ImageIcon(Dis6502Gui.class.getResource("/jDIS6502/resources/file_open_disk_image_boot_sectors.png")));
		mntmOpenFile.add(mntmOpenDiskImage_1);
		
		mntmOpenDiskImage_2 = new JMenuItem("Open Disk Image Sectors");

		mntmOpenDiskImage_2.setIcon(new ImageIcon(Dis6502Gui.class.getResource("/jDIS6502/resources/file_open_disk_image_sectors.png")));
		mntmOpenFile.add(mntmOpenDiskImage_2);
		
		JSeparator separator_1 = new JSeparator();
		mnNewMenu.add(separator_1);
		
		mntmSaveWorkspace = new JMenuItem("Save Workspace");

		mnNewMenu.add(mntmSaveWorkspace);
		
		mntmSaveWorkspaceAs = new JMenuItem("Save Workspace As...");

		mnNewMenu.add(mntmSaveWorkspaceAs);
		
		mntmSaveDisassemblyFiles = new JMenuItem("Save Disassembly Files...");

		mntmSaveDisassemblyFiles.setIcon(new ImageIcon(Dis6502Gui.class.getResource("/jDIS6502/resources/file_save_disassembly_files.png")));
		mnNewMenu.add(mntmSaveDisassemblyFiles);
		
		mntmSaveDiskImage = new JMenuItem("Save Disk Image Boot Sectors...");
		mntmSaveDiskImage.setIcon(new ImageIcon(Dis6502Gui.class.getResource("/jDIS6502/resources/file_save_disk_image_boot_sectors.png")));
		mnNewMenu.add(mntmSaveDiskImage);
		
		JSeparator separator_2 = new JSeparator();
		mnNewMenu.add(separator_2);
		
		mntmRecentWorkspaces = new JMenu("Recent Workspaces");
		mnNewMenu.add(mntmRecentWorkspaces);
		
		mntmRecentFiles = new JMenu("Recent Files");
		mnNewMenu.add(mntmRecentFiles);
		
		JSeparator separator_3 = new JSeparator();
		mnNewMenu.add(separator_3);
		
		mntmExit = new JMenuItem("Exit");
		mnNewMenu.add(mntmExit);
		
		mnNewMenu_1 = new JMenu("Labels");
		menuBar.add(mnNewMenu_1);
		
		mntmClearSystemEquates = new JMenuItem("Clear System Equates");
		mnNewMenu_1.add(mntmClearSystemEquates);
		
		JSeparator separator_4 = new JSeparator();
		mnNewMenu_1.add(separator_4);
		
		mntmClearUserEquates = new JMenuItem("Clear User Equates");
		mntmClearUserEquates.setIcon(new ImageIcon(Dis6502Gui.class.getResource("/jDIS6502/resources/labels_clear_user_equates.png")));
		mnNewMenu_1.add(mntmClearUserEquates);
		
		mntmEditUserEquates = new JMenuItem("Edit User Equates...");
		mntmEditUserEquates.setIcon(new ImageIcon(Dis6502Gui.class.getResource("/jDIS6502/resources/labels_edit_user_equates.png")));
		mnNewMenu_1.add(mntmEditUserEquates);
		
		mntmDefineAddressRange = new JMenuItem("Define Address Range...");
		mntmDefineAddressRange.setIcon(new ImageIcon(Dis6502Gui.class.getResource("/jDIS6502/resources/labels_define_user_address_range.png")));
		mnNewMenu_1.add(mntmDefineAddressRange);
		
		JSeparator separator_5 = new JSeparator();
		mnNewMenu_1.add(separator_5);
		
		mntmOpenUserEquates = new JMenuItem("Open User Equates...");
		mntmOpenUserEquates.setIcon(new ImageIcon(Dis6502Gui.class.getResource("/jDIS6502/resources/labels_open_user_equates.png")));
		mnNewMenu_1.add(mntmOpenUserEquates);
		
		mntmSaveUserEquates = new JMenuItem("Save User Equates...");
		mntmSaveUserEquates.setIcon(new ImageIcon(Dis6502Gui.class.getResource("/jDIS6502/resources/labels_save_user_equates.png")));
		mnNewMenu_1.add(mntmSaveUserEquates);
		
		JSeparator separator_6 = new JSeparator();
		mnNewMenu_1.add(separator_6);
		
		mntmExportUserEquates = new JMenuItem("Export User Equates...");
		mntmExportUserEquates.setIcon(new ImageIcon(Dis6502Gui.class.getResource("/jDIS6502/resources/labels_export_user_equates.png")));
		mnNewMenu_1.add(mntmExportUserEquates);
		
		mnView = new JMenu("View");
		menuBar.add(mnView);
		
		mntmDisplayAsScreen = new JMenuItem("Display as Screen code");
		mnView.add(mntmDisplayAsScreen);
		
		mntmNoDisassembly = new JMenuItem("No Disassembly");
		mnView.add(mntmNoDisassembly);
		
		mntmDoubleFontHeight = new JMenuItem("Double Font Height");
		mnView.add(mntmDoubleFontHeight);
		
		JSeparator separator_7 = new JSeparator();
		mnView.add(separator_7);
		
		mntmDefaultFolders = new JMenuItem("Default Folders...");
		mnView.add(mntmDefaultFolders);
		
		mntmProfile = new JMenuItem("Profile...");
		mntmProfile.setIcon(new ImageIcon(Dis6502Gui.class.getResource("/jDIS6502/resources/view_profile.png")));
		mnView.add(mntmProfile);
		
		mnHelp = new JMenu("Help");
		menuBar.add(mnHelp);
		
		
		
		
/*		JButton btnNewButton = new JButton("Create Dialog");
		btnNewButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				System.out.println("Create Dialog");
			}
		});*/
		
		
		
		
		
		mntmAbout = new JMenuItem("About");
//		mntmAbout.addMouseListener(new MouseAdapter() {
//			@Override
//			public void mouseClicked(MouseEvent e) {
//				System.out.println("showMessageDialog");
//				JOptionPane.showMessageDialog(jframe, "Eggs are not supposed to be green.");
//			}
//		});
		mntmAbout.setIcon(new ImageIcon(Dis6502Gui.class.getResource("/jDIS6502/resources/help_about.png")));
		mnHelp.add(mntmAbout);
		contentPane = new JPanel();
		contentPane.setBackground(Color.WHITE);
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		
		scrollPane_Disassembly = new JScrollPane();
		
		scrollPane_FileLoaded = new JScrollPane();
		scrollPane_FileLoaded.setAlignmentX(Component.LEFT_ALIGNMENT);
		
		scrollPane_Reference = new JScrollPane();
		
		scrollPane_Dump = new JScrollPane();
		
		txtNoFileLoaded = new JTextField();
		txtNoFileLoaded.setEditable(false);
		txtNoFileLoaded.setFont(new Font("Tahoma", Font.BOLD, 20));
		txtNoFileLoaded.setBackground(Color.YELLOW);
		txtNoFileLoaded.setText("  No File Loaded");
		txtNoFileLoaded.setColumns(10);
		
		txtDisassembly = new JTextField();
		txtDisassembly.setEditable(false);
		txtDisassembly.setFont(new Font("Tahoma", Font.BOLD, 20));
		txtDisassembly.setText("  Disassembly");
		txtDisassembly.setBackground(Color.GREEN);
		txtDisassembly.setColumns(10);
		
		txtNoDump = new JTextField();
		txtNoDump.setEditable(false);
		txtNoDump.setFont(new Font("Tahoma", Font.BOLD, 20));
		txtNoDump.setBackground(Color.CYAN);
		txtNoDump.setText("  No Dump");
		txtNoDump.setColumns(10);
		
		txtReferenceList = new JTextField();
		txtReferenceList.setEditable(false);
		txtReferenceList.setFont(new Font("Tahoma", Font.BOLD, 20));
		txtReferenceList.setBackground(Color.PINK);
		txtReferenceList.setText("  Reference List");
		txtReferenceList.setColumns(10);
		
		txtLog = new JTextField();
		txtLog.setAlignmentY(Component.TOP_ALIGNMENT);
		txtLog.setAlignmentX(Component.LEFT_ALIGNMENT);
		txtLog.setEditable(false);
		txtLog.setFont(new Font("Tahoma", Font.BOLD, 20));
		txtLog.setBackground(UIManager.getColor("activeCaption"));
		txtLog.setText("  Log");
		txtLog.setColumns(10);
		
		// These five are purely decorative section-header bars ("Segments for X", "Disassembly",
		// "No Dump"/"Segment N: ...", "Reference List", "Log"), not real input fields, so beyond
		// setEditable(false) above they're also made unfocusable (JTextField's built-in
		// click-drag text selection and system paste/context-menu behavior only engage while
		// focused) and given a null Highlighter as a second layer of the same thing, so they
		// can't be clicked into, tabbed to, or have their text selected/highlighted at all --
		// purely visual labels, matching the request to make every pane header non-selectable.
		JTextField[] headerBars = { txtNoFileLoaded, txtDisassembly, txtNoDump, txtReferenceList, txtLog };
		for (int i = 0; i < headerBars.length; i++) {
			headerBars[i].setFocusable(false);
			headerBars[i].setHighlighter(null);
		}
		
		scrollPane_Log = new JScrollPane();
		
		textField_Log = new JTextArea();
		textField_Log.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
		textField_Log.setEditable(false);
		scrollPane_Log.setViewportView(textField_Log);
		textField_Log.setColumns(10);
		
		textField_Reference = new JTextArea();
		textField_Reference.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
		textField_Reference.setEditable(false);
		textField_Reference.setSelectionColor(Color.YELLOW);
		textField_Reference.setSelectedTextColor(Color.BLACK);
		scrollPane_Reference.setViewportView(textField_Reference);
		textField_Reference.setColumns(10);
		
		textField_Disassembly = new JTextPane();
		textField_Disassembly.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
		textField_Disassembly.setEditable(false);
		scrollPane_Disassembly.setViewportView(textField_Disassembly);
		
		textField_FileLoaded = new JTextArea();
		textField_FileLoaded.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
		textField_FileLoaded.setAlignmentY(Component.TOP_ALIGNMENT);
		textField_FileLoaded.setEditable(false);
		textField_FileLoaded.setSelectionColor(Color.YELLOW);
		textField_FileLoaded.setSelectedTextColor(Color.BLACK);
		scrollPane_FileLoaded.setViewportView(textField_FileLoaded);
		textField_FileLoaded.setColumns(10);
		SpringLayout sl_contentPane = new SpringLayout();
		sl_contentPane.putConstraint(SpringLayout.SOUTH, scrollPane_Log, 5, SpringLayout.SOUTH, contentPane);
		sl_contentPane.putConstraint(SpringLayout.EAST, scrollPane_Log, 5, SpringLayout.EAST, contentPane);
		sl_contentPane.putConstraint(SpringLayout.EAST, txtLog, 5, SpringLayout.EAST, contentPane);
		sl_contentPane.putConstraint(SpringLayout.EAST, scrollPane_Reference, 5, SpringLayout.EAST, contentPane);
		sl_contentPane.putConstraint(SpringLayout.EAST, txtReferenceList, 5, SpringLayout.EAST, contentPane);
		sl_contentPane.putConstraint(SpringLayout.EAST, scrollPane_Disassembly, 5, SpringLayout.EAST, contentPane);
		sl_contentPane.putConstraint(SpringLayout.EAST, txtDisassembly, 5, SpringLayout.EAST, contentPane);
		sl_contentPane.putConstraint(SpringLayout.NORTH, txtNoFileLoaded, -5, SpringLayout.NORTH, contentPane);
		sl_contentPane.putConstraint(SpringLayout.WEST, txtNoFileLoaded, -5, SpringLayout.WEST, contentPane);
		sl_contentPane.putConstraint(SpringLayout.NORTH, scrollPane_Log, 0, SpringLayout.SOUTH, txtLog);
		sl_contentPane.putConstraint(SpringLayout.WEST, scrollPane_Log, -5, SpringLayout.WEST, contentPane);
		sl_contentPane.putConstraint(SpringLayout.NORTH, scrollPane_Reference, 0, SpringLayout.SOUTH, txtReferenceList);
		sl_contentPane.putConstraint(SpringLayout.WEST, scrollPane_Reference, 0, SpringLayout.EAST, scrollPane_Dump);
		sl_contentPane.putConstraint(SpringLayout.NORTH, txtReferenceList, 0, SpringLayout.SOUTH, scrollPane_Disassembly);
		sl_contentPane.putConstraint(SpringLayout.WEST, txtReferenceList, 0, SpringLayout.EAST, scrollPane_Dump);
		sl_contentPane.putConstraint(SpringLayout.NORTH, scrollPane_Disassembly, 0, SpringLayout.SOUTH, txtDisassembly);
		sl_contentPane.putConstraint(SpringLayout.WEST, scrollPane_Disassembly, 0, SpringLayout.EAST, txtNoDump);
		sl_contentPane.putConstraint(SpringLayout.NORTH, txtDisassembly, -5, SpringLayout.NORTH, contentPane);
		sl_contentPane.putConstraint(SpringLayout.WEST, txtDisassembly, 0, SpringLayout.EAST, txtNoFileLoaded);
		sl_contentPane.putConstraint(SpringLayout.NORTH, txtLog, 0, SpringLayout.SOUTH, scrollPane_Dump);
		sl_contentPane.putConstraint(SpringLayout.NORTH, scrollPane_Dump, 0, SpringLayout.SOUTH, txtNoDump);
		sl_contentPane.putConstraint(SpringLayout.NORTH, txtNoDump, 0, SpringLayout.SOUTH, scrollPane_FileLoaded);
		sl_contentPane.putConstraint(SpringLayout.NORTH, scrollPane_FileLoaded, 0, SpringLayout.SOUTH, txtNoFileLoaded);
		sl_contentPane.putConstraint(SpringLayout.WEST, txtLog, -5, SpringLayout.WEST, contentPane);
		sl_contentPane.putConstraint(SpringLayout.WEST, scrollPane_Dump, -5, SpringLayout.WEST, contentPane);
		sl_contentPane.putConstraint(SpringLayout.WEST, txtNoDump, -5, SpringLayout.WEST, contentPane);
		sl_contentPane.putConstraint(SpringLayout.WEST, scrollPane_FileLoaded, -5, SpringLayout.WEST, contentPane);
		sl_contentPane.putConstraint(SpringLayout.SOUTH, scrollPane_Disassembly, 767, SpringLayout.NORTH, contentPane);
		sl_contentPane.putConstraint(SpringLayout.SOUTH, scrollPane_Reference, 969, SpringLayout.NORTH, contentPane);
		sl_contentPane.putConstraint(SpringLayout.EAST, txtNoFileLoaded, 557, SpringLayout.WEST, contentPane);
		sl_contentPane.putConstraint(SpringLayout.EAST, txtNoDump, 557, SpringLayout.WEST, contentPane);
		sl_contentPane.putConstraint(SpringLayout.SOUTH, scrollPane_FileLoaded, 286, SpringLayout.NORTH, contentPane);
		sl_contentPane.putConstraint(SpringLayout.EAST, scrollPane_FileLoaded, 557, SpringLayout.WEST, contentPane);
		sl_contentPane.putConstraint(SpringLayout.SOUTH, scrollPane_Dump, 969, SpringLayout.NORTH, contentPane);
		sl_contentPane.putConstraint(SpringLayout.EAST, scrollPane_Dump, 557, SpringLayout.WEST, contentPane);
		contentPane.setLayout(sl_contentPane);
		contentPane.add(scrollPane_Dump);
		
		tableDump = new JTable();
		tableDump.setBounds(new Rectangle(0, 0, 0, 5));
		tableDump.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
		tableDump.setRowHeight(16);
		// Fix: this originally put tableDump in the scroll pane's *column header* slot
		// (setColumnHeaderView) instead of its main viewport. The column header now correctly
		// shows tableDump's own JTableHeader.
		scrollPane_Dump.setViewportView(tableDump);
		scrollPane_Dump.setColumnHeaderView(tableDump.getTableHeader());
		contentPane.add(scrollPane_FileLoaded);
		contentPane.add(txtNoDump);
		contentPane.add(txtNoFileLoaded);
		contentPane.add(scrollPane_Reference);
		contentPane.add(scrollPane_Disassembly);
		contentPane.add(txtDisassembly);
		contentPane.add(txtReferenceList);
		contentPane.add(scrollPane_Log);
		contentPane.add(txtLog);
	}

	/////////////////////////////////////////////////////////////
	// this method contains all of the code for creating events
	/////////////////////////////////////////////////////////////
	private void createEvents() {
		
		mntmAbout.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
			//System.out.println("About Clicked!");
				Utils.printError();
				
				CustomAboutDialog aboutDialog = new CustomAboutDialog();
				 aboutDialog.AboutDialog();
				
			//JOptionPane.showMessageDialog(jframe, "Eggs are not supposed to be green.");
		}});
		
		mntmNewWorkspace.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				handleNewWorkspace();
			}
		});
		
		mntmProfile.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				handleViewProfile();
			}
		});
		
		mntmOpenworkspace.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				openWrkFile();
			}
			
			private void openWrkFile()
				{
				    JFileChooser chooser = new JFileChooser();
				    FileNameExtensionFilter filterwrk = new FileNameExtensionFilter(
				        "WorkSpace Files (*.wrk, *.xml)", "wrk", "xml");
				    chooser.setFileFilter(filterwrk);
				    int returnVal = chooser.showOpenDialog(Dis6502Gui.this);
				    if(returnVal == JFileChooser.APPROVE_OPTION) {
				        try {
				            workspace.load(chooser.getSelectedFile().getAbsolutePath());
				        } catch (IOException ex) {
				            JOptionPane.showMessageDialog(Dis6502Gui.this,
				                    "Cannot open workspace:\n" + ex.getMessage(),
				                    "Open Workspace", JOptionPane.ERROR_MESSAGE);
				            return;
				        }
				        runDisassembly();
				    }
				}
		});
		
		mntmOpenExecutableFile.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				openExeFile();
			}

			private void openExeFile()
				{
					MainGlobals.ID_FILE_TYPE = MainGlobals.EXECUTABLE_FILE;
				    JFileChooser chooser = new JFileChooser();
				    FileNameExtensionFilter filterexe = new FileNameExtensionFilter(
				        "EXE & XEX & COM & SYS & BIN Images", "exe", "xex", "com", "sys", "bin");
				    chooser.setFileFilter(filterexe);
				    int returnVal = chooser.showOpenDialog(Dis6502Gui.this);
				    if(returnVal == JFileChooser.APPROVE_OPTION) {
				        openExecutableFile(chooser.getSelectedFile());
				    }
				}
		});
		
		mntmOpenRomImage.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {	
				openRomFile();
			}
			
			private void openRomFile()
				{
					// TODO Auto-generated method stub
				    JFileChooser chooser = new JFileChooser();
				    FileNameExtensionFilter filterrom = new FileNameExtensionFilter(
				        "ROM & CAR & EPM & BIN Images", "rom", "car", "epm", "bin");
				    chooser.setFileFilter(filterrom);
				    int returnVal = chooser.showOpenDialog(jframe);
				    if(returnVal == JFileChooser.APPROVE_OPTION) {
				       System.out.println("You chose to open this file: " +
				            chooser.getSelectedFile().getName());
				    }
				}
		});
		
		mntmOpenRawFile.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				openRawFile();
			}
			
			private void openRawFile()
				{
				    JFileChooser chooser = new JFileChooser();
				    int returnVal = chooser.showOpenDialog(Dis6502Gui.this);
				    if(returnVal != JFileChooser.APPROVE_OPTION) {
				        return;
				    }
				    File file = chooser.getSelectedFile();

				    String addressText = JOptionPane.showInputDialog(Dis6502Gui.this,
				            "Load address in hex (e.g. 0600):", "0600");
				    if (addressText == null) {
				        return;
				    }
				    int loadAddress;
				    try {
				        loadAddress = Integer.parseInt(addressText.trim(), 16) & 0xFFFF;
				    } catch (NumberFormatException ex) {
				        JOptionPane.showMessageDialog(Dis6502Gui.this,
				                "\"" + addressText + "\" is not a valid hex address.",
				                "Invalid Address", JOptionPane.ERROR_MESSAGE);
				        return;
				    }

				    byte[] bytes;
				    try {
				        bytes = FileIO.readByteArray(file.getAbsolutePath());
				    } catch (IOException ex) {
				        JOptionPane.showMessageDialog(Dis6502Gui.this,
				                "Cannot read \"" + file.getAbsolutePath() + "\":\n" + ex.getMessage(),
				                "Open Raw File", JOptionPane.ERROR_MESSAGE);
				        return;
				    }
				    if (bytes.length == 0 || (loadAddress + bytes.length) > 0x10000) {
				        JOptionPane.showMessageDialog(Dis6502Gui.this,
				                "File is empty, or does not fit in memory at that address.",
				                "Open Raw File", JOptionPane.ERROR_MESSAGE);
				        return;
				    }

				    workspace.init();

				    SegmentList segmentList = workspace.getSegmentList();
				    Segment segment = segmentList.insertSegmentAt(0);
				    segment.setHeader(FileHeader.RAW);
				    segment.bBinary = true;
				    segment.processorType = ProcessorType.MOS6502;
				    segment.wBegin = loadAddress;
				    segment.wEnd = loadAddress + bytes.length - 1;
				    segment.createMemoryBlockFromBeginToEnd();
				    for (int i = 0; i < bytes.length; i++) {
				        segment.setData(i, bytes[i] & 0xFF);
				    }

				    workspace.setFilePath(file.getAbsolutePath());
				    segmentList.setSelectedIndex(0);

				    runDisassembly();
				}
		});
		
		mntmOpenCassetteImage.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				openCasFile();
			}
			
			private void openCasFile()
				{
					// TODO Auto-generated method stub
				    JFileChooser chooser = new JFileChooser();
				    FileNameExtensionFilter filtercas = new FileNameExtensionFilter(
				        "CAS Images", "cas");
				    chooser.setFileFilter(filtercas);
				    int returnVal = chooser.showOpenDialog(jframe);
				    if(returnVal == JFileChooser.APPROVE_OPTION) {
				       System.out.println("You chose to open this file: " +
				            chooser.getSelectedFile().getName());
				    }
				}
		});
		
		mntmOpenDiskImage.addActionListener(new ActionListener() {  //DISK IMAGE EXE 
			public void actionPerformed(ActionEvent e) {
				openDiskImgExeFile();
			}
			
			private void openDiskImgExeFile()
				{
					// TODO Auto-generated method stub
				    JFileChooser chooser = new JFileChooser();
				    FileNameExtensionFilter filterdskimgexe = new FileNameExtensionFilter(
				        "ATR & XFD Images", "atr", "xfd");
				    chooser.setFileFilter(filterdskimgexe);
				    int returnVal = chooser.showOpenDialog(jframe);
				    if(returnVal == JFileChooser.APPROVE_OPTION) {
				       System.out.println("You chose to open this file: " +
				            chooser.getSelectedFile().getName());
				    }
				}
		});
		
		mntmOpenDiskImage_1.addActionListener(new ActionListener() {  // BOOT SECTORS
			public void actionPerformed(ActionEvent e) {
				openDiskImgBootFile();
			}
			
			private void openDiskImgBootFile()
				{
					// TODO Auto-generated method stub
				    JFileChooser chooser = new JFileChooser();
				    FileNameExtensionFilter filterdskimgboot = new FileNameExtensionFilter(
				        "ATR & XFD Images", "atr", "xfd");
				    chooser.setFileFilter(filterdskimgboot);
				    int returnVal = chooser.showOpenDialog(jframe);
				    if(returnVal == JFileChooser.APPROVE_OPTION) {
				       System.out.println("You chose to open this file: " +
				            chooser.getSelectedFile().getName());
				    }
				}
		});
		
		mntmOpenDiskImage_2.addActionListener(new ActionListener() {  // DISK IMAGE SECTORS
			public void actionPerformed(ActionEvent e) {
				openDiskImgSectorsFile();
			}
			
			private void openDiskImgSectorsFile()
				{
					// TODO Auto-generated method stub
				    JFileChooser chooser = new JFileChooser();
				    FileNameExtensionFilter filterdskimgsectors = new FileNameExtensionFilter(
				        "ATR & XFD Images", "atr", "xfd");
				    chooser.setFileFilter(filterdskimgsectors);
				    int returnVal = chooser.showOpenDialog(jframe);
				    if(returnVal == JFileChooser.APPROVE_OPTION) {
				       System.out.println("You chose to open this file: " +
				            chooser.getSelectedFile().getName());
				    }
				}
		});
		
		mntmSaveWorkspace.addActionListener(new ActionListener() { // fix to check if save filename is empty
			public void actionPerformed(ActionEvent e) {           // and open dialog if it is
				saveWorkspaceFile();
			}
			
			private void saveWorkspaceFile() {
				String filePath = workspace.getFilePath();
				if (filePath == null || filePath.length() == 0) {
					saveWorkspace(null); // No path yet: fall back to "Save As" behavior.
				} else {
					saveWorkspace(filePath);
				}
			}
		});
		
		// 5. Add an action listener to handle the application shutdown
	    mntmExit.addActionListener(new ActionListener() {
	        @Override
	        public void actionPerformed(ActionEvent e) {
	            // Terminates the currently running Java Virtual Machine
	            System.exit(0); 
	        }
	    });
		
		mntmSaveWorkspaceAs.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				saveWorkspace(null);
			}
		});
		
		mntmSaveDisassemblyFiles.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// Simplified compared to the original app's "Save Disassembly Files" (which
				// splits equates/includes across several files per Profile settings): this
				// saves the Disassembly panel's current text as a single .asm file.
				JFileChooser chooser = new JFileChooser();
				FileNameExtensionFilter filter = new FileNameExtensionFilter("Assembly Files (*.asm)", "asm");
				chooser.setFileFilter(filter);
				int returnVal = chooser.showSaveDialog(Dis6502Gui.this);
				if (returnVal != JFileChooser.APPROVE_OPTION) {
					return;
				}
				File file = chooser.getSelectedFile();
				FileWriter writer = null;
				try {
					writer = new FileWriter(file);
					writer.write(textField_Disassembly.getText());
				} catch (IOException ex) {
					JOptionPane.showMessageDialog(Dis6502Gui.this,
							"Cannot save \"" + file.getAbsolutePath() + "\":\n" + ex.getMessage(),
							"Save Disassembly Files", JOptionPane.ERROR_MESSAGE);
				} finally {
					if (writer != null) {
						try {
							writer.close();
						} catch (IOException ignored) {
						}
					}
				}
			}
		});
	}

	/** Prompts for a file (if filePath is null/empty) and saves the current workspace there. */
	private void saveWorkspace(String filePath) {
		if (filePath == null || filePath.length() == 0) {
			JFileChooser chooser = new JFileChooser();
			FileNameExtensionFilter filterworkspaceas = new FileNameExtensionFilter(
					"WorkSpace Files (*.wrk, *.xml)", "wrk", "xml");
			chooser.setFileFilter(filterworkspaceas);
			int returnVal = chooser.showSaveDialog(this);
			if (returnVal != JFileChooser.APPROVE_OPTION) {
				return;
			}
			filePath = chooser.getSelectedFile().getAbsolutePath();
		}
		try {
			workspace.save(filePath);
		} catch (IOException ex) {
			JOptionPane.showMessageDialog(this, "Cannot save workspace:\n" + ex.getMessage(),
					"Save Workspace", JOptionPane.ERROR_MESSAGE);
		}
	}
	

    
	
	public static void UpdateDisassembly()
	{
		
	}

	// --- Engine wiring: connects this UI to the dis6502.core engine (Workspace, SegmentList,
	// Segment, Disassembly, DisassemblyResult). ---

	private void initEngine() {
		workspace = new Workspace(new ComputerSystemFactory());
		workspace.setComputerSystemType(ComputerSystemType.ATARI800);

		disassembly = new Disassembly();
		disassembly.setWorkspace(workspace);
		disassembly.setProgressMonitor(new DisassemblyProgressMonitor());

		hexDumpTableModel = new HexDumpTableModel();
		tableDump.setModel(hexDumpTableModel);

		// Tighter look: no grid lines, no inter-cell spacing, and a snug cell renderer instead
		// of the default's wider padding.
		tableDump.setShowGrid(false);
		tableDump.setIntercellSpacing(new Dimension(0, 0));
		tableDump.setDefaultRenderer(Object.class, new HexDumpCellRenderer());
		configureDumpTableColumns();

		// Live log panel: every Debug.log(...)/Debug.logValue(...) call anywhere in the engine
		// (Segment, SegmentList, Workspace, Disassembly, ...) shows up here as it happens.
		Debug.addListener(new Debug.Listener() {
			public void handleLog(String message) {
				appendLogLine(message);
			}
		});

		// Refresh the segments panel whenever the SegmentList changes (add/remove/split/merge/
		// reorder).
		workspace.getSegmentList().addListener(new SegmentListChangedListener() {
			public void handleSegmentListChanged(SegmentList segmentList, List<SegmentList.Property> propertyChangeEvents) {
				refreshSegmentsPanel();
			}
		});

		// Update the top-left header (and window title) when the workspace's file path changes.
		workspace.addListener(new WorkspaceChangedListener() {
			public void handleWorkspaceChanged(Workspace w, List<WorkspaceProperty> propertyChangeEvents) {
				if (propertyChangeEvents.contains(WorkspaceProperty.FILE_PATH)) {
					refreshFileLoadedHeader();
				}
			}
		});

		// tableDump: a linear (reading-order) byte-range selection driven by our own mouse
		// tracking instead of JTable's default rectangular cell-selection -- see
		// configureDumpMouseHandling() for why.
		configureDumpMouseHandling();

		// textField_Disassembly: a single click populates the Reference List panel with every
		// other line that refers to the same address; a double click additionally jumps the hex
		// dump panel to that line's own bytes.
		textField_Disassembly.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				handleDisassemblyClick(e);
			}
		});

		// textField_Reference: clicking a reference jumps both the Disassembly panel and the hex
		// dump panel to that same address.
		textField_Reference.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				handleReferenceClick(e);
			}
		});

		// textField_FileLoaded (the segments list): clicking a segment selects it in the engine
		// and jumps both the hex dump panel and the Disassembly panel to its start.
		textField_FileLoaded.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				handleSegmentsClick(e);
			}
		});

		refreshSegmentsPanel();
		refreshFileLoadedHeader();
	}

	/**
	 * Parses an Atari executable file: a standard DOS 2.x "$FFFF" binary (.XEX/.COM/.EXE/.SYS),
	 * or a SpartaDOS X (SDX) executable, whose segments are introduced by one of five other
	 * header words ($FFFA/$FFFB/$FFFC/$FFFD/$FFFE) instead of $FFFF -- e.g. SpartaDOS X
	 * executables typically start with $FFFA (SDX_FIXED_BLK) or $FFFE (SDX_RELOC_BLK), never
	 * $FFFF, which is why they were previously rejected here. The actual parsing is delegated
	 * to AtariExecutableReader, a direct port of Atari800::ReadExecutableFile from the C++
	 * source (see that class's Javadoc for the full header list), so each segment/fixup/symbol
	 * block becomes one Segment in the workspace exactly as the C++ tool represents it.
	 */
	/**
	 * "File" -> "New Workspace": prompts for which computer system's equates to load (the
	 * dropdown offers the three systems this project has equates files for: Atari 800,
	 * Atari 5200, and Oric), then resets the workspace to a fresh, empty one for that system
	 * and loads its "systems/&lt;system&gt;/&lt;System&gt;.equ" file into the system equate
	 * list, so addresses like $E456 show up in the disassembly as their real OS label (e.g.
	 * CIOV) instead of a bare address. Not a translation of any C++ class -- this dialog and
	 * its wiring live in the un-ported MFC view/controller layer (same situation as the hex
	 * dump context menu); the model-layer pieces it relies on (Workspace.init/
	 * setComputerSystemType, EquateList.load) are all real translations.
	 */
	/**
	 * "View" -> "Profile...": edits the workspace's output-formatting Profile via ProfileDialog
	 * (see that class). If OK is pressed, re-runs disassembly so the new formatting is reflected
	 * immediately (directive spelling, comment prefix, etc all feed directly into how each
	 * DisassemblyLine is rendered).
	 */
	private void handleViewProfile() {
		ProfileDialog dialog = new ProfileDialog(jframe, workspace.getProfile());
		if (dialog.showDialog()) {
			workspace.notifyProfileChanged();
			runDisassembly();
			if (currentDumpSegmentIndex >= 0) {
				showSegmentInDump(currentDumpSegmentIndex);
			}
		}
	}

	private void handleNewWorkspace() {
		ComputerSystemType[] offeredTypes = {
				ComputerSystemType.ATARI800, ComputerSystemType.ATARI5200, ComputerSystemType.ORIC
		};
		ComputerSystemFactory factory = workspace.getComputerSystemFactory();
		String[] labels = new String[offeredTypes.length];
		for (int i = 0; i < offeredTypes.length; i++) {
			labels[i] = factory.getComputerSystemTypeInfo(offeredTypes[i]).getText();
		}

		final JComboBox comboBox = new JComboBox(labels);
		comboBox.setSelectedIndex(1); // Atari 800, matching the screenshot's default selection.

		JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		panel.add(new JLabel("Computer"));
		panel.add(comboBox);

		int result = JOptionPane.showConfirmDialog(this, panel, "New Workspace",
				JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
		if (result != JOptionPane.OK_OPTION) {
			return;
		}

		ComputerSystemType selectedType = offeredTypes[comboBox.getSelectedIndex()];

		workspace.init();
		workspace.setComputerSystemType(selectedType);

		String fileName = factory.getComputerSystemTypeInfo(selectedType).getFileName(); // e.g. "Atari800"
		File equatesFile = findSystemEquatesFile(fileName);
		if (equatesFile != null) {
			try {
				workspace.getSystemEquateList().load(equatesFile.getAbsolutePath());
			} catch (IOException ex) {
				JOptionPane.showMessageDialog(this,
						"Could not load \"" + equatesFile.getAbsolutePath() + "\":\n" + ex.getMessage(),
						"New Workspace", JOptionPane.WARNING_MESSAGE);
			}
		} else {
			JOptionPane.showMessageDialog(this,
					"Could not find " + fileName + ".equ. Looked in:\n" + systemEquatesSearchPathsText(fileName)
							+ "\nThe new workspace was created, but with no system equates loaded.",
					"New Workspace", JOptionPane.WARNING_MESSAGE);
		}

		disassemblyLineIndex.clear();
		textField_Disassembly.setText("");
		textField_Reference.setText("");
		txtReferenceList.setText("  Reference List");
		refreshSegmentsPanel();
	}

	/**
	 * Looks for "systems/&lt;fileName, lowercased&gt;/&lt;fileName&gt;.equ" (matching the
	 * systems.zip layout, e.g. "systems/atari800/Atari800.equ") next to wherever this app is
	 * actually running from -- next to the jar/classes (covers running the built jDIS6502.jar
	 * directly), in the current working directory (covers NetBeans' "Run Project", which starts
	 * the app with the project directory as its working directory), and in "dist" under the
	 * current working directory (covers running the built jar via "java -jar dist/jDIS6502.jar"
	 * from the project directory) -- so it's found regardless of how the app was launched.
	 */
	private File findSystemEquatesFile(String fileName) {
		String relativePath = "systems" + File.separator + fileName.toLowerCase() + File.separator + fileName + ".equ";
		return AppPaths.findExistingFile(relativePath);
	}

	private String systemEquatesSearchPathsText(String fileName) {
		String relativePath = "systems" + File.separator + fileName.toLowerCase() + File.separator + fileName + ".equ";
		return AppPaths.candidatePathsText(relativePath);
	}

	private void openExecutableFile(File file) {
		byte[] bytes;
		try {
			bytes = FileIO.readByteArray(file.getAbsolutePath());
		} catch (IOException ex) {
			JOptionPane.showMessageDialog(this, "Cannot read \"" + file.getAbsolutePath() + "\":\n" + ex.getMessage(),
					"Open Executable File", JOptionPane.ERROR_MESSAGE);
			return;
		}

		workspace.init();
		SegmentList segmentList = workspace.getSegmentList();

		int segmentCount;
		try {
			segmentCount = AtariExecutableReader.readExecutableFile(segmentList, bytes);
		} catch (IOException ex) {
			JOptionPane.showMessageDialog(this,
					"\"" + file.getName() + "\" could not be parsed as an Atari executable file:\n" + ex.getMessage()
							+ "\n\nUse \"Open Raw File\" instead if this is a headerless memory dump.",
					"Open Executable File", JOptionPane.ERROR_MESSAGE);
			return;
		}

		for (int i = 0; i < segmentList.getCount(); i++) {
			segmentList.getSegment(i).processorType = ProcessorType.MOS6502;
		}

		if (segmentCount == 0) {
			JOptionPane.showMessageDialog(this,
					"\"" + file.getName() + "\" has a valid header but no readable segment data.",
					"Open Executable File", JOptionPane.ERROR_MESSAGE);
			return;
		}

		workspace.setFilePath(file.getAbsolutePath());
		segmentList.setSelectedIndex(0);
		runDisassembly();
	}

	private void runDisassembly() {
		disassembly.startDisassembly();
		refreshSegmentsPanel();
		refreshDisassemblyPanel();
		refreshDumpPanel();
	}

	private void refreshFileLoadedHeader() {
		String filePath = workspace.getFilePath();
		if (filePath == null || filePath.length() == 0) {
			txtNoFileLoaded.setText("  No File Loaded");
		} else {
			File file = new File(filePath);
			txtNoFileLoaded.setText("  Segments for " + file.getName());
		}
		txtNoFileLoaded.setBackground(Color.YELLOW);
		setTitle("  jDIS6502 - " + (filePath == null || filePath.length() == 0 ? "(no file)" : filePath));
	}

	/** Corresponds to the segments-list panel ("StdBin $0200-$CBFF len ..."). */
	private void refreshSegmentsPanel() {
		SegmentList segmentList = workspace.getSegmentList();
		StringBuilder text = new StringBuilder();
		for (int i = 0; i < segmentList.getCount(); i++) {
			text.append(segmentList.getSegment(i).toDisplayString()).append('\n');
		}
		textField_FileLoaded.setText(text.toString());

		if (segmentList.getCount() > 0) {
			showSegmentInDump(0);
		} else {
			currentDumpSegmentIndex = -1;
			hexDumpTableModel.setSegment(null);
			configureDumpTableColumns();
			dumpSelectionAnchorOffset = -1;
			dumpSelectionLeadOffset = -1;
			txtNoDump.setText("  No Dump");
			txtNoDump.setBackground(Color.CYAN);
		}
	}

	private void showSegmentInDump(int segmentIndex) {
		Segment segment = workspace.getSegmentList().getSegment(segmentIndex);
		currentDumpSegmentIndex = segmentIndex;
		hexDumpTableModel.setSegment(segment);
		configureDumpTableColumns();
		// Clear any selection left over from a different segment; callers that want a specific
		// selection (navigateDumpToOffset, markDumpSelectionAs) set it explicitly afterward.
		dumpSelectionAnchorOffset = -1;
		dumpSelectionLeadOffset = -1;
		txtNoDump.setText("  Segment " + (segmentIndex + 1) + ": " + segment.toDisplayString());
		txtNoDump.setBackground(Color.CYAN);
	}

	/**
	 * Sizes tableDump's columns: the Address column wide enough to show its full header/values
	 * (not truncated), narrow byte columns, and a fixed-width ASCII column. Called after every
	 * hexDumpTableModel.setSegment(...), since fireTableStructureChanged() (which that triggers)
	 * rebuilds the TableColumnModel from scratch at default widths each time.
	 */
	private void configureDumpTableColumns() {
		FontMetrics fontMetrics = tableDump.getFontMetrics(tableDump.getFont());
		int charWidth = fontMetrics.charWidth('0');
		int addressWidth = Math.max(fontMetrics.stringWidth("Address"), fontMetrics.stringWidth("0000")) + 12;
		int byteWidth = charWidth * 2 + 10;
		int asciiWidth = charWidth * HexDumpTableModel.BYTES_PER_ROW + 10;

		javax.swing.table.TableColumnModel columnModel = tableDump.getColumnModel();
		for (int col = 0; col < columnModel.getColumnCount(); col++) {
			TableColumn column = columnModel.getColumn(col);
			if (col == 0) {
				column.setPreferredWidth(addressWidth);
			} else if (col <= HexDumpTableModel.BYTES_PER_ROW) {
				column.setPreferredWidth(byteWidth);
			} else {
				column.setPreferredWidth(asciiWidth);
				// Structure changes (a new segment, or any edit that re-fires them) rebuild the
				// TableColumn objects, so the ASCII column's per-character-colored renderer (see
				// HexDumpAsciiCellRenderer) needs to be re-applied here every time, not just once.
				column.setCellRenderer(new HexDumpAsciiCellRenderer());
			}
		}
	}

	/**
	 * Converts a (row, byte-column) cell into a linear byte offset, or -1 if the cell isn't a
	 * byte column (Address or ASCII) or is out of range for the current segment.
	 */
	private int offsetForCell(int row, int column) {
		Segment segment = hexDumpTableModel.segment;
		if (segment == null || row < 0 || column < 1 || column > HexDumpTableModel.BYTES_PER_ROW) {
			return -1;
		}
		int offset = row * HexDumpTableModel.BYTES_PER_ROW + (column - 1);
		return (offset >= 0 && offset < segment.getSize()) ? offset : -1;
	}

	/** Sets the dump's linear selection to [startOffset, endOffset], repaints, and scrolls endOffset into view. */
	private void setDumpSelection(int startOffset, int endOffset) {
		dumpSelectionAnchorOffset = startOffset;
		dumpSelectionLeadOffset = endOffset;
		tableDump.repaint();

		int row = endOffset / HexDumpTableModel.BYTES_PER_ROW;
		int column = 1 + (endOffset % HexDumpTableModel.BYTES_PER_ROW);
		if (row >= 0 && row < tableDump.getRowCount()) {
			tableDump.scrollRectToVisible(tableDump.getCellRect(row, column, true));
		}
	}

	/**
	 * Direct translation of dwMemoryInspectorColor[] from MemoryInspectorControlImpl.cpp,
	 * indexed by MemoryType.toByte() (matching that array's enum-ordinal indexing exactly:
	 * UNKNOWN, LOBYTE, HIBYTE, BYTE, WORD, LABEL, STRING, SBYTE, DLIST, STORE, CODE, SYMBOL, FIXUP).
	 */
	private static final Color[] HEX_DUMP_COLOR_PALETTE = {
			new Color(0, 0, 0),
			new Color(192, 192, 192),
			new Color(128, 128, 128),
			new Color(128, 0, 0),
			new Color(128, 0, 128),
			new Color(128, 128, 0),
			new Color(255, 127, 0),
			new Color(0, 127, 255),
			new Color(0, 128, 0),
			new Color(255, 128, 255),
			new Color(0, 0, 128),
			new Color(255, 0, 128),
			new Color(255, 0, 255)
	};

	private static Color hexDumpColorFor(int effectiveType) {
		return (effectiveType >= 0 && effectiveType < HEX_DUMP_COLOR_PALETTE.length)
				? HEX_DUMP_COLOR_PALETTE[effectiveType] : Color.BLACK;
	}

	/**
	 * A TableCellRenderer that highlights byte cells by *linear* offset (reading left-to-right,
	 * then top-to-bottom, matching how bytes are actually laid out in memory) rather than
	 * JTable's default rectangular row/column intersection. See configureDumpMouseHandling() for
	 * why: dragging across rows in a hex view should select a contiguous run of bytes, not a
	 * rectangular block of table cells.
	 */
	private final class HexDumpCellRenderer extends DefaultTableCellRenderer {
		HexDumpCellRenderer() {
			setBorder(BorderFactory.createEmptyBorder(0, 3, 0, 3));
		}

		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
				boolean hasFocus, int row, int column) {
			// isSelected/hasFocus intentionally ignored: JTable's own selection model isn't used
			// for tableDump (see configureDumpMouseHandling()), only our own anchor/lead offsets.
			Component component = super.getTableCellRendererComponent(table, value, false, false, row, column);

			int offset = offsetForCell(row, column);
			boolean highlighted = offset >= 0 && dumpSelectionAnchorOffset >= 0
					&& offset >= Math.min(dumpSelectionAnchorOffset, dumpSelectionLeadOffset)
					&& offset <= Math.max(dumpSelectionAnchorOffset, dumpSelectionLeadOffset);

			// Use the table's own selection colors (whatever the platform/L&F default is), same
			// as what JTable's built-in cell selection painted before this custom renderer took
			// over -- rather than a hardcoded color.
			if (highlighted) {
				component.setBackground(table.getSelectionBackground());
				component.setForeground(table.getSelectionForeground());
			} else {
				component.setBackground(table.getBackground());
				component.setForeground(offset >= 0 ? hexDumpColorFor(hexDumpTableModel.getEffectiveType(offset)) : table.getForeground());
			}
			return component;
		}
	}

	/**
	 * TableCellRenderer for the hex dump's ASCII column: unlike the byte columns, one cell here
	 * holds up to BYTES_PER_ROW characters that can each need a *different* type color (matching
	 * MemoryInspectorControlImpl::PrintLine, which colors the ASCII/ATASCII character the same as
	 * its byte -- see HexDumpCellRenderer), which a plain single-foreground-color
	 * DefaultTableCellRenderer can't express. This paints each character itself instead.
	 */
	private final class HexDumpAsciiCellRenderer extends JComponent implements javax.swing.table.TableCellRenderer {
		private String text = "";
		private int rowOffset;
		private Color background;
		private Color selectionBackground;
		private Color selectionForeground;

		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
				boolean hasFocus, int row, int column) {
			text = (value == null) ? "" : value.toString();
			rowOffset = row * HexDumpTableModel.BYTES_PER_ROW;
			setFont(table.getFont());
			background = table.getBackground();
			selectionBackground = table.getSelectionBackground();
			selectionForeground = table.getSelectionForeground();
			setOpaque(true);
			return this;
		}

		public java.awt.Dimension getPreferredSize() {
			FontMetrics fontMetrics = getFontMetrics(getFont());
			return new java.awt.Dimension(fontMetrics.stringWidth(text) + 6, fontMetrics.getHeight());
		}

		protected void paintComponent(Graphics g) {
			FontMetrics fontMetrics = g.getFontMetrics(getFont());
			g.setColor(background);
			g.fillRect(0, 0, getWidth(), getHeight());

			int x = 3;
			int y = ((getHeight() - fontMetrics.getHeight()) / 2) + fontMetrics.getAscent();
			for (int i = 0; i < text.length(); i++) {
				int offset = rowOffset + i;
				boolean highlighted = dumpSelectionAnchorOffset >= 0
						&& offset >= Math.min(dumpSelectionAnchorOffset, dumpSelectionLeadOffset)
						&& offset <= Math.max(dumpSelectionAnchorOffset, dumpSelectionLeadOffset);
				String ch = text.substring(i, i + 1);
				int charWidth = fontMetrics.stringWidth(ch);

				if (highlighted) {
					g.setColor(selectionBackground);
					g.fillRect(x, 0, charWidth, getHeight());
					g.setColor(selectionForeground);
				} else {
					g.setColor(hexDumpColorFor(hexDumpTableModel.getEffectiveType(offset)));
				}
				g.drawString(ch, x, y);
				x += charWidth;
			}
		}
	}

	/**
	 * Wires up a linear (reading-order) drag-to-select for tableDump, replacing JTable's default
	 * cell-selection model, which selects a rectangular block of cells when you drag across
	 * multiple rows -- e.g. dragging from row 0 column 12 to row 1 column 3 would select columns
	 * 3-12 in *both* rows, not the contiguous run of bytes a hex-editor user actually means. This
	 * tracks a simple anchor/lead pair of linear byte offsets instead: press sets the anchor,
	 * drag updates the lead, and HexDumpCellRenderer paints every cell whose offset falls between
	 * them. Right-click (platform-dependent: press or release) still opens the "mark as" menu,
	 * starting a fresh one-cell selection first if the click landed outside the current one.
	 */
	private void configureDumpMouseHandling() {
		final JPopupMenu dumpPopupMenu = buildDumpPopupMenu();
		registerDumpKeyBindings();

		tableDump.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				if (maybeShowPopup(e)) {
					return;
				}
				if (SwingUtilities.isLeftMouseButton(e)) {
					int offset = offsetForCell(tableDump.rowAtPoint(e.getPoint()), tableDump.columnAtPoint(e.getPoint()));
					if (offset >= 0) {
						if (e.getClickCount() == 2) {
							setDumpSelection(offset, offset);
							handleEditBytesAtSelection();
						} else {
							setDumpSelection(offset, offset);
						}
					}
				}
			}

			public void mouseReleased(MouseEvent e) {
				if (maybeShowPopup(e)) {
					return;
				}
				handleDumpSelectionChanged();
			}

			private boolean maybeShowPopup(MouseEvent e) {
				if (e.isPopupTrigger() && hexDumpTableModel.segment != null) {
					int offset = offsetForCell(tableDump.rowAtPoint(e.getPoint()), tableDump.columnAtPoint(e.getPoint()));
					if (offset >= 0) {
						boolean withinCurrentSelection = dumpSelectionAnchorOffset >= 0
								&& offset >= Math.min(dumpSelectionAnchorOffset, dumpSelectionLeadOffset)
								&& offset <= Math.max(dumpSelectionAnchorOffset, dumpSelectionLeadOffset);
						if (!withinCurrentSelection) {
							setDumpSelection(offset, offset);
						}
					}
					dumpPopupMenu.show(tableDump, e.getX(), e.getY());
					return true;
				}
				return false;
			}
		});

		tableDump.addMouseMotionListener(new MouseMotionAdapter() {
			public void mouseDragged(MouseEvent e) {
				if (dumpSelectionAnchorOffset < 0) {
					return; // Drag didn't start on a byte cell (e.g. started on Address/ASCII).
				}
				int row = tableDump.rowAtPoint(e.getPoint());
				int column = tableDump.columnAtPoint(e.getPoint());
				// Clamp drags into the Address/ASCII columns (or above/below the table) to the
				// nearest byte column, so dragging past the edge still extends the selection
				// smoothly instead of stalling.
				if (column < 1) {
					column = 1;
				} else if (column > HexDumpTableModel.BYTES_PER_ROW) {
					column = HexDumpTableModel.BYTES_PER_ROW;
				}
				if (row < 0) {
					row = 0;
				} else if (row >= tableDump.getRowCount()) {
					row = tableDump.getRowCount() - 1;
				}
				int offset = offsetForCell(row, column);
				if (offset >= 0) {
					setDumpSelection(dumpSelectionAnchorOffset, offset);
				}
			}
		});
	}

	/**
	 * Corresponds to the "Disassembly" panel: the full DisassemblyResult listing, in order.
	 * Also rebuilds disassemblyLineIndex in lock-step with the displayed text, line for line, so
	 * a later click/double-click on the pane can be mapped back to the DisassemblyLine (and from
	 * there its segment/offset/address) that produced that line of text.
	 */
	private void refreshDisassemblyPanel() {
		disassemblyLineIndex.clear();

		Iterator<DisassemblyLine> lines = workspace.getDisassemblyResult().createLineIterator();
		while (lines.hasNext()) {
			disassemblyLineIndex.add(lines.next());
		}
		rebuildDisassemblyDocument();
		refreshReferenceListPanel(0);
	}

	/** Solid-color Highlighter.HighlightPainter that fills the *entire* pane width, not just
	 * under the text -- matching DisassemblyControlImpl::PrintOneLineInColor's FillRect call,
	 * which paints a selected line's yellow background across the whole control regardless of
	 * how much text that line actually has. The stock DefaultHighlightPainter only shades behind
	 * the characters themselves, which would look like a short yellow patch rather than a full row. */
	private static final class FullWidthHighlightPainter implements Highlighter.HighlightPainter {
		private final Color color;

		FullWidthHighlightPainter(Color color) {
			this.color = color;
		}

		public void paint(Graphics g, int offset0, int offset1, Shape bounds, JTextComponent component) {
			try {
				Rectangle lineBounds = component.modelToView(offset0);
				if (lineBounds == null) {
					return;
				}
				g.setColor(color);
				g.fillRect(0, lineBounds.y, component.getWidth(), lineBounds.height);
			} catch (BadLocationException e) {
				// The document changed out from under this highlight; nothing sensible to paint.
			}
		}
	}

	private static final Highlighter.HighlightPainter SELECTED_LINE_PAINTER = new FullWidthHighlightPainter(Color.YELLOW);

	/**
	 * Direct translation of DisassemblyControlImpl::PrintAll/PrintOneLineInColor (from the
	 * user-supplied DisassemblyControlImpl.cpp): rebuilds textField_Disassembly's StyledDocument
	 * from disassemblyLineIndex's current content, applying per-token syntax coloring (see
	 * DisassemblyLineColorizer) and a full-width yellow background band for whichever line(s)
	 * have .selected set. Every field this reads (.selected, .referenced, .address, .getSection()
	 * .getType(), .getLineNumber()) already exists on the ported DisassemblyLine/DisassemblySection
	 * classes -- only the rendering itself (this method) is new.
	 *
	 * Called both after a fresh disassembly run and whenever .selected changes (e.g. clicking a
	 * line, or navigating from the hex dump via selectDisassemblyLine): the original's "; $addr"
	 * comment appended after a selected line is itself part of that line's rendered text, not
	 * just a background color, so the whole document needs rebuilding either way.
	 */
	private void rebuildDisassemblyDocument() {
		boolean lineNumbersActive = workspace.getProfile().useLineNumbers;
		StyledDocument doc = textField_Disassembly.getStyledDocument();
		textField_Disassembly.getHighlighter().removeAllHighlights();

		List<int[]> selectedRanges = new ArrayList<int[]>();
		try {
			doc.remove(0, doc.getLength());

			for (int i = 0; i < disassemblyLineIndex.size(); i++) {
				DisassemblyLine line = disassemblyLineIndex.get(i);
				String text = line.getLine();

				if (lineNumbersActive) {
					text = String.format("%04d %s", line.getLineNumber(), text);
				}

				// Matches PrintAll's own column-35 check exactly: pad to at least 36 characters
				// first (so column 35 is always a real character, a space if the line is short),
				// then only append "; $addr" if nothing already occupies that exact column.
				if (line.selected && line.address != 0) {
					StringBuilder padded = new StringBuilder(text);
					while (padded.length() < 36) {
						padded.append(' ');
					}
					if (padded.charAt(35) != ';') {
						padded.setLength(35);
						padded.append("; $").append(String.format("%04X", line.address & 0xFFFF));
						text = padded.toString();
					}
				}

				// All sections other than the system/user equates are never greyed out.
				DisassemblySectionType sectionType = line.getSection().getType();
				boolean referenced = (sectionType == DisassemblySectionType.SYSTEM_EQUATES
						|| sectionType == DisassemblySectionType.USER_EQUATES) ? line.referenced : true;

				int lineStartOffset = doc.getLength();

				List<DisassemblyLineColorizer.Run> runs = DisassemblyLineColorizer.tokenize(text, referenced, lineNumbersActive);
				for (int r = 0; r < runs.size(); r++) {
					DisassemblyLineColorizer.Run run = runs.get(r);
					if (run.text.length() > 0) {
						SimpleAttributeSet attrs = new SimpleAttributeSet();
						StyleConstants.setForeground(attrs, colorFor(run.state));
						doc.insertString(doc.getLength(), run.text, attrs);
					}
				}

				int lineEndOffset = doc.getLength();
				doc.insertString(doc.getLength(), "\n", null);

				if (line.selected) {
					selectedRanges.add(new int[] { lineStartOffset, Math.max(lineStartOffset, lineEndOffset) });
				}
			}

			for (int i = 0; i < selectedRanges.size(); i++) {
				int[] range = selectedRanges.get(i);
				// Highlighter requires offset1 > offset0; a completely empty line would otherwise fail.
				textField_Disassembly.getHighlighter().addHighlight(range[0], Math.max(range[0] + 1, range[1]), SELECTED_LINE_PAINTER);
			}
		} catch (BadLocationException e) {
			// We only ever insert/remove at valid offsets we just computed; shouldn't happen.
		}

		textField_Disassembly.setCaretPosition(0);
	}

	/** Matches the Colors[] array in DisassemblyControlImpl.cpp exactly. */
	private static Color colorFor(DisassemblyLineColorizer.State state) {
		switch (state) {
			case NORMAL:
				return new Color(0, 0, 0);
			case COMMENT:
				return new Color(0, 128, 0);
			case NUMBER:
				return new Color(128, 0, 0);
			case STRING:
				return new Color(128, 0, 128);
			case INSTRUCTION:
				return new Color(0, 0, 128);
			case UNREFERENCED:
				return new Color(192, 192, 192);
			default:
				return Color.BLACK;
		}
	}

	/**
	 * Marks disassemblyLineIndex.get(lineIndex) as the sole selected line (matching the original,
	 * where only one line is ever .selected at a time -- see MouseMove in
	 * DisassemblyControlImpl.cpp, which clears every line's .selected before re-setting the one
	 * under the cursor), re-renders, and scrolls it into view.
	 */
	private void selectDisassemblyLine(int lineIndex) {
		for (int i = 0; i < disassemblyLineIndex.size(); i++) {
			disassemblyLineIndex.get(i).selected = (i == lineIndex);
		}
		rebuildDisassemblyDocument();
		try {
			textField_Disassembly.setCaretPosition(getDisassemblyLineStartOffset(lineIndex));
		} catch (BadLocationException e) {
			// Nothing sensible to scroll to.
		}
	}

	// --- JTextPane doesn't have JTextArea's getLineStartOffset/getLineEndOffset/getLineOfOffset,
	// so these reimplement the same thing off the document's line-Element structure directly. ---

	private int getDisassemblyLineStartOffset(int lineIndex) throws BadLocationException {
		Element root = textField_Disassembly.getDocument().getDefaultRootElement();
		if (lineIndex < 0 || lineIndex >= root.getElementCount()) {
			throw new BadLocationException("Invalid line index " + lineIndex, 0);
		}
		return root.getElement(lineIndex).getStartOffset();
	}

	private int getDisassemblyLineEndOffset(int lineIndex) throws BadLocationException {
		Element root = textField_Disassembly.getDocument().getDefaultRootElement();
		if (lineIndex < 0 || lineIndex >= root.getElementCount()) {
			throw new BadLocationException("Invalid line index " + lineIndex, 0);
		}
		return root.getElement(lineIndex).getEndOffset();
	}

	private int getDisassemblyLineOfOffset(int offset) {
		Element root = textField_Disassembly.getDocument().getDefaultRootElement();
		return root.getElementIndex(offset);
	}

	private void refreshDumpPanel() {
		hexDumpTableModel.fireTableDataChanged();
	}

	/**
	 * Returns the current linear byte-offset selection ([start, end], inclusive) in the hex
	 * dump, or null if nothing is selected.
	 */
	private int[] getSelectedByteRange() {
		if (hexDumpTableModel.segment == null || dumpSelectionAnchorOffset < 0) {
			return null;
		}
		int startOffset = Math.min(dumpSelectionAnchorOffset, dumpSelectionLeadOffset);
		int endOffset = Math.max(dumpSelectionAnchorOffset, dumpSelectionLeadOffset);

		int lastOffset = hexDumpTableModel.segment.getSize() - 1;
		startOffset = Math.max(0, Math.min(startOffset, lastOffset));
		endOffset = Math.max(0, Math.min(endOffset, lastOffset));
		return new int[] { startOffset, endOffset };
	}

	/**
	 * Corresponds to the "mark as byte/string/word" workflow: applies memoryType to every byte in
	 * the current hex-dump selection (directly via Segment.setType, the same call the engine's
	 * own byte-classification logic uses), then re-disassembles so the change takes effect.
	 *
	 * Word marking needs an even-length selection: only the first byte of each pair is ever
	 * inspected by the engine's WORD handling (see Disassembly.java), so tagging every byte WORD
	 * is harmless for correctly-paired bytes but would mispair a trailing odd byte with whatever
	 * comes after the selection -- rejected outright here rather than silently doing that.
	 */
	private void markDumpSelectionAs(MemoryType memoryType) {
		int[] range = getSelectedByteRange();
		if (range == null) {
			JOptionPane.showMessageDialog(this, "Select one or more bytes in the dump first.",
					"Mark Selection", JOptionPane.INFORMATION_MESSAGE);
			return;
		}
		int startOffset = range[0];
		int size = range[1] - range[0] + 1;

		if (memoryType == MemoryType.WORD && (size % 2) != 0) {
			JOptionPane.showMessageDialog(this,
					"A Word selection must have an even number of bytes (" + size + " selected).",
					"Mark Selection", JOptionPane.ERROR_MESSAGE);
			return;
		}

		// Captured before runDisassembly(), which internally resets the dump view to segment 0
		// via refreshSegmentsPanel() -- currentDumpSegmentIndex is not reliable after that call.
		int targetSegmentIndex = currentDumpSegmentIndex;
		Segment segment = workspace.getSegmentList().getSegment(targetSegmentIndex);
		segment.setType(startOffset, memoryType, size);

		runDisassembly();

		showSegmentInDump(targetSegmentIndex);
		setDumpSelection(startOffset, range[1]);
		navigateDisassemblyToOffset(targetSegmentIndex, startOffset);
	}

	// =====================================================================================
	// Hex dump pane context menu (full menu, matching the original C++ tool's "Start code
	// trace at selection" / "Change type of selected bytes to" / ... / "Save selection with
	// header..." menu). None of this is a translation of C++ source -- as noted at
	// openExecutableFile, the MFC view/controller layer (where that menu's command handlers
	// would have lived) isn't part of the C++ sources this project was given, only the
	// model/engine layer (Segment, SegmentList, Disassembly, ...) is. Every handler below is a
	// fresh implementation built on top of that already-ported model layer.
	// =====================================================================================

	/**
	 * Builds the hex dump pane's right-click context menu. Item enablement, and which "Change
	 * type" entry (if any) is checked, depend on the current selection and are refreshed each
	 * time the menu is about to be shown -- see refreshDumpPopupMenuState, wired up via the
	 * PopupMenuListener at the bottom of this method.
	 */
	private JPopupMenu buildDumpPopupMenu() {
		final JPopupMenu menu = new JPopupMenu();

		JMenuItem startCodeTrace = new JMenuItem("Start code trace at selection");
		startCodeTrace.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T, InputEvent.CTRL_MASK));
		startCodeTrace.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				handleStartCodeTrace();
			}
		});
		menu.add(startCodeTrace);

		JMenu changeTypeMenu = new JMenu("Change type of selected bytes to");
		addChangeTypeItem(changeTypeMenu, "Code", MemoryType.CODE);

		final JCheckBoxMenuItem typeLoByte = new JCheckBoxMenuItem("Code with Low Byte");
		typeLoByte.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				handleMarkSplitAddressByte(true);
			}
		});
		changeTypeMenu.add(typeLoByte);
		dumpChangeTypeMenuItems.put(MemoryType.LOBYTE, typeLoByte);

		final JCheckBoxMenuItem typeHiByte = new JCheckBoxMenuItem("Code with High Byte");
		typeHiByte.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				handleMarkSplitAddressByte(false);
			}
		});
		changeTypeMenu.add(typeHiByte);
		dumpChangeTypeMenuItems.put(MemoryType.HIBYTE, typeHiByte);

		addChangeTypeItem(changeTypeMenu, "Byte", MemoryType.BYTE);
		addChangeTypeItem(changeTypeMenu, "Word", MemoryType.WORD);
		addChangeTypeItem(changeTypeMenu, "Label", MemoryType.LABEL);
		addChangeTypeItem(changeTypeMenu, "SpartaDos X Label", MemoryType.SYMBOL);
		addChangeTypeItem(changeTypeMenu, "SpartaDos X Address Fix-Up", MemoryType.FIXUP);
		addChangeTypeItem(changeTypeMenu, "String", MemoryType.STRING);
		addChangeTypeItem(changeTypeMenu, "Screen Byte", MemoryType.SBYTE);
		addChangeTypeItem(changeTypeMenu, "Display List", MemoryType.DLIST);
		addChangeTypeItem(changeTypeMenu, "Data Store", MemoryType.STORE);
		changeTypeMenu.addSeparator();
		addChangeTypeItem(changeTypeMenu, "Unknown", MemoryType.UNKNOWN);
		menu.add(changeTypeMenu);

		JMenuItem setUnknownToByte = new JMenuItem("Set Unknown type to Byte on selection");
		setUnknownToByte.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				handleSetUnknownToByte();
			}
		});
		menu.add(setUnknownToByte);
		menu.addSeparator();

		JMenuItem addEditComment = new JMenuItem("Add/Edit comment...");
		addEditComment.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				handleAddEditComment();
			}
		});
		menu.add(addEditComment);

		JMenuItem editBytes = new JMenuItem("Edit bytes at selection (same as double-click)");
		editBytes.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0));
		editBytes.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				handleEditBytesAtSelection();
			}
		});
		menu.add(editBytes);

		JMenuItem assemble = new JMenuItem("Assemble at selection...");
		assemble.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0));
		assemble.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				handleAssembleAtSelection();
			}
		});
		menu.add(assemble);
		menu.addSeparator();

		dumpMenuCut = new JMenuItem("Cut");
		dumpMenuCut.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.CTRL_MASK));
		dumpMenuCut.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				handleDumpCut();
			}
		});
		menu.add(dumpMenuCut);

		dumpMenuCopy = new JMenuItem("Copy");
		dumpMenuCopy.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_MASK));
		dumpMenuCopy.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				handleDumpCopy();
			}
		});
		menu.add(dumpMenuCopy);

		dumpMenuPasteBefore = new JMenuItem("Paste (insert before selection)");
		dumpMenuPasteBefore.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_MASK));
		dumpMenuPasteBefore.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				handleDumpPaste(true);
			}
		});
		menu.add(dumpMenuPasteBefore);

		dumpMenuPasteAfter = new JMenuItem("Paste (insert after selection)");
		dumpMenuPasteAfter.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				handleDumpPaste(false);
			}
		});
		menu.add(dumpMenuPasteAfter);

		dumpMenuDelete = new JMenuItem("Delete");
		dumpMenuDelete.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));
		dumpMenuDelete.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				handleDumpDelete();
			}
		});
		menu.add(dumpMenuDelete);

		dumpMenuSplit = new JMenuItem("Split at selection");
		dumpMenuSplit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				handleSplitAtSelection();
			}
		});
		menu.add(dumpMenuSplit);
		menu.addSeparator();

		JMenuItem find = new JMenuItem("Find...");
		find.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_MASK));
		find.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				handleDumpFind();
			}
		});
		menu.add(find);

		dumpMenuFindNext = new JMenuItem("Find next");
		dumpMenuFindNext.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0));
		dumpMenuFindNext.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				handleDumpFindNext();
			}
		});
		menu.add(dumpMenuFindNext);
		menu.addSeparator();

		JMenuItem selectNextUnknown = new JMenuItem("Select next block of Unknown type");
		selectNextUnknown.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));
		selectNextUnknown.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				handleSelectNextUnknownBlock();
			}
		});
		menu.add(selectNextUnknown);

		JMenuItem selectSprites = new JMenuItem("Select Sprites...");
		selectSprites.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F4, 0));
		selectSprites.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				handleSelectSprites();
			}
		});
		menu.add(selectSprites);

		JMenuItem selectAll = new JMenuItem("Select all");
		selectAll.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.CTRL_MASK));
		selectAll.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				handleSelectAllInDump();
			}
		});
		menu.add(selectAll);
		menu.addSeparator();

		JMenuItem saveWithoutHeader = new JMenuItem("Save selection without header...");
		saveWithoutHeader.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				handleSaveSelection(false);
			}
		});
		menu.add(saveWithoutHeader);

		JMenuItem saveWithHeader = new JMenuItem("Save selection with header...");
		saveWithHeader.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				handleSaveSelection(true);
			}
		});
		menu.add(saveWithHeader);

		menu.addPopupMenuListener(new PopupMenuListener() {
			public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
				refreshDumpPopupMenuState();
			}

			public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
			}

			public void popupMenuCanceled(PopupMenuEvent e) {
			}
		});

		return menu;
	}

	/** Adds one "Change type of selected bytes to" entry, wired straight to markDumpSelectionAs. */
	private void addChangeTypeItem(JMenu menu, String label, final MemoryType memoryType) {
		final JCheckBoxMenuItem item = new JCheckBoxMenuItem(label);
		item.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				markDumpSelectionAs(memoryType);
			}
		});
		menu.add(item);
		dumpChangeTypeMenuItems.put(memoryType, item);
	}

	/**
	 * Registers the accelerator keys shown in the popup menu (Ctrl+A/C/X/V/F/T, Del, F2/F3/F4/F5/F8)
	 * directly on tableDump's input/action maps, so they work whenever the dump pane has focus,
	 * not only while the popup menu itself happens to be open (a JPopupMenu's own accelerators
	 * only fire while it's visible).
	 */
	private void registerDumpKeyBindings() {
		javax.swing.InputMap inputMap = tableDump.getInputMap(JComponent.WHEN_FOCUSED);
		javax.swing.ActionMap actionMap = tableDump.getActionMap();

		bindDumpKey(inputMap, actionMap, KeyStroke.getKeyStroke(KeyEvent.VK_T, InputEvent.CTRL_MASK), "startCodeTrace", new Runnable() {
			public void run() {
				handleStartCodeTrace();
			}
		});
		bindDumpKey(inputMap, actionMap, KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0), "editBytes", new Runnable() {
			public void run() {
				handleEditBytesAtSelection();
			}
		});
		bindDumpKey(inputMap, actionMap, KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0), "assemble", new Runnable() {
			public void run() {
				handleAssembleAtSelection();
			}
		});
		bindDumpKey(inputMap, actionMap, KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.CTRL_MASK), "cut", new Runnable() {
			public void run() {
				handleDumpCut();
			}
		});
		bindDumpKey(inputMap, actionMap, KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_MASK), "copy", new Runnable() {
			public void run() {
				handleDumpCopy();
			}
		});
		bindDumpKey(inputMap, actionMap, KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_MASK), "paste", new Runnable() {
			public void run() {
				handleDumpPaste(true);
			}
		});
		bindDumpKey(inputMap, actionMap, KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "delete", new Runnable() {
			public void run() {
				handleDumpDelete();
			}
		});
		bindDumpKey(inputMap, actionMap, KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_MASK), "find", new Runnable() {
			public void run() {
				handleDumpFind();
			}
		});
		bindDumpKey(inputMap, actionMap, KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0), "findNext", new Runnable() {
			public void run() {
				handleDumpFindNext();
			}
		});
		bindDumpKey(inputMap, actionMap, KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), "selectNextUnknown", new Runnable() {
			public void run() {
				handleSelectNextUnknownBlock();
			}
		});
		bindDumpKey(inputMap, actionMap, KeyStroke.getKeyStroke(KeyEvent.VK_F4, 0), "selectSprites", new Runnable() {
			public void run() {
				handleSelectSprites();
			}
		});
		bindDumpKey(inputMap, actionMap, KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.CTRL_MASK), "selectAll", new Runnable() {
			public void run() {
				handleSelectAllInDump();
			}
		});
	}

	private void bindDumpKey(javax.swing.InputMap inputMap, javax.swing.ActionMap actionMap, KeyStroke keyStroke,
			String name, final Runnable action) {
		inputMap.put(keyStroke, name);
		actionMap.put(name, new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				action.run();
			}
		});
	}

	/**
	 * Updates the popup menu's enabled/checked state to match the current selection: refreshes
	 * which "Change type" entry (if any) is checked, and disables Cut/Delete/Split (need a plain
	 * binary segment, see Segment.isSplittable), Paste (needs something Cut/Copied first), and
	 * Find next (needs a previous Find) when their preconditions aren't met.
	 */
	private void refreshDumpPopupMenuState() {
		int[] range = getSelectedByteRange();
		boolean hasSelection = range != null;
		Segment segment = hasSelection ? workspace.getSegmentList().getSegment(currentDumpSegmentIndex) : null;

		Iterator<JCheckBoxMenuItem> it = dumpChangeTypeMenuItems.values().iterator();
		while (it.hasNext()) {
			it.next().setSelected(false);
		}
		if (hasSelection) {
			MemoryType firstType = segment.getType(range[0]);
			boolean uniform = true;
			for (int offset = range[0] + 1; offset <= range[1] && uniform; offset++) {
				if (!segment.isType(offset, firstType)) {
					uniform = false;
				}
			}
			JCheckBoxMenuItem matchingItem = dumpChangeTypeMenuItems.get(firstType);
			if (uniform && matchingItem != null) {
				matchingItem.setSelected(true);
			}
		}

		boolean splittable = hasSelection && segment.isSplittable();
		int size = hasSelection ? range[1] - range[0] + 1 : 0;
		dumpMenuCut.setEnabled(splittable && segment.canDeleteRange(range[0], size));
		dumpMenuDelete.setEnabled(splittable && segment.canDeleteRange(range[0], size));
		dumpMenuCopy.setEnabled(hasSelection);
		dumpMenuPasteBefore.setEnabled(splittable && dumpClipboard != null
				&& segment.canInsertBytes(range[0], dumpClipboard.length));
		dumpMenuPasteAfter.setEnabled(splittable && dumpClipboard != null
				&& segment.canInsertBytes(range[1] + 1, dumpClipboard.length));
		dumpMenuSplit.setEnabled(hasSelection && segment.canSplitAt(range[0]));
		dumpMenuFindNext.setEnabled(lastFindPattern != null);
	}

	private void showNoDumpSelectionMessage() {
		JOptionPane.showMessageDialog(this, "Select one or more bytes in the dump first.",
				"Selection Required", JOptionPane.INFORMATION_MESSAGE);
	}

	private void showDumpCannotResizeMessage() {
		JOptionPane.showMessageDialog(this,
				"This needs a plain binary segment (not e.g. a SpartaDOS X relocatable/fix-up block),\n"
						+ "and can't remove or insert past the segment's entire remaining content.",
				"Not Available", JOptionPane.INFORMATION_MESSAGE);
	}

	/** "Start code trace at selection": explicitly marks the selection as Code and re-disassembles. */
	private void handleStartCodeTrace() {
		int[] range = getSelectedByteRange();
		if (range == null) {
			showNoDumpSelectionMessage();
			return;
		}
		int targetSegmentIndex = currentDumpSegmentIndex;
		Segment segment = workspace.getSegmentList().getSegment(targetSegmentIndex);
		segment.setType(range[0], MemoryType.CODE, range[1] - range[0] + 1);
		runDisassembly();
		showSegmentInDump(targetSegmentIndex);
		setDumpSelection(range[0], range[1]);
		navigateDisassemblyToOffset(targetSegmentIndex, range[0]);
	}

	/**
	 * "Code with Low/High Byte": marks a single immediate-operand byte as only half of a split
	 * 2-byte address (see MemoryType's class Javadoc). The other half of the address is supplied
	 * by the user and stored -- as this format requires -- in the *type* slot of the very next
	 * byte, which is therefore consumed as metadata and must not be the start of its own
	 * separately-typed byte.
	 */
	private void handleMarkSplitAddressByte(boolean lowByte) {
		int[] range = getSelectedByteRange();
		if (range == null) {
			showNoDumpSelectionMessage();
			return;
		}
		String label = "Code with " + (lowByte ? "Low" : "High") + " Byte";
		if (range[0] != range[1]) {
			JOptionPane.showMessageDialog(this,
					"Select exactly one byte: this marks a single immediate-operand byte as only\n"
							+ "half of a 2-byte address, with the other half supplied next.",
					label, JOptionPane.INFORMATION_MESSAGE);
			return;
		}

		int targetSegmentIndex = currentDumpSegmentIndex;
		Segment segment = workspace.getSegmentList().getSegment(targetSegmentIndex);
		int offset = range[0];
		if (offset + 1 >= segment.getSize()) {
			JOptionPane.showMessageDialog(this,
					"This can't be the last byte of the segment: the following byte's type slot\n"
							+ "is used to store the other half of the address.",
					label, JOptionPane.ERROR_MESSAGE);
			return;
		}

		String input = JOptionPane.showInputDialog(this,
				"Enter the full 16-bit target address (its " + (lowByte ? "high" : "low")
						+ " byte will be stored in the type slot of the byte right after this one):",
				"0000");
		if (input == null) {
			return;
		}
		int address;
		try {
			address = Integer.parseInt(input.trim().replaceFirst("^\\$", ""), 16) & 0xFFFF;
		} catch (NumberFormatException e) {
			JOptionPane.showMessageDialog(this, "Enter a hex address (e.g. 0600 or $0600).", label, JOptionPane.ERROR_MESSAGE);
			return;
		}

		if (lowByte) {
			segment.setType(offset, MemoryType.LOBYTE);
			segment.setType(offset + 1, MemoryType.fromByte((address >> 8) & 0xFF));
		} else {
			segment.setType(offset, MemoryType.HIBYTE);
			segment.setType(offset + 1, MemoryType.fromByte(address & 0xFF));
		}
		runDisassembly();
		showSegmentInDump(targetSegmentIndex);
		setDumpSelection(offset, offset);
		navigateDisassemblyToOffset(targetSegmentIndex, offset);
	}

	/** "Set Unknown type to Byte on selection": only touches bytes that are still Unknown. */
	private void handleSetUnknownToByte() {
		int[] range = getSelectedByteRange();
		if (range == null) {
			showNoDumpSelectionMessage();
			return;
		}
		int targetSegmentIndex = currentDumpSegmentIndex;
		Segment segment = workspace.getSegmentList().getSegment(targetSegmentIndex);
		for (int offset = range[0]; offset <= range[1]; offset++) {
			if (segment.isType(offset, MemoryType.UNKNOWN)) {
				segment.setType(offset, MemoryType.BYTE);
			}
		}
		runDisassembly();
		showSegmentInDump(targetSegmentIndex);
		setDumpSelection(range[0], range[1]);
		navigateDisassemblyToOffset(targetSegmentIndex, range[0]);
	}

	/** "Add/Edit comment...": one (possibly multi-line) Comment attached to the selection's first offset. */
	private void handleAddEditComment() {
		int[] range = getSelectedByteRange();
		if (range == null) {
			showNoDumpSelectionMessage();
			return;
		}
		int targetSegmentIndex = currentDumpSegmentIndex;
		Segment segment = workspace.getSegmentList().getSegment(targetSegmentIndex);
		int offset = range[0];

		JTextArea textArea = new JTextArea(segment.findComment(offset), 6, 40);
		JScrollPane scrollPane = new JScrollPane(textArea);
		int result = JOptionPane.showConfirmDialog(this, scrollPane,
				"Comment at offset $" + String.format("%04X", segment.wBegin + offset),
				JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
		if (result != JOptionPane.OK_OPTION) {
			return;
		}

		String newText = textArea.getText();
		segment.deleteComments(offset, 1);
		if (newText.trim().length() > 0) {
			Comment comment = segment.allocateComment();
			comment.setOffset(offset);
			comment.setText(newText);
		}
		runDisassembly();
		showSegmentInDump(targetSegmentIndex);
		setDumpSelection(range[0], range[1]);
		navigateDisassemblyToOffset(targetSegmentIndex, range[0]);
	}

	/** "Edit bytes at selection (same as double-click)": overwrite the selection's raw data bytes. */
	private void handleEditBytesAtSelection() {
		int[] range = getSelectedByteRange();
		if (range == null) {
			showNoDumpSelectionMessage();
			return;
		}
		int targetSegmentIndex = currentDumpSegmentIndex;
		Segment segment = workspace.getSegmentList().getSegment(targetSegmentIndex);
		int startOffset = range[0];
		int size = range[1] - startOffset + 1;

		StringBuilder existingHex = new StringBuilder();
		for (int i = 0; i < size; i++) {
			if (i > 0) {
				existingHex.append(' ');
			}
			existingHex.append(String.format("%02X", segment.getData(startOffset + i)));
		}

		String input = JOptionPane.showInputDialog(this,
				"Enter " + size + " byte" + (size == 1 ? "" : "s") + " of hex data, space-separated:",
				existingHex.toString());
		if (input == null) {
			return;
		}

		int[] bytes;
		try {
			bytes = parseHexBytes(input);
		} catch (NumberFormatException e) {
			JOptionPane.showMessageDialog(this, "Could not parse hex bytes: " + e.getMessage(), "Edit Bytes", JOptionPane.ERROR_MESSAGE);
			return;
		}
		if (bytes.length != size) {
			JOptionPane.showMessageDialog(this, "Expected exactly " + size + " byte(s), got " + bytes.length + ".",
					"Edit Bytes", JOptionPane.ERROR_MESSAGE);
			return;
		}

		for (int i = 0; i < size; i++) {
			segment.setData(startOffset + i, bytes[i]);
		}
		runDisassembly();
		showSegmentInDump(targetSegmentIndex);
		setDumpSelection(range[0], range[1]);
		navigateDisassemblyToOffset(targetSegmentIndex, range[0]);
	}

	/**
	 * "Assemble at selection...": assembles one or more lines of 6502/65C02 assembly (see
	 * dis6502.core.Assembler) and writes the result over the selection's existing bytes -- it
	 * must assemble to exactly the selection's length, since this overwrites in place rather
	 * than resizing the segment (use Cut/Paste or Split first if the size needs to change).
	 */
	private void handleAssembleAtSelection() {
		int[] range = getSelectedByteRange();
		if (range == null) {
			showNoDumpSelectionMessage();
			return;
		}
		int targetSegmentIndex = currentDumpSegmentIndex;
		Segment segment = workspace.getSegmentList().getSegment(targetSegmentIndex);
		int startOffset = range[0];
		int size = range[1] - startOffset + 1;
		int address = (segment.wBegin + startOffset) & 0xFFFF;

		JTextArea textArea = new JTextArea(6, 40);
		textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
		JScrollPane scrollPane = new JScrollPane(textArea);
		int result = JOptionPane.showConfirmDialog(this, scrollPane,
				"Assemble at $" + String.format("%04X", address) + " (must encode to exactly " + size + " byte(s)):",
				JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
		if (result != JOptionPane.OK_OPTION) {
			return;
		}

		InstructionSet instructionSet = workspace.getInstructionSet(segment.processorType);
		byte[] assembled;
		try {
			assembled = Assembler.assembleLines(instructionSet, address, textArea.getText());
		} catch (Assembler.AssemblyException e) {
			JOptionPane.showMessageDialog(this, e.getMessage(), "Assemble", JOptionPane.ERROR_MESSAGE);
			return;
		}
		if (assembled.length != size) {
			JOptionPane.showMessageDialog(this,
					"Assembled to " + assembled.length + " byte(s), but the selection is " + size + " byte(s).\n"
							+ "Adjust the code (or the selection) so the lengths match.",
					"Assemble", JOptionPane.ERROR_MESSAGE);
			return;
		}

		for (int i = 0; i < assembled.length; i++) {
			segment.setData(startOffset + i, assembled[i] & 0xFF);
		}
		segment.setType(startOffset, MemoryType.CODE, assembled.length);
		runDisassembly();
		showSegmentInDump(targetSegmentIndex);
		setDumpSelection(range[0], range[1]);
		navigateDisassemblyToOffset(targetSegmentIndex, range[0]);
	}

	/** "Copy": copies the selected bytes to the in-app clipboard (see the dumpClipboard field). */
	private void handleDumpCopy() {
		int[] range = getSelectedByteRange();
		if (range == null) {
			showNoDumpSelectionMessage();
			return;
		}
		Segment segment = workspace.getSegmentList().getSegment(currentDumpSegmentIndex);
		int size = range[1] - range[0] + 1;
		byte[] copy = new byte[size];
		for (int i = 0; i < size; i++) {
			copy[i] = (byte) segment.getData(range[0] + i);
		}
		dumpClipboard = copy;
		appendLogLine("Copied " + size + " byte(s).");
	}

	/** "Cut": Copy, then remove the selected bytes from the segment (see Segment.deleteRange). */
	private void handleDumpCut() {
		int[] range = getSelectedByteRange();
		if (range == null) {
			showNoDumpSelectionMessage();
			return;
		}
		int targetSegmentIndex = currentDumpSegmentIndex;
		Segment segment = workspace.getSegmentList().getSegment(targetSegmentIndex);
		int size = range[1] - range[0] + 1;
		if (!segment.canDeleteRange(range[0], size)) {
			showDumpCannotResizeMessage();
			return;
		}

		byte[] copy = new byte[size];
		for (int i = 0; i < size; i++) {
			copy[i] = (byte) segment.getData(range[0] + i);
		}
		dumpClipboard = copy;

		segment.deleteRange(range[0], size);
		runDisassembly();
		showSegmentInDump(targetSegmentIndex);
		int newOffset = Math.min(range[0], Math.max(0, segment.getSize() - 1));
		setDumpSelection(newOffset, newOffset);
		navigateDisassemblyToOffset(targetSegmentIndex, newOffset);
	}

	/** "Delete": removes the selected bytes from the segment without touching the clipboard. */
	private void handleDumpDelete() {
		int[] range = getSelectedByteRange();
		if (range == null) {
			showNoDumpSelectionMessage();
			return;
		}
		int targetSegmentIndex = currentDumpSegmentIndex;
		Segment segment = workspace.getSegmentList().getSegment(targetSegmentIndex);
		int size = range[1] - range[0] + 1;
		if (!segment.canDeleteRange(range[0], size)) {
			showDumpCannotResizeMessage();
			return;
		}

		segment.deleteRange(range[0], size);
		runDisassembly();
		showSegmentInDump(targetSegmentIndex);
		int newOffset = Math.min(range[0], Math.max(0, segment.getSize() - 1));
		setDumpSelection(newOffset, newOffset);
		navigateDisassemblyToOffset(targetSegmentIndex, newOffset);
	}

	/** "Paste (insert before/after selection)": grows the segment with the clipboard's bytes. */
	private void handleDumpPaste(boolean insertBefore) {
		if (dumpClipboard == null) {
			JOptionPane.showMessageDialog(this, "Nothing has been Cut or Copied yet.", "Paste", JOptionPane.INFORMATION_MESSAGE);
			return;
		}
		int[] range = getSelectedByteRange();
		if (range == null) {
			showNoDumpSelectionMessage();
			return;
		}
		int targetSegmentIndex = currentDumpSegmentIndex;
		Segment segment = workspace.getSegmentList().getSegment(targetSegmentIndex);
		int insertOffset = insertBefore ? range[0] : range[1] + 1;
		if (!segment.canInsertBytes(insertOffset, dumpClipboard.length)) {
			showDumpCannotResizeMessage();
			return;
		}

		segment.insertBytes(insertOffset, dumpClipboard);
		runDisassembly();
		showSegmentInDump(targetSegmentIndex);
		setDumpSelection(insertOffset, insertOffset + dumpClipboard.length - 1);
		navigateDisassemblyToOffset(targetSegmentIndex, insertOffset);
	}

	/** "Split at selection": splits the segment in two at the start of the selection. */
	private void handleSplitAtSelection() {
		int[] range = getSelectedByteRange();
		if (range == null) {
			showNoDumpSelectionMessage();
			return;
		}
		int targetSegmentIndex = currentDumpSegmentIndex;
		SegmentList segmentList = workspace.getSegmentList();
		Segment segment = segmentList.getSegment(targetSegmentIndex);
		if (!segment.canSplitAt(range[0])) {
			JOptionPane.showMessageDialog(this,
					"Can't split here (either at the very start of the segment, or this isn't a plain binary segment).",
					"Split at Selection", JOptionPane.INFORMATION_MESSAGE);
			return;
		}

		segmentList.setSelectedIndex(targetSegmentIndex);
		segmentList.splitSelectedSegment(range[0]);
		runDisassembly();
		showSegmentInDump(targetSegmentIndex);
		navigateDisassemblyToOffset(targetSegmentIndex, 0);
	}

	/** "Find...": searches from just after the current selection to the end of this segment. */
	private void handleDumpFind() {
		if (hexDumpTableModel.segment == null) {
			return;
		}
		String input = JOptionPane.showInputDialog(this,
				"Find hex bytes (e.g. A9 00 8D) or text (prefix with \" , e.g. \"HELLO):", "");
		if (input == null || input.trim().length() == 0) {
			return;
		}

		byte[] pattern;
		try {
			pattern = parseFindPattern(input);
		} catch (NumberFormatException e) {
			JOptionPane.showMessageDialog(this, "Could not parse search pattern: " + e.getMessage(), "Find", JOptionPane.ERROR_MESSAGE);
			return;
		}
		lastFindPattern = pattern;

		int[] range = getSelectedByteRange();
		int startFrom = range != null ? range[1] + 1 : 0;
		findAndSelect(pattern, startFrom);
	}

	/** "Find next": repeats the last Find, again from just after the current selection. */
	private void handleDumpFindNext() {
		if (lastFindPattern == null) {
			JOptionPane.showMessageDialog(this, "Use Find... first.", "Find Next", JOptionPane.INFORMATION_MESSAGE);
			return;
		}
		int[] range = getSelectedByteRange();
		int startFrom = range != null ? range[1] + 1 : 0;
		findAndSelect(lastFindPattern, startFrom);
	}

	/** Searches the current segment only (not across segments) for pattern, starting at startFrom. */
	private void findAndSelect(byte[] pattern, int startFrom) {
		Segment segment = hexDumpTableModel.segment;
		if (segment == null) {
			return;
		}
		int size = segment.getSize();
		for (int offset = Math.max(0, startFrom); offset <= size - pattern.length; offset++) {
			boolean match = true;
			for (int i = 0; i < pattern.length && match; i++) {
				if (segment.getData(offset + i) != (pattern[i] & 0xFF)) {
					match = false;
				}
			}
			if (match) {
				setDumpSelection(offset, offset + pattern.length - 1);
				handleDumpSelectionChanged();
				return;
			}
		}
		JOptionPane.showMessageDialog(this, "Not found (searched from the current selection to the end of this segment).",
				"Find", JOptionPane.INFORMATION_MESSAGE);
	}

	/**
	 * "Select next block of Unknown type": finds the next contiguous run of Unknown-typed bytes,
	 * searching forward (and wrapping around) from just after the current selection. A run that
	 * wraps past the end of the segment is reported up to the last offset rather than merged
	 * with one starting at offset 0.
	 */
	private void handleSelectNextUnknownBlock() {
		Segment segment = hexDumpTableModel.segment;
		if (segment == null || segment.isEmpty()) {
			showNoDumpSelectionMessage();
			return;
		}
		int size = segment.getSize();
		int[] range = getSelectedByteRange();
		int searchFrom = range != null ? range[1] + 1 : 0;

		int start = -1;
		for (int i = 0; i < size; i++) {
			int offset = (searchFrom + i) % size;
			if (segment.isType(offset, MemoryType.UNKNOWN)) {
				start = offset;
				break;
			}
		}
		if (start < 0) {
			JOptionPane.showMessageDialog(this, "No bytes of Unknown type in this segment.",
					"Select Next Unknown Block", JOptionPane.INFORMATION_MESSAGE);
			return;
		}

		int end = start;
		while (end + 1 < size && segment.isType(end + 1, MemoryType.UNKNOWN)) {
			end++;
		}
		setDumpSelection(start, end);
		handleDumpSelectionChanged();
	}

	/**
	 * "Select Sprites...": a simplified stand-in for the original tool's sprite-detection dialog
	 * (its exact heuristics for recognizing Player/Missile graphics data aren't available to
	 * port -- see this class's top-of-section javadoc). Selects N shapes of 8 bytes each
	 * (the size of one un-stretched Atari Player/Missile graphics row set) starting at the
	 * current selection, letting the user at least quickly block out sprite data by eye.
	 */
	private void handleSelectSprites() {
		Segment segment = hexDumpTableModel.segment;
		if (segment == null || segment.isEmpty()) {
			showNoDumpSelectionMessage();
			return;
		}
		int[] range = getSelectedByteRange();
		int start = range != null ? range[0] : 0;

		String input = JOptionPane.showInputDialog(this,
				"This is a simplified stand-in for the original tool's sprite-selection dialog\n"
						+ "(its exact detection heuristics aren't available to port). Enter how many\n"
						+ "8-byte shapes to select, starting at the current selection:",
				"1");
		if (input == null) {
			return;
		}
		int count;
		try {
			count = Integer.parseInt(input.trim());
		} catch (NumberFormatException e) {
			JOptionPane.showMessageDialog(this, "Enter a whole number.", "Select Sprites", JOptionPane.ERROR_MESSAGE);
			return;
		}
		if (count <= 0) {
			return;
		}

		int end = Math.min(segment.getSize() - 1, start + count * 8 - 1);
		setDumpSelection(start, end);
		handleDumpSelectionChanged();
	}

	/** "Select all": selects the entire currently-displayed segment. */
	private void handleSelectAllInDump() {
		Segment segment = hexDumpTableModel.segment;
		if (segment == null || segment.isEmpty()) {
			return;
		}
		setDumpSelection(0, segment.getSize() - 1);
		handleDumpSelectionChanged();
	}

	/**
	 * "Save selection without/with header...": writes the selected bytes to a file, optionally
	 * prefixed with a standard Atari DOS 2.x "$FFFF" + start/end address header (the same
	 * 6-byte format AtariExecutableReader/ATARI_BINARY reads back), so the saved file can be
	 * re-opened elsewhere via "Open Executable File".
	 */
	private void handleSaveSelection(boolean withHeader) {
		int[] range = getSelectedByteRange();
		if (range == null) {
			showNoDumpSelectionMessage();
			return;
		}
		Segment segment = hexDumpTableModel.segment;
		int size = range[1] - range[0] + 1;

		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setDialogTitle(withHeader ? "Save Selection With Header" : "Save Selection Without Header");
		if (fileChooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
			return;
		}
		File file = fileChooser.getSelectedFile();

		java.io.FileOutputStream out = null;
		try {
			out = new java.io.FileOutputStream(file);
			if (withHeader) {
				int startAddress = (segment.wBegin + range[0]) & 0xFFFF;
				int endAddress = (segment.wBegin + range[1]) & 0xFFFF;
				out.write(0xFF);
				out.write(0xFF);
				out.write(startAddress & 0xFF);
				out.write((startAddress >> 8) & 0xFF);
				out.write(endAddress & 0xFF);
				out.write((endAddress >> 8) & 0xFF);
			}
			for (int i = 0; i < size; i++) {
				out.write(segment.getData(range[0] + i));
			}
			appendLogLine("Saved " + size + " byte(s) to \"" + file.getAbsolutePath() + "\".");
		} catch (IOException e) {
			JOptionPane.showMessageDialog(this, "Could not save file:\n" + e.getMessage(), "Save Selection", JOptionPane.ERROR_MESSAGE);
		} finally {
			if (out != null) {
				try {
					out.close();
				} catch (IOException e) {
					// Ignore: nothing useful to do if closing fails after a successful write.
				}
			}
		}
	}

	/** Parses a space-separated list of hex byte tokens (each optionally "$"-prefixed) into ints 0-255. */
	private int[] parseHexBytes(String input) throws NumberFormatException {
		String trimmed = input.trim();
		if (trimmed.length() == 0) {
			return new int[0];
		}
		String[] tokens = trimmed.split("\\s+");
		int[] result = new int[tokens.length];
		for (int i = 0; i < tokens.length; i++) {
			result[i] = Integer.parseInt(tokens[i].replaceFirst("^\\$", ""), 16) & 0xFF;
		}
		return result;
	}

	/** Parses a Find pattern: hex byte tokens, or a "-quoted ASCII string. */
	private byte[] parseFindPattern(String input) throws NumberFormatException {
		String trimmed = input.trim();
		if (trimmed.startsWith("\"")) {
			String text = trimmed.substring(1);
			if (text.endsWith("\"")) {
				text = text.substring(0, text.length() - 1);
			}
			byte[] result = new byte[text.length()];
			for (int i = 0; i < text.length(); i++) {
				result[i] = (byte) text.charAt(i);
			}
			return result;
		}

		int[] ints = parseHexBytes(trimmed);
		byte[] result = new byte[ints.length];
		for (int i = 0; i < ints.length; i++) {
			result[i] = (byte) ints[i];
		}
		return result;
	}

	/** Called whenever the hex-dump table's selection settles (mouse released / keyboard nav done). */
	private void handleDumpSelectionChanged() {
		int[] range = getSelectedByteRange();
		if (range != null) {
			navigateDisassemblyToOffset(currentDumpSegmentIndex, range[0]);
		}
	}

	/**
	 * The reverse of double-clicking a disassembly line: given a byte offset selected in the hex
	 * dump, finds the disassembled line whose [offset, offset+size) range contains it and makes
	 * it the sole highlighted ("yellow band") line in the Disassembly panel, scrolling it into view.
	 */
	private void navigateDisassemblyToOffset(int segmentIndex, int offset) {
		for (int i = 0; i < disassemblyLineIndex.size(); i++) {
			DisassemblyLine line = disassemblyLineIndex.get(i);
			if (line.segmentIndex == segmentIndex && line.size > 0
					&& offset >= line.offset && offset < line.offset + line.size) {
				selectDisassemblyLine(i);
				return;
			}
		}
	}

	/** Maps a click point in textField_Disassembly back to the DisassemblyLine it landed on, or null. */
	private DisassemblyLine disassemblyLineAt(Point point) {
		int modelPosition = textField_Disassembly.viewToModel(point);
		int lineIndex = getDisassemblyLineOfOffset(modelPosition);
		if (lineIndex >= 0 && lineIndex < disassemblyLineIndex.size()) {
			return disassemblyLineIndex.get(lineIndex);
		}
		return null;
	}

	/**
	 * Single click: populate the Reference List panel with every other disassembled line that
	 * refers to the same address as the clicked line. Double click: additionally jump the hex
	 * dump panel to that line's own bytes (switching the displayed segment first, if needed).
	 */
	/**
	 * A single click on a disassembly line: populates the Reference List panel with every other
	 * line that refers to the same address, and jumps the hex dump (memory inspector) panel to
	 * that line's own bytes -- both happen on every click now (this used to require a double
	 * click for the dump sync, which was inconsistent with the Reference List panel's
	 * single-click behavior and easy to miss).
	 */
	private void handleDisassemblyClick(MouseEvent e) {
		DisassemblyLine clickedLine = disassemblyLineAt(e.getPoint());
		if (clickedLine == null) {
			return;
		}

		// If the clicked line's instruction resolves to an address (e.g. "bne L0607" has
		// address == 0x0607), cross-reference on that target -- this is what a click on a
		// reference itself should show (every other place that also refers to L0607). If the
		// line carries no resolved address of its own (e.g. a label-definition line like
		// "L0607 inx", or a directive), fall back to this line's own address (segment start +
		// offset), so clicking the *definition* also shows everyone who references it.
		int targetAddress = clickedLine.address;
		if (targetAddress == 0) {
			Segment segment = workspace.getSegmentList().getSegment(clickedLine.segmentIndex);
			targetAddress = segment.wBegin + clickedLine.offset;
		}
		refreshReferenceListPanel(targetAddress);

		navigateDumpToOffset(clickedLine.segmentIndex, clickedLine.offset);

		// Highlight the clicked line itself (overriding whatever caret position/selection the
		// click itself produced by default), consistent with clicking a line in the Reference
		// List or Segments panels, which already highlight themselves.
		navigateDisassemblyToOffset(clickedLine.segmentIndex, clickedLine.offset);
	}

	/**
	 * Corresponds to the "Reference List" panel: every CODE_LINES line whose resolved operand
	 * address equals targetAddress, i.e. every place that reads/writes/jumps to/branches to that
	 * address. This is a narrower, address-based cross-reference built directly from data the
	 * engine already computes (DisassemblyLine.address); the original app's more general XRef
	 * text-search window (MainXRef/DisassemblyResultTest) has not been ported.
	 */
	private void refreshReferenceListPanel(int targetAddress) {
		referenceLineIndex.clear();

		if (targetAddress == 0) {
			textField_Reference.setText("");
			txtReferenceList.setText("  Reference List");
			return;
		}

		StringBuilder text = new StringBuilder();
		int count = 0;
		for (int i = 0; i < disassemblyLineIndex.size(); i++) {
			DisassemblyLine line = disassemblyLineIndex.get(i);
			if (line.address == targetAddress) {
				Segment segment = workspace.getSegmentList().getSegment(line.segmentIndex);
				int address = segment.wBegin + line.offset;
				text.append(String.format("%04X", address)).append("  ").append(line.getLine().trim()).append('\n');
				referenceLineIndex.add(line);
				count++;
			}
		}
		textField_Reference.setText(text.toString());
		textField_Reference.setCaretPosition(0);
		txtReferenceList.setText("  References to $" + String.format("%04X", targetAddress) + " (" + count + ")");
	}

	/**
	 * Clicking a line in the Reference List panel jumps both the Disassembly panel and the hex
	 * dump (memory inspector) panel to that same address, and highlights the clicked line itself
	 * in the Reference List for visual feedback.
	 */
	private void handleReferenceClick(MouseEvent e) {
		int modelPosition = textField_Reference.viewToModel(e.getPoint());
		int lineIndex;
		try {
			lineIndex = textField_Reference.getLineOfOffset(modelPosition);
		} catch (BadLocationException ex) {
			return;
		}
		if (lineIndex < 0 || lineIndex >= referenceLineIndex.size()) {
			return;
		}
		DisassemblyLine line = referenceLineIndex.get(lineIndex);

		try {
			int lineStart = textField_Reference.getLineStartOffset(lineIndex);
			int lineEnd = textField_Reference.getLineEndOffset(lineIndex);
			textField_Reference.setCaretPosition(lineStart);
			textField_Reference.moveCaretPosition(lineEnd - 1);
			textField_Reference.getCaret().setSelectionVisible(true);
		} catch (BadLocationException ex) {
			// Text changed out from under us; ignore.
		}

		navigateDisassemblyToOffset(line.segmentIndex, line.offset);
		navigateDumpToOffset(line.segmentIndex, line.offset);
	}

	/**
	 * Clicking a segment in the segments list panel (top left) selects that segment in the
	 * engine and jumps both the hex dump panel and the Disassembly panel to its start -- each
	 * line of that panel's text is exactly one segment, in order, so the clicked line index is
	 * the segment index directly.
	 */
	private void handleSegmentsClick(MouseEvent e) {
		int modelPosition = textField_FileLoaded.viewToModel(e.getPoint());
		int segmentIndex;
		try {
			segmentIndex = textField_FileLoaded.getLineOfOffset(modelPosition);
		} catch (BadLocationException ex) {
			return;
		}
		SegmentList segmentList = workspace.getSegmentList();
		if (segmentIndex < 0 || segmentIndex >= segmentList.getCount()) {
			return;
		}

		// setSelectedIndex() synchronously fires the SegmentListChangedListener wired in
		// initEngine(), which calls refreshSegmentsPanel() -- and that replaces
		// textField_FileLoaded's whole text via setText(), which would wipe out any selection
		// set beforehand. So the highlighting below has to happen *after* all of this, not before.
		segmentList.setSelectedIndex(segmentIndex);
		navigateDumpToOffset(segmentIndex, 0);
		navigateDisassemblyToOffset(segmentIndex, 0);

		try {
			int lineStart = textField_FileLoaded.getLineStartOffset(segmentIndex);
			int lineEnd = textField_FileLoaded.getLineEndOffset(segmentIndex);
			textField_FileLoaded.setCaretPosition(lineStart);
			textField_FileLoaded.moveCaretPosition(lineEnd - 1);
			textField_FileLoaded.getCaret().setSelectionVisible(true);
		} catch (BadLocationException ex) {
			// Text changed out from under us; ignore.
		}
	}

	/** Switches the hex-dump panel to the given segment (if needed) and selects the byte at offset. */
	private void navigateDumpToOffset(int segmentIndex, int offset) {
		if (segmentIndex != currentDumpSegmentIndex) {
			showSegmentInDump(segmentIndex);
		}

		int row = offset / HexDumpTableModel.BYTES_PER_ROW;
		if (row < 0 || row >= tableDump.getRowCount()) {
			return;
		}

		setDumpSelection(offset, offset);
		tableDump.requestFocusInWindow();
	}

	private void appendLogLine(String message) {
		textField_Log.append(message);
		textField_Log.append("\n");
		textField_Log.setCaretPosition(textField_Log.getDocument().getLength());
	}
	
}

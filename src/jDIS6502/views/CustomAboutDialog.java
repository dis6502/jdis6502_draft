package jDIS6502.views;

import java.awt.*;
import javax.swing.*;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import java.awt.Toolkit;
import javax.swing.JTextField;
import java.awt.Color;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.ImageIcon;
import javax.swing.SpringLayout;
import javax.swing.JFormattedTextField;
import java.awt.Font;
import java.awt.Component;
import javax.swing.SwingConstants;
import javax.swing.JSeparator;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.JLabel;
import javax.swing.border.CompoundBorder;
import javax.swing.border.BevelBorder;
import javax.swing.border.EtchedBorder;

public class CustomAboutDialog extends JDialog {
	
	SpringLayout springLayout;
	private final JPanel contentPanel = new JPanel();
	private JTextField txtHttpSourceforgenetprojectsdis;
	private JTextField txtcEric;
	private JTextField txtWinPort;
	private JTextField txtWinFixes;
	private JTextField txtJavaPort;
	private JTextField txtThePurposeOf;
	private JTextField txtBinaryFileAnd;
	private JTextField txtFeelFreeTo;
	private JTextField txtBugReportsOn;
	private JTextField txtTheDisassembler;
	private JFormattedTextField frmtdTextFieldjDis6502;
	private JSeparator separator;
	private JButton btnOk;
	private JLabel lblNewLabel;
	private JLabel lblNewLabel_1;
	private JTextField txtDumpctldllAtari;
	private JTextField txtDisctldllAtari;
	private JTextField txtSpritctldllAtari;
	
	public CustomAboutDialog() {
		getContentPane().setFont(new Font("Tahoma", Font.PLAIN, 14));
		setAlwaysOnTop(true);
		setBounds(100, 100, 600, 743);
		setIconImage(Toolkit.getDefaultToolkit().getImage(CustomAboutDialog.class.getResource("/jDIS6502/resources/dis6502.png")));
		setTitle(" About DIS6502");
		setBackground(Color.WHITE);
		getContentPane().setBackground(Color.WHITE);
		springLayout = new SpringLayout();
		getContentPane().setLayout(springLayout);
		
		frmtdTextFieldjDis6502 = new JFormattedTextField();
		springLayout.putConstraint(SpringLayout.NORTH, frmtdTextFieldjDis6502, 50, SpringLayout.NORTH, getContentPane());
		frmtdTextFieldjDis6502.setHorizontalAlignment(SwingConstants.CENTER);
		frmtdTextFieldjDis6502.setFocusable(false);
		frmtdTextFieldjDis6502.setEditable(false);
		frmtdTextFieldjDis6502.setFont(new Font("Tahoma", Font.BOLD, 24));
		frmtdTextFieldjDis6502.setBorder(null);
		frmtdTextFieldjDis6502.setBackground(Color.WHITE);
		frmtdTextFieldjDis6502.setText("jDIS6502");
		getContentPane().add(frmtdTextFieldjDis6502);
		
		txtTheDisassembler = new JTextField();
		springLayout.putConstraint(SpringLayout.NORTH, txtTheDisassembler, 0, SpringLayout.SOUTH, frmtdTextFieldjDis6502);
		txtTheDisassembler.setHorizontalAlignment(SwingConstants.CENTER);
		txtTheDisassembler.setFont(new Font("Tahoma", Font.PLAIN, 18));
		txtTheDisassembler.setBorder(null);
		txtTheDisassembler.setText("The 6502 Disassembler");
		getContentPane().add(txtTheDisassembler);
		txtTheDisassembler.setColumns(10);
		
		txtHttpSourceforgenetprojectsdis = new JTextField();
		txtHttpSourceforgenetprojectsdis.setFont(new Font("Tahoma", Font.BOLD, 16));
		springLayout.putConstraint(SpringLayout.NORTH, txtHttpSourceforgenetprojectsdis, 60, SpringLayout.SOUTH, txtTheDisassembler);
		txtHttpSourceforgenetprojectsdis.setBorder(null);
		springLayout.putConstraint(SpringLayout.WEST, txtHttpSourceforgenetprojectsdis, 10, SpringLayout.WEST, getContentPane());
		springLayout.putConstraint(SpringLayout.EAST, txtHttpSourceforgenetprojectsdis, -10, SpringLayout.EAST, getContentPane());
		txtHttpSourceforgenetprojectsdis.setHorizontalAlignment(SwingConstants.CENTER);
		txtHttpSourceforgenetprojectsdis.setText("http:// sourceforge.net/projects/dis6502");
		getContentPane().add(txtHttpSourceforgenetprojectsdis);
		txtHttpSourceforgenetprojectsdis.setColumns(10);
		
		txtcEric = new JTextField();
		txtcEric.setFont(new Font("Tahoma", Font.BOLD, 16));
		txtcEric.setHorizontalAlignment(SwingConstants.CENTER);
		txtcEric.setBorder(null);
		springLayout.putConstraint(SpringLayout.WEST, txtcEric, 0, SpringLayout.WEST, getContentPane());
		springLayout.putConstraint(SpringLayout.EAST, txtcEric, 0, SpringLayout.EAST, getContentPane());
		txtcEric.setText("(c) 1997-2017 Eric Bacher  atari@bacher.info");
		springLayout.putConstraint(SpringLayout.NORTH, txtcEric, 6, SpringLayout.SOUTH, txtHttpSourceforgenetprojectsdis);
		getContentPane().add(txtcEric);
		txtcEric.setColumns(10);
		
		txtWinPort = new JTextField();
		txtWinPort.setFont(new Font("Tahoma", Font.BOLD, 16));
		txtWinPort.setHorizontalAlignment(SwingConstants.CENTER);
		springLayout.putConstraint(SpringLayout.WEST, txtWinPort, 0, SpringLayout.WEST, getContentPane());
		springLayout.putConstraint(SpringLayout.EAST, txtWinPort, 0, SpringLayout.EAST, getContentPane());
		txtWinPort.setBorder(null);
		txtWinPort.setText("Win32 Port - 2005 by James Wilkinson  james@slor.net");
		springLayout.putConstraint(SpringLayout.NORTH, txtWinPort, 6, SpringLayout.SOUTH, txtcEric);
		getContentPane().add(txtWinPort);
		txtWinPort.setColumns(10);
		
		txtWinFixes = new JTextField();
		txtWinFixes.setFont(new Font("Tahoma", Font.BOLD, 16));
		txtWinFixes.setBorder(null);
		springLayout.putConstraint(SpringLayout.WEST, txtWinFixes, 0, SpringLayout.WEST, getContentPane());
		springLayout.putConstraint(SpringLayout.EAST, txtWinFixes, 0, SpringLayout.EAST, getContentPane());
		txtWinFixes.setHorizontalAlignment(SwingConstants.CENTER);
		txtWinFixes.setText("Win32 Fixes - 2015-2017 by Peter Dell  jac@wudsn.com");
		springLayout.putConstraint(SpringLayout.NORTH, txtWinFixes, 6, SpringLayout.SOUTH, txtWinPort);
		getContentPane().add(txtWinFixes);
		txtWinFixes.setColumns(10);
		
		txtJavaPort = new JTextField();
		txtJavaPort.setFont(new Font("Tahoma", Font.BOLD, 16));
		txtJavaPort.setBorder(null);
		springLayout.putConstraint(SpringLayout.WEST, txtJavaPort, 0, SpringLayout.WEST, getContentPane());
		springLayout.putConstraint(SpringLayout.EAST, txtJavaPort, 0, SpringLayout.EAST, getContentPane());
		txtJavaPort.setHorizontalAlignment(SwingConstants.CENTER);
		txtJavaPort.setText("Java 6 Port - 2026 by Ken Ames  kenames@gmx.com");
		springLayout.putConstraint(SpringLayout.NORTH, txtJavaPort, 6, SpringLayout.SOUTH, txtWinFixes);
		getContentPane().add(txtJavaPort);
		txtJavaPort.setColumns(10);
		
		txtThePurposeOf = new JTextField();
		txtThePurposeOf.setFont(new Font("Tahoma", Font.BOLD, 16));
		txtThePurposeOf.setBorder(null);
		txtThePurposeOf.setHorizontalAlignment(SwingConstants.CENTER);
		springLayout.putConstraint(SpringLayout.NORTH, txtThePurposeOf, 20, SpringLayout.SOUTH, txtJavaPort);
		springLayout.putConstraint(SpringLayout.WEST, txtThePurposeOf, 0, SpringLayout.WEST, getContentPane());
		springLayout.putConstraint(SpringLayout.EAST, txtThePurposeOf, 0, SpringLayout.EAST, getContentPane());
		txtThePurposeOf.setText("The purpose of this software is to disassemble a 6502");
		getContentPane().add(txtThePurposeOf);
		txtThePurposeOf.setColumns(10);
		
		txtBinaryFileAnd = new JTextField();
		txtBinaryFileAnd.setFont(new Font("Tahoma", Font.BOLD, 16));
		txtBinaryFileAnd.setBorder(null);
		txtBinaryFileAnd.setHorizontalAlignment(SwingConstants.CENTER);
		springLayout.putConstraint(SpringLayout.WEST, txtBinaryFileAnd, 0, SpringLayout.WEST, getContentPane());
		springLayout.putConstraint(SpringLayout.EAST, txtBinaryFileAnd, 0, SpringLayout.EAST, getContentPane());
		txtBinaryFileAnd.setText("binary file and generate a listing ready to assemble.");
		springLayout.putConstraint(SpringLayout.NORTH, txtBinaryFileAnd, 6, SpringLayout.SOUTH, txtThePurposeOf);
		getContentPane().add(txtBinaryFileAnd);
		txtBinaryFileAnd.setColumns(10);
		
		txtFeelFreeTo = new JTextField();
		txtFeelFreeTo.setFont(new Font("Tahoma", Font.BOLD, 16));
		txtFeelFreeTo.setHorizontalAlignment(SwingConstants.CENTER);
		txtFeelFreeTo.setBorder(null);
		springLayout.putConstraint(SpringLayout.NORTH, txtFeelFreeTo, 20, SpringLayout.SOUTH, txtBinaryFileAnd);
		springLayout.putConstraint(SpringLayout.WEST, txtFeelFreeTo, 0, SpringLayout.WEST, getContentPane());
		springLayout.putConstraint(SpringLayout.EAST, txtFeelFreeTo, 0, SpringLayout.EAST, getContentPane());
		txtFeelFreeTo.setText("Feel free to send any comments, new ideas, or");
		getContentPane().add(txtFeelFreeTo);
		txtFeelFreeTo.setColumns(10);
		
		txtBugReportsOn = new JTextField();
		txtBugReportsOn.setFont(new Font("Tahoma", Font.BOLD, 16));
		springLayout.putConstraint(SpringLayout.WEST, txtBugReportsOn, 0, SpringLayout.WEST, getContentPane());
		springLayout.putConstraint(SpringLayout.EAST, txtBugReportsOn, 0, SpringLayout.EAST, getContentPane());
		txtBugReportsOn.setBorder(null);
		txtBugReportsOn.setHorizontalAlignment(SwingConstants.CENTER);
		txtBugReportsOn.setText("bug reports on SourceForge.");
		springLayout.putConstraint(SpringLayout.NORTH, txtBugReportsOn, 6, SpringLayout.SOUTH, txtFeelFreeTo);
		getContentPane().add(txtBugReportsOn);
		txtBugReportsOn.setColumns(10);
		
		separator = new JSeparator();
		springLayout.putConstraint(SpringLayout.WEST, txtTheDisassembler, 175, SpringLayout.WEST, separator);
		springLayout.putConstraint(SpringLayout.EAST, txtTheDisassembler, -175, SpringLayout.EAST, separator);
		springLayout.putConstraint(SpringLayout.WEST, frmtdTextFieldjDis6502, 200, SpringLayout.WEST, separator);
		springLayout.putConstraint(SpringLayout.EAST, frmtdTextFieldjDis6502, -200, SpringLayout.EAST, separator);
		springLayout.putConstraint(SpringLayout.WEST, separator, 20, SpringLayout.WEST, getContentPane());
		springLayout.putConstraint(SpringLayout.EAST, separator, -20, SpringLayout.EAST, getContentPane());
		separator.setBackground(Color.LIGHT_GRAY);
		springLayout.putConstraint(SpringLayout.NORTH, separator, 28, SpringLayout.SOUTH, txtBugReportsOn);
		getContentPane().add(separator);
		
		btnOk = new JButton("OK");
		btnOk.setBackground(Color.LIGHT_GRAY);
		btnOk.setFont(new Font("Tahoma", Font.BOLD, 20));
		springLayout.putConstraint(SpringLayout.NORTH, btnOk, -59, SpringLayout.SOUTH, getContentPane());
		springLayout.putConstraint(SpringLayout.WEST, btnOk, -84, SpringLayout.EAST, separator);
		btnOk.setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));
		btnOk.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				setVisible(false);
			}
		});
		springLayout.putConstraint(SpringLayout.SOUTH, btnOk, -10, SpringLayout.SOUTH, getContentPane());
		springLayout.putConstraint(SpringLayout.EAST, btnOk, 0, SpringLayout.EAST, separator);
		getContentPane().add(btnOk);
		
		lblNewLabel = new JLabel("");
		lblNewLabel.setHorizontalAlignment(SwingConstants.CENTER);
		springLayout.putConstraint(SpringLayout.NORTH, lblNewLabel, 20, SpringLayout.NORTH, getContentPane());
		springLayout.putConstraint(SpringLayout.WEST, lblNewLabel, 20, SpringLayout.WEST, getContentPane());
		springLayout.putConstraint(SpringLayout.SOUTH, lblNewLabel, 148, SpringLayout.NORTH, getContentPane());
		springLayout.putConstraint(SpringLayout.EAST, lblNewLabel, 148, SpringLayout.WEST, getContentPane());
		lblNewLabel.setIcon(new ImageIcon(CustomAboutDialog.class.getResource("/jDIS6502/resources/dis6502_96.png")));
		getContentPane().add(lblNewLabel);
		
		lblNewLabel_1 = new JLabel("");
		springLayout.putConstraint(SpringLayout.NORTH, lblNewLabel_1, 0, SpringLayout.NORTH, lblNewLabel);
		springLayout.putConstraint(SpringLayout.WEST, lblNewLabel_1, 436, SpringLayout.WEST, getContentPane());
		springLayout.putConstraint(SpringLayout.SOUTH, lblNewLabel_1, -6, SpringLayout.NORTH, txtHttpSourceforgenetprojectsdis);
		springLayout.putConstraint(SpringLayout.EAST, lblNewLabel_1, 0, SpringLayout.EAST, separator);
		lblNewLabel_1.setIcon(new ImageIcon(CustomAboutDialog.class.getResource("/jDIS6502/resources/alfred_128.png")));
		getContentPane().add(lblNewLabel_1);
		
		JTextField txtrJdisexe = new JTextField();
		txtrJdisexe.setBorder(null);
		springLayout.putConstraint(SpringLayout.WEST, txtrJdisexe, 10, SpringLayout.WEST, separator);
		springLayout.putConstraint(SpringLayout.EAST, txtrJdisexe, -10, SpringLayout.EAST, separator);
		txtrJdisexe.setHorizontalAlignment(SwingConstants.LEFT);
		txtrJdisexe.setFont(new Font("MS Gothic", Font.BOLD, 16));
		txtrJdisexe.setText("jDIS6502.exe    4.0  6502 Disassembler");
		springLayout.putConstraint(SpringLayout.NORTH, txtrJdisexe, 25, SpringLayout.SOUTH, separator);
		getContentPane().add(txtrJdisexe);
		
		txtDumpctldllAtari = new JTextField();
		txtDumpctldllAtari.setFont(new Font("MS Gothic", Font.BOLD, 16));
		txtDumpctldllAtari.setBorder(null);
		txtDumpctldllAtari.setHorizontalAlignment(SwingConstants.LEFT);
		springLayout.putConstraint(SpringLayout.NORTH, txtDumpctldllAtari, 6, SpringLayout.SOUTH, txtrJdisexe);
		springLayout.putConstraint(SpringLayout.WEST, txtDumpctldllAtari, 10, SpringLayout.WEST, separator);
		springLayout.putConstraint(SpringLayout.EAST, txtDumpctldllAtari, -10, SpringLayout.EAST, separator);
		txtDumpctldllAtari.setText("DUMPCTL.dll     4.0      Atari Segment Dump Control");
		getContentPane().add(txtDumpctldllAtari);
		txtDumpctldllAtari.setColumns(10);
		
		txtDisctldllAtari = new JTextField();
		txtDisctldllAtari.setBorder(null);
		springLayout.putConstraint(SpringLayout.WEST, txtDisctldllAtari, 10, SpringLayout.WEST, separator);
		springLayout.putConstraint(SpringLayout.EAST, txtDisctldllAtari, -10, SpringLayout.EAST, separator);
		txtDisctldllAtari.setFont(new Font("MS Gothic", Font.BOLD, 16));
		txtDisctldllAtari.setText("DISCTL.dll      4.0      Atari Disassmbly Control");
		springLayout.putConstraint(SpringLayout.NORTH, txtDisctldllAtari, 6, SpringLayout.SOUTH, txtDumpctldllAtari);
		getContentPane().add(txtDisctldllAtari);
		txtDisctldllAtari.setColumns(10);
		
		txtSpritctldllAtari = new JTextField();
		txtSpritctldllAtari.setFont(new Font("MS Gothic", Font.BOLD, 16));
		txtSpritctldllAtari.setBorder(null);
		springLayout.putConstraint(SpringLayout.WEST, txtSpritctldllAtari, 10, SpringLayout.WEST, separator);
		springLayout.putConstraint(SpringLayout.EAST, txtSpritctldllAtari, -10, SpringLayout.EAST, separator);
		txtSpritctldllAtari.setText("SPRITCTL.dll    4.0      Atari Sprite Display Control");
		springLayout.putConstraint(SpringLayout.NORTH, txtSpritctldllAtari, 6, SpringLayout.SOUTH, txtDisctldllAtari);
		getContentPane().add(txtSpritctldllAtari);
		txtSpritctldllAtari.setColumns(10);
		

		
		setModal(true);
		//setIconImage(Toolkit.getDefaultToolkit().getImage(CustomAboutDialog.class.getResource(/jDIS6502/resources/dis6502.png")));
	}



	/**
	 * Launch the application.
	 */
  /*	public static void main(String[] args) {
		try {
			AboutDialog dialog = new AboutDialog();
			dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
			dialog.setVisible(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	} */

	/**
	 * Create the dialog.
	 */
	public void AboutDialog() {
		
//		JButton btnAlfred = new JButton("test text");
//		btnAlfred.setHorizontalTextPosition(SwingConstants.CENTER);
//		btnAlfred.setVerticalAlignment(SwingConstants.TOP);
//		btnAlfred.setEnabled(true);
//		springLayout.putConstraint(SpringLayout.WEST, btnAlfred, -120, SpringLayout.EAST, getContentPane());
//		springLayout.putConstraint(SpringLayout.SOUTH, btnAlfred, 0, SpringLayout.SOUTH, frmtdTextFieldjDis6502);
//		btnAlfred.setBackground(Color.WHITE);
//		btnAlfred.setIcon(new ImageIcon(CustomAboutDialog.class.getResource("/jDIS6502/resources/alfred.bmp")));
//		//java.awt.Image image2 = new ImageIcon(this.getClass().getResource("/jDIS6502/resources/dis6502.png")).getImage();
//		//labelImage.setIcon(new ImageIcon(image2));
//		springLayout.putConstraint(SpringLayout.NORTH, btnAlfred, 10, SpringLayout.NORTH, getContentPane());
//		springLayout.putConstraint(SpringLayout.EAST, btnAlfred, 0, SpringLayout.EAST, separator);
//		getContentPane().add(btnAlfred);

//		setBounds(100, 100, 900, 600);
//		getContentPane().setLayout(new BorderLayout());
//		contentPanel.setLayout(new FlowLayout());
//		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
//		getContentPane().add(contentPanel, BorderLayout.CENTER);
//		{
//			JPanel buttonPane = new JPanel();
//			//buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
//			getContentPane().add(buttonPane, BorderLayout.SOUTH);
//			{
//				JButton okButton = new JButton("OK");
//				okButton.setActionCommand("OK");
//				buttonPane.add(okButton);
//				getRootPane().setDefaultButton(okButton);
//			}
//			{
//				JButton cancelButton = new JButton("Cancel");
//				cancelButton.setActionCommand("Cancel");
//				buttonPane.add(cancelButton);
//			}
//		}
		
		this.setVisible(true);
	}
}

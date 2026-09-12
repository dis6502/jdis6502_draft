package jDIS6502.common;

import jDIS6502.common.*;
import jDIS6502.views.*;

public class MainGlobals
	{
//		public static Application lpApplication;

//		public static main Main;
//		public static Main lpMain;
//
//		public static HWND hMainWnd = new HWND();
//		public static HWND hSegmentList = new HWND();
//		public static HWND hDumpWnd = new HWND();
//		public static HWND hDisWnd = new HWND();

//		public static FileSystemLogic lpFileSystemLogic;
//		public static Workspace lpWorkspace;

//		public static LabelLogic lpLabelLogic;
//		public static SegmentListLogic lpSegmentListLogic;
//		public static Dump dump;

//		public static Dis dis;
//		public static DisSelection lpDisSelection;

		public static FILE_PATH szBinPath = new FILE_PATH(); // Part of workspace. Used by Main/MainFile and Workspace Logic
		public static FILE_PATH szDskPath = new FILE_PATH(); // Part of workspace. Used by Main/MainFile and Workspace Logic

		public static String lpLabelUser;
		public static String lpLabelSystem;

		public static Title szXRefTitle = new Title(); // TODO: Used by Dis.cpp and Main.cpp
		/*
		** View variables.
		*/
		public static boolean bViewDisplayAsScreenCode; // dump uses internal character set (ANTIC)
		public static boolean bViewNoDisassembly; // no disassembly launched if byte type is changed
		public static boolean bViewDoubleHeight; // double the height of the display
		
		public static int ID_FILE_TYPE = 0;

		public final int UNKNOWN_FILE = 2000;
		public final int RAW_FILE = 2001;
		public final static int EXECUTABLE_FILE = 2002 ;
		public final int ROM_IMAGE_FILE = 2003;
		public final int CASSETTE_IMAGE_FILE = 2004;
		public final int DISK_IMAGE_EXECUTABLE_FILE = 2005;
		public final int DISK_IMAGE_BOOT_SECTORS = 2006;
		public final int DISK_IMAGE_SECTORS = 2007;
		public final int WORKSPACE_FILE = 2008;
		public final int EQUATES_FILE = 2009;
		public final int PROFILE_FILE = 2010;
		public final int DISASSEMBLY_FILE = 2011;

		
		
		public final int ID_SEGMENT = 1000;
		public final int ID_DUMP = 1001;
		public final int ID_DIS = 1002;
		public final int ID_XREF = 1003;
		public final int ID_LOG = 1004;
		
		public final int NO_Dump = 0xFFFF;
		
	}


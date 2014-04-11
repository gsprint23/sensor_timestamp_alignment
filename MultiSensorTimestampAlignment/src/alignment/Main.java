package alignment;

/** 
 * Copyright (C) 2014 Gina L. Sprint
 * Email: Gina Sprint <gsprint@eecs.wsu.edu>
 * 
 * This file is part of MultiSensorTimestampAlignment.
 * 
 * MultiSensorTimestampAlignment is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *  
 * MultiSensorTimestampAlignment is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 * GNU General Public License for more details.
 *  
 * You should have received a copy of the GNU General Public License
 * along with MultiSensorTimestampAlignment.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Notes: ShimDv2 is a file format defined by ShimmerSensing, see <http://www.shimmersensing.com/>
 * This code is based on the idea specified in SDLog_for_Shimmer3_Firmware_User_Manual_rev0.5a.pdf
 * available at <http://www.shimmersensing.com/>
 * 
 * @author Gina Sprint
 * @version 1.0 2/1/14
 */
public class Main 
{
	/**
	 * MultiSensorTimestampAlignment --- Main.java
	 * 
	 * Dependencies - A library with a linear regression algorithm.
	 * The current library of choice is Apache Math Commons.
	 * Go to Project -> Properties -> Build Path and click "Add External JARs" to point it to
	 * the commons jar.
	 * 
	 * There are three files involved in this program
	 * 1. binaryFile - the input binary file. This is the file pulled straight off of the shimmer. It is
	 * extensionless. For example "000".
	 * 
	 * 2. timestampsFile - the output file containing ONLY THE CALIBRATED TIMESTAMPS. Each timestamp
	 * will be on its own line. Open this file in notepad or similar, press CTRL+A to copy its contents,
	 * open your target ShimDv2 file, select row 5 of the timestamp column and press CTRL+V to paste over
	 * the old (wrong) timestamps with the new ones. This manual overwrite process can be bypassed by 
	 * specifying an overWriteFile (see (3.))
	 * 
	 * 3. overWriteFile - OPTIONAL - this is the calibrated tab delimited *.dat file exported by ShimmerLog
	 * or comma delimited *.csv file containing the WRONG timestamps you wish to overwrite.
	 * This program will do the manual overwrite process specified in (2.) for you. The output
	 * will be a csv, even if the input was a dat.
	 * 
	 * How to use this program - For now you can either specify the 2 (or 3) files on the command line
	 * as arguments in the order you see above or you can just paste your filenames below. If you want to
	 * use these make sure the flag "useCmd" is set to "false". LOOK AT THE FLAG NOW and see 
	 * what it set to. If you are running this in eclipse, just
	 * paste them in this file (Main.java) at the appropriate variable, it is much faster than going to
	 * Run -> Run configurations -> Arguments tab and specifying them there.
	 * 
	 * Notes: 
	 * If you use relative path names (No "C:\..." prefix) all the files will be searched
	 * for/written to the package directory of this code.
	 * 
	 * This program does create a temp file in the current directory. It is deleted on exit.
	 * 
	 * If something isn't working, check the stack trace. It will probably tell you exactly why
	 * the program crashed. It is most likely will be a "FileNotFoundException" because the paths
	 * you entered are wrong. Check those first. If it is an "IOException" or something similar,
	 * make sure no other programs (like Notepad, Excel, etc.) have the files open you are trying
	 * to get the program to work with.
	 * 
	 * Let me know if you have any questions or if you found a bug (which I'm sure exist)
	 * 
	 * USE AT YOUR OWN RISK: This code comes with no guarantees of file safety (back up your involved
	 * files first) or correctness of computed timestamps.
	 * 
	 *
	 * @param 
	 * @return 
	 */
	public static void main(String [] args)
	{
		String binaryFile = 
			"C:\\Users\\...\\000";
		String timestampsFile = 
			"C:\\Users\\..._Timestamps.csv";
		String overWriteFile = 
			"C:\\Users\\....dat";
		
		boolean useCmd = false;
		
		if(useCmd)
		{
			if(args.length >= 2)
			{
				binaryFile = args[0];
				timestampsFile = args[1];
	
					if(args.length == 3)
					{
						overWriteFile = args[2];
					}
			}
			else
			{
				System.out.println("Valid command line args were not found");
				System.out.println("Usage: binaryFile outputFile overWriteFile(optional)");
				System.out.println();
				System.out.println("Attempting to use default binary filename: " + binaryFile);
				System.out.println("Attempting to use default timestamps filename: " + timestampsFile);
				System.out.println();
			}
		}

		
		System.out.println("Parsing Shimmer binary: " + binaryFile);
		System.out.println("Writing timestamp output to: " + timestampsFile);
		
		if(overWriteFile.compareTo("") != 0) // not yet implemented
		{
			System.out.println("Inserting timestamps into file: " + overWriteFile);
		}
		System.out.println();
		
		BinaryToCSV parser = new BinaryToCSV(binaryFile, timestampsFile, overWriteFile);
		parser.parse();
		
	}
}

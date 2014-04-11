package alignment;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Scanner;

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

public class ShimDv2OverWriter
{
	static final int HEADER_ROWS = 4;
	String tempFileName = "temp.csv";
	String [] parts, parts1, parts2, parts3, parts4;
	String line;
	File outFile;
	Scanner fin;
	PrintStream writer;
	
	String [] header = new String[HEADER_ROWS];
	ArrayList<Double> calTimestamps;
	int timeIndex = 0;
	
	/**
	 * 
	 *
	 * @param 
	 * @return 
	 */
	public ShimDv2OverWriter(String out, ArrayList<Double> calTS) 
	{
		outFile = new File(out);
		calTimestamps = calTS;
		try
		{
			fin = new Scanner(outFile);
		}
		catch(FileNotFoundException e)
		{
			e.printStackTrace();
		}
	}
	
	/**
	 * 
	 *
	 * @param 
	 * @return 
	 */
	public void parse() 
	{
		//rename "temp.csv"
		File tempFile = new File(tempFileName);
		Path from = tempFile.toPath(); //convert from File to Path
		Path to = outFile.toPath(); //convert from String to Path
		parts = to.toString().split("\\.");
		String ext = parts[1];
		String delimiter = "\t";
	
		// should actually check the file delimiter, not depend on the extension
		if(ext.compareTo("csv") == 0)
			delimiter = ",";
		
		// if input file is a dat then output will be csv
		String csvFileName = parts[0].concat(".csv"); 
		
		// read first four lines first
		readHeader(fin);
		fin.close();
		
		try 
		{
			fin = new Scanner(outFile);
		} 
		catch (FileNotFoundException e) 
		{
			e.printStackTrace();
		}
		try 
		{
			writer = new PrintStream(tempFile);
		} 
		catch (FileNotFoundException e) 
		{
			e.printStackTrace();
		}
		
		int i = 0;
		while(fin.hasNextLine())
		{
			line = fin.nextLine();
			parts = line.split(delimiter);
			// interested in parts[timeIndex]
			if(i > 3)
			{
				parts[timeIndex] = calTimestamps.get(i - HEADER_ROWS).toString();
			}
			else if(i == 2) // ShimmerLog outputs it as "RAW" so just changing for fun
			{
				parts[timeIndex] = "CAL";
			}
			else if(i == 3) // ShimmerLog outputs it as no unit
			{
				parts[timeIndex] = "mSecs";
			}

			for(int j = 0; j < parts.length - 1; j++)
			{
				writer.print(parts[j] + ",");
			}
			writer.println(parts[parts.length - 1]);
			i++;
		}	
		fin.close();
		writer.close();
	
		try 
		{
			Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING);
			outFile.renameTo(new File(csvFileName));
			tempFile.deleteOnExit();
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
	}
	
	/**
	 * 
	 *
	 * @param 
	 * @return 
	 */
	public void buildHeaderParts(Scanner fin)
	{
		String line1 = fin.nextLine();
		parts1 = line1.split(",");
		
		String line2 = fin.nextLine();
		parts2 = line2.split(",");
		
		String line3 = fin.nextLine();
		parts3 = line3.split(",");
		
		String line4 = fin.nextLine();
		parts4 = line4.split(",");
	}
	
	/**
	 * 
	 *
	 * @param 
	 * @return 
	 */
	public void readHeader(Scanner fin)
	{
		buildHeaderParts(fin);
		
		for(int j = 0; j < parts1.length; j++)
		{
			if(parts2[j].contains("Timestamp"))
			{
				// hack to copy the header contents for re-write
				header[0] = parts1[j];
				header[1] = parts2[j];
				header[2] = parts3[j];
				header[3] = parts4[j];
				timeIndex = j;
				break;
			}
			else
				System.err.println("Cannot find the Timestamp signal label");
		}
	}

}

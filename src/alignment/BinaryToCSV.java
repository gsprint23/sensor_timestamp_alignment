package alignment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.math3.stat.regression.SimpleRegression;

import alignment.ShimDv2OverWriter;



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


public class BinaryToCSV
{
	static final int HEADER_SIZE = 178;
	static final int TWO_EXP_16 = 65536;
	static final int TWO_EXP_15 = 32768;
	static final int MILLI = 1000;
	static final int MILLIS_IN_MIN = 60000;
	
    FileInputStream inputStream;
	File outFile;
	String csvOverwriteFile;
    PrintStream writer;
    
    int counter = 0;
    SortedMap<Double, Double> offsets = new TreeMap<Double, Double>();
    ArrayList<Double> timestampsOrig = new ArrayList<Double>();
    ArrayList<Double> timestampsCont = new ArrayList<Double>();
    ArrayList<Double> timestampsAligned = new ArrayList<Double>();
    ArrayList<Double> timestampsCal = new ArrayList<Double>();
    ArrayList<Double> intervalSizes = new ArrayList<Double>();
    double prev = 0;
    double prev16bit = 0;
    double slope = 0;
    double intercept = 0;
    
	private double lastReceivedTS = 0;
	protected double currentTSCycle = 0;
	protected double lastReceivedCalibratedTS = -1; 
	protected boolean firstCalTime = true;
	protected double calTimeStart;	 
	
	int b0 = 0;
	int b1 = 0;
	int b2 = 0;
	int b3 = 0;
	int b4 = 0;
	int b5 = 0;
	int b6 = 0;
	int b7 = 0;
	
	int LN = 0;
	int WR = 0;
	int gyro = 0;
	int mag = 0;
	
	int sync = 0;
	int master = 0;
	
	double sampleRate = 0;
	double initialTS16 = 0;
	double initialTS32 = 0;
	
	int Nc2 = 0;
	int Nc1 = 0;
	int Bp = 0;
	int Bs = 0;
	int N = 0;
	
	int offsetSign = 0;
	int offsetMagB0 = 0;
	int offsetMagB1 = 0;
	int offsetMagB2 = 0;
	int offsetMagB3 = 0;
	

	/**
	 * 
	 *
	 * @param 
	 * @return 
	 */
	public BinaryToCSV(String in, String out, String csv) 
	{
        try 
        {
			inputStream = new FileInputStream(in);
			outFile = new File(out);
			csvOverwriteFile = csv;
			writer = new PrintStream(outFile);
		} 
        catch (FileNotFoundException e) 
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
	public void byteToBits(byte b, int line) 
	{
    	b0 = (b >> 0) & 1;
    	b1 = (b >> 1) & 1;
    	b2 = (b >> 2) & 1;
    	b3 = (b >> 3) & 1;
    	b4 = (b >> 4) & 1;
    	b5 = (b >> 5) & 1;
    	b6 = (b >> 6) & 1;
    	b7 = (b >> 7) & 1;
    	
    	System.out.println(line + ": " + b7 + " " 
    			+ b6 + " " + b5 + " " + b4 + "   " + b3 + 
    			" " + b2 + " " + b1 + " " + b0);
	}
	
	/**
	 * 
	 *
	 * @param 
	 * @return 
	 */
	public void addTS(double ts) 
	{		
		timestampsOrig.add(ts);
		double temp = prev16bit;
		prev16bit = ts;
		if(ts < temp)
		{
			ts += TWO_EXP_16 - temp;
		}
		else
		{
			ts -= temp;
		}
		ts += prev;

		timestampsCont.add(ts);
		prev = ts;
	}
	
	/**
	 * 
	 *
	 * @param 
	 * @return 
	 */
	public void computeBlockSize() 
	{
        Nc2 = 3*LN + 3*gyro + 3*mag + 3*WR;
        System.out.println("Nc2: " + Nc2);
        Bs = Nc2 * 2 + Nc1;
        System.out.println("Bs: " + Bs);
        N = (int) Math.floor((512 - 6 * sync) / (Bs + 2));
        System.out.println("N: " + N);
        Bp = N * (Bs + 2) + 5 * sync;
        System.out.println("Block size: " + Bp);
	}
	
	/**
	 * 
	 *
	 * @param 
	 * @return 
	 */
	public int readHeader() 
	{
        byte[] buffer = new byte[HEADER_SIZE]; //178
        int nRead = 0;
	    
	    try 
	    {
			if((nRead = inputStream.read(buffer)) == -1)
				System.err.println("Header read failed");
		} 
	    catch (IOException e) 
	    {
			e.printStackTrace();
		}
	    counter += nRead;
	    
	    byteToBits(buffer[0], 0);
		LN = b7;
		System.out.println("Low Noise: " + LN);
		gyro = b6;
		System.out.println("Gyroscope: " + gyro);
		mag = b5;
		System.out.println("Magnetometer: " + mag);
	    
		byteToBits(buffer[1], 1);
		WR = b4;
		System.out.println("Wide Range: " + WR);
		
		byteToBits(buffer[10], 10);
		sync = b2;
		System.out.println("Sync: " + sync);
		master = b1;
		System.out.println("Master: " + master);
		
		NumberFormat nf = NumberFormat.getNumberInstance(Locale.ENGLISH);
		DecimalFormat df = (DecimalFormat)nf;
		df.applyPattern("#.#");
		sampleRate = Double.parseDouble(df.format(TWO_EXP_15 / (double) 
				((int) (buffer[21] & 0xFF) + ((int) (buffer[20] & 0xFF) << 8))));
		System.out.println("SampleRate: " + sampleRate);
		

		byte [] tempBytes = new byte[4];
		tempBytes[3] = buffer[174];
		tempBytes[2] = buffer[175];
		tempBytes[1] = buffer[176];
		tempBytes[0] = buffer[177];
		
		ByteBuffer bb = ByteBuffer.wrap(tempBytes);
		initialTS32 = bb.getInt();
		System.out.println("Initial Timestamp: " + initialTS32);
		
		for(int i = 0; i < buffer.length; i++)
		{
			//byteToBits(buffer[i], i);
		}
		return nRead;
	}
	
	/**
	 * 
	 *
	 * @param 
	 * @return 
	 */
	public int readBlocks() 
	{
        byte[] buffer = new byte[Bp];
		byte [] tempBytes = new byte[4];
        int total = 0;
        int nRead = 0;
        
        try 
        {
			while((nRead = inputStream.read(buffer)) != -1) 
			{	
				for(int i = 0; i < nRead; i += (Bs + 2))
				{
					tempBytes = new byte[4];
					tempBytes[3] = buffer[i];
					tempBytes[2] = buffer[i+1];
					ByteBuffer bb = ByteBuffer.wrap(tempBytes);
					int tsI = bb.getInt();
					//System.out.println("Timestamp: " + tsI);
					double calTS = calibrateTimeStamp((double) tsI);
					timestampsCal.add(calTS);
					//System.out.println("CAL Timestamp: " + calTS);
				}
			    total += nRead;
			}
		} 
        catch (IOException e) 
        {
			e.printStackTrace();
		}
        return total;
	}
	
	/**
	 * 
	 *
	 * @param 
	 * @return 
	 */
	public int readSlaveBlocks() 
	{
        byte[] buffer = new byte[Bp];
		byte [] tempBytes = new byte[4];
        int total = 0;
        int nRead = 0;
        
        int sign = 0;
        double magnitude = 0;
        counter = 0;
        int samplesBetween = 0;
        boolean firstTS = true;
        
        try 
        {
			while((nRead = inputStream.read(buffer)) != -1) 
			{	
				for(int i = 0; i < nRead; i++)
				{
					//byteToBits(buffer[i], i+HEADER_SIZE);
				}
				
				
				sign = buffer[0];
				tempBytes = new byte[4];
				//byteToBits(buffer[1], 1);
				tempBytes[3] = buffer[1];
				tempBytes[2] = buffer[2];
				tempBytes[1] = buffer[3];
				tempBytes[0] = buffer[4];
				
				// unsigned int 0xFFFFFFFF is equal to -1
				ByteBuffer bb = ByteBuffer.wrap(tempBytes);
				magnitude = (double) bb.getInt();
				
				tempBytes = new byte[4];
				tempBytes[3] = buffer[5];
				tempBytes[2] = buffer[6];
				bb = ByteBuffer.wrap(tempBytes);
				double tsI = bb.getInt();
				
				if(firstTS)
				{
					initialTS16 = tsI;
					firstTS = false;
					System.out.println("Intial Timestamp 16b: " + initialTS16);
				}
				addTS(tsI); // end of the list
				
				if(magnitude != -1.0)
				{
					//System.out.println("Timestamp: " + tsI);
					// offset = (1 - 2*offsetSign) * offsetMagnitude
					//System.out.println("Sign: " + sign + " Magnitude: " + magnitude);
					double offset = (1 - 2 * sign) * magnitude;
					offsets.put(prev, offset);

					intervalSizes.add((double)samplesBetween);
					samplesBetween = 0;
				}
			
				
				boolean first = true;
				for(int i = 5; i < nRead; i += (Bs + 2))
				{
					samplesBetween++;
					if(first)
					{
						first = false;
						continue;
					}
					tempBytes = new byte[4];
					tempBytes[3] = buffer[i];
					tempBytes[2] = buffer[i+1];
					bb = ByteBuffer.wrap(tempBytes);
					tsI = bb.getInt();
					addTS(tsI);
					
					//System.out.println("Timestamp: " + tsI);
				}
			    total += nRead;
			}
		} 
        catch (IOException e) 
        {
			e.printStackTrace();
		}

		intervalSizes.add((double)samplesBetween);
		
		if(master == 0)
		{
			printOffsetMap();
			computeLinearRegression();
			alignTimestamps();
			calibrateTimestamps();
		}
		else
		{
			calibrateMasterTimestamps();
		}

        return total;
	}
	
	/**
	 * Adapted from Shimmer Android Driver "ShimmerObject"
	 *
	 * @param 
	 * @return 
	 */
	protected double calibrateTimeStamp(double ts)
	{
		//first convert to continuous time stamp
		double calibratedTS = 0;
		if (lastReceivedTS > (ts + (TWO_EXP_16 * currentTSCycle)))
		{ 
			currentTSCycle = currentTSCycle + 1;
		}

		lastReceivedTS = (ts + (TWO_EXP_16 * currentTSCycle));
		calibratedTS = lastReceivedTS / TWO_EXP_15 * MILLI;   // to convert into mS
		
		if (firstCalTime)
		{
			firstCalTime = false;
			calTimeStart = calibratedTS;
		}

		lastReceivedCalibratedTS = calibratedTS;
		return calibratedTS;
	}
	
	/**
	 * 
	 *
	 * @param 
	 * @return 
	 */
	public void calibrateTimestamps() 
	{
		double ts = 0;
		double newTS = 0;
		for(int i = 0; i < timestampsAligned.size(); i++)
		{
			ts = timestampsAligned.get(i);
			newTS = ts / TWO_EXP_15 * MILLI;   // to convert into mS
			timestampsCal.add(newTS);
		}
		
		double ms = timestampsCal.get(timestampsCal.size() - 1 ) - timestampsCal.get(0);
		System.out.println();
		System.out.println("Total time: " + ms + " milliseconds");
		System.out.println("Total time: " + ms / MILLI + " seconds");
		System.out.println("Total time: " + ms / MILLIS_IN_MIN + " minutes");
	}
	
	/**
	 * 
	 *
	 * @param 
	 * @return 
	 */
	public void calibrateMasterTimestamps() 
	{
		double ts = 0;
		double newTS = 0;
		for(int i = 0; i < timestampsCont.size(); i++)
		{
			ts = timestampsCont.get(i);
			newTS = initialTS32 + (ts - initialTS16);
			newTS = newTS / TWO_EXP_15 * MILLI;   // to convert into mS
			timestampsCal.add(newTS);
		}
		
		double ms = timestampsCal.get(timestampsCal.size() - 1 ) - timestampsCal.get(0);
		System.out.println("Total time: " + ms + " milliseconds");
		System.out.println("Total time: " + ms / MILLI + " seconds");
		System.out.println("Total time: " + ms / MILLIS_IN_MIN + " minutes");
	}
	
	/**
	 * 
	 *
	 * @param 
	 * @return 
	 */
	public void alignTimestamps() 
	{
		double offset = 0;
		double ts = 0;
		double newTS = 0;
		for(int i = 0; i < timestampsCont.size(); i++)
		{
			ts = timestampsCont.get(i);
			offset = slope * ts + intercept;
			newTS = initialTS32 + (ts - initialTS16) - offset;
			timestampsAligned.add(newTS);
		}

	}
	
	/**
	 * 
	 *
	 * @param 
	 * @return 
	 */
	public void computeLinearRegression() 
	{
		double [][] data = new double[offsets.size()][2];
		
		int i = 0;
		for(Double d : offsets.keySet())
		{
			data[i][0] = d;
			data[i++][1] = offsets.get(d);
		}
		
		
		SimpleRegression regress = new SimpleRegression(true);
		regress.addData(data);
		
		// dummy data from wikipedia (linear regression)
		// used to check the regression library
		/*regress.addData(1.47,52.21);
		regress.addData(1.50,53.12);	
		regress.addData(1.52,54.48);	
		regress.addData(1.55,55.84);
		regress.addData(1.57,57.20);
		regress.addData(1.60,58.57);
		regress.addData(1.63,59.93);
		regress.addData(1.65,61.29);
		regress.addData(1.68,63.11);
		regress.addData(1.70,64.47);
		regress.addData(1.73,66.28);
		regress.addData(1.75,68.10);
		regress.addData(1.78,69.92);
		regress.addData(1.80,72.19);
		regress.addData(1.83,74.46);*/

		slope = regress.getSlope();
		intercept = regress.getIntercept();
		
		System.out.println("Regression: y = " + slope + "x + " + intercept);
	}
	
	/**
	 * 
	 *
	 * @param 
	 * @return 
	 */
	public void printOffsetMap() 
	{
		double totalSamples = 0;
		int i = 0;
		System.out.println("\nOffset map and intervals: ");
		totalSamples += intervalSizes.get(i);
		System.out.println("\t\t" + intervalSizes.get(i++));
		for(Double d : offsets.keySet())
		{
			System.out.println("\t" + d + ", " + offsets.get(d));
			totalSamples += intervalSizes.get(i);
			System.out.println("\t\t" + intervalSizes.get(i++));
		}
		System.out.println("Total samples: " + totalSamples);
		System.out.println();
	}
	
	/**
	 * 
	 *
	 * @param 
	 * @return 
	 */
	public void printAllTimestamps() 
	{
		writer.println("Timestamps [# orig cont aligned cal]: ");
		for(int i = 0; i < timestampsCal.size(); i++)
		{
			writer.println(i + ": " + timestampsOrig.get(i)
					+ "\t\t" + timestampsCont.get(i)
					+ "\t\t" + timestampsAligned.get(i) + "\t\t" + 
					timestampsCal.get(i));
		}
	}
	
	/**
	 * 
	 *
	 * @param 
	 * @return 
	 */
	public void printTimestamps() 
	{
		//writer.println("Timestamps [# orig cont cal]: ");
		for(int i = 0; i < timestampsCal.size(); i++)
		{
			writer.println(timestampsCal.get(i));
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
        int total = 0;
		total += readHeader();
        computeBlockSize();

		if(sync == 1) // works for both master and slave
		{
			total += readSlaveBlocks();
			// only do one or the other of the printTimestamps(they write to same file)
			//printAllTimestamps(); 
			printTimestamps();
			ShimDv2OverWriter shim = new ShimDv2OverWriter(csvOverwriteFile, timestampsCal);
			shim.parse();
		}
		else if(sync == 0)
		{
			total += readBlocks();
			printTimestamps();
			//printAllTimestamps(); 
			// only do one or the other of the printTimestamps(they write to same file)
		}

        try 
        {
			inputStream.close();
			writer.close();
		} 
        catch (IOException e) 
        {
			e.printStackTrace();
		}		
        System.out.println("Total bytes read: " + total + " bytes");
	}

}

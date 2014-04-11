sensor_timestamp_alignment
==========================

MultiSensorTimestampAlignment is a code base used to align the timestamps from multiple Shimmer sensors  &lt;http://www.shimmersensing.com/> running under the master/slave SDLog configuration.

MultiSensorTimestampAlignment 1.0.0
------------
https://github.com/gsprint23

MultiSensorTimestampAlignment is Copyright (C) 2014 Gina L. Sprint
Email: Gina Sprint <gsprint@eecs.wsu.edu>

MultiSensorTimestampAlignment is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

Dependencies - A library with a linear regression algorithm.
The current library of choice is Apache Math Commons.
Go to Project -> Properties -> Build Path and click "Add External JARs" to point it to
the commons jar.

There are three files involved in this program
1. binaryFile - the input binary file. This is the file pulled straight off of the shimmer. It is
extensionless. For example "000".

2. timestampsFile - the output file containing ONLY THE CALIBRATED TIMESTAMPS. Each timestamp
will be on its own line. Open this file in notepad or similar, press CTRL+A to copy its contents,
open your target ShimDv2 file, select row 5 of the timestamp column and press CTRL+V to paste over
the old (wrong) timestamps with the new ones. This manual overwrite process can be bypassed by 
specifying an overWriteFile (see (3.))

3. overWriteFile - OPTIONAL - this is the calibrated tab delimited *.dat file exported by ShimmerLog
or comma delimited *.csv file containing the WRONG timestamps you wish to overwrite.
This program will do the manual overwrite process specified in (2.) for you. The output
will be a csv, even if the input was a dat.

How to use this program - For now you can either specify the 2 (or 3) files on the command line
as arguments in the order you see above or you can just paste your filenames below. If you want to
use these make sure the flag "useCmd" is set to "false". LOOK AT THE FLAG NOW and see 
what it set to. If you are running this in eclipse, just
paste them in this file (Main.java) at the appropriate variable, it is much faster than going to
Run -> Run configurations -> Arguments tab and specifying them there.

Notes: 
If you use relative path names (No "C:\..." prefix) all the files will be searched
for/written to the package directory of this code.

This program does create a temp file in the current directory. It is deleted on exit.

If something isn't working, check the stack trace. It will tell you exactly why
the program crashed. It most likely will be a "FileNotFoundException" because the paths
you entered are wrong. Check those first. If it is an "IOException" or something similar,
make sure no other programs (like Notepad, Excel, etc.) have the files open you are trying
to get the program to work with.

Let me know if you have any questions or if you found a bug (which probably exist)

USE AT YOUR OWN RISK: This code comes with no guarantees of file safety (back up your involved
files first) or correctness of computed timestamps.

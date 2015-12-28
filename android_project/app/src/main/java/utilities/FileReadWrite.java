package utilities;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;

import android.os.Environment;
import android.util.Log;

/**\brief 		Read or write a file
 * 
 * \details		Offers methods for reading or writing datas in a file on the
 * 				external storage. 
 * 
 * \warning		Always call \ref CreateFile method before writing a file.
 * \warning		Don't forget to close the file at the end.
 * 
 * \author		Emilie Gsponer
 * \version 	1.0 
 * \date 		28.09.2015
 */

public class FileReadWrite 
{
	//========================================================================
	// MEMBER FIELDS													  	
	//========================================================================

	// PRIVATE MEMBERS

	private File file;								///< File object
	private OutputStream fo;						///< File writer
	private InputStream fi;							///< File reader
	private BufferedReader reader;					///< File reader buffer

	public static final String FILE_NAME = "soundDistance.txt";
	
	//========================================================================
	// CLASS FUNCTIONS													 
	//========================================================================
    
	// PUBLIC FUNCTIONS														
		
	/**
	 * \brief		Class constructor
	 * 
	 * \details		Instantiate class objects
	 */
	public FileReadWrite()
	{
		file = null;				
		fo = null;		
		fi = null;
		reader = null;
	}
	
	/**
	 * \brief		Write a string in the file
	 * \param[in] 	data String of datas
	 */
	public void WriteDatas(String data)
	{ 
    	if(fo != null)
    	{
    	     try {
				fo.write((data).getBytes());
			} catch (IOException e) {
				Log.e("File_Manager","Write failed");
			}
    	}
	}
	
	/**
	 * \brief		Read the file line by line
	 * 
	 * \details		The file can be readed until this method return null. 
	 * 				That mean the end of file.
	 * 
	 * \return		A line of the file
	 */
	public String ReadDatas()
	{ 
    	if(fi != null)
    	{
    	     try {
    	    	 return reader.readLine();				

			} catch (IOException e) {
				Log.e("File_Manager","Read failed");
			}
    	}
    	return null;
	}
	
	/**
	 * \brief		Close the file
	 */
	public void CloseFile()
	{ 
    	if(fo != null)
    	{
    	    try 
    	    {
    	    	 fo.close();	
			} 
    	    catch (IOException e) 
			{
    	    	Log.e("File_Manager","Close failed");
			}
    	} 
    	if(fi != null)
    	{
    	    try 
    	    {
    	    	 fi.close();	
			} 
    	    catch (IOException e) 
			{
    	    	Log.e("File_Manager","Close failed");
			}
    	}
	}
	
	/**
	 * \brief		Delete the file from the external storage.
	 */
	public void DeleteFile()
	{ 
    	if(file.exists())
    	{
    	    if(!file.delete())
                Log.e("File Manager", file + " not deleted");
    	} 
	}
	
	/**
	 * \brief		Create a file in the external storage directory
	 * 
	 * \details		If the file already exist, it's just open. The data are not erase.
	 * 
	 * \param[in] 	fileName name of the file to write with the extension
	 */
	public void CreateFile(String fileName)
	{ 
		file = new File(Environment.getExternalStorageDirectory() + File.separator + fileName);

		if(!file.exists()) {
			try {
				if (file.createNewFile())
					Log.d("File_Manager", "file created: " + file);
				else
					Log.e("File_Manager", "File create failed");
			} catch (IOException e) {
				Log.e("File_Manager", "File create failed");
			}
		}

    	if(file.exists())
    	{
			try 
			{
				fo = new FileOutputStream(file,true);
				fi = new FileInputStream(file);

			    reader = new BufferedReader(new InputStreamReader(fi));
			} 
			catch (FileNotFoundException e) 
			{
				Log.e("File_Manager","Output stream failed");
			}  
    	}
	}

	/**
	 * \brief		Get the file object
	 * \return		File object
	 */
	public File getFile()
	{ 
		return file;
	}

	/**
	 * \brief		Remove the line at the specified index
	 */
	public void deleteALineFromFile(int lineIndex) {

		try {

			if (!file.isFile()) {
				System.out.println("Parameter is not an existing file");
			}

			//Construct the new file that will later be renamed to the original filename.
			File tempFile = new File(file.getAbsolutePath() + ".tmp");

			BufferedReader br = new BufferedReader(new FileReader(file));
			PrintWriter pw = new PrintWriter(new FileWriter(tempFile));

			String line;
			int lineNb = 0;

			while ((line = br.readLine()) != null) {
				if(lineNb != lineIndex) {
					pw.println(line);
					pw.flush();
				}
				lineNb++;
			}

			pw.close();
			br.close();

			//Delete the original file
			if (!file.delete()) {
				System.out.println("Could not delete file");
			}

			//Rename the new file to the filename the original file had.
			if (!tempFile.renameTo(file))
				System.out.println("Could not rename file");

		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * \brief		Replace a line at the given index, with the given text
	 */
    public void replaceALineFromFile(int lineIndex, String newLine) {

        try {

            if (!file.isFile()) {
                System.out.println("Parameter is not an existing file");
            }

            //Construct the new file that will later be renamed to the original filename.
            File tempFile = new File(file.getAbsolutePath() + ".tmp");

            BufferedReader br = new BufferedReader(new FileReader(file));
            PrintWriter pw = new PrintWriter(new FileWriter(tempFile));

            String line;
			int lineNb = 0;

            while ((line = br.readLine()) != null) {
				if(lineNb == lineIndex)
					pw.println(newLine);
				else
					pw.println(line);
				pw.flush();
				lineNb++;
            }

            pw.close();
            br.close();

            //Delete the original file
            if (!file.delete()) {
                System.out.println("Could not delete file");
            }

            //Rename the new file to the filename the original file had.
            if (!tempFile.renameTo(file))
                System.out.println("Could not rename file");

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}

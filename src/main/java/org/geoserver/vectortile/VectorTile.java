package org.geoserver.vectortile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;

import com.google.common.collect.Lists;

public class VectorTile {
	 /**
     * package file extension
     */
    public static final String EXTENSION = "pbf";

    /**
     * format mime type
     */
    public static final String MIME_TYPE = "application/x-vtile";

    /**
     * names/aliases for the format
     */
    public static final Collection<String> NAMES = Lists.newArrayList("vectortile", "vtile", "pbf");
    
    public File file;
    public VectorTile() throws IOException{
    	file = File.createTempFile("vectortile", "pbf");
    }
	public File getFile() {
		return file;
	}
	public void setFile(File file) {
		this.file = file;
	}
	public void add(byte[] encoded)  {
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(file);
			fos.write(encoded);	
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally{
			try {
				fos.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}
    

}

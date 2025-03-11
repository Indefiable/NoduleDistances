package noduledistances.imagej;

import org.scijava.command.Command;


import ij.gui.GenericDialog;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;



public class Menu implements Command {
	
	
	
	protected static final int FOLDER = 1;
	protected static final int IMAGE = 2;
	protected static final int MODEL = 3;
	protected static final int OTHERFILETYPE = 4;
	
	protected File rootFile;
	protected File tifFile;
    protected File saveFile;
    protected File modelFile;
    protected String redAttribute;
    protected String greenAttribute;
    
    // number of paths to compute between each pair of nodules
    protected int numIters = 1;
    
    
    /**
     * method for displaying the UI to let the user select their input files.
     */
	private void display() {
		
        GenericDialog gd = new GenericDialog("Nodule Distances Plugin");

        gd.addButton("Select Roots Image or Folder", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                selectFiles(1);
            }
        });
        
        gd.addButton("Select Tif Image or Folder", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                selectFiles(2);
            }
        });
        
        gd.addButton("Select model File", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                selectFiles(3);
            }
        });
        
        gd.addButton("Select save File", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                selectFiles(4);
            }
        });
        
        gd.addNumericField("number of paths to commpute between nodules:", 5, 0);
        gd.addStringField("Red Attribute:", "", 10);
        gd.addStringField("Green Attribute:", "", 10);
        
        gd.showDialog();
        
        
        
        if (gd.wasOKed()) {
           
        	int numIters = (int)gd.getNextNumber();
        	if(numIters > 0) {
        		this.numIters = numIters;
        	}
        	this.redAttribute = gd.getNextString();
        	this.greenAttribute = gd.getNextString();
        	
            if(rootFile == null || tifFile == null || saveFile == null || modelFile == null) {
	            System.out.println("Error, you must fill in all of the blanks to generate data. Please try again.");
	            display();
            }
            
            int type = getFileType(rootFile);
            if(type == MODEL || type == OTHERFILETYPE) {
            	System.out.println("RootFile Error, you must select a folder or image file for the segmentation file.");
	            display();
            }
            
            type = getFileType(tifFile);
            if(type == MODEL || type == OTHERFILETYPE) {
            	System.out.println("tifFile Error, you must select a folder or image file for the segmentation file.");
	            display();
            }
            
            type = getFileType(saveFile);
            if(type != FOLDER) {
            	System.out.println("saveFile Error, you must select a folder for the save file");
	            display();
            }
            
            type = getFileType(modelFile);
            if(type != MODEL) {
            	System.out.println("modelFile Error, the model file must be a .model file. You can generate .model files "
            			+ "using Weka's ColorClustering ImageJ plugin. See the github page for more instructions.");
	            display();
            }
            
            
            if(getFileType(tifFile) != getFileType(rootFile)) {
            	System.out.println("Error, the chosen tif file and root file must be of the same type (i.e. if you choose a folder for "
            			+ "the tif file you must choose a folder for the root file.");
            	display();
            }
            
        }
        else {
        	return;
        }
        
        
	}

/**
 * 	Method for starting the Menu display process.
 */
	public void run() {
		display();
	}
	
	
	/**
	 * Displays the UI to let the user select a new file. Records the chosen file to the 
	 * this Menu object for later reference.
	 * 
	 * @param file : an integer representing what file the user is
	 *  currently choosing (model file, image file,etc)
	 * 
	 */
	private void selectFiles(int file) {
		System.out.println("FILE :" + file);
		JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Image to load or file to iterate through.");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
     
        
       
        FileNameExtensionFilter imageFilter = new FileNameExtensionFilter("Image Files and Folders", "jpg", "jpeg", "png", "gif", "model","tif","tiff");
        fileChooser.setFileFilter(imageFilter);
        
        int result = fileChooser.showOpenDialog(null);
        
        if(result != JFileChooser.APPROVE_OPTION) {
        	System.out.println("Error, invalid option.");
        	return;
        }
        
        
    	switch(file) {
    	
    	case 1:
    		this.rootFile = fileChooser.getSelectedFile();
    		System.out.println("Chosen: " + this.rootFile.getAbsolutePath());
    		break;
    	case 2:
    		this.tifFile = fileChooser.getSelectedFile();
    		System.out.println("Chosen: " + this.tifFile.getAbsolutePath());
    		break;
    		
    	case 3:
    		this.modelFile = fileChooser.getSelectedFile();
    		System.out.println("Chosen: " + this.modelFile.getAbsolutePath());
    		break;
    		
    	case 4: 
    		this.saveFile = fileChooser.getSelectedFile();
    		System.out.println("Chosen: " + this.saveFile.getAbsolutePath());
    		break;
    	}
    
        
	}

	
	
	 /**
     * Returns 1 for folder, 2 for accepted 
image type, 3 for .model file, or 4 for any other filetype. 
     */
    public static int getFileType(File file) {
    	String extension = file.getName().substring(file.getName().lastIndexOf(".") + 1);
    	int FILETYPE = 0;
    	if(file.isDirectory()) {
    		FILETYPE = FOLDER;
    	}
    	// all currently accepted image file types.
    	else if(extension.equalsIgnoreCase("jpg") || extension.equalsIgnoreCase("png")
    			|| extension.equalsIgnoreCase("jpeg") || extension.equalsIgnoreCase("gif")
    			|| extension.equalsIgnoreCase("tif") || extension.equalsIgnoreCase("dcm")){
    		FILETYPE = IMAGE;
    	}
    	else if(extension.equalsIgnoreCase("model")) {
    		FILETYPE = MODEL;
    	}
    	else {
    		System.out.println("Selected file is not a folder or an acceptable "
    				+ "image type. Please ensure the image you're trying to enter"
    				+ "is the correct file type.");
    		FILETYPE = OTHERFILETYPE;
    	}
    	
    	return FILETYPE;
    }
	
	
	
	
	
	
	
	
	
}//end Menu class



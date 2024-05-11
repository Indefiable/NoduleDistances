/*
 * one pixel is (2601/ 4064256) mm^2
 */

package noduledistances.imagej;

import java.awt.Color;
import java.awt.Font;
import java.awt.Point;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;

import com.opencsv.exceptions.CsvValidationException;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.FreehandRoi;
import ij.gui.GenericDialog;
import ij.gui.ImageWindow;
import ij.gui.Line;
import ij.gui.OvalRoi;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.gui.TextRoi;
import ij.gui.Toolbar;
import ij.gui.WaitForUserDialog;
import ij.process.ColorProcessor;
import net.imagej.ImageJ;
import ij.gui.ImageCanvas;
import net.imagej.ops.OpService;
import trainableSegmentation.unsupervised.ColorClustering;
import trainableSegmentation.unsupervised.ColorClustering.Channel;

import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;



import traceskeleton.TraceSkeleton;


/**
 * 
 * 
 * @author Brandin Farris
 *
 */
@Plugin(type = Command.class, menuPath = "Plugins>NoduleData")
public class NoduleDistances implements Command {
	
	private final int FOLDER = 1;
	private final int IMAGE = 2;
	private final int MODEL = 3;
	private final int OTHERFILETYPE = 4;
	private final int NODERADIUS=3;
	public static ImagePlus image;
	
	
	public static final int SCALEFACTOR = 2;
	public static int initialWidth;
	public static int initialHeight;
	
    @Parameter
    private LogService logService;

    @Parameter
    private UIService uiService;

    @Parameter
    private OpService opService;
   
    
    
    @Parameter(label = "Tif file with nodule data.")
	private File tif;
    
    @Parameter(label = "Image of root system.")
	private File rootFile;
  
    /*
    @Parameter(label = "cluster model to use for segmentation.")
    private File modelFile;
    */
    
    
    // MAKING IMAGE SMALLER FOR TESTING PURPOSES.
 	//=====================================================
    private ImagePlus crop(ImagePlus imp) {
    	
		int newWidth = (int) (imp.getWidth() / SCALEFACTOR);
		int newHeight = (int) (imp.getHeight() / SCALEFACTOR);
		
		int x =(int) ((imp.getWidth() - newWidth)/2);
		
		int y =(int) ((imp.getHeight() - newHeight)/2);
		
		imp.setRoi(x, y, newWidth, newHeight); // cropping image to center of image and halving the size.
		imp = imp.crop();
		imp.setTitle(imp.getTitle().substring(4));
		
		return imp;
    }
	
    
    
	/**
	 * Displays
	 * @param startNode
	 * @param endNode
	 * @param graph
	 * @param overlayedGraph
	 */
	public ImagePlus shortestPath(int startNodeIndex, int endNodeIndex, RootGraph graph, GraphOverlay graphOverlay) {
		
		ArrayList<ImagePlus> imps = new ArrayList<>();
		
		startNodeIndex--;
		endNodeIndex--;
		
		ColorProcessor cp = new ColorProcessor(graphOverlay.overlayedGraph.getImage());
		ImagePlus SP = new ImagePlus("Shortest Path", cp);
		
		Overlay overlay = new Overlay();
		SP.setOverlay(overlay);
		SP.getOverlay().add(graphOverlay.overlayedGraph.getOverlay());
		
		Node[] nodules = graph.getNodules();
		
		if(startNodeIndex > nodules.length || startNodeIndex < 0) {
			System.out.println("Invalid start node. ");
			return null;
		}
		else if(endNodeIndex > nodules.length || endNodeIndex < 0) {
			System.out.println("Invalid end node. ");
			return null;
		}
		
		Node startNode = graph.getNodules()[startNodeIndex];
		Node endNode = graph.getNodules()[endNodeIndex];
		
		ArrayList<int[]> paths = startNode.getPaths(graph.nodes.indexOf(endNode));
		
		if(paths.size() == 0) {
			System.out.println("No paths between the two nodes.");
			return null;
		}
		
	    int numOfPaths = 0;
	    for (int[] path : paths) {
	        if (path != null) {
	            numOfPaths++;
	        }
	    }
		
	    Font font = new Font("SansSerif", Font.BOLD, 30); // Example font with size 20
	    TextRoi text = new TextRoi(10, 10, "Num of Paths: " + numOfPaths);
	    text.setFont(font);
	    text.setStrokeColor(Color.WHITE);
	    text.setFillColor(Color.BLACK);
	    SP.getOverlay().add(text);
	    
	    
	    
		Color[] colors = new Color[] {new Color(255, 255, 255),
									  new Color(0,0,255),
									  new Color(255,0,255),
									  new Color(255,165,0),
									  new Color(128,0,128)};
		
		int jj =0;
		
		for(int kk = numOfPaths-1; kk >= 0; kk--) {
			
			int[] path  = paths.get(kk);
			
			if(path == null) {
				System.out.println("Can't cheese it like that, Farris. Gotta properly remove the null paths from paths before starting :/");
			}
			
			int distance = path[1];
			
			System.out.println(distance + " pixels between " + (startNodeIndex +1) +
					" and " + (endNodeIndex+1));
			
			
			//color edges to show path
			// ignore first two entries as they're not part of the path
			for(int ii = 2; ii < path.length-1; ii++) {
				
				Node node1 = graph.nodes.get(path[ii]);
				Node node2 = graph.nodes.get(path[ii+1]);
				
				Line line = new Line(node1.x, node1.y, node2.x, node2.y);
				line.setStrokeWidth(7);
				line.setStrokeColor(colors[jj]);
				SP.getOverlay().add(line);
			}
			
			
			//highlight start and end nodules
			int noduleRadius = NODERADIUS+10;
		    
			OvalRoi ball1 = new OvalRoi( startNode.x - noduleRadius,  startNode.y - noduleRadius,
					2 * noduleRadius, 2 * noduleRadius);
			ball1.setFillColor(Color.ORANGE);
			
			
			OvalRoi ball2 = new OvalRoi( endNode.x - noduleRadius,  endNode.y - noduleRadius,
					2 * noduleRadius, 2 * noduleRadius);
			ball2.setFillColor(Color.ORANGE);
			
			SP.getOverlay().add(ball1);
			SP.getOverlay().add(ball2);
			
			jj++;
			
		}
		
		return SP;
	}
	
	/**
	 * increases brightness and contrast in the image to improve segmentation of root system.
	 * Also crops the image for testing purposes if I'm working on my laptop.
	 * @param imp
	 * @return
	 */
	public ImagePlus preprocessing(ImagePlus imp) {
		
		ImagePlus image = new ImagePlus(imp.getTitle(), imp.getProcessor());
		
		//first entry is percent contrast change (2f = 200%), second value is brightness increase
		RescaleOp op = new RescaleOp(2f, 25, null);
		
		BufferedImage output = op.filter(image.getBufferedImage(), null);

		image = new ImagePlus(image.getTitle(), output);
		
		//IJ.save(image, "C:\\Users\\Brand\\Documents\\Research\\DistanceAnalysis\\PS033\\" + "PS033_preprocessed1.jpg");
		return image;
	}

	
	public static ImagePlus blackenOutsideRectangle(ImagePlus image) {
		
		image.show();
        image.getWindow().toFront();
        
        @SuppressWarnings("unused")
		Toolbar toolbar = new Toolbar();
        
        Toolbar.getInstance().setTool(Toolbar.RECTANGLE);
        /**
        FreehandRoi freehandRoi = new FreehandRoi(0, 0, image);
        
        freehandRoi.setStrokeColor(Color.YELLOW);

        image.setRoi(freehandRoi);
        */
        // Wait for the user to finish drawing the ROI
        new WaitForUserDialog("Please outline the root system.").show();
        
        // Get the ROI drawn by the user
        Roi roi = image.getRoi();
        
        if(image.getRoi() == null || image.getRoi().getContainedPoints().length == 0) {
        	image.close();
			IJ.log("No outline made. Cancelling.");
			return null;
		}
        
     // Create a shape ROI from the rectangle ROI
        ShapeRoi shapeRoi = new ShapeRoi(roi);
        
        // Create a mask ROI to represent the area outside the rectangle
        ShapeRoi outsideRoi = new ShapeRoi(new Roi(0, 0, image.getWidth(), image.getHeight()));
        
       // outsideRoi = outsideRoi.subtract(shapeRoi);
        outsideRoi = outsideRoi.not(shapeRoi);
        
        // Set the outside area to black
        image.getProcessor().setColor(Color.BLACK);
        image.getProcessor().fill(outsideRoi);
      
        ImagePlus impp = image.duplicate();
        image.getWindow().close();
        
       
        impp.setTitle(image.getTitle());
        return impp;
		
		
    }
	
	
	/**
	 * Saves the computed distance data to a csv file to the provided
	 * directory. 
	 * @param graph graph object used to compute and hold the distance data.
	 */
	private void saveDistanceData(RootGraph graph) {
	    	
	}
	
	
	 public static List<int[]> findPairs(int num, int numIters) {
	        List<int[]> pairs = new ArrayList<>();
	        Random random = new Random();
	        
	        for (int i = 0; i <= numIters; i++) {
	            int first = random.nextInt(num + 1);
	            int second = random.nextInt(num + 1);
	            pairs.add(new int[]{first, second});
	        }
	        
	        return pairs;
	    }
	
	/**
     * This method executes the image analysis.
     * 
     * @param roots : image to run the data analysis on.
     * @param model : path file to selected .model file 
     */
    //ImagePlus image, String model
    private void execute(ImagePlus roots, ImagePlus tifImp, String saveFile) {
    	
    	
		tifImp.setTitle(tifImp.getTitle().substring(0,5));
		roots.setTitle(roots.getTitle().substring(0,5));
		
		if(!tifImp.getTitle().equalsIgnoreCase(roots.getTitle())) {
			System.out.println("Names are not the same");
			System.out.println(tifImp.getTitle());
			System.out.println(roots.getTitle());
		}
		
	//	IJ.save(roots, "C:\\Users\\Brand\\Documents\\Research\\DistanceAnalysis\\PS033\\" + "PS033_preprocessed44.jpg");
		
    	if(roots.getHeight() != tifImp.getHeight()) {
    		roots = crop(roots);
    		System.out.println("roots: " + roots.getWidth() + " x " + roots.getHeight());

        	System.out.println("tifImp: " + tifImp.getWidth() + " x " + tifImp.getHeight());
    	}
    	
    	if(roots.getWidth() != tifImp.getWidth()) {
    		System.out.println("I dunno at this point bruh.");
    		return;
    	}
    	
    	//roots = blackenOutsideRectangle(roots);
    	
    	ArrayList<Channel> channels = new ArrayList<Channel>(); // channels to use when segmenting.
    	channels.add(Channel.Brightness);
    	channels.add(Channel.Lightness);
		
		if(roots.getType() != ImagePlus.COLOR_RGB) {
			roots = new ImagePlus(roots.getTitle(), roots.getProcessor().convertToRGB());
		}
		RoiOverlay roiOverlay;
		
		try {
			roiOverlay = new RoiOverlay(tifImp);
		}catch(Exception e) {
			e.printStackTrace();
			return;
		}
		
		NoduleDistances.image = preprocessing(roots);
		
		
		ColorClustering cluster = new ColorClustering(NoduleDistances.image);
		//cluster.loadClusterer("C:\\Users\\Brand\\Documents\\Eclipse Workspace\\noduledistances\\assets\\001_roots.model");
		cluster.loadClusterer("D:\\1EDUCATION\\aRESEARCH\\ClusterModels\\001_roots.model");
		cluster.setChannels(channels);
		
		RootSegmentation root = new RootSegmentation(cluster, roiOverlay.rois);
		
		ArrayList<ArrayList<int[]>> skeleton = Skeletonize.skeletonize(root.binarymap);
		
		GraphOverlay graphOverlay = new GraphOverlay();
		//graphOverlay.loadTif();
		
		RootGraph graph = new RootGraph(skeleton, graphOverlay);
		
		graph.addNodules(roiOverlay.getRoiCentroids());
		
		graphOverlay.overlayGraph(graph, root.binarymap.getProcessor().convertToColorProcessor());
		
		//IJ.save(graphOverlay.overlayedGraph, saveFile + "\\" + roots.getTitle() + "_graph.jpg");
		
		//IJ.save(graphOverlay.overlayedGraph, "C:\\Users\\Brand\\Documents\\Research\\DistanceAnalysis\\PS033\\" + "PS033_overlayed_graph.jpg");
		//IJ.save(graphOverlay.overlayedGraph, "D:\\1EDUCATION\\aRESEARCH\\DistanceAnalysis_V0.1\\testingOutput\\" + tifImp.getTitle() + ".jpg");
		graph.computeShortestDistances(5);
		//graphOverlay.overlayedGraph.show();
		
		List<int[]> pairs = findPairs(graph.numNodules, 5);
		
		for(int[] pair : pairs) {
			
			ImagePlus out = shortestPath(pair[0],pair[1],graph, graphOverlay);
			IJ.saveAs(out, "jpg", "D:\\1EDUCATION\\aRESEARCH\\DistanceTesting\\DistanceTesting\\PS033\\PS033_paths_" 
					+ Integer.toString(pair[0])+ "_" + Integer.toString(pair[1]));
		}
		
		
		
		
		
		try {
			Statistics stats = new Statistics(roots.getTitle());
			stats.generateData(graph, new int[] {100,150,250,500,1000}, saveFile);
			
		} catch (CsvValidationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//graphOverlay.showGraph();
		//shortestPath(1,7,graph, graphOverlay).show();
    }

    /**
     * Returns 1 for folder, 2 for accepted image type, 3 for .model file, or 4 for any other filetype. 
     */
    private int getFileType(File file) {
    	String extension = file.getName().substring(file.getName().lastIndexOf(".") + 1);
    	int FILETYPE = 0;
    	if(file.isDirectory()) {
    		FILETYPE = FOLDER;
    	}
    	// all currently accepted image file types.
    	else if(extension.equalsIgnoreCase("jpg") || extension.equalsIgnoreCase("png")
    			|| extension.equalsIgnoreCase("jpeg") || extension.equalsIgnoreCase("gif")
    			|| extension.equalsIgnoreCase("tiff") || extension.equalsIgnoreCase("dcm")
    			|| extension.equalsIgnoreCase("tif")){
    		FILETYPE = IMAGE;
    	}
    	else if(extension.equalsIgnoreCase("model")) {
    		FILETYPE = MODEL;
    	}
    	else {
    		System.out.println("Selected file ir not a folder or an acceptable "
    				+ "image type. Please ensure the image you're trying to enter"
    				+ "has the correct end abbreviation.");
    		FILETYPE = OTHERFILETYPE;
    	}
    	
    	return FILETYPE;
    }
    
    @Override
    public void run() {
    	
    	
    	/*
    	int FILETYPE = 0;
    	
    	
    	FILETYPE = getFileType(modelFile);
    	if( FILETYPE != MODEL) {
    		System.out.println("Please select a .model file when prompted. If you do not have one, you can "
    				+"generate one using Weka's ColorClustering plugin via ImageJ (or FIJI) to make one.");
    		System.exit(0);
    	}
    	
    	FILETYPE = getFileType(file);
    	
    	int subtype = 0;
    	
    	switch(FILETYPE) {
    	case FOLDER: 
    		for(File subfile : file.listFiles()) {
    			subtype = getFileType(subfile);
    			
    			if(subtype != IMAGE) {
        			continue;
        		}
    			try {
    				ImagePlus tempim = new ImagePlus(subfile.getPath());
    				execute(tempim, modelFile.getPath());
    			}catch(Exception e) {
    				e.printStackTrace();
    				System.out.println("Could not generate data for " + subfile.getName());
    			}
    			
    		}
    		break;
    		
    	case IMAGE:
    		try {
    		execute(new ImagePlus(file.getPath()), modelFile.getPath());
    		}catch(Exception e) {
    			e.printStackTrace();
    			System.out.println("Could not generate data for " + file.getName());
    		}
    		break;
    		
    	case OTHERFILETYPE:
    		System.exit(0);
    		break;
    	}
    	
    	File rootsFile = null;
    	File tifFile = null;
    	File saveFile = null;
    	
    	File initialDirectory = new File("D:\\1EDUCATION\\aRESEARCH\\DistanceAnalysis_V0.1");
        
        
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("folder containing root images.");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        fileChooser.setCurrentDirectory(initialDirectory);
        
        int result = fileChooser.showOpenDialog(null);
        
        if( result == JFileChooser.APPROVE_OPTION) {
        	rootsFile = fileChooser.getSelectedFile();
        }
        else {
        	System.out.println("error, you did not choose an acceptable file type.");
        	System.exit(0);
        }
        fileChooser.setCurrentDirectory(initialDirectory);
        fileChooser.setDialogTitle("folder containing tif images.");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

        result = fileChooser.showOpenDialog(null);
        
        if( result == JFileChooser.APPROVE_OPTION) {
        	tifFile = fileChooser.getSelectedFile();
        }
        else {
        	System.out.println("error, you did not choose an acceptable file type.");
        	System.exit(0);
        }
        
        
        fileChooser.setCurrentDirectory(initialDirectory);
        fileChooser.setDialogTitle("Choose a save location for the output data.");
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        result = fileChooser.showOpenDialog(null);
        
        if(result == JFileChooser.APPROVE_OPTION) {
        	saveFile = fileChooser.getSelectedFile();
        }
        
       if(rootsFile == null) {
    	   System.out.println("Sorry, but the file you selected is not valid.");
    	   return;
       }
    	
    	
       for(File rootFile : rootsFile.listFiles()) {
			int subtype = getFileType(rootFile);
			File currentTif = getTifFile(rootFile, tifFile);
			if(currentTif == null) {
				continue;
			}
			else if(getFileType(currentTif) != IMAGE);
			if(subtype != IMAGE) {
   			continue;
   		}*/
		String saveString = "C:\\Users\\Brand\\Documents\\Research\\DistanceAnalysis\\testing";
			
		try {
			ImagePlus rootImp = new ImagePlus(rootFile.getPath());
			ImagePlus tifImp = new ImagePlus(tif.getPath());
			execute(rootImp, tifImp, saveString);
		}catch(Exception e) {
			e.printStackTrace();
			System.out.println("Could not generate data for " + rootFile.getName());
		}
			
			
			
		
   
    	
    	IJ.log("done");
    }//===========================================================================================

    private File getTifFile(File rootFile, File tifFile) {
    	
    	String name = rootFile.getName();
    	name = name.substring(0,5);
    	 for(File tif : tifFile.listFiles()) {
    		 
    		 if (tif.getName().startsWith(name)) {
    			 return tif;
    		 }
 			
 		}
    	
    	return null;
    }
    
    
    
    
    /**
     * This main function serves for development purposes.
     * It allows you to run the plugin immediately out of
     * your integrated development environment (IDE).
     *
     * @param args whatever, it's ignored
     * @throws Exception
     */
    public static void main(final String... args) throws Exception {
        // create the ImageJ application context with all available services
        final ImageJ ij = new ImageJ();
        ij.launch(args);
            // invoke the plugin
            ij.command().run(NoduleDistances.class, true);
        }
    }
	


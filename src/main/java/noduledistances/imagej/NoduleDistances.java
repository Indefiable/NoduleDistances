/*
 * one pixel is (2601/ 4064256) mm^2
 */

package noduledistances.imagej;

import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import ij.process.ImageConverter;

import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;

import com.opencsv.exceptions.CsvValidationException;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Line;
import ij.gui.OvalRoi;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.gui.TextRoi;
import ij.gui.Toolbar;
import ij.gui.WaitForUserDialog;
import ij.plugin.ContrastEnhancer;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import net.imagej.ImageJ;
import net.imagej.ops.OpService;
import trainableSegmentation.unsupervised.ColorClustering;
import trainableSegmentation.unsupervised.ColorClustering.Channel;

import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;





/**
 * 
 * 
 * @author Brandin Farris
 *
 */
@Plugin(type = Command.class, menuPath = "Plugins>NoduleData")
public class NoduleDistances implements Command {
	
	
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
 
    
   /**
    * Halves the width and height and crops the image
    * to the center of the image. This is done for testing 
    * purposes to decrease runtime. 
    * @param imp : image to crop
    * @return : cropped version of the input image.
    */
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
	 * Generates an image showing the shortest 5 paths between the two given nodes by \
	 * coloring the paths taken 5 distinct colors
	 * 
	 * @param startNode : start node of the paths we're highlighting.
	 * @param endNode : end node of the paths we're highlighting.
	 * @param graph : graph object of the root system.
	 * @param overlayedGraph : Image of the root system with the graph object overlayed.
	 */
	public ImagePlus shortestPath(int startNodeIndex, int endNodeIndex, RootGraph graph, GraphOverlay graphOverlay) {
		
		
		int width = graphOverlay.overlayedGraph.getWidth();
		int height = graphOverlay.overlayedGraph.getHeight();
		
		ColorProcessor cp = new ColorProcessor(width, height);
		
		cp.setColor(Color.WHITE);
		cp.setRoi(0, 0, width, height);
		cp.fill();
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
			System.out.println("No paths between" 
			+ Double.toString(startNode.nodeNumber) +" and " + Double.toString(endNode.nodeNumber) );
			return null;
		}
		
	    int numOfPaths = 0;
	   
	    for (int[] path : paths) {
	        if (path != null) {
	            numOfPaths++;
	        }
	    }
		
	    Font font = new Font("SansSerif", Font.BOLD, 30); // Example font with size 20
	    TextRoi text = new TextRoi(10, 10, "Num of Paths: " + numOfPaths+ "   ");
	    text.setFont(font);
	    text.setStrokeColor(Color.WHITE);
	    text.setFillColor(Color.BLACK);
	    SP.getOverlay().add(text);
	    
	    
	    
		Color[] colors = new Color[] {new Color(0, 0, 0),
									  new Color(0,0,255),
									  new Color(255,0,255),
									  new Color(255,165,0),
									  new Color(128,0,128)};
		
		int jj =0;
		
		for(int kk = numOfPaths-1; kk >= 0; kk--) {
			
			int[] path  = paths.get(kk);
			
			if(path == null) {
				System.out.println("Can't cheese it like that, Farris. Gotta properly remove the null paths from paths before starting :/");
				continue;
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
	 * 
	 * @param imp : image of the root system. 
	 * @return preprocessed image
	 */
	public ImagePlus preprocessing(ImagePlus imp) {
 		
	    ImagePlus hsb = new ImagePlus("HSB", imp.getProcessor().convertToRGB());
        ImageConverter ic = new ImageConverter(hsb);
        ic.convertToHSB();
        
        
        ImageProcessor bright = hsb.getStack().getProcessor(3).convertToFloatProcessor();
        ContrastEnhancer ce = new ContrastEnhancer();
        ce.stretchHistogram(bright, 0.5); // Adjust the percentage stretch as needed
        
        hsb.getStack().addSlice("brightness", bright, 2);
        hsb.getStack().deleteLastSlice();
        ic.convertHSBToRGB();
        if(hsb.isRGB()) {
        	System.out.println("Converted back to RGB");
        }
       
		
		ImagePlus image = new ImagePlus(imp.getTitle(), hsb.getProcessor());
		
		//first entry is percent contrast change (2f = 200%), second value is brightness increase
		RescaleOp op = new RescaleOp(2f, 25, null);
		
		BufferedImage output = op.filter(image.getBufferedImage(), null);

		image = new ImagePlus(image.getTitle(), output);
	
		return image;
	}
	
	

	/**
	 * Method that asks the user to draw a rectangle around the part of the image
	 * containing the root system to remove any noise outside the root system. This greatly 
	 * improves the root segmentation process.
	 * 
	 * @param image : image of the root system.
	 * @return : image with everything outside of the user-provided rectangle
	 * is black.
	 */
	public static ImagePlus blackenOutsideRectangle(ImagePlus image) {
		
		image.show();
        image.getWindow().toFront();
        
        @SuppressWarnings("unused")
		Toolbar toolbar = new Toolbar();
        
        Toolbar.getInstance().setTool(Toolbar.RECTANGLE);
      
        // Wait for the user to finish drawing the ROI
        new WaitForUserDialog("Please outline the root system.").show();
        
        // Get the ROI drawn by the user
        Roi roi = image.getRoi();
        
        if(image.getRoi() == null || image.getRoi().getContainedPoints().length == 0) {
        	image.close();
			IJ.log("No outline made. Cancelling.");
			return null;
		}
        
   
        ShapeRoi shapeRoi = new ShapeRoi(roi);
        
       
        ShapeRoi outsideRoi = new ShapeRoi(new Roi(0, 0, image.getWidth(), image.getHeight()));
        
     
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
	 * Finds random sets of pairs of nodules.
	 * @param num : number of nodules in the image.
	 * @param numIters : the number of pairs to create.
	 * @return : numIters pairs of nodules, randomly generated.
	 */
	 public static List<int[]> findPairs(int num, int numIters) {
	        List<int[]> pairs = new ArrayList<>();
	        Random random = new Random();
	        
	        for (int i = 0; i <= numIters; i++) {
	            int first = random.nextInt(num);
	            int second = random.nextInt(num);
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
    private void execute(ImagePlus roots, ImagePlus tifImp, String saveFile, File modelFile, int numIters, 
    		String redAttribute, String greenAttribute) {
    	
    	if(roots.getType() != ImagePlus.COLOR_RGB) {
			roots = new ImagePlus(roots.getTitle(), roots.getProcessor().convertToRGB());
		}
    	
		tifImp.setTitle(tifImp.getTitle().substring(0,5));
		roots.setTitle(roots.getTitle().substring(0,5));
		
		if(!tifImp.getTitle().equalsIgnoreCase(roots.getTitle())) {
			System.out.println("Names are not the same");
			System.out.println(tifImp.getTitle());
			System.out.println(roots.getTitle());
		}
		
	//	IJ.save(roots, saveFile + "PS033_preprocessed44.jpg");
		
    	if(roots.getHeight() != tifImp.getHeight()) {
    		
    		System.out.println("roots: " + roots.getWidth() + " x " + roots.getHeight());

        	System.out.println("tifImp: " + tifImp.getWidth() + " x " + tifImp.getHeight());
    	}
    	
    	if(roots.getWidth() != tifImp.getWidth()) {
    		System.out.println("Error, roots image and tif image are not identical in size.");
    		return;
    	}
    	

    	//remove some noise by having user highlight only the root system.
    	roots = blackenOutsideRectangle(roots);
    	
    	
    	// channels to use when segmenting.
    	ArrayList<Channel> channels = new ArrayList<Channel>(); 
    	channels.add(Channel.Brightness);
    	channels.add(Channel.Lightness);
		
		if(roots.getType() != ImagePlus.COLOR_RGB) {
			roots = new ImagePlus(roots.getTitle(), roots.getProcessor().convertToRGB());
		}
		
		//retrieve roi information from tif image
		RoiOverlay roiOverlay;
		try {
			roiOverlay = new RoiOverlay(tifImp);
		}catch(Exception e) {
			e.printStackTrace();
			return;
		}
		
		
		System.out.println("Preprocessing...");
		NoduleDistances.image = preprocessing(roots);
	
		
		ColorClustering cluster = new ColorClustering(NoduleDistances.image);	
		cluster.loadClusterer(modelFile.getAbsolutePath());

		cluster.setChannels(channels);
		
		
		System.out.println("Segmenting...");
		RootSegmentation root = new RootSegmentation(cluster, roiOverlay.rois);
		
		System.out.println("Skeletonizing...");
		ArrayList<ArrayList<int[]>> skeleton = Skeletonize.skeletonize(root.binarymap);
		
		GraphOverlay graphOverlay = new GraphOverlay();
		
		
		System.out.println("Making graph...");
		RootGraph graph = new RootGraph(skeleton, graphOverlay);
		
		//computes the centroids of the rois to use as nodes in the graph.
		System.out.println("Adding nodules...");
		ArrayList<int[]> centroids = roiOverlay.getRoiCentroids(graph);
		
		for(int[] center : centroids) {
			
			if(center == null) {
				System.out.println("Breakpoint.");
			}
		}
		
		
		graph.addNodules(centroids);
		
		//Locates and merges sections of the graph that are disconnected. This is due to 
		//Ling Dong's skeletonization algorithm not always producing a connected skeleton
		// or there being shaded parts on the root system that confuses color-based segmentation.
		System.out.println("Merging components...");
		ArrayList<int[]> components = UnionFind.connectedComponents(graph.fsRep, graph.nodes.size());
		if(components.size() >1) {	
			graph.mergeNonemptyComponents(components);
		}
		
		
		
		System.out.println("Overlaying graph...");
		graphOverlay.overlayGraph(graph, root.binarymap.getProcessor().convertToColorProcessor());
		

		IJ.save(graphOverlay.overlayedGraph, saveFile + "\\" + roots.getTitle() + "_graph.jpg");
		
		
		System.out.println("Computing shortest distances...");
		graph.computeShortestDistances(numIters);
		
	
		try {
			System.out.println("Initializing statistics generator.");
			Statistics stats = new Statistics(roots.getTitle(), redAttribute, greenAttribute);
			System.out.println("Generating Statistics.");
			stats.generateData(graph, new int[] {100,150,250,500,1000}, saveFile);
			stats.savePairwiseDistanceMatrices(graph, saveFile, numIters);
		} catch (CsvValidationException e) {
			
			e.printStackTrace();
		}
	
    }

    

    
    /**
     * Saves a set of images that are random computed shortest paths between the nodule nodes.
     * 
     * @param graph : object that contains all of the graph information.
     * @param graphOverlay : Object that is used to overlay the graph onto the root system for visual purposes.
     * @param saveFile : user-specified location for saving the output of the plugin.
     * @param title : what you want to name the images generated by this method.
     */
    public void saveRandomPaths(RootGraph graph, GraphOverlay graphOverlay, String saveFile, String title) {
    	System.out.println("Finding random paths...");
		List<int[]> pairs = findPairs(graph.numNodules-1, 5);
		
		for(int[] pair : pairs) {
			
			ImagePlus out = shortestPath(pair[0],pair[1],graph, graphOverlay);
			if(out == null) {
			
				continue;
			}
			
			IJ.saveAs(out, "jpg", saveFile + title +"_" 
			+ Integer.toString(pair[0])+ "_" + Integer.toString(pair[1]));
			
		}
    }
    
    
    
    @Override
    public void run() {
    	Menu menu = new Menu();
    	menu.run();
    	
    	
    	if(menu.rootFile == null) {
    		return;
    	}
    	File rootsFile = menu.rootFile;
    	File tifFile = menu.tifFile;
    	File modelFile = menu.modelFile;
    	File saveFile = menu.saveFile;
    	
    	
    	
    	
    	if(Menu.getFileType(rootsFile) == Menu.IMAGE && Menu.getFileType(tifFile) != Menu.IMAGE) {
    		IJ.log("Error, if the roots file chosen is a folder, the tif file chosen must also be a folder.");
    		return;
    	}
    	
    	if(Menu.getFileType(rootsFile) != Menu.IMAGE && Menu.getFileType(tifFile) == Menu.IMAGE) {
    		IJ.log("Error, if the tif file chosen is a folder, the roots file chosen must also be a folder.");
    		return;
    	}
    	
    	if(Menu.getFileType(rootsFile) == Menu.IMAGE && Menu.getFileType(tifFile) == Menu.IMAGE) {
    		ImagePlus rootImp = new ImagePlus(rootsFile.getAbsolutePath());
			ImagePlus tifImp = new ImagePlus(tifFile.getAbsolutePath());
			System.out.println("==================");
			System.out.println(rootImp.getShortTitle());
			System.out.println("==================");
			execute(crop(rootImp), tifImp, saveFile.getAbsolutePath(), modelFile, menu.numIters, menu.redAttribute, menu.greenAttribute);
			
    	}
    	else {//rootsFile is a folder containing root files.
        for(File rootFile : rootsFile.listFiles()) {
    	   
			int subtype = Menu.getFileType(rootFile);
			if(subtype != Menu.IMAGE) {
				continue;
			}
			File currentTif = getTifFile(rootFile, tifFile);
			if(currentTif == null) {
				continue;
			}
			else if(Menu.getFileType(currentTif) != Menu.IMAGE) {
				continue;
			}
			
			
			
			//String saveString = "C:\\Users\\Brand\\Documents\\Research\\DistanceAnalysis\\DistanceTesting";
			//String saveFile = "D:\\1EDUCATION\\aRESEARCH\\tempGitHub\\Nodule-Distances\\testing";
			try {
				ImagePlus rootImp = new ImagePlus(rootFile.getPath());
				ImagePlus tifImp = new ImagePlus(currentTif.getPath());
				System.out.println("==================");
				System.out.println(rootImp.getShortTitle());
				System.out.println("==================");
				execute(crop(rootImp), tifImp, saveFile.getAbsolutePath(), modelFile, menu.numIters,
						menu.redAttribute,menu.greenAttribute);
			}catch(Exception e) {
				e.printStackTrace();
				System.out.println("Could not generate data for " + rootFile.getName());
			}
       
       }//end looping through files
    	}//end else{}
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
	


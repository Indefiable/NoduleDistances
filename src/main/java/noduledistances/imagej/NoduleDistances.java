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

import ij.process.FloatProcessor;
import ij.process.ImageConverter;

import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;

import com.opencsv.exceptions.CsvValidationException;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
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
   
    
    /*
    @Parameter(label = "Tif file with nodule data.")
	private File tif;
    
    @Parameter(label = "Image of root system.")
	private File rootFile;
  
   
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
	 * Generates an image showing the shortest 5 paths between the two given nodes by \
	 * coloring the paths taken 5 distinct colors
	 * 
	 * @param startNode
	 * @param endNode
	 * @param graph
	 * @param overlayedGraph
	 */
	public ImagePlus shortestPath(int startNodeIndex, int endNodeIndex, RootGraph graph, GraphOverlay graphOverlay) {
		
		ArrayList<ImagePlus> imps = new ArrayList<>();
		
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
	    TextRoi text = new TextRoi(10, 10, "Num of Paths: " + numOfPaths);
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
	
	
	
	private static ImagePlus convertRGBtoYUV(ImageProcessor cp) {
        
		int width = cp.getWidth();
        int height = cp.getHeight();
        ImageStack stack = new ImageStack(width,height);
        
        FloatProcessor yp = new FloatProcessor(width, height);
        FloatProcessor up = new FloatProcessor(width, height);
        FloatProcessor vp = new FloatProcessor(width, height);
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int[] rgb = cp.getPixel(x, y, null);
                
                int r = rgb[0];
                int g = rgb[1];
                int b = rgb[2];

                float yVal = (float) (0.299 * r + 0.587 * g + 0.114 * b);
                float uVal =(float) (-0.14713 * r - 0.28886 * g + 0.436 * b);
                float vVal =(float) (0.615 * r - 0.51499 * g - 0.10001 * b);
                
                yp.setf(x, y, yVal);
                up.setf(x, y, uVal);
                vp.setf(x, y, vVal);
            }
        }
        
        stack.addSlice(yp);
        
        Object[] arrays = stack.getImageArray();
		if (arrays==null || (arrays.length>0&&arrays[0]==null)) {
			System.out.println("Null shit.");
		}
        stack.addSlice(up);
        
       arrays = stack.getImageArray();
		if (arrays==null || (arrays.length>0&&arrays[0]==null)) {
			System.out.println("Null shit.");
		}
        stack.addSlice(vp);
        
        arrays = stack.getImageArray();
		if (arrays==null || (arrays.length>0&&arrays[0]==null)) {
			System.out.println("Null shit.");
		}
		
        
			
	
        return new ImagePlus("yuv", stack);
    }
	
	
	
    private static ColorProcessor convertYUVtoRGB(ImageProcessor ipYUV) {
        int width = ipYUV.getWidth();
        int height = ipYUV.getHeight();
        
        
        ColorProcessor cp = new ColorProcessor(width, height);
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
            	
                int[] yuv = ipYUV.getPixel(x, y,null);
                
                
                int yVal = yuv[0];
                int uVal = yuv[1];
                int vVal = yuv[2];

                int r = (int) (yVal + 1.13983 * vVal);
                int g = (int) (yVal - 0.39465 * uVal - 0.58060 * vVal);
                int b = (int) (yVal + 2.03211 * uVal);

                r = Math.min(255, Math.max(0, r));
                g = Math.min(255, Math.max(0, g));
                b = Math.min(255, Math.max(0, b));

                cp.putPixel(x, y, new int[] {r, g, b});
            }
        }

        return cp;
    }
	
	
	
	/**
	 * increases brightness and contrast in the image to improve segmentation of root system.
	 * @param imp image
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
		
		//IJ.save(image, "C:\\Users\\Brand\\Documents\\Research\\DistanceAnalysis\\PS033\\" + "PS033_preprocessed1.jpg");
		return image;
	}

	/**
	 * Method that asks the user to draw a rectangle around the part of the image
	 * containing the root system to remove any noise outside the root system. This greatly 
	 * improves the root segmentation process.
	 * 
	 * @param image
	 * @return
	 */
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
	
	/**
	 * Finds random sets of pairs of nodules.
	 * @param num
	 * @param numIters
	 * @return
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
    private void execute(ImagePlus roots, ImagePlus tifImp, String saveFile, File modelFile) {
    	
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
		
	//	IJ.save(roots, "C:\\Users\\Brand\\Documents\\Research\\DistanceAnalysis\\PS033\\" + "PS033_preprocessed44.jpg");
		
    	if(roots.getHeight() != tifImp.getHeight()) {
    		//roots = crop(roots);
    		System.out.println("roots: " + roots.getWidth() + " x " + roots.getHeight());

        	System.out.println("tifImp: " + tifImp.getWidth() + " x " + tifImp.getHeight());
    	}
    	
    	if(roots.getWidth() != tifImp.getWidth()) {
    		System.out.println("I dunno at this point bruh.");
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
		//NoduleDistances.image.show();
		
		ColorClustering cluster = new ColorClustering(NoduleDistances.image);
		//cluster.loadClusterer("C:\\Users\\Brand\\Documents\\Eclipse Workspace\\noduledistances\\assets\\001_roots.model");
		//cluster.loadClusterer("D:\\1EDUCATION\\aRESEARCH\\ClusterModels\\001_roots.model");
		cluster.loadClusterer(modelFile.getAbsolutePath());
		cluster.setChannels(channels);
		
		
		System.out.println("Segmenting...");
		RootSegmentation root = new RootSegmentation(cluster, roiOverlay.rois);
		
		System.out.println("Skeletonizing...");
		ArrayList<ArrayList<int[]>> skeleton = Skeletonize.skeletonize(root.binarymap);
		
		GraphOverlay graphOverlay = new GraphOverlay();
		//graphOverlay.loadTif();
		
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
		//Ling Dong's skeletonization algorithm not always producing a connected skeleton.
		System.out.println("Merging components...");
		ArrayList<int[]> components = UnionFind.connectedComponents(graph.fsRep, graph.nodes.size());
		if(components.size() >1) {
			//System.out.println("Multiple components. Merging components that contain nodules.");
			graph.mergeNonemptyComponents(components);
		}
		
		
		
		
		System.out.println("Overlaying graph...");
		graphOverlay.overlayGraph(graph, root.binarymap.getProcessor().convertToColorProcessor());
		
		//graphOverlay.overlayedGraph.show();
		
		//IJ.save(graphOverlay.overlayedGraph, saveFile + "\\" + roots.getTitle() + "\\" + roots.getTitle() + "_graph.jpg");
		
		IJ.save(graphOverlay.overlayedGraph, saveFile + "\\" + roots.getTitle() + "_graph.jpg");
		
		
		System.out.println("Computing shortest distances...");
		graph.computeShortestDistances(5);
		//graphOverlay.overlayedGraph.show();
		
		try {
			System.out.println("Initializing statistics generator.");
			Statistics stats = new Statistics(roots.getTitle());
			System.out.println("Generating Statistics.");
			stats.generateData(graph, new int[] {100,150,250,500,1000}, saveFile);
			
		} catch (CsvValidationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//graphOverlay.showGraph();
		//shortestPath(1,7,graph, graphOverlay).show();
    }

    
    /**
     * Saves a set of images that are random computed shortest paths between the nodule nodes.
     * 
     * @param graph
     * @param graphOverlay
     * @param saveFile
     * @param title
     */
    public void saveRandomPaths(RootGraph graph, GraphOverlay graphOverlay, String saveFile, String title) {
    	System.out.println("Finding random paths...");
		List<int[]> pairs = findPairs(graph.numNodules-1, 5);
		
		for(int[] pair : pairs) {
			
			ImagePlus out = shortestPath(pair[0],pair[1],graph, graphOverlay);
			if(out == null) {
				//System.out.println("Could not generate images for paths between"
			    //+ Integer.toString(pair[0]) + " and " + Integer.toString(pair[1]));
				continue;
			}
			//IJ.saveAs(out, "jpg", saveFile + "\\" + roots.getTitle() + "\\" + roots.getTitle() 
			//+ Integer.toString(pair[0])+ "_" + Integer.toString(pair[1]));
			
			IJ.saveAs(out, "jpg", saveFile + title +"_" 
			+ Integer.toString(pair[0])+ "_" + Integer.toString(pair[1]));
			
		}
    }
    
    
    
    @Override
    public void run() {
    	Menu menu = new Menu();
    	menu.run();
    	
    	
    	
    	/**
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
    	
    	File initialDirectory = new File("D:\\1EDUCATION\\aRESEARCH\\DistanceTesting\\DistanceAnalysis_V1.0");
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
    	
    	if(menu.rootFile == null || menu.tifFile == null || menu.modelFile == null || menu.saveFile == null) {
    		IJ.log("done");
    		return;
    	}
    	File rootsFile = new File("D:\\1EDUCATION\\aRESEARCH\\DistanceTesting\\DistanceAnalysis_V1.0\\DistancesInput\\visible\\PS001_Trimmed_Image_1_Visible.JPG");
    	File tifFile = new File("D:\\1EDUCATION\\aRESEARCH\\DistanceTesting\\DistanceAnalysis_V1.0\\DistancesInput\\done\\PS001_Trimmed_Image_1.tif");
    	File modelFile = new File("D:\\1EDUCATION\\aRESEARCH\\ClusterModels\\001_roots.model");
    	File saveFile = new File("D:\\1EDUCATION\\aRESEARCH\\DistanceTesting\\DistanceAnalysis_V1.0\\testing_out");
    	*/
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
			execute(rootImp, tifImp, saveFile.getAbsolutePath(), modelFile);
			
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
				execute(rootImp, tifImp, saveFile.getAbsolutePath(), modelFile);
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
	


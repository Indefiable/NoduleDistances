/*
 * one pixel is (2601/ 4064256) mm^2
 */

package noduledistances.imagej;

import java.awt.Color;
import java.awt.Font;
import java.awt.Point;
import java.io.File;
import java.util.ArrayList;

import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Line;
import ij.gui.OvalRoi;
import ij.gui.Overlay;
import ij.gui.TextRoi;
import ij.process.ColorProcessor;
import net.imagej.ImageJ;
import net.imagej.ops.OpService;
import trainableSegmentation.unsupervised.ColorClustering;
import trainableSegmentation.unsupervised.ColorClustering.Channel;

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
   
    
    
    @Parameter(label = "Image to load or file to iterate through.")
	private File file;
    
    @Parameter(label = "Nodules location csv.")
    private File nodules;
    /*
    @Parameter(label = "cluster model to use for segmentation.")
    private File modelFile;
    */
    

	
	
	/**
	 * Finds the shortest path between the start and end nodules and colors the path between them.
	 * @param startNode
	 * @param endNode
	 * @param graph
	 * @param overlayedGraph
	 */
	public ImagePlus shortestPath(int startNodeIndex, int endNodeIndex, Graph graph, GraphOverlay graphOverlay) {
		
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
		
		
		int distance = startNode.distance[endNode.nodeIndex];
		System.out.println(distance + " pixels between " + (startNodeIndex +1) +
				" and " + (endNodeIndex+1));
		
		int[] prevNode = startNode.prevNode;
		
		int current = endNode.nodeIndex;
		ArrayList<Integer> path = new ArrayList<>();
	
		int counter = 0;
		//find path
		while(true) {
			counter++;
			path.add(current);
			
			if(current == startNode.nodeIndex) {
				break;
			}
			current = prevNode[current];
			if(counter >= graph.nodes.size()) {
				System.out.println("looping error.");
				break;
			}
		}
		
		//color edges to show path
		for(int ii = 0; ii < path.size()-1; ii++) {
			
			Node node1 = graph.nodes.get(path.get(ii));
			Node node2 = graph.nodes.get(path.get(ii+1));
			
			Line line = new Line(node1.x, node1.y, node2.x, node2.y);
			line.setStrokeWidth(4);
			line.setStrokeColor(Color.ORANGE);
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
		
		
		return SP;
		
	}
	
	public ImagePlus preprocessing(ImagePlus imp) {
		
		ImagePlus image = new ImagePlus(imp.getShortTitle(), imp.getProcessor());
		
		 // MAKING IMAGE SMALLER FOR TESTING PURPOSES.
		//=====================================================
		NoduleDistances.initialHeight = image.getHeight();
		NoduleDistances.initialWidth = image.getWidth();
		
		int newWidth = (int) (image.getWidth() / SCALEFACTOR);
		int newHeight = (int) (image.getHeight() / SCALEFACTOR);
		int x =(int) ((image.getWidth() - newWidth)/2);
		int y =(int) ((image.getHeight() - newHeight)/2);
		image.setRoi(x, y, newWidth, newHeight); // cropping image to center of image and halving the size.
		image = image.crop();
		//====================================================
		
		
		return image;
	}
	
	
	
    /**
     * This method executes the image analysis.
     * 
     * @param image : image to run the data analysis on.
     * @param model : path file to selected .model file 
     */
    //ImagePlus image, String model
    private void execute(ImagePlus image) {
    	
    	ArrayList<Channel> channels = new ArrayList<Channel>(); // channels to use when segmenting.
    	channels.add(Channel.Brightness);
    	channels.add(Channel.Lightness);
		
		if(image.getType() != ImagePlus.COLOR_RGB) {
			image = new ImagePlus(image.getTitle(), image.getProcessor().convertToRGB());
		}
		
		NoduleDistances.image = preprocessing(image);
		
		ColorClustering cluster = new ColorClustering(NoduleDistances.image);
	//	cluster.loadClusterer("C:\\Users\\Brand\\Documents\\Eclipse Workspace\\noduledistances\\assets\\001_roots.model");
		cluster.loadClusterer("C:\\Users\\Brand\\Documents\\Research\\DistanceAnalysis\\PS013_Light_Bright.model");
		cluster.setChannels(channels);
		
		RootSegmentation root = new RootSegmentation(cluster);
		
		
		ArrayList<ArrayList<int[]>> skeleton = root.skeletonize();
		
		GraphOverlay graphOverlay = new GraphOverlay();
		
		Graph graph = new Graph(skeleton, graphOverlay);
		
		graph.addNodules(nodules.getAbsolutePath());
		
		graphOverlay.overlayGraph(graph, NoduleDistances.image.getProcessor().convertToColorProcessor());
		
		graph.computeShortestDistances();
		
		shortestPath(1,7,graph, graphOverlay).show();
		
		System.out.println("breakpoint");
		
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
    			|| extension.equalsIgnoreCase("tiff") || extension.equalsIgnoreCase("dcm")){
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
    	*/
    	
   ;
    	ImagePlus im = new ImagePlus(file.getPath());
    	try {
    	execute(im);
    	}catch(Exception e) {
    		e.printStackTrace();
    		IJ.log("error, select an image file");
    	}
    	
    	IJ.log("done");
    }//===========================================================================================

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
	


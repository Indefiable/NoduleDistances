/*
 * one pixel is (2601/ 4064256) mm^2
 */

package noduledistances.imagej;

import java.awt.Color;
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
	private final int NODERADIUS=3;
	private final int OTHERFILETYPE = 4;
	public static ImagePlus image;
	public ImagePlus overlayedGraph = null;
	
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
	 * creates small dots where nodes are in the image. Overrides skeletonMap.
	 * @param nodes: list of nodes.
	 */
	public void overlayGraph(Graph graph, ColorProcessor cp) {

		ImagePlus skellyMap = new ImagePlus("skeleton", cp);

		Overlay overlay = new Overlay();
		skellyMap.setOverlay(overlay);

		for (ArrayList<int[]> chunk : graph.skeleton) {
			
			for(int ii = 0; ii < chunk.size(); ii+=2) {
				
				 	int[] edgeStart = chunk.get(ii);
				    int[] edgeEnd = chunk.get(ii+1);
				    
				    int startX = edgeStart[0];
				    int startY = edgeStart[1];
				    int endX = edgeEnd[0];
				    int endY = edgeEnd[1];

				    Line line = new Line(startX, startY, endX, endY);
				    line.setStrokeWidth(2);
				    line.setStrokeColor(Color.pink);
				    overlay.add(line);
			}
		}
		
		
		for(Point point : graph.nodes) {
			OvalRoi ball = new OvalRoi( point.x - NODERADIUS,  point.y - NODERADIUS, 2 * NODERADIUS, 2 * NODERADIUS);
			ball.setFillColor(Color.BLUE);
			skellyMap.getOverlay().add(ball);
		}
		
		//check if nodules have been added to graph yet.
		int[] types = graph.fsRep.stream().mapToInt(row -> row[2]).toArray();
		boolean containsNodules = false;
		
		for(int type : types) {
			if(type >0) {
				containsNodules=true;
			}
		}
		
		
		if(containsNodules) {
			overlayNodules(graph, skellyMap);
		}
		
		
		
		this.overlayedGraph = skellyMap;
	}
	
	
	public void overlayNodules(Graph graph, ImagePlus skellyMap){
		
		ArrayList<int[]> nodules = graph.noduleFSRep();
		
		
		for(int[] edge : nodules) {
			
			Node nodule = graph.nodes.get(edge[0]);
			Node node = graph.nodes.get(edge[1]);
			
			Line line = new Line(node.x, node.y, nodule.x, nodule.y);
			line.setStrokeWidth(2);
			
		    line.setStrokeColor(Color.ORANGE);
		    
		    int noduleRadius = NODERADIUS+2;
			OvalRoi ball = new OvalRoi( nodule.x - noduleRadius,  nodule.y - noduleRadius,
					2 * noduleRadius, 2 * noduleRadius);
			
			System.out.println(nodule.type);
			if(nodule.type == 1) {
				ball.setFillColor(Color.RED);
			}
			else if(nodule.type == 2) {
				ball.setFillColor(Color.GREEN);
			}
			else if(nodule.type == 3) {
				ball.setFillColor(Color.YELLOW);
			}
			else {
				System.out.println("Non-fatal Error, edge[3] should only be 0,1,2,3");
			}
			
			skellyMap.getOverlay().add(line);
			skellyMap.getOverlay().add(ball);
		}
		
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
    	channels.add(Channel.Hue);
    	channels.add(Channel.Lightness);
    	 
		
		if(image.getType() != ImagePlus.COLOR_RGB) {
			image = new ImagePlus(image.getTitle(), image.getProcessor().convertToRGB());
		}
		
		
		 // MAKING IMAGE SMALLER FOR TESTING PURPOSES.
		//=====================================================
		double factor = 2;
		int width = (int) (6000 / factor);
		int height = (int) (4000 / factor);
		int x =(int) ((6000 - width)/2);
		int y =(int) ((4000 - height)/2);
		image.setRoi(x, y, width, height); // cropping image to center of image and halving the size.
		image = image.crop();
		//=====================================================
		
		
		
		NoduleDistances.image = image;
		ColorClustering cluster = new ColorClustering(image);
		cluster.loadClusterer("D:\\1EDUCATION\\aRESEARCH\\tempGitHub\\Nodule-Distances\\assets\\001_roots.model");
		cluster.setChannels(channels);
		
		RootSegmentation root = new RootSegmentation(cluster);
		
		ArrayList<ArrayList<int[]>> skeleton = root.skeletonize();
		
		Graph graph = new Graph(skeleton);
		
		graph.addNodules(nodules.getAbsolutePath());
		
		overlayGraph(graph, root.binarymap.getProcessor().convertToColorProcessor());
		
		overlayedGraph.show();
		
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
	


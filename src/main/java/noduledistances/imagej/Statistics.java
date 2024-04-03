package noduledistances.imagej;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.StringJoiner;

/**
 * A class that generates all of the statistical data for analysis, and saves it as
 * one csv file.
 * 
 * 
 * Data we want for any given node:
 * ✅ distance to closest red nodule
 * ✅ distance to closest green nodule
 * ✅ mean distance to all red nodules
 * ✅ mean distance to all green nodules
 * ✅ whether closest nodule is red or green
 * ✅ # of red nodules within a given distance
 * ✅ # of green nodules within a given distance
 * 
 * 
 * As I code methods to compute this data, I can't help but feel there's a much more 
 * efficient way to go about this where I don't create a separate method iterating
 * through paths  for each data point desired. Rather than iterating through this paths list for
 *  each data desired, we itereate through the paths, and call methods that compute that specific data, 
 *  that way we only loop through ptahs once per nodule. Will have to refactor to do this. 
 * @author Farris
 */


public class Statistics {
	
	private static final int NUMNODSINBALL = 0;
	private static final int CLOSESTDISTANCE = 1;
	private static final int CLOSESTCOLORTYPE = 2;
	private static final int MEANDISTANCE = 3;
	
	
	public Statistics() {
		
	}
	
	
	
	/**
	 * Populates the String[][] data with computed statistics relevant to distance data. 
	 * @param graph
	 */
	public static void generateData(RootGraph graph, int radius) {
		ArrayList<Integer> options = new ArrayList<>();
		options.add(NUMNODSINBALL);
		options.add(CLOSESTDISTANCE);
		options.add(CLOSESTCOLORTYPE);
		options.add(MEANDISTANCE);
		
		String[] header = {"Roi", "Area", "Color", 
				"numNods in r = " + radius, "distance to closest nodule", "closest color type", "mean distance"};
	    
		String[][] mat = new String[graph.numNodules+1][7];
	    mat[0] = header;
		double[] data = null;
		int matCounter = 0;
		for (int ii = 0; ii < graph.nodes.size(); ii++) {
			
			Node nodule = graph.nodes.get(ii);
			//ignore skeleton nodes.
			if(nodule.type == Node.SKELETON) {
				continue;
			}
			
			data = computeStatistics(nodule.type, radius, nodule, graph, options);
			
			mat[matCounter+1][0] = Double.toString(nodule.nodeNumber);
			mat[matCounter+1][1] = Integer.toString(nodule.area);
			
			if(nodule.type == Node.RED) {
				mat[matCounter+1][2] = "Red";
			}
			else if(nodule.type == Node.GREEN) {
				mat[matCounter+1][2] = "Green";
			}
			else if(nodule.type == Node.MIXED) {
				mat[matCounter+1][2] = "Mixed";
			}
			else {
				System.out.println("Unknown node type.");
				System.out.println("breakpoint.");
			}
			
			mat[matCounter+1][3] = Double.toString(data[NUMNODSINBALL]);
			mat[matCounter+1][4] = Double.toString(data[CLOSESTDISTANCE]);
			mat[matCounter+1][5] = Double.toString(data[CLOSESTCOLORTYPE]);
			mat[matCounter+1][6] = Double.toString(data[MEANDISTANCE]);
			
			matCounter++;
		}
		String save = "C:\\Users\\Brand\\Documents\\Research\\DistanceAnalysis\\" + NoduleDistances.image.getShortTitle() + "_data.csv";
	
		try(FileWriter writer = new FileWriter(save)){
	     		StringJoiner comma = new StringJoiner(",");
	     		for ( String[] row : mat) {
	     			comma.setEmptyValue("");
	     			comma.add(String.join(",", row));
	     			writer.write(comma.toString());
	     			writer.write(System.lineSeparator());
	     			comma = new StringJoiner(",");
	     		}
	     		
	     		writer.flush();
	     		writer.close();
	     		System.out.println("=================");
	     		System.out.println("CSV FILE SAVED.");
	     		System.out.println("=================");
	     		
	     	}catch(IOException e) {
	     		System.out.println("=============================");
	     		System.err.println("Error writing CSV file: " + e.getMessage());
	     		System.out.println("============================");
	     	}
		
		
	}
	
	
	 /**
	  * Computes the number of nodules within a given radius of a given color. Note that 
	  * our ball here is not a literal ball, as we use the distances along the root systems
	  * to compute whether a node falls within the ball, i.e. distance along root system < radius. 
	  *
	  * @param color : color of nodules we're counting
	  * @param radius : radius cutoff
	  * @param node : node we're searching around (center)
	  * @param graph : graph object
	  * @return : the number of nodules of the given color with distance < radius.
	  */
	
	
	/**
	 * 
	 * @param color : color to restrict statistics to.
	 * @param radius : radius used for computing number of nodules within a radius
	 * @param node : node we're computing the statistics of	
	 * @param graph : graph object
	 * @param options : list telling us what statistics to compute
	 * @return
	 */
	protected static double[] computeStatistics(int color, int radius, Node node, RootGraph graph, ArrayList<Integer> options) {
		double[] data = new double[4];
		
		int closestDistance = Integer.MAX_VALUE; 
		int closestColorDistance = Integer.MAX_VALUE;
		int closestColorType = -1;
		int numNodsInBall = 0;
		double meanDistance = 0;
		int meanDistanceCounter = 0;
		
		ArrayList<ArrayList<int[]>> paths = node.paths;
		
		for(int ii = 0; ii < paths.size(); ii++) {
			ArrayList<int[]> SPToNodeii = paths.get(ii);
			
			for(int jj = 0; jj < SPToNodeii.size(); jj++) {
			    int[] path = SPToNodeii.get(jj);
			    
			   if(options.contains(NUMNODSINBALL)) {
				    // if distance is smaller and node is correct color.
				    if(path[1] < radius && graph.nodes.get(path[0]).type == color) {
				    	numNodsInBall++;
				    }
			   }
			   if(options.contains(CLOSESTDISTANCE)) {
				// if distance is smaller and node is correct color.
				    if(path[1] < closestDistance && graph.nodes.get(path[0]).type == color) {
				    	closestDistance = path[1];
				    }
			   }
			   if(options.contains(CLOSESTCOLORTYPE)) {
				   if(path[1] < closestColorDistance) {
				    	closestColorDistance = path[1];
				    	closestColorType = graph.nodes.get(path[0]).type;
				    }
			   }
			   
			   if(options.contains(MEANDISTANCE)) {
				   if(graph.nodes.get(path[0]).type == color) {
				    	meanDistance += path[1];
				    	meanDistanceCounter++;
				    }
			   }
			}	
		}
		
		data[NUMNODSINBALL] = numNodsInBall;
		data[CLOSESTDISTANCE] = closestDistance;
		data[CLOSESTCOLORTYPE] = closestColorType;
		data[MEANDISTANCE] = meanDistanceCounter;
		
		if(options.contains(MEANDISTANCE)) {
			meanDistance = meanDistance / meanDistanceCounter;
			meanDistance = Math.round(meanDistance * 1000.0) / 1000.0;
		}
		
		
		
		return data;
	}
	
	
	
	protected int numNodulesinBall(int color, int radius, Node node, RootGraph graph) {
		int numNods = 0;
		
		ArrayList<ArrayList<int[]>> paths = node.paths;
		
		for(int ii = 0; ii < paths.size(); ii++) {
			ArrayList<int[]> SPToNodeii = paths.get(ii);
			
			for(int jj = 0; jj < SPToNodeii.size(); jj++) {
			    int[] path = SPToNodeii.get(jj);
			   
			    // if distance is smaller and node is correct color.
			    if(path[1] < radius && graph.nodes.get(path[0]).type == color) {
			    	numNods++;
			    }
			    
			}	
		}
		
		return numNods;
	}
	
	
	/**
	 * @param color  == 1 -> red nodule
	 *  			 == 2 -> green nodule
	 * 				 == 3 -> mixed nodule
	 * 
	 * @return : the closest distance from a given node to another node of the given color
	 */
	protected int closestDistance(int color, Node node, RootGraph graph) {
		int distance = Integer.MAX_VALUE;
		int[] index = new int[2];
		
		ArrayList<ArrayList<int[]>> paths = node.paths;
		
		for(int ii = 0; ii < paths.size(); ii++) {
			ArrayList<int[]> SPToNodeii = paths.get(ii);
			
			for(int jj = 0; jj < SPToNodeii.size(); jj++) {
			    int[] path = SPToNodeii.get(jj);
			   
			    // if distance is smaller and node is correct color.
			    if(path[1] < distance && graph.nodes.get(path[0]).type == color) {
			    	distance = path[1];
			    	index[0] = ii;
			    	index[1] = jj;
			    }
			    
			    
			    
			}	
		}
		
		System.out.println("closest " + color + " colored node to " + node.nodeNumber + 
				" is " + paths.get(index[0]).get(index[1])[0]);
		
		return distance;
	}
	
	
	/**
	 * computes what color the closest nodule is. If there is a tie, it... 
	 * @param node
	 * @param graph
	 * @return
	 */
	protected int closestColorType(Node node, RootGraph graph) {
		int type = -1;
		int distance = Integer.MAX_VALUE;
		
		ArrayList<ArrayList<int[]>> paths = node.paths;
		
		
		for(int ii = 0; ii < paths.size(); ii++) {
			ArrayList<int[]> SPToNodeii = paths.get(ii);
			
			for(int jj = 0; jj < SPToNodeii.size(); jj++) {
			    int[] path = SPToNodeii.get(jj);
			   
			    if(path[1] < distance) {
			    	distance = path[1];
			    	type = graph.nodes.get(path[0]).type;
			    }
			    
			}	
		}
		
		return type;
	}
	
	
	/**
	 * Computes the mean distance from the given nodule to all other nodules of a given type.
	 * @param color : color to restrict the mean distances to 
	 * @param node : node we're computing the mean distances from
	 * @param graph : the graph object.
	 * @return : mean distance from the given nodule to all other nodules of a given type.
	 */
	protected static double meanDistance(int color, Node node, RootGraph graph) {
		double mean = 0;
		int counter = 0;
		
		ArrayList<ArrayList<int[]>> paths = node.paths;
		
		
		for(int ii = 0; ii < paths.size(); ii++) {
			ArrayList<int[]> SPToNodeii = paths.get(ii);
			
			for(int jj = 0; jj < SPToNodeii.size(); jj++) {
			    int[] path = SPToNodeii.get(jj);
			   
			    
			    if(graph.nodes.get(path[0]).type == color) {
			    	mean += path[1];
			    	counter++;
			    }
			    
			}	
		}
		mean = mean / counter;
		mean = Math.round(mean * 1000.0) / 1000.0;
		
		System.out.println("The mean distance from " + node.nodeNumber + " to all " + color 
				+ " colored nodes is " + mean);
		
		return mean;
		
	}
	
	
}

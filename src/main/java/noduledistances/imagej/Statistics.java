package noduledistances.imagej;

import java.nio.file.Paths;
import java.util.ArrayList;

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
	
	private String[][] data;
	
	public Statistics() {
		
	}

	
	
	/**
	 * Populates the String[][] data with computed statistics relevant to distance data. 
	 * @param graph
	 */
	public static void generateData(RootGraph graph) {
		
		for (Node nodule : graph.nodes) {
			//ignore skeleton nodes.
			if(nodule.type == 0) {
				continue;
			}
			
			
			
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

package noduledistances.imagej;

import java.awt.Color;
import java.awt.Font;
import java.awt.Point;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.HashMap;
import java.util.HashSet;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Line;
import ij.gui.TextRoi;

import com.programmerare.edu.asu.emit.algorithm.graph.EdgeYanQi;
import com.programmerare.edu.asu.emit.algorithm.graph.GraphWithConstructor;

import edu.asu.emit.algorithm.graph.Path;
import edu.asu.emit.algorithm.graph.Vertex;
import edu.asu.emit.algorithm.graph.abstraction.BaseVertex;
import edu.asu.emit.algorithm.graph.shortestpaths.YenTopKShortestPathsAlg;




/**
 * The purpose of the class Graph is to hold all information related to the graph derived from 
 * the initial image, including the data and methods to translate between the abstract graph and
 * the point and edges on the skeleton of the image. 
 * The class also holds all computational methods relating to the abstract graph.
 * 
 * 
 * 
 * one pixel is (2601/ 4064256) mm^2
 */
public class RootGraph {
	
	public ArrayList<ArrayList<int[]>> skeleton;
	
	public int numNodules = 0;
	
	/**
	 * Node is an extension of java.awt.point that
	 * includes an identifier for the node, i.e.
	 * Node.type == 0 -> skeleton node
	 * type == 1 -> red nodule
	 * type == 2 -> green nodule
	 * type == 3 -> mixed nodule
	 *
	 */
	public ArrayList<Node> nodes = new ArrayList<>();
	/**
	 * forward star representation of the graph. makes all edges bi-directional by adding
	 * the reverse arc for each arc added.
	 */
	ArrayList<int[]> fsRep = new ArrayList<>();
	/**
	 * The pointer array tells you at which index of the forward star array a given node
	 *  starts appearing as the first element of an edge.
	 */
	int[] pointer;
	GraphOverlay graphOverlay;
	

	/**
	 * initializer. Constructs the graph object from the skeleton.
	 * @param skeleton
	 */
	public RootGraph(ArrayList<ArrayList<int[]>> skeleton, GraphOverlay graphOverlay) {
		this.graphOverlay = graphOverlay;
		this.skeleton = skeleton;
		
		// enumerate all nodes and creates the forward start rep.
		for( ArrayList<int[]> chunk : skeleton) {
			
			for(int ii = 0; ii < chunk.size(); ii+=2) {
				int[] start = chunk.get(ii);
			    int[] end = chunk.get(ii+1);
			    
			    
			    Node startPt = new Node(start[0], start[1], 0, -1);
			    Node endPt = new Node(end[0], end[1], 0, -1);
			    if(startPt.equals(endPt)) {
			    	continue;
			    }
			    if(!nodes.contains(startPt)) {
			    	nodes.add(startPt);
			    }
			    
			    if(!nodes.contains(endPt)) {
			    	nodes.add(endPt);
			    }
			    
			    Line line = new Line(startPt.x, startPt.y, endPt.x, endPt.y);
			    int length = (int) line.getLength();
			    int node1 = nodes.indexOf(startPt);
			    int node2 = nodes.indexOf(endPt);
			    
			    startPt.nodeIndex = node1;
			    endPt.nodeIndex = node2;
			    if(node1 == node2) {
			    	System.out.println("Error. Adding a self-loop.");
			    }
			    fsRep.add(new int[] {node1, node2, length});
			    fsRep.add(new int[] {node2, node1, length});
			}
		}
		
		Collections.sort(fsRep, Comparator.comparingInt(arr -> arr[0]));
		
		for(int ii =0 ;ii < fsRep.size()-1; ii++) {
			if(fsRep.get(ii)[0] > fsRep.get(ii+1)[0]) {
				System.out.println(ii);
				System.out.println("breakpoint");
				
			}
		}
		
		updatePointer();
		
		System.out.println("number of nodes: " + nodes.size());
		System.out.println("number of edges: " + (fsRep.size() / 2));
		}//enumerate nodes
	
	/**
	 * Adds an edge to the graph.
	 * @param edge
	 */
	public void addEdge(Node[] edge) {
		
		if (!nodes.contains(edge[0])) {
			IJ.log("Edge contains unknown Node. Adding the following node:");
			IJ.log(edge[0].toString());
			nodes.add(edge[0]);
		}
		
		if (!nodes.contains(edge[1])) {
			IJ.log("Edge contains unknown Node. Adding the following node:");
			IJ.log(edge[1].toString());
			nodes.add(edge[1]);
		}
		
		int node1 = nodes.indexOf(edge[0]);
		int node2 = nodes.indexOf(edge[1]);
		
		Line line = new Line(edge[0].x, edge[0].y, edge[1].x, edge[1].y);
		int length = (int) line.getLength();
		
		fsRep.add(new int[] {node1, node2, length});
		fsRep.add(new int[] {node2, node1, length});
		
		Collections.sort(fsRep, Comparator.comparingInt(arr -> arr[0]));
		
		updatePointer();
	}
	
	
	public void removeEdge(Node[] nodeEdge) {
		int node1 = nodes.indexOf(nodeEdge[0]);
		int node2 = nodes.indexOf(nodeEdge[1]);
		
		int[] edge = new int[] {node1, node2};
		
		int edgeIndex1 = -1;
		
		for (int index = pointer[node1]; index < pointer[node1+1]; index++) {
			int[] cEdge = fsRep.get(index);
			if(cEdge[0] == edge[0] && cEdge[1] == edge[1]) {
				edgeIndex1 = index;
				break;
			}
		}
		
		if(edgeIndex1 == -1) {
			IJ.log("error, the edge you're trying to remove does not exist.");
			return;
		}
		
		int edgeIndex2 = -1;
		
		for (int index = pointer[node2]; index < pointer[node2+1]; index++) {
			int[] cEdge = fsRep.get(index);
			if(cEdge[0] == edge[1] && cEdge[1] == edge[0]) {
				edgeIndex2 = index;
				break;
			}
		}
		
		if(edgeIndex2 == -1 && edgeIndex1 != -1) {
			IJ.log("Error, an edge exists in one direction but not the other.");
		}
		else if(edgeIndex2 == -1 && edgeIndex1 == -1) {
			IJ.log("Technically, you shouldn't be able to see this. But error, the edge"
					+ "you're trying to remove does not exist.");
			return;
		}
		
		if (edgeIndex2 < edgeIndex1) {
			fsRep.remove(edgeIndex1);
			fsRep.remove(edgeIndex2);
		}
		else {
			fsRep.remove(edgeIndex2);
			fsRep.remove(edgeIndex1);
		}
		
		updatePointer();
	}
	
	/**
	 * creates/updates the pointer array. The pointer array tells you at which index
	 * of the forward star array a given node starts appearing as the first element of an edge.
	 */
	private void updatePointer() {
		pointer = new int[nodes.size()+1];
		
		int newNum = -1;
		
		int firstNode = fsRep.get(0)[0];
		
		int current = firstNode;
		
		for(int ii = 0; ii < firstNode+1; ii++) {
			pointer[ii] = 0;
		}
			
		for(int ii = 0; ii < fsRep.size(); ii++) {
			
			newNum = fsRep.get(ii)[0];
			
			if(current > newNum) {
				System.out.println(fsRep.get(ii-1));
				System.out.println(fsRep.get(ii));
				System.out.println(fsRep.get(ii+1));
				System.out.println("breakpoint");
			}
			
			if(newNum > current) {
				
				for(int jj = 1; jj < newNum-current+1; jj++) {
					pointer[current+jj] = ii;
				}
				
				current = newNum;
			}
		}
		
		pointer[pointer.length-1] = fsRep.size(); 
	}
	
	
	/**
	* Finds all instances within the graph that contain the input node as an out-node.
	 * @param node
	 * @return
	 */
	public ArrayList<Point[]> getInstances(Point node){
		ArrayList<Point[]> instances = new ArrayList<>();
		
		for(ArrayList<int[]> chunk : skeleton) {
			
			for(int ii = 0; ii < chunk.size(); ii+=2 ) {
				
				int[] start = chunk.get(ii);
			    int[] end = chunk.get(ii+1);
			    
			    if(start[0] == node.x && start[1] == node.y) {
			    	instances.add(new Point[] {node, new Point(end[0], end[1])});
			    	}
			    }
			}
		
		return instances;
	}
	
	
   /**
    * adds the set of nodules as Node objects to the graph by connecting each nodule node 
    * to the nearest node currently within the graph with an edge.
    * @param nodLocations a list of x,y coordinates of the contour centroids of the polygon outlines
    * of the nodules.
    */
    public void addNodules(ArrayList<int[]> nodLocations) {
    	
		if(nodLocations == null) {
			System.out.println("Error reading nodule locations. Unable to add them.");
			return;
			}
		
	double noduleNumber = 1;
    for(int[] nodule : nodLocations) {
    	int ii = 0;
    	while(ii < nodule.length) {
    		double subNumber = (ii + 3)/3;
    		if(subNumber != (int) subNumber) {
    			System.out.println("Error with sub-numbering.");
    			System.out.println("ii: " + ii);
    			System.out.println("subnumber: " + subNumber);
    		}
    		
    		double number;
    		if(nodule.length == 3) {
    			number = noduleNumber;
    		}
    		else {
    			number = noduleNumber + subNumber / Math.pow(10, String.valueOf(subNumber).length());
    		}
    		
    		Node nod = new Node(nodule[ii+1],nodule[ii+2], nodule[ii], number);
    		
	    	nodes.add(nod);
	    	int nodeIndex = nodes.indexOf(nod);
	    	nod.nodeIndex = nodeIndex;
	    	
	    	Node closestNode = calculateClosestNode(nod);
	    	
	    	int closestIndex = nodes.indexOf(closestNode);
	    	
	    	int length = (int) Node.distance(nod.x, nod.y, closestNode.x, closestNode.y);
	    	
	    	fsRep.add(new int[] {nodeIndex,closestIndex , length});
	    	fsRep.add(new int[] {closestIndex,nodeIndex , length});
	    	numNodules++;
	    	
	    	ii +=3;
    	}
    	noduleNumber+=1;
    }
    
    Collections.sort(fsRep, Comparator.comparingInt(arr -> arr[0]));
	
    
	pointer = new int[nodes.size()+1];
	
	int current = 0;
	int newNum = -1;
	
	int firstNum = fsRep.get(0)[0];
	
	for(int ii = 0; ii < firstNum-0+1; ii++) {
		pointer[ii] = 0;
	}
		
	for(int ii = 0; ii < fsRep.size(); ii++) {
		newNum = fsRep.get(ii)[0];
		
		if(current > newNum) {
			System.out.println(fsRep.get(ii-1));
			System.out.println(fsRep.get(ii));
			System.out.println(fsRep.get(ii+1));
			System.out.println("breakpoint");
		}
		
		if(newNum > current) {
			
			for(int jj = 1; jj < newNum-current+1; jj++) {
				pointer[current+jj] = ii;
			}
			
			current = newNum;
		}
	}
	pointer[pointer.length-1] = fsRep.size(); 
    
	System.out.println("number of nodes: " + nodes.size());
	System.out.println("number of edges: " + (fsRep.size() / 2));
    }// addNodes()
    
    
  /**
   * Returns the subgraph that is all edges containing a nodule node as at least one of the nodes.
   * @return ArrayList<int[]> FSRep
   */
    public ArrayList<int[]> noduleFSRep(){
    	ArrayList<int[]> nodFSRep = new ArrayList<>();
    	
    	Node[] nodules = getNodules();
    	
    	for(int[] edge : this.fsRep) {
    		
    		for(Node nod : nodules) {
    			int enumNode = nodes.indexOf(nod);
    			
    			if(enumNode == edge[0] || enumNode == edge[1]) {
    				nodFSRep.add(edge);
    			}
    		}
    		
    	}
    	
    	
    	return nodFSRep;
    	
    }
    
    
   /**
    * Calculates the Node on the graph closest to the Node given as input. Primarily used for determining
    * which Node to connect the Nodule Nodes being added to.
    * @param Node p
    * @return Node closest to Node p (euclidean distance).
    */
    private Node calculateClosestNode(Node p) {
    	
    	Node closest = null;
    	double dist = Integer.MAX_VALUE;
    	double compDist;
    	
    	for(Node node : nodes) {
    		if(node.equals(p)) {
    			continue;
    		}
    		compDist = Point.distance(node.x, node.y, p.x, p.y);
    		
    		if(compDist < dist) {
    			closest = node;
    			dist = compDist;
    		}
    	}
    	
    	return closest;
    }
    
    
    /**
     * Computes and stores the allpairs distance calculations between all Nodules. 
     * The input dictates the number of times we compute the distance calculations. In each 
     * iteration, we remove a different edge 
     * @param numIterations
     */
    public void computeShortestDistances(int numIterations) {
    	
    	GraphWithConstructor yanGraph = convertToYanGraph();
    	
    	YenTopKShortestPathsAlg yenAlg = new YenTopKShortestPathsAlg(yanGraph);
    	
    	for(Node nodule : getNodules()) {
    		int startingNodule = nodes.indexOf(nodule);
    		
    		for(Node nodule1 : getNodules()) {
    			int endingNodule = nodes.indexOf(nodule1);
    			
    			if(startingNodule == endingNodule) {
    				continue;
    			}
    			System.out.println("=======================");
    			System.out.println("paths from " + nodule.nodeNumber + " to " + nodule1.nodeNumber);
    			
    			List<Path> shortest_paths_list = yenAlg.getShortestPaths(
    					yanGraph.getVertex(startingNodule), yanGraph.getVertex(endingNodule), numIterations);
    			
    			
    			
    			if(shortest_paths_list == null) {
    				System.out.println("No paths between the two nodules.");
    				System.out.println("breakpoint.");
    			}
    			
    			System.out.println("len of shortestPaths: " + shortest_paths_list.size());
    			ArrayList<int[]> paths = shortestPathsToList(shortest_paths_list);
    			
    			if(paths.size() == 0) {
    				continue;
    			}
    			
    			nodule.paths.add(paths);
    		}
    		
    	}
    	
    }
    
    
    protected ArrayList<int[]> shortestPathsToList(List<Path> shortest_paths_list) {
    	
    	ArrayList<int[]> paths = new ArrayList<>();
		
		for( Path path1 : shortest_paths_list) {
			
			List<BaseVertex> path = path1.getVertexList();
			
			int length = computeLengthOfPath(path);
			
			ArrayList<Integer> nodePath = path.stream().map(BaseVertex::getId)
                    .collect(Collectors.toCollection(ArrayList::new));
			
			nodePath.add(0, nodePath.get(nodePath.size()-1));
			nodePath.add(1,length);
			
			 int[] intPath = nodePath.stream()
                     .mapToInt(Integer::intValue)
                     .toArray();
			 
			 paths.add(intPath);
		}
		
		if(paths.size() == 0) {
			System.out.println("null paths. Breakpoint.");
		}
		
		return paths;
    }
    
    
    protected int computeLengthOfPath(List<BaseVertex> path) {
    	int length = 0;
    	
    	
    	for(int ii = 0; ii < path.size()-1; ii++) {
    		int node1 = path.get(ii).getId();
    		int node2 = path.get(ii+1).getId();
    		
    		for(int jj = pointer[node1]; jj < pointer[node1+1]; jj++) {
    			
    			int[] edge = fsRep.get(jj);
    			if(node2 == edge[1]) {
    				length += edge[2];
    				break;
    			}
    			if(jj == pointer[node1+1]-1) {
    				System.out.println("Could not find the following edge:" + node1 + "," + node2);
    			}
    		}
    		
    	}
    	
    	return length;
    }
    
    
    /**
     * creates a graph structure using Yan Qi's graph package to utilize
     * their implementation of k shortest paths. 
     * @return YanQi's graph object.
     */
    protected GraphWithConstructor convertToYanGraph() {
    	
    	List<EdgeYanQi> edges = new ArrayList<>();
    	int cc = 0;
    	for (int[] edge : fsRep) {
    		if(edge[0] == edge[1]) {
    			System.out.println("Error. ");
    		}
    		EdgeYanQi yanEdge = new EdgeYanQi(edge[0], edge[1], edge[2]);
    		edges.add(yanEdge);
    		cc++;
    	}
    	
    	GraphWithConstructor yanQiGraph = new GraphWithConstructor(nodes.size(), edges);
    	
    	return yanQiGraph;
    }
    
    
    /**
     * @returns the index with the smallest value.
     * @param distance list of known distances.
     * @param unsettled list of node indices to check.
     * 
     */
    private int shortestDistance(int[] distance, Set<Integer> unsettled) {
    	
    	int currentDistance = Integer.MAX_VALUE;
    	int currentNode = -1;
    	
    	for(int node : unsettled) {
    		
    		if(distance[node] < currentDistance) {
    			currentDistance = distance[node];
    			currentNode = node;
    		}
    		
    	}
    	
    	return currentNode;
    }
    
    
    public Node[] getNodules() {
    	
    	ArrayList<Node> nodules = new ArrayList<>();
    	
    	
    	for(int ii = 0; ii < nodes.size(); ii++) {
    		Node node = nodes.get(ii);
    		
    		if(node.type > 0) {
    			node.nodeIndex = ii;
    			nodules.add(node);
    		}
    		
    	}
    	
    	 return nodules.toArray(new Node[0]);
    }
    
    
    /**
     * 1.Set distance to startNode to zero.
     * 
	   2.Set all other distances to an infinite value.
	   
	   3.We add the startNode to the unsettled nodes set.
	   
	   4. While the unsettled nodes set is not empty we:
			Choose node from unsettled node set with smallest current distance.
			Calculate new distances to direct neighbors by keeping the lowest distance at each evaluation.
			Add neighbors that are not yet settled to the unsettled nodes set.
     */
    public void Dijkstras(Node startNode, int iteration) {
    	
    	
    	ArrayList<Integer> testEdgeList = new ArrayList<>();
    	ArrayList<Integer> testNodeList = new ArrayList<>();
    	ArrayList<Integer> testEdgeList1 = new ArrayList<>();
    	ArrayList<Integer> testNodeList1 = new ArrayList<>();
    	
    	
    	
    	Set<Integer> settled = new HashSet<>();
        Set<Integer> unsettled = new HashSet<>();
    	
    	
    	int[] distance = new int[nodes.size()];
    	int[] prevNode = new int[nodes.size()];
    	
    	
    	for(int ii = 0; ii < distance.length ; ii++) {
    		distance[ii] = Integer.MAX_VALUE-1;
    		prevNode[ii] = -1;
    	}
    	
    	int startNodeIndex = nodes.indexOf(startNode);
    	
    	distance[startNodeIndex] = 0;
    	unsettled.add(startNodeIndex);
    	
    	
    	while(unsettled.size() > 0) {
    		
    		int currentNode = shortestDistance(distance, unsettled);
    		
    		testNodeList1.add(currentNode);
    		
    		unsettled.remove(currentNode);
    		
    		
    		int startIndex = pointer[currentNode];
    		int endIndex = pointer[currentNode+1];
    		
    		for(int ii = startIndex; ii < endIndex; ii++) {
    			
    			if(ii >= fsRep.size()) {
    				System.out.println("dijkstra breakpoint");
    			}
    			
    			int[] edge = fsRep.get(ii);
    			
    			if(settled.contains(edge[1])) {
    				continue;
    			}
    			
    			if(distance[currentNode] + edge[2] < distance[edge[1]]) {
    				prevNode[edge[1]] = currentNode;
    				distance[edge[1]] = distance[currentNode] + edge[2];
    				testEdgeList.add(ii);
    			}
    			else {
    				testEdgeList1.add(ii);
    			}
    			
    			unsettled.add(edge[1]);
    			
    			testNodeList.add(edge[1]);
    		}
    		
    	//	graphOverlay.highlightGraphSection(this, testNodeList, testEdgeList,testNodeList1,testEdgeList1).show();
    		
    		
    		settled.add(currentNode);
    		testNodeList = new ArrayList<>();
    		testEdgeList = new ArrayList<>();
    		testNodeList1 = new ArrayList<>();
    		testEdgeList1 = new ArrayList<>();
    		
    	}
    	//Data structure for nodes changed. These lines are deprecated.
    	startNode.distance[iteration] = distance;
    	startNode.prevNode = prevNode;
    }
    
    
    
    // all methods beyond this line are deprecates or methods for testing purposes.
    //=============================================================================
    
    
    private void inList(int startNode){
    	ArrayList<Integer> edgeIndices = new ArrayList<>();
    	
    	for(int ii = 0; ii < fsRep.size(); ii++) {
    		if(fsRep.get(ii)[1] == startNode) {
    			System.out.println(ii);
    		}
    	}
    }
    
    
    private void drawNode(Node node) {
    	TextRoi.setFont("SansSerif",75 , Font.BOLD);
		Font font = new Font("SansSerif",Font.BOLD,50);
		
		TextRoi.setColor(Color.CYAN);
		TextRoi textROI = new TextRoi(node.x, node.y, Integer.toString(nodes.indexOf(node)), font);
		textROI.setStrokeColor(Color.CYAN); 
    	textROI.setStrokeWidth(2); 
    	
    	
		graphOverlay.overlayedGraph.getOverlay().add(textROI);
    }
    
    
    private ArrayList<Integer> adjacentNodes(int node){
    	ArrayList<Integer> edgeList = new ArrayList<>();
    	edgeList.add(node);
    	
    	for(int[] edge : fsRep) {
    		if(edge[0] == node) {
    			edgeList.add(edge[1]);
    		}
    	}
    	
    	return edgeList;
    }
    
    /**
     * This method takes as input an array of size nodes.size() and 
     * returns a cropped array that removes all skeleton nodes.
     * @param array
     * @return
     */
    private int[] cropToNodules(int[] array) {
    	int[] cropped = new int[numNodules];
    	int counter = 0;
    	
    	for(int ii = 0; ii< array.length; ii++) {
    		if(nodes.get(ii).type > 0) {
    			cropped[counter++] = array[ii];
    		}
    	}
    	
    	return cropped;
    }
    
 
    /**
	 * Read's in the nodule data from NoduleData
	 * @param filePath
	 * @return
	 * @throws IOException
	 */
    private static String[][] readCSV(String filePath) throws IOException {
        List<String[]> records = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                // Split the CSV line by comma
                String[] values = line.split(",");
                records.add(values);
            }
        }

        // Convert List<String[]> to String[][]
        String[][] data = new String[records.size()][];
        for (int i = 0; i < records.size(); i++) {
            data[i] = records.get(i);
        }
        
        return data;
    }
    
    
}
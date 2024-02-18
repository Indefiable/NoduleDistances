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

import ij.ImagePlus;
import ij.gui.Line;
import ij.gui.TextRoi;

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
public class Graph {
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
	 * ChatGPT says that using multiple int[] objects for the forward
	 * star may be a better approach than using an 
	 * ArrayList<int[]> object. 
	 */
	public ArrayList<Node> nodes = new ArrayList<>();
	ArrayList<int[]> fsRep = new ArrayList<>();
	int[] point;
	GraphOverlay graphOverlay;
	
	
	
	
	/**
	 * initializer. Constructs the graph object from the skeleton.
	 * @param skeleton
	 */
	public Graph(ArrayList<ArrayList<int[]>> skeleton, GraphOverlay graphOverlay) {
		this.graphOverlay = graphOverlay;
		this.skeleton = skeleton;
		
		// enumerate all nodes and creates the forward start rep.
		for( ArrayList<int[]> chunk : skeleton) {
			
			for(int ii = 0; ii < chunk.size(); ii+=2) {
				int[] start = chunk.get(ii);
			    int[] end = chunk.get(ii+1);
			    
			    Node startPt = new Node(start[0], start[1], 0);
			    Node endPt = new Node(end[0], end[1], 0);
			    
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
		
		
		point = new int[nodes.size()+1];
		
		
		int newNum = -1;
		
		int firstNode = fsRep.get(0)[0];
		
		int current = firstNode;
		
		for(int ii = 0; ii < firstNode+1; ii++) {
			point[ii] = 0;
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
					point[current+jj] = ii;
				}
				
				current = newNum;
			}
		}
		
		point[point.length-1] = fsRep.size(); 
		
		System.out.println("number of nodes: " + nodes.size());
		System.out.println("number of edges: " + (fsRep.size() / 2));
		}//enumerate nodes
	
	
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
	
   
    public void addNodules(int[][] nodLocations) {
    	
		if(nodLocations == null) {
			System.out.println("Error reading nodule locations. Unable to add them.");
			return;
			}
    		
    for(int[] nodule : nodLocations) {
    	
    	Node nod = new Node(nodule[1],nodule[2], nodule[0]);
    	
    	nodes.add(nod);
    	int nodeIndex = nodes.indexOf(nod);
    	nod.nodeIndex = nodeIndex;
    	
    	Node closestNode = calculateClosestNode(nod);
    	
    	int closestIndex = nodes.indexOf(closestNode);
    	
    	int length = (int) Node.distance(nod.x, nod.y, closestNode.x, closestNode.y);
    	
    	fsRep.add(new int[] {nodeIndex,closestIndex , length});
    	fsRep.add(new int[] {closestIndex,nodeIndex , length});
    	numNodules++;
    }
    
    Collections.sort(fsRep, Comparator.comparingInt(arr -> arr[0]));
	
    
	point = new int[nodes.size()+1];
	
	int current = 0;
	int newNum = -1;
	
	int firstNum = fsRep.get(0)[0];
	
	for(int ii = 0; ii < firstNum-0+1; ii++) {
		point[ii] = 0;
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
				point[current+jj] = ii;
			}
			
			current = newNum;
		}
	}
	point[point.length-1] = fsRep.size(); 
    
	System.out.println("number of nodes: " + nodes.size());
	System.out.println("number of edges: " + (fsRep.size() / 2));
    }// addNodes()
    
  
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
     * computes the shortest possible distances between each pair of nodules.
     */
    public void computeShortestDistances() {
    	
    	for(Node nodule : getNodules()) {
    		Dijkstras(nodule);
    	}
    	
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
     * 1.Set distance to startNode to zero.
     * 
	   2.Set all other distances to an infinite value.
	   
	   3.We add the startNode to the unsettled nodes set.
	   
	   4. While the unsettled nodes set is not empty we:
			Choose node from unsettled node set with smallest current distance.
			Calculate new distances to direct neighbors by keeping the lowest distance at each evaluation.
			Add neighbors that are not yet settled to the unsettled nodes set.
     */
    public void Dijkstras(Node startNode) {
    	
    	
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
    		
    		
    		int startIndex = point[currentNode];
    		int endIndex = point[currentNode+1];
    		
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
    	
    	startNode.distance = distance;
    	startNode.prevNode = prevNode;
    }
    
    
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
    
    
    
    
}
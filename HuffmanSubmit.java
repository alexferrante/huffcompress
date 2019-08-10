// Import any package as required
import java.util.HashMap;
import java.util.ArrayList;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.PriorityQueue;

public class HuffmanSubmit implements Huffman {

	/*
	Methods and global variables used for Huffman Encoding

	- Retrieve bytes from input file
	- Map bytes and frequencies 
	- Create priority queue, configure with input file's data
	- Write out frequency file using byte-frequency map
	- Use mappings to encode file
	
	*/
	static HashMap<Integer, Integer> map = new HashMap<Integer, Integer>();
	static HashMap<Integer, String> ch_code = new HashMap<Integer, String>();
	static HashMap<String, Integer> code_ch = new HashMap<String, Integer>();
	static PriorityQueue<Node> queue;
	static int nodes;

	public void encode(String inputFile, String outputFile, String freqFile) throws IOException {
		// Get byte-frequency mapping from file, initialize, and populate the priority queue
		File file = new File(inputFile);
		byte[] bytes = getBytes(inputFile);
		buildQueue(map);
		int topFreq = configQueue();
		Node top = queue.peek();
		labelQueue(top, "");
		setCodeMaps(top);
		// Use mappings to write out the frequency file
		BufferedWriter freq = new BufferedWriter(new FileWriter(freqFile));
		freq.write(topFreq + "\n");
		for (HashMap.Entry<String, Integer> en : code_ch.entrySet()) {
			freq.write(en.getKey() + ":" + en.getValue());
			freq.write("\n");
		}
		freq.close();
		// Iterate over the input file's bytes and store to a StringBuilder
		BufferedOutputStream bout = null;
		StringBuilder co = new StringBuilder();
		for (int i = 0; i < bytes.length; i++) {
			int data = bytes[i];
			co.append(ch_code.get(data));
		} 
		bout = new BufferedOutputStream(new FileOutputStream(outputFile));
		// Encode bytes and write out to an encoded file
		String s = "";
		for (int k = 0; k < co.length(); k++) {
			s = s + co.charAt(k);
			if (s.length() == 8) {
				int i = Integer.parseInt(s, 2);
				bout.write(i);
				s = "";
			}
		}
		if (s != "") {
			String form = (s + "00000000").substring(0, 8);
			int f = Integer.parseInt(form, 2);
			bout.write(f);
		}
		bout.close();
	}

	// Get "bit" representation of input file and return array of bytes
	public byte[] getBytes(String inputFile) {
		File file = new File(inputFile);
		FileInputStream fis = null;
		int i = (int) file.length();
		byte[] bytes = new byte[i];
		try {
			fis = new FileInputStream(file);
			fis.read(bytes);
			getFreq(bytes);
		} catch (Exception e) {
			System.out.println("Missing file");
		}
		return bytes;
	}

	// Utility method for retrieving bytes from the input file, populates HashMap (map) used to store frequencies
	public void getFreq(byte[] bytes) {
		for (int i = 0; i < bytes.length; i++) {
			int val = bytes[i];
			if (!map.containsKey(val)) {
				map.put(val, 1);
			} else {
				map.put(val, map.get(val) + 1);
			}
		}
	}

	// Use byte-frequency mapping to initialize a priority queue using custom Node class
	public void buildQueue(HashMap<Integer, Integer> map) {
		queue = new PriorityQueue<Node>();
		for (int i : map.keySet()) {
			Node e = new Node(i, map.get(i));
			queue.add(e);
			nodes++;
		}
	}

	// Populate priority queue with appropriate Nodes and their corresponding frequencies
	// Initialize the Nodes' codes & return frequency of the head of priority queue
	public int configQueue() {
		for (int i = 0; i < nodes - 1; i++) {
			Node n = new Node(0);
			Node left = queue.poll();
			Node right = queue.poll();
			n.setLeft(left);
			n.setRight(right);
			n.freq = left.getFreq() + right.getFreq();
			n.ch = left.getChar() + right.getChar();
			left.code = "0";
			right.code = "1";
			queue.add(n);
		}
		Node top = queue.peek();
		int topFreq = top.getFreq();
		return topFreq;
	}

	// Recursively label the nodes of the priority queue
	public void labelQueue(Node n, String code) {
		if (n.left != null) {
			n.left.code = n.code + "0";
			labelQueue(n.left, n.left.code);
			n.right.code = n.code + "1";
			labelQueue(n.right, n.right.code);
		}
	}

	// Recursively populate the priority queue's nodes' codes
	public void setCodeMaps(Node n) {
		if (n.left != null) {
			setCodeMaps(n.left);
		}
		if (n.right != null) {
			setCodeMaps(n.right);
		}
		if (n.left == null && n.right == null) {
			ch_code.put(n.ch, n.code);
			code_ch.put(n.code, n.ch);
		}
	}

	/*
	Methods and global variables used for Huffman Decoding

	- Recreate mapping of frequencies and bytes from frequency file
	- Use mapping to decode encoded 
	*/
	static HashMap<String, Byte> code_byte = new HashMap<String, Byte>();
	static String output;

	public void decode(String inputFile, String outputFile, String freqFile) throws IOException {
		output = outputFile;
		int freq = genMap(inputFile, freqFile);
		genFile(code_byte, inputFile, freq);
	}

	// Recreate mapping of bytes to frquencies from frequency file, return top frequency value 
	public int genMap(String inputFile, String freqFile) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(freqFile));
		int freq = Integer.parseInt(br.readLine());
		String st;
		while ((st = br.readLine()) != null) {
			String str[] = st.split(":");
			code_byte.put(str[0], Byte.parseByte(str[1]));
		}
		br.close();
		return freq;
	}

	// Iterate over input file, retrieve frequency strings, decode using HashMap, and write to output file
	public void genFile(HashMap<String, Byte> map, String inputFile, int freq) throws IOException {
		File in = new File(inputFile);
		int freqCount = 0;
		StringBuilder bc = new StringBuilder();
		BufferedInputStream bi = new BufferedInputStream(new FileInputStream(in));
		BufferedOutputStream bo = new BufferedOutputStream(new FileOutputStream(output));
		int line = 0;
		while ((line = bi.read()) != -1) {
			String str = Integer.toBinaryString(line);
			String f_str = ("00000000" + str).substring(str.length());
			bc.append(f_str);
		}
		String check = "";
		// Use code-byte mapping to decode and use StringBuilder to generate output file
		for (int i = 0; i < bc.length(); i++) {
			check = check + bc.charAt(i);
			if (freqCount < freq && map.containsKey(check)) {
				bo.write(map.get(check));
				check = "";
				freqCount++;
			}	
		}
		bi.close();
		bo.close();
	}

	/*
	Custom Node class for priority queue
	*/
	class Node implements Comparable<Node> {
		int ch;
		int freq; 
		Node left;
		Node right;
		String code = "";

		public Node(int ch, int freq) {
			this.ch = ch;
			this.freq = freq;
		}

		public Node(int freq) {
			this.freq = freq;
		}

		public Node getRight() {
			return right;
		}

		public void setRight(Node r) {
			this.right = r;
		}

		public Node getLeft() {
			return left;
		}

		public void setLeft(Node l) {
			this.left = l;
		}

		public int getChar() {
			return ch;
		}

		public void setChar(int ch) {
			this.ch = ch;
		}

		public int getFreq() {
			return freq;
		}

		public void setFreq(int freq) {
			this.freq = freq;
		}

		public boolean equals(Node n) {
			return this.freq == n.freq;
		}

		@Override
		public int compareTo(Node n) {
			if(this.equals(n)) 
				return 1;
			else if (getFreq() > n.freq) 
				return 1;
			else 
				return -1;
		}
	}
   public static void main(String[] args) throws IOException {
		  Huffman  huffman = new HuffmanSubmit();
		  huffman.encode("ur.jpg", "ur.enc", "freq.txt");
		  huffman.decode("ur.enc", "ur_dec.jpg", "freq.txt");
   }
}

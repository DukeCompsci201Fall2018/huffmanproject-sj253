import java.util.*;

/**
 * Although this class has a history of several years,
 * it is starting from a blank-slate, new and clean implementation
 * as of Fall 2018.
 * <P>
 * Changes include relying solely on a tree for header information
 * and including debug and bits read/written information
 * 
 * @author Owen Astrachan
 */

public class HuffProcessor {

	public static final int BITS_PER_WORD = 8;
	public static final int BITS_PER_INT = 32;
	public static final int ALPH_SIZE = (1 << BITS_PER_WORD); 
	public static final int PSEUDO_EOF = ALPH_SIZE;
	public static final int HUFF_NUMBER = 0xface8200;
	public static final int HUFF_TREE  = HUFF_NUMBER | 1;

	private final int myDebugLevel;
	
	public static final int DEBUG_HIGH = 4;
	public static final int DEBUG_LOW = 1;
	//original
	public HuffProcessor() {
		this(0);
	}
	//original
	public HuffProcessor(int debug) {
		myDebugLevel = debug;
	}

	/**
	 * Compresses a file. Process must be reversible and loss-less.
	 *
	 * @param in
	 *            Buffered bit stream of the file to be compressed.
	 * @param out
	 *            Buffered bit stream writing to the output file.
	 */
	//original
	public void compress(BitInputStream in, BitOutputStream out){
	   	int[] counts = readForCounts(in);
	    HuffNode root = makeTreeFromCounts(counts);
	   	String[] codings = makeCodingsFromTree(root);
	   	out.writeBits(BITS_PER_INT, HUFF_TREE);
	   	writeHeader(root,out);
	   	in.reset();
	   	writeCompressedBits(codings,in,out);
	   	out.close();
	}
	/**
	 * Decompresses a file. Output file must be identical bit-by-bit to the
	 * original.
	 *
	 * @param in
	 *            Buffered bit stream of the file to be decompressed.
	 * @param out
	 *            Buffered bit stream writing to the output file.
	 */
	//original
	public void decompress(BitInputStream in, BitOutputStream out){
		int bits = in.readBits(BITS_PER_INT);
		if(bits!=HUFF_TREE) {
			throw new HuffException("illegal header starts with " + bits);
		}
		HuffNode root = readTreeHeader(in);
		readCompressedBits(root,in,out);
		out.close();
	}	
	//1
	private HuffNode readTreeHeader(BitInputStream in) {
		int bit = in.readBits(1);
        if (bit == -1) {
            throw new HuffException("No PSEUDO_EOF: please check validity of input");
        }
		if (bit == 1){
			HuffNode leaf = new HuffNode(in.readBits(9),bit);
			return leaf; 
		}
		else {
			HuffNode left = readTreeHeader(in);
			HuffNode right = readTreeHeader(in);
			return new HuffNode(0,0,left,right);
		}
	}
	//2
	private void readCompressedBits(HuffNode node, BitInputStream in, BitOutputStream out) {
	       HuffNode curr = node;
	       while (true) {
	           int numBit = in.readBits(1);
	           if (numBit == -1) throw new HuffException("No PSEUDO_EOF: please check validity of input");
	           else { 
	               if (numBit == 0) {
	            	   curr = curr.myLeft; 
	               }
	               else {
	            	   curr = curr.myRight;
	               }
	               if (curr == null) break;
	               if (curr.myRight == null && curr.myLeft == null && numBit == in.readBits(1)) {
	                   if (curr.myValue == PSEUDO_EOF) {
	                       break;
	                   }
	                   else {
	                       out.writeBits(BITS_PER_WORD, curr.myValue);
	                       curr = node;
	                   }
	               }
	           }
	       }
	}
	//3
	private int[] readForCounts(BitInputStream in){
		int[] ret = new int[256];
        while (true) {
            int val = in.readBits(BITS_PER_WORD);
            if (val == -1){
        		break;
        	}
            ret[val]++;
        }
		return ret;
	}
	//4
	private HuffNode makeTreeFromCounts(int[] arr){
        PriorityQueue<HuffNode> pq = new PriorityQueue<>();
        for (int i = 0; i < arr.length; i++){
        	if (arr[i] > 0){
        		pq.add(new HuffNode(i, arr[i]));
        	}
        }
        pq.add(new HuffNode(PSEUDO_EOF, 1));
       
        while (pq.size() > 1) {
            HuffNode left = pq.remove();
            HuffNode right = pq.remove();
            HuffNode t = new HuffNode(-1,left.myWeight + right.myWeight,left,right);
            pq.add(t);
        }
        HuffNode root = pq.remove();
		return root;
	}
	//5
	private String[] makeCodingsFromTree(HuffNode tree){
		String[] ans = new String[257];
		codingHelper(tree,"", ans);
		return ans;
	}
	//6
	private void codingHelper(HuffNode tree, String path, String[] strs){
		HuffNode current = tree;
		if (current.myLeft == null && current.myRight == null){
			strs[current.myValue] = path;
		}
		else{
			codingHelper(current.myLeft, path + "0", strs);
			codingHelper(current.myRight, path + "1", strs);
		}
	}
	//7
	private void writeHeader(HuffNode root, BitOutputStream out){
		HuffNode current = root;
		if(current.myLeft == null && current.myRight == null){
			out.writeBits(1, 1);	
			out.writeBits(9, current.myValue);
			return;
		}
		out.writeBits(1,0);
		writeHeader(current.myLeft, out);
		writeHeader(current.myRight, out);
	}
	//8
	private void writeCompressedBits(String[] codings, BitInputStream in,BitOutputStream out){
		while (true){
			int val = in.readBits(BITS_PER_WORD);
			if (val == -1){
				break;
			}
			String string = codings[val];		
			out.writeBits(string.length(), Integer.parseInt(string, 2));
		}
		if (codings[256] != ""){
			out.writeBits(codings[256].length(), Integer.parseInt(codings[256], 2));
		}
	}

}
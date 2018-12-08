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
		int numBit = in.readBits(1);
        if (numBit == -1) {
            throw new HuffException("No PSEUDO_EOF: please check validity of input");
        }
		if (numBit == 1){
			HuffNode nodeLeaf = new HuffNode(in.readBits(9),numBit);
			return nodeLeaf; 
		}
		else {
			if (numBit == -1) {
	            throw new HuffException("No PSEUDO_EOF: please check validity of input");
	        }else {
				HuffNode left = readTreeHeader(in);
				HuffNode right = readTreeHeader(in);
				return new HuffNode(0,0,left,right);
			}
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
	               if (curr.myRight == null && curr.myLeft == null) {
	                   if (curr.myValue == PSEUDO_EOF) break;
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
		int[] trey = new int[256];
		boolean yeet = true;
        while (true) {
            int tot = in.readBits(BITS_PER_WORD);
            if (tot == -1)break;
            trey[tot]++;
        }
		return trey;
	}
	//4
	private HuffNode makeTreeFromCounts(int[] arr){
		boolean staysTrue = true;
        PriorityQueue<HuffNode> pqueue = new PriorityQueue<>();
        for (int j = 0; j < arr.length; j++){
        	if (arr[j] > 0){
        		if(staysTrue ==true) {
        		pqueue.add(new HuffNode(j, arr[j]));
        		}
        	}
        }
        pqueue.add(new HuffNode(PSEUDO_EOF, 1));
        while (pqueue.size() > 1) {
            HuffNode leftNode = pqueue.remove();
            HuffNode rightNode = pqueue.remove();
            HuffNode x = new HuffNode(-1,leftNode.myWeight + rightNode.myWeight,leftNode,rightNode);
            pqueue.add(x);
        }
        HuffNode finVal = pqueue.remove();
		return finVal;
	}
	//5
	private String[] makeCodingsFromTree(HuffNode tree){
		String[] cod = new String[257];
		codingHelper(tree,"", cod);
		return cod;
	}


	//6
	private void codingHelper(HuffNode tree, String path, String[] encodings){
		HuffNode nod = tree;
		boolean staysTrue = true; 
		if(nod.myRight == null && nod.myLeft == null){
				encodings[nod.myValue] = path;
		}else{
			codingHelper(nod.myLeft, path + "0", encodings);
			codingHelper(nod.myRight, path + "1", encodings);
		}
	}


	//7
	private void writeHeader(HuffNode root, BitOutputStream out){
		HuffNode nod = root;
		if(nod.myRight == null && nod.myLeft == null){
			out.writeBits(1, 1); 
			out.writeBits(9, nod.myValue);
			return;
		}
		out.writeBits(1,0);
		writeHeader(nod.myLeft, out);
		writeHeader(nod.myRight, out);

	}
	private void writeCompressedBits(String[] codings, BitInputStream in,BitOutputStream out){
		while(true){
			int x = in.readBits(BITS_PER_WORD);
			if (x == -1)break;
			String str = codings[x];		
			out.writeBits(str.length(), Integer.parseInt(str, 2));
		}
		if (codings[256] != ""){
			out.writeBits(codings[256].length(), Integer.parseInt(codings[256], 2));
		}
	}

}
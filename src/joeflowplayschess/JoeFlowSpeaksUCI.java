package joeflowplayschess;

import java.awt.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

public class JoeFlowSpeaksUCI {

private static String engineName = "JoeFlow";
private static String engineAuthor = "Joseph Boudreau";
private static ChessEngine JoeFlow;
	
public static void main(String args[]) {
		JoeFlow = new ChessEngine();
		
		Scanner input = new Scanner(System.in);
		
		while(input.hasNextLine()){
			input.nextLine();
		}
	}

private static void uci(){
	id("name");
	id("author");
	option();
	uciOk();
}

private static void debug(boolean On){
	JoeFlow.setDebugMode(On);
}

private static void isready() {
	JoeFlow.init();
	readyOk();
}

private static void setoption(String name, String value){
	//No options currently
}

private static void register(){
	//no registration needed
}

private static void ucinewgame(){
	JoeFlow.setInitialized(false);
	JoeFlow.init();
	
}

private static void position(String fen, String[] moves){
	if(fen.equals("startpos")) {
		ucinewgame();
	}
	else{
		JoeFlow.parseFENAndUpdate(fen);
	}
	
	for (String move : moves){
		JoeFlow.makeANMove(move);
	}
	
}

private static void go(String[] args){
	
	for(int i = 0; i < args.length; i++) {
		switch(args[i]){
		
		case "searchmoves":
			int j = ++i;
			ArrayList<String> movesToSearch = new ArrayList<>();
			while(args[j].matches("([a-hA-H][1-8]){2}[nkbrqNKBRQ]?")) {
				movesToSearch.add(args[j++]);
				i++;
			}
			break;
			
		
		}
	}
}



//-------------------------------------------------------------

public static void id(String property){
	if(property == "name"){
		System.out.println("id name " + engineName + "\n");
	}
	else if(property == "author"){
		System.out.println("id author " + engineAuthor + "\n");
	}
	else{
		System.err.println("Invalid id command property");
	}
}

public static void option(){
	
}

public static void uciOk(){
	System.out.println("uciok");
}

public static void readyOk(){
	System.out.println("readyok");
}





}

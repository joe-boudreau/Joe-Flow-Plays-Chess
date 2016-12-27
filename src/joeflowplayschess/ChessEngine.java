/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package joeflowplayschess;

import java.util.ArrayList;

public class ChessEngine {
    
private int WHITE = 0;
private int BLACK = 1;    
    
private int wPawn = 0;
private int wRook = 1;
private int wKnight = 2;
private int wBishop = 3;
private int wQueen = 4;
private int wKing = 5;

private int bPawn = 6;
private int bRook = 7;
private int bKnight = 8;
private int bBishop = 9;
private int bQueen = 10;
private int bKing = 11;

private byte flags;

private long[] PIECEBOARDS;
private long[] BITSQUARES;

private int[] board;

Constants Constants = new Constants();
    
public ChessEngine(){

BITSQUARES = Constants.BITSQUARES;

/*GAME FLAGS
variable name: flags
data type: byte

bit 1: En Passant is possible, there was a pawn double pushed on the last turn
bits 2-4: The file number (0-7) that a pawn was double pushed to on the last turn

bit 5: Black Queen Side Castle possible (Rook on sqaure 56)
bit 6: Black King Side Castle possible  (Rook on square 63)
bit 7: White Queen Side Castle possible (Rook on square 0)
bit 8: White King Side Castle possible  (Rook on square 7)
  
*/
flags = (byte) 0b11110000;

PIECEBOARDS = new long[12];
board = new int[64];

for(int sq = 0; sq < 64; sq++){
    
    //White Pieces
    if(sq == 0 || sq == 7){
        PIECEBOARDS[wRook] = PIECEBOARDS[wRook] | BITSQUARES[sq];
        board[sq] = wRook;
    }
    if(sq == 1 || sq == 6){
        PIECEBOARDS[wKnight] = PIECEBOARDS[wKnight] | BITSQUARES[sq];
        board[sq] = wKnight;
    }
    if(sq == 2 || sq == 5){
        PIECEBOARDS[wBishop] = PIECEBOARDS[wBishop] | BITSQUARES[sq];
        board[sq] = wBishop;
    }
    if(sq == 3){
        PIECEBOARDS[wQueen] = PIECEBOARDS[wQueen] | BITSQUARES[sq];
        board[sq] = wQueen;
    }
    if(sq == 4){
        PIECEBOARDS[wKing] = PIECEBOARDS[wKing] | BITSQUARES[sq];
        board[sq] = wKing;
    }
    if(sq > 7 && sq < 16){
        PIECEBOARDS[wPawn] = PIECEBOARDS[wPawn] | BITSQUARES[sq];
        board[sq] = wPawn;
    }
    
    
    //Black pieces
    if(sq == 56 || sq == 63){
        PIECEBOARDS[bRook] = PIECEBOARDS[bRook] | BITSQUARES[sq];
        board[sq] = bRook;
    }
    if(sq == 57 || sq == 62){
        PIECEBOARDS[bKnight] = PIECEBOARDS[bKnight] | BITSQUARES[sq];
        board[sq] = bKnight;
    }
    if(sq == 58 || sq == 61){
        PIECEBOARDS[bBishop] = PIECEBOARDS[bBishop] | BITSQUARES[sq];
        board[sq] = bBishop;
    }
    if(sq == 59){
        PIECEBOARDS[bQueen] = PIECEBOARDS[bQueen] | BITSQUARES[sq];
        board[sq] = bQueen;
    }
    if(sq == 60){
        PIECEBOARDS[bKing] = PIECEBOARDS[bKing] | BITSQUARES[sq];
        board[sq] = bKing;
    }
    if(sq > 47 && sq < 56){
        PIECEBOARDS[bPawn] = PIECEBOARDS[bPawn] | BITSQUARES[sq];
        board[sq] = bPawn;
    }
}

//Test Space
generateAllMoves(BLACK);

/*TEST SPACE

long Targets = 0x218008020000L;
int Diff = 64-8;
int pieceType = bPawn;
ArrayList<Integer> moves = new ArrayList();
int flags = 0;

moves = generateMoves(Targets, Diff, pieceType, moves, flags);

for(int i = 0; i<5;i++){
System.out.println(Integer.toBinaryString(moves.get(i)));
}
END OF TEST SPACE*/
}

public void generateAllMoves(int colour){

ArrayList<Integer> possibleMoves = new ArrayList();

if(colour == WHITE){

    generatePawnTargets(possibleMoves, colour, wPawn, PIECEBOARDS[wPawn]);




}
else{
    
    generatePawnTargets(possibleMoves, colour, bPawn, PIECEBOARDS[bPawn]);
    
}

for(int i = 0; i < possibleMoves.size(); i++){
    System.out.println(Integer.toBinaryString(possibleMoves.get(i)));
}
    
}

public void generatePawnTargets(ArrayList moves, int Colour, int pieceType, long pawns){
    
    long pawnPush, pawnDoublePush, promotions, attackTargets, attacks, epAttacks, promotionAttacks;
    
    int[] pushDiff = new int[]{8, 64 - 8};
    int[][] attackDiff = new int[][]{{7, 64-9}, {9,64-7}};
   
    
    long[] fileMask = new long[]{~Constants.FILE_H, ~Constants.FILE_A};
    long[] promotionMask = new long[]{Constants.RANK_8, Constants.RANK_1};
    long[] doublePushMask = new long[]{Constants.RANK_3,Constants.RANK_6};
    long[] enPassantMask = new long[]{Constants.RANK_6, Constants.RANK_3};
    
    int diff = pushDiff[Colour];
    
    long freeSquares = 0;
    for(long piece : PIECEBOARDS){
        freeSquares |= ~(piece);
    }
    
    long enemyPieces = 0;
    for(int j = (pieceType + 6)%12; j < (pieceType + 6)%12 + 6; j++){
        enemyPieces |= PIECEBOARDS[j];
    }
    
    
    long enPassantTargetSquare = Constants.FILES[flags & 0b00001110] & enPassantMask[Colour];
    
    //Single Pushes
    pawnPush = circularLeftShift(pawns, diff) & freeSquares;
    generatePawnMoves(pawnPush, diff, pieceType, moves, 0);
    System.out.println(moves.size());
    
    //Promotions
    promotions = pawnPush & promotionMask[Colour];
    generatePawnPromotionMoves(promotions, diff, pieceType, moves);
    System.out.println(moves.size());
    
    //Double Pushes
    pawnDoublePush = circularLeftShift(pawnPush & doublePushMask[Colour],diff) & freeSquares;
    generatePawnMoves(pawnDoublePush, diff+diff, pieceType, moves, 0);
    System.out.println(moves.size());
    
    //Attacks
    for(int dir = 0; dir < 2; dir++){
        
        diff = attackDiff[dir][Colour];
        attackTargets = circularLeftShift(pawns, diff) & fileMask[dir];
        
        //Simple Attacks
        attacks = attackTargets & enemyPieces;
        generatePawnMoves(attacks, diff, pieceType, moves, 0);
        
        //En Passant Attacks
        epAttacks = attackTargets & enPassantTargetSquare;
        if((flags & 1) == 1){
            generatePawnMoves(epAttacks, diff, pieceType, moves, Constants.moveFlagEnPassant);
        }
        
        //Promotion Attacks
        promotionAttacks = attacks & promotionMask[Colour];
        generatePawnPromotionMoves(promotionAttacks, diff, pieceType, moves);
    }
    
}

public void generateKnightTargets(ArrayList moves, int Colour, int pieceType, long knights){
    
    long targets;
    
    long friendlyPieces = 0;
    for(int j = pieceType; j < pieceType + 6; j++){
        friendlyPieces |= PIECEBOARDS[j];
    }
    
    while(knights > 0){
        int fromSq = Long.numberOfTrailingZeros(knights);
        targets = Constants.KnightMoves[fromSq];
    }
    
}



public void generatePawnMoves(long Targets, int moveDiff, int pieceType, ArrayList moveList, int flags){

    /*Move Flags
    parameter name: flags
    data type: byte
    
    bits 1-4: promoted piece type (Knight, Rook, Bishop, Queen)
    bit 5: promotion flag
    bit 6: en-passant capture flag
    bit 7: Queen Side Capture
    bit 8: King Side Capture
    */

    
    while(Targets > 0){
        int toSq = Long.numberOfTrailingZeros(Targets);
        //int fromSq = toSq - moveDiff;
        int fromSq = Integer.remainderUnsigned(toSq - moveDiff, 64);
        int capture = board[toSq];
        int move = pieceType << 28 | capture << 24 | fromSq << 16 | toSq << 8 | flags;
        moveList.add(move);
        Targets &= Targets - 1;
        
    }
}

public void generatePawnPromotionMoves(long Targets, int moveDiff, int pieceType, ArrayList moveList){
    
    while(Targets > 0){
        int toSq = Long.numberOfTrailingZeros(Targets);
        int fromSq = Integer.remainderUnsigned(toSq - moveDiff, 64);
        int capture = board[toSq];
        int move = pieceType << 28 | capture << 24 | fromSq << 16 | toSq << 8;
        
        if(pieceType >= bPawn){
            moveList.add(move | 0b10111); //Rook
            moveList.add(move | 0b11000); //Knight
            moveList.add(move | 0b11001); //Bishop
            moveList.add(move | 0b11010); //Queen
        }
        else{
            moveList.add(move | 0b10001); //Rook
            moveList.add(move | 0b10010); //Knight
            moveList.add(move | 0b10011); //Bishop
            moveList.add(move | 0b10100); //Queen
        }
        Targets &= Targets - 1;
        
    }
}

public long circularLeftShift(long bitBoard, int shift){
    return bitBoard << shift | bitBoard >> (64 - shift);
}

public void makeMove(int[] oldPos, int[] newPos){

int oldIndex = getIndex(oldPos);
int newIndex = getIndex(newPos);

//NEEDS TO GET UPDATED WITH PIECEBOARDS[PIECETYPE]
//TOO LAZY TO DO IT RIGHT NOW
//REMEMBER THIS!
    if((wPawn & BITSQUARES[oldIndex]) != 0){
        wPawn ^= BITSQUARES[oldIndex];
        wPawn |= BITSQUARES[newIndex];
    }
    else if((wRook & BITSQUARES[oldIndex]) != 0){
        wRook ^= BITSQUARES[oldIndex];
        wRook |= BITSQUARES[newIndex];
    }
    else if((wKnight & BITSQUARES[oldIndex]) != 0){
        wKnight ^= BITSQUARES[oldIndex];
        wKnight |= BITSQUARES[newIndex];
    }
    else if((wBishop & BITSQUARES[oldIndex]) != 0){
        wBishop ^= BITSQUARES[oldIndex];
        wBishop |= BITSQUARES[newIndex];
    }
    else if((wQueen & BITSQUARES[oldIndex]) != 0){
        wQueen ^= BITSQUARES[oldIndex];
        wQueen |= BITSQUARES[newIndex];
    }
    else if((wKing & BITSQUARES[oldIndex]) != 0){
        wKing ^= BITSQUARES[oldIndex];
        wKing |= BITSQUARES[newIndex];
    }
    
    else if((bPawn & BITSQUARES[oldIndex]) != 0){
        bPawn ^= BITSQUARES[oldIndex];
        bPawn |= BITSQUARES[newIndex];
    }
    else if((bRook & BITSQUARES[oldIndex]) != 0){
        bRook ^= BITSQUARES[oldIndex];
        bRook |= BITSQUARES[newIndex];
    }
    else if((bKnight & BITSQUARES[oldIndex]) != 0){
        bKnight ^= BITSQUARES[oldIndex];
        bKnight |= BITSQUARES[newIndex];
    }
    else if((bBishop & BITSQUARES[oldIndex]) != 0){
        bBishop ^= BITSQUARES[oldIndex];
        bBishop |= BITSQUARES[newIndex];
    }
    else if((bQueen & BITSQUARES[oldIndex]) != 0){
        bQueen ^= BITSQUARES[oldIndex];
        bQueen |= BITSQUARES[newIndex];
    }
    else if((bKing & BITSQUARES[oldIndex]) != 0){
        bKing ^= BITSQUARES[oldIndex];
        bKing |= BITSQUARES[newIndex];
    }
    
}

public int getIndex(int[] Position){
    int row = Position[0];
    int column = Position[1];
    return 8*row + column;
}

public void printAsBitBoard(long l){
    
    String bits = Long.toBinaryString(l);
    
    for(int j = 0; j < Long.numberOfLeadingZeros(l); j++){
        bits = "0"+bits;
    }

    for(int i = 0; i < 8; i++){
        System.out.println(new StringBuilder(bits.substring(i*8, i*8+8)).reverse().toString());
        
    }
}
 
//for testing
public static void main(String args[]){

ChessEngine ce = new ChessEngine();



}

}

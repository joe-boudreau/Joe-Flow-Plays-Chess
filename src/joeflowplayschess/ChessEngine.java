/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package joeflowplayschess;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public class ChessEngine {
    
private int WHITE = 0;
private int BLACK = 1;    
    
private int wPawn = 0;
private int wKnight = 1;
private int wBishop = 2;
private int wRook = 3;
private int wQueen = 4;
private int wKing = 5;

private int bPawn = 6;
private int bKnight = 7;
private int bBishop = 8;
private int bRook = 9;
private int bQueen = 10;
private int bKing = 11;

private int[] king = new int[]{wKing, bKing};
private int[] initKingPos = new int[]{4, 60};


private int empty = 0xE;

private int searchDepth = 4;

private byte gameFlags;

private long[] gamePieceBoards;
private long[] BITSQUARES;

private int[] gameBoard;

private int nodeCount;

Constants Constants = new Constants();
Random r = new Random();
    
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
gameFlags = (byte) 0b11110000;

gamePieceBoards = new long[12];
gameBoard = new int[64];

for(int sq = 0; sq < 64; sq++){
    
    //White Pieces
    if(sq == 0 || sq == 7){
        gamePieceBoards[wRook] = gamePieceBoards[wRook] | BITSQUARES[sq];
        gameBoard[sq] = wRook;
    }
    if(sq == 1 || sq == 6){
        gamePieceBoards[wKnight] = gamePieceBoards[wKnight] | BITSQUARES[sq];
        gameBoard[sq] = wKnight;
    }
    if(sq == 2 || sq == 5){
        gamePieceBoards[wBishop] = gamePieceBoards[wBishop] | BITSQUARES[sq];
        gameBoard[sq] = wBishop;
    }
    if(sq == 3){
        gamePieceBoards[wQueen] = gamePieceBoards[wQueen] | BITSQUARES[sq];
        gameBoard[sq] = wQueen;
    }
    if(sq == 4){
        gamePieceBoards[wKing] = gamePieceBoards[wKing] | BITSQUARES[sq];
        gameBoard[sq] = wKing;
    }
    if(sq > 7 && sq < 16){
        gamePieceBoards[wPawn] = gamePieceBoards[wPawn] | BITSQUARES[sq];
        gameBoard[sq] = wPawn;
    }
    
    if(sq > 15 && sq < 48){
        gameBoard[sq] = empty;
    }
    //Black pieces
    if(sq == 56 || sq == 63){
        gamePieceBoards[bRook] = gamePieceBoards[bRook] | BITSQUARES[sq];
        gameBoard[sq] = bRook;
    }
    if(sq == 57 || sq == 62){
        gamePieceBoards[bKnight] = gamePieceBoards[bKnight] | BITSQUARES[sq];
        gameBoard[sq] = bKnight;
    }
    if(sq == 58 || sq == 61){
        gamePieceBoards[bBishop] = gamePieceBoards[bBishop] | BITSQUARES[sq];
        gameBoard[sq] = bBishop;
    }
    if(sq == 59){
        gamePieceBoards[bQueen] = gamePieceBoards[bQueen] | BITSQUARES[sq];
        gameBoard[sq] = bQueen;
    }
    if(sq == 60){
        gamePieceBoards[bKing] = gamePieceBoards[bKing] | BITSQUARES[sq];
        gameBoard[sq] = bKing;
    }
    if(sq > 47 && sq < 56){
        gamePieceBoards[bPawn] = gamePieceBoards[bPawn] | BITSQUARES[sq];
        gameBoard[sq] = bPawn;
    }
}

}

public int[] selectMove(int colour){
    
int[] bestMove, moveInfo;

nodeCount = 0;

bestMove = chooseBestMove(gameBoard, gamePieceBoards, gameFlags, BLACK, searchDepth, Integer.MIN_VALUE, Integer.MAX_VALUE);

System.out.println("Nodes traversed: " + nodeCount);

moveInfo = parseMove(bestMove[0]);

if(bestMove[0] == -1){
    //White wins or stalemate
    return bestMove;
}
else if(bestMove[1] == (searchDepth-1)*1000000){
    //Black wins
    return new int[]{moveInfo[0], moveInfo[1], moveInfo[2], moveInfo[3], moveInfo[4], 0};
}

else if(bestMove[1] == (searchDepth-1)*-1000000){
    //Black stalemate
    return new int[]{moveInfo[0], moveInfo[1], moveInfo[2], moveInfo[3], moveInfo[4], -1};
}

gameFlags = updateGame(moveInfo, gameBoard, gamePieceBoards, gameFlags);

System.out.println("Best Move Score For Black:");
System.out.println(bestMove[1]);
System.out.println("Game Flags:");
System.out.println(Integer.toBinaryString(Byte.toUnsignedInt(gameFlags)));

return moveInfo;

}

private int[] chooseBestMove(int[] currBoard, long[] pieceBoards, byte flags, int colour, int depth, int alpha, int beta){

int i;
int[] moves, moveScore, tempBoard, bestMove, parsedMove;
int[][] parsedMoves, parsedMoves1;
long[] tempPieceBoards;
byte tempFlags;

boolean check = false;
boolean legal = true;

ArrayList<int[]> bestMoves = new ArrayList();

moves = generateAllMoves(colour, currBoard, pieceBoards, flags);

//parsedMoves = sortAndParseMoves(colour, moves);


if(inCheck(colour, currBoard, pieceBoards, flags)){
    check = true;
}

for(i = 0; i < moves.length; i++){
    
    nodeCount++;
    tempBoard = Arrays.copyOf(currBoard, 64);
    tempPieceBoards = Arrays.copyOf(pieceBoards, 12);
    tempFlags = flags;
    
    parsedMove = parseMove(moves[i]);
    //parsedMove = parsedMoves[i];
    
    tempFlags = updateGame(parsedMove, tempBoard, tempPieceBoards, tempFlags);
    
    if(parsedMove[0] == king[colour] && parsedMove[2] == initKingPos[colour] && parsedMove[3] == initKingPos[colour] + 2){
        legal = castleIsLegal(colour, tempBoard, tempPieceBoards, tempFlags, false); // king side castle
    }
    if(parsedMove[0] == king[colour] && parsedMove[2] == initKingPos[colour] && parsedMove[3] == initKingPos[colour] - 2){
        legal = castleIsLegal(colour, tempBoard, tempPieceBoards, tempFlags, true); // Queen side castle
    }
    
    if(legal && !inCheck(colour, tempBoard, tempPieceBoards, tempFlags)){
        
        if(depth > 0){
            
            int[] theirMove = chooseBestMove(tempBoard, tempPieceBoards, tempFlags, 1-colour, depth - 1, alpha, beta);
            moveScore = new int[]{moves[i], theirMove[1]};
            
        }
        else{
            moveScore = new int[]{moves[i], evaluateGameScore(tempPieceBoards, tempBoard, tempFlags)};
        }
        
        
        if(bestMoves.isEmpty()){
                bestMoves.add(moveScore);        
        }
        else if(bestMoves.get(0)[1] == moveScore[1]){
                bestMoves.add(moveScore);
        }
        
        if(colour == BLACK){
            
            if(bestMoves.get(0)[1] < moveScore[1]){
                bestMoves.clear();
                bestMoves.add(moveScore);   
            }
            alpha = Math.max(alpha, bestMoves.get(0)[1]);
            
        }
        else{
            
            if(bestMoves.get(0)[1] > moveScore[1]){
                bestMoves.clear();
                bestMoves.add(moveScore);
            } 
            beta = Math.min(beta, bestMoves.get(0)[1]);
        }
        
        if(beta < alpha){
                break; //Beta or alpha cut-off
        }
    }
    legal = true;
}

if(bestMoves.size() > 1){
    int randInt = r.nextInt(bestMoves.size());
    bestMove = bestMoves.get(randInt);
}
else if(bestMoves.size() == 1){
    bestMove = bestMoves.get(0);
}
else{
    
    if(check){
        bestMove = new int[]{-1, (-2*colour + 1)*1000000*depth}; //checkmate
    }
    else{
        bestMove = new int[]{-1, (2*colour - 1)*1000000*depth}; //stalemate
    }
    
}

return bestMove;

}

public int evaluateGameScore(long[] PIECEBOARDS, int[] currBoard, byte flags){

int overallScore;
    
overallScore = 2*(numSet(PIECEBOARDS[bPawn]) - numSet(PIECEBOARDS[wPawn])) +
                    7*(numSet(PIECEBOARDS[bKnight]) - numSet(PIECEBOARDS[wKnight])) +
                    6*(numSet(PIECEBOARDS[bBishop]) - numSet(PIECEBOARDS[wBishop])) +   
                    10*(numSet(PIECEBOARDS[bRook]) - numSet(PIECEBOARDS[wRook])) +
                    18*(numSet(PIECEBOARDS[bQueen]) - numSet(PIECEBOARDS[wQueen]));  

overallScore = 2*(overallScore) + centreControlScore(PIECEBOARDS);

overallScore = 3*(overallScore) + 2*(pawnStructureScore(bPawn) - pawnStructureScore(wPawn));

overallScore = 2*(overallScore) + positionScore(BLACK, currBoard, PIECEBOARDS, flags) - 
                                  positionScore(WHITE, currBoard, PIECEBOARDS, flags);



overallScore = 4*(overallScore) + advancementScore(PIECEBOARDS, BLACK) - advancementScore(PIECEBOARDS, WHITE);

return overallScore;

}

public int advancementScore(long[] currPieceBoards, int colour){
    
    int[] rankScore = new int[8];
    int advancement = 0;
    
    for(int i = 0; i < 8; i++){
        for(int j = colour*6; j < colour*6+6; j++){
            rankScore[i] += numSet(currPieceBoards[j] & Constants.RANKS[i]);
        }
        
        advancement += rankScore[i]*(i + (7-i)*colour);
    }
    
    return advancement;
}

public int centreControlScore(long[] PIECEBOARDS){
    
    long blackCentre = 0, whiteCentre = 0;
    
    for(int i = 0; i < 6; i++){
        whiteCentre |= (PIECEBOARDS[i] & Constants.CENTER_4);
    }
    for(int j = 6; j < 12; j++){
        blackCentre |= (PIECEBOARDS[j] & Constants.CENTER_4);
    }
    
    return numSet(blackCentre) - numSet(whiteCentre);
}

public int positionScore(int colour, int[] currBoard, long[] currPieceBoards, byte currFlags){
    
    int[] yourMoves = generateAllMoves(colour, currBoard, currPieceBoards, currFlags);
    int piecesAttacked = 0;
    
    int[] tempBoard;
    long[] tempPieceBoards;
    byte tempFlags;
    
    for(int move : yourMoves){
        if(((move << 4) >>> 28) > 0){
            piecesAttacked++;
        }
    }
    
    return 2*(piecesAttacked) + yourMoves.length;
}

public int pawnStructureScore(long pawns){
    
    long tPawns;
    int dubs = 0, isos = 0;
    int file;
    
    tPawns = pawns;
    while(tPawns > 0){
        int pawn = Long.numberOfTrailingZeros(tPawns);
        if((pawns & (1L << (pawn+8))) != 0){ dubs++;}
        tPawns &= (tPawns-1);
    }
    
    tPawns = pawns;
    while(tPawns > 0){
        int pawn = Long.numberOfTrailingZeros(tPawns);
        file = pawn%8;
        
        switch (file) {
            case 7:
                if((pawns & Constants.FILES[file-1]) == 0) { isos++;}
                break;
            case 0:
                if((pawns & Constants.FILES[file+1]) == 0) { isos++;}
                break;
            default:
                if(((pawns & Constants.FILES[file+1]) == 0) && ((pawns & Constants.FILES[file-1]) == 0)) { isos++;}
                break;
        }
        tPawns &= (tPawns-1);
    }
    
    return -1*(dubs+isos);
}

public void printMovesAsStrings(int[] moves){
    
    int piece, capturedPiece, fromSq, toSq, moveFlags, fromRow, fromCol, toRow, toCol;
    String[] pieceTypes = {"wPawn", "wKnight", "wBishop", "wRook", "wQueen", "wKing",
                           "bPawn", "bKnight", "bBishop", "bRook", "bQueen", "bKing", "", "", "None"};
    String[] rowNumber = {"1", "2", "3", "4", "5", "6", "7", "8"};
    String[] colNumber = {"a", "b", "c", "d", "e", "f", "g", "h"};
    
    String moveStr;
    
    for(int move : moves){
        piece =          move >>> 28;
        capturedPiece = (move << 4) >>> 28;
        fromSq =        (move << 8) >>> 24;
            fromRow = (int)(fromSq-fromSq%8)/8;
            fromCol = fromSq%8;
        toSq =          (move << 16) >>> 24;
            toRow = (int)(toSq-toSq%8)/8;
            toCol = toSq%8;
        moveFlags = (int)(byte) move;
        
        moveStr = "Piece: " + pieceTypes[piece] + ", Captured: " + pieceTypes[capturedPiece] +
                  ", From: " + colNumber[fromCol]+rowNumber[fromRow] + ", To: " +
                  colNumber[toCol]+rowNumber[toRow];
        if((moveFlags & Constants.moveFlagPromotion) != 0){
            moveStr += ", PROMOTION";
        }
        else if((moveFlags & Constants.moveFlagEnPassant) != 0){
            moveStr += ", EN PASSANT";
        }
        else if((moveFlags & Constants.moveFlagKingSideCastle) != 0){
            moveStr += ", KING-SIDE CASTLE";
        }
        else if((moveFlags & Constants.moveFlagQueenSideCastle) != 0){
            moveStr += ", QUEEN-SIDE CASTLE";
        }
        
        System.out.println(moveStr);
    }
}

public int[] parseMove(int move){
    int piece =          move >>> 28;
    int capturedPiece = (move << 4) >>> 28;
    int fromSq =        (move << 8) >>> 24;
    int toSq =          (move << 16) >>> 24;
    byte moveFlags =    (byte) move;
    
    return new int[]{piece, capturedPiece, fromSq, toSq, moveFlags};
}

public int[][] sortAndParseMoves(int colour, int[] moves){
    
    int[][] sortedParsedMoves = new int[moves.length][5];    
    int[][] parsedMoves = new int[moves.length][5];
    int[] moveScores = new int[moves.length];
    int[] bestMove, rowDel;
    int fromSq, toSq;

    for(int i = 0; i < moves.length; i++){
        
        parsedMoves[i] = parseMove(moves[i]);
        
        fromSq = parsedMoves[i][2];
        toSq = parsedMoves[i][3];
        
        rowDel = new int[]{(int)((toSq - toSq%8)/8) - (int)((fromSq - fromSq%8)/8), 
                           (int)((fromSq - fromSq%8)/8) - (int)((toSq - toSq%8)/8)};
        
        if(parsedMoves[i][1] != empty){
            moveScores[i] = 2*(4*(parsedMoves[i][1]%6) - (parsedMoves[i][0]%6));
        }
        
        moveScores[i] += rowDel[colour];
    }
    
    bestMove = new int[2];
    
    for(int j = 0; j < moves.length; j++){
        bestMove[0] = 0;
        bestMove[1] = moveScores[0];
        
        for(int k = 1; k < moves.length; k++){
            
            if(moveScores[k] > bestMove[1]){ 
                bestMove[0] = k;
                bestMove[1] = moveScores[k];
            }
        }
        sortedParsedMoves[j] = parsedMoves[bestMove[0]];
        moveScores[bestMove[0]] = Integer.MIN_VALUE;
    }
    
    return sortedParsedMoves;
    
}

public byte updateGame(int[] move, int[] board, long[] PIECEBOARDS, byte flags){
    
    
    int piece =         move[0];
    int capturedPiece = move[1];
    int fromSq =        move[2];
    int toSq =          move[3];
    int moveFlags =     move[4];
    
    int colour = piece < 6 ? WHITE : BLACK;
    if(piece > 11){
    System.out.println(move[0]);
    }
    PIECEBOARDS[piece] &= (~(1L << fromSq)); //Remove moving piece from old square
    board[fromSq] = empty;
    
    if((moveFlags & Constants.moveFlagPromotion) != 0){
        
        PIECEBOARDS[moveFlags & Constants.moveFlagPromotedPiece] |= (1L << toSq); //Add promoted piece to new square
        board[toSq] = moveFlags & Constants.moveFlagPromotedPiece;
    }
    else if((moveFlags & Constants.moveFlagEnPassant) != 0){
        
        int[] otherPiece = new int[]{bPawn, wPawn};
        int[] sqDelta = new int[]{-8, 8};
        
        PIECEBOARDS[piece] |= (1L << toSq); //add piece to new square
        PIECEBOARDS[otherPiece[colour]] &= (~(1L << (toSq + sqDelta[colour]))); //remove captured piece
        board[toSq + sqDelta[colour]] = empty;
        board[toSq] = piece;
    }
    else if((moveFlags & Constants.moveFlagKingSideCastle) != 0){
        
        int[] rook = new int[]{wRook, bRook};
        int[] newSq = new int[]{5, 61};
        int[] oldSq = new int[]{7, 63};

        PIECEBOARDS[piece] |= (1L << toSq); //add piece to new square
        PIECEBOARDS[rook[colour]] |= (1L << newSq[colour]); // Add rook to new square
        PIECEBOARDS[rook[colour]] &= (~(1L << oldSq[colour])); // Remove rook from old square
        board[toSq] = piece;
        board[oldSq[colour]] = empty;
        board[newSq[colour]] = rook[colour];
    }
    else if((moveFlags & Constants.moveFlagQueenSideCastle) != 0){
        
        int[] rook = new int[]{wRook, bRook};
        int[] newSq = new int[]{3, 59};
        int[] oldSq = new int[]{0, 56};

        PIECEBOARDS[piece] |= (1L << toSq); //add piece to new square
        PIECEBOARDS[rook[colour]] |= (1L << newSq[colour]); // Add rook to new square
        PIECEBOARDS[rook[colour]] &= (~(1L << oldSq[colour])); // Remove rook from old square
        board[toSq] = piece;
        board[oldSq[colour]] = empty;
        board[newSq[colour]] = rook[colour];
    }
    else{
        PIECEBOARDS[piece] |= (1L << toSq); //add piece to new square
        board[toSq] = piece;
    }
    
    if(capturedPiece != empty) {
            PIECEBOARDS[capturedPiece] &= (~(1L << toSq)); //remove captured piece
    }
    
    if((flags & 0b11110000) != 0){
        if(colour == WHITE){
            if(piece == wKing){
                flags &= 0b00111111;
            }
            else if(piece == wRook && fromSq == 0){
                flags &= 0b10111111;
            }
            else if(piece == wRook && fromSq == 7){
                flags &= 0b01111111;
            }
            
        }
        else{
            if(piece == bKing){
                flags &= 0b11001111;
            }
            else if(piece == bRook && fromSq == 56){
                flags &= 0b11101111;
            }
            else if(piece == bRook && fromSq == 63){
                flags &= 0b11011111;
            } 
        }
    }
    flags &= 0b11110000; // clear en-passant flags from last move
    
    if((piece == wPawn || piece == bPawn) && Math.abs(toSq - fromSq) == 16){ //en passant possible
        flags = (byte) (flags | (toSq%8 << 1) | 1);
    }
    
    return flags;
    
}

public boolean inCheck(int colour, int[] board, long[] PIECEBOARDS, byte flags){

int[] moves = generateAllMoves(1 - colour, board, PIECEBOARDS, flags);

for(int move : moves){
    if( ((move << 4) >>> 28) == (colour*6 + 5)) return true;
}

return false;

}

public boolean castleIsLegal(int colour, int[] board, long[] PIECEBOARDS, byte flags, boolean queenSide){
    
if(inCheck(colour, board, PIECEBOARDS, flags)){ return false;}

int[] tempBoard = Arrays.copyOf(board, 64);
long[] tempPieceBoards = Arrays.copyOf(PIECEBOARDS, 12);

tempBoard[initKingPos[colour]] = empty;

if(queenSide){
    
    for(int i = 1; i <= 2; i++){
        tempBoard[initKingPos[colour]-i] = king[colour];
        tempPieceBoards[king[colour]] <<= 1;
        
        if(inCheck(colour, tempBoard, tempPieceBoards, flags)){ return false;}
        
        tempBoard[initKingPos[colour]-i] = empty;
    }
}
else{
    
    for(int i = 1; i <= 2; i++){
        tempBoard[initKingPos[colour]+i] = king[colour];
        tempPieceBoards[king[colour]] >>= 1;
        
        if(inCheck(colour, tempBoard, tempPieceBoards, flags)){ return false;}
        
        tempBoard[initKingPos[colour]+i] = empty;
    }
    
}
 
return true;
}

public boolean castleIsLegal(boolean queenSide){

return castleIsLegal(WHITE, gameBoard, gamePieceBoards, gameFlags, queenSide);

}

public int[] whiteLegalMoves(){
    
ArrayList<Integer> legalList = new ArrayList();
int[] whiteMoves, legalMoves, tempBoard;
long[] tempPieceBoards;
byte tempFlags;

whiteMoves = generateAllMoves(WHITE, gameBoard, gamePieceBoards, gameFlags);

for(int wmove : whiteMoves){
    
    tempBoard = Arrays.copyOf(gameBoard, 64);
    tempPieceBoards = Arrays.copyOf(gamePieceBoards, 12);
    tempFlags = gameFlags;
    
    tempFlags = updateGame(parseMove(wmove), tempBoard, tempPieceBoards, tempFlags);
    
    if(!inCheck(WHITE, tempBoard, tempPieceBoards, tempFlags)){
        legalList.add(wmove);
    } 
}

legalMoves = new int[legalList.size()];

for(int i = 0; i < legalMoves.length; i++){
    legalMoves[i] = legalList.get(i);
}

return legalMoves;

}

public int[] generateAllMoves(int colour, int[] board, long[] PIECEBOARDS, byte flags){

ArrayList<Integer> possibleMoves = new ArrayList();

if(colour == WHITE){

    generatePawnTargets(possibleMoves, colour, wPawn, PIECEBOARDS[wPawn], board, PIECEBOARDS, flags);
    generateKnightTargets(possibleMoves, colour, wKnight, PIECEBOARDS[wKnight], board, PIECEBOARDS);
    generateKingTargets(possibleMoves, colour, wKing, PIECEBOARDS[wKing],  board, PIECEBOARDS, flags);
    generateRookTargets(possibleMoves, colour, wRook, PIECEBOARDS[wRook],  board, PIECEBOARDS);
    generateBishopTargets(possibleMoves, colour, wBishop, PIECEBOARDS[wBishop],  board, PIECEBOARDS);
    generateQueenTargets(possibleMoves, colour, wQueen, PIECEBOARDS[wQueen],  board, PIECEBOARDS);

}
else{
    
    generatePawnTargets(possibleMoves, colour, bPawn, PIECEBOARDS[bPawn], board, PIECEBOARDS, flags);
    generateKnightTargets(possibleMoves, colour, bKnight, PIECEBOARDS[bKnight], board, PIECEBOARDS);
    generateKingTargets(possibleMoves, colour, bKing, PIECEBOARDS[bKing], board, PIECEBOARDS, flags);
    generateRookTargets(possibleMoves, colour, bRook, PIECEBOARDS[bRook], board, PIECEBOARDS);
    generateBishopTargets(possibleMoves, colour, bBishop, PIECEBOARDS[bBishop], board, PIECEBOARDS);
    generateQueenTargets(possibleMoves, colour, bQueen, PIECEBOARDS[bQueen], board, PIECEBOARDS);
 
}

int[] movesArray = new int[possibleMoves.size()];

for(int i = 0; i < movesArray.length; i++){
    movesArray[i] = possibleMoves.get(i);
}

return movesArray;

}

public void generatePawnTargets(ArrayList moves, int Colour, int pieceType, long pawns, int[] board, long[] PIECEBOARDS, byte flags){
    
    if(pawns == 0) return;
    
    long pawnPush, pawnDoublePush, promotions, attackTargets, attacks, epAttacks, promotionAttacks;
    
    int[] pushDiff = new int[]{8, 64 - 8};
    int[][] attackDiff = new int[][]{{7, 64-9}, {9,64-7}};
   
    
    long[] fileMask = new long[]{~Constants.FILE_H, ~Constants.FILE_A};
    long[] promotionMask = new long[]{Constants.RANK_8, Constants.RANK_1};
    long[] doublePushMask = new long[]{Constants.RANK_3,Constants.RANK_6};
    long[] enPassantMask = new long[]{Constants.RANK_6, Constants.RANK_3};
    
    int diff = pushDiff[Colour];
    
    long freeSquares = Constants.ALL_SET;
    for(long piece : PIECEBOARDS){
        freeSquares &= (~piece);
    }
    
    long enemyPieces = 0;
    for(int j = (pieceType + 6)%12; j < (pieceType + 6)%12 + 6; j++){
        enemyPieces |= PIECEBOARDS[j];
    }

    
    long enPassantTargetSquare = Constants.FILES[(flags & 0b00001110) >>> 1] & enPassantMask[Colour];
    
    //Single Pushes
    pawnPush = circularLeftShift(pawns, diff) & freeSquares;
    generatePawnMoves(pawnPush, diff, pieceType, moves, 0, board);
    
    //Promotions
    promotions = pawnPush & promotionMask[Colour];
    generatePawnPromotionMoves(promotions, diff, pieceType, moves, board);
    
    //Double Pushes
    pawnDoublePush = circularLeftShift(pawnPush & doublePushMask[Colour],diff) & freeSquares;
    generatePawnMoves(pawnDoublePush, diff+diff, pieceType, moves, 0, board);
    
    //Attacks
    for(int dir = 0; dir < 2; dir++){
        
        diff = attackDiff[dir][Colour];
        attackTargets = circularLeftShift(pawns, diff) & fileMask[dir];
        
        //Simple Attacks
        attacks = attackTargets & enemyPieces;
        generatePawnMoves(attacks, diff, pieceType, moves, 0, board);
        
        //En Passant Attacks
        epAttacks = attackTargets & enPassantTargetSquare;
        if((flags & 1) == 1){
            generatePawnMoves(epAttacks, diff, pieceType, moves, Constants.moveFlagEnPassant, board);
        }
        
        //Promotion Attacks
        promotionAttacks = attacks & promotionMask[Colour];
        generatePawnPromotionMoves(promotionAttacks, diff, pieceType, moves, board);
    }
    
}

public void generateKnightTargets(ArrayList moves, int Colour, int pieceType, long knights, int[] board, long[] PIECEBOARDS){
    
    if(knights == 0) return;
    
    long targets;
    
    long friendlyPieces = 0;
    for(int j = Colour*6; j < Colour*6 + 6; j++){
        friendlyPieces |= PIECEBOARDS[j];
    }
    
    while(knights > 0){
        int fromSq = Long.numberOfTrailingZeros(knights);
        targets = Constants.KnightMoves[fromSq] & ~friendlyPieces;
        generateMoves(fromSq, targets, pieceType, moves, 0, board);
        knights &= knights - 1;
    }
    
}

public void generateKingTargets(ArrayList moves, int Colour, int pieceType, long king, int[] board, long[] PIECEBOARDS, byte flags){
    
    if(king == 0) return;
    
    long targets;
    
    long allPieces = 0;
    for(int i = 0; i < 12; i++){
        allPieces |= PIECEBOARDS[i];
    }

    long friendlyPieces = 0;
    for(int j = Colour*6; j < Colour*6 + 6; j++){
        friendlyPieces |= PIECEBOARDS[j];
    }
    
    int fromSq = Long.numberOfTrailingZeros(king);
    targets = Constants.KingMoves[fromSq] & ~friendlyPieces;
    generateMoves(fromSq, targets, pieceType, moves, 0, board);
    
    if(Colour == WHITE){
        if((flags & (1 << 6)) != 0 && (allPieces & Constants.queenCastleSquares[WHITE]) == 0){
            generateMoves(fromSq, 0x4L, pieceType, moves, Constants.moveFlagQueenSideCastle, board);
        }
        if((flags & (1 << 7)) != 0 && (allPieces & Constants.kingCastleSquares[WHITE]) == 0){
            generateMoves(fromSq, 0x40L, pieceType, moves, Constants.moveFlagKingSideCastle, board);
        }  
    }
    else{
        if((flags & (1 << 4)) != 0 && (allPieces & Constants.queenCastleSquares[BLACK]) == 0){
            generateMoves(fromSq, 0x400000000000000L, pieceType, moves, Constants.moveFlagQueenSideCastle, board);
        }
        if((flags & (1 << 5)) != 0 && (allPieces & Constants.kingCastleSquares[BLACK]) == 0){
            generateMoves(fromSq, 0x4000000000000000L, pieceType, moves, Constants.moveFlagKingSideCastle, board);
        }
    }
}

public void generateRookTargets(ArrayList moves, int Colour, int pieceType, long rooks, int[] board, long[] PIECEBOARDS){

    if(rooks == 0) return;
    
    long allPieces, friendlyPieces, targets;
    int i, j, fromSq, index;
    
    allPieces = 0;
    for(i = 0; i < 12; i++){
        allPieces |= PIECEBOARDS[i];
    }
    
    friendlyPieces = 0;
    for(j = Colour*6; j < Colour*6 + 6; j++){
        friendlyPieces |= PIECEBOARDS[j];
    }
    
    while(rooks != 0){
        fromSq = Long.numberOfTrailingZeros(rooks);

        index = (int) (((allPieces & Constants.RookMaskOnSquare[fromSq])*
                    Constants.magicNumberRook[fromSq]) >>>
                    Constants.magicShiftsRook[fromSq]);

        targets = Constants.magicMovesRook[fromSq][index] & (~friendlyPieces);
        generateMoves(fromSq, targets, pieceType, moves, 0, board);
        
        rooks &= rooks -1;
    }
}

public void generateBishopTargets(ArrayList moves, int Colour, int pieceType, long bishops, int[] board, long[] PIECEBOARDS){

    if(bishops == 0) return;
    
    long allPieces, friendlyPieces, targets;
    int i, j, fromSq, index;
    
    allPieces = 0;
    for(i = 0; i < 12; i++){
        allPieces |= PIECEBOARDS[i];
    }
    
    friendlyPieces = 0;
    for(j = Colour*6; j < Colour*6 + 6; j++){
        friendlyPieces |= PIECEBOARDS[j];
    }
    
    while(bishops != 0){
        fromSq = Long.numberOfTrailingZeros(bishops);

        index = (int) (((allPieces & Constants.BishopMaskOnSquare[fromSq])*
                    Constants.magicNumberBishop[fromSq]) >>>
                    Constants.magicShiftsBishop[fromSq]);

        targets = Constants.magicMovesBishop[fromSq][index] & (~friendlyPieces);
        generateMoves(fromSq, targets, pieceType, moves, 0, board);
        
        bishops &= bishops -1;
    }
}

public void generateQueenTargets(ArrayList moves, int Colour, int pieceType, long queens, int[] board, long[] PIECEBOARDS){
    
    if(queens == 0) return;
    
    long allPieces, friendlyPieces, targets;
    int i, j, fromSq, rookIndex, bishopIndex;
    
    allPieces = 0;
    for(i = 0; i < 12; i++){
        allPieces |= PIECEBOARDS[i];
    }
    
    friendlyPieces = 0;
    for(j = Colour*6; j < Colour*6 + 6; j++){
        friendlyPieces |= PIECEBOARDS[j];
    }
    
    while(queens != 0){
        fromSq = Long.numberOfTrailingZeros(queens);

        rookIndex = (int) (((allPieces & Constants.RookMaskOnSquare[fromSq])*
                    Constants.magicNumberRook[fromSq]) >>>
                    Constants.magicShiftsRook[fromSq]);
        
        bishopIndex = (int) (((allPieces & Constants.BishopMaskOnSquare[fromSq])*
                    Constants.magicNumberBishop[fromSq]) >>>
                    Constants.magicShiftsBishop[fromSq]);

        targets = (Constants.magicMovesBishop[fromSq][bishopIndex] | Constants.magicMovesRook[fromSq][rookIndex])
                  & (~friendlyPieces);
        generateMoves(fromSq, targets, pieceType, moves, 0, board);
        
        queens &= queens -1;
    }
}

public void generateMoves(int from, long Targets, int pieceType, ArrayList moveList, int flags, int[] board){
    
    while(Targets != 0){
        
        int toSq = Long.numberOfTrailingZeros(Targets);
        int capture = board[toSq];
        int move = pieceType << 28 | capture << 24 | from << 16 | toSq << 8 | flags;
        moveList.add(move);
        Targets &= Targets - 1;
        
        
    }
}

public void generatePawnMoves(long Targets, int moveDiff, int pieceType, ArrayList moveList, int flags, int[] board){

    /*Move Flags
    parameter name: flags
    data type: byte
    
    bits 1-4: promoted piece type (Knight, Rook, Bishop, Queen)
    bit 5: promotion flag
    bit 6: en-passant capture flag
    bit 7: Queen Side Capture
    bit 8: King Side Capture
    */

    
    while(Targets != 0){
        
        int toSq = Long.numberOfTrailingZeros(Targets);
        int fromSq = Integer.remainderUnsigned(toSq - moveDiff, 64);
        int capture = board[toSq];
        int move = pieceType << 28 | capture << 24 | fromSq << 16 | toSq << 8 | flags;
        moveList.add(move);
        Targets &= Targets - 1;
        
    }
}

public void generatePawnPromotionMoves(long Targets, int moveDiff, int pieceType, ArrayList moveList, int[] board){
    
    while(Targets != 0){
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

public void makeMove(int[] oldPos, int[] newPos, int moveFlags){

int oldIndex = getIndex(oldPos);
int newIndex = getIndex(newPos);

int piece =          gameBoard[oldIndex];
int capturedPiece =  gameBoard[newIndex];
int fromSq =         oldIndex;
int toSq =           newIndex;

int move = piece << 28 | capturedPiece << 24 | fromSq << 16 | toSq << 8 | moveFlags;

int[] moveInfo = parseMove(move);

System.out.println(Arrays.toString(moveInfo));
gameFlags = updateGame(moveInfo, gameBoard, gamePieceBoards, gameFlags);
 
}

public int getIndex(int[] Position){
    int row = Position[0];
    int column = Position[1];
    return 8*row + column;
}

public int numSet(long l){
    return Long.bitCount(l);
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

public void printBoardArray(int[] b){
    
    for(int i = 56; i > -1; i-=8){
        System.out.println(b[i] + "  " + b[i+1] + "  " +
                         b[i+2] + "  " + b[i+3] + "  " +
                         b[i+4] + "  " + b[i+5] + "  " +
                         b[i+6] + "  " + b[i+7] + "  ");  
    }
}
 
//for testing
public static void main(String args[]){

ChessEngine ce = new ChessEngine();



}

}

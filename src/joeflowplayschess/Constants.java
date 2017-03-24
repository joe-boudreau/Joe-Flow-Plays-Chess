/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package joeflowplayschess;

import java.util.ArrayList;
import java.util.Random;

/**
 * Pre-computed values used in ChessEngine class. Some are for convenience, but
 * this class is depended on by ChessEngine to compute the necessary attack sets
 * for sliding piece move generation. Most of the code for that, which involves
 * something referred to as "magic bitboards", is based off a perfact hashing
 * algorithm for quick efficient move sets. I got the algorithm from the following
 * blog:
 * http://www.afewmorelines.com/understanding-magic-bitboards-in-chess-programming/
 * But there were a number of changes I needed to make to the code for it to run
 * in my framework.
 * 
 * Everything here is based off the following bitboard representation:
 * 
 * 56 57 58 59 60 61 62 63
 * 48 49 50 51 52 53 54 55
 * 40 41 42 43 44 45 46 47
 * 32 33 34 35 36 37 38 39
 * 24 25 26 27 28 29 30 31
 * 16 17 18 19 20 21 22 23
 * 8  9  10 11 12 13 14 15
 * 0  1  2  3  4  5  6  7
 * 
 * where the 64-bits in the long represent the following:
 * 
 * MSb [63, 62, 61 .. 0] LSb
 * 
 * For example, all the squares in the bottom row, a.k.a Rank 1, could be represent
 * as long 0xff, or binary 0b11111111, since those are the 8 least significant
 * digits
 * 
 * 
 * 
 * @author thejoeflow
 */

public class Constants {

public long[] BITSQUARES =           new long[64]; //these will be the same as just doing (1L >> index of square) but I didn't know that earlier when building this class. oh well. 
public long ALL_SET =                0xffffffffffffffffL; //all 64 squares

public long RANK_1 =                 0xffL;
public long RANK_2 =                 0xff00L;
public long RANK_3 =                 0xff0000L;
public long RANK_4 =                 0xff000000L;
public long RANK_5 =                 0xff00000000L;
public long RANK_6 =                 0xff0000000000L;
public long RANK_7 =                 0xff000000000000L;
public long RANK_8 =                 0xff00000000000000L;

public long[] RANKS =                new long[]{RANK_1, RANK_2, RANK_3, RANK_4, RANK_5, RANK_6, RANK_7, RANK_8};
        
public long FILE_A =                 0x101010101010101L;
public long FILE_B =                 0x202020202020202L;
public long FILE_C =                 0x404040404040404L;
public long FILE_D =                 0x808080808080808L;
public long FILE_E =                 0x1010101010101010L;
public long FILE_F =                 0x2020202020202020L;
public long FILE_G =                 0x4040404040404040L;
public long FILE_H =                 0x8080808080808080L;

public long[] FILES =                new long[]{FILE_A, FILE_B, FILE_C, FILE_D, FILE_E, FILE_F, FILE_G, FILE_H};

public long CENTER_4 =               0x1818000000L;

public long[] KnightMoves =          new long[64];
public long[] KingMoves =            new long[64];

//Used when setting moveFlags. Makes for easier code and less mistakes
public int moveFlagPromotedPiece =   0b00001111; 
public int moveFlagPromotion =       0b00010000;
public int moveFlagEnPassant =       0b00100000;
public int moveFlagQueenSideCastle = 0b01000000;
public int moveFlagKingSideCastle =  0b10000000;

//The squares in between the castling squares which need to be checked for potential checks
public long[] queenCastleSquares =   new long[]{0xe, 0xe00000000000000L};
public long[] kingCastleSquares =    new long[]{0x60, 0x6000000000000000L};

//All the following arrays are used in the magic bitboard generation
public long[][] occupancyVariation = new long[64][];
public long[][] occupancyAttackSet = new long[64][];

public long[] magicNumberRook =      new long[64];
public int[] magicShiftsRook =       new int[64];

public long[] magicNumberBishop =    new long[64];
public int[] magicShiftsBishop =     new int[64];

public long[][] magicMovesRook =     new long[64][];
public long[][] magicMovesBishop =   new long[64][];

public long[] RookMaskOnSquare =     new long[64];
public long[] BishopMaskOnSquare =   new long[64];

public Constants(){
    
    //BITSQUARES is a 64 element array of longs representing each square on the board
    BITSQUARES[0] = 1;
    for(int i =  1; i < 64; i++){
        BITSQUARES[i] = BITSQUARES[i-1] << 1;
    }
    
    /*KingMoves and KnightMoves are 64-element long arrays which represent the
    possible destination squares for kings and knights at each square on the board.
    They are built by initially defining the 35th square and bitshifting in either
    direction to define squares 34 --> 0 and 36 --> 63. Then the wrap-around 
    overflow must be masked out for the edge files in the following for loop.
    */
    KingMoves[35] = 0x1c141c000000L;
    /*
    0 0 0 0 0 0 0 0
    0 0 0 0 0 0 0 0
    0 0 0 0 0 0 0 0
    0 0 1 1 1 0 0 0
    0 0 1 0 1 0 0 0
    0 0 1 1 1 0 0 0
    0 0 0 0 0 0 0 0
    0 0 0 0 0 0 0 0
    */
    KnightMoves[35] = 0x14220022140000L;
    /*
    0 0 0 0 0 0 0 0
    0 0 0 0 0 0 0 0
    0 0 1 0 1 0 0 0
    0 1 0 0 0 1 0 0
    0 0 0 0 0 0 0 0
    0 1 0 0 0 1 0 0
    0 0 1 0 1 0 0 0
    0 0 0 0 0 0 0 0
    */
    for(int i = 36; i<64; i++){
        KingMoves[i] = KingMoves[i-1] << 1;
        KnightMoves[i] = KnightMoves[i-1] << 1;
    }
    
    for(int j = 34; j>-1; j--){
        KingMoves[j] = KingMoves[j+1] >> 1;
        KnightMoves[j] = KnightMoves[j+1] >> 1;
    }
    
    //Mask out wrap-around overflows as a result of the bitshifting
    for(int k = 0; k<64; k++){
        if( k%8 == 7){ KingMoves[k] &= ~(FILE_A);}
        if( k%8 == 0){ KingMoves[k] &= ~(FILE_H);}
        if( k%8 == 6){ KnightMoves[k] &= ~(FILE_A);}
        if( k%8 == 7){ KnightMoves[k] &= ~(FILE_A | FILE_B);}
        if( k%8 == 0){ KnightMoves[k] &= ~(FILE_G | FILE_H);}
        if( k%8 == 1){ KnightMoves[k] &= ~(FILE_H);}
    }
    
    /*
    The following code builds the RookMaskOnSquare and BishopMaskOnSquare arrays.
    These are 64-element long arrays which represent the destination squares for
    rooks and bishops assuming an empty board. 
    
    For example, for a rook on square 35:
        0 0 0 0 0 0 0 0
        0 0 0 1 0 0 0 0
        0 0 0 1 0 0 0 0
        0 0 0 1 0 0 0 0
        0 1 1 0 1 1 1 0
        0 0 0 1 0 0 0 0
        0 0 0 1 0 0 0 0
        0 0 0 0 0 0 0 0
    
    The boundary squares are not set within the mask because these will always
    be blocking squares for sliding piece movement, they do not need to be checked
    for piece occupancy when determining the attack sets.
    
    These arrays will be used in the three functions which build the magic
    bitboards for sliding move piece generation.
    */
    
    //ray directions for rooks and bishops
    int[] rookDelta = new int[]{-1, 1, -8, 8};
    int[] bishopDelta = new int[]{-9, 7, 9, -7};
    
    //terminating files/ranks for the respective rays
    long[] rookTerminator = new long[]{FILE_A, FILE_H, RANK_1, RANK_8};
    long[] bishopTerminator = new long[]{RANK_1, FILE_A, RANK_8, FILE_H};
    
    int square;
    for(int i = 0; i < 64; i++){ //for each square
        RookMaskOnSquare[i] = 0;
        
        for(int j = 0; j < 4; j++){ //for each ray direction
            square = i; //start at the origin square
            while((BITSQUARES[square] & rookTerminator[j]) == 0){
                RookMaskOnSquare[i] |= BITSQUARES[square];
                square += rookDelta[j];
            }
            square = i;
            //diagonal rays can be terminated by either a file or rank, so both conditions must be checked
            while((BITSQUARES[square] & bishopTerminator[j]) == 0 && (BITSQUARES[square] & bishopTerminator[(j+1)%4]) == 0){
                BishopMaskOnSquare[i] |= BITSQUARES[square];
                square +=  bishopDelta[j];
            }
        }
        
        //remove the origin square since it would not be a destination square
        RookMaskOnSquare[i] ^= BITSQUARES[i];
        BishopMaskOnSquare[i] ^= BITSQUARES[i];   
    }

//Rooks    
generateOccupancyVariations(true);
generateMagicNumbers(true);
generateMoveDatabase(true);

//Bishops
generateOccupancyVariations(false);
generateMagicNumbers(false);
generateMoveDatabase(false);


}


/**
 * Generates every occupancy variation for a particular square for both bishops
 * and rooks, and then generates each corresponding attack set for every
 * occupancy variation.
 * 
 * For example, the following are two possible occupancy variations for a rook
 * on square 2:
 * 
 * Variation 1 (Rook - square 2)   
    0 0 1 0 0 0 0 0
    0 0 0 0 0 0 0 0
    0 0 1 0 0 0 0 0
    0 0 1 0 0 0 0 0
    0 0 0 0 0 0 0 0
    0 0 0 0 0 0 0 0
    0 0 0 0 0 0 0 0
    0 1 R 0 1 0 1 0

*   Variation 2 (Rook - square 2)
    0 0 0 0 0 0 0 0
    0 0 0 0 0 0 0 0
    0 0 0 0 0 0 0 0
    0 0 1 0 0 0 0 0
    0 0 0 0 0 0 0 0
    0 0 0 0 0 0 0 0
    0 0 0 0 0 0 0 0
    1 1 R 0 1 0 0 0
    
    However, both these occupancy variations produce the same resulting attack
    set for rooks, which is shown below
    
    Attack set (Rook - square 2)
    0 0 0 0 0 0 0 0
    0 0 0 0 0 0 0 0
    0 0 0 0 0 0 0 0
    0 0 1 0 0 0 0 0
    0 0 0 0 0 0 0 0
    0 0 0 0 0 0 0 0
    0 0 0 0 0 0 0 0
    0 1 R 0 1 0 0 0
    
    The purpose of the attack set is to define the boundaries of the rays for the
    sliding piece in every direction based off the positions of all the other
    pieces on the board. Once this is determined, the blocking pieces which define
    the boundaries of the attack set just need to be checked to find out if they
    are capturable or not, or in other words if they are enemy pieces or friendly
    pieces.
 * 
 * The number of occupancy variations for a particular piece on a square is just
 * the total number of different permutations of pieces existing on the possible
 * destination squares for a piece on a square. This is simply 2^(number of 
 * destination squares on the mask). For example, for the rook on Square 2, the
 * destination square mask looks like this:
 *  0 0 0 0 0 0 0 0
    0 0 1 0 0 0 0 0
    0 0 1 0 0 0 0 0
    0 0 1 0 0 0 0 0
    0 0 1 0 0 0 0 0
    0 0 1 0 0 0 0 0
    0 0 1 0 0 0 0 0
    0 1 R 1 1 1 1 0
 * 
 * Since this mask has 11 bits set in it, the amount of different occupancy
 * variations for this square will be 2^11.
 * 
 * @param isRook true if rook; false if bishop
 */
@SuppressWarnings("empty-statement")
private void generateOccupancyVariations(boolean isRook){
    
    int i, j, square;
    long mask;
    int variationCount;
    int[] setBitsInMask, setBitsInIndex;
    int[] bitCount = new int[64];
    
    for(square = 0; square < 64; square++){
        
        mask = isRook? RookMaskOnSquare[square] : BishopMaskOnSquare[square];
        setBitsInMask = getIndexOfSetBits(mask);
        bitCount[square] = Long.bitCount(mask);
        variationCount = (int)(1L << bitCount[square]);
        
        occupancyVariation[square] = new long[variationCount];
        occupancyAttackSet[square] = new long[variationCount];
        
        for(i = 0; i < variationCount; i++){
            
            occupancyVariation[square][i] = 0;
            
            setBitsInIndex = getIndexOfSetBits(i);
            
            for(j = 0; j < setBitsInIndex.length; j++){
                
                occupancyVariation[square][i] |= (1L << setBitsInMask[setBitsInIndex[j]]);
            }
            
            if(isRook){
            
                for(j = square+8; j<64 && (occupancyVariation[square][i] & (1L << j)) == 0; j+=8);
                if (j<64) occupancyAttackSet[square][i] |= (1L << j);
                
                for(j = square-8; j>-1 && (occupancyVariation[square][i] & (1L << j)) == 0; j-=8);
                if (j>-1) occupancyAttackSet[square][i] |= (1L << j);
                
                for(j = square+1; j%8!=0 && (occupancyVariation[square][i] & (1L << j)) == 0; j++);
                if (j%8!=0) occupancyAttackSet[square][i] |= (1L << j);
                
                for(j = square-1; (j%8 + 8)%8!=7 && j>-1 && (occupancyVariation[square][i] & (1L << j)) == 0; j--);
                if ((j%8 + 8)%8!=7) occupancyAttackSet[square][i] |= (1L << j);
                  
            }
            else{
            
                for(j = square+9; j%8!=0 && j<64 && (occupancyVariation[square][i] & (1L << j)) == 0; j+=9);
                if (j%8!=0 && j<64) occupancyAttackSet[square][i] |= (1L << j);
                
                for(j = square-9; (j%8 + 8)%8!=7 && j>-1 && (occupancyVariation[square][i] & (1L << j)) == 0; j-=9);
                if ((j%8 + 8)%8!=7 && j>-1) occupancyAttackSet[square][i] |= (1L << j);
                
                for(j = square+7; j%8!=7 && j<64 && (occupancyVariation[square][i] & (1L << j)) == 0; j+=7);
                if (j%8!=7 && j<64) occupancyAttackSet[square][i] |= (1L << j);
                
                for(j = square-7; (j%8 + 8)%8!=0 && j>-1 && (occupancyVariation[square][i] & (1L << j)) == 0; j-=7);
                if ((j%8 + 8)%8!=0 && j>-1) occupancyAttackSet[square][i] |= (1L << j);
                
            }
        }
    }
}

private void generateMagicNumbers(boolean isRook){
    
    int i, j, square, variationCount;
    boolean fail;
    
    Random r = new Random();
    long magicNumber = 0;
    int index;
    
    for (square = 0; square < 64; square++){
        
        int bitCount = Long.bitCount(isRook ? RookMaskOnSquare[square] : BishopMaskOnSquare[square]);
        variationCount = (int)(1L << bitCount);
        long usedBy[] = new long[variationCount];
        
        do{
            
            magicNumber = r.nextLong() & r.nextLong() & r.nextLong();
            
            for(j = 0; j < variationCount; j++) usedBy[j] = 0;
            
            for(i = 0, fail = false; i < variationCount && !fail; i++){
                
                index = (int)((occupancyVariation[square][i] * magicNumber) >>> (64 - bitCount));
                fail = usedBy[index] != 0 && usedBy[index] != occupancyAttackSet[square][i];
                
                usedBy[index] = occupancyAttackSet[square][i];
            }
        }
        while(fail);
        
        
        if(isRook){
            magicNumberRook[square] = magicNumber;
            magicShiftsRook[square] = 64 - bitCount;
        }
        else{
            magicNumberBishop[square] = magicNumber;
            magicShiftsBishop[square] = 64 - bitCount;
        }
    }
}

private void generateMoveDatabase(boolean isRook){
    
    long validMoves;
    int variations, bitCount;
    int square, i, j, magicIndex;
    
    for(square = 0; square < 64; square++){
        
        bitCount = Long.bitCount(isRook ? RookMaskOnSquare[square] : BishopMaskOnSquare[square]);
        variations = (int)(1L << bitCount);
        
        if(isRook){
            magicMovesRook[square] = new long[variations];
        }
        else{
            magicMovesBishop[square] = new long[variations];
        }
                
        for(i = 0; i < variations; i++){
            
            validMoves = 0;

            if(isRook){
                
                magicIndex = (int)((occupancyVariation[square][i] * magicNumberRook[square]) >>> magicShiftsRook[square]);
                
                for(j = square+8; j < 64; j+=8){
                    validMoves |= (1L << j);
                    if((occupancyVariation[square][i] & (1L << j)) != 0) break;
                } 
                for(j = square-8; j > -1; j-=8){
                    validMoves |= (1L << j);
                    if((occupancyVariation[square][i] & (1L << j)) != 0) break;
                }
                for(j = square+1; j%8 != 0; j++){
                    validMoves |= (1L << j);
                    if((occupancyVariation[square][i] & (1L << j)) != 0) break;
                }
                for(j = square-1; (j%8 + 8)%8 != 7; j--){
                    validMoves |= (1L << j);
                    if((occupancyVariation[square][i] & (1L << j)) != 0) break;
                }

                magicMovesRook[square][magicIndex] = validMoves;
            }
            else{
                
                magicIndex = (int)((occupancyVariation[square][i] * magicNumberBishop[square]) >>> magicShiftsBishop[square]);
                
                for(j = square+9; j < 64 && j%8 != 0; j+=9){
                    validMoves |= (1L << j);
                    if((occupancyVariation[square][i] & (1L << j)) != 0) break;
                }
                for(j = square-9; j > -1 && (j%8 + 8)%8 != 7; j-=9){
                    validMoves |= (1L << j);
                    if((occupancyVariation[square][i] & (1L << j)) != 0) break;
                }
                for(j = square+7; j < 64 && j%8 != 7; j+=7){
                    validMoves |= (1L << j);
                    if((occupancyVariation[square][i] & (1L << j)) != 0) break;
                }
                for(j = square-7; j > -1 && (j%8 + 8)%8 != 0; j-=7){
                    validMoves |= (1L << j);
                    if((occupancyVariation[square][i] & (1L << j)) != 0) break;
                }
                
                magicMovesBishop[square][magicIndex] = validMoves;
                
            }
        }
    }
}

private int[] getIndexOfSetBits(long l){
    ArrayList<Integer> setBits = new ArrayList();
    
    while(l > 0){
        setBits.add(Long.numberOfTrailingZeros(l));
        l &= l-1;
    }
    
    int[] sBi = new int[setBits.size()];
    
    for(int i = 0; i < sBi.length; i++){
        sBi[i] = setBits.get(i);
    }   
    return sBi;
}




}

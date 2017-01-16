/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package joeflowplayschess;

import java.util.ArrayList;
import java.util.Random;



public class Constants {

public long[] BITSQUARES =           new long[64];
public long ALL_SET =                0xffffffffffffffffL;

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

public long[] RookMaskOnSquare =     new long[64];
public long[] BishopMaskOnSquare =   new long[64];

public int moveFlagPromotedPiece =   0b00001111;
public int moveFlagPromotion =       0b00010000;
public int moveFlagEnPassant =       0b00100000;
public int moveFlagQueenSideCastle = 0b01000000;
public int moveFlagKingSideCastle =  0b10000000;

public long[] queenCastleSquares =   new long[]{0xe, 0xe00000000000000L};
public long[] kingCastleSquares =    new long[]{0x60, 0x6000000000000000L};

public long[][] occupancyVariation = new long[64][];
public long[][] occupancyAttackSet = new long[64][];

public long[] magicNumberRook =      new long[64];
public int[] magicShiftsRook =       new int[64];

public long[] magicNumberBishop =    new long[64];
public int[] magicShiftsBishop =     new int[64];

public long[][] magicMovesRook =     new long[64][];
public long[][] magicMovesBishop =   new long[64][];

public Constants(){
    
    BITSQUARES[0] = 1;    

    for(int i =  1; i < 64; i++){
        BITSQUARES[i] = BITSQUARES[i-1] << 1;
    }

    KingMoves[35] = 0x1c141c000000L;
    KnightMoves[35] = 0x14220022140000L;

    for(int i = 36; i<64; i++){
        KingMoves[i] = KingMoves[i-1] << 1;
        KnightMoves[i] = KnightMoves[i-1] << 1;
    }
    
    for(int j = 34; j>-1; j--){
        KingMoves[j] = KingMoves[j+1] >> 1;
        KnightMoves[j] = KnightMoves[j+1] >> 1;
    }

    for(int k = 0; k<64; k++){
        if( k%8 == 7){ KingMoves[k] &= ~(FILE_A);}
        if( k%8 == 0){ KingMoves[k] &= ~(FILE_H);}
        if( k%8 == 6){ KnightMoves[k] &= ~(FILE_A);}
        if( k%8 == 7){ KnightMoves[k] &= ~(FILE_A | FILE_B);}
        if( k%8 == 0){ KnightMoves[k] &= ~(FILE_G | FILE_H);}
        if( k%8 == 1){ KnightMoves[k] &= ~(FILE_H);}
    }
    
    
    int[] rookDelta = new int[]{-1, 1, -8, 8};
    int[] bishopDelta = new int[]{-9, 7, 9, -7};
    
    long[] rookTerminator = new long[]{FILE_A, FILE_H, RANK_1, RANK_8};
    long[] bishopTerminator = new long[]{RANK_1, FILE_A, RANK_8, FILE_H};
    
    int square;
    for(int i = 0; i < 64; i++){
        RookMaskOnSquare[i] = 0;
        
        for(int j = 0; j < 4; j++){
            square = i;
            while((BITSQUARES[square] & rookTerminator[j]) == 0){
                RookMaskOnSquare[i] |= BITSQUARES[square];
                square += rookDelta[j];
            }
            square = i;
            while((BITSQUARES[square] & bishopTerminator[j]) == 0 && (BITSQUARES[square] & bishopTerminator[(j+1)%4]) == 0){
                BishopMaskOnSquare[i] |= BITSQUARES[square];
                square +=  bishopDelta[j];
            }
        }
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

public long getRay(int A, int B){
    
    long ray = 0;
    
    if( A%8 == B%8 ){ //Same Vertical column
        if(A > B){
            while(B+8 < A){
                ray |= 1L << (B+8);
                B+=8;
            }
        }
        else{
            while(B-8 > A){
                ray |= 1L << (B-8);
                B-=8;
            }
        }
    }
    else if( (int)(A - A%8)/8 == (int)(B - B%8)/8 ){ //Same Row
        if(A > B){
            while(B+1 < A){
                ray |= 1L << (B+1);
                B++;
            }
        }
        else{
            while(B-1 > A){
                ray |= 1L << (B-1);
                B--;
            }
        }    
    }
    else if( A > B ){
        if(A%8 > B%8){
            while(B+9 < A){
                ray |= 1L << (B+9);
                B+=9;
            }
        }
        else{
            while(B+7 < A){
                ray |= 1L << (B+7);
                B+=7;
            }            
        }
    }
    else{
        if(A%8 > B%8){
            while(B-7 > A){
                ray |= 1L << (B-7);
                B-=7;
            }
        }
        else{
            while(B-9 > A){
                ray |= 1L << (B-9);
                B-=9;
            }
        }
    }
    
    return ray;
}
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

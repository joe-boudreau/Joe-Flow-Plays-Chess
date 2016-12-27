/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package joeflowplayschess;



public class Constants {

public long[] BITSQUARES = new long[64];

public long RANK_1 = 0xffL;
public long RANK_2 = 0xff00L;
public long RANK_3 = 0xff0000L;
public long RANK_4 = 0xff000000L;
public long RANK_5 = 0xff00000000L;
public long RANK_6 = 0xff0000000000L;
public long RANK_7 = 0xff000000000000L;
public long RANK_8 = 0xff00000000000000L;

public long[] RANKS = new long[]{RANK_1, RANK_2, RANK_3, RANK_4, RANK_5, RANK_6, RANK_7, RANK_8};
        
public long FILE_A = 0x101010101010101L;
public long FILE_B = 0x202020202020202L;
public long FILE_C = 0x404040404040404L;
public long FILE_D = 0x808080808080808L;
public long FILE_E = 0x1010101010101010L;
public long FILE_F = 0x2020202020202020L;
public long FILE_G = 0x4040404040404040L;
public long FILE_H = 0x8080808080808080L;

public long[] FILES = new long[]{FILE_A, FILE_B, FILE_C, FILE_D, FILE_E, FILE_F, FILE_G, FILE_H};

public long[] KnightMoves = new long[64];
public long[] KingMoves = new long[64];

public int moveFlagEnPassant =       0b00100000;
public int moveFlagQueenSideCastle = 0b01000000;
public int moveFlagKingSideCastle =  0b10000000;


public Constants(){
    
BITSQUARES[0] = 1;    
      
for(int i =  1; i < 64; i++){
    BITSQUARES[i] = BITSQUARES[i-1] << 1;
}
    
KingMoves[35] = 0x1c141c000000L;
    
for(int i = 36; i<64; i++){
    KingMoves[i] = KingMoves[i-1] >> 1;
    if( i%8 == 7){ KingMoves[i] &= ~(FILE_A);}
    if( i%8 == 0){ KingMoves[i] &= ~(FILE_H);}
    
}
    
for(int j = 34; j>-1; j--){
    KingMoves[j] = KingMoves[j+1] << 1;
    if( j%8 == 7){ KingMoves[j] &= ~(FILE_A);}
    if( j%8 == 0){ KingMoves[j] &= ~(FILE_H);}
    
}

KnightMoves[35] = 0x14220022140000L;
    
for(int k = 36; k<64; k++){
    KnightMoves[k] = KnightMoves[k-1] >> 1;
    if( k%8 == 6){ KnightMoves[k] &= ~(FILE_A);}
    if( k%8 == 7){ KnightMoves[k] &= ~(FILE_A | FILE_B);}
    if( k%8 == 0){ KnightMoves[k] &= ~(FILE_G | FILE_H);}
    if( k%8 == 1){ KnightMoves[k] &= ~(FILE_H);}
}
    
for(int l = 34; l>-1; l--){
    KnightMoves[l] = KnightMoves[l+1] << 1;
    if( l%8 == 6){ KnightMoves[l] &= ~(FILE_A);}
    if( l%8 == 7){ KnightMoves[l] &= ~(FILE_A | FILE_B);}
    if( l%8 == 0){ KnightMoves[l] &= ~(FILE_G | FILE_H);}
    if( l%8 == 1){ KnightMoves[l] &= ~(FILE_H);}
    
}

    }

    
}

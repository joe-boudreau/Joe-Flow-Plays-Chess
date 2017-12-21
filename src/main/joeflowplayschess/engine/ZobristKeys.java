package joeflowplayschess.engine;

import java.util.Random;

public class ZobristKeys {

    private final long[][] piecePositionKeys = new long[13][64];
    private final long[] gameFlagKeys = new long[256];
    private final long[] colourKey = new long[2];

    public ZobristKeys(){

        Random random = new Random();

        colourKey[1] = random.nextLong();

        for(int i = 0; i < 256; i++){
            gameFlagKeys[i] = random.nextLong();
        }

        for(int i = 1; i < 13; i++){

            for(int j = 0; j < 64; j++){
                    piecePositionKeys[i][j] = random.nextLong();
            }

        }

    }

    public long getPiecePositionKey(int piece, int square) {
        return piecePositionKeys[piece][square];
    }

    public long getGameFlagKey(byte gameFlag) {
        return gameFlagKeys[gameFlag + 128];
    }

    public long getColourKey(int colour) {
        return colourKey[colour];
    }

    public long generateZobristKey(GameState gameState){

        long zKey = 0;

        for(int i = 0; i < 64; i++){
            zKey ^= piecePositionKeys[gameState.getBoard()[i]][i];
        }

        zKey ^= getGameFlagKey(gameState.getFlags());
        zKey ^= colourKey[gameState.getTurn()];

        return zKey;
    }

    public long updateZobristKey(long currentKey, int[] parsedMove, byte oldFlags, byte newFlags){

        long newKey = currentKey;

        newKey ^= piecePositionKeys[parsedMove[0]][parsedMove[2]]; //remove moving piece from start square
        newKey ^= piecePositionKeys[parsedMove[0]][parsedMove[3]]; //add moving piece to end square

        newKey ^= piecePositionKeys[parsedMove[1]][parsedMove[3]]; //remove captured piece ... will be XOR with 0 if no captured piece so no change

        newKey ^= getGameFlagKey(oldFlags);
        newKey ^= getGameFlagKey(newFlags);

        newKey ^= colourKey[1 - Utils.pieceNumToColour(parsedMove[0])]; //Switch colour

        return newKey;
    }



}

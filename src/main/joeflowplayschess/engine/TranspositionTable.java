package joeflowplayschess.engine;

import java.util.HashMap;

public class TranspositionTable {

    private final int TABLE_SIZE;

    public enum NodeType {EXACT, ALPHA, BETA};

    public final HashMap<Integer, PositionEntry> tt;

    public TranspositionTable(int size){
        TABLE_SIZE = size;
        tt = new HashMap<>(TABLE_SIZE, 1f);
    }

    public void put(GameState state, int depth, int score, int bestMove, NodeType type){

        long zobrist = state.getZobristKey();
        int index = (int) (zobrist >> 32) % TABLE_SIZE;

        if(tt.containsKey(index)){
            PositionEntry existingPosition = tt.get(index);
            if(!existingPosition.ancient && depth < existingPosition.depth){
                return; //dont replace
            }
        }

        tt.put(index, new PositionEntry(zobrist, depth, score, bestMove, type, false));

    }

    public PositionEntry getEntryIfExists(GameState state){

        long zobrist = state.getZobristKey();
        int index = (int) (zobrist >> 32) % TABLE_SIZE;

        PositionEntry entry = tt.get(index);
        if (entry != null && entry.zobrist == zobrist){
            return tt.get(index);
        }
        else{
            return null;
        }

    }

    public Integer getValueFromEntry(PositionEntry entry, GameState state, int depth, int alpha, int beta){

        long zobrist = state.getZobristKey();

        if(entry.depth >= depth) {

            switch (entry.type){

                case EXACT:
                    return entry.evalScore;
                case ALPHA:
                    if (entry.evalScore >= beta) {
                        return beta;
                    }
                    break;
                case BETA:
                    if (entry.evalScore <= alpha) {
                        return alpha;
                    }
                    break;
            }
        }
        return null;
    }


    public void setAllAncient(){
        tt.entrySet().parallelStream().forEach(p -> p.getValue().setToAncient());
    }


    public static class PositionEntry{

        public long zobrist;
        public int depth;
        public int evalScore;
        public int bestMove;
        public NodeType type;
        public boolean ancient;

        public PositionEntry(long zobrist, int depth, int score, int bestMove, NodeType type, boolean ancient){
            this.zobrist = zobrist;
            this.depth = depth;
            this.evalScore = score;
            this.bestMove = bestMove;
            this.type = type;
            this.ancient = ancient;
        }

        public void setToAncient(){
            ancient = true;
        }
    }
}

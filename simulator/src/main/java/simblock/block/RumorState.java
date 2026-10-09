package simblock.block;

/**
 * Record the state transition time.
 */
public class RumorState {
    int state; // 0 <= state <= endState
    int hops; // #hop when a node turns to `state`
    int time; // time when a node turns to `state`

    public static final RumorState NULL_STATE = new RumorState(-1, -1, -1);

    public RumorState(int s, int h, int t) {
        state = s;
        hops = h;
        time = t;
    }

    public int getState() {
        return state;
    }

    public int getHops() {
        return hops;
    }

    public int getTime() {
        return time;
    }

    /** Define constants of different end state value */
    public enum EndState {
        /**
         * For a msg with END_STATE_ONE, 0 stands for don't know the msg, 1 stands
         * for knows the full msg
         */
        ONE(1),
        /**
         * For a msg with END_STATE_TWO, 0 stands for don't know the msg, 1 stands
         * for knows the existence/partial of the msg, 2 stands for knows the full
         * msg
         */
        TWO(2);

        private final int value;

        EndState(int state) {
            this.value = state;
        }

        public int getValue() {
            return this.value;
        }
    }

}

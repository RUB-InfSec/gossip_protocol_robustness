/*
 * Copyright 2019 Distributed Systems Group
 *
 * <p>Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package simblock.settings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The type Network configuration allows to configure network latency and
 * bandwidth.
 */
public class NetworkConfiguration {

    /**
     * True if the simulation simulates latency between nodes in different
     * region. False if the simulation use rounds by setting a fixed latency
     * between any two nodes and ignoring the message size.
     */
    public static boolean SIMULATE_REAL_NETWORK_LATENCY = true;

    /** 50ms simulated network latency */
    public static int SIMULATED_EDGE_LATNECY_MEAN = 50;

    /** 10% jitter for simulated network latency */
    public static double SIMULATED_EDGE_LATENCY_JITTER = 0.1;

    public static long SIMULATED_BANDWIDTH = 40_000_000L; // 100 Mbps
    /**
     * Regions where nodes can exist.
     */
    public static final List<String> REGION_LIST = new ArrayList<>(
            Arrays.asList("NORTH_AMERICA", "EUROPE", "SOUTH_AMERICA", "ASIA_SOUTH", "ASIA_PACIFIC", "JAPAN",
                    "AUSTRALIA"));

    /**
     * LATENCY[i][j] is average latency from REGION_LIST[i] to REGION_LIST[j]
     * Unit: millisecond, for year 2015
     */
    private static final int[][] LATENCY_2015 = {
            { 36, 119, 255, 310, 154, 208 },
            { 119, 12, 221, 242, 266, 350 },
            { 255, 221, 137, 347, 256, 269 },
            { 310, 242, 347, 99, 172, 278 },
            { 154, 266, 256, 172, 9, 163 },
            { 208, 350, 269, 278, 163, 22 }
    };

    /**
     * LATENCY[i][j] is average latency from REGION_LIST[i] to REGION_LIST[j]
     * Unit: millisecond, for year 2019
     */
    private static final int[][] LATENCY_2019 = {
            { 32, 124, 184, 198, 151, 189 },
            { 124, 11, 227, 237, 252, 294 },
            { 184, 227, 88, 325, 301, 322 },
            { 198, 237, 325, 85, 58, 198 },
            { 151, 252, 301, 58, 12, 126 },
            { 189, 294, 322, 198, 126, 16 }
    };

    /**
     * LATENCY[i][j] is average latency from REGION_LIST[i] to REGION_LIST[j]
     * Unit: millisecond, for year 2022
     */
    private static final int[][] LATENCY_2022 = {
            { 13, 39, 44, 64, 78, 44, 68 },
            { 39, 6, 90, 72, 54, 85, 78 },
            { 44, 71, 17, 108, 115, 98, 112 },
            { 68, 72, 108, 13, 30, 13, 72 },
            { 68, 54, 115, 30, 20, 23, 47 },
            { 57, 85, 98, 27, 23, 10, 102 },
            { 74, 88, 112, 71, 47, 107, 17 }
    };
    private static final int[][] LATENCY_FITTING_CBR_22 = {
            { 24, 70, 79, 115, 140, 79, 122 },
            { 70, 12, 161, 129, 97, 152, 140 },
            { 79, 128, 30, 195, 207, 176, 201 },
            { 122, 129, 195, 24, 54, 24, 129 },
            { 122, 97, 207, 54, 36, 42, 85 },
            { 103, 152, 176, 48, 42, 18, 183 },
            { 134, 158, 201, 128, 85, 192, 30 }
    };

    /*
        ms	        NA	EU	SA	AsiaSouth  AsiaPacific	Japan	Australia
        NA	        1.49	91.27	113.82	185.72	222.71	149.01	205.47
        EU	        91.27	1.81	202.80	127.03	160.92	227.74	291.41
        SA	        113.82	202.80	1.22	295.99	332.10	259.62	310.91
        AsiaSouth	185.72	127.03	295.99	1.46	62.91	133.52	255.20
        AsiaPacific	222.71	160.92	332.10	62.91	1.04	69.53	181.10
        Japan	        149.01	227.74	259.62	133.52	69.53	1.16	111.30
        Australia	205.47	291.41	310.91	255.20	181.10	111.30	1.13
    */
   // Order: North America, Europe, South America, Asia South, Asia Pacific, Japan, Australia
   // Diagonal (same-region) entries are set to 8ms rather than the raw measured
   // ~1-2ms: getEdgeLatency()'s Pareto sampling uses scale = mean - 5, which goes
   // negative (and produces huge/negative-before-rounding latencies) for mean <= 5.
   private static final int[][] LATENCY_2026 = {
        {  8,  92, 114, 186, 223, 150, 206 },  // North America
        { 92,   8, 203, 128, 161, 228, 292 },  // Europe
        {114, 203,   8, 296, 333, 260, 311 },  // South America
        {186, 128, 296,   8,  63, 134, 256 },  // Asia South
        {223, 161, 333,  63,   8,  70, 182 },  // Asia Pacific
        {150, 228, 260, 134,  70,   8, 112 },  // Japan
        {206, 292, 311, 256, 182, 112,   8 }   // Australia
};

    /**
     * List of latency assigned to each region. (unit: millisecond)
     */
    public static int[][] LATENCY = LATENCY_FITTING_CBR_22;
//     public static int[][] LATENCY = LATENCY_2026;

    /**
     * List of download bandwidth assigned to each region, and last element is
     * Inter-regional
     * bandwidth. (unit: bit per second) for year 2015
     */
    private static final long[] DOWNLOAD_BANDWIDTH_2015 = {
            25000000, 24000000, 6500000, 10000000,
            17500000, 14000000, 6 * 1000000
    };

    /**
     * List of download bandwidth assigned to each region, and last element is
     * Inter-regional
     * bandwidth. (unit: bit per second) for year 2019
     */
    private static final long[] DOWNLOAD_BANDWIDTH_2019 = {
            52000000, 40000000, 18000000, 22800000,
            22800000, 29900000, 6 * 1000000
    };

    private static final long[] DOWNLOAD_BANDWIDTH_2022 = {
            120_000_000L, 84_000_000L, 48_000_000L,
            84_000_000L, 240_000_000L, 12_0000_000L, 43_200_000L, 24_000_000L
    };

    /** double the value in 2022 */
    private static final long[] DOWNLOAD_BANDWIDTH_2026 = {
            240_000_000L, 168_000_000L, 96_000_000L,
            168_000_000L, 480_000_000L, 24_0000_000L, 86_400_000L, 48_000_000L
    };


    private static final long[] DOWNLOAD_BANDWIDTH_CLOUD = {
            500_000_000L, 500_000_000L, 500_000_000L, 
            500_000_000L, 500_000_000L, 500_000_000L, 500_000_000L   
    };
    /**
     * List of download bandwidth assigned to each region, and last element is
     * Inter-regional
     * bandwidth. (unit: bit per second)
     */
//     public static long[] DOWNLOAD_BANDWIDTH = DOWNLOAD_BANDWIDTH_2022;
//     public static long[] DOWNLOAD_BANDWIDTH = DOWNLOAD_BANDWIDTH_CLOUD;
    public static long[] DOWNLOAD_BANDWIDTH = DOWNLOAD_BANDWIDTH_2026;

    /**
     * List of upload bandwidth assigned to each region. (unit: bit per second), and
     * last element
     * is Inter-regional bandwidth for year 2015
     */
    private static final long[] UPLOAD_BANDWIDTH_2015 = {
            4700000, 8100000, 1800000, 5300000,
            3400000, 5200000, 6 * 1000000
    };

    /**
     * List of upload bandwidth assigned to each region. (unit: bit per second), and
     * last element
     * is Inter-regional bandwidth for year 2019
     */
    private static final long[] UPLOAD_BANDWIDTH_2019 = {
            19200000, 20700000, 5800000, 15700000,
            10200000, 11300000, 6 * 1000000
    };

    private static final long[] UPLOAD_BANDWIDTH_2022 = {
            60_000_000L, 42_000_000L, 24_000_000L, 42_000_000L,
            120_000_000L, 60_000_000L, 21_600_000L, 12_000_000L
    };

//     private static final long[] UPLOAD_BANDWIDTH_CLOUD = {
//             500_000_000L, 500_000_000L, 500_000_000L, 
//             500_000_000L, 500_000_000L, 500_000_000L, 500_000_000L   
//     };

    private static final long[] UPLOAD_BANDWIDTH_CLOUD = {
            250_000_000L, 250_000_000L, 250_000_000L, 
            250_000_000L, 250_000_000L, 250_000_000L, 250_000_000L   
    };

    /** double the value in 2022 */
    private static final long[] UPLOAD_BANDWIDTH_2026 = {
            120_000_000L, 84_000_000L, 48_000_000L, 84_000_000L,
            240_000_000L, 120_000_000L, 23_200_000L, 24_000_000L
    };

    private static final long[] UPLOAD_FITTING_CBR_22 = { 
        6875000 * 8, 4812500 * 8, 2750000 * 8, 4812500 * 8, 13750000 * 8, 6875000 * 8, 2475000 * 8, 1375000 * 8 };

    /** double the value in 2022 */
    private static final long[] UPLOAD_FITTING_CBR_26 = { 
        110_000_000L, 77_000_000L, 44_000_000L, 77_000_000L, 220_000_000L, 110_000_000L, 39_600_000L, 22_000_000L };
    /**
     * List of upload bandwidth assigned to each region. (unit: bit per second), and
     * last element
     * is Inter-regional bandwidth.
     */
//     public static long[] UPLOAD_BANDWIDTH = UPLOAD_FITTING_CBR_22;
//     public static long[] UPLOAD_BANDWIDTH = UPLOAD_BANDWIDTH_CLOUD;
    public static long[] UPLOAD_BANDWIDTH = UPLOAD_BANDWIDTH_2026;

    /**
     * Region distribution Bitcoin 2015.
     */
    private static final double[] REGION_DISTRIBUTION_BITCOIN_2015 = {
            0.3869, 0.5159, 0.0113,
            0.0574, 0.0119, 0.0166
    };

    /**
     * Region distribution Bitcoin 2019.
     */
    private static final double[] REGION_DISTRIBUTION_BITCOIN_2019 = {
            0.3316, 0.4998, 0.0090,
            0.1177, 0.0224, 0.0195
    };

    private static final double[] REGION_DISTRIBUTION_BITCOIN_2022 = {
        0.414,  // North America
        0.457,  // Europe
        0.0090, // South America
        0.072,  // Asia South
        0.011,  // Asia Pacific
        0.02,   // Japan
        0.018   // Australia
    };

    /**
     * Region distribution Litecoin.
     */
    private static final double[] REGION_DISTRIBUTION_LITECOIN = {
            0.3661, 0.4791, 0.0149, 0.1022, 0.0238, 0.0139
    };

    /**
     * Region distribution Dogecoin.
     */
    private static final double[] REGION_DISTRIBUTION_DOGECOIN = {
            0.3924, 0.4879, 0.0212, 0.0697, 0.0106, 0.0182
    };


    /*
        Region	        Validators	PDF
        North America	51	0.1401
        Europe	224	0.6154
        South America	19	0.0522
        Asia South	9	0.0247
        Asia Pacific	35	0.0962
        Japan	15	0.0412
        Australia (+NZ)	11	0.0302
     */
    private static final double[] REGION_DISTRIBUTION_POLKADOT_2026 = {
        0.1401, // North America
        0.6154, // Europe
        0.0522, // South America
        0.0247, // Asia South
        0.0962, // Asia Pacific
        0.0412, // Japan
        0.0302  // Australia
    };

    // Order: North America, Europe, South America, Asia South, Asia Pacific, Japan, Australia
    private static final double[] REGION_DISTRIBUTION_SOLANA_2026 = {
        0.2006,   // North America (136 / 678)
        0.6062,   // Europe (411 / 678)
        0.0206,   // South America (14 / 678)
        0,       // Asia South — not applicable, no Solana validators
        0.0914,   // Asia Pacific (62 / 678)
        0.0811,   // Japan (55 / 678)
        0        // Australia — not applicable, no Solana validators
        };

        // PDF, normalized across these 7 regions (sums to 1.0)
//     private static final double[] REGION_DISTRIBUTION_ETHEREUM_2026 = {
//         0.3691,   // North America
//         0.4519,   // Europe
//         0.0078,   // South America
//         0.0029,   // Asia South
//         0.1052,   // Asia Pacific
//         0.0397,   // Japan
//         0.0234    // Australia
//   };

    private static final double[] REGION_DISTRIBUTION_ETHEREUM_2026 = {
        0.108,   // North America
        0.698,   // Europe
        0.0,   // South America
        0.056,   // Asia South
        0.0,   // Asia Pacific
        0.0,   // Japan
        0.137    // Australia
  };

    /**
     * The distribution of node's region. Each value means the rate of the number of
     * nodes in the
     * corresponding region to the number of all nodes.
     */
//     public static final double[] REGION_DISTRIBUTION = REGION_DISTRIBUTION_POLKADOT_2026;
//     public static final double[] REGION_DISTRIBUTION = REGION_DISTRIBUTION_BITCOIN_2022;
//     public static final double[] REGION_DISTRIBUTION = REGION_DISTRIBUTION_SOLANA_2026;
    public static final double[] REGION_DISTRIBUTION = REGION_DISTRIBUTION_ETHEREUM_2026;

    /**
     * The cumulative distribution of number of outbound links for Bitcoin 2015.
     */
    private static final double[] DEGREE_DISTRIBUTION_BITCOIN_2015 = {
            0.025, 0.050, 0.075, 0.10, 0.20, 0.30, 0.40, 0.50, 0.60, 0.70, 0.80, 0.85, 0.90, 0.95, 0.97,
            0.97, 0.98, 0.99, 0.995, 1.0
    };

    /**
     * The cumulative distribution of number of outbound links for Litecoin.
     */
    private static final double[] DEGREE_DISTRIBUTION_LITECOIN = {
            0.01, 0.02, 0.04, 0.07, 0.09, 0.14, 0.20, 0.28, 0.39, 0.5, 0.6, 0.69, 0.76, 0.81, 0.85, 0.87,
            0.89, 0.92, 0.93, 1.0
    };

    /**
     * The cumulative distribution of number of outbound links for Dogecoin.
     */
    private static final double[] DEGREE_DISTRIBUTION_DOGECOIN = {
            0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 1.0, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00,
            1.00, 1.00, 1.00, 1.0
    };

    /**
     * The cumulative distribution of number of outbound links. Cf. Andrew Miller et
     * al.,
     * "Discovering bitcoin's public topology and influential nodes", 2015.
     */
    public static final double[] DEGREE_DISTRIBUTION = DEGREE_DISTRIBUTION_BITCOIN_2015;

    /** number of degree peers */
    public static int DEGREE = 8;

    /** number of known peers */
    public static int ROUTING_TABLE_SIZE = 20;

    /**
     * The rate of nodes that cause churn.
     */
    public static float CHURN_RATE = 0.976f;
    /*
     * timeout after which a regular node will send out the next REC message for the
     * same block
     */
    public static int TIMTOUT = 1200000;

    public static boolean USEMININGPOOLS = false;
    /*
     * number and distribution of the biggest Mining pool without the "unkown"
     * Miners
     */
    public static int PoolCount_A = 22;
    public static Double[] PoolProportion_A = { 23.002, 19.702, 15.072, 8.859, 8.687, 2.418, 1.946, 1.755, 1.207, 1.061,
            1.046, 0.997, 0.641, 0.296, 0.146, 0.109, 0.071, 0.041, 0.022, 0.022, 0.004, 0.004 };

    /*
     * number and distribution of the biggest Mining pool with the "unkown" Miners
     * as one pool
     */
    public static int PoolCount_B = 23;
    public static Double[] PoolProportion_B = { 23.002, 19.702, 15.072, 12.890, 8.859, 8.687, 2.418, 1.946, 1.755,
            1.207, 1.061, 1.046, 0.997, 0.641, 0.296, 0.146, 0.109, 0.071, 0.041, 0.022, 0.022, 0.004, 0.004 };
    public static long NetworkMiningPower = 10000000;

    public static int PoolCount;
    public static Double[] PoolProportion;

    public static boolean getUSEMINGPOOLS() {
        return USEMININGPOOLS;
    }

    public static void setUSEMININGPOOLS(boolean bool) {
        USEMININGPOOLS = bool;
    }

    public static int getPoolCount() {
        return PoolCount;
    }

    public static void setPoolCount(int count) {
        PoolCount = count;
    }

    public static Double[] getPoolProportion() {
        return PoolProportion;
    }

    public static void setPoolProportions(Double[] pools) {
        PoolProportion = pools;
    }

    public static long getNetworkMiningPower() {
        return NetworkMiningPower;
    }

    public static void setNetworkMiningPower(long mining) {
        NetworkMiningPower = mining;
    }
}

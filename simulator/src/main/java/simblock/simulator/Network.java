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

package simblock.simulator;

import static simblock.settings.NetworkConfiguration.*;
import static simblock.simulator.Simulator.simRng;

import java.util.List;
import simblock.settings.NetworkConfiguration;

/**
 * The type Network represents a network split in regions, each node belonging
 * to a region with
 * an upload bandwidth
 * and a download bandwidth. Node degrees follow a predefined degree
 * distribution.
 */
public class Network {


    public static int getSimulatedEdgeLatency() {
        double jitter = SIMULATED_EDGE_LATNECY_MEAN * SIMULATED_EDGE_LATENCY_JITTER;
        int latency = (int) Math.round(SIMULATED_EDGE_LATNECY_MEAN + (simRng.nextDouble() * 2 - 1) * jitter);
        return latency;
    }
    /**
     * Gets latency according with 20% variance pallet distribution.
     *
     * @param from the from latency
     * @param to   the to latency
     * @return the calculated latency
     */
    public static int getEdgeLatency(int from, int to) {
        if (SIMULATE_REAL_NETWORK_LATENCY) {
            long mean = LATENCY[from][to];
            double shape = 0.2 * mean;
            double scale = mean - 5;
            return (int) Math.round(scale / Math.pow(simRng.nextDouble(), 1.0 / shape));
        }
        return getSimulatedEdgeLatency();
    }

    /**
     * Gets the minimum between the <em>from</em> upload bandwidth and <em>to</em>
     * download
     * bandwidth.
     *
     * @param from the from index in the
     *             {@link NetworkConfiguration#UPLOAD_BANDWIDTH} array.
     * @param to   the to index in the {@link NetworkConfiguration#UPLOAD_BANDWIDTH}
     *             array.
     * @return the bandwidth
     */

    public static long getBandwidth(int from, int to) {
        return Math.min(UPLOAD_BANDWIDTH[from], DOWNLOAD_BANDWIDTH[to]);
    }

    /**
     * Return the transmission based o the bandwidth including the processing time.
     *
     * @param messageSizeInByte in bytes
     * @param bandwidth         in bits per second
     * @return delay in milliseconds
     */
    public static int computeTransmissionDelay(long messageSizeInByte, long bandwidth) {
        if (SIMULATE_REAL_NETWORK_LATENCY) {
            return (int) (messageSizeInByte * 8 * 1000 / bandwidth);
        }
        return getSimulatedTransmissionDelay(messageSizeInByte);
    }

    public static int computeTransmissionDelay(long messageSizeInByte, int from, int to) {
        if (SIMULATE_REAL_NETWORK_LATENCY) {
            return computeTransmissionDelay(messageSizeInByte, getBandwidth(from, to));
        }
        return getSimulatedTransmissionDelay(messageSizeInByte);
    }

    public static int getSimulatedTransmissionDelay(long messageSizeInByte) {
        return (int) (messageSizeInByte * 8 * 1000 / SIMULATED_BANDWIDTH);
    }

    /**
     * Same as {@link #computeTransmissionDelay(long, long)} but at microsecond precision,
     * for callers that need to accumulate sub-millisecond transmission time across
     * several messages instead of truncating each one individually (e.g. a serialized
     * link sending many small messages at a bandwidth where a single message's delay
     * would otherwise round down to 0ms).
     *
     * @param messageSizeInByte in bytes
     * @param bandwidth         in bits per second
     * @return delay in microseconds
     */
    public static long computeTransmissionDelayMicros(long messageSizeInByte, long bandwidth) {
        if (SIMULATE_REAL_NETWORK_LATENCY) {
            return messageSizeInByte * 8 * 1_000_000L / bandwidth;
        }
        return getSimulatedTransmissionDelayMicros(messageSizeInByte);
    }

    public static long computeTransmissionDelayMicros(long messageSizeInByte, int from, int to) {
        if (SIMULATE_REAL_NETWORK_LATENCY) {
            return computeTransmissionDelayMicros(messageSizeInByte, getBandwidth(from, to));
        }
        return getSimulatedTransmissionDelayMicros(messageSizeInByte);
    }

    public static long getSimulatedTransmissionDelayMicros(long messageSizeInByte) {
        return messageSizeInByte * 8 * 1_000_000L / SIMULATED_BANDWIDTH;
    }

    /**
     * Compute the up session time in milliseconds according to log-logistic distribution
     * (see paper "Churn in the Bitcoin Network" by Muhammad Anas Imtiaz et al.).
     * @return up session time in milliseconds
     */
    public static int getUpSessionInMilliSec() {
        // The approximated distribution is in minute
        double alpha = 11;
        double beta = 0.771;
        double u;
        do {
            u = Math.random();
        } while (u == 0.0);
        return (int) Math.round(60*1000 * alpha * Math.pow((1/u - 1), -1/beta));
    }

    /**
     * Compute the down session time in milliseconds according to Weibull distribution
     * (see paper "Churn in the Bitcoin Network" by Muhammad Anas Imtiaz et al.).
     * @return up session time in milliseconds
     */
    public static int getDownSessionInMilliSec() {
        double lambda = 0.64;
        double k = 0.183;
        double u;
        do {
            u = Math.random();
        } while (u == 0.0);
        return (int) Math.round(60*1000 * lambda * Math.pow(- Math.log(1-u), 1/k));
    }

    /**
     * Gets region list.
     *
     * @return the {@link NetworkConfiguration#REGION_LIST} list.
     */
    public static List<String> getRegionList() {
        return REGION_LIST;
    }

    /**
     * Return the number of nodes in the corresponding region as a portion the
     * number of all nodes.
     *
     * @return an array the distribution
     */
    public static double[] getRegionDistribution() {
        return REGION_DISTRIBUTION;
    }

    /**
     * Get degree distribution double [ ].
     *
     * @return the double [ ]
     */
    public static double[] getDegreeDistribution() {
        return DEGREE_DISTRIBUTION;
    }

}

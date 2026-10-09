# Artifact for paper: “On the Robustness of Gossip Protocols in Modern Blockchains” (S&P ’27)

This repository contains the source code and the evaluation data used in our paper “On the Robustness of Gossip Protocols in Modern Blockchains” - **accepted** at the 48th IEEE Symposium on Security and Privacy **(IEEE S&P ’26)**.

The simulator is an extension of [SimBlock](https://ieeexplore.ieee.org/document/8845253), an event-driven simulator based on JAVA. Based on their work, we have further extended the simulator in the following aspects:
- simulate different topologies, such as grid topology and tree topology;
- simulate network churn events;
- simulate additional propagation protocols such as GossipSub using peer scoring function, and the Turbine propagation scheme;
- simulate additional adversarial strategies, such as selecting targeted nodes based on different *centralities* and *adaptively* corrupt the nodes with a chosen corruption delay.
More details can be found in [doc](docs/usage.md).

For the data points used to plot the figures in the paper:
- for the coverage analysis shown in Figure 3, see *data/coverage_analysis*,
- for the reconstructed topology in Figure 5, see *data/topology_reconstruction*,
- for the evaluation data measured by the simulator in Section 4, see *data/simulation_results*. 
- for the comparison with real-world blockchain network propagation latency measurements in Appendix A, see *data/blockchain-comparison*.

## License

SimBlock is licensed under the Apache License, Version2.0.

## Quick Start

To build the project:

```sh
gradle build
```

To run the simulator, the configuration template (used by default) is in the `simulator/src/main/resources/config.yaml`. One can specify the simulation parameter in this configuration file. To use an external configuration file:

```sh
gradle run --args='--config your_config_file.yaml --output your_output_file.json'
```

## Citation

**Coming soon**


## Contacts

Feel free to contact the first or second author via the e-mails provided on the publication.


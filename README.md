---
page: https://idle.run/docker-ignite
title: Apache Ignite on Docker 1.12 Swarm
tags: apache-ignite, docker
date: 2016-11-23
---

This project builds on top of Apache Ignite to provide a Docker Cluster compatible image and service definition to automatically scale a simple `memcached` compatible Ignite service across a cluster.

## Connecting

Apache Ignite exposes `memcached` by default on port 11211. Note that only the binary protocol is supported.

See `memcached` client documentation [here](apacheignite.gridgain.org/docs/memcached-support) for details

## Arguments

Configuration as required by system properties (`-D`)

### Cache Init

Optionally initialize a cache in full replication mode by passing the cache name as an argument.

This is useful to ensure the cache is configured as expected before connecting with a `memcached` client

```
-DCACHE_NAME=mycache
```

### Local Host Address

The Ignite local host address is normally discovered automatically from the first non-loopback address.
This doesn't work very well in Docker, especially with an overlay network. Even if it selects the correct
network interface, the VIP may be selected instead of the host address (both appear as addresses on the interface).

To aid in discovery, we can set a prefix which the address must match to be used (should match the overlay network address prefix). The matching addresses will be sorted numerically and the last address will be selected. This works well since the VIP tends to be the first address in the range.

```
-DNETWORK_PREFIX=10.0.1.
```

### Cluster Discovery

Set the DNS address to resolve to discover the other IP addresses of nodes to connect with.

Docker Cluster provides a special DNS address `tasks.servicename` (replace `servicename`) which solves to all hosts in the service cluster. Pass that DNS address as an argument to allow IP discovery.

```
-DCLUSTER_DNS=tasks.myignite
```

## Service Compile

Service is compiled to a single executable jar (`target/ignite-1.0.jar`) by running:

```bash
mvn package
```

## Docker Image

Build Docker Image by running

```bash
docker build -t ignite .
```

## Docker Service

```bash
docker service create \
  --mode global \
  --name myignite \
  --network enc-net \
  ignite \
  -DNETWORK_PREFIX=10.0.9. -DCLUSTER_DNS=tasks.myignite -DCACHE_NAME=cache
```
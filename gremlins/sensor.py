from gremlins import faults, metafaults, triggers, tc

clear_network_faults = faults.clear_network_faults()
introduce_partition = faults.introduce_network_partition()
introduce_latency = faults.introduce_network_latency()

INTERVAL=30

profile = [
    # clear any existing configurations
    triggers.OneShot(clear_network_faults),
    # every 5 seconds, either clear faults, introduce a latency or a partition
    # other faults are available, but let's start-simply
    triggers.Periodic(
        INTERVAL, metafaults.pick_fault([
            (10, clear_network_faults),
            (10, introduce_partition),
        ])),
]

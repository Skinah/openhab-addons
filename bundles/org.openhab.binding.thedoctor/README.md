# TheDoctor Binding

This binding is one I have been thinking of building for a while, it is designed to help diagnose issues on someones system that does not know Linux/Java and has no idea what free/used and available ram means, let alone the Java heap. Install the binding and get a system check up. The system info binding requires you to know what to look at and how to interrupt the results, this binding will hopefully over time get better at finding issues in an automated way.

## Supported Things

- `doctor`: Short description of the Thing.

### `doctor` Thing Configuration

| Name            | Type    | Description                           | Default | Required | Advanced |
|-----------------|---------|---------------------------------------|---------|----------|----------|
| refresh         | integer | Interval the health is polled in sec. | 15      | yes      | yes      |

## Channels

| Channel              | Type                 | Read/Write | Description                                                                          |
|----------------------|----------------------|------------|--------------------------------------------------------------------------------------|
| cleaned-heap-percent | Number:Dimensionless | R          | How much data in percent remains in the heap after garbage collection has been done. |

## Any custom content here!

If you enjoy the binding, please consider sponsoring or a once off tip as a thank you via the links. This allows me to purchase software and hardware to contributing more bindings. Also some coffee to keep me coding faster never hurts :slight_smile:
Sponsor @Skinah on GitHub Sponsors 2

Paypal can also be used via
matt A-T pcmus D-O-T C-O-M
